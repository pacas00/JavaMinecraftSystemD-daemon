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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;
import net.petercashel.commonlib.threading.threadManager;
import net.petercashel.jmsDd.Configuration;
import net.petercashel.jmsDd.daemonMain;
import net.petercashel.jmsDd.command.commandServer;
import static net.petercashel.jmsDd.command.commandServer.out;

public class PrintStreamHandler {

	static PrintStream SystemOut = null;
	static PrintStream SystemErr = null;

	static PrintStream WrappedSystemOut = null;
	static PrintStream WrappedSystemErr = null;

	static PrintStream logger = null;

	public static void run() {
		SystemOut = System.out;
		SystemErr = System.err;
		String date = daemonMain.logDateFormat.format(Calendar.getInstance().getTime());
		File logDir = new File(Configuration.configDir, "logs");
		if (!logDir.exists()) logDir.mkdir();
		File logfile = new File(logDir, "JMSDd-" + date + ".log");
		logfile.delete();
		try {
			logfile.createNewFile();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			logger = new PrintStream(new FileOutputStream(logfile, true));
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		WrappedSystemOut = new PrintStreamWrapper(SystemOut, "[OUT]");
		WrappedSystemErr = new PrintStreamWrapper(SystemErr, "[ERR]");
		System.setOut(WrappedSystemOut);
		System.setErr(WrappedSystemErr);
	}
	public static void logRotate() {
		logger.flush();
		logger.close();
		String date = daemonMain.logDateFormat.format(Calendar.getInstance().getTime());
		File logDir = new File(Configuration.configDir, "logs");
		if (!logDir.exists()) logDir.mkdir();
		File logfile = new File(logDir, "JMSDd-" + date + ".log");;
		logfile.delete();
		try {
			logfile.createNewFile();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			logger = new PrintStream(new FileOutputStream(logfile, true));
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	static class PrintStreamWrapper extends PrintStream {

		PrintStream outputStream;
		String prefix = "";

		public PrintStreamWrapper(PrintStream out) {
			this(out, false);
			outputStream = out;
		}

		public PrintStreamWrapper(PrintStream out, boolean autoFlush) {
			super(out, autoFlush);
			outputStream = out;
		}

		public PrintStreamWrapper(PrintStream out, String pfix) {
			this(out, false);
			outputStream = out;
			prefix = pfix;
		}

		public PrintStreamWrapper(PrintStream out, boolean autoFlush, String pfix) {
			super(out, autoFlush);
			outputStream = out;
			prefix = pfix;
		}

		public void println(String x) {
			synchronized (this) {
				outputStream.println(x);
				outputStream.flush();
				logger.print("[ " + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())) + " ] " + prefix
						+ " ");
				logger.println(x);
			}
		}

		public void println() {
			outputStream.println();
			outputStream.flush();
			logger.println();
		}

		public void print(String s) {
			outputStream.print(s);
			outputStream.flush();
			logger.print("[ " + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())) + " ] " + prefix + " ");
			logger.print(s);
			logger.println();
		}

	}

}
