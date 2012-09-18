package org.sagebionetworks.repo.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StringUtilTest {
	
	String[] input = {"", "foo", "foo@bar.com", "foobar@bazblue.com", "foo@bar@bazblue.com"};
	String[] expectedOutput = {"", "foo", "f...o@bar.com", "foo...r@bazblue.com", "foo...r@bazblue.com"};

	@Test
	public void testObfuscateEmailAddress() {
		for (int i = 0; i < input.length; i++) {
			String actualOutput = StringUtil.obfuscateEmailAddress(input[i]);
			assertEquals("Obfuscation failed", expectedOutput[i], actualOutput);
		}
	}
}
