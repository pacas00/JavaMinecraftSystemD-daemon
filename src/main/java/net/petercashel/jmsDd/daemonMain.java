/*******************************************************************************
 *    Copyright 2015 Peter Cashel (pacas00@petercashel.net)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *******************************************************************************/

package net.petercashel.jmsDd;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.ScheduleBuilder;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import net.petercashel.commonlib.threading.threadManager;
import net.petercashel.commonlib.util.OS_Util;
import net.petercashel.jmsDd.API.API;
import net.petercashel.jmsDd.auth.AuthSystem;
import net.petercashel.jmsDd.command.commandServer;
import net.petercashel.jmsDd.module.ModuleSystem;
import net.petercashel.jmsDd.util.AutoRestartJob;
import net.petercashel.jmsDd.util.PrintStreamHandler;
import net.petercashel.nettyCore.server.serverCore;
import net.petercashel.nettyCore.serverUDS.serverCoreUDS;
import net.petercashel.nettyCore.ssl.SSLContextProvider;
import static net.petercashel.jmsDd.Configuration.*;
import net.petercashel.jmsDd.event.*;
import net.petercashel.jmsDd.event.module.ModuleConfigEvent;
import net.petercashel.jmsDd.event.module.ModuleInitEvent;
import net.petercashel.jmsDd.event.module.ModulePostInitEvent;
import net.petercashel.jmsDd.event.module.ModulePreInitEvent;
import net.petercashel.jmsDd.event.module.ModuleShutdownEvent;
import net.petercashel.jmsDd.event.process.AutoRestartStartEvent;
import net.petercashel.jmsDd.event.process.AutoRestartStopEvent;
import net.petercashel.jmsDd.event.process.ProcessPreRestartEvent;
import net.petercashel.jmsDd.event.process.ProcessRestartEvent;
import net.petercashel.jmsDd.event.process.ProcessShutdownEvent;
import net.petercashel.jmsDd.event.process.WatchDogStartEvent;
import net.petercashel.jmsDd.event.process.WatchDogStopEvent;

public class daemonMain {

	public static boolean run = true;
	public static EventBus eventBus = new EventBus();
	static Process p = null;
	static boolean pHasStopCmd = false;
	private static int pid = 0;
	static boolean runDog = true;
	static Timer watchdoggy;
	private static Boolean autoRestart;
	private static ScheduledExecutorService service;
	public static Scheduler quartzSched = null;
	private static JobKey AutoRestartJobKey;

	public static void main(String[] args) throws IOException {
		boolean run = true;
		boolean runCLI = false;
		for (String s : args) {
			if (s.equalsIgnoreCase("install")) {
				run = false;
			}
			if (s.equalsIgnoreCase("client")) {
				runCLI = true;
			}
		}
		if (run) if (runCLI) {
			net.petercashel.jmsDc.clientMain.main(args);
		} else {
			main();
		}
		else {
			net.petercashel.installer.installMain.main(args);
		}
	}

