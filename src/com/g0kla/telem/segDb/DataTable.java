package com.g0kla.telem.segDb;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.util.StringTokenizer;

import com.g0kla.telem.data.ByteArrayLayout;
import com.g0kla.telem.data.DataLoadException;
import com.g0kla.telem.data.DataRecord;
import com.g0kla.telem.data.LayoutLoadException;


/**
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2018 amsat.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * This class is a flat file database for a single payload type.  It is referred to as a table, but
 * the actual data may be spread across several files on disk
 * 
 */
public class DataTable {

	public static final int MAX_DATA_LENGTH = 62;
	public static final int MAX_SEGMENT_SIZE = 1000;
	public static final int RESETS_COL = 0;
	public static final int UPTIME_COL = 1;
	public static final int DATA_COL = 2;
	public static final int LAT_COL = 3;
	public static final int LON_COL = 4;
	private SortedArrayList<TableSeg> tableIdx; // The map of data on disk and the parts of it that are loaded
	private static final int INITIAL_SIZE = 2; // inital number of table parts
	private String fileName; // this is the path and filename for this table
	private String tableName; // this is the base filename for this table
	private String dbDir;
	private ByteArrayLayout layout;
	private SortedDataRecordArrayList rtRecords; // this is the rtRecords that are loaded into memory
	private boolean updated = false;

	public DataTable(int size, String dbDir, ByteArrayLayout layout, String tableName) throws IOException {
		tableIdx = new SortedArrayList<TableSeg>(INITIAL_SIZE);
		this.tableName = tableName;
		this.layout = layout;
		this.dbDir = dbDir;
        fileName = dbDir +File.separator + tableName;
      
		rtRecords = new SortedDataRecordArrayList(size);
		loadIdx();
		updated = true;
	}
	
	public void setUpdated(boolean t) { updated = t; }
	public boolean getUpdated() { return updated; }
		
	public int getSize() { 
		int s=0;
		for (TableSeg t: tableIdx) {
			s = s + t.records;
		}
		return s; 
	}
	
	public boolean hasFrame(int id, long uptime, int resets) throws IOException, NumberFormatException, DataLoadException { 
		// Make sure the segment is loaded, so we can check
		@SuppressWarnings("unused")
		TableSeg seg = loadSeg(resets, uptime);
		return rtRecords.hasFrame(id, uptime, resets); }
	
	public DataRecord getLatest() throws IOException, NumberFormatException, DataLoadException {
		if (tableIdx.size() > 0) {
			TableSeg lastSeg = tableIdx.get(tableIdx.size()-1);
			if (!lastSeg.isLoaded()) {
				load(lastSeg);
				
			}
			if (rtRecords.size() == 0) return null;
			return rtRecords.get(rtRecords.size()-1);
		}
		return null;
	}
	
	/**
	 * Return the frame part with the passed reset and uptime or the first frame after it. Unless the "prev" boolean is passed
	 * then we return the previous frame if the required reset/uptime is not found 
	 * @param id
	 * @param uptime
	 * @param resets
	 * @return
	 * @throws IOException
	 * @throws DataLoadException 
	 * @throws LayoutLoadException 
	 * @throws NumberFormatException 
	 */
	public DataRecord getFrame(int id, long uptime, int resets, boolean prev) throws IOException, NumberFormatException, DataLoadException { 
		// Make sure the segment is loaded, so we can check
		@SuppressWarnings("unused")
		TableSeg seg = loadSeg(resets, uptime);
		if (seg.records == 0) return null;
		if (prev) {
			int i = rtRecords.getNearestPrevFrameIndex(id, uptime, resets); 
			if (i == -1) return null;
			return rtRecords.get(i);
		} else {
			int i = rtRecords.getNearestFrameIndex(id, uptime, resets); 
			if (i == -1) return null;
			return rtRecords.get(i);
		}
	}
	
