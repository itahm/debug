package com.itahm.enterprise;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;

import com.itahm.Agent;
import com.itahm.json.JSONObject;
import com.itahm.table.Table;

public class KIER extends Enterprise {

	public static final String KEY = "01F10774-7EDA-409A-A545-117228B4E3B2";
	
	private static final String URL = "jdbc:oracle:thin:@127.0.0.1:1521:XE";
	private static final String USER = "KIERWEB";
	private static final String PASSWD = "KIERWEBpw";
	
	Connection connection;
	long index = 0; /** TODO 인덱스 처리 어떻게 할지 확인할것.*/
	
	public static String getDateString() {
		Calendar c = Calendar.getInstance();
		
		return String.format("%04d%02d%02d%02d%02d%02d",
				c.get(Calendar.YEAR),
				c.get(Calendar.MONTH +1),
				c.get(Calendar.DATE),
				c.get(Calendar.HOUR_OF_DAY),
				c.get(Calendar.MINUTE),
				c.get(Calendar.SECOND));
	}
	
	@Override
	public void sendEvent(String event) {
		try (Connection connection = DriverManager.getConnection(URL, USER, PASSWD)) {
			try (Statement s = this.connection.createStatement()) {
				JSONObject smsData = Agent.getTable(Table.SMS).getJSONObject();
				String date = KIER.getDateString();
				
				for (Object id : smsData.keySet()) {
					sendEvent(s, this.index++, date, event, smsData.getJSONObject((String)id).getString("number"));
				}
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
	}
	
	public static void sendEvent(Statement s, long index, String date, String event, String to) throws SQLException {
		s.executeUpdate(String.format("insert into SMSLIST (M_INDEX, TO_NUM, TRS_TIME, MSG, SENDER_ID)"
			+" values ('%d', '%s', '%s', '%s', 'NMS')"
			, 2 // index
			, to /* 수신 */
			, date /* 송신 시간 */
			, event /* 메세지 */ ));
	}
	
	public static void main(String [] args) throws SQLException, ClassNotFoundException, IOException {
		new KIER();
	}
}
