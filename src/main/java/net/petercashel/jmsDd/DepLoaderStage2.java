package net.petercashel.jmsDd;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

public class DepLoaderStage2 {

	public static void run(File local) {
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
					new org.sonatype.aether.util.artifact.DefaultArtifact("net.petercashel.JMSDd", "JMSDd-Common", "", "jar", "0.5.0"),
					"runtime"
					);
			deps.addAll(resolver.resolve(new org.sonatype.aether.util.artifact.DefaultArtifact("net.petercashel.JMSDd", "JMSDd-client", "", "jar", "0.5.0"),
					"runtime"));
			deps.addAll(resolver.resolve(new org.sonatype.aether.util.artifact.DefaultArtifact("com.google.code.gson", "gson", "", "jar", "2.3.1"),
					"runtime"));
			deps.addAll(resolver.resolve(new org.sonatype.aether.util.artifact.DefaultArtifact("net.java.dev.jna", "jna", "", "jar", "4.1.0"),
					"runtime"));
			deps.addAll(resolver.resolve(new org.sonatype.aether.util.artifact.DefaultArtifact("net.java.dev.jna", "jna-platform", "", "jar", "4.1.0"),
					"runtime"));
			
			for (org.sonatype.aether.artifact.Artifact a : deps) {
				try {
					DepLoader.addFile(a.getFile());
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

}
