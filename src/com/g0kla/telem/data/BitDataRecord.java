package com.g0kla.telem.data;

import java.io.IOException;

public class BitDataRecord extends DataRecord {
	protected static final String PAD = "pad";
	public boolean[] rawBits = null;
	protected int bitPosition = 0; // position in the raw bits as we allocate them to the fields	
	public int numberBytesAdded = 0;
	boolean littleEndian = true;
	public static final boolean BIG_ENDIAN = false;
	public static final boolean LITTLE_ENDIAN = true;
	
	public BitDataRecord(BitArrayLayout layout, int id, int resets, long uptime, int type, int[] data, boolean littleEndian)
			throws LayoutLoadException, IOException {
		super(layout, id, resets, uptime, type, data);
		this.littleEndian = littleEndian;
	}

	public void parseData(int[] data) {
		rawBits = new boolean[((BitArrayLayout)layout).getMaxNumberOfBits()];
		for (int b : data)
			addNext8Bits((byte)(b & 0xff));
		copyBitsToFields();
	}
	
	public void resetBitPosition() {
		bitPosition = 0;
	}
	
	public void addNext8Bits(byte b) {
		if (littleEndian)
			littleEndianAddNext8Bits(b);
		else
			bigEndianAddNext8Bits(b);
				
	}
	
	/**
	 * Given a downloaded byte, add it to the raw bits array
	 * Store the least significant bit first, even though the satellite sends the msb first.
	 * This compensates for the little endian nature of the satellite.  It means that the lsb or
	 * the least significant byte is the first value we come across.
	 * @param b
	 */
	public void littleEndianAddNext8Bits(byte b) {
		for (int i=0; i<8; i++) {
			if ((b >> i & 1) == 1) 
				rawBits[i+numberBytesAdded*8] = true;
			else 
				rawBits[i+numberBytesAdded*8] = false;
		}
		numberBytesAdded++;	
	}
	
	/**
	 * We have bytes in big endian order, so we need to add the bits in a way
	 * that makes sense when we retrieve them sequentially
	 * So we add the msb first.  Then when 12 bits pulled in a row it will make sense.
	 * Note that if we pull a subset of 8 bits, then we have to be careful of the order.
	 * @param b
	 */
	public void bigEndianAddNext8Bits(byte b) {
		for (int i=0; i<8; i++) {
			if ((b >> i & 1) == 1) 
				rawBits[7-i+numberBytesAdded*8] = true;
			else 
				rawBits[7-i+numberBytesAdded*8] = false;
		}
		numberBytesAdded++;	
	}
	
	/**
	 *  Copy all of the bits from the raw byte frame to the fields according to the bit pattern in the fieldBitLength array
	 *  
	 */
	public void copyBitsToFields() {
		if (rawBits != null) { // only convert if we actually have a raw binary array.  Otherwise this was loaded from a file and we do not want to convert
			resetBitPosition();
			for (int i=0; i < layout.fieldName.length; i++) {
				if (layout.fieldName[i] == null || layout.fieldName[i].startsWith(PAD)) {  // ignore pad values and set the results to zero
					nextbits(layout.fieldLength[i]);
					fieldValue[i] = 0;
				} else
					fieldValue[i] = nextbits(layout.fieldLength[i]);
			}
		}
	}
	
	protected int nextbits(int n ) {
		if (littleEndian)
			return littleEndianNextbits(n);
		else
			return bigEndianNextbits(n);
	}

	/**
	 * Return the next n bits of the raw bit array, converted into an integer
	 * @param n
	 * @return
	 */
	protected int littleEndianNextbits(int n ) {
		int field = 0;
		
		boolean[] b = new boolean[n];
		for (int i=0; i < n; i++) {
			b[i] = rawBits[bitPosition+n-i-1];
			
		}
		bitPosition = bitPosition + n;
		field = binToInt(b);
		return field;
		
	}
	
	

	/**
	 * Return the next n bits of the raw bit array, converted into an integer
	 * We get them sequentially, with the msb first, so they just go into the 
	 * array in order
	 * @param n
	 * @return
	*/
	protected int bigEndianNextbits(int n ) {
		int field = 0;
		
		boolean[] b = new boolean[n];
		for (int i=0; i < n; i++) {
			b[i] = rawBits[bitPosition+i];
			
		}
		bitPosition = bitPosition + n;
		field = binToInt(b);
		return field;
		
	}
	
	/**
	 * Given a set of bits, convert it into an integer
	 * The most significant bit is in the lowest index of the array. e.g. 1 0 0 will have the value 4, with the 1 in array position 0
	 * We start from the highest array index, 
	 * @param word10
	 * @return
	 */
	public static int binToInt(boolean[] word10) {
		int d = 0;
	      
		for (int i=0; i<word10.length; i++) {
			int value = 0;
			if (word10[word10.length-1-i]) value = 1;
			d = d + (value << i);
		}
		return d;
	}
	
	/**
	 * Given an integer that represents an 8 bit word, convert it back to 8 bits
	 * This stores the bits in an array with the msb in position 0, so it prints
	 * in the right order
	 * @param word
	 * @return
	 */
	public static boolean[] intToBin8(int word) {
		boolean b[] = new boolean[8];
		for (int i=0; i<8; i++) {
			if (((word >>i) & 0x01) == 1) b[7-i] = true; else b[7-i] = false; 
		}
		return b;
	}
}
