package net.petercashel.jmsDd.event;

import com.google.gson.JsonObject;

public class ModuleConfigEvent {

		JsonObject _cfg;
		public ModuleConfigEvent(JsonObject cfg){
			this._cfg = cfg;
		}
}
