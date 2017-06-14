package com.itahm.enterprise;

public class Enterprise {

	public static Enterprise getInstance() {
		
		return new KIER();
		
		//return new Enterprise();
	}
	
	public Enterprise() {
		
	}
	
	public void sendEvent(String event) {
	}
		
}
