package org.sagebionetworks.javadoc;

import static org.junit.Assert.*;

import org.junit.Test;

public class FileUtilsTest {
	
	@Test
	public void testGetFileNameForClassName(){
		String result = FileUtils.getFileNameForClassName("org.sagebionetworks.Example", "html");
		System.out.println(result);
		assertEquals("org/sagebionetworks/Example.html", result);
	}
	
	@Test
	public void testPathToRoot(){
		String result = FileUtils.pathToRoot("org.sagebionetworks.samples.Example");
		System.out.println(result);
		assertEquals("../../..", result);
	}
	
}
