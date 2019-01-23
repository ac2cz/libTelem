package com.g0kla.telem.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;

/**
 * FOX 1 Telemetry Decoder
 * 
 * @author chris.e.thompson g0kla/ac2cz
 *
 *         Copyright (C) 2015 amsat.org
 *
 *         This program is free software: you can redistribute it and/or modify
 *         it under the terms of the GNU General Public License as published by
 *         the Free Software Foundation, either version 3 of the License, or (at
 *         your option) any later version.
 *
 *         This program is distributed in the hope that it will be useful, but
 *         WITHOUT ANY WARRANTY; without even the implied warranty of
 *         MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *         General Public License for more details.
 *
 *         You should have received a copy of the GNU General Public License
 *         along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *         A Satellite Telemetry Protocol record
 * 
 */
public class STP implements Comparable<STP> {
	
	public static final DateFormat stpDateFormat = new SimpleDateFormat(
			"yyyyMMdd HH:mm:ss", Locale.ENGLISH);
	private static Sequence sequence;
	
	public static final String NONE = "NONE";

	public int satId;
	public String receiver = NONE; // unique name (usually callsign) chosen by
	// the user. May vary over life of program
	// usage, so stored
	private String frequency = NONE; // frequency when this frame received
	private String source; // The frame source subsystem
	private String length; // The frame length in bytes.  Should be bytes.length
	public String rx_location = NONE; // the lat, long and altitude
	public String receiver_rf = NONE; // human description of the receiver
	public String demodulator; // will contain Config.VERSION
	private Date stpDate;
	public long sequenceNumber = Sequence.ERROR_NUMBER;

	private String measuredTCA = NONE; // time of TCA
	private String measuredTCAfrequency = NONE; // frequency if this frame was
	// just after TCA

	int numberBytesAdded = 0;
	protected int[] bytes;
	
	/**
	 * Constructor to create a new STP record from decoded telemetry
	 * @param id
	 * @param callsign
	 * @param latitude
	 * @param longitude
	 * @param altitude
	 * @param stationDetails
	 * @param software
	 * @param recordSource
	 * @param sequenceNum
	 * @param record
	 */
	public STP(int id, String callsign, String latitude, String longitude, String altitude, String stationDetails, 
			String software, String recordSource, long sequenceNum, STPable record) {
		satId = id;
		receiver = callsign;
		// frequency = "";
		rx_location = formatLatitude(latitude) + " "
				+ formatLongitude(longitude);
		if (notNone(altitude))
			rx_location = rx_location + " " + altitude;
		receiver_rf = stationDetails;
		String os = System.getProperty("os.name").toLowerCase();
		demodulator = software + " (" + os + ")";
		stpDate = Calendar.getInstance().getTime();
		source = recordSource;
		sequenceNumber = sequenceNum;
		bytes = record.getRawBytes();
		length = ""+bytes.length;
	}

	public STP(int id, Date stpDate, String callsign, String location, String stationDetails, 
			String software, String recordSource, long sequenceNum, int[] data) {
		satId = id;
		receiver = callsign;
		// frequency = "";
		rx_location = location;
		receiver_rf = stationDetails;
		String os = System.getProperty("os.name").toLowerCase();
		demodulator = software + " (" + os + ")";
		this.stpDate = stpDate;
		source = recordSource;
		sequenceNumber = sequenceNum;
		bytes = data;
		length = ""+bytes.length;
	}
	
	public STP(BufferedReader file) throws IOException {
		load(file);
	}

	
	public void setFrequency(long freq) {
		frequency = Long.toString(Math.round(freq)) + " Hz";
	}
	
	public void setTCA(Date tca, long tcaFreq) {
		stpDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		measuredTCA = stpDateFormat.format(tca);
		measuredTCAfrequency = Long.toString(Math.round(tcaFreq)) + " Hz";
	}
	
	private boolean notNone(String s) {
		if (s.equalsIgnoreCase(NONE))
			return false;
		if (s.equalsIgnoreCase(""))
			return false;
		return true;

	}
	private String formatLatitude(String lat) {
		String s = "";
		float f = Float.parseFloat(lat);
		if (f >= 0)
			s = "N " + lat;
		else
			s = "S " + Math.abs(f);
		return s;
	}

