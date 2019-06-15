package org.sagebionetworks.repo.web.filter.throttle;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.sagebionetworks.repo.model.dao.throttle.ThrottleRulesDAO;
import org.sagebionetworks.repo.model.throttle.ThrottleLimit;
import org.sagebionetworks.repo.model.throttle.ThrottleRule;
import org.springframework.beans.factory.annotation.Autowired;


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
	private AtomicReference<Map<String, ThrottleLimit>> throttleRulesMapReference = new AtomicReference<Map<String, ThrottleLimit>>( new ConcurrentHashMap<String, ThrottleLimit>());
	private AtomicLong lastUpdated = new AtomicLong(0);
	
	@Autowired
	private ThrottleRulesDAO throttleRulesDao;
	
	/**
	 * Returns the ThrottleLimit of the given normalize API path. Returns null if there is no limit
	 * @param normalizedPath 
	 * @return
	 */
	public ThrottleLimit getThrottleLimit(String normalizedPath){
		return throttleRulesMapReference.get().get(normalizedPath);
	}
	
	/**
	 * Returns the number of API paths that are throttled
	 */
	public int getNumThrottleRules(){
		return throttleRulesMapReference.get().size();
	}
	

	/**
	 * Quartz will fire this method on a timer to update the cache.
	 */
	public void timerFired(){
		//put rules in a new map
		Map<String, ThrottleLimit> updatedRulesMap = new ConcurrentHashMap<String, ThrottleLimit>();
		List<ThrottleRule> rules = throttleRulesDao.getAllThrottleRules();
		for(ThrottleRule rule : rules){
			updatedRulesMap.put(rule.getNormalizedPath().toLowerCase(), new ThrottleLimit(rule.getMaxCallsPerPeriod(), rule.getPeriod()));
		}
		
		//change the reference to the new map and flag that the thread finished updating

		throttleRulesMapReference.set(updatedRulesMap);
		lastUpdated.set(System.currentTimeMillis());
	}
	
	/**
	 * <h1>USED FOR TESTING</h1><br>
	 * Returns the unix timestamp in miliseconds of when the cache was last updated. If the map has never been updated, returns 0.
	 * @return
	 */
	public long getLastUpdated(){
		return lastUpdated.get();
	}
}
