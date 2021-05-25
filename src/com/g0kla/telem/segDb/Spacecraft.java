package com.g0kla.telem.segDb;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.g0kla.telem.data.BitArrayLayout;
import com.g0kla.telem.data.ByteArrayLayout;
import com.g0kla.telem.data.ConversionTable;
import com.g0kla.telem.data.DataRecord;
import com.g0kla.telem.data.EpochTime;
import com.g0kla.telem.data.LayoutLoadException;
import com.g0kla.telem.predict.FoxTLE;
import com.g0kla.telem.predict.PositionCalcException;
import com.g0kla.telem.predict.SortedTleList;

import uk.me.g4dpz.satellite.GroundStationPosition;
import uk.me.g4dpz.satellite.SatPos;
import uk.me.g4dpz.satellite.Satellite;
import uk.me.g4dpz.satellite.SatelliteFactory;
import uk.me.g4dpz.satellite.TLE;

public class Spacecraft implements Comparable<Spacecraft> {
	public Properties properties; // Java properties file for user defined values
	public File propertiesFile;
	SpacecraftPositionCache positionCache;
	String dirName; // the directory that the spacecraft config file is in.  Where the layouts are.  In the install dir
	String spacecraftLogDir; // the directory that the logs are in.  Where we put the TLEs.  Writable.
	GroundStationPosition position; // place to cache the ground station position for calculations

	boolean epochUsesT0 = false; // if false uptime is seconds since Unix epoch
	
	public static final int ERROR_IDX = -1;
	
	// Model Versions
	public static final int EM = 0;
	public static final int FM = 1;
	public static final int FS = 2;
	
	// Flattened ENUM for spacecraft name
	public static String[] modelNames = {
			"Engineering Model",
			"Flight Model",
			"Flight Spare"
	};
		
	public static String[] models = {
			"EM",
			"FM",
			"FS"
	};
	
	public int satId = 1;
	public int catalogNumber = 0;
	public String name = "";
	public String description = "";
	public int model;
	public int telemetryDownlinkFreqkHz = 145980;
	public int minFreqBoundkHz = 145970;
	public int maxFreqBoundkHz = 145990;
	public boolean layoutsUseBits = false;
	
	public boolean telemetryMSBfirst = true;
	public boolean ihuLittleEndian = true;
		
	public int numberOfLayouts = 0;
	public String[] layoutFilename;
	public ByteArrayLayout[] layout;
	 	
	public int numberOfLookupTables = 0;
	public String[] lookupTableFilename;
	public LookUpTable[] lookupTable;
	
	public String measurementsFileName;
	public String passMeasurementsFileName;
	public ByteArrayLayout measurementLayout;
	public ByteArrayLayout passMeasurementLayout;
	
	public static final String MEASUREMENTS = "measurements";
	public static final String PASS_MEASUREMENTS = "passmeasurements";
	
	public int numberOfFrameLayouts = 1;
	public String[] frameLayoutFilename;
	//public FrameLayout[] frameLayout;
	
	// User Config
	public boolean track = true; // default is we track a satellite
	public SatPos satPos; // cache the position when it gets calculated so others can read it
	public double satPosErrorCode; // Store the error code when we return null for the position
	
	private SortedTleList tleList; // this is a list of TLEs loaded from the history file.  We search this for historical TLEs
	
	public String conversionCoefficientsFileName;
	
	/*
	final String[] testTLE = {
            "AO-85",
            "1 40967U 15058D   16111.35540844  .00000590  00000-0  79740-4 0 01029",
            "2 40967 064.7791 061.1881 0209866 223.3946 135.0462 14.74939952014747"};
	*/
	
	public Spacecraft(String spacecraftLogDir, File fileName) throws LayoutLoadException, IOException {
		properties = new Properties();
		propertiesFile = fileName;	
		dirName = fileName.getParent();
		this.spacecraftLogDir = spacecraftLogDir;
		tleList = new SortedTleList(10);
		positionCache = new SpacecraftPositionCache(satId); // need to have loaded to set the id??
		load();
	}
	
	public void setGroundStationPosition(GroundStationPosition pos) {
		position = pos;
	}
		
