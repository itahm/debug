package com.itahm.table;

import java.io.File;
import java.io.IOException;

import com.itahm.json.JSONObject;

import com.itahm.json.JSONFile;

public class Table {
	public final static String ACCOUNT = "account";
	public final static String CRITICAL = "critical";
	public final static String DEVICE = "device";
	public final static String MONITOR = "monitor";
	public final static String ICON = "icon";
	public final static String POSITION = "position";
	public final static String PROFILE = "profile";
	public final static String CONFIG = "config";
	public final static String GCM = "gcm";
	
	protected JSONFile file;
	protected JSONObject table;
	
	public Table(File dataRoot, String tableName) throws IOException {
		this.file = new JSONFile((new File(dataRoot, tableName)));
		this.table = file.getJSONObject();
	}
	
	protected boolean isEmpty() {
		return this.table.length() == 0;
	}
	
	public JSONObject getJSONObject() {
		return this.table;
	}
	
	public JSONObject getJSONObject(String key) {
		if (this.table.has(key)) {
			return this.table.getJSONObject(key);
		}
		
		return null;
	}
	
	public JSONObject put(String key, JSONObject value) throws IOException {
		if (value == null) {
			this.table.remove(key);
		}
		else {
			this.table.put(key, value);
		}
		
		return save();
	}
	
	public JSONObject save() throws IOException {
		this.file.save();
		
		return this.table;
	}

	public JSONObject save(JSONObject data) throws IOException{
		this.file.save(this.table = data);
		
		return this.table;
	}
	
}
