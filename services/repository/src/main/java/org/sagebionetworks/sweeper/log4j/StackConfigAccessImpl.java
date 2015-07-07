package org.sagebionetworks.sweeper.log4j;

import org.sagebionetworks.StackConfiguration;

public class StackConfigAccessImpl implements StackConfigAccess {

	StackConfigAccessImpl(){
		StackConfiguration.init();
	}
	@Override
	public String getStack() {
		return StackConfiguration.singleton().getStack();
	}


	@Override
	public String getStackInstance() {
		return StackConfiguration.getStackInstance();
	}

	@Override
	public String getIAMUserId() {
		
		return StackConfiguration.getIAMUserId();
	}

	@Override
	public String getIAMUserKey() {
		return StackConfiguration.getIAMUserKey();
	}

	@Override
	public String getS3LogBucket() {
		return StackConfiguration.getS3LogBucket();
	}

	@Override
	public boolean getLogSweepingEnabled() {
		return StackConfiguration.getLogSweepingEnabled();
	}

	@Override
	public boolean getDeleteAfterSweepingEnabled() {
		return StackConfiguration.getDeleteAfterSweepingEnabled();
	}

}
