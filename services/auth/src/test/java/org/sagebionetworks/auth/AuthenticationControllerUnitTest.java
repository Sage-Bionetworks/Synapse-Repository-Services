package org.sagebionetworks.auth;

import static org.junit.Assert.*;

import java.net.URLEncoder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AuthenticationControllerUnitTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test 
	public void test() throws Exception {
		// generate a resource access token and then check that it's URL safe
		String sessionToken = "zfcN5W9ZM0NknT2NvPUPBg00";
		String encryptionKey = "20c832f5c262b9d228c721c190567ae0";
		String encryptedSessionToken = AuthenticationController.encryptString(sessionToken, encryptionKey);
		String decryptedSessionToken = AuthenticationController.decryptedString(encryptedSessionToken, encryptionKey);
		//verify session token was changed
		assertTrue(!sessionToken.equals(encryptedSessionToken));
		//test round trip
		assertEquals(sessionToken, decryptedSessionToken);
	}

}
