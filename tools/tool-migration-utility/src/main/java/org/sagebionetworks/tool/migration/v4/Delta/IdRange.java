package org.sagebionetworks.tool.migration.v4.Delta;

public class IdRange {
	
	private long minId;
	private long maxId;
	
	public IdRange(long min, long max) {
		this.minId = min;
		this.maxId = max;
	}

}
