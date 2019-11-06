package org.sagebionetworks.repo.model.dbo.throttle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.dao.throttle.ThrottleRulesDAO;
import org.sagebionetworks.repo.model.throttle.ThrottleRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOThrottleRulesImplTest {
	
	@Autowired
	ThrottleRulesDAO throttleRulesDao;
	
	private static String testUri = "/fake/uri/#/asdf/";
	private static long maxCalls = 3;
	private static long callPeriod = 30;
	
	@Before
	public void setUp() throws Exception {
		assertNotNull(throttleRulesDao);
		throttleRulesDao.clearAllThrottles();
		
	}

	@After
	public void tearDown() throws Exception {
		assertNotNull(throttleRulesDao);
		throttleRulesDao.clearAllThrottles();
	}
	
	///////////////////////
	// addThrottle() Tests
	///////////////////////

	@Test (expected = IllegalArgumentException.class)
	public void testAddThrottleNegativeId() {
		throttleRulesDao.addThrottle(new ThrottleRule(-1, testUri, maxCalls, callPeriod));
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testAddThrottleNullUri() {
		throttleRulesDao.addThrottle(new ThrottleRule(1, null, maxCalls, callPeriod));
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testAddThrottleNegativeMaxCalls() {
		throttleRulesDao.addThrottle(new ThrottleRule(1, testUri, -1, callPeriod));
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testAddThrottleNegativeCallPeriod() {
		throttleRulesDao.addThrottle(new ThrottleRule(1, testUri, 1, -1));
	}
	
	@Test (expected = DuplicateKeyException.class)
	public void testAddThrottleDuplicateUri(){
		throttleRulesDao.addThrottle(new ThrottleRule(1, testUri, maxCalls, callPeriod));
		throttleRulesDao.addThrottle(new ThrottleRule(2, testUri, maxCalls, callPeriod));
	}
	
	@Test (expected = DuplicateKeyException.class)
	public void testAddThrottleDuplicateId(){
		throttleRulesDao.addThrottle(new ThrottleRule(1, testUri, maxCalls, callPeriod));
		throttleRulesDao.addThrottle(new ThrottleRule(1, testUri + "asdf", maxCalls, callPeriod));
	}
	
	@Test
	public void testAddThrottle(){
		long id = 0;
		
		throttleRulesDao.addThrottle(new ThrottleRule(id, testUri, maxCalls, callPeriod));
		
		List<ThrottleRule> throttles = throttleRulesDao.getAllThrottleRules();
		assertEquals(1, throttles.size());
		ThrottleRule throttleRule = throttles.get(0);
		assertEquals((Long) id, throttleRule.getId());
		assertEquals(testUri, throttleRule.getNormalizedPath());
		assertEquals((Long) maxCalls, throttleRule.getMaxCallsPerPeriod());
		assertEquals((Long) callPeriod, throttleRule.getPeriod());
	}
	
	
	/////////////////////////
	// getAllThrottles Tests
	/////////////////////////
	@Test
	public void testGetAllThrottlesWithElements(){
		// add 2 throttles
		throttleRulesDao.addThrottle(new ThrottleRule(0, testUri, maxCalls, callPeriod));
		throttleRulesDao.addThrottle(new ThrottleRule(1, testUri + "asdf", maxCalls, callPeriod));
		
		List<ThrottleRule> throttles = throttleRulesDao.getAllThrottleRules();
		assertNotNull(throttles);
		assertEquals(2, throttles.size());
	}
	
	@Test
	public void testGetAllThrottleRulesNoRules(){
		//should return enmpty list
		List<ThrottleRule> throttles= throttleRulesDao.getAllThrottleRules();
		assertNotNull(throttles);
		assertEquals(0, throttles.size());
	}
}
