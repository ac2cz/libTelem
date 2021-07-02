package com.g0kla.telem.segDb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import com.g0kla.telem.gui.ProgressPanel;
import com.g0kla.telem.predict.FoxTLE;
import com.g0kla.telem.predict.SortedTleList;

/**
 * 
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2019 amsat.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * This class holds a list of the satellites and loads their details from a file.  The
 * spacecraft class then loads the telemetry layouts and any lookup tables
 * 
 * 
 */
public class SatelliteManager implements Runnable {
	
	public static final String AMSAT_NASA_ALL = "http://lansing182.amsat.org/tle/current/nasabare.txt";
	public boolean updated = true; // true when we have first been created or the sats have been updated and layout needs to change
	boolean server = false;
	public ArrayList<Spacecraft> spacecraftList = new ArrayList<Spacecraft>();
	
	public SatelliteManager(boolean server, String logSpacecraftDir) throws IOException  {
//		init();
	}
	
	public boolean add(Spacecraft sat) {
		return spacecraftList.add(sat);
	}
	
//	public void init() {
//		File masterFolder = new File(Config.currentDir + File.separator + FoxSpacecraft.SPACECRAFT_DIR);
//		File folder = getFolder(masterFolder);
//		//File folder = new File("spacecraft");
//		loadSats(folder);
//	}
	
	
//	private void loadSats(File folder) {
//		File[] listOfFiles = folder.listFiles();
//		Pattern pattern = Pattern.compile("AO-73");
//		if (listOfFiles != null) {
//			for (int i = 0; i < listOfFiles.length; i++) {
//				if (listOfFiles[i].isFile() && listOfFiles[i].getName().endsWith(".dat")) {
////					Log.println("Loading spacecraft from: " + listOfFiles[i].getName());
//					Spacecraft satellite = null;
//					try {
//							satellite = new Spacecraft(listOfFiles[i]);
//					} catch (FileNotFoundException e) {
//						Log.errorDialog("ERROR processing " + listOfFiles[i].getName(), e.getMessage() + "\nThis satellite will not be loaded");
//						e.printStackTrace(Log.getWriter());
//						satellite = null;
//					} catch (LayoutLoadException e) {
//						Log.errorDialog("ERROR processing " + listOfFiles[i].getName(), e.getMessage() + "\nThis satellite will not be loaded");
//						e.printStackTrace(Log.getWriter());
//						satellite = null;
//					} catch (IOException e) {
//						Log.errorDialog("IO ERROR processing " + listOfFiles[i].getName(), e.getMessage() + "\nThis satellite will not be loaded");
//						e.printStackTrace(Log.getWriter());
//					}
//					if (satellite != null)
//						if (getSpacecraft(satellite.foxId) != null)
//							Log.errorDialog("WARNING", "Can not load two satellites with the same Fox ID.  Skipping file\n"
//									+ listOfFiles[i].getName());
//						else
//							spacecraftList.add(satellite);
//				} 
//			}
//		}
//		if (spacecraftList.size() == 0) {
//			Log.errorDialog("FATAL!", "No satellites could be loaded.  Check the spacecraft directory:\n " + 
//					Config.currentDir + File.separator + FoxSpacecraft.SPACECRAFT_DIR +
//					"\n and confirm it contains the "
//					+ "satellite data files, their telemetry layouts and lookup tables. Program will exit");
//			System.exit(1);
//		}
//		Collections.sort((List<Spacecraft>)spacecraftList);
//	}
	
//	/**
//	 * Return the Bit Array layout for this satellite Id
//	 * @param sat
//	 * @return
//	 */
//	public BitArrayLayout getLayoutByName(int sat, String name) {
//		if (!validFoxId(sat)) return null;
//		FoxSpacecraft sc = (FoxSpacecraft)getSpacecraft(sat);
//		if (sc != null) return sc.getLayoutByName(name);
//		return null;
//	}
//	
//	public BitArrayLayout getLayoutByCanId(int sat, int id) {
//		if (!validFoxId(sat)) return null;
//		FoxSpacecraft sc = (FoxSpacecraft)getSpacecraft(sat);
//		if (sc != null) return sc.getLayoutByCanId(id);
//		return null;
//	}
//	
//	public BitArrayLayout getMeasurementLayout(int sat) {
//		if (!validFoxId(sat)) return null;
//		FoxSpacecraft sc = (FoxSpacecraft)getSpacecraft(sat);
//		if (sc != null) return sc.measurementLayout;
//		return null;
//	}
//
//	public BitArrayLayout getPassMeasurementLayout(int sat) {
//		if (!validFoxId(sat)) return null;
//		FoxSpacecraft sc = (FoxSpacecraft)getSpacecraft(sat);
//		if (sc != null) return sc.passMeasurementLayout;
//		return null;
//	}
//	
//	public ArrayList<Spacecraft> getSpacecraftList() { 
//		return spacecraftList; 
//	} 
//
//	public int getNumberOfSpacecraft() { return spacecraftList.size(); }

