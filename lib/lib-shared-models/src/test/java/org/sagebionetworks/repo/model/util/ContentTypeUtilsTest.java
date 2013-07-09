package org.sagebionetworks.repo.model.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.schema.adapter.org.json.AdapterFactoryImpl;

/**
 * @author Jay
 * 
 */
public class ContentTypeUtilsTest {
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testGetContentType() throws Exception {
		String myCodeFilename = "myCodeFile.R";
		//if current content type is null or application/octet-stream, it should go with text/plain 
		assertEquals(ContentTypeUtils.PLAIN_TEXT, ContentTypeUtils.getContentType(null, myCodeFilename));
		assertEquals(ContentTypeUtils.PLAIN_TEXT, ContentTypeUtils.getContentType(ContentTypeUtils.APPLICATION_OCTET_STREAM, myCodeFilename));
		
		//but if it's something else, it should use that instead (even if it's whacky, trust the content type unless it really appears wrong)
		String myKnownContentType = "application/whackyfile";
		assertEquals(myKnownContentType, ContentTypeUtils.getContentType(myKnownContentType, myCodeFilename));
		
		//if it doesn't look like a code file, then don't try to adjust the content type
		String myBinaryFilename = "myExecutableFile.bin";
		assertEquals(null, ContentTypeUtils.getContentType(null, myBinaryFilename));
		assertEquals(ContentTypeUtils.APPLICATION_OCTET_STREAM, ContentTypeUtils.getContentType(ContentTypeUtils.APPLICATION_OCTET_STREAM, myBinaryFilename));
	}

	public void testIsRecognizedCodeFileName() throws Exception {
		assertTrue(ContentTypeUtils.isRecognizedCodeFileName("myCodeFile.R"));
		assertTrue(ContentTypeUtils.isRecognizedCodeFileName("myCodeFile.py"));
		assertTrue(ContentTypeUtils.isRecognizedCodeFileName("myCodeFile.ipy"));
		assertTrue(ContentTypeUtils.isRecognizedCodeFileName("myCodeFile.jAvA"));
		assertTrue(ContentTypeUtils.isRecognizedCodeFileName("myCodeFile.manyDots.cpp"));
		
		assertFalse(ContentTypeUtils.isRecognizedCodeFileName(null));
		assertFalse(ContentTypeUtils.isRecognizedCodeFileName(""));
		assertFalse(ContentTypeUtils.isRecognizedCodeFileName("myTextFile.txt"));
		assertFalse(ContentTypeUtils.isRecognizedCodeFileName("myImageFile.png"));
		assertFalse(ContentTypeUtils.isRecognizedCodeFileName("myZippedFile.R.zip"));
	}
}
