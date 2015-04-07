package net.petercashel.jmsDd.command;

public interface ICommand {
	
	public String commandName();
	public boolean processCommand(String[] args);
	public int requiredPermissionLevel();
	public void RegisterMe();

}
