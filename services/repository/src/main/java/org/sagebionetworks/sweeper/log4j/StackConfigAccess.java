package org.sagebionetworks.sweeper.log4j;

public interface StackConfigAccess {
	
	public String getS3LogBucket();

	public boolean getLogSweepingEnabled();

	public boolean getDeleteAfterSweepingEnabled();

	public String getStackInstance();

	public String getStack();
}
