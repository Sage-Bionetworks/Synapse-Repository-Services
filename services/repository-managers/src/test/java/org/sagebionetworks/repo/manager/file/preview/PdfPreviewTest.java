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

public class PdfPreviewTest {
	private static final String TEST_PDF_NAME = "images/test.pdf";

	private PdfPreviewGenerator pdfPreviewGenerator;

	@BeforeClass
	public static void beforeClass() throws IOException {
		ConvertCmd convert = new ConvertCmd();
		try {
			convert.searchForCmd(convert.getCommand().get(0), PdfPreviewGenerator.IMAGE_MAGICK_SEARCH_PATH);
		} catch (FileNotFoundException e) {
			Assume.assumeNoException(e);
		}
	}

	@Before
	public void before() throws IOException, ServiceUnavailableException {
		pdfPreviewGenerator = new PdfPreviewGenerator();
	}

	@Test
	public void testGeneratePreview() throws IOException {
		InputStream in = PdfPreviewGenerator.class.getClassLoader().getResourceAsStream(TEST_PDF_NAME);
		assertNotNull("Failed to find a test file on the classpath: " + TEST_PDF_NAME, in);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PreviewOutputMetadata metaData = pdfPreviewGenerator.generatePreview(in, baos);
		baos.close();
		assertEquals("image/gif", metaData.getContentType());
		assertEquals(".gif", metaData.getExtension());
		assertTrue(baos.toByteArray().length > 0);
	}
}
