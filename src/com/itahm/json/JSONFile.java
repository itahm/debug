package com.itahm.json;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;

/**
 * The Class JSONFile.
 */
public class JSONFile {
	
	private final File file;
	protected JSONObject json;
	
	public JSONFile(File file) throws IOException {
		this.file = file;
		
		if (file.isFile()) {
			json = new JSONObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
		}
		else {
			json = new JSONObject();
			
			save();
		}
	}
	
	public JSONObject getJSONObject() {
		return this.json;
	}
	
	public Object get(String key) {
		return this.json.has(key)? this.json.get(key): null;
	}
	
	public void save() throws IOException {
		try (OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(this.file), StandardCharsets.UTF_8.name())) {
			osw.write(this.json.toString());
		}
	}
	
	public void save(JSONObject json) throws IOException {	
		this.json = json;
		
		save();
	}
	
	public static JSONObject getJSONObject(File file) throws IOException {
		try {
			return new JSONObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
		}
		catch (JSONException jsone) {
			throw new IOException(jsone);
		}
		catch (NoSuchFileException nsfe) {
			return null;
		}
	}
	
	public static void save(File file, JSONObject json) throws IOException {
		try(OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8.name())) {
			osw.write(json.toString());
		}
	}
	
}