	public boolean haveSpacecraft(String name) {
		for (int i=0; i < spacecraftList.size(); i++) {
			if (spacecraftList.get(i).name.equalsIgnoreCase(name))
				return true;
		}
		return false;
	}
	
	public Spacecraft getSpacecraftByName(String name) {
		for (int i=0; i < spacecraftList.size(); i++) {
			if (spacecraftList.get(i).name.equalsIgnoreCase(name))
				return spacecraftList.get(i);
		}
		return null;
	}
	
	public Spacecraft getSpacecraft(int sat) {
		for (int i=0; i < spacecraftList.size(); i++) {
			if (spacecraftList.get(i).satId == sat)
				return spacecraftList.get(i);
		}
		return null;
	}
	
	/*
	 * We Fetch a TLE file from amsat.org.  We then see if it contains TLEs for the Spacecraft we are interested in. If it does we
	 * check if there is a later TLE than the one we have.  If it is, then we append it to the TLE store for the given sat.
	 * We then load the TLEs for each Sat and store the, in the spacecraft class.  This can then be used to find the position of the spacecraft at 
	 * any time since launch
	 */

	public void fetchTLEFile(String logSpacecraftDir) throws IOException {
		String urlString = AMSAT_NASA_ALL;
		String file = logSpacecraftDir + File.separator + "nasabare.txt";
		String filetmp = file + ".tmp";
		File f1 = new File(filetmp);
		File f2 = new File(file);
		Date lm = new Date(f2.lastModified());

		String msg = "Downloading new keps ...                 ";
		ProgressPanel initProgress = null;
//		if (!server) {
//			initProgress = new ProgressPanel(Config.frame, msg, false);
//			initProgress.setVisible(true);
//		}
		URL website;
		FileOutputStream fos = null;
		ReadableByteChannel rbc = null;
		try {
			website = new URL(urlString);
			HttpURLConnection httpCon = (HttpURLConnection) website.openConnection();
			long date = httpCon.getLastModified();
			httpCon.disconnect();
			Date kepsDate = new Date(date);
			if (kepsDate.getTime() <= lm.getTime()) { // then dont try to update it
//				Log.println(".. keps are current");
				filetmp = file;
			} else {
//				Log.println(" ... open RBC ..");
				rbc = Channels.newChannel(website.openStream());
//				Log.println(" ... open output file .." + filetmp);
				fos = new FileOutputStream(filetmp);
//				Log.println(" ... getting file ..");
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
//				Log.println(" ... closing outpt stream ..");
				fos.close();
//				Log.println(" ... closing input stream ..");
				rbc.close();
			}
			// Always process the file because it is quick and the user may have changed the name of a spacecraft
			// The code throws away duplicate keps with the same epoch
			// Now lets see if we have a good file.  If we did not, it will throw an exception
//			Log.println(" ... parsing file ..");
			parseTleFile(filetmp);
			// this is a good file so we can now use it as the default
//			Log.println(" ... remove and rename ..");
			if (!file.equalsIgnoreCase(filetmp)) {
				// We downloaded a new file so rename tmp as the new file
				DataTable.remove(file);
				DataTable.copyFile(f1, f2);
			}
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (!file.equalsIgnoreCase(filetmp))
				DataTable.remove(file + ".tmp");
			return;

		} catch (MalformedURLException e) {
			try { DataTable.remove(file + ".tmp"); } catch (IOException e1) {e1.printStackTrace();}
			throw new MalformedURLException ("Invalid location for Keps file: " + file);
		} catch (IOException e) {
			try { DataTable.remove(file + ".tmp"); } catch (IOException e1) {e1.printStackTrace();}
			throw new IOException("Could not fetch Keps file and write it to:\n" + file);
		} catch (IndexOutOfBoundsException e) {
			try { DataTable.remove(file + ".tmp"); } catch (IOException e1) {e1.printStackTrace();}
			throw new IndexOutOfBoundsException("Keps file is corrupt: " + file);
		} finally {
			if (!server && initProgress != null) 
				initProgress.updateProgress(100);
			try {
				if (fos != null) fos.close();
				if (rbc != null) rbc.close();
			} catch (IOException e) {
				// ignore
			}
		}

	}

