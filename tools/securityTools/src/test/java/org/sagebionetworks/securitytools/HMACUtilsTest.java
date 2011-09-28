package org.sagebionetworks.securitytools;

import org.joda.time.DateTime;
import org.junit.Test;


/**
 * Unit test for HMACUtilsTest
 */
public class HMACUtilsTest {
	
	@Test
	public void testHMACUtils() throws Exception {
		String userId="demouser@sagebase.org";
		String uri="/services-repository-0.7-SNAPSHOT/repo/v1/dataset";
		DateTime timeStamp = new DateTime();
		System.out.println(timeStamp);
		String base64EncodedSecretKey = "nUielh3l3rHuZis4JQ/4sr05N8ounV8OnQsZqmjmHnD2r1ITmJQSkr4WmM37e5Fi81lQ+WdZ794G6KEDMx/NKw==";
		
		String encoded = HMACUtils.generateHMACSHA1Signature(
				userId,
	    		uri,
	    		timeStamp.toString(),
	    		base64EncodedSecretKey);
		if (false) System.out.println("Data:\n"+userId+uri+timeStamp+"\nHash for data: "+encoded);
	}
	
}
