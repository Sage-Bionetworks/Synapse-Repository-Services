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
		testingPolicy.activateOptions();
		verify(mockStackAccess).getIAMUserId();
		verify(mockStackAccess).getIAMUserKey();
		verify(mockStackAccess).getLogSweepingEnabled();
		verify(mockStackAccess).getS3LogBucket();
	}

	@Test
	public void testInitialize() {
		testingPolicy.initialize("", true);

	}

	@Test
	public void testIsTriggeringEvent() {
		testingPolicy.isTriggeringEvent(null, null, null, 0);
	}

	@Test
	public void testIsTriggeringEventNullRoller() {
		testingPolicy = new TimeBasedRollingToS3Policy(mockS3Provider, mockStackAccess);
		testingPolicy.isTriggeringEvent(null, null, null, 0);

	}

	@Test(expected=NullPointerException.class)
	public void testIsTriggeringEventNullTrigger() {
		testingPolicy = new TimeBasedRollingToS3Policy(mockS3Provider, mockStackAccess);
		testingPolicy.isTriggeringEvent(null, null, null, 0);

	}

}
