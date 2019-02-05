package org.sagebionetworks.repo.model.dbo.dao;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class UnsuccessfulAttemptLockoutDAOImplTest {
	@Autowired
	UnsuccessfulAttemptLockoutDAOImpl dao;

	String key = "key1";

	@Before
	public void setUp(){

	}

	@After
	public void cleanUp(){
		dao.truncateTable();
	}

	@Test
	public void testIncrementNumFailedAttempts_updateAfterCreate(){
		assertEquals(1L, dao.incrementNumFailedAttempts(key));
		assertEquals(2L, dao.incrementNumFailedAttempts(key));
		assertEquals(3L, dao.incrementNumFailedAttempts(key));
	}

	@Test
	public void testIncrementNumFailedAttempts_differentKeys(){
		assertEquals(1L, dao.incrementNumFailedAttempts(key));
		assertEquals(1L, dao.incrementNumFailedAttempts("key2"));
	}

	@Test
	public void testGetLockoutExpirationTimestamp_noEntryExists(){
		dao.truncateTable();
		assertNull(dao.getLockoutExpirationTimestamp(key));
	}

	@Test
	public void testGetLockoutExpirationTimestamp_lessThanCurrentTimestamp(){
		dao.incrementNumFailedAttempts(key);
		assertNull(dao.getLockoutExpirationTimestamp(key));
	}

	@Test
	public void testGetLockoutExpirationTimestamp_greaterThanCurrentTimestamp(){
		dao.incrementNumFailedAttempts(key);
		dao.setExpiration(key, 4000);
		assertNotNull(dao.getLockoutExpirationTimestamp(key));
	}

	@Test
	public void testRemoveLockout(){
		//create a lockout entry that is later removed
		dao.incrementNumFailedAttempts(key);
		dao.setExpiration(key, 9001L);
		assertNotNull(dao.getLockoutExpirationTimestamp(key));

		//create a lockout entry that will not be removed
		String key2 = "key2";
		dao.incrementNumFailedAttempts(key2);
		dao.setExpiration(key2, 420L);
		assertNotNull(dao.getLockoutExpirationTimestamp(key2));

		//method under test
		dao.removeLockout(key);

		//assert only key1 removed
		assertNull(dao.getLockoutExpirationTimestamp(key));
		assertNotNull(dao.getLockoutExpirationTimestamp(key2));
	}

	@Test
	public void testSetExpiration() throws InterruptedException {
		long lockDuration = 400L;
		dao.incrementNumFailedAttempts(key);

		//set lock and sleep for 2 seconds
		dao.setExpiration(key, lockDuration);
		long oldExpiration = dao.getLockoutExpirationTimestamp(key);
		Thread.sleep(1000);

		//set lock again with the same duration
		dao.setExpiration(key, lockDuration);
		long newExpiration = dao.getLockoutExpirationTimestamp(key);

		assertTrue(oldExpiration < newExpiration);
	}
}
