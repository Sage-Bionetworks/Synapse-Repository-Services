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
	 * Returns a list of all currently throttled API calls.
	 * The returned list will be empty if there are no rules.
	 * @return list of all current throttles
	 */
	public List<ThrottleRule> getAllThrottleRules();
	
	/**
	 * Adds a new throttle rule.
	 * @param id id to assign to the rule
	 * @param normalizedUri normalized URI of the API call to throttle
	 * @param maxCalls maximum number of calls allowed per call period
	 * @param callPeriodSec call period in seconds in which up to maxCalls can be made
	 * @return number of rows affected
	 */
	public int addThrottle(ThrottleRule throttleRule);
	
	/**
	 * Removes all throttles
	 */
	public void clearAllThrottles();
}
