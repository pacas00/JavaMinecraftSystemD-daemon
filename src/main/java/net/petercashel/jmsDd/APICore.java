package net.petercashel.jmsDd;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;

import com.google.common.eventbus.EventBus;

import net.petercashel.jmsDd.API.API;
import net.petercashel.jmsDd.auth.AuthSystem;
import net.petercashel.jmsDd.command.ICommand;
import net.petercashel.jmsDd.command.commandServer;
import net.petercashel.jmsDd.module.ModuleSystem;

public class APICore implements API {

	@Override
	public PrintStream OutputToClient() {
		return commandServer.out;
	}

	@Override
	public OutputStream InputToProcess() {
		return commandServer.Progin;
	}

	@Override
	public void LoadJar(File f) {
		ModuleSystem.LoadModule(f);
	}


	@Override
	public void registerCommand(Class<? extends ICommand> com) {
		commandServer.registerCommand(com);
	}

	@Override
	public EventBus getEventBus() {
		return daemonMain.eventBus;
	}

	@Override
	public void registerEventBus(Object handler) {
		getEventBus().register(handler);		
	}

	@Override
	public void PostEvent(Object event) {
		getEventBus().post(event);		
	}

	@Override
	public boolean HasUser(String u) {
		return AuthSystem.backend.HasUser(u);
	}

	@Override
	public int UserPermissionLevel(String u) {
		return AuthSystem.backend.GetPermissionLevel(u);
	}

	@Override
	public boolean IsSaltedTokenValid(String user, String SaltToken) {
		return SaltToken == AuthSystem.backend.GetSaltedToken(user);
	}
}
