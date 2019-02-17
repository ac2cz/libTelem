package com.g0kla.telem.segDb;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import com.g0kla.telem.server.STP;
import com.g0kla.telem.server.StpFileProcessException;

/**
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
 * SQL database backend for telemetry data
 * 
 *
 */
public class SatDbStore {
	// MySQL error codes
	public static final String ERR_TABLE_DOES_NOT_EXIST = "42S02";
	public static final String ERR_DUPLICATE = "23000";
	public static final String ERR_OPEN_RESULT_SET = "X0X95";

	public Connection conn;

	static String url = "jdbc:mysql://localhost:3306/"; //FOXDB?autoReconnect=true";
	static String db = "FOXDB";
	static String user = "g0kla";
	static String password = "";

	public SatDbStore(String u, String pw, String database) throws SQLException {
		db = database;
		user = u;
		password = pw;

		Statement st = null;
		ResultSet rs = null;


		try {
			conn = getConnection();
			st = conn.createStatement();
			rs = st.executeQuery("SELECT VERSION()");

			if (rs.next()) {
				// we are connected
			} else {
				// we failed
			}

			initStpHeaderTable();

		} finally {
			try {
				if (rs != null) {  rs.close();}
				if (st != null) { st.close(); }
			} catch (SQLException ex) { // ignore; }
		}
	}
	}

	public Connection getConnection() throws SQLException {
		if (conn == null || !conn.isValid(2))  // check that the connection is still valid, otherwise reconnect
			conn = DriverManager.getConnection(url + db + "?autoReconnect=true", user, password);
		return conn;

	}

	public void closeConnection() throws SQLException {
		if (conn != null) 
			conn.close();
	}


	private void initStpHeaderTable() throws SQLException {
		String table = "STP_HEADER";
		Statement stmt = null;
		ResultSet select = null;
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			select = stmt.executeQuery("select 1 from " + table + " LIMIT 1");

		} catch (SQLException e) {

			if ( e.getSQLState().equals(ERR_TABLE_DOES_NOT_EXIST) ) {  // table does not exist
				String createString = "CREATE TABLE " + table + " ";
				createString = createString + STP.getTableCreateStmt();

				try {
					stmt.execute(createString);
				} catch (SQLException ex) {
					throw new SQLException("initStpHeaderTable", ex);
				}
			} else {
				throw new SQLException("initStpHeaderTable", e);
			}
		} finally {
			try { if (select != null) select.close(); } catch (SQLException e2) {};
			try { if (stmt != null) stmt.close(); } catch (SQLException e2) {};
		}
	}

	public boolean addStpHeader(STP f) throws SQLException {
		PreparedStatement ps = null;
		try {
			conn = getConnection();
			ps = f.getPreparedInsertStmt(conn);

			@SuppressWarnings("unused")
			int count = ps.executeUpdate();
		} catch (SQLException e) {
			if ( e.getSQLState().equals(ERR_DUPLICATE) ) {  // duplicate
				return true; // we consider this data added
			} else {
				throw new SQLException("addStpHeader", e);
			}
		} finally {
			try { if (ps != null) ps.close(); } catch (SQLException e2) {};
		}
		return true;
	}

	public boolean updateStpHeader(STP f) throws StpFileProcessException, SQLException {

		Statement stmt = null;
		String update = "update STP_HEADER ";
		update = update + "set rx_location='"+f.rx_location +"', ";
		update = update + "receiver_rf='"+f.receiver_rf +"' ";
		update = update + " where receiver='"+f.receiver+"' ";
		update = update + " and sequenceNumber="+f.sequenceNumber;
		update = update + " and resets="+f.resets;
		update = update + " and uptime="+f.uptime;
		update = update + " and id="+f.id;
		//Log.println("SQL:" + update);
		try {
			conn = getConnection();
			stmt = conn.createStatement();
			int r = stmt.executeUpdate(update);
			if (r > 1) throw new StpFileProcessException("FOXDB","MULTIPLE ROWS UPDATED!");
		} catch (SQLException e) {
			if ( e.getSQLState().equals(ERR_DUPLICATE) ) {  // duplicate
				return true; // we consider this data added
			} else {
				throw new SQLException("updateStpHeader", e);
			}
		} finally {
			try { if (stmt != null) stmt.close(); } catch (SQLException e2) {};
		}
		return true;
	}
	
	public static String errorPrint(String cause, Throwable e) {
		if (e instanceof SQLException)
			return SQLExceptionPrint(cause, (SQLException)e);
		else {
			return("ERROR: "+cause+" A NON SQLException error occured while accessing the DB");
		}
	} // END errorPrint

	// Iterates through a stack of SQLExceptions
	static String SQLExceptionPrint(String cause, SQLException sqle) {
		String s = "";
		while (sqle != null) {
			s = s + ("\n---SQLException Caught--- Caused by: "+cause+"\n");
			s = s + ("SQLState: " + (sqle).getSQLState());
			s = s + ("Severity: " + (sqle).getErrorCode());
			s = s + ("Message: " + (sqle).getMessage());
			sqle = sqle.getNextException();
		}
		return s;
	} // END SQLExceptionPrint


}
