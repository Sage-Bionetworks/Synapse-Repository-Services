package org.sagebionetworks.tool.migration.job;

public class AggregateResult {
	
	private int totalEntitesProcessed;
	private int failedJobCount;
	private int successfulJobCount;
	
	
	public AggregateResult(int totalEntitesProcessed, int failedJobCount,
			int successfulJobCount) {
		super();
		this.totalEntitesProcessed = totalEntitesProcessed;
		this.failedJobCount = failedJobCount;
		this.successfulJobCount = successfulJobCount;
	}
	public int getTotalEntitesProcessed() {
		return totalEntitesProcessed;
	}
	public int getFailedJobCount() {
		return failedJobCount;
	}
	public int getSuccessfulJobCount() {
		return successfulJobCount;
	}

}
