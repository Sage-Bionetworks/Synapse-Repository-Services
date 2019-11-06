package org.sagebionetworks.javadoc.web.services;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OptionsTests {

	@Test
	public void testOptionLength(){
		assertEquals(2, Options.optionLength("-d"));
		assertEquals(0, Options.optionLength("-foo"));
	}
	
	@Test
	public void testGetOptionValue(){
		String options[][] = new String[][]{
				new String[]{"-foo", "bar"},
				new String[]{"-d", "path"},
		};
		assertEquals("bar", Options.getOptionValue(options, "-foo"));
		assertEquals("path", Options.getOptionValue(options, "-d"));
		assertEquals(null, Options.getOptionValue(options, "-unknown"));
	}
}
