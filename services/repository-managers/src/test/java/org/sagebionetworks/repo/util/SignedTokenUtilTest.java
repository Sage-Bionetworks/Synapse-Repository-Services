package org.sagebionetworks.repo.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;


public class SignedTokenUtilTest {
	
	@Test
	public void testRoundTrip() {
		SignedTokenSample sample = new SignedTokenSample();
		sample.setStringField("foo");
		String serialized = SignedTokenUtil.signAndSerialized(sample);
		System.out.println(serialized);
		SignedTokenSample deser = SignedTokenUtil.deserializeAndValidateToken(serialized, SignedTokenSample.class);
		assertNotNull(deser.getHmac());
		System.out.println(deser.getHmac());
		assertEquals(sample, deser);
	}

	
}
