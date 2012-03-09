package org.sagebionetworks.auth;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

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
		List<Integer> o = new ArrayList<Integer>();
		o.add(1);
		o.add(2);
		o.add(100);
		o.add(-10000);
		
//	    ByteArrayOutputStream out = new ByteArrayOutputStream();
//	    ObjectOutputStream oos = new ObjectOutputStream(out);
//	    oos.writeObject(o);
//	    oos.close();
//	    byte[] b1 = out.toByteArray();
//
//		ByteArrayInputStream bais = new ByteArrayInputStream(b1);
//		ObjectInputStream ois = new ObjectInputStream(bais);
//		try {
//			assertEquals(o, ois.readObject());
//		} catch (ClassNotFoundException e) {
//			throw new RuntimeException(e);
//		}

		String e = SampleConsumer.encryptingSerializer(o);

//	   	StringEncrypter se = new StringEncrypter(StackConfiguration.getEncryptionKey());
//	   	String serializedAndBase64EncodedDI = se.decrypt(e);
//	   	String serialized = new String(Base64.decodeBase64(serializedAndBase64EncodedDI.getBytes()));

//		System.err.println(e);
		ArrayList<Integer> o2 = SampleConsumer.decryptingDeserializer(e);
		assertEquals(o, o2);
	}

}
