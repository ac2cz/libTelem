package com.g0kla.telem.data;

import java.util.Date;

public class EpochTime {
	int reset;
	long uptime;
	Date utcEquivalent;
	
	public EpochTime(int r, long u) {
		reset = r;
		uptime = u;
	}
	
	public int getReset() {
		return reset;
	}
	public void setReset(int reset) {
		this.reset = reset;
	}
	public long getUptime() {
		return uptime;
	}
	public void setUptime(long uptime) {
		this.uptime = uptime;
	}
	public Date getUtcEquivalent() {
		return utcEquivalent;
	}
	public void setUtcEquivalent(Date utcEqivalent) {
		this.utcEquivalent = utcEqivalent;
	}
	
	
}