	public String getStpDate() {
		if (stpDate == null) return null;
		return STP.stpDateFormat.format(stpDate);
	}

	private String formatLongitude(String lon) {
		String s = "";
		float f = Float.parseFloat(lon);
		if (f >= 0)
			s = "E " + lon;
		else
			s = "W " + Math.abs(f);
		return s;
	}
	
	/**
	 * Compare STP records based on their sequence number. We should not be comparing
	 * frames across satellites, and we should not have two records with the
	 * same sequence number
	 * 
	 * @param STP record
	 *            to compare against
	 */
	public int compareTo(STP f) {
		if (sequenceNumber == f.sequenceNumber)
			return 0;
		else if (sequenceNumber < f.sequenceNumber)
			return -1;
		else if (sequenceNumber > f.sequenceNumber)
			return +1;
		return +1;
	}
	
	private String getSTPCoreHeader() {
		String header;

		stpDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String date = stpDateFormat.format(stpDate);

		// These have to be sent, so cannot be NONE
		header = "Sequence: " + sequenceNumber + "\r\n";
		header = header + "Source: " + source + "\r\n";
		header = header + "Length: " + length + "\r\n";
		header = header + "Date: " + date + " UTC\r\n";
		header = header + "Receiver: " + receiver + "\r\n";
		header = header + "Rx-Location: " + rx_location + "\r\n";

		// These are optional
		if (notNone(frequency))
			header = header + "Frequency: " + frequency + "\r\n";

		if (notNone(receiver_rf))
			header = header + "Receiver-RF: " + receiver_rf + "\r\n";

		if (notNone(demodulator))
			header = header + "Demodulator: " + demodulator + "\r\n";

		return header;
	}

	private String getSTPExtendedHeader() {
		String header = "";

		if (notNone(measuredTCA)) {
			header = "MeasuredTCA: " + measuredTCA + " UTC\r\n";
			;
			header = header + "MeasuredTCAFrequency: " + measuredTCAfrequency
					+ "\r\n";
		}
		return header;

	}

	static char pattern = ',';
	static char escapeChar = '~';
	private String escapeComma(String s) {	
		return s.replace(pattern, escapeChar);
	}

	private String insertComma(String s) {
		return s.replace(escapeChar, pattern);
	}

	public void save(BufferedWriter output) throws IOException {
		output.write(Long.toString(sequenceNumber) + "," + satId + ","
				+ escapeComma(source) + "," + escapeComma(receiver) + ","
				+ escapeComma(frequency) + "," + escapeComma(rx_location) + ","
				+ escapeComma(receiver_rf) + "," + escapeComma(demodulator)
				+ "," + stpDate.getTime() + "," + length + ",");
		for (int i = 0; i < bytes.length - 1; i++)
			output.write(Integer.toString(bytes[i]) + ",");
		output.write(Integer.toString(bytes[bytes.length - 1]) + "\n");
	}

