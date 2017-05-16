package com.itahm.json;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Calendar;

import com.itahm.json.JSONObject;
import com.itahm.util.Util;

/**
 * The Class RollingFile.
 */
public class RollingFile {
	
	/** The lastHour. */
	private long lastHour = -1;
	private long lastDay = -1;
	
	/** rollingRoot, itahm/snmp/ip address/resource/index */
	private final File root;
	
	private File summaryFile;
	private JSONObject summaryData;
	private JSONObject summary;
	private String summaryHour;
	
	private File dayDirectory;
	private File hourFile;
	private JSONObject hourData;
	private long max;
	private long min;
	private BigInteger hourSum = BigInteger.valueOf(0);
	private int hourCnt = 0;
	private BigInteger minuteSum = BigInteger.valueOf(0);
	private long minuteSumCnt = 0;
	
	/**
	 * Instantiates a new rolling file.
	 *
	 * @param root the root (itahm\snmp\ip\resource)
	 * @param index the index of host, interfaces, etc.
	 * @param type gauge or counter
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public RollingFile(File rscRoot, String index) throws IOException {
		Calendar c = Calendar.getInstance();
		
		c.set(Calendar.MILLISECOND, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MINUTE, 0);
		
		root = new File(rscRoot, index);
		root.mkdir();
		
		this.lastHour = c.getTimeInMillis();
		this.summaryHour = Long.toString(this.lastHour);
				
		c.set(Calendar.HOUR_OF_DAY, 0);
		
		this.lastDay = c.getTimeInMillis();
		
		this.dayDirectory = new File(this.root, Long.toString(this.lastDay));
		this.dayDirectory.mkdir();
		
		// summary file 생성
		this.summaryFile = new File(this.dayDirectory, "summary");
		if (this.summaryFile.isFile()) {
			this.summaryData = Util.getJSONFromFile(this.summaryFile);
		}
		
		// 최초 생성되거나, 파일이 깨졌을때
		if (this.summaryData == null) {
			Util.putJSONtoFile(this.summaryFile, this.summaryData = new JSONObject());
		}
		
		if (this.summaryData.has(this.summaryHour)) {
			summary = this.summaryData.getJSONObject(this.summaryHour);
		}
		else {
			this.summaryData.put(this.summaryHour, summary = new JSONObject());
		}
		
		// hourly file 생성
		this.hourFile = new File(this.dayDirectory, this.summaryHour);
		if (this.hourFile.isFile()) {
			this.hourData = Util.getJSONFromFile(this.hourFile);
		}
		
		// 최초 생성되거나, 파일이 깨졌을때
		if (this.hourData == null) {
			Util.putJSONtoFile(this.hourFile, this.hourData = new JSONObject());
		}
	}
	
	/**
	 * Roll.
	 *
	 * @param value the value
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public void roll(long value) throws IOException {
		Calendar c = Calendar.getInstance();
		String minString;
		long hourMills, dayMills;

		c.set(Calendar.MILLISECOND, 0);
		c.set(Calendar.SECOND, 0);
		
		minString = Long.toString(c.getTimeInMillis());
		
		c.set(Calendar.MINUTE, 0);
		hourMills = c.getTimeInMillis();
		
		if (this.lastHour != hourMills) {
			// 시간마다 summary 파일과 시간파일 저장
			Util.putJSONtoFile(this.summaryFile, this.summaryData);
			Util.putJSONtoFile(this.hourFile, this.hourData);
			
			c.set(Calendar.HOUR_OF_DAY, 0);
			dayMills = c.getTimeInMillis();
			
			if (this.lastDay != dayMills) {
				this.lastDay = dayMills;
				
				// day directory 생성
				this.dayDirectory = new File(this.root, Long.toString(dayMills));
				this.dayDirectory.mkdir();
				
				// summary file 생성
				this.summaryFile = new File(this.dayDirectory, "summary");
				this.summaryData = new JSONObject();
			}
			
			// hourly file 생성
			this.lastHour = hourMills;
			this.summaryHour = Long.toString(hourMills);
			this.hourFile = new File(this.dayDirectory, this.summaryHour);
			this.hourData = new JSONObject();
			this.hourCnt = 0;
			this.summaryData.put(this.summaryHour, this.summary = new JSONObject());
		}
		
		roll(minString, value);
	}
	
	private void roll(String minuteString, long value) throws IOException {
		if (this.hourData.has(minuteString)) {
			this.minuteSum = this.minuteSum.add(BigInteger.valueOf(value));
		}
		else {
			this.minuteSum = BigInteger.valueOf(value);
			this.minuteSumCnt = 0;
		}
		
		this.minuteSumCnt++;
		
		this.hourData.put(minuteString, this.minuteSum.divide(BigInteger.valueOf(this.minuteSumCnt)));
		
		if (this.hourCnt == 0) {
			this.hourSum = BigInteger.valueOf(value);
			this.max = value;
			this.min = value;
		}
		else {
			this.hourSum = this.hourSum.add(BigInteger.valueOf(value));
			this.max = Math.max(this.max, value);
			this.min = Math.min(this.min, value);
		}
		
		this.hourCnt++;
		
		summarize(this.hourSum.divide(BigInteger.valueOf(this.hourCnt)).longValue());
	}
	
	private void summarize(long avg) throws IOException {
		this.summary
			.put("avg", avg)
			.put("max", Math.max(avg, this.max))
			.put("min", Math.min(avg, this.min));
	}
	
	public JSONObject getData(long start, long end, boolean summary) throws IOException {
		Data data;
		JSONObject source, result;
		long today = Util.trimDate(Calendar.getInstance()).getTimeInMillis();
		
		if (summary) {
			data = new JSONSummary(this.root);
			source = this.summaryData;
		}
		else {
			data = new JSONData(this.root);
			source = this.hourData;
		}
			
		result = data.getJSON(start, end);
		
		if (start < today && today < end) {
			for (Object key : source.keySet()) {
				data.put((String)key, source.getJSONObject((String)key));
			}
		}
		
		return result;
	}
	
}

