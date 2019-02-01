package com.g0kla.telem.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.StringTokenizer;

public class LeaderboardTable implements Serializable {
	private static final long serialVersionUID = 1L;
	SortedStpTotalRowList totals;
	String fileName;
	
	public LeaderboardTable(String file) throws NumberFormatException, IOException {
		fileName = file;
		totals = new SortedStpTotalRowList();
		load();
	}
	
	public void add(STP stp) throws IOException {
		StpTotalRow totalRecord = new StpTotalRow(stp);
		StpTotalRow existing = totals.findRow(totalRecord);
		if (existing == null) {
			totals.add(totalRecord);
		} else {
			existing.update();
		}
		totals.sort(null);
		save();
	}
	
	/**
	 * Save Index to the a file
	 * @throws IOException
	 */
	private void save() throws IOException {
		File aFile = new File(fileName );
		createNewFile(fileName);
		//Log.println("Saving: " + log);
		//use buffering and REPLACE the existing file
		Writer output = new BufferedWriter(new FileWriter(aFile, false));
		try {
			for (StpTotalRow r : totals) {
				output.write( r.toFile() + "\n" );
				output.flush();
			}
		} finally {
			// Make sure it is closed even if we hit an error
			output.flush();
			output.close();
		}
	}
	
	private boolean createNewFile(String log) throws IOException {
		File aFile = new File(log );
		if(!aFile.exists()){
				aFile.createNewFile();
				return true;
		}
		return false;
	}
	
	/**
	 * Load the Index from disk
	 * @throws IOException 
	 */
	public void load() throws IOException, NumberFormatException {
        String line;
        File aFile = new File(fileName );
		if (createNewFile(fileName)) {
			Writer output = new BufferedWriter(new FileWriter(aFile, true));
			output.close();
		}
 
        BufferedReader dis = new BufferedReader(new FileReader(aFile.getPath()));

        try {
        	while ((line = dis.readLine()) != null) {
        		if (line != null) {
        			StringTokenizer st = new StringTokenizer(line, ",");
        			StpTotalRow row = new StpTotalRow(st);
    				totals.add(row);
        		}
        	}
        } finally {        	
        	dis.close();
        }
	}	
	
	
	public String toString() {
		String s= "LEADERBOARD TABLE\n";
		for (StpTotalRow r : totals)
			s = s + r + "\n";
		return s;
	}
}
