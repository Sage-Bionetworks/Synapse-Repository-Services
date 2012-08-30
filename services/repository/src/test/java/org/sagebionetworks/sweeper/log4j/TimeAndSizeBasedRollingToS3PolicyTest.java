package org.sagebionetworks.sweeper.log4j;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.ArrayList;

import org.apache.log4j.rolling.RollingPolicy;
import org.apache.log4j.rolling.RollingPolicyBase;
import org.apache.log4j.rolling.RolloverDescription;
import org.apache.log4j.rolling.TriggeringPolicy;
import org.apache.log4j.rolling.helper.Action;
import org.apache.log4j.rolling.helper.CompositeAction;
import org.apache.log4j.rolling.helper.FileRenameAction;
import org.apache.log4j.rolling.helper.GZCompressAction;
import org.apache.log4j.rolling.helper.ZipCompressAction;
import org.junit.Before;
import org.junit.Test;

public class TimeAndSizeBasedRollingToS3PolicyTest {

	private StackConfigAccess mockStackAccess;
	private RollingTriggeringPolicy mockRollingPolicy;
	private TriggeringPolicy mockTriggeringPolicy;
	private TimeAndSizeBasedRollingToS3Policy testingPolicy;
	private AmazonS3Provider mockS3Provider;

	@Before
	public void setup() {
		mockS3Provider = mock(AmazonS3Provider.class, RETURNS_DEEP_STUBS);
		mockStackAccess = mock(StackConfigAccess.class);
		mockRollingPolicy = mock(RollingTriggeringPolicy.class);
		mockTriggeringPolicy = mock(TriggeringPolicy.class);

		testingPolicy = new TimeAndSizeBasedRollingToS3Policy(mockS3Provider, mockStackAccess, mockRollingPolicy, mockTriggeringPolicy);
	}

	@Test
	public void testActivateOptions() {
		testingPolicy.activateOptions();
		verify(mockRollingPolicy).activateOptions();
		verify(mockTriggeringPolicy).activateOptions();
		verify(mockStackAccess).getIAMUserId();
		verify(mockStackAccess).getIAMUserKey();
		verify(mockStackAccess).getLogSweepingEnabled();
		verify(mockStackAccess).getS3LogBucket();
	}

	@Test
	public void testInitialize() {
		testingPolicy.initialize("", true);
		verify(mockRollingPolicy).initialize("", true);
	}

	@Test
	public void testIsTriggeringEvent() {
		testingPolicy.isTriggeringEvent(null, null, null, 0);
		verify(mockRollingPolicy).isTriggeringEvent(null, null, null, 0);
		verify(mockTriggeringPolicy).isTriggeringEvent(null, null, null, 0);
	}

	@Test
	public void testIsTriggeringEventSimpleRoller() {
		RollingPolicy mockSimpleRoller = mock(RollingPolicyBase.class);
		testingPolicy = new TimeAndSizeBasedRollingToS3Policy(mockS3Provider, mockStackAccess, mockSimpleRoller, mockTriggeringPolicy);
		testingPolicy.isTriggeringEvent(null, null, null, 0);
		verifyZeroInteractions(mockSimpleRoller);
		verify(mockTriggeringPolicy).isTriggeringEvent(null, null, null, 0);
	}

	@Test
	public void testIsTriggeringEventNullRoller() {
		testingPolicy = new TimeAndSizeBasedRollingToS3Policy(mockS3Provider, mockStackAccess, null, mockTriggeringPolicy);
		testingPolicy.isTriggeringEvent(null, null, null, 0);
		verify(mockTriggeringPolicy).isTriggeringEvent(null, null, null, 0);
	}

