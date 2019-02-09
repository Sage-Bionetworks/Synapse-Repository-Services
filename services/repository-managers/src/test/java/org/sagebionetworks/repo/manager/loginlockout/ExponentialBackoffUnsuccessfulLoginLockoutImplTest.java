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

@RunWith(MockitoJUnitRunner.class)
public class ExponentialBackoffUnsuccessfulLoginLockoutImplTest {
	@Mock
	UnsuccessfulLoginLockoutDAO mockUnsuccessfulLoginLockoutDAO;

	@InjectMocks
	ExponentialBackoffUnsuccessfulLoginLockoutImpl lockout;

	String key = "keyyyyyyyyy";

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testCheckIsLockedOut_IsLockedOut() {
		when(mockUnsuccessfulLoginLockoutDAO.getUnexpiredLockoutTimestampMillis(key)).thenReturn(12345L);

		try {
			//method under test
			lockout.checkIsLockedOut(key);
			fail("expected exception to be thrown");
		} catch (UnsuccessfulLoginLockoutException e){
			//expected
		}
	}

	@Test
	public void testCheckIsLockedOut_IsNotLockedOut(){
		when(mockUnsuccessfulLoginLockoutDAO.getUnexpiredLockoutTimestampMillis(key)).thenReturn(null);

		assertEquals(new ExponentialBackoffAttemptReporter(key, mockUnsuccessfulLoginLockoutDAO),
				lockout.checkIsLockedOut(key));
	}
}