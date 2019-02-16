package org.sagebionetworks.repo.manager.loginlockout;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.UnsuccessfulLoginLockoutDAO;
import org.sagebionetworks.repo.model.UnsuccessfulLoginLockoutDTO;

@RunWith(MockitoJUnitRunner.class)
public class ExponentialBackoffUnsuccessfulLoginLockoutImplTest {
	@Mock
	UnsuccessfulLoginLockoutDAO mockUnsuccessfulLoginLockoutDAO;

	@InjectMocks
	ExponentialBackoffUnsuccessfulLoginLockoutImpl lockout;

	long userId = 123456L;

	UnsuccessfulLoginLockoutDTO unsuccessfulLoginLockoutInfo;

	long databaseTimestampMillis = 7956915742222L;

	@Before
	public void setUp() throws Exception {
		unsuccessfulLoginLockoutInfo = new UnsuccessfulLoginLockoutDTO(userId);
		when(mockUnsuccessfulLoginLockoutDAO.getDatabaseTimestampMillis()).thenReturn(databaseTimestampMillis);
		when(mockUnsuccessfulLoginLockoutDAO.getUnsuccessfulLoginLockoutInfoIfExist(userId)).thenReturn(unsuccessfulLoginLockoutInfo);
	}

	@Test
	public void testCheckIsLockedOut_IsLockedOut() {
		unsuccessfulLoginLockoutInfo.setLockoutExpiration(databaseTimestampMillis + 1);

		try {
			//method under test
			lockout.checkIsLockedOut(userId);
			fail("expected exception to be thrown");
		} catch (UnsuccessfulLoginLockoutException e){
			//expected
		}
	}

	@Test
	public void testCheckIsLockedOut_IsNotLockedOut_becauseDAOreturnedNull(){
		when(mockUnsuccessfulLoginLockoutDAO.getUnsuccessfulLoginLockoutInfoIfExist(userId)).thenReturn(null);

		assertEquals(new ExponentialBackoffLoginAttemptReporter(unsuccessfulLoginLockoutInfo, mockUnsuccessfulLoginLockoutDAO),
				lockout.checkIsLockedOut(userId));
	}

	@Test
	public void testCheckIsLockedOut_IsNotLockedOut_becauseDAOreturnedInfoWithExpired(){
		unsuccessfulLoginLockoutInfo.setLockoutExpiration(databaseTimestampMillis);

		assertEquals(new ExponentialBackoffLoginAttemptReporter(unsuccessfulLoginLockoutInfo, mockUnsuccessfulLoginLockoutDAO),
				lockout.checkIsLockedOut(userId));
	}
}