	public DataRecord getFrame(int id, long uptime, int resets, int type, boolean prev) throws IOException, NumberFormatException, DataLoadException { 
		// Make sure the segment is loaded, so we can check
		@SuppressWarnings("unused")
		TableSeg seg = loadSeg(resets, uptime);
		if (prev) {
			int i = rtRecords.getNearestPrevFrameIndex(id, uptime, resets, type); 
			if (i == -1) return null;
			return rtRecords.get(i);
		} else {
			int i = rtRecords.getNearestFrameIndex(id, uptime, resets, type); 
			if (i == -1) return null;
			return rtRecords.get(i);
		}
	}
	
	public String[][] getPayloadData(int period, int id, int fromReset, long fromUptime, int length, boolean reverse) throws IOException, NumberFormatException, LayoutLoadException, DataLoadException {
		return getPayloadData(period, id, fromReset, fromUptime, length, false, reverse);
	}
	
	/**
	 * Return an array of payloads data with "period" entries for this sat id and from the given reset and
	 * uptime.
	 * @param period
	 * @param id
	 * @param fromReset
	 * @param fromUptime
	 * @return
	 * @throws IOException 
	 * @throws DataLoadException 
	 * @throws LayoutLoadException 
	 * @throws NumberFormatException 
	 */
	public String[][] getPayloadData(int period, int id, int fromReset, long fromUptime, int length, boolean returnType, boolean reverse) throws IOException, NumberFormatException, LayoutLoadException, DataLoadException {
		if (rtRecords == null) return null;
		loadSegments(fromReset, fromUptime, period, reverse);
		int start = 0;
		int end = 0;
		
		if (reverse) { // then we take records nearest the end
			start = rtRecords.size()-period;
			end = rtRecords.size();
		} else {
			// we need to find the start point
			start = rtRecords.getNearestFrameIndex(id, fromUptime, fromReset);
			if (start == -1 ) start = rtRecords.size()-period;
			end = start + period;
		}
		if (end > rtRecords.size()) end = rtRecords.size();
		if (end < start) end = start;
		if (start < 0) start = 0;
		if (start > rtRecords.size()) start = rtRecords.size();
		
		int[][] results = new int[end-start][];
		String[] upTime = new String[end-start];
		String[] resets = new String[end-start];
		String[] type = null;
		
		if (returnType)
			type = new String[end-start];
		
		int j = results.length-1;
		for (int i=end-1; i>= start; i--) {
			//System.out.println(rtRecords.size());
			results[j] = rtRecords.get(i).fieldValue;
			if (returnType)
				type[j] = ""+rtRecords.get(i).type; // get type returns a different type for some payloads, e.g. HerciPackets.  Reference directly
			upTime[j] = ""+rtRecords.get(i).uptime;
			resets[j--] = ""+rtRecords.get(i).resets;
		}
		
		// Create a results set, with reset, uptime and the data on the same line
		int offset = 2;
		if (returnType) offset = 3;
		String[][] resultSet = new String[end-start][length+offset];   // removed +1 to debug CAN Pkt display issue 8/14/2018
		for (int r=0; r< end-start; r++) {
			resultSet[r][0] = resets[r];
			resultSet[r][1] = upTime[r];
			if (returnType)
				resultSet[r][2] = type[r];
			for (int k=0; k<results[r].length; k++)
				resultSet[r][k+offset] = ""+results[r][k];
		}
		
		return resultSet;
	}
	
