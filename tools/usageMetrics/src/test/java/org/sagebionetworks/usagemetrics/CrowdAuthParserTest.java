package org.sagebionetworks.usagemetrics;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


/**
 * Crowd is no longer in use
 */
@Deprecated
public class CrowdAuthParserTest  {
	
	@Test
	public void testRecordParser() throws Exception {
		AuthEvent ae = null;
		ae = CrowdLogParser.parseAuthEvent("50.16.26.176 - - [30/Apr/2012:04:31:42 +0000] POST /crowd/rest/usermanagement/latest/session/sAKU25skdjhkqvt0g00 HTTP/1.1 200 438 0.939");
		assertNull(ae);
		ae = CrowdLogParser.parseAuthEvent("50.16.26.176 - - [30/Apr/2012:04:31:42 +0000] GET /crowd/rest/usermanagement/latest/user?expand=attributes&username=auser@sagebase.org HTTP/1.1 200 1862 0.031");
		assertEquals("auser@sagebase.org", ae.getUserName());
		assertEquals(1335760302000L, ae.getTimestamp());
	}
	
	@Test
	public void testGetDayFromTimeStamp() throws Exception {
		DateFormat df = new SimpleDateFormat(CrowdLogParser.LOG_DATE_TIME_FORMAT);
		assertEquals(CrowdLogParser.getDayFromTimeStamp(df.parse("30/Apr/2012:04:31:42 +0000").getTime()), 
				CrowdLogParser.getDayFromTimeStamp(df.parse("30/Apr/2012:00:00:00 +0000").getTime()));
		assertFalse(CrowdLogParser.getDayFromTimeStamp(df.parse("29/Apr/2012:00:00:00 +0000").getTime()) == 
				CrowdLogParser.getDayFromTimeStamp(df.parse("30/Apr/2012:00:00:00 +0000").getTime()));
	}

}
