package com.g0kla.telem.server;

import java.util.Date;

import com.g0kla.telem.data.DataRecord;

public class ReceiverLayoutTotal implements Comparable<ReceiverLayoutTotal>  {
	public String receiver;
	public String layoutName;
	public int total;
	public Date lastUpdated;
	
	public ReceiverLayoutTotal(STP stp) {
		receiver = stp.receiver;
		layoutName = stp.source; // last component of the soruce is the layout
		total = 1;
		lastUpdated = new Date();
	}
	
	public void update() {
		total++;
		lastUpdated = new Date();		
	}

	@Override
	public int compareTo(ReceiverLayoutTotal p) {
		if (receiver.equalsIgnoreCase(p.receiver) && layoutName.equalsIgnoreCase(p.layoutName) ) 
			return 0;
		else if (receiver.equalsIgnoreCase(p.receiver))
			return layoutName.compareTo(p.layoutName);
		else // neither are equal
			return receiver.compareTo(p.receiver);
	}
	
	public String toString() {
		String s = "";
		s = s + receiver + " ";
		s = s + layoutName + ":  ";
		s = s + total + " Last Record: ";
		s = s + lastUpdated;
		return s;
	}
}
