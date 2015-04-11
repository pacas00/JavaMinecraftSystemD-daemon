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

import net.petercashel.jmsDd.daemonMain;
import net.petercashel.jmsDd.command.ICommand;
import net.petercashel.jmsDd.command.commandServer;
import net.petercashel.nettyCore.server.serverCore;

public class server implements ICommand {

	@Override
	public String commandName() {
		// TODO Auto-generated method stub
		return "server";
	}

	@Override
	public boolean processCommand(String[] args) {
		if (args.length < 2) {
			commandServer.out.println("Invalid Arguments.");
			return true;
		}
		if (args[1].equalsIgnoreCase("help")) {
			help();
			return true;
		}
		if (args[1].equalsIgnoreCase("shutdown")) {
			commandServer.out.println("Server is Shutting Down");
			daemonMain.shutdown();
			return true;
		}
		return false;
	}

	private void help() {
		commandServer.out.println("help,shutdown");
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