	@Test(expected=NullPointerException.class)
	public void testIsTriggeringEventNullTrigger() {
		testingPolicy = new TimeAndSizeBasedRollingToS3Policy(mockS3Provider, mockStackAccess, mockRollingPolicy, null);
		testingPolicy.isTriggeringEvent(null, null, null, 0);
		verify(mockRollingPolicy).isTriggeringEvent(null, null, null, 0);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testDiscoverTargetFileNameNull() {
		testingPolicy.discoverTargetFileName(null, "");
	}

	@Test
	public void testDiscoverTargetFileNameNullActions() {
		RolloverDescription mockRollover = mock(RolloverDescription.class);
		String currentActiveFile = "anArbitraryString";

		// Both actions are null
		assertEquals(currentActiveFile, testingPolicy.discoverTargetFileName(mockRollover, currentActiveFile));

		Action mockAction = mock(Action.class);

		// Only sync not null
		when(mockRollover.getSynchronous()).thenReturn(mockAction);
		assertNull(testingPolicy.discoverTargetFileName(mockRollover, ""));

		// Both are not null
		when(mockRollover.getAsynchronous()).thenReturn(mockAction);
		assertNull(testingPolicy.discoverTargetFileName(mockRollover, ""));

		// Only async not null
		when(mockRollover.getSynchronous()).thenReturn(null);
		assertNull(testingPolicy.discoverTargetFileName(mockRollover, ""));

	}

	@Test
	public void testExtractFileName() {
		File source = new File("source");
		File dest = new File("dest");

		GZCompressAction gzAction = new GZCompressAction(source, dest, false);
		String extractFileName = testingPolicy.extractFileName(gzAction);
		assertEquals(dest.getName(), extractFileName);

		ZipCompressAction zipAction = new ZipCompressAction(source, dest, false);
		String extractFileNameZip = testingPolicy.extractFileName(zipAction);
		assertEquals(dest.getName(), extractFileNameZip);

		FileRenameAction renameAction = new FileRenameAction(source, dest, false);
		String extractFileNameMV = testingPolicy.extractFileName(renameAction);
		assertEquals(dest.getName(), extractFileNameMV);
	}

	@Test
	public void testExtractFileNameNullResult() {
		CompositeAction compositeAction = new CompositeAction(new ArrayList<Action>(), false);
		String extractFileName = testingPolicy.extractFileName(compositeAction);
		assertNull(extractFileName);
	}

	@Test
	public void testAddSweepActionNull() {
		RolloverDescription rollover = mock(RolloverDescription.class);
		assertEquals(rollover, testingPolicy.addSweepAction(rollover, null));
	}

	@Test
	public void testMakeCompressAndSweepActionNullFilename() {
		when(mockStackAccess.getLogSweepingEnabled()).thenReturn(false);
		testingPolicy.activateOptions();

		File file = new File("");
		Action compressAction = new GZCompressAction(file, file, false);

		assertEquals(compressAction, testingPolicy.makeCompressAndSweepAction(compressAction, null));
	}

	@Test
	public void testMakeCompressAndSweepActionNullAction() {
		when(mockStackAccess.getLogSweepingEnabled()).thenReturn(true);
		when(mockStackAccess.getS3LogBucket()).thenReturn("s3LogBucket");
		testingPolicy.activateOptions();

		Action action = testingPolicy.makeCompressAndSweepAction(null, "targetFileName");
		System.out.println(action.getClass().getCanonicalName());
		assertTrue("Returned action is not a SweepAction", action instanceof SweepAction);
	}

	@Test
	public void testMakeCompressAndSweepAction() {
		when(mockStackAccess.getLogSweepingEnabled()).thenReturn(true);
		when(mockStackAccess.getS3LogBucket()).thenReturn("s3LogBucket");
		testingPolicy.activateOptions();

		File file = new File("");
		Action compressAction = new GZCompressAction(file, file, false);
		Action action = testingPolicy.makeCompressAndSweepAction(compressAction, "targetFileName");
		System.out.println(action.getClass().getCanonicalName());
		assertTrue("Returned action is not a CompositeAction", action instanceof CompositeAction);
	}
}
