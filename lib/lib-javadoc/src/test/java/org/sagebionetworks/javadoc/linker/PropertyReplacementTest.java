package org.sagebionetworks.javadoc.linker;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class PropertyReplacementTest {

	@Test
	public void testBasic(){
		Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("to.replace.one", "foo");
		replacements.put("to.replace.two", "bar");
		String input = "<html><body>This is ${to.replace.one} and this is the ${to.replace.two}</body></html>";
		String expected = "<html><body>This is foo and this is the bar</body></html>";
		String result = PropertyReplacement.replaceProperties(input, replacements);
		assertEquals(expected, result);
	}
	
	@Test
	public void testDuplicate(){
		Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("to.replace.one", "foo");
		replacements.put("to.replace.two", "bar");
		String input = "${to.replace.one},${to.replace.two},${to.replace.one}";
		String expected = "foo,bar,foo";
		String result = PropertyReplacement.replaceProperties(input, replacements);
		assertEquals(expected, result);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testMissingValue(){
		Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("to.replace.one", "foo");
		String input = "${to.replace.one},${doesNotExist}";
		String expected = "foo,bar,foo";
		String result = PropertyReplacement.replaceProperties(input, replacements);
		assertEquals(expected, result);
	}
	
	@Test
	public void testFileNames(){
		Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("to.replace.one", "C:/file/path.text");
		String input = "File: \"${to.replace.one}\".";
		String expected = "File: \"C:/file/path.text\".";
		String result = PropertyReplacement.replaceProperties(input, replacements);
		assertEquals(expected, result);
	}
	
	@Test
	public void testFileNames2(){
		Map<String, String> replacements = new HashMap<String, String>();
		String replaced = "C:\\file\\path.text".replaceAll("\\\\","/" );
		replacements.put("to.replace.one", replaced);
		String input = "File: \"${to.replace.one}\".";
		String expected = "File: \"C:/file/path.text\".";
		String result = PropertyReplacement.replaceProperties(input, replacements);
		assertEquals(expected, result);
	}
}
