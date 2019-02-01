package com.g0kla.telem.server;

import java.util.Date;
import java.util.StringTokenizer;

public class StpTotalRow implements Comparable<StpTotalRow>  {
	public String receiver;
	public String layoutName;
	public int total;
	public Date lastUpdated;
	
	public StpTotalRow(STP stp) {
		receiver = stp.receiver;
		layoutName = stp.source; // last component of the soruce is the layout
		total = 1;
		lastUpdated = new Date();
	}
	
	public StpTotalRow(StringTokenizer st) {
		long updated = Long.valueOf(st.nextToken()).longValue();
		lastUpdated = new Date(updated);
		receiver = st.nextToken();
		layoutName = st.nextToken();
		total = Integer.valueOf(st.nextToken()).intValue();
	}
	
	public void update() {
		total++;
		lastUpdated = new Date();		
	}

	@Override
	public int compareTo(StpTotalRow p) {
//		if (receiver.equalsIgnoreCase(p.receiver) && layoutName.equalsIgnoreCase(p.layoutName)
//				&& this.total == p.total) 
//			return 0;
		if (total == p.total)
			return 0;
		else if (total < p.total)
			return 1;
		else if (total > p.total)
			return -1;
		// otherwise the totals are equal
//		else if (receiver.equalsIgnoreCase(p.receiver))
//			return layoutName.compareTo(p.layoutName);
//		else // neither are equal
//			return receiver.compareTo(p.receiver);
		return 0;
	}
	
	public String toFile() {
		String s = "";
		s = s + lastUpdated.getTime() + ",";
		s = s + receiver + ",";
		s = s + layoutName + ",";
		s = s + total;
		return s;
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
