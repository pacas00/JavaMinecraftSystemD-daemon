package net.petercashel.jmsDd.auth;

import net.petercashel.jmsDd.auth.interfaces.IAuthDataSystem;
import net.petercashel.jmsDd.event.module.EventBase;

public class AuthSystemInitEvent extends EventBase {

	public String _ds;

	public AuthSystemInitEvent(String ds) {
		this._ds = ds;
	}
	public IAuthDataSystem getBackend() {
		return AuthSystem.backend;
	}
	public void setBackend(IAuthDataSystem auth) {
		AuthSystem.backend = auth;
	}

}