	/**
	 * Return a single field so that it can be graphed or analyzed
	 * @param name
	 * @param period
	 * @param id
	 * @param fromReset
	 * @param fromUptime
	 * @param positionData
	 * @param reverse - return the data from the end of the table in reverse order,such as when monitoring a live graph
	 * @return
	 * @throws IOException 
	 * @throws DataLoadException 
	 * @throws NumberFormatException 
	 */
	double[][] getGraphData(String name, int period,int id, int fromReset, long fromUptime, boolean raw, boolean positionData, boolean reverse) throws IOException, NumberFormatException, DataLoadException {
		loadSegments(fromReset, fromUptime, period, reverse);
		int start = 0;
		int end = 0;
		
		int COLUMNS = 3;
		double[] lat = null;
		double[] lon = null;
		if (positionData)
			COLUMNS = 5;
		
		if (reverse) { // then we take records nearest the end
			start = rtRecords.size()-period;
			end = rtRecords.size();
		} else {
			// we need to find the start point
			start = rtRecords.getNearestFrameIndex(id, fromUptime, fromReset);
			if (start == -1 ) start = rtRecords.size()-period;
			end = start + period;
		}
		if (end > rtRecords.size()) end = rtRecords.size();
		if (end < start) end = start;
		if (start < 0) start = 0;
		if (start > rtRecords.size()) start = rtRecords.size();
		double[] results = new double[end-start];
		double[] upTime = new double[end-start];
		double[] resets = new double[end-start];
		if (positionData) {
			lat = new double[end-start];
			lon = new double[end-start];
		}
		int j = results.length-1;
		for (int i=end-1; i>= start; i--) {
			//System.out.println(rtRecords.size());
			if (raw)
				results[j] = rtRecords.get(i).getRawValue(name);
			else
				results[j] = rtRecords.get(i).getDoubleValue(name);
			if (positionData) {
				lat[j] = rtRecords.get(i).satLatitude;
				lon[j] = rtRecords.get(i).satLongitude;
			}
			upTime[j] = rtRecords.get(i).uptime;
			resets[j--] = rtRecords.get(i).resets;
		}
		
		double[][] resultSet = new double[COLUMNS][end-start];
		resultSet[DATA_COL] = results;
		resultSet[UPTIME_COL] = upTime;
		resultSet[RESETS_COL] = resets;
		if (positionData) {
			resultSet[LAT_COL] = lat;
			resultSet[LON_COL] = lon;
			
		}
		return resultSet;
	}
	
	private TableSeg getSeg(int reset, long uptime) throws IOException {
		for (int i=tableIdx.size()-1; i>=0; i--) {
			if (tableIdx.get(i).fromReset <= reset && tableIdx.get(i).fromUptime <= uptime) {
				return tableIdx.get(i);
			}
		}
		// We could not find a valid Segment, so create a new segment at the head of the list
		TableSeg seg = new TableSeg(reset, uptime, tableName);
		tableIdx.add(seg);
		saveIdx();
		return seg;
	}
	
	/**
	 * Make sure the segment for this reset/uptime is loaded and is ready to receive data
	 * @param f
	 * @throws IOException 
	 * @throws DataLoadException 
	 * @throws LayoutLoadException 
	 * @throws NumberFormatException 
	 */
	private TableSeg loadSeg(int reset, long uptime) throws IOException, NumberFormatException, DataLoadException {
		TableSeg seg = getSeg(reset, uptime);
		if (seg.isLoaded()) return seg;
		load(seg);
		return seg;
	}
	
	/**
	 * Load all of the segments needed between two timestamps and return the number of records in that range
	 * Our first challenge is that either or both timestamps may be outside of a file.
	 * 
	 * @param reset
	 * @param uptime
	 * @param toReset
	 * @param toUptime
	 * @return the number of records in the range
	 * @throws IOException
	 * @throws DataLoadException 
	 * @throws LayoutLoadException 
	 * @throws NumberFormatException 
	 */
	protected int getNumberOfPayloadsBetweenTimestamps(int reset, long uptime, int toReset, long toUptime) throws IOException, NumberFormatException, LayoutLoadException, DataLoadException {
		int fromSeg = findFirstSeg(reset, uptime);
		int toSeg = findFirstSeg(toReset, toUptime);
		int number = 0;
		// Then we need to load segment at i and start counting from here, until we find the toReset and toUptime
		//System.err.println("Loading from seg: "+i);

		int i = fromSeg;
		while(i <= toSeg && i < tableIdx.size()) {
			if (!tableIdx.get(i).isLoaded())
				load(tableIdx.get(i));
			i++;
		}
		int id = rtRecords.get(0).id; // id is the same for all records in this table
		// Now all the segments are loaded that contain the data we want, so find the nearest records and count the distance between
		int start = rtRecords.getNearestFrameIndex(id, uptime, reset);
		int end = rtRecords.getNearestFrameIndex(id, toUptime, toReset);
		if (start < end)
			number = end - start;

		return number;
	}
	
