package com.itahm.command;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;

import com.itahm.json.JSONException;
import com.itahm.json.JSONObject;
import com.itahm.table.Table;
import com.itahm.Agent;
import com.itahm.http.Request;
import com.itahm.http.Response;
import com.itahm.util.Network;

public class Extra implements Command {
	
	private static int TOP_MAX = 10;
	
	public enum Key {
		RESET,
		FAILURE,
		SEARCH,
		MESSAGE,
		TOP,
		NETWORK,
		LOG,
		ARP,
		LINK,
		ENTERPRISE;
	};
	
	@Override
	public Response execute(Request request, JSONObject data) throws IOException {
		
		try {
			switch(Key.valueOf(data.getString("extra").toUpperCase())) {
			case RESET:
				Agent.snmp.resetResponse(data.getString("ip"));
				
				return Response.getInstance(request, Response.Status.OK);
			case FAILURE:
				JSONObject json = Agent.snmp.getFailureRate(data.getString("ip"));
				
				if (json == null) {
					return Response.getInstance(request, Response.Status.BADREQUEST,
						new JSONObject().put("error", "node not found").toString());
				}
				
				return Response.getInstance(request, Response.Status.OK, json.toString());
			case SEARCH:
				Network network = new Network(InetAddress.getByName(data.getString("network")).getAddress(), data.getInt("mask"));
				Iterator<String> it = network.iterator();
				
				while(it.hasNext()) {
					Agent.snmp.testNode(it.next(), false);
				}
				
				return Response.getInstance(request, Response.Status.OK);
			case MESSAGE:
				if (Agent.gcmm == null) {
					return Response.getInstance(request, Response.Status.BADREQUEST,
						new JSONObject().put("error", "gcm not enabled").toString());
				}
				
				Agent.gcmm.broadcast(data.getString("message"));
				
				return Response.getInstance(request, Response.Status.OK);
			case TOP:
				int count = TOP_MAX;
				if (data.has("count")) {
					count = Math.min(data.getInt("count"), TOP_MAX);
				}
				
				return Response.getInstance(request, Response.Status.OK, Agent.snmp.getTop(count).toString());
			case NETWORK:
				return Response.getInstance(request, Response.Status.OK, Agent.snmp.getNetwork().toString());
			case LOG:
				return Response.getInstance(request, Response.Status.OK, Agent.log.read(data.getLong("date")));
			case ARP:
				return Response.getInstance(request, Response.Status.OK, Agent.snmp.getARP().toString());
			case LINK:
				Table table = Agent.getTable("device");
				JSONObject deviceData = table.getJSONObject();
				String ip1 = data.getString("peer1");
				String ip2 = data.getString("peer2");
				boolean link = data.getBoolean("link");
				JSONObject device1 = deviceData.getJSONObject(ip1);
				JSONObject device2 = deviceData.getJSONObject(ip2);
				JSONObject ifEntry1 = device1.getJSONObject("ifEntry");
				JSONObject ifEntry2 = device2.getJSONObject("ifEntry");
				
				if (link) {
					ifEntry1.put(ip2, Agent.snmp.getPeerIFName(ip1, ip2));
					ifEntry2.put(ip1, Agent.snmp.getPeerIFName(ip2, ip1));
				}
				else {
					ifEntry1.remove(ip2);
					ifEntry2.remove(ip1);
				}
				
				return Response.getInstance(request, Response.Status.OK, table.save().toString());
			case ENTERPRISE:
				return Agent.snmp.executeEnterprise(request, data);
				
			}
		}
		catch (NullPointerException npe) {
			return Response.getInstance(request, Response.Status.UNAVAILABLE);
		}
		catch (JSONException jsone) {
			return Response.getInstance(request, Response.Status.BADREQUEST,
					new JSONObject().put("error", "invalid json request").toString());
		}
		catch(IllegalArgumentException iae) {
		}
		
		return Response.getInstance(request, Response.Status.BADREQUEST,
			new JSONObject().put("error", "invalid extra").toString());
	}

}
