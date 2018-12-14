package org.sagebionetworks.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ContentDispositionUtilsTest {

	@Test
	public void testGetContentDispositionValue(){
		String result = ContentDispositionUtils.getContentDispositionValue("foo.txt");
		assertEquals("attachment; filename=\"foo.txt\"", result);
	}
}
