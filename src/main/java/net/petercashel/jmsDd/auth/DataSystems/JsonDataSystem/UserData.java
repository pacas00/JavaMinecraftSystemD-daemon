package net.petercashel.jmsDd.auth.DataSystems.JsonDataSystem;

import com.google.gson.annotations.SerializedName;

public class UserData {
	
	@SerializedName("Username")
	public String Username;
	
	@SerializedName("Token")
	public String Token;
	
	@SerializedName("TokenSalt")
	public String TokenSalt;

}
