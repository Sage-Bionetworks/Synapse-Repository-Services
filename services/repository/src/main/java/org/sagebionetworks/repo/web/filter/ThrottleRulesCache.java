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
	private static long UPDATE_PERIOD_MILISECONDS = 5 * 60 * 1000; //update every 5 minutes
	
	private Map<String, ThrottleLimit> throttleRulesMap = new HashMap<String, ThrottleLimit>();
	private long lastUpdated = 0; //last time in miliseconds the cache was updated
	private boolean currentlyUpdating = false; //used to ensure only one thread created for update
	private Object lockObj = new Object(); //used as a lock to synchronize on
	
	@Autowired
	private ThrottleRulesDAO throttleRulesDao;
	
	public ThrottleLimit getThrottleLimit(String normalizedUri){
		synchronized (lockObj) {
			//make sure it is time to update and no thread is currently updating already
			if(System.currentTimeMillis() - lastUpdated >= UPDATE_PERIOD_MILISECONDS && !currentlyUpdating){
				//spin up new thread to do update
				currentlyUpdating = true;
				(new Thread(){
					@Override
					public void run(){ //updates the map
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
				}).start();
			}
		}
		
		return throttleRulesMap.get(normalizedUri);
	}

}
