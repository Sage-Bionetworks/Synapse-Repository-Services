package org.sagebionetworks;


import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StringEncrypterTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testEncryption() throws Exception {
		StringEncrypter e = new StringEncrypter("SagePlatformEncrytionKey2011");
		String cleartext = "MyPassword";
		String encrypted = e.encrypt(cleartext);
		System.out.println(cleartext+" -> "+encrypted);
		assertEquals(e.decrypt(encrypted), cleartext);
	}
	
	@Test
	public void testMismatchedEncryptionKey() throws Exception {
		String cleartext = "MyPassword";
		StringEncrypter e1 = new StringEncrypter("SagePlatformEncrytionKey2011");
		String encrypted1 = e1.encrypt(cleartext);
		System.out.println(cleartext+" -> "+encrypted1);
		assertEquals(e1.decrypt(encrypted1), cleartext);

		StringEncrypter e2 = new StringEncrypter("ADifferentEncrytionKeyThanTheOneWeUsedBefore");
		String encrypted2 = e2.encrypt(cleartext);
		System.out.println(cleartext+" -> "+encrypted2);
		String message = null;
		try {
			e2.decrypt(encrypted1);
		}
		catch(RuntimeException e) {
			message = e.getMessage();
		}
		assertEquals("The encryption key does not match the one used to encrypt the property", message);
	}
}
