package com.g0kla.telem.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;

/**
 * 
 * @author chris.e.thompson g0kla/ac2cz
 *
 *         Copyright (C) 2019 amsat.org
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

	// Identification
	public int id; // Used to seperate records if more than one spacecraft in the same database
	public int resets; // Used as part of the timebase if needed or set to 0
	public long uptime; // uptime of the spacecraft since the resets
	public int type; // identifier if multiple types of data records, especially if they have the same layout

	public String receiver = NONE; // unique name (usually callsign) chosen by
	// the user. May vary over life of program
	// usage, so stored
	private String frequency = NONE; // frequency when this frame received
	public String source; // The frame source subsystem
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
		this.id = id;
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

	/**
	 * Constructor to create a new STP record from a kiss frame
	 * @param id
	 * @param stpDate
	 * @param callsign
	 * @param location
	 * @param stationDetails
	 * @param software
	 * @param recordSource
	 * @param sequenceNum
	 * @param data
	 */
	public STP(int id, Date stpDate, String callsign, String location, String stationDetails, 
			String software, String recordSource, long sequenceNum, int[] data) {
		this.id = id;
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
	
	/**
	 * Constructor for a new STP record read as the next line from a file
	 * @param file
	 * @throws IOException
	 */
	public STP(BufferedReader file) throws IOException {
		load(file);
	}

	/**
	 * Constructor for a new STP record given bytes received by the server
	 * @param data
	 * @throws IOException
	 */
	public STP(int[] data) throws IOException {
		parseFromBytes(data);
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
		output.write(Long.toString(sequenceNumber) + "," + id + ","
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
	public STP(String fileName) throws IOException, StpFileProcessException {
		boolean done = false;
		int c,p = 0;
		File infile = new File(fileName);
		FileInputStream in = new FileInputStream(fileName);
		int[] data = new int[(int) infile.length()];
		try {
			while ((c = in.read()) != -1) {
				data[p++] = c;
			}			
		} finally {
			in.close();
		}
		in.close();

		parseFromBytes(data);

	}

	public void parseFromBytes(int[] data) {
		int c;
		int lineLen = 0;

		boolean done = false;
		boolean readingKey = true;
		String key = "";
		String value = "";
		int[] rawFrame = null;
		int length = 0;
		receiver = null;
		stpDate = null;
		frequency = NONE; // frequency when this frame received
		source = null; // The frame source subsystem
		rx_location = NONE; // the lat, long and altitude
		receiver_rf = NONE; // human description of the receiver
		demodulator = null; // will contain Config.VERSION
		sequenceNumber = Sequence.ERROR_NUMBER;

		measuredTCA = NONE; // time of TCA
		measuredTCAfrequency = NONE;

		boolean firstColon = true;
		int p = 0; // position in the data
		char ch;
		// Read the file

		while (!done && (c = data[p++]) != -1) {
			ch = (char) c;
			//System.out.print(ch);

			if (c == 58 && firstColon) { // ':'
				firstColon = false;
				c = data[p++]; // consume the space
				c = data[p++];
				ch = (char) c; // set ch to the first character
				readingKey = false;
			}
			if ( (c == 13 || c == 10)) { // CR or LF
				c = data[p++]; // consume the lf
				if ((length != 0) && lineLen == 1) {
					// then we are ready to process
					rawFrame = new int[length/8];
					for (int i=0; i<length/8; i++) {
						rawFrame[i] = data[p++];
					}
					done = true;
				} else {
					// It was a header line
					readingKey = true;
					firstColon = true;
					if (key.startsWith("Length")) {
						this.length = value;
						length = Integer.parseInt(value);
					}
					if (key.equalsIgnoreCase("Receiver")) {
						receiver = value;
					}
					if (key.equalsIgnoreCase("Frequency")) {
						frequency = value;
					}
					if (key.equalsIgnoreCase("Rx-location")) {
						rx_location = value;
					}
					if (key.equalsIgnoreCase("Source")) {
						source = value;
					}
					if (key.equalsIgnoreCase("Receiver-RF")) {
						receiver_rf = value;
					}
					if (key.equalsIgnoreCase("Demodulator")) {
						demodulator = value;
					}
					if (key.endsWith("Sequence")) {
						sequenceNumber = Long.parseLong(value);
					}
					if (key.equalsIgnoreCase("MeasuredTCA")) {
						measuredTCA = value;
					}
					if (key.equalsIgnoreCase("MeasuredTCAfrequency")) {
						measuredTCAfrequency = value;
					}
					if (key.startsWith("Date")) {
						String dt = value.replace(" UTC", "");
						stpDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
						try {
							stpDate = stpDateFormat.parse(dt);
						} catch (ParseException e) {
							stpDate = null;
						} catch (NumberFormatException e) {
							stpDate = null;
						} catch (Exception e) { // we can get other unusual exceptions such as ArrayIndexOutOfBounds...
							stpDate = null;
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
		if (stpDate == null)
			stpDate = new Date(0);
		bytes = rawFrame;

	}

	public static String getTableCreateStmt() {
		String s = new String();
		s = s + "(stpDate varchar(35), id int, "
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
		s = s + "PRIMARY KEY (stpDate, id, receiver))";
		return s;
	}
	
	public PreparedStatement getPreparedInsertStmt(Connection con) throws SQLException {
				
		//java.sql.Date sqlDate = new java.sql.Date(stpDate.getTime());
		//FIXME - need to make this a proper date in the DB
		String dt = "";
		if (stpDate != null)
			dt = stpDateFormat.format(stpDate);
		String s = new String();
		if (demodulator.length() > 99) demodulator = demodulator.substring(0, 99);
		if (source.length() > 32) source = source.substring(0, 32);
		if (receiver.length() > 32) receiver = receiver.substring(0, 32);
		if (frequency.length() > 32) frequency = frequency.substring(0, 32);
		if (rx_location.length() > 32) rx_location = rx_location.substring(0, 32);
		if (receiver_rf.length() > 50) receiver_rf = receiver_rf.substring(0, 50);
		if (measuredTCA.length() > 32) measuredTCA = measuredTCA.substring(0, 32);
		if (measuredTCAfrequency.length() > 32) measuredTCAfrequency = measuredTCAfrequency.substring(0, 32);
		s = s + "insert into STP_HEADER (stpDate,  id, \n";
		s = s + "sequenceNumber,\n";
		s = s + "length,\n";
		s = s + "source,\n";
		s = s + "receiver,\n";
		s = s + "frequency,\n";
		s = s + "rx_location,\n";
		s = s + "receiver_rf,\n";
		s = s + "demodulator,\n";
		s = s + "measuredTCA,\n";
		s = s + "measuredTCAfrequency)\n";

		s = s + "values (?, ?,"
				+ "?,?,?,?,?,?,?,?,?,?)";

		java.sql.PreparedStatement ps = con.prepareStatement(s);
		
		ps.setString(1, dt);
		ps.setInt(2, this.id);
		ps.setLong(3, sequenceNumber);
		ps.setString(4, length);
		ps.setString(5, source);
		ps.setString(6, receiver);
		ps.setString(7, frequency);
		ps.setString(8, rx_location);
		ps.setString(9, receiver_rf);
		ps.setString(10, demodulator);
		ps.setString(11, measuredTCA);
		ps.setString(12, measuredTCAfrequency);

		return ps;
		
	}
	
	public void load(BufferedReader input) throws IOException {
		
		String line = input.readLine();
		if (line != null) {
			StringTokenizer st = new StringTokenizer(line, ",");
			sequenceNumber = Long.parseLong(st.nextToken());
			id = Integer.parseInt(st.nextToken());
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
	
	public String toString() {
		String s = "";
		s = getSTPCoreHeader();
		s = s + getSTPExtendedHeader();
		s = s + "\n";
		return s;
	}

}
