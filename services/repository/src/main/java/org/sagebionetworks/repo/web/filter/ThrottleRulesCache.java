package org.sagebionetworks.repo.web.filter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


import org.sagebionetworks.repo.model.dao.throttle.ThrottleRulesDAO;
import org.sagebionetworks.repo.model.throttle.ThrottleLimit;
import org.sagebionetworks.repo.model.throttle.ThrottleRule;
import org.springframework.beans.factory.annotation.Autowired;

import com.atlassian.util.concurrent.atomic.AtomicLong;
import com.atlassian.util.concurrent.atomic.AtomicReference;

/**
 * Caches the THROTTLE_RULES database table
 * @author zdong
 *
 */
public class ThrottleRulesCache {	
	/*
	 * The AtomicAtomicReference and AtomicLong allow multiple threads to update them at the same time 
	 * Using ConcurrentHashMap because it allows multiple threads to read at the same time
	 * The maps in this class are immutable. When the throttle rules are updated, a new map is created
	 * and the reference is changed.
	 */
	private AtomicReference<Map<String, ThrottleLimit>> throttleRulesMap = new AtomicReference<Map<String, ThrottleLimit>>( new ConcurrentHashMap<String, ThrottleLimit>());
	private AtomicLong lastUpdated = new AtomicLong(0);
	
	@Autowired
	private ThrottleRulesDAO throttleRulesDao;
	
	public ThrottleLimit getThrottleLimit(String normalizedUri){
		return throttleRulesMap.get().get(normalizedUri);
	}
	

	/**
	 * Quartz will fire this method on a timer to update the cache.
	 */
	public void timerFired(){
		//put rules in a new map
		Map<String, ThrottleLimit> updatedRulesMap = new ConcurrentHashMap<String, ThrottleLimit>();
		List<ThrottleRule> rules = throttleRulesDao.getAllThrottles();
		for(ThrottleRule rule : rules){
			updatedRulesMap.put(rule.getNormalizedUri(), new ThrottleLimit(rule.getMaxCalls(), rule.getCallPeriodSec()));
		}
		
		//change the reference to the new map and flag that the thread finished updating

		throttleRulesMap.set(updatedRulesMap);
		lastUpdated.set(System.currentTimeMillis());
	}
	
	//autowire the trigger in integrationtest
	//add atomic long -timestamp of when it last ran. in integration test waits for that value to be greater than 0
	//log the time 
}
