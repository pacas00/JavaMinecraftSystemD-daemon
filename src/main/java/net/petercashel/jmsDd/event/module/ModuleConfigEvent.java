package net.petercashel.jmsDd.event.module;

import com.google.gson.JsonObject;

public class ModuleConfigEvent {

		JsonObject _cfg;
		public ModuleConfigEvent(JsonObject cfg){
			this._cfg = cfg;
		}
}