	/**
	 * Search forwards through the segments to find the Segment with this reset/uptime
	 * We search from the earliest index records looking for the first instance where the reset or uptime is greater than the search point.
	 * We would then start loading from the previous record.
	 * @param reset
	 * @param uptime
	 * @return
	 */
	private int findFirstSeg(int reset, long uptime) {
		// load forwards from the relevant reset/uptime
		/* Logic is like this:
			reset x
			uptime y
			
			idx
			fromR	fromU
			0		100
			1		50
			2		900
			3		55
			
			case 1: 
			reset=1
			uptime=100
			We want to load 2/900 onwards
			So:
			reset < fromR, uptime then irrelevant
			
			case 2: - special case at the start
			reset = 0
			uptime = 1
			We want to load 0/100 onwards because we DO NOT HAVE data before it, otherwise it is case 4
			So: reset = fromR, y < fromU
							
			case 3: - special case at the end
			x=3
			y=100
			We want to load 3/55
			x = fromR, y > fromU - load current
			
			case 4: 
			reset=1
			uptime=0
			We want to load 0/100 because the data could be at the end
			x > fromR, y is irrelevent
			AND (x < next fromR OR (x = next from R AND y < uptime) )
		 * 
		 */
		
		boolean loadnow = false;
		for (int i=0; i< tableIdx.size(); i++) {
			if (!loadnow) { // we test the cases
				//case 1:
				if (tableIdx.get(i).fromReset > reset ) { // situation where the data is for a higher reset, so load from here always by default
					loadnow = true;
					//System.out.println("Case 1: " + i);
				} 
				// case 2:
				if (i == 0) 
					if (tableIdx.get(i).fromReset == reset) { // load this.  It might have the data we need and its the last segment
						loadnow = true;
					}
				//case 4:
				if (i < tableIdx.size()-1 &&  tableIdx.get(i).fromReset < reset && // this record has a lower reset
						(tableIdx.get(i+1).fromReset > reset || (tableIdx.get(i+1).fromReset == reset && tableIdx.get(i+1).fromUptime > uptime))) { // but the next record has higher reset or same reset and high uptime
					loadnow = true;
					//System.out.println("Case 4: " + i);
				}
				//case 4b:
				if (i < tableIdx.size()-1 &&  tableIdx.get(i).fromReset == reset && tableIdx.get(i).fromUptime < uptime && // this record has the same reset and uptime is less than the target
						(tableIdx.get(i+1).fromReset > reset || (tableIdx.get(i+1).fromReset == reset && tableIdx.get(i+1).fromUptime > uptime))) { // but the next record has higher reset or same reset and uptime higher
					loadnow = true;
					//System.out.println("Case 4b: " + i);

				}
				//case 3:
				if (i == tableIdx.size()-1 && tableIdx.get(i).fromReset <= reset) { // load this.  It might have the data we need and its the last segment
					loadnow = true;
					//System.out.println("Case 3: " + i);

				}
			}
			if (loadnow) return i;
		}
		return -99;
		
	}
	
	/*
	private int findLastSeg(int reset, long uptime) {
		return 0;
	}
	*/
	
