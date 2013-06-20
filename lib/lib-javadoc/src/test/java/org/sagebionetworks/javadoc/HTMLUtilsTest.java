package org.sagebionetworks.javadoc;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

public class HTMLUtilsTest {

	@Test
	public void testCreateHTMFromTempalte() throws IOException{
		String pathToRoot = BasicFileUtils.pathToRoot("org.sagebionetworks.sample.Example");
		String body = "this is the body of the test";
		String result = HTMLUtils.createHTMFromTempalte(pathToRoot, body);
		System.out.println(result);
		assertTrue(result.indexOf(body) > 0);
		assertTrue(result.indexOf(pathToRoot) > 0);
	}
}
