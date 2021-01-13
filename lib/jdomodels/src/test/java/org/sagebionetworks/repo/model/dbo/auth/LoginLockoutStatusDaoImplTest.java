package org.sagebionetworks.repo.model.dbo.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.auth.LockoutInfo;
import org.sagebionetworks.repo.model.auth.LoginLockoutStatusDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class LoginLockoutStatusDaoImplTest {

	@Autowired
	private UserGroupDAO userGroupDao;

	@Autowired
	private LoginLockoutStatusDao loginLockoutStatusDao;

	private Long userOneId;
	private Long userTwoId;

	@BeforeEach
	public void before() {

		UserGroup ug = new UserGroup();
		ug.setIsIndividual(true);
		userOneId = userGroupDao.create(ug);
		userTwoId = userGroupDao.create(ug);
		loginLockoutStatusDao.truncateAll();
	}

	@AfterEach
	public void after() {
		loginLockoutStatusDao.truncateAll();
		if (userOneId != null) {
			userGroupDao.delete(userOneId.toString());
		}
		if (userTwoId != null) {
			userGroupDao.delete(userTwoId.toString());
		}
	}

	@Test
	public void testGetLockoutInfoWithDoesNotExist() {
		// call under test
		LockoutInfo info = loginLockoutStatusDao.getLockoutInfo(userOneId);
		LockoutInfo expected = new LockoutInfo().withNumberOfFailedLoginAttempts(0L)
				.withRemainingMillisecondsToNextLoginAttempt(0L);
		assertEquals(expected, info);
	}

	@Test
	public void testIncrementLockoutInfoWithNewTransactionWithNoData() throws InterruptedException {
		// There should be no lock info for this user at this point.
		// call under test
		loginLockoutStatusDao.incrementLockoutInfoWithNewTransaction(userOneId);
		LockoutInfo info = loginLockoutStatusDao.getLockoutInfo(userOneId);
		assertNotNull(info);
		assertEquals(1L, info.getNumberOfFailedLoginAttempts());
		assertNotNull(info.getRemainingMillisecondsToNextLoginAttempt());
		assertTrue(info.getRemainingMillisecondsToNextLoginAttempt() < (1 << 2));
	}

	/**
	 * This test might be sensitive to timing.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testIncrementLockoutInfoWithNewTransactionWithMultipleIncrements() throws InterruptedException {
		long startMS = 0L;
		// 10 attempts should lockout for 1024 MS.
		long numberOfAttempts = 10L;
		for (long i = 0; i < numberOfAttempts; i++) {
			// call under test
			startMS = System.currentTimeMillis();
			loginLockoutStatusDao.incrementLockoutInfoWithNewTransaction(userOneId);
		}
		LockoutInfo info = loginLockoutStatusDao.getLockoutInfo(userOneId);
		// This helps adjust for slow running test machines.
		long millisecondsSinceLastIncrement = System.currentTimeMillis() - startMS;
		assertNotNull(info);
		assertEquals(numberOfAttempts, info.getNumberOfFailedLoginAttempts());
		assertTrue(info.getRemainingMillisecondsToNextLoginAttempt() > (1 << numberOfAttempts - 1)
				- millisecondsSinceLastIncrement);
		assertTrue(info.getRemainingMillisecondsToNextLoginAttempt() < (1 << numberOfAttempts + 1));
		// wait for the lock to expire if it is not already.
		if (info.getRemainingMillisecondsToNextLoginAttempt() > 0) {
			Thread.sleep(info.getRemainingMillisecondsToNextLoginAttempt() + 10L);
		}
		info = loginLockoutStatusDao.getLockoutInfo(userOneId);
		assertNotNull(info);
		assertEquals(numberOfAttempts, info.getNumberOfFailedLoginAttempts());
		assertTrue(info.getRemainingMillisecondsToNextLoginAttempt() <= 0L);
	}
	
	@Test
	public void testIestIncrementLockoutInfoWithOverflowExpiration() {
		// 2^65 is larger than the 64 bit expiration time.
		long numberOfAttempts = 65L;
		for (long i = 0; i < numberOfAttempts; i++) {
			// call under test
			loginLockoutStatusDao.incrementLockoutInfoWithNewTransaction(userOneId);
		}
		LockoutInfo info = loginLockoutStatusDao.getLockoutInfo(userOneId);
		assertNotNull(info);
		assertEquals(numberOfAttempts, info.getNumberOfFailedLoginAttempts());
		// The remaining should not overflow and should remain positive.
		assertTrue(info.getRemainingMillisecondsToNextLoginAttempt() > 0L);
	}

	@Test
	public void testResetLockoutInfoWithNewTransactionWithNoData() {
		// call under test
		loginLockoutStatusDao.resetLockoutInfoWithNewTransaction(userOneId);
		LockoutInfo info = loginLockoutStatusDao.getLockoutInfo(userOneId);
		assertNotNull(info);
		assertEquals(0L, info.getNumberOfFailedLoginAttempts());
		assertTrue(info.getRemainingMillisecondsToNextLoginAttempt() <= 0L);
	}

	@Test
	public void testResetLockoutInfoWithNewTransactionWithData() {
		loginLockoutStatusDao.incrementLockoutInfoWithNewTransaction(userOneId);
		LockoutInfo info = loginLockoutStatusDao.getLockoutInfo(userOneId);
		assertNotNull(info);
		assertEquals(1L, info.getNumberOfFailedLoginAttempts());
		// call under test
		loginLockoutStatusDao.resetLockoutInfoWithNewTransaction(userOneId);
		info = loginLockoutStatusDao.getLockoutInfo(userOneId);
		assertNotNull(info);
		assertEquals(0L, info.getNumberOfFailedLoginAttempts());
		assertTrue(info.getRemainingMillisecondsToNextLoginAttempt() <= 0L);
	}

	@Test
	public void testMultipleUsers() {
		// user one is incremented twice
		loginLockoutStatusDao.incrementLockoutInfoWithNewTransaction(userOneId);
		loginLockoutStatusDao.incrementLockoutInfoWithNewTransaction(userOneId);
		// user two is incremented once
		loginLockoutStatusDao.incrementLockoutInfoWithNewTransaction(userTwoId);

		LockoutInfo info = loginLockoutStatusDao.getLockoutInfo(userOneId);
		assertNotNull(info);
		assertEquals(2L, info.getNumberOfFailedLoginAttempts());

		info = loginLockoutStatusDao.getLockoutInfo(userTwoId);
		assertNotNull(info);
		assertEquals(1L, info.getNumberOfFailedLoginAttempts());
		// rest user one
		loginLockoutStatusDao.resetLockoutInfoWithNewTransaction(userOneId);
		info = loginLockoutStatusDao.getLockoutInfo(userOneId);
		assertNotNull(info);
		assertEquals(0L, info.getNumberOfFailedLoginAttempts());
		// user two should not be changed.
		info = loginLockoutStatusDao.getLockoutInfo(userTwoId);
		assertNotNull(info);
		assertEquals(1L, info.getNumberOfFailedLoginAttempts());
	}

	@Test
	public void testGetLockoutInfoWithNullUser() {
		Long userId = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			loginLockoutStatusDao.getLockoutInfo(userId);
		}).getMessage();
		assertEquals("userId is required.", message);
	}

	@Test
	public void testIncrementLockoutInfoWithNewTransactionWithNullUser() {
		Long userId = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			loginLockoutStatusDao.incrementLockoutInfoWithNewTransaction(userId);
		}).getMessage();
		assertEquals("userId is required.", message);
	}

	@Test
	public void testResetLockoutInfoWithNewTransactionWithNullUser() {
		Long userId = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			loginLockoutStatusDao.resetLockoutInfoWithNewTransaction(userId);
		}).getMessage();
		assertEquals("userId is required.", message);
	}

	/**
	 * This test was added because we observed deadlock in the change password
	 * services. Changing the user's password would update the usergroup table in
	 * the main transaction. Since the loginlock table used to have a foreign key
	 * constraint on the usergroup, the call to reset loginlock in a new transaction
	 * would deadlock. To address the issue we removed the foreign key on the
	 * loginlock table.
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	@Test
	public void testResetWithUserGroupLock() {
		userGroupDao.touch(userOneId);
		loginLockoutStatusDao.resetLockoutInfoWithNewTransaction(userOneId);
	}
}