	public int getLayoutIdxByName(String name) {
		for (int i=0; i<numberOfLayouts; i++)
			if (layout[i].name.equalsIgnoreCase(name))
				return i;
		return ERROR_IDX;
	}
	public int getLookupIdxByName(String name) {
		for (int i=0; i<numberOfLookupTables; i++)
			if (lookupTable[i].name.equalsIgnoreCase(name))
				return i;
		return ERROR_IDX;
	}
	
	public ByteArrayLayout getLayoutByName(String name) {
		int i = getLayoutIdxByName(name);
		if (i != ERROR_IDX)
				return layout[i];
		return null;
	}

	public LookUpTable getLookupTableByName(String name) {
		int i = getLookupIdxByName(name);
		if (i != ERROR_IDX)
				return lookupTable[i];
		return null;
	}

	public String getLayoutFileNameByName(String name) {
		int i = getLayoutIdxByName(name);
		if (i != ERROR_IDX)
				return layoutFilename[i];
		return null;
	}

	public String getLookupTableFileNameByName(String name) {
		int i = getLookupIdxByName(name);
		if (i != ERROR_IDX)
				return lookupTableFilename[i];
		return null;
	}

	
	/**
	 * TLEs are stored in the spacecraft directory in the logFileDirectory.
	 * @throws TleFileException 
	 * @throws IOException 
	 */
	public void loadTleHistory() throws TleFileException {
		String file = spacecraftLogDir + File.separator + name + ".tle";
		
		File f = new File(file);
		InputStream is = null;
		try {
			is = new FileInputStream(f);
			tleList = FoxTLE.importFoxSat(is);
		} catch (IOException e) {
			throw new TleFileException("TLE file not loaded: " + file);
			//e.printStackTrace(Log.getWriter()); // No TLE, but this is not viewed as fatal.  It should be fixed by Kep check
		} finally {
			try {
				if (is != null) is.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	private void saveTleHistory() throws IOException {
		String file = spacecraftLogDir + File.separator + name + ".tle";
		File f = new File(file);
		Writer output = new BufferedWriter(new FileWriter(f, false));
		for (FoxTLE tle : tleList) {
			//Log.println("Saving TLE to file: " + tle.toString() + ": " + tle.getEpoch());
			output.write(tle.toFileString());
		}
		output.flush();
		output.close();
	}
	
	/**
	 * We are passed a new TLE for this spacecarft.  We want to store it in the file if it is a TLE that we do not already have.
	 * @param tle
	 * @return
	 * @throws IOException 
	 */
	public boolean addTLE(FoxTLE tle) throws IOException {
		tleList.add(tle);
		saveTleHistory();
		return true;
	}
	
	protected TLE getTLEbyDate(DateTime dateTime) throws PositionCalcException {
		if (tleList == null) return null;
		TLE t = tleList.getTleByDate(dateTime);
		if (t==null) {
			satPosErrorCode = DataRecord.NO_TLE;
			throw new PositionCalcException(DataRecord.NO_TLE);
		}
		return t;
	}
	
	
	
	/**
	 * Calculate the position at a historical data/time
	 * Typically we don't call this directly.  Instead we call with the reset/uptime and hope that the value is already cached
	 * @param timeNow
	 * @return
	 * @throws PositionCalcException
	 */
	public SatPos calcSatellitePosition(DateTime timeNow) throws PositionCalcException {
		final TLE tle = getTLEbyDate(timeNow);
//		if (Config.debugFrames) Log.println("TLE Selected fOR date: " + timeNow + " used TLE epoch " + tle.getEpoch());
		if (tle == null) {
			satPosErrorCode = DataRecord.NO_TLE;
			throw new PositionCalcException(DataRecord.NO_TLE); // We have no keps
		}
		final Satellite satellite = SatelliteFactory.createSatellite(tle);
        final SatPos satellitePosition = satellite.getPosition(position, timeNow.toDate());
		return satellitePosition;
	}

	/**
	 * Calculate the current position and cache it
	 * @return
	 * @throws PositionCalcException
	 */
	protected SatPos calcualteCurrentPosition() throws PositionCalcException {
		DateTime timeNow = new DateTime(DateTimeZone.UTC);
		SatPos pos = null;
		pos = calcSatellitePosition(timeNow);
		satPos = pos;
//		if (Config.debugSignalFinder)
//			Log.println("Fox at: " + Spacecraft.latRadToDeg(pos.getAzimuth()) + " : " + DataRecord.Spacecraft(pos.getElevation()));
		return pos;
	}

	public SatPos getCurrentPosition() throws PositionCalcException {
		if (satPos == null) {
			throw new PositionCalcException(DataRecord.NO_POSITION_DATA);
		}
		return satPos;
	}
	
	public boolean aboveHorizon() {
		if (satPos == null)
			return false;
		return (Spacecraft.radToDeg(satPos.getElevation()) >= 0);
	}
	
		
	protected void load() throws LayoutLoadException, IOException {
		// try to load the properties from a file
		FileInputStream f = null;
		try {
			f=new FileInputStream(propertiesFile);
			properties.load(f);
			f.close();
		} catch (IOException e) {
			if (f!=null) try { f.close(); } catch (Exception e1) {};
			throw new LayoutLoadException("Could not load spacecraft files: " + propertiesFile.getAbsolutePath());
			
		}
		try {
			satId = Integer.parseInt(getProperty("satId"));
			catalogNumber = Integer.parseInt(getProperty("catalogNumber"));			
			name = getProperty("name");
			description = getProperty("description");
			model = Integer.parseInt(getProperty("model"));
			
			String b = getOptionalProperty("layoutsUseBits");
			if (b == null) 
				layoutsUseBits = false;
			else 
				layoutsUseBits = Boolean.parseBoolean(b);

			// Conversions
			conversionCoefficientsFileName = getProperty("conversionCoefficients");
			
			
			// Telemetry Layouts
			numberOfLayouts = Integer.parseInt(getProperty("numberOfLayouts"));
			layoutFilename = new String[numberOfLayouts];
			if (layoutsUseBits)
				layout = new BitArrayLayout[numberOfLayouts];
			else
				layout = new ByteArrayLayout[numberOfLayouts];
			for (int i=0; i < numberOfLayouts; i++) {
				layoutFilename[i] = dirName + File.separator + getProperty("layout"+i+".filename");
				String name = getProperty("layout"+i+".name");
				if (layoutsUseBits)
					layout[i] = new BitArrayLayout(name, layoutFilename[i]);
				else
					layout[i] = new ByteArrayLayout(name, layoutFilename[i]);
				layout[i].name = name;
				layout[i].parentLayout = getOptionalProperty("layout"+i+".parentLayout");
			}

			// Lookup Tables
			numberOfLookupTables = Integer.parseInt(getProperty("numberOfLookupTables"));
			lookupTableFilename = new String[numberOfLookupTables];
			lookupTable = new LookUpTable[numberOfLookupTables];
			for (int i=0; i < numberOfLookupTables; i++) {
				lookupTableFilename[i] = dirName + File.separator + getProperty("lookupTable"+i+".filename");
				lookupTable[i] = new LookUpTable(lookupTableFilename[i]);
				lookupTable[i].name = getProperty("lookupTable"+i);
			}
			
			String t = getOptionalProperty("track");
			if (t == null) 
				track = true;
			else 
				track = Boolean.parseBoolean(t);
		} catch (NumberFormatException nf) {
			throw new LayoutLoadException("Corrupt data found: "+ nf.getMessage() + "\nwhen processing Spacecraft file: " + propertiesFile.getAbsolutePath() );
		} catch (NullPointerException nf) {
			throw new LayoutLoadException("Missing data value: "+ nf.getMessage() + "\nwhen processing Spacecraft file: " + propertiesFile.getAbsolutePath() );		
		} catch (FileNotFoundException e) {
			throw new LayoutLoadException("File not found: "+ e.getMessage() + "\nwhen processing Spacecraft file: " + propertiesFile.getAbsolutePath());
		}
	}
	
	protected String getOptionalProperty(String key) throws LayoutLoadException {
		String value = properties.getProperty(key);
		if (value == null) {
			return null;
		}
		return value;
	}

	protected String getProperty(String key) throws LayoutLoadException {
		String value = properties.getProperty(key);
		if (value == null) {
			throw new LayoutLoadException("Missing data value: " + key + " when loading Spacecraft file: \n" + propertiesFile.getAbsolutePath() );
//			throw new NullPointerException();
		}
		return value;
	}
	protected void store() throws LayoutLoadException {
		FileInputStream f = null;
		try {
			f=new FileInputStream(propertiesFile);
			properties.store(new FileOutputStream(propertiesFile), "Fox 1 Telemetry Decoder Properties");
			f.close();
		} catch (FileNotFoundException e1) {
			if (f!=null) try { f.close(); } catch (Exception e2) {};
			throw new LayoutLoadException("ERROR Could not write spacecraft file. Check permissions on run directory or on the file");
		} catch (IOException e1) {
			if (f!=null) try { f.close(); } catch (Exception e3) {};
			throw new LayoutLoadException("ERROR writing spacecraft file");
		}
	}
	protected void save() {
		
		properties.setProperty("foxId", Integer.toString(satId));
		properties.setProperty("catalogNumber", Integer.toString(catalogNumber));
		properties.setProperty("name", name);
		properties.setProperty("description", description);
		properties.setProperty("model", Integer.toString(model));
		properties.setProperty("telemetryDownlinkFreqkHz", Integer.toString(telemetryDownlinkFreqkHz));
		properties.setProperty("minFreqBoundkHz", Integer.toString(minFreqBoundkHz));
		properties.setProperty("maxFreqBoundkHz", Integer.toString(maxFreqBoundkHz));
		properties.setProperty("maxFreqBoundkHz", Integer.toString(maxFreqBoundkHz));
		properties.setProperty("track", Boolean.toString(track));
		
	}
	
	public String getIdString() {
		String id = "??";
		id = Integer.toString(satId);

		return id;
	}
	
	public String toString() {
		return name;
	}
	
	@Override
	public int compareTo(Spacecraft s2) {
		return name.compareToIgnoreCase(s2.name);
	}
	
	public static final DateFormat timeDateFormat = new SimpleDateFormat("HH:mm:ss");
	public static final DateFormat dateDateFormat = new SimpleDateFormat("dd MMM yy");
	
	public boolean hasTimeZero() { 
//		if (timeZero == null) return false;
//		if (timeZero.size() == 0) return false;
//		return true;
		return false;
	}
	
	public boolean hasTimeZero(int reset) { 
//		if (timeZero == null) return false;
//		if (reset >= timeZero.size()) return false;
//		return true;
		return false;
	}
	
	public String[][] getT0TableData() {
//		if (timeZero == null) return null;
//		if (timeZero.size() == 0) return null;
//		String[][] data = new String[timeZero.size()][];
//		for (int i=0; i< timeZero.size(); i++) {
//			data[i] = new String[2];
//			data[i][0] = ""+i;
//			data[i][1] = getUtcDateForReset(i,0) + " " + getUtcTimeForReset(i,0);
//		}
//		return data;
		return null;
	}
	
	public String getUtcTimeForReset(int reset, long uptime) {
		Date dt = null;
		if (!epochUsesT0) {
			dt = new Date(uptime *1000);
		} else {
//			if (timeZero == null) return null;
//			if (reset >= timeZero.size()) return null;
//			dt = new Date(timeZero.get(reset) + uptime*1000);
		}
		timeDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String time = timeDateFormat.format(dt); 
		return time;
	}

	public String getUtcDateForReset(int reset, long uptime) {
		Date dt = null;
		if (!epochUsesT0) {
			dt = new Date(uptime *1000);
		} else {
//		if (timeZero == null) return null;
//		if (reset >= timeZero.size()) return null;
//		 dt = new Date(timeZero.get(reset) + uptime *1000);
		}
		dateDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		String time = dateDateFormat.format(dt);
		return time;
	}

	public Date getUtcForReset(int reset, long uptime) {
		if (!epochUsesT0) { // then we just have unit time in seconds
			Date dt = new Date(uptime*1000);
			return dt;
		} else {
//		if (timeZero == null) return null;
//		if (reset >= timeZero.size()) return null;
//		Date dt = new Date(timeZero.get(reset) + uptime*1000);
//		return dt;
			return null;
		}
	}

//	public Date getUtcForReset(int reset, long uptime) {
//		if (timeZero == null) return null;
//		if (reset >= timeZero.size()) return null;
//		Date dt = new Date(timeZero.get(reset) + uptime*1000);
//		return dt;
//	}
	
	public DateTime getUtcDateTimeForReset(int reset, long uptime) {
		if (!epochUsesT0) { // then we just have unit time in seconds
			Date dt = new Date(uptime*1000);
			DateTime dateTime = new DateTime(dt);
			return dateTime;
		} else {
//		if (timeZero == null) return null;
//		if (reset >= timeZero.size()) return null;
//		Date dt = new Date(timeZero.get(reset) + uptime*1000);
//		DateTime dateTime = new DateTime(dt); // FIXME - this date conversion is not working.  Need to understand how it works.		
//		return dateTime;
		}
		return null;
	}

	/**
	 * Given a date, what is the reset and uptime
	 * @param fromDate
	 * @return a FoxTime object with the reset and uptime
	 */
	public EpochTime getUptimeForUtcDate(Date fromDate) {
		if (!epochUsesT0) { // then we just have unit time in seconds
			int reset = 0;
			long uptime = fromDate.getTime()/1000;
			EpochTime ft = new EpochTime(reset, uptime);	
			return ft;
		} else {
//		if (timeZero == null) return null;
//		if (fromDate == null) return null;
//		if (timeZero.size() == 0) return null;
//		long dateTime = fromDate.getTime();
//		long T0 = -1;
//		int reset = 0;
//		long uptime = 0;
//		// Search T0.  It's short so we can scan the whole list
//		for (int i=0; i<timeZero.size(); i++) {
//			if (timeZero.get(i) > dateTime) {
//				// then we want the previous value
//				if (i==0) break; 
//				reset = i-1;
//				T0 = timeZero.get(reset);
//				break;
//			}
//			
//		}
//		if (T0 == -1) {
//			// return the last reset, we scanned the whole list
//			reset = timeZero.size()-1;
//			T0 = timeZero.get(reset);
//		}
//		
//		// Otherwise we have a valid reset, so calc the uptime, which is seconds from the T0 to the passed dateTime
//		uptime = dateTime - T0; // milliseconds
//		uptime = uptime / 1000; // seconds;
//		
//		EpochTime ft = new EpochTime(reset, uptime);
//		return ft;
		}
		return null;
	}

	
	public SatPos getSatellitePosition(int reset, long uptime) throws PositionCalcException {
		// We need to construct a date for the historical time of this WOD record
		DateTime timeNow = getUtcDateTimeForReset(reset, uptime);
		if (timeNow == null) return null;
		SatPos satellitePosition = positionCache.getPosition(timeNow.getMillis());
		if (satellitePosition != null) {
			return satellitePosition;
		}
		final TLE tle = getTLEbyDate(timeNow);
//		if (Config.debugFrames) Log.println("TLE Selected fOR date: " + timeNow + " used TLE epoch " + tle.getEpoch());
		if (tle == null) throw new PositionCalcException(DataRecord.NO_TLE); // We have no keps
		final Satellite satellite = SatelliteFactory.createSatellite(tle);
        satellitePosition = satellite.getPosition(position, timeNow.toDate());
//        Log.println("Cache value");
        positionCache.storePosition(timeNow.getMillis(), satellitePosition);
		return satellitePosition;
	}

	public static double radToDeg(Double rad) {
		return 180 * (rad / Math.PI);
	}
	public static double latRadToDeg(Double rad) {
		return radToDeg(rad);
	}

	public static double lonRadToDeg(Double rad) {
		double lon = radToDeg(rad);
		if (lon > 180)
			return lon -360;
		else
			return lon;
	}

	
}

