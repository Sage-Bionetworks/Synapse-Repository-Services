package org.sagebionetworks.repo.manager.file.preview;

import static org.junit.Assert.*;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;

import javassist.bytecode.ByteArray;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.im4java.core.ConvertCmd;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.sagebionetworks.util.TestStreams;
import org.springframework.util.StreamUtils;

public class OfficePreviewTest {
	private static final String TEST_DOC_GIF = "images/testdoc_result.gif";

	@BeforeClass
	public static void beforeClass() throws Exception {
		try {
			OfficePreviewGenerator.initialize();
		} catch (FileNotFoundException e) {
			Assume.assumeNoException(e);
		} catch (Exception e) {
			throw e;
		}
	}

	@Before
	public void before() throws IOException, ServiceUnavailableException {
	}

	@Test
	public void testGenerateDocPreview() throws IOException {
		doTestGeneratePreview("images/test.doc", "application/msword", "images/testdoc_result.gif");
	}

	@Test
	public void testGenerateDocxPreview() throws IOException {
		doTestGeneratePreview("images/test.docx", "application/msword", "images/testdocx_result.gif");
	}

	@Test
	public void testGenerateDocmPreview() throws IOException {
		doTestGeneratePreview("images/test.docm", "application/msword", "images/testdocx_result.gif");
	}

	@Test
	public void testGenerateRtfPreview() throws IOException {
		doTestGeneratePreview("images/test.rtf", "application/msword", "images/testdoc_result.gif");
	}

	@Test
	@Ignore
	public void testGenerateXmlPreview() throws IOException {
		doTestGeneratePreview("images/test_word.xml", "application/msword", "images/testdoc_result.gif");
	}

	@Test
	@Ignore
	public void testGenerateWord2003XmlPreview() throws IOException {
		doTestGeneratePreview("images/test_word2003.xml", "application/msword", "images/testdoc_result.gif");
	}

	private void doTestGeneratePreview(String docName, String mimeType, String resultImage) throws IOException {
		OfficePreviewGenerator officePreviewGenerator = new OfficePreviewGenerator();
		InputStream in = OfficePreviewGenerator.class.getClassLoader().getResourceAsStream(docName);
		assertNotNull("Failed to find a test file on the classpath: " + docName, in);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PreviewOutputMetadata metaData = officePreviewGenerator.generatePreview(in, baos);
		baos.close();
		assertEquals("image/gif", metaData.getContentType());
		assertEquals(".gif", metaData.getExtension());

		File tmp = File.createTempFile("temp", ".gif");
		FileOutputStream tmpOut = new FileOutputStream(tmp);
		StreamUtils.copy(baos.toByteArray(), tmpOut);
		tmpOut.close();
		System.out.println(tmp.getAbsolutePath());

		InputStream expected = OfficePreviewGenerator.class.getClassLoader().getResourceAsStream(resultImage);
		TestStreams.assertEquals(expected, new ByteArrayInputStream(baos.toByteArray()));
	}
}
