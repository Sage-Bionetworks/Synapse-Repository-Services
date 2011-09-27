package org.sagebionetworks.securitytools;

import org.junit.Test;


/**
 * Unit test for HMACUtilsTest
 */
public class HMACUtilsTest {
	
	@Test
	public void testHMACUtils() throws Exception {
		String secretKey = HMACUtils.newHMACSHA1Key();
		System.out.println("base64 encoded secret key: "+secretKey);
		String data = "My dog has fleas. ";
		String encoded = HMACUtils.generateHMACSHA1Signature(data,  secretKey);
		System.out.println("Data:\n"+data+"\nHash for data: "+encoded);
//		String base64Decoded = new String(Base64.decodeBase64(encoded.getBytes()));
//		System.out.println("hash"+base64Decoded+"\nlength of hash (not base64 encoded) "+base64Decoded.length());
//		System.out.println("Base64 test: 'My dog has fleas.'->"+(new String(Base64.encodeBase64(data.getBytes()))));
	}
	
}
