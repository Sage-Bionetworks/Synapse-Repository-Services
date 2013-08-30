package org.sagebionetworks.logging.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;

import org.junit.Test;

public class LogSweepUtilsTest {

	@Test
	public void testGetDateString(){
		String expected = "2059-11-31";
		String results = LogSweepUtils.getDateString(2059, 11, 31);
		assertEquals(expected, results);
	}
	
	@Test
	public void testGetDateStringTimeMS(){
	    Calendar cal = LogSweepUtils.getClaendarUTC();
		cal.set(2001, 11, 30, 22, 49);
		String expected = "2001-12-30";
		String results = LogSweepUtils.getDateString(cal.getTimeInMillis());
		assertEquals(expected, results);
	}
	
	@Test
	public void testGetKeyForFile(){
	    Calendar cal = LogSweepUtils.getClaendarUTC();
		cal.set(1999, 0, 15, 22, 49, 12);
		String results = LogSweepUtils.createKeyForFile(123, "repo-trace-profile.2013-08-29-07-07.log.gz", cal.getTimeInMillis());
		System.out.println(results);
		assertTrue(results.startsWith("000000123/repo-trace-profile/1999-01-15/22-49-12"));
		assertTrue(results.endsWith(".log.gz"));
	}
}
