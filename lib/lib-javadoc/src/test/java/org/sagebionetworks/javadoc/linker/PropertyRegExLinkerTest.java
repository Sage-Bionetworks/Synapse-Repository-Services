package org.sagebionetworks.javadoc.linker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class PropertyRegExLinkerTest {

	@Test
	public void testBuildReplacement() throws IOException{
		File baseDir = new File("basedir");
		File fileOne = new File(baseDir, "org/sage/test.txt");
		File fileTwo = new File(baseDir, "org/sage/something/foo.bar");
		File index = new File(baseDir, "index.txt");
		List<FileLink> list = new LinkedList<FileLink>();
		list.add(new FileLink(fileOne, "one"));
		list.add(new FileLink(fileTwo, "two"));
		// Build relative to file one.
		Map<String, String> map = PropertyRegExLinker.buildReplacement(baseDir, fileOne, list);
		assertNotNull(map);
		assertEquals(2, map.size());
		String expectedPath = "../../org/sage/test.txt";
		assertEquals(expectedPath, map.get("one"));
		expectedPath = "../../org/sage/something/foo.bar";
		assertEquals(expectedPath, map.get("two"));
		// Now relative to file two
		map = PropertyRegExLinker.buildReplacement(baseDir, fileTwo, list);
		assertNotNull(map);
		assertEquals(2, map.size());
		expectedPath = "../../../org/sage/test.txt";
		assertEquals(expectedPath, map.get("one"));
		expectedPath = "../../../org/sage/something/foo.bar";
		assertEquals(expectedPath, map.get("two"));
		
		// relative to the index
		map = PropertyRegExLinker.buildReplacement(baseDir, index, list);
		assertNotNull(map);
		assertEquals(2, map.size());
		expectedPath = "org/sage/test.txt";
		assertEquals(expectedPath, map.get("one"));
		expectedPath = "org/sage/something/foo.bar";
		assertEquals(expectedPath, map.get("two"));
	}
}
