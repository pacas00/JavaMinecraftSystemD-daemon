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

import net.petercashel.jmsDd.command.ICommand;
import net.petercashel.jmsDd.command.commandServer;

public class printargs implements ICommand {

	@Override
	public String commandName() {
		// TODO Auto-generated method stub
		return "printargs";
	}

	@Override
	public boolean processCommand(String[] args) {
		commandServer.out.println("Printing Args.");
		if (args.length == 0) {
		} else if (args.length == 1) {
			commandServer.out.print(args[0]);
		} else {
			int i = 0;
			for (String s : args) {
				commandServer.out.println(i + ": " + s);
				i++;
			}
		}
		commandServer.out.println("Args Printed.");
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
