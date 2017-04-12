package com.itahm.snmp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.UserTarget;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

abstract public class TmpNode implements ResponseListener {


	
	//protected final SNMPAgent agent;
	protected final Snmp agent;
	private long timeout;
	private final LinkedList<Target> list;
	private final Map<Target, String> profileMap;
	
	protected final String ip;
	
	abstract public void onSuccess(String profileName);
	abstract public void onFailure();
	
	public TmpNode(Snmp agent, String ip, long timeout) {
		this.agent = agent;
		this.ip = ip;
		this.timeout = timeout;
		
		list = new LinkedList<>();
		
		profileMap = new HashMap<>();
	}
	
	public TmpNode addProfile(String name, int udp, OctetString community) throws UnknownHostException{
		CommunityTarget target;
			
		target = new CommunityTarget(new UdpAddress(InetAddress.getByName(this.ip), udp), community);
		target.setVersion(SnmpConstants.version2c);
		target.setTimeout(this.timeout);
		
		this.list.add(target);
		this.profileMap.put(target, name);	
		
		return this;
	}
	
	public TmpNode addV3Profile(String name, int udp, OctetString user, int level) throws UnknownHostException{
		UserTarget target = new UserTarget();
		
		target.setAddress(new UdpAddress(InetAddress.getByName(this.ip), udp));
		target.setVersion(SnmpConstants.version3);
		target.setSecurityLevel(level);
		target.setSecurityName(user);
		target.setTimeout(this.timeout);
		
		this.list.add(target);
		this.profileMap.put(target, name);	
		
		return this;
	}
	
	public void test() {
		Target target = this.list.peek();
		PDU pdu;
		
		if (target == null) {
			onFailure();
		}
		else {
			if (target instanceof UserTarget) {
				pdu = new ScopedPDU();
			}
			else {
				pdu = new PDU();
			}
			
			pdu.setType(PDU.GET);
			
			try {
				this.agent.send(pdu, target, null, this);
			} catch (IOException e) {
				onFailure();
			}
		}
	}
	
	@Override
	public void onResponse(ResponseEvent event) {
		this.agent.cancel(event.getRequest(), this);

		Target target = this.list.pop();
		
		if (!(event.getSource() instanceof Snmp.ReportHandler) && event.getResponse() != null && event.getResponse().getErrorStatus() == PDU.noError) {
			onSuccess(this.profileMap.get(target));
		}
		else {
			test();
		}
	}
	
	public static void main(String[] args) throws IOException {
		Snmp snmp = new Snmp(new DefaultUdpTransportMapping());
		
		snmp.listen();
		
		TmpNode node = new TmpNode(snmp, args[0], 10000) {

			@Override
			public void onSuccess(String profileName) {
				System.out.println("success profile name is "+ profileName);
			}

			@Override
			public void onFailure() {
				System.out.println("falure");
			}};
			
		node.addProfile("test", Integer.parseInt(args[2]), new OctetString(args[1]));
		
		node.test();
		
		System.in.read();
			
		snmp.close();
	}
}
