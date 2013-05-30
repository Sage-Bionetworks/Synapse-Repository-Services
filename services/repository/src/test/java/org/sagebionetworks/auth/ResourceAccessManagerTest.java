package org.sagebionetworks.auth;

import static org.junit.Assert.assertEquals;

import java.net.URLEncoder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ResourceAccessManagerTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test 
	public void testResourceAccessToken() throws Exception {
		// generate a resource access token and then check that it's URL safe
		String sessionToken = "zfcN5W9ZM0NknT2NvPUPBg00";
		String resourceName = "ComputeResource";
		String encryptionKey = "20c832f5c262b9d228c721c190567ae0";
		String resourceAccessToken = ResourceAccessManager.createResourceAccessToken(sessionToken, resourceName, encryptionKey);
		// we check URL safety by seeing if URL-encoding changes the string.
		// the one adjustment we have to make is to change '+' to ' ' (space), since
		// '+' is URL safe but is percent-encoded, since space is URL encoded as '+'.
		assertEquals(resourceAccessToken, URLEncoder.encode(resourceAccessToken.replace('+', ' '), "UTF-8"));
	}

}
