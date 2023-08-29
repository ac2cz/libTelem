package com.g0kla.telem.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TimeZone;

public class ConversionTable {
	String fileName;
	
	int NUMBER_OF_FIELDS = 0;
	public static final DateFormat dateFormatSecs = new SimpleDateFormat(
			"dd MMM yy HH:mm:ss", Locale.ENGLISH);
	
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
	
	static {
		dateFormatSecs.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	public ConversionTable(String fileName) throws LayoutLoadException, IOException {
		this.fileName = fileName;
		load(fileName);
	}

	public String getUnits(int conversion) {
		String s = units[conversion];
		return s;
	}

	public String getStringValue(int conversion, long rawValue) {
		double val = convertRawValue(conversion, rawValue);
		String s = "";
		if (units[conversion].equalsIgnoreCase("Counts")) {
			s = Integer.toString((int) val);
		} else if (units[conversion].equalsIgnoreCase("Boolean")) {
			if (val > 0) s = "True";
			else s = "False";
		} else if (units[conversion].equalsIgnoreCase("Time")) {
			int h = (int) (val / (60*60*1000));
			val = val - (h*60*60*1000);
			int m = (int) (val / (60*1000));
			int sec = (int) val - m * 60*1000;
			sec = sec / 1000;
			s = String.format("%02d:%02d:%02d", h,m,sec);
		} else if (units[conversion].equalsIgnoreCase("Date")) {
			Date dt = new Date((long) val*1000);
			s = dateFormatSecs.format(dt);
		} else if (units[conversion].equalsIgnoreCase("Raw8")) {
			s = toByteString(rawValue,8);
		} else if (units[conversion].equalsIgnoreCase("Raw10")) {
			s = toByteString(rawValue,10);
		} else if (units[conversion].equalsIgnoreCase("Bit2")) {
			s = intToBin((int)val,2);
		} else if (units[conversion].equalsIgnoreCase("Bit3")) {
			s = intToBin((int)val,3);
		} else if (units[conversion].equalsIgnoreCase("Bit4")) {
			s = intToBin((int)val,4);
		} else if (units[conversion].equalsIgnoreCase("Bit8")) {
			s = intToBin((int)val,8);
		} else if (units[conversion].equalsIgnoreCase("Bit16")) {
			s = intToBin((int)val,16);
		} else if (units[conversion].equalsIgnoreCase("Sint8")) {
			// Signed Magnitude with msb as the sign
			long bitMask = 0x80;
			long valueMask = 0x7F;
			long sval = rawValue & valueMask;
			long sign = rawValue & bitMask;
			if (sign == bitMask)
				sval = sval * -1;
			val = convertRawValue(conversion, sval);
			s = String.format("%1.2f", val);				
		} else if (units[conversion].equalsIgnoreCase("Sint16")) {
			// Signed Magnitude with msb as the sign
			long bitMask = 0x8000;
			long valueMask = 0x7FFF;
			long sval = rawValue & valueMask;
			long sign = rawValue & bitMask;
			if (sign == bitMask)
				sval = sval * -1;
			val = convertRawValue(conversion, sval);
			s = String.format("%1.2f", val);
		} else if (units[conversion].equalsIgnoreCase("MirADCSMode")) {
			String[] mode = {"standby","detumble","coarse-point","fine-point"};
			if (rawValue < mode.length)
				s = mode[(int) rawValue];
			else 
				s = ""+rawValue;
		} else if (units[conversion].equalsIgnoreCase("MirSpacecraftMode")) {
			String[] mode = {"reset/power-up","separation","safe","standby", "mission"};
			if (rawValue < mode.length)
				s = mode[(int) rawValue];
			else 
				s = ""+rawValue;
		} else if (units[conversion].equalsIgnoreCase("MirADCSState")) {
			String[] mode = {"nadir","sun","velocity","LLA","moon"};
			if (rawValue < mode.length)
				s = mode[(int) rawValue];
			else 
				s = ""+rawValue;
		} else if (units[conversion].equalsIgnoreCase("AX5043_PWRMODE")) {
			String[] mode = {"OFF","SLEEP","","","","STDBY","","FIFO","SYNTHRX",
					"RX","","WOR","SNTHTX","TX"};
			if (rawValue < mode.length)
				s = mode[(int) rawValue];
			else 
				s = ""+rawValue;
		} else {
			s = String.format("%1.2f", val);
		}
		return s;
	}
	
	public String intToBin(int word, int len) {
		boolean b[] = new boolean[len];
		for (int i=0; i<len; i++) {
			if (((word >>i) & 0x01) == 1) b[len-1-i] = true; else b[len-1-i] = false; 
		}
		String s = "";
		for (boolean bit : b)
			if (bit) s=s+"1"; else s=s+"0";
		return s;
	}
	
	public static String toByteString(long value, int len) {
		String s = "";
		for (int i=0; i<len; i++) {
			s = plainhex(value & 0xff) + s; // we get the least sig byte each time, so new bytes go on the front
			value = value >> 8 ;
		}
		return s;
	}
	
	public static String plainhex(long l) {
		return String.format("%2s", Long.toHexString(l)).replace(' ', '0');
		//return String.valueOf(n);
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
		long v = 555555555555L;
		String s = toByteString(v,5);
		System.out.println(s);
		
	//	ConversionTable ct = new ConversionTable("C:\\Users\\chris\\Google Drive\\AMSAT\\FalconSat-3\\telem\\Fs3coef.csv");
		ConversionTable ct = new ConversionTable("C:\\Users\\chris\\Desktop\\workspace\\Falcon\\spacecraft\\mir-sat-1-coef.csv");
		double val = ct.convertRawValue(70, -32767);
		System.out.println(val);
		System.out.println(ct.getStringValue(123, 2));
	}
}
