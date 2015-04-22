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

package net.petercashel.jmsDd.auth.DataSystems.JsonDataSystem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.petercashel.commonlib.random.HexCodeGenerator;
import net.petercashel.jmsDd.Configuration;
import net.petercashel.jmsDd.auth.interfaces.IAuthDataSystem;

public class JsonDataSystem implements IAuthDataSystem {

	protected static HashMap<String, UserData> userList = new HashMap<String, UserData>();

	@Override
	public void init() {
		load();
		if (userList.isEmpty()) {
			try {
				AddUser("admin");
			}
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		save();
	}

	@Override
	public void save() {
		JsonArray jsonRoot = new JsonArray();

		Iterator<UserData> iterator = userList.values().iterator();
		while (iterator.hasNext()) {
			Gson gson = new GsonBuilder().create();
			UserData p = iterator.next();
			jsonRoot.add(new JsonParser().parse(gson.toJson(p, UserData.class)).getAsJsonObject());
		}

		// Use GSON to pretty up my JSON.Simple
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String jsonString;
		jsonString = gson.toJson(jsonRoot);
		// done

		FileOutputStream fop = null;
		File file;
		String content = jsonString;
		try {

			file = new File(Configuration.configDir, "userConfig.json");
			fop = new FileOutputStream(file);

			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}

			// get the content in bytes
			byte[] contentInBytes = content.getBytes();

			fop.write(contentInBytes);
			fop.flush();
			fop.close();

		}
		catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (fop != null) {
					fop.close();
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void load() {

		String content = "";
		try {
			content = readFile(new File(Configuration.configDir, "userConfig.json").toPath().toString(),
					StandardCharsets.US_ASCII);
		}
		catch (IOException e) {
			e.printStackTrace();
			return;
		}
		JsonElement jelement = new JsonParser().parse(content);
		// JsonObject jobject = jelement.getAsJsonObject();
		JsonArray jarray = jelement.getAsJsonArray();

		Iterator<JsonElement> iterator = jarray.iterator();
		while (iterator.hasNext()) {
			JsonElement e = iterator.next();
			JsonObject s = e.getAsJsonObject();
			Gson gson = new GsonBuilder().create();
			UserData r = gson.fromJson(s, UserData.class);
			userList.put(r.Username, r);
		}

	}

	@Override
	public void shutdown() {
		save();

	}

	@Override
	public void AddUser(String user) throws Exception {
		if (userList.containsKey(user)) throw new Exception("User Already Exists");
		UserData d = new UserData();
		d.Username = user;
		userList.put(user, d);
		ResetToken(user);
		SetPermissionLevel(user, 0);
	}

	@Override
	public void AddUser(String user, String token) throws Exception {
		if (userList.containsKey(user)) throw new Exception("User Already Exists");
		UserData d = new UserData();
		d.Username = user;
		d.Token = token;
		userList.put(user, d);
		ResetTokenSalt(user);
		SetPermissionLevel(user, 0);
	}

	@Override
	public boolean HasUser(String user) {
		return userList.containsKey(user);
	}

	@Override
	public void DelUser(String user) {
		userList.remove(user);
	}

	@Override
	public String GetToken(String user) {
		return userList.get(user).Token;
	}

	@Override
	public String GetTokenSalt(String user) {
		return userList.get(user).TokenSalt;
	}

	@Override
	public String GetSaltedToken(String user) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-512");
		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		String text = userList.get(user).Token + userList.get(user).TokenSalt;

		md.update(text.getBytes(StandardCharsets.UTF_8));
		byte[] hash = md.digest();
		StringBuffer hexString = new StringBuffer();

		for (int i = 0; i < hash.length; i++) {
			String hex = Integer.toHexString(0xff & hash[i]);
			if (hex.length() == 1) hexString.append('0');
			hexString.append(hex);
		}

		return hexString.toString();
	}

	@Override
	public void ResetToken(String user) {
		String text = HexCodeGenerator.Generate(user, 128);
		UserData d = userList.get(user);
		d.Token = text;
		userList.put(d.Username, d);
		ResetTokenSalt(user);
	}

	@Override
	public void ResetTokenSalt(String user) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-512");
		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		String text = HexCodeGenerator.Generate(user + userList.get(user).Token, 64);

		md.update(text.getBytes(StandardCharsets.UTF_8));
		byte[] hash = md.digest();
		StringBuffer hexString = new StringBuffer();

		for (int i = 0; i < hash.length; i++) {
			String hex = Integer.toHexString(0xff & hash[i]);
			if (hex.length() == 1) hexString.append('0');
			hexString.append(hex);
		}

		UserData d = userList.get(user);
		d.TokenSalt = hexString.toString();
		userList.put(d.Username, d);

	}

	public static String readFile(String path, Charset encoding) throws IOException {

		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	@Override
	public int GetPermissionLevel(String user) {
		// TODO Auto-generated method stub
		return userList.get(user).perms;
	}

	@Override
	public void SetPermissionLevel(String user, int level) {
		UserData d = userList.get(user);
		d.perms = level;
		userList.put(d.Username, d);
	}
}
