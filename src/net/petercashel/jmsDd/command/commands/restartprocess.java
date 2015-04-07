package net.petercashel.jmsDd.command.commands;

import net.petercashel.jmsDd.daemonMain;
import net.petercashel.jmsDd.command.ICommand;
import net.petercashel.jmsDd.command.commandServer;
import net.petercashel.nettyCore.server.serverCore;

public class restartprocess implements ICommand {

	@Override
	public String commandName() {
		// TODO Auto-generated method stub
		return "restartprocess";
	}

	@Override
	public boolean processCommand(String[] args) {
		daemonMain.RestartProcess();
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
