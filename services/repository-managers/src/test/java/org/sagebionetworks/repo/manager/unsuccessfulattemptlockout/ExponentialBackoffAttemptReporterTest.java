package org.sagebionetworks.repo.manager.unsuccessfulattemptlockout;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.UnsuccessfulAttemptLockoutDAO;

@RunWith(MockitoJUnitRunner.class)
public class ExponentialBackoffAttemptReporterTest {
	@Mock
	UnsuccessfulAttemptLockoutDAO mockUnsuccessfulAttemptLockoutDAO;

	final String key = "keeeeeeeeeeeeey";

	ExponentialBackoffAttemptReporter reporter;

	@Before
	public void setUp() throws Exception {
		this.reporter = new ExponentialBackoffAttemptReporter(key, mockUnsuccessfulAttemptLockoutDAO);
	}

	@Test
	public void testReportSuccess() {
		reporter.reportSuccess();
		verify(mockUnsuccessfulAttemptLockoutDAO).removeLockout(key);
		verifyNoMoreInteractions(mockUnsuccessfulAttemptLockoutDAO);
	}

	@Test
	public void testReportFailure() {
		when(mockUnsuccessfulAttemptLockoutDAO.incrementNumFailedAttempts(key)).thenReturn(4L);
		reporter.reportFailure();
		verify(mockUnsuccessfulAttemptLockoutDAO).incrementNumFailedAttempts(key);
		// 2^4= 16 seconds of expected lockout
		verify(mockUnsuccessfulAttemptLockoutDAO).setExpiration(key, 16L);
		verifyNoMoreInteractions(mockUnsuccessfulAttemptLockoutDAO);
	}
}