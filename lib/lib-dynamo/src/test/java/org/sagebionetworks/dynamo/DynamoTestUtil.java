package org.sagebionetworks.dynamo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * Utility class helping run Dynamo tests.
 */
public class DynamoTestUtil {

	/**
	 * Gets a pseudo-random number as a string.  This is to salt the IDs used
	 * in tests so that multiple users can run tests over the same shared resource.
	 */
	public static String nextRandomId() {
		return Long.toString((long)(Math.random()*1000000000000.0));
	}

	/**
	 * Creates a map that maps a sequence of auto-generated letters to random IDs.
	 * Letters will be generated in a sequence like this, a, b, c, ..., aa, ab, ac, ... 
	 */
	public static Map<String, String> createRandomIdMap(int n) {

		if (n < 0) {
			throw new IllegalArgumentException();
		}

		Map<String, String> idMap = new HashMap<String, String>();
		for (int i = 0; i < n; i++) {
			String letters = "";
			int span = 'z' - 'a' + 1;
			int j = i;
			do {
				char c = (char)(j % span + 'a');
				letters = c + letters;
				j = j / span - 1;
			} while (j >= 0);
			idMap.put(letters, nextRandomId());
		}
		return idMap;
	}

	/**
	 * Creates a map that maps names to random IDs.
	 */
	public static Map<String, String> createRandomIdMap(List<String> nameList) {

		if (nameList == null) {
			throw new NullPointerException();
		}

		Map<String, String> idMap = new HashMap<String, String>();
		for (String name : nameList) {
			idMap.put(name, nextRandomId());
		}
		return idMap;
	}

	@Test
	public void test() {
		int n = 60;
		Map<String, String> idMap = createRandomIdMap(n);
		Assert.assertEquals(n, idMap.entrySet().size());
		Assert.assertTrue(idMap.containsKey("a"));
		Assert.assertTrue(idMap.containsKey("j"));
		Assert.assertTrue(idMap.containsKey("z"));
		Assert.assertTrue(idMap.containsKey("aa"));
		Assert.assertTrue(idMap.containsKey("ac"));
		Assert.assertTrue(idMap.containsKey("az"));
		Assert.assertTrue(idMap.containsKey("ba"));
		Assert.assertTrue(idMap.containsKey("bb"));
		Assert.assertTrue(idMap.containsKey("bh"));
		Assert.assertFalse(idMap.containsKey("bi"));
		Assert.assertFalse(idMap.containsKey("bm"));
		List<String> nameList = new ArrayList<String>();
		nameList.add("name1");
		nameList.add("name2");
		nameList.add("name3");
		idMap = createRandomIdMap(nameList);
		Assert.assertNotNull(idMap.get("name1"));
		Assert.assertNotNull(idMap.get("name2"));
		Assert.assertNotNull(idMap.get("name3"));
	}
}
