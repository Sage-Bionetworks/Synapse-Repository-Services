package org.sagebionetworks.sweeper.log4j;

import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

import org.apache.log4j.rolling.RolloverDescription;
import org.apache.log4j.rolling.helper.Action;
import org.apache.log4j.rolling.helper.CompositeAction;
import org.apache.log4j.rolling.helper.FileRenameAction;
import org.apache.log4j.rolling.helper.GZCompressAction;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.amazonaws.services.s3.AmazonS3;

public class TimeBasedRollingToS3PolicyTest {

	private static final String TEST_FNP = "test-%d{yyyy-MM-ddTHH:mm:ss}.gz";
	private static final String TEST_AFN = "test.log";
	private static final String TEST_BUCKET = "bucket.test";

	private StackConfigAccess mockStackAccess;
	private TimeBasedRollingToS3Policy testingPolicy;
	private AmazonS3 mockS3Provider;

	@Before
	public void setup() {
		mockS3Provider = mock(AmazonS3.class);
		mockStackAccess = mock(StackConfigAccess.class);

		testingPolicy = new TimeBasedRollingToS3Policy(1, mockS3Provider, mockStackAccess);
	}

	@Test(expected=IllegalStateException.class)
	public void testActivateOptionsNoAFNOrFNP() {
		testingPolicy.activateOptions();
	}

	@Test(expected=IllegalStateException.class)
	public void testActivateOptionsNoAFN() {
		testingPolicy.setFileNamePattern("test-%d");
		testingPolicy.activateOptions();
	}

	@SuppressWarnings("deprecation")
	@Test(expected=IllegalStateException.class)
	public void testActivateOptionsNoFNP() {
		testingPolicy.setActiveFileName(TEST_AFN);
		testingPolicy.activateOptions();
	}

	@Test
	public void testActivateOptions() {
		setDefaultPolicyOptions();
		verify(mockStackAccess).getIAMUserId();
		verify(mockStackAccess).getIAMUserKey();
		verify(mockStackAccess).getLogSweepingEnabled();
		verify(mockStackAccess).getDeleteAfterSweepingEnabled();
		verify(mockStackAccess).getS3LogBucket();
	}

	@Test
	public void testInitialize() {
		setupStackConfigDefaults(true, false, TEST_BUCKET);
		setDefaultPolicyOptions();
		RolloverDescription initialize = testingPolicy.initialize(TEST_AFN, true);
		assertNotNull("initialize should not be null", initialize);
		assertEquals(TEST_AFN, initialize.getActiveFileName());
		assertNull(initialize.getSynchronous());
		assertNull(initialize.getAsynchronous());
	}

	@Test
	public void testRollover() throws InterruptedException {
		setupStackConfigDefaults(true, false, TEST_BUCKET);
		RolloverDescription rollover = doRolloverTest();

		Action synchronous = rollover.getSynchronous();
		Action asynchronous = rollover.getAsynchronous();

		assertNotNull("Synchronous action should not be null", synchronous);
		assertNotNull("Asynchronous action should not be null", asynchronous);
		assertTrue(synchronous instanceof FileRenameAction);
		assertTrue(asynchronous instanceof CompositeAction);
	}

	@Test
	public void testRolloverNoSweep() throws InterruptedException {
		setupStackConfigDefaults(false, false, TEST_BUCKET);
		RolloverDescription rollover = doRolloverTest();

		Action synchronous = rollover.getSynchronous();
		Action asynchronous = rollover.getAsynchronous();

		assertNotNull("Synchronous action should not be null", synchronous);
		assertNotNull("Asynchronous action should not be null", asynchronous);
		assertTrue(synchronous instanceof FileRenameAction);
		assertTrue(asynchronous instanceof GZCompressAction);
	}

	private RolloverDescription doRolloverTest() throws InterruptedException {
		setDefaultPolicyOptions();
		RolloverDescription initialize = testingPolicy.initialize(TEST_AFN, true);
		Thread.sleep(1000);
		RolloverDescription rollover = testingPolicy.rollover(TEST_AFN);

		assertNotNull("initialize should not be null", initialize);
		assertNotNull("Rollover should not be null", rollover);
		assertEquals(TEST_AFN, rollover.getActiveFileName());
		return rollover;
	}

	@Test
	public void testIsTriggeringEvent() {
		testingPolicy.isTriggeringEvent(null, null, null, 0);
	}

	@SuppressWarnings("deprecation")
	private void setDefaultPolicyOptions() {
		testingPolicy.setActiveFileName(TEST_AFN);
		testingPolicy.setFileNamePattern(TEST_FNP);
		testingPolicy.activateOptions();
	}

	private void setupStackConfigDefaults(boolean sweeping, boolean deleteAfter, String s3Bucket) {
		when(mockStackAccess.getDeleteAfterSweepingEnabled()).thenReturn(deleteAfter);
		when(mockStackAccess.getLogSweepingEnabled()).thenReturn(sweeping);
		when(mockStackAccess.getS3LogBucket()).thenReturn(s3Bucket);
	}

}