	/**
	 * Load all of the segments needed so that "number" of records is available.  Used for plotting graphs.  If segments are missing then
	 * we do not create them
	 * @param reset
	 * @param uptime
	 * @param number
	 * @throws IOException
	 * @throws DataLoadException 
	 * @throws LayoutLoadException 
	 * @throws NumberFormatException 
	 */
	private void loadSegments(int reset, long uptime, int number, boolean reverse) throws IOException, NumberFormatException, DataLoadException {
		int total = 0;
		if (reverse) {
			// load backwards, but load in the right order so that the inserts into the records list are fast (append at end)
			// So we first calculate where to start
			int startIdx = 0;
			for (int i=tableIdx.size()-1; i>=0; i--) {
				total += tableIdx.get(i).records;
				if (total >= number) {
					startIdx = i;
					break;
				}
			}
			total = 0;
			// Now start index is the first segment we need to load, so now load them if needed
			for (int i=startIdx; i<tableIdx.size(); i++) {
				if (!tableIdx.get(i).isLoaded()) {
					load(tableIdx.get(i));
				}
				total += tableIdx.get(i).records;
				
			}
			//if (total >= number) System.err.println("Success we got: "+total+" records and needed "+number);
		} else {
			int i = findFirstSeg(reset, uptime);
			// Then we need to load segment at i and start counting from here
			//System.err.println("Loading from seg: "+i);
			if (i >= 0)
				while(i < tableIdx.size()) {
					if (!tableIdx.get(i).isLoaded())
						load(tableIdx.get(i));
					total += tableIdx.get(i++).records;
					if (total >= number+MAX_SEGMENT_SIZE) break; // add an extra segment because often we start from the segment before
				}

		}
	}
	
	/**
	 * Save a new record to disk		
	 * @param f
	 * @throws DataLoadException 
	 * @throws NumberFormatException 
	 */
	public boolean save(DataRecord f) throws IOException, NumberFormatException, DataLoadException {
		// Make sure this segment is loaded, or create an empty segment if it does not exist
		TableSeg seg = loadSeg(f.resets, f.uptime);
		if (rtRecords.add(f)) {
		//if (!rtRecords.hasFrame(f.id, f.uptime, f.resets)) {
			updated = true;
			if (seg.records == MAX_SEGMENT_SIZE) {
				// We need to add a new segment with this as the first record
				seg = new TableSeg(f.resets, f.uptime, tableName);
				tableIdx.add(seg);
			}
			save(f, dbDir+File.separator + seg.fileName);
			seg.records++;
			saveIdx();
			//return rtRecords.add(f);
			return true;
		} else {
			// Duplicate record
			return false;
		}
	}
	
	/**
	 * Load a table segment file from disk
	 * 
	 * @param log
	 * @throws IOException 
	 * @throws DataLoadException 
	 */
	public void load(TableSeg seg) throws IOException, NumberFormatException, DataLoadException {
		String log = dbDir+File.separator + seg.fileName;
        String line;
        createNewFile(log);
 
        BufferedReader dis = new BufferedReader(new FileReader(log));

        try {
        	while ((line = dis.readLine()) != null) {
        		if (line != null) {
        			DataRecord rt = addLine(line);
        			boolean ret = rtRecords.add(rt);
        		}
        	}
        	seg.setLoaded(true);
        	dis.close();
        
        } finally {
        	dis.close();
        }

	}

	/**
	 * Given a line of data, load it into the data record and store it in the table
	 * @param line
	 * @return
	 * @throws DataLoadException 
	 * @throws IOException 
	 */
	private DataRecord addLine(String line) throws IOException, DataLoadException {
		if (line.length() == 0) return null;
		DataRecord rt = new DataRecord(layout, line);
		return rt;
	}
	
	
	
	private boolean createNewFile(String log) throws IOException {
		File aFile = new File(log );
		if(!aFile.exists()){
				aFile.createNewFile();
				return true;
		}
		return false;
	}
	
