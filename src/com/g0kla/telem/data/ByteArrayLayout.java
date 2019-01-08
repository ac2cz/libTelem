package com.g0kla.telem.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * 
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2019 amsat.org
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
 *
 * This class holds the layout for the telemetry for a given satellite.  It is loaded from a CSV file at program start
 * 
 * The layout will not change during the life of the program for a given satellite, so no provision is added for version control
 * or loading old formats.
 * 
 *
 */
public class ByteArrayLayout {
	public int NUMBER_OF_FIELDS = 0;
	public static int ERROR_POSITION = -1;
	
	public String fileName;
	public String name; // the name, which is stored in the spacecraft file and used to index the layouts
	public String parentLayout = null; // this is set to the value of the primary payload that spawns this
	
	public static final String NONE = "NONE";
	
	public String[] fieldName = null;  // name of the field that the bits correspond to
	public String[] type = null;  // the type of the data long, int, byte
	public int[] conversion = null; // the conversion routine to change raw bits into a real value
	public String[] fieldUnits = null; // the units as they would be displayed on a graph e.g. C for Celcius
	public int[] fieldByteLength = null; // the number of bytes in this field
	public String[] module = null; // the module on the display screen that this would be shown in e.g. Radio
	public int[] moduleNum = null; // the order that the module is displayed on the screen with 1-9 in the top and 10-19 in the bottom
	public int[] moduleLinePosition = null; // the line position in the module, starting from 1
	public int[] moduleDisplayType = null; // a code determining if this is displayed across all columns or in the RT, MIN, MAX columns.  Defined in DisplayModule

	public String[] shortName = null;
	public String[] description = null;
	
	private int numberOfBytes = 0;

	public static final int CONVERT_NONE = 0;
	
	ConversionTable conversions = null; // if present, this table holds the conversion factors
	
	/**
	 * Create a layout and load it from the file with the given path
	 * @param f
	 * @throws FileNotFoundException
	 * @throws LayoutLoadException 
	 * @throws IOException 
	 */
	public ByteArrayLayout(String name, String fileName) throws LayoutLoadException, IOException {
		this.name = name;
		load(fileName);
	}
	
	/**
	 * Calculate and return the total number of bits across all fields
	 * @return
	 */
	
	public int getMaxNumberOfBytes() {
		return numberOfBytes;
	}
	
	public boolean isSecondaryPayload() {
		if (parentLayout != null) return true;
		return false;
	}
	
	public void setConversionTable(ConversionTable ct) {
		conversions = ct;
	}
	
	public ConversionTable getConversionTable() {
		return conversions;
	}
	
	public boolean hasFieldName(String name) {
		for (int i=0; i < fieldName.length; i++) {
			if (name.equalsIgnoreCase(fieldName[i]))
				return true;
		}
		return false;
	}
	
	public int getConversionByName(String name) {
		int pos = ERROR_POSITION;
		for (int i=0; i < fieldName.length; i++) {
			if (name.equalsIgnoreCase(fieldName[i]))
				pos = i;
		}
		if (pos == ERROR_POSITION) {
			return ByteArrayLayout.CONVERT_NONE;
		} else {
			return (conversion[pos]);
		}
	}

	public String getUnitsByName(String name) {
		int pos = ERROR_POSITION;
		for (int i=0; i < fieldName.length; i++) {
			if (name.equalsIgnoreCase(fieldName[i]))
				pos = i;
		}
		if (pos == ERROR_POSITION) {
			return "";
		} else {
			return (fieldUnits[pos]);
		}
	}
	
	public int getPositionByName(String name) {
		int pos = ERROR_POSITION;
		for (int i=0; i < fieldName.length; i++) {
			if (name.equalsIgnoreCase(fieldName[i]))
				pos = i;
		}
		if (pos == ERROR_POSITION) {
			return ERROR_POSITION;
		} else {
			return pos;
		}
	}
	
	public String getShortNameByName(String name) {
		int pos = ERROR_POSITION;
		for (int i=0; i < fieldName.length; i++) {
			if (name.equalsIgnoreCase(fieldName[i]))
				pos = i;
		}
		if (pos == ERROR_POSITION) {
			return "";
		} else {
			return (shortName[pos]);
		}
	}

	public String getModuleByName(String name) {
		int pos = ERROR_POSITION;
		for (int i=0; i < fieldName.length; i++) {
			if (name.equalsIgnoreCase(fieldName[i]))
				pos = i;
		}
		if (pos == ERROR_POSITION) {
			return "";
		} else {
			return (module[pos]);
		}
	}

	protected void load(String fileName) throws LayoutLoadException, IOException {

		String line;
		
		BufferedReader dis = new BufferedReader(new FileReader(fileName));
		int field=0;

		line = dis.readLine(); // read the header, and only look at first item, the number of fields.
		StringTokenizer header = new StringTokenizer(line, ",");
		NUMBER_OF_FIELDS = Integer.valueOf(header.nextToken()).intValue();			
		fieldName = new String[NUMBER_OF_FIELDS];		
		type = new String[NUMBER_OF_FIELDS];		
		conversion = new int[NUMBER_OF_FIELDS];
		fieldByteLength = new int[NUMBER_OF_FIELDS];
		fieldUnits = new String[NUMBER_OF_FIELDS];
		module = new String[NUMBER_OF_FIELDS];
		moduleLinePosition = new int[NUMBER_OF_FIELDS];
		moduleNum = new int[NUMBER_OF_FIELDS];
		moduleDisplayType = new int[NUMBER_OF_FIELDS];
		shortName = new String[NUMBER_OF_FIELDS];
		description = new String[NUMBER_OF_FIELDS];

		while ((line = dis.readLine()) != null) {
			if (line != null) {
				StringTokenizer st = new StringTokenizer(line, ",");

				@SuppressWarnings("unused")
				int fieldId = Integer.valueOf(st.nextToken()).intValue();
				type[field] = st.nextToken();
				fieldName[field] = st.nextToken();
				fieldByteLength[field] = Integer.valueOf(st.nextToken()).intValue();
				fieldUnits[field] = st.nextToken();
				conversion[field] = Integer.valueOf(st.nextToken()).intValue();
				module[field] = st.nextToken();					
				moduleNum[field] = Integer.valueOf(st.nextToken()).intValue();
				moduleLinePosition[field] = Integer.valueOf(st.nextToken()).intValue();
				moduleDisplayType[field] = Integer.valueOf(st.nextToken()).intValue();
				shortName[field] = st.nextToken();
				try {
				description[field] = st.nextToken();
				} catch (NoSuchElementException e) {
					// ignore no description
				}
				field++;
			}
		}
		dis.close();

		if (NUMBER_OF_FIELDS != field) throw new LayoutLoadException("Error loading fields from " + fileName +
				". Expected " + NUMBER_OF_FIELDS + " fields , but loaded " + field);
		if (fieldByteLength != null) {
			numberOfBytes = 0;
			for (int i=0; i < fieldByteLength.length; i++) {
				numberOfBytes += fieldByteLength[i];
			}
		}
	}

	public String getTableCreateStmt(boolean storeMode) {
		String s = new String();
		s = s + "(captureDate varchar(14), id int, resets int, uptime bigint, type int, ";
		if (storeMode)
			s = s + "newMode int,";
		for (int i=0; i < fieldName.length; i++) {
			s = s + fieldName[i] + " int,\n";
		}
		// We use serial for the type, except for type 4 where we use it for the payload number.  This allows us to have
		// multiple high speed records with the same reset and uptime
		s = s + "PRIMARY KEY (id, resets, uptime, type))";
		return s;
	}

}
