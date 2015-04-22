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

package net.petercashel.jmsDd;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import com.google.common.eventbus.EventBus;
import com.google.gson.JsonObject;
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

	@Override
	public JsonObject getConfigJSONObject(JsonObject e, String name) {
		return Configuration.getJSONObject(e, name);
	};

	@Override
	public Boolean getConfigDefault(JsonObject e, String name, Boolean def) {
		return Configuration.getDefault(e, name, def);
	};

	@Override
	public int getConfigDefault(JsonObject e, String name, int def) {
		return Configuration.getDefault(e, name, def);
	};

	@Override
	public String getConfigDefault(JsonObject e, String name, String def) {
		return Configuration.getDefault(e, name, def);
	}

	@Override
	public File getConfigDir() {
		return Configuration.configDir;
	};
}
