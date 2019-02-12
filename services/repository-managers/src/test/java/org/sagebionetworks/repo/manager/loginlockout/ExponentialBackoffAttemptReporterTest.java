package org.sagebionetworks.repo.manager.loginlockout;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.UnsuccessfulLoginLockoutDAO;
import org.sagebionetworks.repo.model.UnsuccessfulLoginLockoutDTO;

@RunWith(MockitoJUnitRunner.class)
public class ExponentialBackoffAttemptReporterTest {
	@Mock
	UnsuccessfulLoginLockoutDAO mockUnsuccessfulLoginLockoutDAO;

	final long userId = 12345L;

	final long unsuccessfulLoginCount = 3;

	UnsuccessfulLoginLockoutDTO lockoutInfo;

	ExponentialBackoffAttemptReporter reporter;

	@Before
	public void setUp() throws Exception {
		lockoutInfo = new UnsuccessfulLoginLockoutDTO(userId).withUnsuccessfulLoginCount(unsuccessfulLoginCount);
		reporter = new ExponentialBackoffAttemptReporter(lockoutInfo, mockUnsuccessfulLoginLockoutDAO);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructor_nullDTO(){
		new ExponentialBackoffAttemptReporter(null, mockUnsuccessfulLoginLockoutDAO);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructor_nullDAO(){
		new ExponentialBackoffAttemptReporter(lockoutInfo, null);
	}


	@Test
	public void testReportSuccess() {
		reporter.reportSuccess();

		verify(mockUnsuccessfulLoginLockoutDAO).deleteUnsuccessfulLoginLockoutInfo(userId);
		verifyNoMoreInteractions(mockUnsuccessfulLoginLockoutDAO);
	}

	@Test
	public void testReportFailure() {
		final long databaseTimeStamp = 3133641630824L;
		when(mockUnsuccessfulLoginLockoutDAO.getDatabaseTimestampMillis()).thenReturn(databaseTimeStamp);

		reporter.reportFailure();

		UnsuccessfulLoginLockoutDTO expected = new UnsuccessfulLoginLockoutDTO(userId)
			.withUnsuccessfulLoginCount(unsuccessfulLoginCount + 1) // 3 + 1 = 4
				.withLockoutExpiration(databaseTimeStamp + 16); // 2^4 = 16

		verify(mockUnsuccessfulLoginLockoutDAO).getDatabaseTimestampMillis();
		verify(mockUnsuccessfulLoginLockoutDAO).createOrUpdateUnsuccessfulLoginLockoutInfo(expected);
		verifyNoMoreInteractions(mockUnsuccessfulLoginLockoutDAO);
	}
}