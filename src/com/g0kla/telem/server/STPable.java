package com.g0kla.telem.server;

/**
 * An object that can be held in an Satellite Telemetry Protocol Queue, can be loaded and saved to disk and can be
 * sent to a telem server
 * @author chris
 *
 */
public interface STPable {

	public int[] getRawBytes(); // returns all of the bytes needed to recreate this object at the other end
	
}
