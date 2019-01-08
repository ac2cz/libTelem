package com.g0kla.telem.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class ConversionTable {
	String fileName;
	
	int NUMBER_OF_FIELDS = 0;
	
	public String[] fieldName = null;  // name of the field that the bits correspond to
	public String[] fieldName2 = null;  // name of the field that the bits correspond to
	public String[] name = null;  // name of the field that the bits correspond to
	public double[] a = null; 
	public double[] b = null; 
	public double[] c = null; 
	public double[] d = null; 
	public double[] e = null; 
	public double[] f = null; 
	public String[] units = null;  // name of the field that the bits correspond to
	public int[] low = null;  // name of the field that the bits correspond to
	public int[] high = null;  // name of the field that the bits correspond to
	public String[] description = null;  // name of the field that the bits correspond to

	public static final String TERMINATOR = "NOTES:"; // File is done when we detect this in the first row
	
	public ConversionTable(String fileName) throws LayoutLoadException, IOException {
		this.fileName = fileName;
		load(fileName);
	}
	
	public String getStringValue(int conversion, long rawValue) {
		double val = convertRawValue(conversion, rawValue);
		String s = "";
		if (units[conversion].equalsIgnoreCase("Counts")) {
			s = Integer.toString((int) val);
		} else {
			s = String.format("%1.2f", val);
			s = s + " " + units[conversion];
		}
		return s;
	}

	public double convertRawValue(int conversion, long rawValue) {
		if (conversion < 0 || conversion > NUMBER_OF_FIELDS)
			throw new NumberFormatException("Conversion number invalid");
		double A = a[conversion];
		double B = b[conversion];
		double C = c[conversion];
		double D = d[conversion];
		double E = e[conversion];
		double F = f[conversion];
		double val = A + B * rawValue + C * Math.pow(rawValue, 2) + D * Math.pow(rawValue, 3) + E * Math.pow(rawValue, 4)
			+ F * Math.pow(rawValue, 5);
		return val;
	}
	
	protected void load(String file) throws LayoutLoadException, IOException {
		String line;
		ArrayList<String> lines = new ArrayList<String>();
		
		BufferedReader dis = new BufferedReader(new FileReader(file));
		int field=0;
		boolean reading = true;
		while (reading) {
			line = dis.readLine(); // read the file and count the number of fields.
			if (line.startsWith(TERMINATOR)) {
				reading = false;
			} else {
				lines.add(line);
				field++;
			}
		}
		if (reading == true) throw new LayoutLoadException("Could not find end of Conversion file");
		
		NUMBER_OF_FIELDS = field;	
		fieldName = new String[NUMBER_OF_FIELDS];		
		fieldName2 = new String[NUMBER_OF_FIELDS];		
		name = new String[NUMBER_OF_FIELDS];		
		a = new double[NUMBER_OF_FIELDS];
		b = new double[NUMBER_OF_FIELDS];
		c = new double[NUMBER_OF_FIELDS];
		d = new double[NUMBER_OF_FIELDS];
		e = new double[NUMBER_OF_FIELDS];
		f = new double[NUMBER_OF_FIELDS];
		units = new String[NUMBER_OF_FIELDS];
		low = new int[NUMBER_OF_FIELDS];
		high = new int[NUMBER_OF_FIELDS];
		description = new String[NUMBER_OF_FIELDS];

		field = 0;
		for (field = 0; field < NUMBER_OF_FIELDS; field++) {
			line = lines.get(field);
			if (line != null) {
				StringTokenizer st = new StringTokenizer(line, ",");

				fieldName[field] = st.nextToken();
				fieldName2[field] = st.nextToken();
				name[field] = st.nextToken();
				a[field] = Double.valueOf(st.nextToken()).doubleValue();
				b[field] = Double.valueOf(st.nextToken()).doubleValue();
				c[field] = Double.valueOf(st.nextToken()).doubleValue();
				d[field] = Double.valueOf(st.nextToken()).doubleValue();
				e[field] = Double.valueOf(st.nextToken()).doubleValue();
				f[field] = Double.valueOf(st.nextToken()).doubleValue();
				units[field] = st.nextToken();
				low[field] = Integer.valueOf(st.nextToken()).intValue();
				high[field] = Integer.valueOf(st.nextToken()).intValue();
				try {
				description[field] = st.nextToken();
				} catch (NoSuchElementException e) {
					// field not populated
				}
			}
		}
		dis.close();

		if (NUMBER_OF_FIELDS != field) throw new LayoutLoadException("Error loading fields from " + file +
				". Expected " + NUMBER_OF_FIELDS + " fields , but loaded " + field);
	}
	
	public static void main(String[] args) throws LayoutLoadException, IOException {
		ConversionTable ct = new ConversionTable("C:\\Users\\chris\\Google Drive\\AMSAT\\FalconSat-3\\telem\\Fs3coef.csv");
		double val = ct.convertRawValue(3, 1353);
		System.out.println(val);
		System.out.println(ct.getStringValue(3, 1353));
	}
}
