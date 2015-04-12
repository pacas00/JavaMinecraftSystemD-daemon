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
package net.petercashel.jmsDd.auth;

import net.petercashel.jmsDd.auth.DataSystems.JsonDataSystem.JsonDataSystem;
import net.petercashel.jmsDd.auth.interfaces.IAuthDataSystem;
import static net.petercashel.jmsDd.Configuration.*;

public class AuthSystem {

	public static IAuthDataSystem backend = null;

	public static void init() {
		if (!getDefault(getJSONObject(cfg, "authSettings"),
				"authenticationEnable", true))
			return;
		String ds = getDefault(getJSONObject(cfg, "authSettings"),
				"authenticationSystem", "JsonDataSystem");

		switch (ds) {

		default:
		case "JsonDataSystem": {
			backend = new JsonDataSystem();
			backend.init();
		}

		}

	}

}
