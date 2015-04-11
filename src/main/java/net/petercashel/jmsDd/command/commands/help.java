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

package net.petercashel.jmsDd.command.commands;

import io.netty.util.internal.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.petercashel.jmsDd.command.ICommand;
import net.petercashel.jmsDd.command.commandServer;

public class help implements ICommand {

	public static List<String> helpList = new ArrayList<String>();

	@Override
	public String commandName() {
		// TODO Auto-generated method stub
		return "help";
	}

	@Override
	public boolean processCommand(String[] args) {
		if (helpList.size() < 1)
			commandServer.out
					.println("Help was run but there is no helping you.");
		else {
			Collections.sort(helpList);
			String s = "List of Commands: ";
			for (String str : helpList)
				s = s + str + ", ";
			s = s.substring(0, s.length() - 2);
			commandServer.out.println(s);
		}
		return true;
	}

	@Override
	public int requiredPermissionLevel() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void RegisterMe() {
		commandServer.registerCommand(this.getClass());
	}

}
