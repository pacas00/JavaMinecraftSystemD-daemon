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
import net.petercashel.jmsDd.auth.AuthSystem;
import net.petercashel.jmsDd.auth.interfaces.IAuthDataSystem.permissionLevels;
import net.petercashel.jmsDd.command.ICommand;
import net.petercashel.jmsDd.command.commandServer;
import net.petercashel.nettyCore.server.serverCore;

public class user implements ICommand {

	@Override
	public String commandName() {
		// TODO Auto-generated method stub
		return "user";
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
		if (args[1].equalsIgnoreCase("getperms")) {
			getperms(args);
			return true;
		}
		if (args[1].equalsIgnoreCase("setperms")) {
			setperms(args);
			return true;
		}
		if (args[1].equalsIgnoreCase("adduser")) {
			adduser(args);
			return true;
		}
		if (args[1].equalsIgnoreCase("deluser")) {
			deluser(args);
			return true;
		}
		if (args[1].equalsIgnoreCase("resettoken")) {
			resettoken(args);
			return true;
		}
		if (args[1].equalsIgnoreCase("resettokensalt")) {
			resettokensalt(args);
			return true;
		}
		return false;
	}

	private void getperms(String[] args) {
		if (args.length < 3) {
			commandServer.out.println("Missing Arguments.");
			commandServer.out.println(".user getperms USERNAME");
			return;
		}
		int i = AuthSystem.backend.GetPermissionLevel(args[2]);
		commandServer.out.println("perm level on user " + args[2] + " is " + i);

	}

	private void setperms(String[] args) {
		if (args.length < 4) {
			commandServer.out.println("Missing Arguments.");
			commandServer.out.println(".user setperms USERNAME 0-4");
			return;
		}
		AuthSystem.backend.SetPermissionLevel(args[2], Integer.parseInt(args[3]));
		commandServer.out.println("Set perm level on user " + args[2]);

	}

	private void resettokensalt(String[] args) {
		if (args.length < 3) {
			commandServer.out.println("Missing Arguments.");
			commandServer.out.println(".user resettokensalt USERNAME");
			return;
		}
		AuthSystem.backend.ResetTokenSalt(args[2]);
		commandServer.out.println("TokenSalt reset for user " + args[2]);
	}

	private void resettoken(String[] args) {
		if (args.length < 3) {
			commandServer.out.println("Missing Arguments.");
			commandServer.out.println(".user resettoken USERNAME");
			return;
		}
		AuthSystem.backend.ResetToken(args[2]);
		commandServer.out.println("Token reset for user " + args[2]);
	}

	private void deluser(String[] args) {
		if (args.length < 3) {
			commandServer.out.println("Missing Arguments.");
			commandServer.out.println(".user deluser USERNAME");
			return;
		}
		if (args.length < 4) {
			commandServer.out.println("Are you Sure?");
			commandServer.out.println(".user deluser USERNAME (y/n)");
			return;
		}
		if (args[3].equalsIgnoreCase("y")) {
			AuthSystem.backend.DelUser(args[2]);
			commandServer.out.println("Deleted user " + args[2]);
		}

	}

	private void adduser(String[] args) {
		if (args.length < 3) {
			commandServer.out.println("Missing Arguments.");
			commandServer.out.println(".user adduser USERNAME");
			return;
		}
		try {
			AuthSystem.backend.AddUser(args[2]);
			commandServer.out.println("User " + args[2] + " Added!");
		}
		catch (Exception e) {
			commandServer.out.println("User " + args[2] + " Already Exists!");
		}

	}

	private void help() {
		commandServer.out.println("help,adduser,deluser,resettoken,resettokensalt,getperms,setperms");
	}

	@Override
	public permissionLevels requiredPermissionLevel() {
		return permissionLevels.ADMINISTRATOR;
	}

	@Override
	public void RegisterMe() {
		commandServer.registerCommand(this.getClass());
	}

}
