package com.g0kla.telem.server;

import com.g0kla.telem.segDb.SortedArrayList;

public class LeaderboardTable {
	SortedArrayList<ReceiverLayoutTotal> totals;
	
	public LeaderboardTable() {
		totals = new SortedArrayList<ReceiverLayoutTotal>();
	}
	
	public void add(STP stp) {
		ReceiverLayoutTotal totalRecord = new ReceiverLayoutTotal(stp);
		ReceiverLayoutTotal existing = totals.get(totalRecord);
		if (existing == null) {
			totals.add(totalRecord);
		} else {
			existing.update();
		}
	}
	
	public String toString() {
		String s= "LEADERBOARD TABLE\n";
		for (ReceiverLayoutTotal r : totals)
			s = s + r + "\n";
		return s + "\n";
	}
}
