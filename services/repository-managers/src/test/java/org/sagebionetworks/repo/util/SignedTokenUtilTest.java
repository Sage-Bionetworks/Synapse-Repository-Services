package org.sagebionetworks.repo.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;


public class SignedTokenUtilTest {
	
	@Test
	public void testRoundTrip() {
		SignedTokenSample sample = new SignedTokenSample();
		sample.setStringField("foo");
		SignedTokenUtil.signToken(sample);
		SignedTokenUtil.validateToken(sample);
		assertNotNull(sample.getHmac());
	}

	
}
