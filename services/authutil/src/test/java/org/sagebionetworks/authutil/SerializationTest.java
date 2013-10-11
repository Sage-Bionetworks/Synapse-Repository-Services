package org.sagebionetworks.authutil;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class SerializationTest {
	
	@Test 
	public void testSerialization() throws Exception {
		List<Integer> o = new ArrayList<Integer>();
		o.add(1);
		o.add(2);
		o.add(100);
		o.add(-10000);
		
		String e = BasicOpenIDConsumer.encryptingSerializer(o);

		ArrayList<Integer> o2 = BasicOpenIDConsumer.decryptingDeserializer(e);
		assertEquals(o, o2);
	}

}
