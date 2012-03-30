package org.sagebionetworks.utils;

import org.junit.Ignore;
import org.junit.Test;

public class EmailUtilsTest {

	@Ignore
	@Test
	public void test() {
		EmailUtils.sendMail("integration.test@sagebase.org", "test subject", "test body");
	}

}
