package org.sagebionetworks.securitytools;

import static org.junit.Assert.assertTrue;

import org.apache.commons.codec.binary.Base64;
import org.junit.Assert;
import org.junit.Test;
import org.junit.platform.commons.util.StringUtils;


/**
 * Unit test for the password hashing scheme
 */
public class PBKDF2UtilsTest {
	
	@Test
	public void testPBKDF2Utils() throws Exception {
		String passHash = "{PKCS5S2}cnDeuXJkUW+sQwdTw4YlBaV0PMYvZQKc69lHAamznecCeEX9IPqpp7TjhEdJlNkV";
		
		byte[] salt = PBKDF2Utils.extractSalt(passHash);
		byte[] expectedSalt = Base64.decodeBase64("cnDeuXJkUW+sQwdTw4YlBQ==".getBytes());
		Assert.assertArrayEquals(expectedSalt, salt);
		
		String rehashed = PBKDF2Utils.hashPassword("password", expectedSalt);
		Assert.assertEquals(passHash, rehashed);
	}
	
	@Test
	public void testHashRandomSalt() throws Exception {
		String passHash = PBKDF2Utils.hashPassword("password", null);
		byte[] salt = PBKDF2Utils.extractSalt(passHash);
		Assert.assertNotNull(salt);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testBadHash_Short() throws Exception {
		String passHash = "{PKCS5S2}I'm too short";
		PBKDF2Utils.extractSalt(passHash);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testBadHash_Long() throws Exception {
		String passHash = "{PKCS5S2}WAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaayOutaHere";
		PBKDF2Utils.extractSalt(passHash);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testBadHash_Prefix() throws Exception {
		String passHash = "Oops, I forgot to add the prefix to this invalid password hash..........."; // right length though
		PBKDF2Utils.extractSalt(passHash);
	}
	
	@Test
	public void testGenerateRandomString() {
		assertTrue(StringUtils.isNotBlank(
				// method under test
				PBKDF2Utils.generateSecureRandomString()
		));
	}
	

}
