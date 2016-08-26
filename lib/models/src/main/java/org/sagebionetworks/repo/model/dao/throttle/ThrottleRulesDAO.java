package org.sagebionetworks.repo.model.dao.throttle;

import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.throttle.ThrottleRule;

/**
 * Used to query for throttle rules
 * @author zdong
 *
 */
public interface ThrottleRulesDAO {
	/**
	 * Returns a list of all currently throttled API calls
	 * @return list of all current throttles
	 */
	public List<ThrottleRule> getAllThrottles();
	
	/**
	 * Returns a list of all throttled API calls modified after the given time
	 * @param time time after which selected throttles have been modified.
	 * @return
	 */
	public List<ThrottleRule> getAllThrottlesAfter(Date time);
	
	/**
	 * Adds a new throttle rule.
	 * @param id id to assign to the rule
	 * @param normalizedUri normalized URI of the API call to throttle
	 * @param maxCalls maximum number of calls allowed per call period
	 * @param callPeriodSec call period in seconds in which up to maxCalls can be made
	 * @return number of rows affected
	 */
	public int addThrottle(long id, String normalizedUri, long maxCalls, long callPeriodSec);
	
	/**
	 * Removes all throttles
	 */
	public void clearAllThrottles();
}
