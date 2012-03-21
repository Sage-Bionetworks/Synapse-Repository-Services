package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.repo.model.DatastoreException;


/**
 * @author deflaux
 *
 */
public class KeyFactoryTest {
	
	@Test
	public void testKeyToString() throws Exception {
		assertEquals("syn123", KeyFactory.keyToString(123L));
		assertEquals("syn0", KeyFactory.keyToString(0L));
		
		// We don't anticipate negative keys, but this class does not prevent them
		assertEquals("syn-9", KeyFactory.keyToString(-9L));
	}

	@Test
	public void testStringToKey() throws Exception {
		assertEquals(Long.valueOf(123L), KeyFactory.stringToKey("syn123"));
		assertEquals(Long.valueOf(123L), KeyFactory.stringToKey(" syn123"));
	}

	@Test(expected=DatastoreException.class)
	public void testStringToKeyInvalidPrefix() throws Exception {
		KeyFactory.stringToKey("foo123");
	}
}
