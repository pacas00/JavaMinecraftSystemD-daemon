package net.petercashel.jmsDd;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DepLoader {
	
	public static File configDir = new File(System.getProperty("user.home")
			+ File.separator + ".JMSDd" + File.separator);
	static String jcabi_aether_url = "https://repo1.maven.org/maven2/com/jcabi/jcabi-aether/0.10.1/jcabi-aether-0.10.1-jar-with-dependencies.jar";
	static String maven305_url = "http://mirror.ventraip.net.au/apache/maven/maven-3/3.0.5/binaries/apache-maven-3.0.5-bin.zip";
	static File libs = new File(configDir, "./libs/");
	static File jcabi_aether = null;
	static File maven305 = null;
	
	public static void main(String[] args) throws IOException {
		System.out.println("Downloading Core Libraries");
		Map<String, String> env = System.getenv();
		configDir.mkdirs();
		if (env.containsKey("JMSDdWkDir")) {
			libs = new File(env.get("JMSDdWkDir"));
		} else { libs = new File(configDir, "./libs/");
		}
		libs.mkdirs();
		
		maven305 = new File(libs, "maven305.zip");
		if (maven305.exists()) { } else {
			try {
				System.out.println("Downloading: Maven 3.0.5");
				URL Json = new URL(maven305_url);
				ReadableByteChannel rbc = Channels.newChannel(Json.openStream());
				FileOutputStream fos;
				fos = new FileOutputStream(maven305);
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			} catch (FileNotFoundException e) {
				throw e;
			} catch (IOException e) {
				throw e;
			}
			}
		try {
			//Unzip
			File mvndir = new File(libs, "mvn");
			unzip(maven305, mvndir.toPath().toString());
			//add each file to classpath
			listFilesForFolder(mvndir);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw e;
			
		}
		
		jcabi_aether = new File(libs, "jcabiaether.jar");
		if (jcabi_aether.exists()) { } else {
			try {
				System.out.println("Downloading: Jcabi Aether");
				URL Json = new URL(jcabi_aether_url);
				ReadableByteChannel rbc = Channels.newChannel(Json.openStream());
				FileOutputStream fos;
				fos = new FileOutputStream(jcabi_aether);
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			} catch (FileNotFoundException e) {
				throw e;
			} catch (IOException e) {
				throw e;
			}
			}
		try {
			addFile(jcabi_aether);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw e;
			
		}

		DepLoaderStage2.run(libs);		
		System.out.println("Loaded Core Libraries");
		daemonMain.main(args);
	}
	
	public static void listFilesForFolder(final File folder) {
	    for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	            listFilesForFolder(fileEntry);
	        } else {
	        	try {
					addFile(fileEntry);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        }
	    }
	}

	private static void unzip(File zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
            	extractZippedFile(zipIn, filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                dir.mkdir();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }	
	
	private static void extractZippedFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[4096];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

	///////////////////////////////////////////////////////////////////////////////////////////////
	// Based on a Stack Overflow answer. For dynamically loading classes.
	// http://stackoverflow.com/a/60766 - Credit to http://stackoverflow.com/users/2443/allain-lalonde
	///////////////////////////////////////////////////////////////////////////////////////////////
	//

	private static final Class[] parameters = new Class[] {URL.class};

	public static void addFile(String s) throws IOException
	{
		File f = new File(s);
		addFile(f);
	}

	public static void addFile(File f) throws IOException
	{
		addURL(f.toURI().toURL());
	}

	public static void addURL(URL u) throws IOException
	{
		URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		Class sysclass = URLClassLoader.class;

		try {
			Method method = sysclass.getDeclaredMethod("addURL", parameters);
			method.setAccessible(true);
			method.invoke(sysloader, new Object[] {u});
		} catch (Throwable t) {
			t.printStackTrace();
			throw new IOException("Error, could not add URL to system classloader");
		}

	}

	//
	///////////////////////////////////////////////////////////////////////////////////////////////
}
