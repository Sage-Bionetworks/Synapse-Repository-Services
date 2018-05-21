package org.sagebionetworks;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.aws.AwsClientFactory;

public class AwsClientFactoryTest {

	@Test
	public void testPLFM4952Nulls() {
		String key = null;
		String value = null;
		// call under test
		AwsClientFactory.setSystemProperty(key, value);
	}
	
	@Test
	public void testPLFM4952Empty() {
		String key = "shouldNotBeSet";
		String value = "";
		// call under test
		AwsClientFactory.setSystemProperty(key, value);
		assertEquals(null, System.getProperty(key));
	}
	
	@Test
	public void testPLFM495WithValue() {
		String key = "shouldBeSet";
		String value = "someValue";
		// call under test
		AwsClientFactory.setSystemProperty(key, value);
		assertEquals(value, System.getProperty(key));
	}
}
