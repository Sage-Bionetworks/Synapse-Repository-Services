package org.sagebionetworks.repo.model.throttle;
/**
 * A class representing the frequency limits of a single throttle rule
 * @author zdong
 *
 */
public class ThrottleLimit {
	private long maxCalls;
	private long callPeriodSec;
	
	public ThrottleLimit(long max, long period){
		this.maxCalls = max;
		this.callPeriodSec = period;
	}

	public long getCallPeriodSec() {
		return callPeriodSec;
	}

	public long getMaxCalls() {
		return maxCalls;
	}
}
