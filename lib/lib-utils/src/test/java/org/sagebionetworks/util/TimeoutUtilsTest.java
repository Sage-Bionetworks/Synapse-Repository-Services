package org.sagebionetworks.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TimeoutUtilsTest {

	@Test
	public void testHasExpired() throws InterruptedException{
		TimeoutUtils utils = new TimeoutUtils();
		long eventStart = System.currentTimeMillis();
		long timeoutMS = 1000;
		assertFalse(utils.hasExpired(timeoutMS, eventStart));
		// Sleep to force expires
		Thread.sleep(timeoutMS+1);
		assertTrue(utils.hasExpired(timeoutMS, eventStart));
		
	}

}
