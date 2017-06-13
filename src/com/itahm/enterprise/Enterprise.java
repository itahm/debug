package com.itahm.enterprise;

import java.sql.SQLException;

public class Enterprise {

	public static Enterprise getInstance() {
		
		try {
			return new KIER();
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
		return new Enterprise();
	}
	
	public Enterprise() {
		
	}
	
	public void sendEvent(String event) {
	}
		
}
