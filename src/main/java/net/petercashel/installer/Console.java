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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class Console {
	BufferedReader br;
	PrintStream ps;

	public Console() {
		br = new BufferedReader(new InputStreamReader(System.in));
		ps = System.out;
	}

	public String readLine(String out) {
		ps.format(out);
		try {
			return br.readLine();
		} catch (IOException e) {
			return null;
		}
	}

	public PrintStream format(String format, Object... objects) {
		return ps.format(format, objects);
	}

	public void print(String s) {
		ps.print(s);
	}

	public void println(String s) {
		ps.println(s);
	}

}
