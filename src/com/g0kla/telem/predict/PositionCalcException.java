package com.g0kla.telem.predict;

import com.g0kla.telem.data.DataRecord;

@SuppressWarnings("serial")
public class PositionCalcException extends Exception {

	public double errorCode = DataRecord.NO_POSITION_DATA;
	
	public PositionCalcException(double exceptionCode) {
		errorCode = exceptionCode;
	}
}
