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

package net.petercashel.jmsDd.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;
import sun.misc.Resource;
import sun.misc.URLClassPath;

public class CustomURLClassLoader extends URLClassLoader {
	/* The search path for classes and resources */
	private final URLClassPath ucp;
	private Map<String, Class> loadedClasses = new HashMap<String, Class>();

	/* The context to be used when loading classes and resources */
	private final AccessControlContext acc;

	public CustomURLClassLoader(URL[] urls) {
		super(urls);
		// this is to make the stack depth consistent with 1.1
		SecurityManager security = System.getSecurityManager();
		if (security != null) {
			security.checkCreateClassLoader();
		}
		ucp = new URLClassPath(urls);
		this.acc = AccessController.getContext();
	}

	public CustomURLClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
		// this is to make the stack depth consistent with 1.1
		SecurityManager security = System.getSecurityManager();
		if (security != null) {
			security.checkCreateClassLoader();
		}
		ucp = new URLClassPath(urls);
		this.acc = AccessController.getContext();
	}

	public CustomURLClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
		super(urls, parent, factory);
		// this is to make the stack depth consistent with 1.1
		SecurityManager security = System.getSecurityManager();
		if (security != null) {
			security.checkCreateClassLoader();
		}
		ucp = new URLClassPath(urls, factory);
		acc = AccessController.getContext();
	}

	@Override
	public Class<?> findClass(final String name) throws ClassNotFoundException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<Class>() {
				public Class run() throws ClassNotFoundException {
					String path = name.replace('.', '/').concat(".class");
					Resource res = ucp.getResource(path, false);
					if (res != null) {
						if (loadedClasses.containsKey(name)) {
							try {
								return loadedClasses.get(name);
							}
							catch (Exception e) {
								try {
									return defineClass(name, res);
								}
								catch (IOException e2) {
									throw new ClassNotFoundException(name, e);
								}
							}
						} else {
							try {
								Class clazz = ASMClass(name, res);
								loadedClasses.put(name, clazz);
								return clazz;
							}
							catch (IOException e) {
								throw new ClassNotFoundException(name, e);
							}
						}
					} else {
						throw new ClassNotFoundException(name);
					}
				}
			}, acc);
		}
		catch (java.security.PrivilegedActionException pae) {
			throw (ClassNotFoundException) pae.getException();
		}
	}

	/*
	 * Defines a Class using the class bytes obtained from the specified Resource. The resulting Class must be resolved
	 * before it can be used.
	 */
	private Class defineClass(String name, Resource res) throws IOException {
		long t0 = System.nanoTime();
		int i = name.lastIndexOf('.');
		URL url = res.getCodeSourceURL();
		if (i != -1) {
			String pkgname = name.substring(0, i);
			// Check if package already loaded.
			Manifest man = res.getManifest();
			if (getAndVerifyPackage(pkgname, man, url) == null) {
				try {
					if (man != null) {
						definePackage(pkgname, man, url);
					} else {
						definePackage(pkgname, null, null, null, null, null, null, null);
					}
				}
				catch (IllegalArgumentException iae) {
					// parallel-capable class loaders: re-verify in case of a
					// race condition
					if (getAndVerifyPackage(pkgname, man, url) == null) {
						// Should never happen
						throw new AssertionError("Cannot find package " + pkgname);
					}
				}
			}
		}
		// Now read the class bytes and define the class
		java.nio.ByteBuffer bb = res.getByteBuffer();
		if (bb != null) {
			// Use (direct) ByteBuffer:
			CodeSigner[] signers = res.getCodeSigners();
			CodeSource cs = new CodeSource(url, signers);
			sun.misc.PerfCounter.getReadClassBytesTime().addElapsedTimeFrom(t0);
			return defineClass(name, bb, cs);
		} else {
			byte[] b = res.getBytes();
			// must read certificates AFTER reading bytes.
			CodeSigner[] signers = res.getCodeSigners();
			CodeSource cs = new CodeSource(url, signers);
			sun.misc.PerfCounter.getReadClassBytesTime().addElapsedTimeFrom(t0);
			return defineClass(name, b, 0, b.length, cs);
		}

	}

	private Package getAndVerifyPackage(String pkgname, Manifest man, URL url) {
		Package pkg = getPackage(pkgname);
		if (pkg != null) {
			// Package found, so check package sealing.
			if (pkg.isSealed()) {
				// Verify that code source URL is the same.
				if (!pkg.isSealed(url)) { throw new SecurityException("sealing violation: package " + pkgname
						+ " is sealed"); }
			} else {
				// Make sure we are not attempting to seal the package
				// at this code source URL.
				if ((man != null) && isSealed(pkgname, man)) { throw new SecurityException(
						"sealing violation: can't seal package " + pkgname + ": already loaded"); }
			}
		}
		return pkg;
	}

	private boolean isSealed(String name, Manifest man) {
		String path = name.replace('.', '/').concat("/");
		Attributes attr = man.getAttributes(path);
		String sealed = null;
		if (attr != null) {
			sealed = attr.getValue(Name.SEALED);
		}
		if (sealed == null) {
			if ((attr = man.getMainAttributes()) != null) {
				sealed = attr.getValue(Name.SEALED);
			}
		}
		return "true".equalsIgnoreCase(sealed);

	}

	public void addURL(URL url) {
		ucp.addURL(url);
	}

	private Class ASMClass(String name, Resource res) throws IOException {
		// override classDefine (as it is protected) and define the class.
		Class clazz = null;

		ClassLoader loader = ClassLoader.getSystemClassLoader();
		try {
			Class jFXApp = Class.forName("net.petercashel.client.launcher");
			loader = Thread.currentThread().getContextClassLoader();
		}
		catch (ClassNotFoundException e1) {
		}

		try {
			Class cls = Class.forName("java.lang.ClassLoader");
			java.lang.reflect.Method method;
			method = cls.getDeclaredMethod("defineClass", new Class[] { String.class, byte[].class, int.class,
					int.class });
			byte[] bOrig = res.getBytes();
			byte[] b = ASMTransformer.transform(name, bOrig);
			// protected method invocaton
			method.setAccessible(true);
			try {
				Object[] args = new Object[] { name, b, new Integer(0), new Integer(b.length) };
				clazz = (Class) method.invoke(loader, args);
			}
			catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			}
			catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			}
			catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();

			}
			finally {
				method.setAccessible(false);
			}
		}
		catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
		catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
		catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
		return clazz;
	}
}
