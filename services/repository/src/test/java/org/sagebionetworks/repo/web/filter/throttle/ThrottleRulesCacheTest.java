package org.sagebionetworks.repo.web.filter.throttle;



import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.dao.throttle.ThrottleRulesDAO;
import org.sagebionetworks.repo.model.throttle.ThrottleLimit;
import org.sagebionetworks.repo.model.throttle.ThrottleRule;
import org.springframework.test.util.ReflectionTestUtils;
@RunWith(MockitoJUnitRunner.class)
public class ThrottleRulesCacheTest {
	@Mock
	private ThrottleRulesDAO throttleRulesDao;
	
	private ThrottleRulesCache throttleRulesCache;

	@Before
	public void setUp() throws Exception {
		throttleRulesCache = new ThrottleRulesCache();
		
		ReflectionTestUtils.setField(throttleRulesCache, "throttleRulesDao", throttleRulesDao);
	}
	
	@Test
	public void testTimerFiredUpdateCache(){
		//set up list with 2 rules
		List<ThrottleRule> rules = new LinkedList<ThrottleRule>();
		String path1 = "never/gonna/give/you/up";
		long maxCalls1 = 123;
		long callPeriod1 = 456;
		String path2 = "/never/gonna/let/you/down";
		long maxCalls2 = 789;
		long callPeriod2 = 420;
		rules.add(new ThrottleRule(0, path1, maxCalls1, callPeriod1));
		rules.add(new ThrottleRule(1, path2, maxCalls2, callPeriod2));
		
		when(throttleRulesDao.getAllThrottleRules()).thenReturn(rules);
		
		//initially empty cache
		assertEquals(0, throttleRulesCache.getNumThrottleRules());
		
		throttleRulesCache.timerFired();
		assertEquals(2, throttleRulesCache.getNumThrottleRules());
		
		//verify the values in the cache
		ThrottleLimit limit1 = throttleRulesCache.getThrottleLimit(path1);
		assertEquals(maxCalls1, limit1.getMaxCallsPerUserPerPeriod());
		assertEquals(callPeriod1, limit1.getCallPeriodSec());
		
		ThrottleLimit limit2 = throttleRulesCache.getThrottleLimit(path2);
		assertEquals(maxCalls2, limit2.getMaxCallsPerUserPerPeriod());
		assertEquals(callPeriod2, limit2.getCallPeriodSec());
	}
	
	@Test
	public void testGetThrottleLimitNonExistantLimit(){
		assertNull(throttleRulesCache.getThrottleLimit("/never/gonna/run/around/and/desert/you"));
	}
	
	@Test
	public void testGetLastUpdatedNoUpdates(){
		assertEquals(0, throttleRulesCache.getLastUpdated());
	}
	
	@Test
	public void testGetLastUpdatedAfterUpdate(){
		when(throttleRulesDao.getAllThrottleRules()).thenReturn(new LinkedList<ThrottleRule>());
		throttleRulesCache.timerFired();
		assertTrue(throttleRulesCache.getLastUpdated() > 0);
	}
	
	/**
	 * PLFM-4073
	 */
	@Test
	public void testTimerFiredAddsLowerCase(){
		long testID = 42;
		String testPath = "/QwErTyAsDf/fakePath";
		long testMaxCalls = 123;
		long testPeriod = 456;
		ThrottleRule throttleRule = new ThrottleRule(testID, testPath, testMaxCalls,testPeriod);
		
		when(throttleRulesDao.getAllThrottleRules()).thenReturn(Arrays.asList(new ThrottleRule[] {throttleRule}));
		throttleRulesCache.timerFired();
		
		assertNull(throttleRulesCache.getThrottleLimit(testPath));
		
		ThrottleLimit limit = throttleRulesCache.getThrottleLimit(testPath.toLowerCase());
		assertNotNull(limit);
		
		assertEquals(testMaxCalls, limit.getMaxCallsPerUserPerPeriod());
		assertEquals(testPeriod, limit.getCallPeriodSec());
	}

}
