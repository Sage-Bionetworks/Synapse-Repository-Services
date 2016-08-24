package org.sagebionetworks.repo.model.dao.throttle;

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
	public List<ThrottleRule> getThrottles();
	
	/**
	 * Adds a new throttle rule.
	 * @param id id to assign to the rule
	 * @param normalizedUri normalized URI of the API call to throttle
	 * @param maxCalls maximum number of calls allowed per call period
	 * @param callPeriodSec call period in seconds in which up to maxCalls can be made
	 */
	public void addThrottle(long id, String normalizedUri, long maxCalls, long callPeriodSec);
	
	/**
	 * removes a throttle given its Id
	 * @param id
	 */
	public void deleteThrottle(long id);
	
	/**
	 * Removes all throttles
	 */
	public void clearAllThrottles();
}
