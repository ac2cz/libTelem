package com.g0kla.telem.data;

import java.io.IOException;

public class BitArrayLayout extends ByteArrayLayout {

	private int numberOfBits = 0;
	
	public BitArrayLayout(String name, String fileName) throws LayoutLoadException, IOException {
		super(name, fileName);
		for (int i: fieldLength)
			numberOfBits += i;
		numberOfBytes = (int)(Math.ceil(numberOfBits / 8.0));
	}

	/**
	 * Calculate and return the total number of bits across all fields
	 * @return
	 */
	public int getMaxNumberOfBits() {
		return numberOfBits;
	}

}
