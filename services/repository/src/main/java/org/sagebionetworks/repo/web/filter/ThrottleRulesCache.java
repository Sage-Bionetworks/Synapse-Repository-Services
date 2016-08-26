package org.sagebionetworks.repo.web.filter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.omg.CORBA.PRIVATE_MEMBER;
import org.omg.CORBA.PUBLIC_MEMBER;
import org.sagebionetworks.repo.model.dao.throttle.ThrottleRulesDAO;
import org.sagebionetworks.repo.model.throttle.ThrottleLimit;
import org.sagebionetworks.repo.model.throttle.ThrottleRule;
import org.springframework.beans.factory.annotation.Autowired;


public class ThrottleRulesCache {
	private Map<String, ThrottleLimit> throttleRulesMap = new HashMap<String, ThrottleLimit>();
	private long lastUpdated = 0; //last time in miliseconds the cache was updated
	private static long UPDATE_PERIOD_MILISECONDS = 5 * 60 * 1000; //update every 5 minutes
	private boolean currentlyUpdating = false; //used to ensure multiple threads do not update at the same time
	
	@Autowired
	private ThrottleRulesDAO throttleRulesDao;
	
	public ThrottleLimit getThrottleLimit(String normalizedUri){
			if(shouldUpdate()){
				//spin up new thread to do update
				(new Thread(){
					@Override
					public void run(){
						updateRules();
					}
				}).start();
				
			}
			return throttleRulesMap.get(normalizedUri);
	}
	
	
	/**
	 * updates the throttle rules Map
	 */
	private synchronized void updateRules(){
		//checks condition again to make so that extra threads that were created do not update.
		if(shouldUpdate()){
			
			//flag that this thread is updating and start updating
			currentlyUpdating = true;
			
			//put rules in a new map
			Map<String, ThrottleLimit> updatedRulesMap = new HashMap<String, ThrottleLimit>();
			List<ThrottleRule> rules = throttleRulesDao.getAllThrottles();
			for(ThrottleRule rule : rules){
				updatedRulesMap.put(rule.getNormalizedUri(), new ThrottleLimit(rule.getMaxCalls(), rule.getCallPeriodSec()));
			}
			
			//change the reference to the new map and flag that the thread finished updating
			throttleRulesMap = updatedRulesMap;
			lastUpdated = System.currentTimeMillis();
			currentlyUpdating = false;
		}
	}
	
	private boolean shouldUpdate(){
		//make sure it is time to update and no thread is currently updating already
		return System.currentTimeMillis() - lastUpdated >= UPDATE_PERIOD_MILISECONDS && !currentlyUpdating;
	}

}
