package org.sagebionetworks.upload;

import static org.junit.Assert.*;

import org.junit.Test;

public class UploadUtilsTest {

	@Test
	public void testGetContentDispositionValue(){
		String result = UploadUtils.getContentDispositionValue("foo.txt");
		assertEquals("attachment; filename=foo.txt", result);
	}
}
