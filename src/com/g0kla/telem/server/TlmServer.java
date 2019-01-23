package com.g0kla.telem.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * 
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2018 amsat.org
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
 * Class the holds the connection to the telemetry server and send data to it
 * 
 *
 */
public class TlmServer {
	public static final int TCP = 0;
	public static final int UDP = 1;
	public static final int FTP_PORT = 22;
	public static final boolean KEEP_OPEN = false;
	public static final boolean AUTO_CLOSE = true;

	String hostName;
	int portNumber;
	boolean autoClose = true; // If this is false we keep the socket open, but reopen if it is closed
	Socket socket = null;
	OutputStream out = null;
	
	public TlmServer(String hostName, int portNumber, boolean autoClose) {
		this.hostName = hostName;
		this.portNumber = portNumber;
		this.autoClose = autoClose;
	}

	public String getHostname() { return hostName; }
	public int getPort() { return portNumber; }
	
	public void setHostName(String hostName) {
		if (this.hostName != hostName)
			close();
		this.hostName = hostName;
	}
	
	public void setPort(int port) {
		if (this.portNumber != port)
			close();
		this.portNumber = port;
	}
		
	public void close() {
		if (out != null)
			try {
				out.close();
			} catch (Exception e) {
				// Nothing to do
			}
		out = null;
		if (socket != null)
			try {
				socket.close();
			} catch (Exception e) {
				// Nothing to do
			}
		socket = null;
	}
	
	/**
	 * Send this frame to the amsat server "hostname" on "port" for storage.  It is sent by the queue
	 * so there is no need for this routine to remove the record from the queue
	 * @param hostName
	 * @param port
	 */
	public void sendToServer(int[] data, int protocol) throws UnknownHostException, IOException {
		byte[] buffer = new byte[data.length];
		for(int i = 0; i< data.length; i++)
			buffer[i] = (byte) data[i];
		if (protocol == TCP) {
			if (autoClose || socket == null) {
				socket = new Socket(hostName, portNumber);
				out = socket.getOutputStream();
			}

			out.write(buffer);
			if (autoClose)
				close();
			
		} else {
			DatagramSocket socket = new DatagramSocket();

			InetAddress address = InetAddress.getByName(hostName);
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, 
					address, portNumber);
			socket.send(packet);
			socket.close();
		}
	}

}
