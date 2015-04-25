package net.petercashel.jmsDd.auth;

import net.petercashel.jmsDd.auth.interfaces.IAuthDataSystem;
import net.petercashel.jmsDd.event.module.EventBase;

public class AuthSystemInitEvent extends EventBase {

	public String _ds;
	public IAuthDataSystem _backend;

	public AuthSystemInitEvent(String ds, IAuthDataSystem backend) {
		this._ds = ds;
		this._backend = backend;
	}

}