	/**
	 * Save a payload to the a file
	 * @param f
	 * @param log
	 * @throws IOException
	 */
	private void save(DataRecord f, String log) throws IOException {
		boolean appendNewLine = false;
		if (!createNewFile(log)) {
			// the file was not new, so check to see if the last written line finsihed correctly, otherwise clean it up.
			if (!newLineExists(log) ) {
				appendNewLine = true;
			}
		}
		//Log.println("Saving: " + log);

		//use buffering and append to the existing file
		File aFile = new File(log );
		Writer output = new BufferedWriter(new FileWriter(aFile, true));
		try {
			if (appendNewLine)
				output.write( "\n" );
			output.write( f.toFile() + "\n" );
			output.flush();
		} finally {
			// Make sure it is closed even if we hit an error
			output.flush();
			output.close();
		}
	}
	
	public static boolean newLineExists(String log) throws IOException {
		File file = new File(log );
	    RandomAccessFile fileHandler = new RandomAccessFile(file, "r");
	    long fileLength = fileHandler.length() - 1;
	    if (fileLength < 0) {
	        fileHandler.close();
	        return true;
	    }
	    fileHandler.seek(fileLength);
	    byte readByte = fileHandler.readByte();
	    fileHandler.close();

	    if (readByte == 0xA || readByte == 0xD) {
	        return true;
	    }
	    return false;
	}

	/**
	 * Save Index to the a file
	 * @throws IOException
	 */
	private void saveIdx() throws IOException {
		File aFile = new File(fileName + ".idx" );
		createNewFile(fileName + ".idx");
		//Log.println("Saving: " + log);
		//use buffering and REPLACE the existing file
		Writer output = new BufferedWriter(new FileWriter(aFile, false));
		try {
			for (TableSeg seg: tableIdx) {

				output.write( seg.toFile() + "\n" );
				output.flush();
			}
		} finally {
			// Make sure it is closed even if we hit an error
			output.flush();
			output.close();
		}
	}
	
	
	/**
	 * Load the Index from disk
	 * @throws IOException 
	 */
	public void loadIdx() throws IOException, NumberFormatException {
        String line;
        File aFile = new File(fileName + ".idx" );
		if (createNewFile(fileName + ".idx")) {
			Writer output = new BufferedWriter(new FileWriter(aFile, true));
			output.close();
		}
 
        BufferedReader dis = new BufferedReader(new FileReader(aFile.getPath()));

        try {
        	while ((line = dis.readLine()) != null) {
        		if (line != null) {
        			StringTokenizer st = new StringTokenizer(line, ",");
        			
        			int resets = Integer.valueOf(st.nextToken()).intValue();
        			long uptime = Long.valueOf(st.nextToken()).longValue();
        			int records = Integer.valueOf(st.nextToken()).intValue();
        			String name = st.nextToken();
        			TableSeg seg = new TableSeg(resets, uptime, name, records);
    				tableIdx.add(seg);
        		}
        	}
        } finally {        	
        	dis.close();
        }
	}	
	
	public void remove() throws IOException, SecurityException {
		for (TableSeg seg: tableIdx)
			remove(dbDir+File.separator + seg.fileName);
		remove(fileName + ".idx");
	}
	
	/**
	 * Utility function to copy a file
	 * @param sourceFile
	 * @param destFile
	 * @throws IOException
	 */
	@SuppressWarnings("resource") // because we have a finally statement and the checker does not seem to realize that
	public static void copyFile(File sourceFile, File destFile) throws IOException {
	    if(!destFile.exists()) {
	        destFile.createNewFile();
	    }

	    FileChannel source = null;
	    FileChannel destination = null;

	    try {
	        source = new FileInputStream(sourceFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();
	        destination.transferFrom(source, 0, source.size());
	    }
	    finally {
	        if(source != null) {
	            source.close();
	        }
	        if(destination != null) {
	            destination.close();
	        }
	    }
	}
	
	public static void remove(String f) throws IOException, SecurityException {
		File file = new File(f);
		if (file.exists())
			if(file.delete()){
				; // success
			}else{
				throw new IOException("Could not delete file " + file.getName() + " Check the file system and remove it manually.");
			}
	}
}
