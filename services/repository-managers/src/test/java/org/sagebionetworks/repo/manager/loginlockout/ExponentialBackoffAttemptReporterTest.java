package org.sagebionetworks.repo.manager.loginlockout;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.UnsuccessfulLoginLockoutDAO;

@RunWith(MockitoJUnitRunner.class)
public class ExponentialBackoffAttemptReporterTest {
	@Mock
	UnsuccessfulLoginLockoutDAO mockUnsuccessfulLoginLockoutDAO;

	final String key = "keeeeeeeeeeeeey";

	ExponentialBackoffAttemptReporter reporter;

	@Before
	public void setUp() throws Exception {
		this.reporter = new ExponentialBackoffAttemptReporter(key, mockUnsuccessfulLoginLockoutDAO);
	}

	@Test
	public void testReportSuccess() {
		reporter.reportSuccess();
		verify(mockUnsuccessfulLoginLockoutDAO).removeLockout(key);
		verifyNoMoreInteractions(mockUnsuccessfulLoginLockoutDAO);
	}

	@Test
	public void testReportFailure() {
		when(mockUnsuccessfulLoginLockoutDAO.incrementNumFailedAttempts(key)).thenReturn(4L);
		reporter.reportFailure();
		verify(mockUnsuccessfulLoginLockoutDAO).incrementNumFailedAttempts(key);
		// 2^4= 16 seconds of expected lockout
		verify(mockUnsuccessfulLoginLockoutDAO).setExpiration(key, 16L);
		verifyNoMoreInteractions(mockUnsuccessfulLoginLockoutDAO);
	}
}