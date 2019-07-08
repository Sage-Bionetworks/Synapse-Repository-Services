package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;


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
		assertEquals(Long.valueOf(0L), KeyFactory.stringToKey("syn0"));
		
		// Case should not matter
		assertEquals(Long.valueOf(123L), KeyFactory.stringToKey("SYN123"));
		assertEquals(Long.valueOf(0L), KeyFactory.stringToKey("SYN0"));
		
		// Whitespace should be ignored
		assertEquals(Long.valueOf(123L), KeyFactory.stringToKey("\tsyn123"));
		assertEquals(Long.valueOf(0L), KeyFactory.stringToKey("syn0   "));
		assertEquals(Long.valueOf(123L), KeyFactory.stringToKey(" syn123\n"));
		assertEquals(Long.valueOf(0L), KeyFactory.stringToKey("\nsyn0 \t"));

		// URLEncoded whitespace should be ignored
		assertEquals(Long.valueOf(123L), KeyFactory.stringToKey("+syn123+"));
		assertEquals(Long.valueOf(0L), KeyFactory.stringToKey("syn0%0d%0a"));
	}

	@Test
	public void testStringToKeyInvalidPrefix() throws Exception {
		try {
			KeyFactory.stringToKey("foo123");
			fail("Should have thrown an exception.");
		} catch (IllegalArgumentException e) {
			String expectedMessage = "foo123"+KeyFactory.IS_NOT_A_VALID_SYNAPSE_ID_SUFFIX;
			assertEquals(expectedMessage, e.getMessage());
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void testStringToKeyNonNumericCharacters() throws Exception {
		KeyFactory.stringToKey("syn/123");
	}
	
	@Test
	public void testStringToKeyList(){
		List<String> in = Lists.newArrayList("syn123","456");
		List<Long> expected = Lists.newArrayList(123L, 456L);
		// call under test
		List<Long> results = KeyFactory.stringToKey(in);
		assertEquals(expected, results);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testStringToKeyListNull(){
		KeyFactory.stringToKey((List<String>)null);
	}
	
	/**
	 * See PLFM-1533
	 */
	@Test
	public void testEquals(){
		// test the various flavors of parent id change
		assertFalse(KeyFactory.equals(null, "syn123"));
		assertFalse(KeyFactory.equals("syn123", null));
		assertFalse(KeyFactory.equals("syn1", "syn2"));
		assertFalse(KeyFactory.equals("syn2", "syn1"));
		assertTrue(KeyFactory.equals(null, null));
		assertTrue(KeyFactory.equals("syn1", "syn1"));
		assertTrue(KeyFactory.equals("1", "syn1"));
		assertTrue(KeyFactory.equals("syn1", "1"));
		assertTrue(KeyFactory.equals("SYN1", " syn1 "));
	}
	
	@Test
	public void testCompare() {
		assertEquals(0, KeyFactory.compare(null, null));
		assertEquals(1, KeyFactory.compare("syn123", null));
		assertEquals(-1, KeyFactory.compare(null, "syn123"));
		assertEquals(0, KeyFactory.compare("syn123", "syn123"));
		assertEquals(0, KeyFactory.compare("123", "syn123"));
		assertEquals(0, KeyFactory.compare("syn123", "123"));
		assertEquals(1, KeyFactory.compare("syn1234", "syn123"));
		assertEquals(1, KeyFactory.compare("1234", "syn123"));
		assertEquals(1, KeyFactory.compare("syn1234", "123"));
		assertEquals(-1, KeyFactory.compare("syn123", "syn1234"));
		assertEquals(-1, KeyFactory.compare("123", "syn1234"));
		assertEquals(-1, KeyFactory.compare("syn123", "1234"));
	}
}
