package org.sagebionetworks.repo.manager.file.preview;


import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PreviewGeneratorUtilsTest {
	
	@Test
	public void testFindExtension() {
		assertEquals("noextension", PreviewGeneratorUtils.findExtension(""));
		assertEquals("noextension", PreviewGeneratorUtils.findExtension("."));
		assertEquals("noextension", PreviewGeneratorUtils.findExtension(".s"));
		assertEquals("noextension", PreviewGeneratorUtils.findExtension(".sdfsfsd"));
		assertEquals("noextension", PreviewGeneratorUtils.findExtension("s."));
		assertEquals("noextension", PreviewGeneratorUtils.findExtension("sasdas."));

		assertEquals("a", PreviewGeneratorUtils.findExtension("x.a"));
		assertEquals("aa", PreviewGeneratorUtils.findExtension("x.aa"));
		assertEquals("aa", PreviewGeneratorUtils.findExtension("x.bb.aa"));
		assertEquals("aa", PreviewGeneratorUtils.findExtension(".bb.aa"));
	}
}
