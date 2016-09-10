package org.sagebionetworks.repo.model.throttle;
/**
 * A immutable class representing the frequency limits of a single throttle rule
 * @author zdong
 *
 */
public class ThrottleLimit {
	private long maxCallsPerUserPerPeriod;
	private long callPeriodSec;
	
	public ThrottleLimit(long max, long period){
		this.maxCallsPerUserPerPeriod = max;
		this.callPeriodSec = period;
	}
	
	/**
	 * returns the time period in seconds
	 * @return
	 */
	public long getCallPeriodSec() {
		return callPeriodSec;
	}
	
	/**
	 * returns the maximum number of calls a user can make in a time period
	 */
	public long getMaxCallsPerUserPerPeriod() {
		return maxCallsPerUserPerPeriod;
	}
}
