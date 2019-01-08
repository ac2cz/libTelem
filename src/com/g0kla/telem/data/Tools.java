package com.g0kla.telem.data;

public class Tools {

	public static long getLongFromBytes(int[] by) {
		long value = 0;
		for (int i = 0; i < by.length; i++) {
			value += (by[i] & 0xffL) << (8 * i);
		}
		return value;
	}

	public static int getIntFromBytes(int[] by) {
		int value = 0;
		for (int i = 0; i < by.length; i++) {
			value += (by[i] & 0xff) << (8 * i);
		}
		return value;
	}

	public static int[] littleEndian2(long in) {
		int[] b = new int[2];

		b[1] = (int)((in >> 8) & 0xff);
		b[0] = (int)((in >> 0) & 0xff);
		return b;
	}

	public static int[] littleEndian4(long in) {
		int[] b = new int[4];

		b[3] = (int) ((in >> 24) & 0xff);
		b[2] = (int) ((in >> 16) & 0xff);
		b[1] = (int) ((in >> 8) & 0xff);
		b[0] = (int) ((in >> 0) & 0xff);
		return b;
	}

	// Return MSB in 0 and LSB in 7
	public static boolean[] intToBin8(int word) {
		return intToBin(word, 8);
	}
	
	public static boolean[] intToBin(int word, int bits) {
		boolean b[] = new boolean[bits];
		for (int i=0; i<bits; i++) {
			if (((word >>i) & 0x01) == 1) b[i] = true; else b[i] = false; 
		}
		return b;
	}

	public static String printBin(int word, int bits) {
		boolean[] b = intToBin(word, bits);
		String s = printBin(b);
		return s;
	}

	public static String printBin(boolean [] bits) {
		String s = "";
		for (int i=7; i>=0; i--) {
			s = s + (bits[i] ? 1 : 0);
		}
		return s;
	}
}
