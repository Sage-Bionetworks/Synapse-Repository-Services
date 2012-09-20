package org.sagebionetworks.sweeper.log4j;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.rolling.RollingFileAppender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.amazonaws.services.s3.AmazonS3;

public class TimeBasedRollingToS3PolicyIntegrationTest {
	Logger logger = Logger
			.getLogger(TimeBasedRollingToS3PolicyIntegrationTest.class);

	@Before
	public void setUp() {
		Logger root = Logger.getRootLogger();
		root.addAppender(new ConsoleAppender(new PatternLayout(
				"%d{ABSOLUTE} [%t] %l %c{2}#%M:%L - %m%n")));
	}

	@After
	public void tearDown() {
		LogManager.shutdown();
	}

	/**
	 * With compression, activeFileName set, no stop/restart,
	 */
	@Test
	public void test6() throws Exception {
		PatternLayout layout = new PatternLayout("%c{1} - %m%n");
		RollingFileAppender rfa = new RollingFileAppender();
		rfa.setLayout(layout);

		String datePattern = "yyyy-MM-dd_HH_mm_ss";

		AmazonS3 mockS3 = Mockito.mock(AmazonS3.class);
		StackConfigAccess mockStackAccess = Mockito.mock(StackConfigAccess.class);

		Mockito.when(mockStackAccess.getDeleteAfterSweepingEnabled()).thenReturn(false);
		Mockito.when(mockStackAccess.getLogSweepingEnabled()).thenReturn(false);
		Mockito.when(mockStackAccess.getS3LogBucket()).thenReturn("log-test-bucket");

		String logDirectoryPrefix = "target/logs/";
		TimeBasedRollingToS3Policy tbrp = new TimeBasedRollingToS3Policy(1, mockS3, mockStackAccess);
		tbrp.setFileNamePattern(logDirectoryPrefix + "test6-%d{" + datePattern + "}.gz");
		tbrp.setActiveFileName(logDirectoryPrefix + "test6.log");
		rfa.setFile("target/logs/test6.log");
		rfa.setRollingPolicy(tbrp);
		rfa.setAppend(false);
		rfa.setThreshold(Level.DEBUG);
		rfa.activateOptions();
		logger.addAppender(rfa);

		SimpleDateFormat sdf = new SimpleDateFormat(datePattern);
		String[] filenames = new String[4];

		Calendar cal = Calendar.getInstance();

		for (int i = 0; i < 3; i++) {
			filenames[i] = logDirectoryPrefix + "test6-" + sdf.format(cal.getTime()) + ".gz";
			cal.add(Calendar.SECOND, 1);
		}

		filenames[3] = logDirectoryPrefix + "test6.log";

		System.out.println("Waiting until next second and 100 millis.");
		delayUntilNextSecond(100);
		System.out.println("Done waiting.");

		for (int i = 0; i < 5; i++) {
			logger.info("Hello---" + i);
			Thread.sleep(500);
		}

		rfa.close();

		for (int i = 0; i < 3; i++) {
			assertTrue(gzCompare(filenames[i], "tbrts3-test." + i
					+ ".gz"));
		}

		assertTrue(compare(filenames[3], "tbrts3-test.3"));

	}

	void delayUntilNextSecond(int millis) {
		long now = System.currentTimeMillis();
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date(now));

		cal.set(Calendar.MILLISECOND, millis);
		cal.add(Calendar.SECOND, 1);

		long next = cal.getTime().getTime();

		try {
			Thread.sleep(next - now);
		} catch (Exception e) {
		}
	}

	private static boolean gzCompare(final String actual, final String expected)
			throws IOException {
		return Compare.gzCompare(
				TimeBasedRollingToS3PolicyIntegrationTest.class, actual,
				expected);
	}

	private static boolean compare(final String actual, final String expected)
			throws IOException {
		return Compare.compare(TimeBasedRollingToS3PolicyIntegrationTest.class, actual, expected);
	}

}