	/**
	 * Static factory method that creates a frame from a file
	 * 
	 * @param fileName
	 * @return
	 * @throws IOException
	 * @throws StpFileProcessException 
	 * @throws LayoutLoadException
	 */
	public static STP loadStp(String fileName) throws IOException, StpFileProcessException {
		
		FileInputStream in = new FileInputStream(fileName);
		int c;
		int lineLen = 0;

		boolean done = false;
		boolean readingKey = true;
		String key = "";
		String value = "";
		int[] rawFrame = null;
		int length = 0;
		String receiver = null;
		Date stpDate = null;
		String frequency = NONE; // frequency when this frame received
		String source = null; // The frame source subsystem
		String rx_location = NONE; // the lat, long and altitude
		String receiver_rf = NONE; // human description of the receiver
		String demodulator = null; // will contain Config.VERSION
		long sequenceNumber = Sequence.ERROR_NUMBER;
		
		String measuredTCA = NONE; // time of TCA
		String measuredTCAfrequency = NONE;
		
		boolean firstColon = true;
		char ch;
		// Read the file
		try {
			while (!done && (c = in.read()) != -1) {
				ch = (char) c;
				//System.out.print(ch);

				if (c == 58 && firstColon) { // ':'
					firstColon = false;
					c = in.read(); // consume the space
					c = in.read();
					ch = (char) c; // set ch to the first character
					readingKey = false;
				}
				if ( (c == 13 || c == 10)) { // CR or LF
					c = in.read(); // consume the lf
					if ((length != 0) && lineLen == 1) {
						// then we are ready to process
						rawFrame = new int[length/8];
						for (int i=0; i<length/8; i++) {
							rawFrame[i] = in.read();
						}
						done = true;
					} else {
						// It was a header line
						readingKey = true;
						firstColon = true;
						if (key.startsWith("Length")) {
							length = Integer.parseInt(value);
						}
						if (key.equalsIgnoreCase("Receiver")) {
							receiver = value;
							//                		System.out.println(key + " " + value);
						}
						if (key.equalsIgnoreCase("Frequency")) {
							frequency = value;
							//                		System.out.println(key + " " + value);
						}
						if (key.equalsIgnoreCase("Rx-location")) {
							rx_location = value;
							//                		System.out.println(key + " " + value);
						}
						if (key.equalsIgnoreCase("Receiver-RF")) {
							receiver_rf = value;
							//                		System.out.println(key + " " + value);
						}
						if (key.equalsIgnoreCase("Demodulator")) {
							demodulator = value;
							//                		System.out.println(key + " " + value);
						}
						if (key.endsWith("Sequence")) {
							sequenceNumber = Long.parseLong(value);
							//System.out.println(key + " *** " + value);
						}
						if (key.equalsIgnoreCase("MeasuredTCA")) {
							measuredTCA = value;
							//                		System.out.println(key + " " + value);
						}
						if (key.equalsIgnoreCase("MeasuredTCAfrequency")) {
							measuredTCAfrequency = value;
							//                		System.out.println(key + " " + value);
						}
						if (key.startsWith("Date")) {
							//                		System.out.println(key + " " + value);
							String dt = value.replace(" UTC", "");
							stpDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
							try {
								stpDate = stpDateFormat.parse(dt);
							} catch (ParseException e) {
								//Log.println("ERROR - Date was not parsable. Setting to null");
								stpDate = null;
								//e.printStackTrace(Log.getWriter());
							} catch (NumberFormatException e) {
								//Log.println("ERROR - Date has number format exception. Setting to null");
								stpDate = null;
							} catch (Exception e) { // we can get other unusual exceptions such as ArrayIndexOutOfBounds...
								//Log.println("ERROR - Date was not parsable. Setting to null: " + e.getMessage());
								stpDate = null;
								//e.printStackTrace(Log.getWriter());								
							}
						}
						key = "";
						value = "";
						lineLen = 0;
					}
				} else {
					if (readingKey) 
						key = key + ch;
					else
						value = value + ch;
				}
				lineLen++;

			}

		} finally {
			in.close();
		}
		in.close();

		if (rawFrame == null) {
			// We failed to process the file
			//Log.println("Failed to Process STP file. RAW FRAME is null.  No content.  Likely SPAM or broken connection.");
			return null;
			
		}

//		public STP(int id, Date stpDate, String callsign, String location, String stationDetails, 
//				String software, String recordSource, int sequenceNum, byte[] data)
		STP frame = new STP(0, stpDate, receiver, rx_location, receiver_rf, demodulator, source, sequenceNumber, rawFrame);

		frame.frequency = frequency;
		frame.measuredTCA = measuredTCA;
		frame.measuredTCAfrequency = measuredTCAfrequency;

		return frame;

	}

