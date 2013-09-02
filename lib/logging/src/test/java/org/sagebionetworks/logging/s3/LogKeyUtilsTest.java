package org.sagebionetworks.logging.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.Calendar;

import org.junit.Test;

public class LogKeyUtilsTest {

	@Test
	public void testGetDateString(){
		String expected = "2059-11-31";
		String results = LogKeyUtils.getDateString(2059, 11, 31);
		assertEquals(expected, results);
	}
	
	@Test
	public void testGetDateStringTimeMS(){
	    Calendar cal = LogKeyUtils.getClaendarUTC();
		cal.set(2001, 11, 30, 22, 49);
		String expected = "2001-12-30";
		String results = LogKeyUtils.getDateString(cal.getTimeInMillis());
		assertEquals(expected, results);
	}
	
	@Test
	public void testGetKeyForFile(){
	    Calendar cal = LogKeyUtils.getClaendarUTC();
		cal.set(1999, 0, 15, 22, 49, 12);
		String results = LogKeyUtils.createKeyForFile(123, "repo-trace-profile.2013-08-29-07-07.log.gz", cal.getTimeInMillis());
		System.out.println(results);
		assertTrue(results.startsWith("000000123/repo-trace-profile/1999-01-15/22-49-12"));
		assertTrue(results.endsWith(".log.gz"));
	}
	
	@Test
	public void testCreateISO8601GMTLogString(){
		long time = 1327532399842l;
		String results = LogKeyUtils.createISO8601GMTLogString(time);
		System.out.println(time+"="+results);
		String expected = "2012-01-25 22:59:59,842";
		assertEquals(expected, results);
	}
	
	@Test
	public void testISO8601GMTRoundTrip() throws ParseException{
	    Calendar cal = LogKeyUtils.getClaendarUTC();
		cal.set(Calendar.YEAR, 1974);
		cal.set(Calendar.MONTH, 4);
		cal.set(Calendar.DAY_OF_MONTH, 20);
		cal.set(Calendar.HOUR_OF_DAY, 13);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 21);
		cal.set(Calendar.MILLISECOND, 123);
		long time = cal.getTimeInMillis();
		String results = LogKeyUtils.createISO8601GMTLogString(time);
		System.out.println(time+"="+results);
		String expected = "1974-05-20 13:59:21,123";
		assertEquals(expected, results);
		// Now parse this string 
		long parsed = LogKeyUtils.readISO8601GMTFromString(expected+" plus some extra stuff");
		assertEquals(time, parsed);
	}
}
