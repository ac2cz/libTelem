package com.g0kla.telem.segDb;

import java.io.File;
import java.io.IOException;

import com.g0kla.telem.data.ByteArrayLayout;
import com.g0kla.telem.data.DataLoadException;
import com.g0kla.telem.data.DataRecord;
import com.g0kla.telem.data.LayoutLoadException;

public class SatTelemStore {
	String dbDir;
	DataTable[] records;
	ByteArrayLayout[] layouts;
	private static final int INIT_SIZE = 1000;
	private static final int ERROR_IDX = -999;

	/**
	 * Given a set of layouts and a name, create a datastore with a set of files
	 * @param id
	 * @param name
	 * @param layouts
	 * @throws IOException 
	 * @throws LayoutLoadException 
	 */
	public SatTelemStore(int id, String dbDir, ByteArrayLayout[] layouts) throws IOException, LayoutLoadException {
		this.dbDir = dbDir;
		makeDir(dbDir);
		this.layouts = layouts;
		records = new DataTable[layouts.length];
		for (int i=0; i<layouts.length; i++)
			records[i] = new DataTable(INIT_SIZE, dbDir, layouts[i], layouts[i].name);
	}
	
	/**
	 * Make the database directory if needed.  Check to see if we have existing legacy data and run the conversion if we do
	 * @param dir
	 * @throws LayoutLoadException 
	 */
	private boolean makeDir(String dir) throws LayoutLoadException {
		
		File aFile = new File(dir);
		if(!aFile.isDirectory()){
			aFile.mkdir();
			if(!aFile.isDirectory()){
				throw new LayoutLoadException("ERROR can't create the directory: " + aFile.getAbsolutePath() +  
						"\nAny decoded payloads will not be saved to disk\n");
			}
			return true;
		}
		
		return false;
	}
	
	public int getLayoutIdxByName(String name) {
		for (int i=0; i<layouts.length; i++)
			if (layouts[i].name.equalsIgnoreCase(name))
				return i;
		return ERROR_IDX;
	}
	
	/**
	 * Add the frame to the correct array and file
	 * @param f
	 * @return
	 * @throws IOException 
	 * @throws DataLoadException 
	 * @throws NumberFormatException 
	 */
	public boolean add(DataRecord f) throws IOException, NumberFormatException, DataLoadException {
		boolean ret = false;
		int i = getLayoutIdxByName(f.layout.name);
		if (i != ERROR_IDX) {
			ret = records[i].save(f); 
		}
		return ret;
		
	}
	
	public void setUpdatedAll() {
		for (int i=0; i<layouts.length; i++)
			records[i].setUpdated(true);
	}

	public boolean getUpdated(String layout) { 
		int i = getLayoutIdxByName(layout);
		if (i != ERROR_IDX)
			return records[i].getUpdated(); 
		return false;
	}
	public void setUpdated(String layout, boolean u) {
		int i = getLayoutIdxByName(layout);
		if (i != ERROR_IDX)
			records[i].setUpdated(u); 
	}
	
	public int getNumberOfFrames() {
		int total = 0;
		for (int i=0; i<records.length; i++) {
				total += records[i].getSize();
		}
		return total;
	}
	
	public int getNumberOfFrames(String layout) { 
		for (int i=0; i<layouts.length; i++) {
			if (layouts[i].name.equalsIgnoreCase(layout)) {
				return records[i].getSize();
			}
		}
		return 0;
	}
	
	public DataRecord getLatest(int id, int reset, long uptime, int type, String layout, boolean prev) throws IOException, NumberFormatException, DataLoadException {
		int i = getLayoutIdxByName(layout);
		if (i != ERROR_IDX)
			return records[i].getFrame(id, uptime, reset, type, prev); 
		return null;
	}

	public DataRecord getLatest(String layout) throws IOException, NumberFormatException, DataLoadException {
		int i = getLayoutIdxByName(layout);
		if (i != ERROR_IDX)
			return records[i].getLatest(); 
		return null;
	}
	
	/**
	 * Try to return an array with "period" entries for this attribute, starting with the most 
	 * recent
	 * 
	 * @param name
	 * @param period
	 * @param positionData - returns lat/lon with data if available
	 * @return
	 * @throws IOException 
	 * @throws DataLoadException 
	 * @throws NumberFormatException 
	 */
	public double[][] getGraphData(String name, int period, int id, int fromReset, long fromUptime, String layout, boolean raw, boolean positionData, boolean reverse) throws IOException, NumberFormatException, DataLoadException {
		int i = getLayoutIdxByName(layout);
		if (i != ERROR_IDX)
			return records[i].getGraphData(name, period, id, fromReset, fromUptime, raw, positionData, reverse);
		return null;
	}
	
	public String[][] getTableData(int period, int id, int fromReset, long fromUptime, boolean returnType, boolean reverse, String layout) throws IOException, NumberFormatException, LayoutLoadException, DataLoadException {
		int i = getLayoutIdxByName(layout);
		if (i != ERROR_IDX)
			return records[i].getPayloadData(period, id, fromReset, fromUptime, layouts[i].fieldName.length, returnType, reverse);
		return null;	
	}
		
}
