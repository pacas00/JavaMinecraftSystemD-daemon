package net.petercashel.jmsDd.API;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;

import net.petercashel.jmsDd.command.ICommand;

import com.google.common.eventbus.EventBus;


public interface API {

	PrintStream OutputToClient();

	OutputStream InputToProcess();

	void LoadJar(File f);

	void registerCommand(Class<? extends ICommand> com);
	
	EventBus getEventBus();

	void registerEventBus(Object handler);
	void PostEvent(Object event);

	boolean HasUser(String u);

	int UserPermissionLevel(String u);

	boolean IsSaltedTokenValid(String user, String SaltToken);

	public static class Impl {
		public static API api = null;
		public static API getAPI() {
			return api;
		}
	}

	
}