	public static String getTableCreateStmt() {
		String s = new String();
		s = s + "(stpDate varchar(35), id int, resets int, uptime bigint, type int, "
		 + "sequenceNumber bigint, "
		 + "length int, "
		 + "source varchar(35)," 
		 + "receiver varchar(35),"		
		 + "frequency varchar(35),"
		 + "rx_location varchar(35),"	
		 + "receiver_rf varchar(52),"		
		 + "demodulator varchar(100),"
		+ "measuredTCA varchar(35),"
		+ "measuredTCAfrequency varchar(35),"
		+ "date_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,";
		s = s + "PRIMARY KEY (id, resets, uptime, type, receiver))";
		return s;
	}
	
//	public PreparedStatement getPreparedInsertStmt(Connection con) throws SQLException {
//				
//		//java.sql.Date sqlDate = new java.sql.Date(stpDate.getTime());
//		//FIXME - need to make this a proper date in the DB
//		String dt = "";
//		if (stpDate != null)
//			dt = stpDateFormat.format(stpDate);
//		String s = new String();
//		if (demodulator.length() > 99) demodulator = demodulator.substring(0, 99);
//		if (source.length() > 32) source = source.substring(0, 32);
//		if (receiver.length() > 32) receiver = receiver.substring(0, 32);
//		if (frequency.length() > 32) frequency = frequency.substring(0, 32);
//		if (rx_location.length() > 32) rx_location = rx_location.substring(0, 32);
//		if (receiver_rf.length() > 50) receiver_rf = receiver_rf.substring(0, 50);
//		if (measuredTCA.length() > 32) measuredTCA = measuredTCA.substring(0, 32);
//		if (measuredTCAfrequency.length() > 32) measuredTCAfrequency = measuredTCAfrequency.substring(0, 32);
//		s = s + "insert into STP_HEADER (stpDate,  id, resets, uptime, type, \n";
//		s = s + "sequenceNumber,\n";
//		s = s + "length,\n";
//		s = s + "source,\n";
//		s = s + "receiver,\n";
//		s = s + "frequency,\n";
//		s = s + "rx_location,\n";
//		s = s + "receiver_rf,\n";
//		s = s + "demodulator,\n";
//		s = s + "measuredTCA,\n";
//		s = s + "measuredTCAfrequency)\n";
//
//		s = s + "values (?, ?, ?, ?, ?,"
//				+ "?,?,?,?,?,?,?,?,?,?)";
//
//		java.sql.PreparedStatement ps = con.prepareStatement(s);
//		
//		ps.setString(1, dt);
//		ps.setInt(2, foxId);
//		ps.setInt(3, header.resets);
//		ps.setLong(4, header.uptime);
//		ps.setInt(5, header.type);
//		ps.setLong(6, sequenceNumber);
//		ps.setString(7, length);
//		ps.setString(8, source);
//		ps.setString(9, receiver);
//		ps.setString(10, frequency);
//		ps.setString(11, rx_location);
//		ps.setString(12, receiver_rf);
//		ps.setString(13, demodulator);
//		ps.setString(14, measuredTCA);
//		ps.setString(15, measuredTCAfrequency);
//
//		return ps;
//		
//	}
	
	public void load(BufferedReader input) throws IOException {
		
		String line = input.readLine();
		if (line != null) {
			StringTokenizer st = new StringTokenizer(line, ",");
			sequenceNumber = Long.parseLong(st.nextToken());
			satId = Integer.parseInt(st.nextToken());
			source = insertComma(st.nextToken());
			receiver = insertComma(st.nextToken());
			frequency = insertComma(st.nextToken());
			rx_location = insertComma(st.nextToken());
			receiver_rf = insertComma(st.nextToken());
			demodulator = insertComma(st.nextToken());
			stpDate = new Date(Long.parseLong(st.nextToken()));
			length = st.nextToken();
			int len = Integer.parseInt(length);
			bytes = new int[len];
			for (int i = 0; i < len; i++) {
				bytes[i] = Integer.parseInt(st.nextToken());
			}
		}
	}

	public int[] getServerBytes() {
		String header = getSTPCoreHeader();
		header = header + getSTPExtendedHeader();
		header = header + "\r\n";
		byte[] headerBytes = header.getBytes();

		int j = 0;
		int[] buffer = new int[headerBytes.length + bytes.length];
		for (int b : headerBytes)
			buffer[j++] = b;
		for (int b : bytes)
			buffer[j++] = b;

		return buffer;
	}

}
