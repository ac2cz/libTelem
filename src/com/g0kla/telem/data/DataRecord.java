package com.g0kla.telem.data;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class DataRecord implements Comparable<DataRecord> {
	public static final int ERROR_VALUE = 999999999; 
	public static final double NO_POSITION_DATA = -999;
	public static final double NO_TLE = -998;
	public static final double NO_T0 = -997;
	
	public ByteArrayLayout layout;
	public int[] fieldValue = null;
	
	// Identification
	public int id; // Used to seperate records if more than one spacecraft in the same database
	public int resets; // Used as part of the timebase if needed or set to 0
	public long uptime; // uptime of the spacecraft since the resets
	public int type; // identifier if multiple types of data records, especially if they have the same layout
	public double satLatitude;
	public double satLongitude;

	public DataRecord(ByteArrayLayout layout, String line) throws IOException, DataLoadException {
		if (layout == null) throw new DataLoadException("Missing layout");
		this.layout = layout;
		fieldValue = new int[layout.fieldName.length];
		load(line);
	}
	
	public DataRecord(ByteArrayLayout layout, int id, int resets, long uptime, int type, int[] data) throws LayoutLoadException, IOException {
		this.id = id;
		this.resets = resets;
		this.uptime = uptime;
		this.type = type;
		this.layout = layout;
		fieldValue = new int[layout.fieldName.length];
		
		int i = 0;
		for (int k=0; k < layout.fieldName.length && i < data.length; k++) {
			if (layout.type[k].equalsIgnoreCase("LONG")) { // 32 bit
				fieldValue[k] = (int) getLongValue(i, data); // unsigned long fits into signed int to save space
			}
			if (layout.type[k].equalsIgnoreCase("INT")) { // 16 bit
				fieldValue[k] = getIntValue(i, data);
			}
			if (layout.type[k].equalsIgnoreCase("BYTE")) { // 8 bit
				fieldValue[k] =  data[i];
			}
			//System.out.println(layout.fieldName[k] + ": " + fieldValue[k]);
			i = i + layout.fieldByteLength[k];
		}
	}
	
	public boolean hasFieldName(String name) {
		return layout.hasFieldName(name);
	}
	
	public int getRawValue(String name) {
		for (int i =0; i < layout.fieldName.length; i++)
			if (layout.fieldName[i].equalsIgnoreCase(name))
				return fieldValue[i];
		return 0;
	}

	public double getDoubleValue(String name) {
		for (int i =0; i < layout.fieldName.length; i++)
			if (layout.fieldName[i].equalsIgnoreCase(name)) {
				ConversionTable ct = layout.getConversionTable(); 
				return ct.convertRawValue(layout.conversion[i], fieldValue[i]);
			}
		return 0;
	}

	public String getStringValue(String name) {
		for (int i =0; i < layout.fieldName.length; i++)
			if (layout.fieldName[i].equalsIgnoreCase(name)) {
				ConversionTable ct = layout.getConversionTable(); 
				return ct.getStringValue(layout.conversion[i], fieldValue[i]);
			}
		return null;
	}

	
	public static long getLongValue(int offset, int[] data) {
		int[] by2 = {data[offset],data[offset+1],data[offset+2],data[offset+3]};
		long value = Tools.getLongFromBytes(by2);
		return value;
	}
	
	public static int getIntValue(int offset, int[] data) {
		int[] by2 = {data[offset],data[offset+1]};
		int value = Tools.getIntFromBytes(by2);
		return value;
	}

	public static int getByteValue(int offset, int[] data) {
		int value = data[offset];
		return value;
	}

	
	@Override
	public int compareTo(DataRecord p) {
		if (resets == p.resets && uptime == p.uptime && type == p.type) 
			return 0;
		else if (resets < p.resets)
			return -1;
		else if (resets > p.resets)
			return +1;
		else if (resets == p.resets && uptime == p.uptime) {
			if (type < p.type)
				return -1;
			if (type > p.type)
				return +1;
		} else if (resets == p.resets) {	
			if (uptime < p.uptime)
				return -1;
			if (uptime > p.uptime)
				return +1;
		} 
		return +1;
	}
	
	/**
	 * Parse this data record from a line of data, loaded from a file.  
	 * @param st
	 * @throws DataLoadException 
	 */
	protected void load(String line) throws DataLoadException {
		int i = 0;
		String s = null;
		StringTokenizer st = null;
		try {
			st = new StringTokenizer(line, ",");
			id = Integer.valueOf(st.nextToken()).intValue();
			resets = Integer.valueOf(st.nextToken()).intValue();
			uptime = Long.valueOf(st.nextToken()).longValue();
			type = Integer.valueOf(st.nextToken()).intValue();
		
			while((s = st.nextToken()) != null) {
				if (s.startsWith("0x")) {
					s = s.replace("0x", "");
					fieldValue[i++] = Integer.valueOf(s,16);
				} else
					fieldValue[i++] = Integer.valueOf(s).intValue();
			}
		} catch (NoSuchElementException e) {
			// we are done and can finish. The line was terminated and is corrupt.
		} catch (ArrayIndexOutOfBoundsException e) {
			// Something nasty happened when we were loading, so skip this record and log an error
			throw new DataLoadException("ERROR: Too many fields: Index out of bounds " +
					 "Could not load field "+i+ " SAT: " + this.id + " Reset:" + this.resets + " Up:" + this.uptime + " Type:" + this.type);
		} catch (NumberFormatException n) {
			throw new DataLoadException("ERROR: Invalid number:  " + n.getMessage() + " Could not load frame " + this.id + " " + this.resets + " " + this.uptime + " " + this.type);
		}
	}
	
	/**
	 * Output the set of fields in this framePart as a set of comma separated values in a string.  This 
	 * can then be written to a file
	 * @return
	 */
	public String toFile() {
		String s = new String();
		s = s + id + "," + resets + "," + uptime + "," + type + ",";
		
		for (int i=0; i < layout.fieldName.length-1; i++)
			s = s + fieldValue[i] + ",";
		s = s + fieldValue[layout.fieldName.length-1];
			
		return s;
	}
	
	public String getInsertStmt() {
		String s = new String();
		s = s + " (id, resets, uptime, type, \n";
		for (int i=0; i < layout.fieldName.length-1; i++) {
			s = s + layout.fieldName[i] + ",\n";
		}
		s = s + layout.fieldName[layout.fieldName.length-1] + ")\n";
		s = s + "values (" + this.id + ", " + this.resets + ", " + this.uptime + ", " + this.type + ",\n";
		for (int i=0; i < fieldValue.length-1; i++) {
			s = s + fieldValue[i] + ",\n";
		}
		s = s + fieldValue[fieldValue.length-1] + ")\n";
		return s;
	}
//	public String toString() {
//		String s = new String();
//		for (int i=0; i < layout.fieldName.length; i++) {
//			s = s + layout.fieldName[i] + ": " + fieldValue[i] + ",   ";
//			if ((i+1)%6 == 0) s = s + "\n";
//		}
//		return s;
//	}
	public String toHeaderString() {
		String s = new String();
		for (int i=0; i < layout.fieldName.length-1; i++) {
				s = s + layout.shortName[i] + ", ";
		}
		s = s + layout.shortName[layout.fieldName.length-1];

		return s;
	}
		
	public String toString() {
		String s = new String();
		ConversionTable ct = layout.getConversionTable(); 
		for (int i=0; i < layout.fieldName.length-1; i++) {
			if (ct != null)
				s = s + ct.getStringValue(layout.conversion[i], fieldValue[i]) + ", ";
			else
				s = s + fieldValue[i] + ", ";
		}if (ct != null)
			s = s + ct.getStringValue(layout.conversion[layout.fieldName.length-1], fieldValue[layout.fieldName.length-1]);
		else
			s = s + fieldValue[layout.fieldName.length-1];
		return s;
	}
}
