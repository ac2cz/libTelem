package com.g0kla.telem.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.g0kla.telem.segDb.DataTable;
import com.g0kla.telem.server.TlmServer;

public abstract class STPQueue implements Runnable {
	ConcurrentLinkedQueue<STP> rawRecords;
	@SuppressWarnings("unused")
	protected boolean updatedQueue = false;
	protected TlmServer server;
	protected boolean running = false;
	String fileName;
	
	public STPQueue(String file, String server, int port) throws IOException {
		this.server = new TlmServer(server, port, TlmServer.AUTO_CLOSE);
		fileName = file;
		init();
	}
	
	private void init() throws IOException {
		rawRecords = new ConcurrentLinkedQueue<STP>();
		synchronized(this) { // lock will be load the files
			load(fileName);
		}
	}
	
	public boolean add(STP f) throws IOException {
		save(f, fileName);
		return rawRecords.add(f);
	}

	public void delete() throws SecurityException, IOException {
		DataTable.remove(fileName);
		init();
	}
	
	protected void load(String log) throws IOException {
		File aFile = new File(log );
		if(!aFile.exists()){
			aFile.createNewFile();
		}

		FileInputStream dis = new FileInputStream(log);
		BufferedReader reader = new BufferedReader(new InputStreamReader(dis));
		STP record;
		
			while (reader.ready()) {
				record = new STP(reader);
				rawRecords.add(record);
			}
			updatedQueue = true;

		dis.close();
	}

	public int getSize() {
		return rawRecords.size();
	}

	/**
	 * Save a payload to the log file
	 * @param frame
	 * @param log
	 * @throws IOException
	 */
	protected void save(STP frame, String log) throws IOException {
		synchronized(this) { // make sure we have exlusive access to the file on disk, otherwise a removed frame can clash with this
			File aFile = new File(log );
			if(!aFile.exists()){
				aFile.createNewFile();
			}
			//Log.println("Saving: " + log);
			//use buffering and append to the existing file
			FileOutputStream dis = new FileOutputStream(log, true);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(dis));

			try {
				frame.save(writer);
			} finally {
				writer.flush();
				writer.close();
			}

			writer.close();
			dis.close();
		}
	}

	/**
	 * Remove the first record in the queue.  Save all of the records to the file as a backup
	 * @throws IOException 
	 */
	protected void deleteAndSave(ConcurrentLinkedQueue<STP> frames, String log) throws IOException {
		synchronized(this) {  // make sure we have exclusive access to the file on disk, otherwise a frame being added can clash with this
			frames.poll(); // remove the head of the queue
			File aFile = new File(log );
			if(!aFile.exists()){
				aFile.createNewFile();
			}
			//use buffering and OVERWRITE the existing file
			FileOutputStream dis = new FileOutputStream(log, false);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(dis));
			try {
				for (STP f : frames) {
					f.save(writer);
				}
			} finally {
				writer.flush();
				writer.close();
			}

		}
	}
	
	protected boolean sendFrame() throws IOException {
		boolean success = false;
		
		// Make sure these are up to date
//		primaryServer.setHostName(Config.primaryServer);



		if (rawRecords.peek() != null) {
			int[] buffer = rawRecords.peek().getServerBytes();
			server.sendToServer(buffer, TlmServer.TCP);
			success = true;
		}

		if (success) // then transmissions was successful
			try {
				deleteAndSave(rawRecords, fileName);
			} catch (IOException e) {
				throw new IOException("Could not remove raw frames from the queue file:\n" + fileName + "\n"
						+ " The frame will be sent again.  If this error repeats you may need to remove the queue file manually");
			}
		//MainWindow.setTotalQueued(this.getSize());
		return success; // return true if one succeeded
	}
	
	public void stopProcessing() {
		running = false;
	}
}
