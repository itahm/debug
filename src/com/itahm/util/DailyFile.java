package com.itahm.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Calendar;

public class DailyFile {

	private final File root;
	private File file;
	private final boolean append;
	private int day = 0;
	
	public DailyFile(File root, boolean append) throws IOException {
		this.root = root;
		this.append = append;
	}
	
	public boolean roll() throws IOException {
		Calendar c = Calendar.getInstance();
		int day = c.get(Calendar.DAY_OF_YEAR);
		boolean roll = false;
		
		if (this.day != day) {
			trim(c);
			
			file = new File(this.root, Long.toString(c.getTimeInMillis()));
			
			if (this.day != 0) {
				roll = true;
			}
			
			this.day = day;
		}
		
		return roll;
	}
	
	public byte [] read(long mills) throws IOException {
		File f = new File(this.root, Long.toString(mills));
		
		if (f.isFile()) {
			return Files.readAllBytes(f.toPath());
		}
		
		return null;
	}
	
	public void write(byte [] data) throws IOException {
		try(OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(this.file, this.append), StandardCharsets.UTF_8.name())) {
			osw.write(new String(data));
		}
	}
	
	public static Calendar trim(Calendar c) {
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		
		return c;
	}
	
}
