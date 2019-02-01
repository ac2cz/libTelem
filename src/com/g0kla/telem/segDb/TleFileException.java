package com.g0kla.telem.segDb;

import java.io.IOException;

public class TleFileException extends IOException {
	private static final long serialVersionUID = 1L;

	public TleFileException(String errorMsg) {
		super(errorMsg);
	}
}
