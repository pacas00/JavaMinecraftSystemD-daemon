package net.petercashel.jmsDd;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class DepLoader {

	static String jcabi_aether_url = "https://repo1.maven.org/maven2/com/jcabi/jcabi-aether/0.10.1/jcabi-aether-0.10.1-jar-with-dependencies.jar";
	static File libs = new File("./libs/");
	static File jcabi_aether = null;
	
	public static void main(String[] args) throws IOException {
		System.out.println("Downloading Core Libraries");
		Map<String, String> env = System.getenv();
		if (env.containsKey("JMSDdWkDir")) {
			libs = new File(env.get("JMSDdWkDir"));
		}		
		libs.mkdirs();
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
		Jcabi_Aether();
		System.out.println("Loaded Core Libraries");
	}


	public static void Jcabi_Aether() {
		File local = libs;
		Collection<org.sonatype.aether.repository.RemoteRepository> remotes = Arrays.asList(
				new org.sonatype.aether.repository.RemoteRepository(
						"maven-central",
						"default",
						"http://repo1.maven.org/maven2/"
						),
				new org.sonatype.aether.repository.RemoteRepository(
						"htb3",
						"default",
						"http://htb2.petercashel.net:81"	        
						)      
				);
		try {
			com.jcabi.aether.Aether resolver = new com.jcabi.aether.Aether(remotes, local);
			Collection<org.sonatype.aether.artifact.Artifact> deps = resolver.resolve(
					new org.sonatype.aether.util.artifact.DefaultArtifact("net.petercashel.JMSDd", "JMSDd-Common", "", "jar", "0.5.0-SNAPSHOT"),
					"runtime"
					);
			deps.addAll(resolver.resolve(new org.sonatype.aether.util.artifact.DefaultArtifact("net.petercashel.JMSDd", "JMSDd-client", "", "jar", "0.5.0-SNAPSHOT"),
					"runtime"));
			deps.addAll(resolver.resolve(new org.sonatype.aether.util.artifact.DefaultArtifact("com.google.code.gson", "gson", "", "jar", "2.3.1"),
					"runtime"));
			deps.addAll(resolver.resolve(new org.sonatype.aether.util.artifact.DefaultArtifact("net.java.dev.jna", "jna", "", "jar", "4.1.0"),
					"runtime"));
			deps.addAll(resolver.resolve(new org.sonatype.aether.util.artifact.DefaultArtifact("net.java.dev.jna", "jna-platform", "", "jar", "4.1.0"),
					"runtime"));
			for (org.sonatype.aether.artifact.Artifact a : deps) {
				try {
					addFile(a.getFile());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (org.sonatype.aether.resolution.DependencyResolutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
