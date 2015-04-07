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
