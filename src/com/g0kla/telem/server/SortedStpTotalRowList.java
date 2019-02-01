package com.g0kla.telem.server;

import com.g0kla.telem.segDb.SortedArrayList;

public class SortedStpTotalRowList extends SortedArrayList<StpTotalRow> {
	private static final long serialVersionUID = 1L;

	public boolean add(StpTotalRow totalRecord) {
		return super.add(totalRecord);
		
	}
	
	/**
	 * Partial match to see if we update row.  We do not match on the total number
	 * @param totalRecord
	 * @return
	 */
	public StpTotalRow findRow(StpTotalRow totalRecord) {
		for (StpTotalRow row : this) {
			if (row.layoutName.equalsIgnoreCase(totalRecord.layoutName) &&
					row.receiver.equalsIgnoreCase(totalRecord.receiver))
				return row;
		}
		return null;
		
	}
	
	

}
