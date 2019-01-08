package com.g0kla.telem.data;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ToolsTest {

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void testGetLongFromBytes() {
		int[] by = {0x01, 0x01,0x01, 0x01};
		long l = Tools.getLongFromBytes(by);
		System.out.println(l);
		assertEquals(l, 16843009);
	}

	@Test
	void testGetIntFromBytes() {
		int[] by = {0x01, 0x01};
		int l = Tools.getIntFromBytes(by);
		System.out.println(l);
		assertEquals(257, l);
	}

	@Test
	void testLittleEndian2() {
		int[] by = Tools.littleEndian2(257);
		assertEquals(1, by[0]);
		assertEquals(1, by[1]);
	}

	@Test
	void testLittleEndian4() {
		int[] by = Tools.littleEndian4(16843009);
		assertEquals(1, by[0]);
		assertEquals(1, by[1]);
		assertEquals(1, by[2]);
		assertEquals(1, by[3]);
	}

	@Test
	void testIntToBin8() {
		boolean[] b = Tools.intToBin8(128);
		System.out.println(Tools.printBin(b));
		assertEquals(false, b[0]);
		assertEquals(false, b[1]);
		assertEquals(true, b[7]);
	}

}
