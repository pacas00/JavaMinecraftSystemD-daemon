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
