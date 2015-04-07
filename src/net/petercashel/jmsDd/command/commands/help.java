package net.petercashel.jmsDd.command.commands;

import net.petercashel.jmsDd.command.ICommand;
import net.petercashel.jmsDd.command.commandServer;

public class help implements ICommand {

	@Override
	public String commandName() {
		// TODO Auto-generated method stub
		return "help";
	}

	@Override
	public boolean processCommand(String[] args) {
		commandServer.out.println("Help was run but there is no helping you.");		
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
