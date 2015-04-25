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

package net.petercashel.jmsDd.API;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import net.petercashel.jmsDd.command.ICommand;
import com.google.common.eventbus.EventBus;
import com.google.gson.JsonObject;

public interface API {

	//StreamIO to client and process
	
	PrintStream OutputToClient();

	OutputStream InputToProcess();

	//Load Jar (calls module loader)
	
	void LoadJar(File f);

	//Commands
	
	void registerCommand(Class<? extends ICommand> com);

	//EventBus
	
	EventBus getEventBus();

	void registerEventBus(Object handler);

	void PostEvent(Object event);

	
	//User
	
	boolean HasUser(String u);

	int UserPermissionLevel(String u);

	boolean IsSaltedTokenValid(String user, String SaltToken);
	
	//Config

	JsonObject getConfigJSONObject(JsonObject e, String name);

	Boolean getConfigDefault(JsonObject e, String name, Boolean def);

	int getConfigDefault(JsonObject e, String name, int def);

	String getConfigDefault(JsonObject e, String name, String def);

	File getConfigDir();
	
	
	//The instance to be populated from APICore.

	public static class Impl {
		public static API api = null;

		public static API getAPI() {
			return api;
		}
	}

}
