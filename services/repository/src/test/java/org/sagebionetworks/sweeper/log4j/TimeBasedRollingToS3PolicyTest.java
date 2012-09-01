package org.sagebionetworks.sweeper.log4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.apache.log4j.rolling.RolloverDescription;
import org.junit.Before;
import org.junit.Test;

public class TimeBasedRollingToS3PolicyTest {

	private StackConfigAccess mockStackAccess;
	private TimeBasedRollingToS3Policy testingPolicy;
	private AmazonS3Provider mockS3Provider;

	@Before
	public void setup() {
		mockS3Provider = mock(AmazonS3Provider.class, RETURNS_DEEP_STUBS);
		mockStackAccess = mock(StackConfigAccess.class);

		testingPolicy = new TimeBasedRollingToS3Policy(mockS3Provider, mockStackAccess);
	}

	@Test
	public void testActivateOptions() {
		// TODO - actually add setting of options
		//  also, need tests that verify that exceptions
		//  are thrown when options aren't properly set
		testingPolicy.activateOptions();
		verify(mockStackAccess).getIAMUserId();
		verify(mockStackAccess).getIAMUserKey();
		verify(mockStackAccess).getLogSweepingEnabled();
		verify(mockStackAccess).getDeletAfterSweepingEnabled();
		verify(mockStackAccess).getS3LogBucket();
	}

	@Test
	public void testInitialize() {
		String currentActiveFile = "arbitraryFileName";
		RolloverDescription initialize = testingPolicy.initialize(currentActiveFile, true);

		assertEquals(currentActiveFile, initialize.getActiveFileName());
		assertNull(initialize.getSynchronous());
		assertNull(initialize.getAsynchronous());
	}

	@Test
	public void testIsTriggeringEvent() {
		testingPolicy.isTriggeringEvent(null, null, null, 0);
	}

}
