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
package net.petercashel.installer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.ProcessBuilder.Redirect;
import java.net.URLDecoder;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import com.sun.jna.Library;
import com.sun.jna.Native;
import java.nio.file.attribute.BasicFileAttributes;

public class installMain {

	static File serviceDir = new File("/etc/systemd/system");
	static File usrsbin = new File("/usr/sbin");
	static File installDir = null;
	static boolean rootInstall = false;

	public static void main(String[] args) {
		Console c = new Console();
		PrintStream out = System.out;

		out.println("Enter the username to run the daemon as.");
		out.println("[Press enter for default of root]");
		String username = c.readLine("Username: ");
		out.println();
		if (username.isEmpty() || username.equalsIgnoreCase("")
				|| username == null) {
			rootInstall = true;
		}

		out.println("Enter the desired installation path.");
		out.println("[Press enter for default of /usr/share/JMSDd]");
		String dir = c.readLine("Directory: ");
		out.println();
		if (dir.isEmpty() || dir.equalsIgnoreCase("") || dir == null) {
			dir = "/usr/share/JMSDd";
		}
		if (dir.endsWith("/")) {
			dir = dir.substring(0, dir.length() - 1);
		}

		out.println("Install to " + dir + ".");
		if (rootInstall) {
			out.println("Install as " + "root");
		} else {
			out.println("Install as " + username);
		}

		String confirm = c.readLine("Are you ready to install? (y/n) ");
		out.println();
		if (confirm.isEmpty() || confirm.equalsIgnoreCase("")
				|| confirm == null || confirm.equalsIgnoreCase("no")
				|| confirm.equalsIgnoreCase("n")) {
			out.println("Installation Aborted!");
			System.exit(0);
		} else if (confirm.equalsIgnoreCase("yes")
				|| confirm.equalsIgnoreCase("ye")
				|| confirm.equalsIgnoreCase("ya")
				|| confirm.equalsIgnoreCase("y")) {
		} else {
			out.println("Invalid response.");
			out.println("Installation Aborted!");
			System.exit(0);
		}

		installDir = new File(dir);
		try {
			installDir.mkdirs();
			installDir.mkdirs();
		} catch (Exception e) {

		}
		if (!installDir.isDirectory()) {
			out.println("Error creating directory.");
			if (dir.equalsIgnoreCase("")) {
				out.println("Installation Failed.");
				System.exit(1);
			} else {
				out.println("Attempting installation to default dir");
				dir = "/usr/share/JMSDd";
				installDir = new File(dir);
				installDir.mkdirs();
				installDir.mkdirs();
				if (!installDir.isDirectory()) {
					out.println("Error creating directory.");
					out.println("Installation Failed.");
					System.exit(1);
				}
			}
		}
		if (!serviceDir.isDirectory()) {
			out.println("Systemd service directory missing.");
			out.println("Installation Failed.");
			System.exit(1);
		}
		File service = new File(serviceDir, "JMSDd.service");
		if (service.exists()) {
			ProcessBuilder pb1 = new ProcessBuilder("systemctl", "disable",
					"JMSDd.service");
			pb1.redirectOutput(Redirect.PIPE);
			Process ps1 = null;
			try {
				ps1 = pb1.start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				ps1.waitFor();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			service.delete();
		}
		FileOutputStream o = null;
		try {
			o = new FileOutputStream(service, true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		PrintStream p = new PrintStream(o, true);

		// [Unit]
		// Description=JavaMinecraftSystemDdaemon
		//
		// [Service]
		// ExecStart=/usr/bin/java -jar /opt/JMSDd/JMSDd-daemon.jar
		// User=peter
		// Restart=on-abort
		//
		// [Install]
		// WantedBy=multi-user.target

		p.println("[Unit]");
		p.println("Description=JavaMinecraftSystemDdaemon");
		p.println();
		p.println("[Service]");
		p.println("Environment=\"JMSDdWkDir= " + dir + "\"");
		p.println("ExecStart=/usr/bin/java -Djava.system.class.loader=net.petercashel.jmsDd.ASMClassLoader -jar " + dir + "/JMSDd-daemon.jar");
		if (!rootInstall)
			p.println("User=" + username);
		p.println("Restart=on-abort");
		p.println();
		p.println("[Install]");
		p.println("WantedBy=multi-user.target");

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

		String path = installMain.class.getProtectionDomain().getCodeSource()
				.getLocation().getPath();
		String decodedPath = null;
		try {
			decodedPath = URLDecoder.decode(path, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		File jar = new File(decodedPath);
		File target = new File(installDir, "JMSDd-daemon.jar");
		try {
			Files.copy(jar.toPath(), target.toPath(),
					StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		FileOutputStream o1 = null;
		File client = new File(usrsbin, "JMSDc");
		try {
			o1 = new FileOutputStream(client, true);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		PrintStream p1 = new PrintStream(o1, true);

		p1.println("#!/bin/bash");
		p1.println("java -cp " + target.toPath().toString()
				+ " net.petercashel.jmsDc.clientMain");
		p1.println();

		p1.flush();
		p1.close();
		try {
			o1.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			o1.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		client.setExecutable(true);
		libc.chmod(client.toPath().toString(), 0777);

		ProcessBuilder pb1 = new ProcessBuilder("systemctl", "enable",
				"JMSDd.service");
		pb1.redirectOutput(Redirect.PIPE);
		Process ps1 = null;
		try {
			ps1 = pb1.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			ps1.waitFor();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ProcessBuilder pb2 = new ProcessBuilder("systemctl", "start",
				"JMSDd.service");
		pb2.redirectOutput(Redirect.PIPE);
		Process ps2 = null;
		try {
			ps2 = pb1.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			ps2.waitFor();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Path p11 = installDir.toPath();
		FileVisitor<Path> fv = new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file,
					BasicFileAttributes attrs) throws IOException {
				System.out.println(file);
				libc.chmod(file.toString(), 0777);
				return FileVisitResult.CONTINUE;
			}
		};

		try {
			Files.walkFileTree(p11, fv);
		} catch (IOException e) {
			e.printStackTrace();
		}

		out.println("Installation Complete!");
	}

	private static CLibrary libc = (CLibrary) Native.loadLibrary("c",
			CLibrary.class);

	interface CLibrary extends Library {
		public int chmod(String path, int mode);
	}
}
