package org.sagebionetworks.ids;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class UuidETagGeneratorTest {

	private ETagGenerator eTagGenerator = new UuidETagGenerator();

	@Test
	public void test() {
		Set<String> set = new HashSet<String>();
		for (int i = 0; i < 100; i++) {
			String eTag = eTagGenerator.generateETag();
			Assert.assertTrue(set.add(eTag));
			eTag = eTagGenerator.generateETag();
			Assert.assertTrue(set.add(eTag));
		}
	}
}
