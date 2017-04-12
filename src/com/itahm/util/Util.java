package com.itahm.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;

public class Util {

	public static void download(URL url, File file) throws IOException {
		try (BufferedInputStream bi = new BufferedInputStream(url.openStream());
			FileOutputStream fos = new FileOutputStream(file);
		) {
			final byte buffer[] = new byte[1024];
			int length;
			
			while ((length = bi.read(buffer, 0, 1024)) != -1) {
				fos.write(buffer,  0, length);
			}
		}
	}
	
	public static Object loadClass(URL url, String name) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
		try (URLClassLoader urlcl = new URLClassLoader(new URL [] {
				url
			})) {
			
			return urlcl.loadClass(name).newInstance();
		}
	}
	
	public static String EToString(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		e.printStackTrace(pw);
		
		return sw.toString();
	}

}
