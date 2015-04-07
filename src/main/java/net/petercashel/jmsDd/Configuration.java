package net.petercashel.jmsDd;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Configuration {
	static File configDir = new File(System.getProperty("user.home") + File.separator + ".JMSDd" + File.separator);
	public static JsonObject cfg = null;

	public static void loadConfig() {
		configDir.mkdirs();
		String content = "";
		new File("config" + File.separator).mkdir();
		try {
			byte[] encoded = Files.readAllBytes(new File(configDir, "config.json").toPath());
			content = new String(encoded, StandardCharsets.US_ASCII);
			System.out.println(content);
			
			JsonElement jelement = new JsonParser().parse(content);
		    cfg = jelement.getAsJsonObject();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			cfg = new JsonObject();
		}
	}

	public static void saveConfig() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String jsonString;
		jsonString = gson.toJson(cfg);
		FileOutputStream fop = null;
		File file;
		try {
			file = new File(configDir, "config.json");
			fop = new FileOutputStream(file);
			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}
			// get the content in bytes
			byte[] contentInBytes = jsonString.getBytes(StandardCharsets.US_ASCII);
			fop.write(contentInBytes);
			fop.flush();
			fop.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fop != null) {
					fop.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public static String getDefault(JsonObject e, String name, String def) {
		String obj = null; 
		if (e.has(name)) {
			obj = e.get(name).getAsString();
		} else {
			e.addProperty(name, (String) def);
			obj = e.get(name).getAsString();
		}
		return obj;
	}

	public static int getDefault(JsonObject e, String name, int def) {
		int obj = 0;
		if (e.has(name)) {
			obj = e.get(name).getAsInt();
		} else {
			e.addProperty(name, (Integer) def);
			obj = e.get(name).getAsInt();
		}
		return obj;
	}

	public static Boolean getDefault(JsonObject e, String name, Boolean def) {
		Boolean obj = null; 
		if (e.has(name)) {
			obj =  e.get(name).getAsBoolean();
		} else {
			e.addProperty(name, def);
			obj = e.get(name).getAsBoolean();
		}
		return obj;
	}
	
	public static JsonObject getJSONObject(JsonObject e, String name) {
		if (e.has(name)) return e.getAsJsonObject(name);
		else {
			JsonObject j = new JsonObject();
			e.add(name, j);
			return e.getAsJsonObject(name);
		}
	}
	public static void init() {
		loadConfig();
		
		getJSONObject(cfg, "daemonSettings");
		getDefault(getJSONObject(cfg, "daemonSettings"), "serverPort", 14444);
		getDefault(getJSONObject(cfg, "daemonSettings"), "serverPortEnable", true);
		getDefault(getJSONObject(cfg, "daemonSettings"), "serverCLIEnable", true);
		getDefault(getJSONObject(cfg, "daemonSettings"), "serverSSLEnable", true);
		getJSONObject(cfg, "processSettings");
		getDefault(getJSONObject(cfg, "processSettings"), "processExcecutable", "");
		getDefault(getJSONObject(cfg, "processSettings"), "processWorkingDirectory", "");
		getDefault(getJSONObject(cfg, "processSettings"), "processArguments", "");
		getDefault(getJSONObject(cfg, "processSettings"), "processHasShutdownCommand", false);
		getDefault(getJSONObject(cfg, "processSettings"), "processShutdownCommand", "");
		getJSONObject(cfg, "authSettings");
		getDefault(getJSONObject(cfg, "authSettings"), "authenticationEnable", true);
		getJSONObject(cfg, "webSettings");
		getDefault(getJSONObject(cfg, "webSettings"), "webAPIEnable", true);
		saveConfig();
	}
}
