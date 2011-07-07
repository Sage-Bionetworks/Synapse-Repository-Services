package org.sagebionetworks;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

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

}
