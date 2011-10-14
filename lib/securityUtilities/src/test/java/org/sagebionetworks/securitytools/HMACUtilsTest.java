package org.sagebionetworks.securitytools;

import org.joda.time.DateTime;
import org.junit.Test;


/**
 * Unit test for HMACUtilsTest
 */
public class HMACUtilsTest {
	
	@Test
	public void testHMACUtils() throws Exception {
		DateTime timeStamp = new DateTime();
		System.out.println(timeStamp);

//		String timeStampString = "";
//		String userId="demouser@sagebase.org";
//		String uri="/repo/v1/dataset";
//		String uri="/services-repository-0.7-SNAPSHOT/repo/v1/dataset";
//		String base64EncodedSecretKey = "nUielh3l3rHuZis4JQ/4sr05N8ounV8OnQsZqmjmHnD2r1ITmJQSkr4WmM37e5Fi81lQ+WdZ794G6KEDMx/NKw==";
		String base64EncodedSecretKey = "pDfk2KtmuvwFNKJzOn16ZfIY5qbSDebNFpTPHd6DuGemivMLWCV3tBFny6qGQ3luwXW7Q13IL3SUYC29mXeKdg==";

//		String timeStampString = timeStamp.toString(); //"2011-09-28T13:31:16.90-0700";
		
		
		String userId="matt.furia@sagebase.org";
		String uri="/repo/v1/entity/17428/type";
		String timeStampString = "2011-10-07T00:09:40.44-0700";

		
		String encoded = HMACUtils.generateHMACSHA1Signature(
				userId,
	    		uri,
	    		timeStampString,
	    		base64EncodedSecretKey);
		
//		encoded = new String(HMACUtils.generateHMACSHA1SignatureFromBase64EncodedKey("matt.furia@sagebase.org/repo/v1/entity/17428/type2011-10-07T00:09:40.44-0700", base64EncodedSecretKey));
		if (true) System.out.println("Secret key: "+base64EncodedSecretKey+"\nData: "+userId+uri+timeStampString+"\nHash for data: "+encoded);
	}
	
}
