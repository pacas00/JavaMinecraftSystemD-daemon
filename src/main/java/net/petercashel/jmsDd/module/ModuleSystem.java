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

package net.petercashel.jmsDd.module;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.petercashel.jmsDd.Configuration;
import net.petercashel.jmsDd.DepLoader;
import net.petercashel.jmsDd.daemonMain;
import net.petercashel.jmsDd.event.module.DummyEvent;
import net.petercashel.jmsDd.event.module.EventBase;
import net.petercashel.jmsDd.util.CustomURLClassLoader;
import sun.net.www.protocol.file.FileURLConnection;

public class ModuleSystem {

	public static HashMap<String, String> modulesToLoad = null;

	public static void loadAllModuleJars() {
		modulesToLoad = new HashMap<String, String>();
		File modulesDir = new File(Configuration.configDir, "modules");
		modulesDir.mkdir();
		WalkFolder(modulesDir);

	}

	public static void WalkFolder(final File folder) {
		for (final File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				WalkFolder(fileEntry);
			} else {
				if (fileEntry.getName().toLowerCase().endsWith("jar")) {
					LoadModule(fileEntry);
				}
			}
		}
	}

	public static void LoadModule(File jar) {
		List<String> classNames = new ArrayList<String>();

		URL[] u = new URL[1];
		try {
			u[0] = jar.toURI().toURL();
		}
		catch (java.net.MalformedURLException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		CustomURLClassLoader CustomCL = new CustomURLClassLoader(u);
		System.out.println("Inspecting Jar for modules: " + jar.getName());
		try {
			ZipInputStream zip = new ZipInputStream(new FileInputStream(jar));
			for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry())
				if (entry.getName().endsWith(".class") && !entry.isDirectory()) {
					// This ZipEntry represents a class. Now, what class does it represent?
					StringBuilder className = new StringBuilder();
					for (String part : entry.getName().split("/")) {
						if (className.length() != 0) className.append(".");
						className.append(part);
						if (part.endsWith(".class")) className.setLength(className.length() - ".class".length());
					}
					classNames.add(className.toString());
				}
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			DepLoader.addFile(jar);
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (classNames.size() > 0) {
			for (String s : classNames) {
				try {
					CustomCL.findClass(s);
				}
				catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}

		


	}
	
	public static void LoadFoundModules() {
		// Process modules to load and null
				for (Entry<String, String> ent : modulesToLoad.entrySet()) {
					System.out.println("Initalising Module: " + ent.getKey());
					Class cls = null;
					try {
						cls = Class.forName(ent.getValue());
					}
					catch (ClassNotFoundException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					Constructor<?> cons = null;
					try {
						cons = cls.getConstructor(DummyEvent.class);
					}
					catch (NoSuchMethodException | SecurityException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					try {
						cons.newInstance(new DummyEvent());
					}
					catch (InstantiationException | IllegalAccessException | IllegalArgumentException
							| InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
	}
}
