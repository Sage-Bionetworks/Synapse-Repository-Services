package org.sagebionetworks.repo.manager.file.preview;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

public class CsvTextPreviewTest {
	CsvPreviewGenerator textPreviewGenerator;
	String csvTestInputString = "\"contains, both delimiters\t inside of double quotes and is very long so it should be abbreviated\", second column, third column\n2nd row first column, 2nd row second column, 2nd row third column";
	InputStream from;
	@Before
	public void before() throws IOException, ServiceUnavailableException{
		from = IOUtils.toInputStream(csvTestInputString);
		textPreviewGenerator = new CsvPreviewGenerator();
	}
	
	@Test
	public void testCsvGeneratePreview() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PreviewOutputMetadata type = textPreviewGenerator.generatePreview(from, baos);
		assertEquals(TabCsvPreviewGenerator.TEXT_CSV_SEPARATED_VALUES, type.getContentType());
		String output = baos.toString();
		String[] lines = output.split("\n");
		//input csv has 2 rows
		assertEquals(2, lines.length);
		//3 columns
		assertEquals(3, lines[0].split(",").length);
	}
	

	@Test
	public void testContentType() throws IOException {
		assertTrue(textPreviewGenerator.supportsContentType(TabCsvPreviewGenerator.TEXT_CSV_SEPARATED_VALUES));
		assertFalse(textPreviewGenerator.supportsContentType(TabCsvPreviewGenerator.TEXT_TAB_SEPARATED_VALUES));
		assertFalse(textPreviewGenerator.supportsContentType("TEXT/XML"));
	}
}
