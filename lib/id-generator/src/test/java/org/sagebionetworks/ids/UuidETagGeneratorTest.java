package org.sagebionetworks.ids;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:id-generator.spb.xml" })
public class UuidETagGeneratorTest {

	@Autowired
	ETagGenerator eTagGenerator;

	@Test
	public void test() {
		Set<String> set = new HashSet<String>();
		for (int i = 0; i < 100; i++) {
			String eTag = eTagGenerator.generateETag(null);
			Assert.assertTrue(set.add(eTag));
		}
	}
}