	/*
	private void loadTLE() {
		String file = FoxSpacecraft.SPACECRAFT_DIR + File.separator + "nasa.all";
		if (!Config.logFileDirectory.equalsIgnoreCase("")) {
			file = Config.logFileDirectory + File.separator + FoxSpacecraft.SPACECRAFT_DIR + File.separator + "nasa.all";		
		}
		try {
			List<TLE> TLEs = parseTleFile(file);
			for (TLE tle : TLEs) {
				String name = tle.getName();
				Spacecraft spacecraft = this.getSpacecraftByName(name);
				if (spacecraft != null) {
					Log.println("Stored TLE for: " + name);
					spacecraft.addTLE(tle);
				}
			}
		} catch (IOException e) {
			Log.errorDialog("TLE Load ERROR", "CANT PARSE the TLE file - " + file + "/n" + e.getMessage());
			e.printStackTrace(Log.getWriter());
		}
	}
	*/
	
	/**
	 * Parse the nasabare.txt file and make a list
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	private SortedTleList parseTleFile(String filename) throws IOException {
		File f = new File(filename);
		InputStream is = new FileInputStream(f);
		try {
			SortedTleList tles = FoxTLE.importFoxSat(is);
			is.close();
			for (FoxTLE ftle : tles) {
				String name = ftle.getName();
				Spacecraft spacecraft = this.getSpacecraftByName(name);
				if (spacecraft != null) {
//					Log.println("Stored TLE for: " + name);
					spacecraft.addTLE(ftle);
				}
			}
			return tles;
		} finally {
			is.close();
		}
	}

	boolean running = true;
	boolean done = false;
	
	public void stop() {
		running = false;
		while (!done) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void run() {
		// Runs until we exit
//		while(running) {
//			// Sleep first to avoid race conditions at start up
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//
//			if (Config.foxTelemCalcsPosition) {
//				// Calculate the sat positions, which caches them in each sat
//				for (int s=0; s < spacecraftList.size(); s++) {
//					Spacecraft sat = spacecraftList.get(s);
//					if (sat.track) {
//						if (Config.GROUND_STATION != null)
//							if (Config.GROUND_STATION.getLatitude() == 0 && Config.GROUND_STATION.getLongitude() == 0) {
//								// We have a dummy Ground station which is fine for sat position calc but not for Az, El calc.
//								sat.track = false;
//								sat.save();
//								Log.errorDialog("MISSING GROUND STATION", "FoxTelem is configured to calculate the spacecraft position, but your ground station\n"
//										+ "is not defined.  Go to the settings tab and setup the ground station position or turn of calculation of the spacecraft position.\n"
//										+ "Tracking will be disabled for " + sat.name + ".");
//								sat.satPos = null;
//							} else {
//								try {
//									sat.calcualteCurrentPosition();
//								} catch (PositionCalcException e) {
//									// We wont get NO T0 as we are using the current time, but we may have missing keps
//							/*		if (running) { // otherwise we reset the sats and another copy of this thread will deal with the issue
//										sat.track = false;
//										sat.save();
//										String scd = Config.getLogFileDirectory() + "spacecraft\\";
//										Log.errorDialog("MISSING TLE", "FoxTelem is configured to calculate the spacecraft position, but no TLE was found for "
//												+ sat.name +".\nMake sure the name of the spacecraft matches the name of the satellite in the nasabare.tle\n "
//												+ "file from amsat.  This file is automatically downloaded from: \nhttp://www.amsat.org/amsat/ftp/keps/current/nasabare.txt\n"
//												+ "TLE for this spacecraft is copied from nasabare.txt into the file:\n"+scd+"FOX"+ sat.foxId + ".tle.  It may be missing or corrupt.\n"
//												+ "Tracking will be disabled for this spacecraft. \n\n "
//												+ "You can still use 'Find Signal' for this spacecraft if you turn off position calculation on the settings panel and \n"
//												+ "uncheck 'Fox Telem calculates position'.  Then re-enable tracking.");
//												
//										sat.satPos = null;
//									}
//									*/
//									sat.satPos = null;
//								}
//							}
//					}
//				}
//			}
//
//		}
		done = true;
	}

}
