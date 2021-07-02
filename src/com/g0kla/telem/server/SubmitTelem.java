package com.g0kla.telem.server;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class SubmitTelem {
	
	public static final String SATNOGS_URL = "https://db.satnogs.org/api/telemetry/";
	String url;
	int noradId;
	String source; // receiving station callsign
	Date timestamp;
	String frame; // hex string of the data
	String locator = "longLat";
	double longitude;
	double latitude;
	float azimuth;
	float elevation;
	long fDown;
	public int responseCode;
	public String responseText; // store the response for later printing or debug
	
	SubmitTelem(String url, int noradId, String source, Date timestamp, double longitude, double latitude, String frame) {
		this.url = url;
		this.noradId = noradId;
		this.source = source;
		this.timestamp = timestamp;
		this.longitude = longitude;
		this.latitude = latitude;
		this.frame = frame;
	}
	
	
	public boolean send() throws Exception {

        HttpPost post = new HttpPost(url);

        // add request parameter, form parameters
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("noradID", ""+noradId));
        urlParameters.add(new BasicNameValuePair("source", source));
        
        TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
		df.setTimeZone(tz);
		String timestampAsISO = df.format(timestamp);
        urlParameters.add(new BasicNameValuePair("timestamp", timestampAsISO));

        urlParameters.add(new BasicNameValuePair("frame", frame));
        urlParameters.add(new BasicNameValuePair("locator", locator));
        
        DecimalFormat decimalFormat5 = new DecimalFormat();
        decimalFormat5.setMaximumFractionDigits(5);
        
        String longDir = "E";
        if (longitude < 0) {
        	longDir = "W";
        	longitude = longitude * -1;
        }
        String latDir = "N";
        if (latitude < 0) {
        	latDir = "S";
        	latitude = latitude * -1;
        }
        
        urlParameters.add(new BasicNameValuePair("longitude", decimalFormat5.format(longitude)+longDir));
        urlParameters.add(new BasicNameValuePair("latitude", decimalFormat5.format(latitude)+latDir));
        
        HttpEntity entity = new UrlEncodedFormEntity(urlParameters,"UTF-8");
               
        post.setEntity(entity);
        String postStr = EntityUtils.toString(entity);
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(post)) {
        	// Getting the status code.
        	responseCode = response.getStatusLine().getStatusCode();

        	// Getting the response body.
        	responseText = EntityUtils.toString(response.getEntity());
        	
        	if (responseCode < 200 || responseCode >= 300) {
        		// Debug error
            	return false;
        	}
        	
        	return true;
        }
    }
	
	public static void main(String args[]) {
		
		String frame = "";
		int[] by = {0xC0,0x00,0x96,0x68,0x96,0x88,0xA4,0x40,0xE0,0xA0,0x8C,0xA6,0x66,0x40,0x40,0x79,0x84,0xF0,0x00,0x06,0xC0};  // example I FRAME WITH DATA
		for (int b : by)
			frame = frame + Integer.toHexString(b&0xff);
		
		Date now = new Date();
		SubmitTelem telem = new SubmitTelem("https://httpbin.org/post", 99718, "G0KLA", now, -73.5, 40.1, frame);
		try {
			boolean r = telem.send();
			System.out.println("Response: " + r);
			System.out.println("Code: " + telem.responseCode);
			System.out.println("Text: " + telem.responseText);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		Date timestamp = new Date();
//		TimeZone tz = TimeZone.getTimeZone("UTC");
//		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
//		df.setTimeZone(tz);
//		String timestampAsISO = df.format(timestamp);
//		System.err.println(timestampAsISO);
	}
	
}
