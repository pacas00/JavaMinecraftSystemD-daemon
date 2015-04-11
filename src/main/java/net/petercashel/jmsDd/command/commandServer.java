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

package net.petercashel.jmsDd.command;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import net.petercashel.commonlib.threading.threadManager;
import net.petercashel.jmsDd.daemonMain;
import net.petercashel.jmsDd.command.commands.help;
import net.petercashel.nettyCore.common.PacketRegistry;
import net.petercashel.nettyCore.common.packets.IOOutPacket;
import net.petercashel.nettyCore.server.serverCore;
import net.petercashel.nettyCore.serverUDS.serverCoreUDS;
import sun.net.www.protocol.file.FileURLConnection;

public class commandServer {

	static HashMap<String, Class<? extends ICommand>> map = new HashMap<String, Class<? extends ICommand>>();

	public static int historylimit = 80;
	public static final LinkedBlockingQueue<String> history = new LinkedBlockingQueue<String>(
			historylimit + 5);
	public static OutputStream Progin = null;

	static PipedOutputStream pipeout = new PipedOutputStream();
	static PipedInputStream pipein = null;
	public static PrintStream out = null;

	public static void init() {
		registerCommands();
		try {
			out = new PrintStream(pipeout, false, "ASCII");
		} catch (UnsupportedEncodingException e) {
			out = new PrintStream(pipeout, false);
		}
		try {
			pipein = new PipedInputStream(pipeout);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		threadManager.getInstance().addRunnable(new Runnable() {
			@Override
			public void run() {
				Scanner sc = null;
				while (daemonMain.run) {
					sc = new Scanner((pipein));
					while (sc.hasNextLine()) {
						String s = sc.nextLine();
						// Add to queue here
						history.offer(s);
						if (history.size() > historylimit)
							history.poll();
						byte[] b = null;
						try {
							b = s.getBytes("ASCII");
						} catch (UnsupportedEncodingException e) {
							b = s.getBytes();
						}
						if (b.length > 0) {
							for (Channel c : serverCore.clientConnectionMap
									.values()) {
								(PacketRegistry.pack(new IOOutPacket(b.length,
										b))).sendPacket(c);
							}
							if (serverCoreUDS.alive) {
								for (Channel c : serverCoreUDS.clientConnectionMap
										.values()) {
									(PacketRegistry.pack(new IOOutPacket(
											b.length, b))).sendPacket(c);
								}
							}
						}
					}

				}
				sc.close();
			}
		});
	}

	public static void sendHistory(ChannelHandlerContext ctx) {
		if (history.size() > 0) {
			Object[] str = null;
			str = history.toArray();
			for (Object o : str) {
				try {
					String s = (String) o;
					byte[] b = null;
					try {
						b = s.getBytes("ASCII");
					} catch (UnsupportedEncodingException e) {
						b = s.getBytes();
					}
					if (b.length > 0) {
						(PacketRegistry.pack(new IOOutPacket(b.length, b)))
								.sendPacket(ctx);
					}
				} catch (Exception e) {
				}
			}
		}

	}

	public static void registerCommand(Class<? extends ICommand> com) {
		try {
			map.put(com.newInstance().commandName(), com);
			help.helpList.add(com.newInstance().commandName());
			System.out.println("Registered Command: ."
					+ com.newInstance().commandName());
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public static void registerCommands() {
		ArrayList<Class<?>> cls = null;
		try {
			cls = getClassesForPackage("net.petercashel.jmsDd.command.commands");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (Class<?> clz : cls) {
			ICommand com = null;
			try {
				com = (ICommand) clz.newInstance();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			com.RegisterMe();
		}
	}

	/**
	 * Private helper method
	 * 
	 * @param directory
	 *            The directory to start with
	 * @param pckgname
	 *            The package name to search for. Will be needed for getting the
	 *            Class object.
	 * @param classes
	 *            if a file isn't loaded but still is in the directory
	 * @throws ClassNotFoundException
	 */
	private static void checkDirectory(File directory, String pckgname,
			ArrayList<Class<?>> classes) throws ClassNotFoundException {
		File tmpDirectory;

		if (directory.exists() && directory.isDirectory()) {
			final String[] files = directory.list();

			for (final String file : files) {
				if (file.endsWith(".class")) {
					try {
						classes.add(Class.forName(pckgname + '.'
								+ file.substring(0, file.length() - 6)));
					} catch (final NoClassDefFoundError e) {
						// do nothing. this class hasn't been found by the
						// loader, and we don't care.
					}
				} else if ((tmpDirectory = new File(directory, file))
						.isDirectory()) {
					checkDirectory(tmpDirectory, pckgname + "." + file, classes);
				}
			}
		}
	}

	/**
	 * Private helper method.
	 * 
	 * @param connection
	 *            the connection to the jar
	 * @param pckgname
	 *            the package name to search for
	 * @param classes
	 *            the current ArrayList of all classes. This method will simply
	 *            add new classes.
	 * @throws ClassNotFoundException
	 *             if a file isn't loaded but still is in the jar file
	 * @throws IOException
	 *             if it can't correctly read from the jar file.
	 */
	private static void checkJarFile(JarURLConnection connection,
			String pckgname, ArrayList<Class<?>> classes)
			throws ClassNotFoundException, IOException {
		final JarFile jarFile = connection.getJarFile();
		final Enumeration<JarEntry> entries = jarFile.entries();
		String name;

		for (JarEntry jarEntry = null; entries.hasMoreElements()
				&& ((jarEntry = entries.nextElement()) != null);) {
			name = jarEntry.getName();

			if (name.contains(".class")) {
				name = name.substring(0, name.length() - 6).replace('/', '.');

				if (name.contains(pckgname)) {
					classes.add(Class.forName(name));
				}
			}
		}
	}

	/**
	 * Attempts to list all the classes in the specified package as determined
	 * by the context class loader
	 * 
	 * @param pckgname
	 *            the package name to search
	 * @return a list of classes that exist within that package
	 * @throws ClassNotFoundException
	 *             if something went wrong
	 */
	public static ArrayList<Class<?>> getClassesForPackage(String pckgname)
			throws ClassNotFoundException {
		final ArrayList<Class<?>> classes = new ArrayList<Class<?>>();

		try {
			final ClassLoader cld = Thread.currentThread()
					.getContextClassLoader();

			if (cld == null)
				throw new ClassNotFoundException("Can't get class loader.");

			final Enumeration<URL> resources = cld.getResources(pckgname
					.replace('.', '/'));
			URLConnection connection;

			for (URL url = null; resources.hasMoreElements()
					&& ((url = resources.nextElement()) != null);) {
				try {
					connection = url.openConnection();

					if (connection instanceof JarURLConnection) {
						checkJarFile((JarURLConnection) connection, pckgname,
								classes);
					} else if (connection instanceof FileURLConnection) {
						try {
							checkDirectory(
									new File(URLDecoder.decode(url.getPath(),
											"UTF-8")), pckgname, classes);
						} catch (final UnsupportedEncodingException ex) {
							throw new ClassNotFoundException(
									pckgname
											+ " does not appear to be a valid package (Unsupported encoding)",
									ex);
						}
					} else
						throw new ClassNotFoundException(pckgname + " ("
								+ url.getPath()
								+ ") does not appear to be a valid package");
				} catch (final IOException ioex) {
					throw new ClassNotFoundException(
							"IOException was thrown when trying to get all resources for "
									+ pckgname, ioex);
				}
			}
		} catch (final NullPointerException ex) {
			throw new ClassNotFoundException(
					pckgname
							+ " does not appear to be a valid package (Null pointer exception)",
					ex);
		} catch (final IOException ioex) {
			throw new ClassNotFoundException(
					"IOException was thrown when trying to get all resources for "
							+ pckgname, ioex);
		}

		return classes;
	}

	public static void processCommand(String s) {
		String[] args = s.split(" ");
		if (map.containsKey(args[0])) {
			ICommand c = null;
			try {
				c = map.get(args[0]).newInstance();
				try {
					c.processCommand(args);
				} catch (NullPointerException e) {
					out.println("COMMAND ERROR");
					e.printStackTrace(out);
					e.printStackTrace();
				}
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				out.println("COMMAND ERROR");
				e.printStackTrace(out);
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				out.println("COMMAND ERROR");
				e.printStackTrace(out);
			}
		} else {
			out.println("INVALID COMMAND");
			out.flush();
		}
	}

}