	public static void main() {
		configInit();
		PrintStreamHandler.run();
		API.Impl.api = new APICore();
		if (getDefault(getJSONObject(cfg, "daemonSettings"), "serverCLIEnable", false) == false
				&& getDefault(getJSONObject(cfg, "daemonSettings"), "serverPortEnable", false) == false) {
			System.err.println("The daemon requires that either serverCLIEnable or serverPortEnable is enabled");
			System.err.println("Please correct your configuration");
			System.err.println(new File(configDir, "config.json").toPath());
			System.exit(1);

		}
		if (getDefault(getJSONObject(cfg, "daemonSettings"), "serverCLIEnable", false) == true && OS_Util.isWinNT()) {
			System.err.println("Socket based CLI connections do not function on the Windows Platform.");
			System.err.println("Please correct your configuration by disabling serverCLIEnable");
			System.err.println(new File(configDir, "config.json").toPath());
			System.exit(1);

		}
		// Init modules into classpath so event system can startup.
		eventBus.register(new daemonMain());
		SchedulerFactory sf = new StdSchedulerFactory();
		try {
			quartzSched = sf.getScheduler();
		}
		catch (SchedulerException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			quartzSched.start();
		}
		catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ModuleSystem.loadAllModuleJars();
		ModuleSystem.LoadFoundModules();
		eventBus.post(new ModuleConfigEvent());

		// init commands
		AuthSystem.init();
		commandServer.init();
		eventBus.post(new ModulePreInitEvent());
		// Do SSL Config Settings
		serverCore.UseSSL = getDefault(getJSONObject(cfg, "daemonSettings"), "serverSSLEnable", true);
		serverCore.DoAuth = getDefault(getJSONObject(cfg, "authSettings"), "authenticationEnable", true);

		if (getDefault(getJSONObject(getJSONObject(cfg, "daemonSettings"), "SSLSettings"), "SSL_UseExternal", true)) {
			SSLContextProvider.useExternalSSL = true;
			SSLContextProvider.pathToSSLCert = getDefault(
					getJSONObject(getJSONObject(cfg, "daemonSettings"), "SSLSettings"), "SSL_ExternalPath", (new File(
							configDir, "SSLCERT.p12").toPath().toString()));
			SSLContextProvider.SSLCertSecret = getDefault(
					getJSONObject(getJSONObject(cfg, "daemonSettings"), "SSLSettings"), "SSL_ExternalSecret", "secret");
		}

		if (getDefault(getJSONObject(cfg, "daemonSettings"), "serverCLIEnable", true)) {
			// init server
			threadManager.getInstance().addRunnable(new Runnable() {
				@Override
				public void run() {
					while (daemonMain.run) {
						try {
							serverCoreUDS.initializeServer(new File((getJSONObject(cfg, "daemonSettings")
									.get("serverCLISocketPath")).getAsString()).toPath());
						}
						catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			});
		}

		if (getDefault(getJSONObject(cfg, "daemonSettings"), "serverPortEnable", false)) {
			// init server
			threadManager.getInstance().addRunnable(new Runnable() {
				@Override
				public void run() {
					while (daemonMain.run) {
						try {
							serverCore.initializeServer(getDefault(getJSONObject(cfg, "daemonSettings"), "serverPort",
									14444));
						}
						catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			});
		}
		try {
			Thread.sleep(2000);
		}
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// init other
		eventBus.post(new ModuleInitEvent());

		runDog = getDefault(getJSONObject(cfg, "processSettings"), "Watchdog", true);
		autoRestart = getDefault(getJSONObject(cfg, "processSettings"), "AutoRestart", true);

		eventBus.post(new AutoRestartStartEvent());

		// init minecraft instance
		if (getDefault(getJSONObject(cfg, "processSettings"), "processAutoStart", true)) RunProcess();

		pHasStopCmd = Configuration.getDefault(Configuration.getJSONObject(Configuration.cfg, "processSettings"),
				"processHasShutdownCommand", false);
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				try {
					eventBus.post(new ProcessShutdownEvent());
				}
				catch (NullPointerException n) {
				}
			}
		}));
		eventBus.post(new ModulePostInitEvent());

		// Every 5 mins do garbage cleanup

		service = Executors.newSingleThreadScheduledExecutor();
		service.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				System.gc();
			}
		}, 5, 15, TimeUnit.MINUTES);
	}

	@Subscribe
	public static void startAutoRestart(AutoRestartStartEvent autoRestartStartEvent) {
		if (autoRestart) {

			int h = getDefault(getJSONObject(cfg, "processSettings"), "AutoRestartHour", 5);
			int m = getDefault(getJSONObject(cfg, "processSettings"), "AutoRestartMinute", 0);

			JobDetail job = JobBuilder.newJob(AutoRestartJob.class)  
					.withIdentity("AutoRestartJob", "AutoRestartJob")  
					.build();
			AutoRestartJobKey = job.getKey();
			// Schedule to run at 5 AM every day
			ScheduleBuilder scheduleBuilder = 
					CronScheduleBuilder.cronSchedule("0 " + m + " " + h + " * * ?");
			Trigger trigger = TriggerBuilder.newTrigger().
					withSchedule(scheduleBuilder).build();

			try {
				quartzSched.scheduleJob(job, trigger);
			}
			catch (SchedulerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	@Subscribe
	public static void stopAutoRestart(AutoRestartStopEvent autoRestartStopEvent) {
		try {
			if (quartzSched.checkExists(AutoRestartJobKey)) {
				quartzSched.deleteJob(AutoRestartJobKey);
			}
		}
		catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void shutdown() {
		run = false;
		saveConfig();
		try {
			quartzSched.pauseAll();
		}
		catch (SchedulerException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			if (quartzSched.checkExists(AutoRestartJobKey)) {
				quartzSched.deleteJob(AutoRestartJobKey);
			}
		}
		catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (runDog && watchdoggy != null) {
			watchdoggy.cancel();
			watchdoggy.purge();
			watchdoggy = null;
		}
		service.shutdown();
		eventBus.post(new ModuleShutdownEvent());
		try {
			eventBus.post(new ProcessShutdownEvent());
		}
		catch (NullPointerException n) {
		}
		serverCore.shutdown();
		serverCoreUDS.shutdown();
		try {
			quartzSched.shutdown();
		}
		catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		threadManager.getInstance().shutdown();		
	}

	@Subscribe
	public static void RestartProcess(ProcessRestartEvent event) {
		eventBus.post(new ProcessShutdownEvent());
		try {
			Thread.sleep(5000);
		}
		catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			// p is null on start, so assume p being null means not running.
			if (p != null) {
				// Process never has an exitcode when running and therefore
				// throws
				int i = p.exitValue();
			}
			// If we get here, it died
			RunProcess();
		}
		catch (IllegalThreadStateException e) {
			e.printStackTrace();
			System.out.println("Failed to restart process");
		}
	}

	@Subscribe
	public static void ShutdownProcess(ProcessShutdownEvent event) throws NullPointerException {
		if (runDog && watchdoggy != null) {
			watchdoggy.cancel();
			watchdoggy.purge();
			watchdoggy = null;
		}

		eventBus.post(new AutoRestartStopEvent());
		try {
			if (pHasStopCmd) {
				String s = Configuration.getDefault(Configuration.getJSONObject(Configuration.cfg, "processSettings"),
						"processShutdownCommand", "") + System.lineSeparator();
				try {
					p.getOutputStream().write(s.getBytes());
					p.getOutputStream().flush();
					try {
						p.waitFor();
					}
					catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				catch (IOException e1) {
					e1.printStackTrace();
					// p.destroy();
				};
			} else {
				p.destroy(); // TODO find away to send CTRL+C in java

			}
		} catch (NullPointerException e) {

		}
	}

	public static void KillProcess() throws NullPointerException {
		if (runDog && watchdoggy != null) {
			watchdoggy.cancel();
			watchdoggy.purge();
			watchdoggy = null;
		}

		eventBus.post(new AutoRestartStopEvent());
		try {
			p.destroy(); // TODO find away to send CTRL+C in java
		} catch (NullPointerException e) {

		}
	}

	private static void RunProcess() {
		threadManager.getInstance().addRunnable(new Runnable() {
			@Override
			public void run() {

				List<String> strings = new ArrayList<String>();
				strings.add(Configuration.getDefault(Configuration.getJSONObject(Configuration.cfg, "processSettings"),
						"processExcecutable", ""));
				String[] args = Configuration.getDefault(Configuration.getJSONObject(Configuration.cfg, "processSettings"),
						"processArguments", "").split(" ");
				for (String s : args)
					strings.add(s);
				ProcessBuilder pb = new ProcessBuilder(strings);
				Map<String, String> env = pb.environment();

				Path currentRelativePath = Paths.get(Configuration.getDefault(
						Configuration.getJSONObject(Configuration.cfg, "processSettings"), "processWorkingDirectory", ""));
				String s = currentRelativePath.toAbsolutePath().toString();
				pb.directory(new File(s));

				pb.redirectErrorStream(true);

				try {
					p = pb.start();
				}
				catch (IOException e) {
					e.printStackTrace();
				}

				commandServer.Progin = p.getOutputStream();
				final InputStream in = p.getInputStream();

				threadManager.getInstance().addRunnable(new Runnable() {
					@Override
					public void run() {
						Scanner sc = new Scanner(in);
						while (daemonMain.run) {
							while (sc.hasNextLine()) {
								String s = sc.nextLine();
								commandServer.out.println(s);
							}
						}
					}
				});

				eventBus.post(new AutoRestartStartEvent());

				eventBus.post(new WatchDogStartEvent());

				if (p.getClass().getName().equals("java.lang.UNIXProcess")) {
					/* get the PID on unix/linux systems */
					try {
						Field f = p.getClass().getDeclaredField("pid");
						f.setAccessible(true);
						pid = f.getInt(p);
					}

					catch (Throwable e) {
					}
				}
				if (p.getClass().getName().equals("java.lang.Win32Process")
						|| p.getClass().getName().equals("java.lang.ProcessImpl")) {
					/* determine the pid on windows platforms */
					try {
						Field f = p.getClass().getDeclaredField("handle");
						f.setAccessible(true);
						long handl = f.getLong(p);
						Kernel32 kernel = Kernel32.INSTANCE;
						HANDLE handle = new HANDLE();
						handle.setPointer(Pointer.createConstant(handl));
						pid = kernel.GetProcessId(handle);
					}
					catch (Throwable e) {
					}
				}
				if (pid != 0) {
					File f = new File(configDir, "process.pid");
					f.delete();
					FileOutputStream o = null;
					try {
						o = new FileOutputStream(f, true);
					}
					catch (FileNotFoundException e) {
						e.printStackTrace();
					}
					PrintStream p = new PrintStream(o, true);
					p.print(pid);
					p.flush();
					p.close();
					try {
						o.flush();
					}
					catch (IOException e) {
						e.printStackTrace();
					}
					try {
						o.close();
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		}
				);
	}

	@Subscribe
	public static void setupWatchDog(WatchDogStartEvent e) {
		if (runDog) {
			watchdoggy = new Timer();
			int i = getDefault(getJSONObject(cfg, "processSettings"), "WatchdogMinuteInterval", 5);
			watchdoggy.schedule(new Watchdog(), i * 60 * 1000);
		}

	}
	@Subscribe
	public static void stopWatchDog(WatchDogStopEvent e) {
		if (runDog && watchdoggy != null) {
			watchdoggy.cancel();
			watchdoggy.purge();
			watchdoggy = null;
		}
	}

	static class Watchdog extends TimerTask {

		@Override
		public void run() {
			StartProcess();
		}

	}

	public static void StartProcess() {
		try {
			// p is null on start, so assume p being null means not running.
			if (p != null) {
				// Process never has an exitcode when running and therefore
				// throws
				int i = p.exitValue();
			}
			// If we get here, it died
			RunProcess();
		}
		catch (IllegalThreadStateException e) {
			// All Good, process is alive.
		}

	}

	public static boolean ProcessRunning() {
		try {
			// p is null on start, so assume p being null means not running.
			if (p != null) {
				// Process never has an exitcode when running and therefore
				// throws
				int i = p.exitValue();
			}
			// If we get here, it died
			return false;
		}
		catch (IllegalThreadStateException e) {
			return true;
		}

	}

	public static void AutoRestart() {
		try {
			if (ProcessRunning()) eventBus.post(new ProcessShutdownEvent());
			while (ProcessRunning()) {
				try {
					Thread.sleep(1000L);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (Exception e) {

		}
		try {
			eventBus.post(new ProcessPreRestartEvent());
		} catch (Exception e) {

		}
		try {
			eventBus.post(new ProcessRestartEvent());
		} catch (Exception e) {

		}

	}

	@Subscribe
	public static void DeadEventHandler(DeadEvent e) {
		System.out.println("DeadEvent fired for event " + e.getEvent().toString() + " .");
	}
}
