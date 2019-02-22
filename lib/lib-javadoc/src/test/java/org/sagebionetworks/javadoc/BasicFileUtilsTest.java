package org.sagebionetworks.javadoc;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class BasicFileUtilsTest {
	
	@Test
	public void testGetFileNameForClassName(){
		String result = BasicFileUtils.getFileNameForClassName("org.sagebionetworks.Example", "html");
		System.out.println(result);
		assertEquals("org/sagebionetworks/Example.html", result);
	}
	
	@Test
	public void testPathToRoot(){
		String result = BasicFileUtils.pathToRoot("org.sagebionetworks.samples.Example");
		System.out.println(result);
		assertEquals("../../../", result);
	}
	
	@Test
	public void testPathToRootParentChild() throws IOException{
		File temp = File.createTempFile("testPathToRootParentChild", ".txt");
		try{
			File one = new File(temp.getParentFile(), "testPathToRootParentChildone");
			File two = new File(one, "two");
			File child = new File(two, "test.txt");
			System.out.println(child.getAbsolutePath());
			String results = BasicFileUtils.pathToRoot(temp.getParentFile(), child);
			assertEquals("../../", results);
		}finally{
			temp.delete();
		}
	}
	
}
