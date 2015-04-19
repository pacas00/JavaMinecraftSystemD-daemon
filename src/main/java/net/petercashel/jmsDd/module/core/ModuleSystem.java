package net.petercashel.jmsDd.module.core;

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
import net.petercashel.jmsDd.CustomURLClassLoader;
import net.petercashel.jmsDd.DepLoader;
import net.petercashel.jmsDd.daemonMain;
import sun.net.www.protocol.file.FileURLConnection;

public class ModuleSystem {
	
	public static HashMap<String, String> modulesToLoad = null;

	public static void loadAllModuleJars() {
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
	            	loadJar(fileEntry);
	            }
	        }
	    }
	}
	
	static void loadJar (File jar) {
		List<String> classNames = new ArrayList<String>();
		
		URL[] u = new URL[1];
		try {
			u[0] = jar.toURI().toURL();
		} catch (java.net.MalformedURLException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}
		CustomURLClassLoader CustomCL = new CustomURLClassLoader(u);
		modulesToLoad = new HashMap<String,String>();
		System.out.println("Inspecting Jar for modules: " + jar.getName());
		    try {
		    	ZipInputStream zip=new ZipInputStream(new FileInputStream(jar));
		    	for(ZipEntry entry=zip.getNextEntry();entry!=null;entry=zip.getNextEntry())
		    	    if(entry.getName().endsWith(".class") && !entry.isDirectory()) {
		    	        // This ZipEntry represents a class. Now, what class does it represent?
		    	        StringBuilder className=new StringBuilder();
		    	        for(String part : entry.getName().split("/")) {
		    	            if(className.length() != 0)
		    	                className.append(".");
		    	            className.append(part);
		    	            if(part.endsWith(".class"))
		    	                className.setLength(className.length()-".class".length());
		    	        }
		    	        classNames.add(className.toString());
		    	    }
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    
		    try {
				DepLoader.addFile(jar);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    
		    if (classNames.size() > 0) {
		    	for (String s : classNames) {
		    		try {
		    			CustomCL.findClass(s);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
		    	}
		    }
		    
		    //Process modules to load and null
		    for (Entry<String, String> ent : modulesToLoad.entrySet()) {
		    	System.out.println("Initalising Module: " + ent.getKey());
		    	Class cls = null;
				try {
					cls = Class.forName(ent.getValue());
				} catch (ClassNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
		    	try {
					cls.newInstance();
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
 		    
		    modulesToLoad = null;

	}
}
