package org.sagebionetworks.repo.web.filter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.sagebionetworks.repo.model.dao.throttle.ThrottleRulesDAO;
import org.sagebionetworks.repo.model.throttle.ThrottleLimit;
import org.sagebionetworks.repo.model.throttle.ThrottleRule;
import org.springframework.beans.factory.annotation.Autowired;


public class ThrottleRulesCache {
	private Map<String, ThrottleLimit> throttleRulesMap;
	
	@Autowired
	private ThrottleRulesDAO throttleRulesDao;
	
	public synchronized ThrottleLimit getThrottleLimit(String normalizedUri){
			return throttleRulesMap.get(normalizedUri);
	}
	
	public synchronized void updateRules(){
		List<ThrottleRule> rules = throttleRulesDao.getAllThrottles();
		throttleRulesMap.clear();
		for(ThrottleRule rule : rules){
			throttleRulesMap.put(rule.getNormalizedUri(), new ThrottleLimit(rule.getMaxCalls(), rule.getCallPeriodSec()));
		}
	}

}
