package org.sagebionetworks.auth;

import static org.junit.Assert.assertEquals;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SerializationTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test 
	public void testSerialization() throws Exception {
		String s = "lkasjfl;asjsdfghuihglfj";
		String e = SampleConsumer.encryptingSerializer(s);
//		System.err.println(e);
		String s2 = SampleConsumer.decryptingDeserializer(e);
		assertEquals(s, s2);
	}

}
