package com.itahm;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import com.itahm.json.JSONObject;

import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.util.DailyFile;
import com.itahm.util.Util;

public class Log implements Closeable {

	public final static String SHUTDOWN = "shutdown";
	public final static String CRITICAL = "critical";
	public final static String TEST = "test";
	
	private final Set<Request> waiter = new HashSet<Request> ();
	
	private DailyFile dailyFile;
	private RandomAccessFile indexFile;
	private DailyFile sysLog;
	private JSONObject indexObject;
	private FileChannel indexChannel;
	private long index;
	private final JSONObject log;
	
	public Log(File root) throws IOException {
		File logRoot = new File(root, "log");
		File indexFile = new File(logRoot, "index");
		File systemRoot = new File(logRoot, "system");
		long mills = DailyFile.trim(Calendar.getInstance()).getTimeInMillis();
		
		logRoot.mkdir();
		systemRoot.mkdir();
		
		dailyFile = new DailyFile(logRoot);
		sysLog = new DailyFile(systemRoot);
		
		byte [] bytes = dailyFile.read(mills);
		
		this.log = bytes == null? new JSONObject(): new JSONObject(new String(bytes, StandardCharsets.UTF_8.name()));
		
		this.indexFile = new RandomAccessFile(indexFile, "rws");
		this.indexChannel = this.indexFile.getChannel();
		
		loadIndex();
	}
	
	private void loadIndex() throws IOException {
		int size = (int)this.indexChannel.size();
		
		if (size == 0) {
			this.indexObject = new JSONObject();
			
			this.indexObject.put("index", this.index = 0);
		}
		else {
			ByteBuffer buffer = ByteBuffer.allocate(size);
			byte[] bytes = new byte [size];
			
			this.indexChannel.read(buffer);

			buffer.flip();
			
			buffer.get(bytes);
			
			this.indexObject = new JSONObject(new String(bytes));
			this.index = this.indexObject.getLong("index");
		}
	}
	
	private synchronized long getIndex() throws IOException {
		long index = this.index++;
		
		this.indexObject.put("index", this.index);
		
		this.indexFile.setLength(0);
		
		this.indexChannel.write(ByteBuffer.wrap(this.indexObject.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8.name())));
		
		return index;
	}
	
	public void write(String ip, String message, String type, boolean status, boolean broadcast) {
		JSONObject logData = new JSONObject();
		long index;
		
		try {
			index = getIndex();
		} catch (IOException ioe) {
			Agent.log(Util.EToString(ioe));
			
			return;
		}
		
		logData
			.put("index", index)
			.put("ip", ip)
			.put("type", type)
			.put("status", status)
			.put("message", message)
			.put("date", Calendar.getInstance().getTimeInMillis());
		
		try {
			if (this.dailyFile.roll()) {
				this.log.clear();
			}
		} catch (IOException ioe) {
			sysLog(Util.EToString(ioe));
		}
		
		this.log.put(Long.toString(index), logData);
		
		try {
			this.dailyFile.write(this.log.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8.name()));
		} catch (IOException ioe) {
			sysLog(Util.EToString(ioe));
		}
		
		synchronized(this.waiter) {
			for (Request request : this.waiter) {
				try {
					request.sendResponse(Response.getInstance(request, Response.Status.OK, logData.toString()));
				} catch (IOException ioe) {
					sysLog(Util.EToString(ioe));
				}
			}
			
			waiter.clear();
		}
		
		if(broadcast && Agent.gcmm != null) {
			Agent.gcmm.broadcast(logData.getString("message"));
		}
	}
	
	public void sysLog(String log) {
		try {
			this.sysLog.roll();
			
			this.sysLog.append((log + System.lineSeparator()).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8.name()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String read(long mills) throws IOException {
		byte [] bytes = this.dailyFile.read(mills);
		
		if (bytes != null) {
			return new String(bytes, StandardCharsets.UTF_8.name());
		}
		
		return new JSONObject().toString();
	}
	
	public JSONObject getDailyLog(long index) {
		String key = Long.toString(index);
		
		if (this.log.has(key)) {
			return this.log.getJSONObject(key);
		}
		
		return null;
	}
	
	/**
	 * waiter가 원하는 이벤트 있으면 돌려주고 없으면 waiter 큐에 추가  
	 * @param waiter
	 * @throws IOException 
	 */
	public void listen(Request request, long l) throws IOException {
		String index = Long.toString(l);
		
		synchronized(this.log) {
			if (this.log.has(index)) {
				request.sendResponse(Response.getInstance(request, Response.Status.OK, this.log.getJSONObject(index).toString()));
			}
			else {
				this.waiter.add(request);
			}
		}
	}
	
	public int getWaiterCount() {
		return this.waiter.size();
	}
	
	public void cancel(Request request) {
		synchronized(this.waiter) {
			this.waiter.remove(request);
		}
	}
	
	@Override
	public void close() {
		try {
			this.dailyFile.close();
		} catch (IOException ioe) {
			sysLog(Util.EToString(ioe));
		}
		
		try {
			this.indexChannel.close();
		} catch (IOException ioe) {
			sysLog(Util.EToString(ioe));
		}
		
		try {
			this.sysLog.close();
		} catch (IOException ioe) {
			sysLog(Util.EToString(ioe));
		}
		
		try {
			this.indexFile.close();
		} catch (IOException ioe) {
			sysLog(Util.EToString(ioe));
		}
	}
	
}
