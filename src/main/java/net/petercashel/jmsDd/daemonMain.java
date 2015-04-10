/*******************************************************************************
 * Copyright 2015 Peter Cashel (pacas00@petercashel.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
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
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT.HANDLE;

import net.petercashel.commonlib.threading.threadManager;
import net.petercashel.commonlib.util.OS_Util;
import net.petercashel.jmsDd.auth.AuthSystem;
import net.petercashel.jmsDd.command.commandServer;
import net.petercashel.nettyCore.server.serverCore;
import net.petercashel.nettyCore.serverUDS.serverCoreUDS;
import net.petercashel.nettyCore.ssl.SSLContextProvider;
import static net.petercashel.jmsDd.Configuration.*;

public class daemonMain {

	public static boolean run = true;
	static Process p = null;
	static boolean pHasStopCmd = false;
	private static int pid = 0;

	public static void main(String[] args) {
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
		if (run)
			if (runCLI) net.petercashel.jmsDc.clientMain.main(args);
			else main();
		else
			net.petercashel.installer.installMain.main(args);
	}

	public static void main() {
		configInit();
		PrintStreamHandler.run();
		if (getDefault(getJSONObject(cfg, "daemonSettings"), "serverCLIEnable",
				false) == false
				&& getDefault(getJSONObject(cfg, "daemonSettings"),
						"serverPortEnable", false) == false) {
			System.err
					.println("The daemon requires that either serverCLIEnable or serverPortEnable is enabled");
			System.err.println("Please correct your configuration");
			System.err.println(new File(configDir, "config.json").toPath());
			System.exit(1);

		}
		if (getDefault(getJSONObject(cfg, "daemonSettings"), "serverCLIEnable",
				false) == true && OS_Util.isWinNT()) {
			System.err
					.println("Socket based CLI connections do not function on the Windows Platform.");
			System.err
					.println("Please correct your configuration by disabling serverCLIEnable");
			System.err.println(new File(configDir, "config.json").toPath());
			System.exit(1);

		}
		// init commands
		AuthSystem.init();
		commandServer.init();
		System.out.println(System.getProperty("user.home"));

		// Do SSL Config Settings
		serverCore.UseSSL = getDefault(getJSONObject(cfg, "daemonSettings"),
				"serverSSLEnable", true);
		serverCore.DoAuth = getDefault(getJSONObject(cfg, "authSettings"),
				"authenticationEnable", true);

		if (getDefault(
				getJSONObject(getJSONObject(cfg, "daemonSettings"),
						"SSLSettings"), "SSL_UseExternal", true)) {
			SSLContextProvider.useExternalSSL = true;
			SSLContextProvider.pathToSSLCert = getDefault(
					getJSONObject(getJSONObject(cfg, "daemonSettings"),
							"SSLSettings"), "SSL_ExternalPath", (new File(
							configDir, "SSLCERT.p12").toPath().toString()));
			SSLContextProvider.SSLCertSecret = getDefault(
					getJSONObject(getJSONObject(cfg, "daemonSettings"),
							"SSLSettings"), "SSL_ExternalSecret", "secret");
		}

		if (getDefault(getJSONObject(cfg, "daemonSettings"), "serverCLIEnable",
				true)) {
			// init server
			threadManager.getInstance().addRunnable(new Runnable() {
				@Override
				public void run() {
					while (daemonMain.run) {
						try {
							serverCoreUDS.initializeServer(new File(
									(getJSONObject(cfg, "daemonSettings")
											.get("serverCLISocketPath"))
											.getAsString()).toPath());
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			});
		}

		if (getDefault(getJSONObject(cfg, "daemonSettings"),
				"serverPortEnable", false)) {
			// init server
			threadManager.getInstance().addRunnable(new Runnable() {
				@Override
				public void run() {
					while (daemonMain.run) {
						try {
							serverCore.initializeServer(getDefault(
									getJSONObject(cfg, "daemonSettings"),
									"serverPort", 14444));
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			});
		}
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// init other

		// init minecraft instance
		if (getDefault(getJSONObject(cfg, "processSettings"),
				"processAutoStart", true))
			RunProcess();

		pHasStopCmd = Configuration.getDefault(Configuration.getJSONObject(
				Configuration.cfg, "processSettings"),
				"processHasShutdownCommand", false);
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				try {
					ShutdownProcess();
				} catch (NullPointerException n) {
				}
			}
		}));
	}

	public static void shutdown() {
		run = false;
		try {
			ShutdownProcess();
		} catch (NullPointerException n) {
		}
		serverCore.shutdown();
		serverCoreUDS.shutdown();
		threadManager.getInstance().shutdown();
		// shutdown other
		saveConfig();
	}

	public static void RestartProcess() {
		try {
			p.exitValue();
			RunProcess();
		} catch (IllegalThreadStateException e) {
			try {
				ShutdownProcess();
			} catch (NullPointerException n) {
			}
		} catch (NullPointerException e) {
			RunProcess();
		}
	}

	public static void ShutdownProcess() throws NullPointerException {
		if (pHasStopCmd) {
			String s = Configuration.getDefault(Configuration.getJSONObject(
					Configuration.cfg, "processSettings"),
					"processShutdownCommand", "")
					+ System.lineSeparator();
			try {
				p.getOutputStream().write(s.getBytes());
				p.getOutputStream().flush();
				try {
					p.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
				// p.destroy();
			}
			;
		} else {
			p.destroy(); // TODO find away to send CTRL+C in java

		}
	}

	private static void RunProcess() {
		List<String> strings = new ArrayList<String>();
		strings.add(Configuration.getDefault(Configuration.getJSONObject(
				Configuration.cfg, "processSettings"), "processExcecutable", ""));
		String[] args = Configuration.getDefault(
				Configuration.getJSONObject(Configuration.cfg,
						"processSettings"), "processArguments", "").split(" ");
		for (String s : args)
			strings.add(s);
		ProcessBuilder pb = new ProcessBuilder(strings);
		Map<String, String> env = pb.environment();

		Path currentRelativePath = Paths.get(Configuration.getDefault(
				Configuration.getJSONObject(Configuration.cfg,
						"processSettings"), "processWorkingDirectory", ""));
		String s = currentRelativePath.toAbsolutePath().toString();
		pb.directory(new File(s));

		pb.redirectErrorStream(true);

		try {
			p = pb.start();
		} catch (IOException e) {
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
						System.out.println(s);
					}
				}
			}
		});

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
			/* determine the pid on windows plattforms */
			try {
				Field f = p.getClass().getDeclaredField("handle");
				f.setAccessible(true);
				long handl = f.getLong(p);
				Kernel32 kernel = Kernel32.INSTANCE;
				HANDLE handle = new HANDLE();
				handle.setPointer(Pointer.createConstant(handl));
				pid = kernel.GetProcessId(handle);
			} catch (Throwable e) {
			}
		}
		if (pid != 0) {
			File f = new File(configDir, "process.pid");
			f.delete();
			FileOutputStream o = null;
			try {
				o = new FileOutputStream(f, true);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			PrintStream p = new PrintStream(o, true);
			p.print(pid);
			p.flush();
			p.close();
			try {
				o.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				o.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
