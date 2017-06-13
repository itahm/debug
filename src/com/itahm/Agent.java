package com.itahm;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import com.itahm.ITAhMAgent;
import com.itahm.GCMManager;
import com.itahm.Log;
import com.itahm.SNMPAgent;
import com.itahm.ICMPAgent;


import com.itahm.json.JSONException;
import com.itahm.json.JSONObject;

import com.itahm.command.Command;
import com.itahm.command.Commander;
import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.http.Session;
import com.itahm.table.Account;
import com.itahm.table.Config;
import com.itahm.table.Critical;
import com.itahm.table.Device;
import com.itahm.table.GCM;
import com.itahm.table.Monitor;
import com.itahm.table.Position;
import com.itahm.table.Profile;
import com.itahm.table.Table;

public class Agent implements ITAhMAgent {

	public final static String VERSION = "1.4.3.0";
	public final static String API_KEY = "AIzaSyBg6u1cj9pPfggp-rzQwvdsTGKPgna0RrA";
	
	public final static int MAX_TIMEOUT = 10000;
	public final static int ICMP_INTV = 1000;
	public final static int MID_TIMEOUT = 5000;
	public final static int DEF_TIMEOUT = 3000;
	
	private static Map<String, Table> tableMap = new HashMap<>();
	
	public static Log log;
	public static GCMManager gcmm = null;
	public static SNMPAgent snmp;
	public static ICMPAgent icmp;
	
	private static File root;
	private boolean isClosed = true;
	
	public Agent() {
		System.out.format("ITAhM Agent version %s (updated) ready.\n", VERSION);
	}
	
	public boolean start(File dataRoot) {
		if (!this.isClosed) {
			return false;
		}
		
		root = dataRoot;
		
		this.isClosed = false;
		
		try {
			tableMap.put(Table.ACCOUNT, new Account(dataRoot));
			tableMap.put(Table.PROFILE, new Profile(dataRoot));
			tableMap.put(Table.DEVICE, new Device(dataRoot));
			tableMap.put(Table.POSITION, new Position(dataRoot));
			tableMap.put(Table.MONITOR, new Monitor(dataRoot));
			tableMap.put(Table.CONFIG, new Config(dataRoot));
			tableMap.put(Table.ICON, new Table(dataRoot, Table.ICON));
			tableMap.put(Table.CRITICAL, new Critical(dataRoot));
			tableMap.put(Table.GCM, new GCM(dataRoot));
			tableMap.put(Table.SMS, new Table(dataRoot, Table.SMS));
			
			log = new Log(dataRoot);
			snmp = new SNMPAgent(dataRoot);
			icmp = new ICMPAgent();
			
			try {
				JSONObject config = getTable(Table.CONFIG).getJSONObject();
				int clean = config.getInt(com.itahm.command.Config.Key.CLEAN.toString().toLowerCase());
				
				if (clean > 0) {
					snmp.clean(clean);
				}
				
				if (!config.isNull(com.itahm.command.Config.Key.GCM.toString().toLowerCase())) {
					gcmm = new GCMManager(API_KEY, config.getString(com.itahm.command.Config.Key.GCM.toString().toLowerCase()));
				}
			}
			catch (JSONException jsone) {
				throw new IOException("invalid json configuration.");
			}
			
			System.out.println("ITAhM agent up.");
			
			return true;
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		stop();
		
		return false;
	}
	
	public static void log(String msg) {
		Calendar c = Calendar.getInstance();
		
		log.sysLog(String.format("%04d-%02d-%02d %02d:%02d:%02d %s"
				, c.get(Calendar.YEAR)
				, c.get(Calendar.MONTH +1)
				, c.get(Calendar.DAY_OF_MONTH)
				, c.get(Calendar.HOUR_OF_DAY)
				, c.get(Calendar.MINUTE)
				, c.get(Calendar.SECOND), msg));
	}
	
	public static long getUsableSpace() {
		if (root == null) {
			return 0;
		}
		
		return root.getUsableSpace();
	}
	
	private Session signIn(JSONObject data) {
		String username = data.getString("username");
		String password = data.getString("password");
		JSONObject accountData = getTable(Table.ACCOUNT).getJSONObject();
		
		if (accountData.has(username)) {
			 JSONObject account = accountData.getJSONObject(username);
			 
			 if (account.getString("password").equals(password)) {
				// signin 성공, cookie 발행
				return Session.getInstance(account.getInt("level"));
			 }
		}
		
		return null;
	}

	private static Session getSession(Request request) {
		String cookie = request.getRequestHeader(Request.Header.COOKIE);
		
		if (cookie == null) {
			return null;
		}
		
		String [] cookies = cookie.split("; ");
		String [] token;
		Session session = null;
		
		for(int i=0, length=cookies.length; i<length; i++) {
			token = cookies[i].split("=");
			
			if (token.length == 2 && "SESSION".equals(token[0])) {
				session = Session.find(token[1]);
				
				if (session != null) {
					session.update();
				}
			}
		}
		
		return session;
	}
	
	public static Table getTable(String table) {
		return tableMap.get(table);
	}
	
	@Override
	public Response executeRequest(Request request, JSONObject data) {
		if (this.isClosed) {
			return Response.getInstance(Response.Status.SERVERERROR);
		}
		
		String cmd = data.getString("command");
		Session session = getSession(request);
		
		if ("signin".equals(cmd)) {
			if (session == null) {
				try {
					session = signIn(data);
				} catch (JSONException jsone) {
					return Response.getInstance(Response.Status.BADREQUEST, new JSONObject().put("error", "invalid json request").toString());
				}
			}
			
			if (session == null) {
				return Response.getInstance(Response.Status.UNAUTHORIZED);
			}
			
			return Response.getInstance(Response.Status.OK, new JSONObject()
				.put("level", (int)session.getExtras())
				.put("version", VERSION).toString())
					.setResponseHeader("Set-Cookie", String.format("SESSION=%s; HttpOnly", session.getCookie()));
		}
		
		if ("signout".equals(cmd)) {
			if (session != null) {
				session.close();
			}
			
			return Response.getInstance(Response.Status.OK);
		}
		
		Command command = Commander.getCommand(cmd);
		
		if (command == null) {
			return Response.getInstance(Response.Status.BADREQUEST, new JSONObject().put("error", "invalid command").toString());
		}
		
		try {
			if ("put".equals(cmd) && "gcm".equals(data.getString("database"))) {
				return command.execute(request, data);
			}
			
			if (session != null) {
				return command.execute(request, data);
			}
		}
		catch (IOException ioe) {
			return Response.getInstance(Response.Status.UNAVAILABLE, new JSONObject().put("error", ioe).toString());
		}
			
		return Response.getInstance(Response.Status.UNAUTHORIZED);
	}

	@Override
	public void closeRequest(Request request) {
		log.cancel(request);
	}

	@Override
	public void stop() {
		if (this.isClosed) {
			return;
		}
		
		this.isClosed = true;
		
		if (snmp != null) {
			snmp.close();
		}
		
		if (icmp != null) {
			icmp.close();
		}
		
		if (gcmm != null) {
			gcmm.close();
		}
		
		System.out.println("ITAhM agent down.");
	}

	@Override
	public Object get(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void set(Object value) {
		// TODO Auto-generated method stub
		
	}

}
