package org.sagebionetworks.repo.manager.file.preview;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

public class TabTextPreviewTest {
	TabPreviewGenerator textPreviewGenerator;
	
	String tabTestInputString = "\"some quoted text\"\t \"second\tcolumn\"\t third column\n2nd row first column\t2nd row second column\t2nd row third column";
	InputStream from;
	@Before
	public void before() throws IOException, ServiceUnavailableException{
		from = IOUtils.toInputStream(tabTestInputString);
		textPreviewGenerator = new TabPreviewGenerator();
	}
	
	@Test
	public void testTabGeneratePreview() throws IOException {
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
	public void testMaxColumnsTabGeneratePreview() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < TabCsvPreviewGenerator.MAX_COLUMN_COUNT + 10; i++) {
			sb.append(i + "\t");
		}
		from = IOUtils.toInputStream(sb.toString());
		PreviewOutputMetadata type = textPreviewGenerator.generatePreview(from, baos);
		assertEquals(TabCsvPreviewGenerator.TEXT_CSV_SEPARATED_VALUES, type.getContentType());
		String output = baos.toString();
		String[] lines = output.split("\n");
		//input csv has 1 row
		assertEquals(1, lines.length);
		//input has more than MAX_COLUMN_COUNT, so output should have been truncated to MAX_COLUMN_COUNT columns (with an additional column that has "...")
		assertEquals(TabCsvPreviewGenerator.MAX_COLUMN_COUNT+1, lines[0].split(",").length);
	}
	
	@Test
	public void testMaxRowsTabGeneratePreview() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < TabCsvPreviewGenerator.MAX_ROW_COUNT + 10; i++) {
			sb.append(i + "\n");
		}
		from = IOUtils.toInputStream(sb.toString());
		PreviewOutputMetadata type = textPreviewGenerator.generatePreview(from, baos);
		assertEquals(TabCsvPreviewGenerator.TEXT_CSV_SEPARATED_VALUES, type.getContentType());
		String output = baos.toString();
		String[] lines = output.split("\n");
		//input csv has more than MAX_ROW_COUNT, so output should have been truncated to MAX_ROW_COUNT+1 rows
		assertEquals(TabCsvPreviewGenerator.MAX_ROW_COUNT+1, lines.length);
		//input csv has 1 column
		assertEquals(1, lines[0].split(",").length);
	}
	
	@Test
	public void testMaxColumnsAndRowsTabGeneratePreview() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < TabCsvPreviewGenerator.MAX_ROW_COUNT + 10; i++) {
			for (int j = 0; j < TabCsvPreviewGenerator.MAX_COLUMN_COUNT + 10; j++) {
				sb.append("row " + i + " column " + j + "\t");
			}
			sb.append("\n");
		}
		from = IOUtils.toInputStream(sb.toString());
		PreviewOutputMetadata type = textPreviewGenerator.generatePreview(from, baos);
		assertEquals(TabCsvPreviewGenerator.TEXT_CSV_SEPARATED_VALUES, type.getContentType());
		String output = baos.toString();
		String[] lines = output.split("\n");
		//input csv has more than MAX_ROW_COUNT, so output should have been truncated to MAX_ROW_COUNT+1 rows
		assertEquals(TabCsvPreviewGenerator.MAX_ROW_COUNT+1, lines.length);
		//input csv has more than MAX_COLUMN_COUNT
		assertEquals(TabCsvPreviewGenerator.MAX_COLUMN_COUNT+1, lines[0].split(",").length);
	}

	@Test
	public void testContentType() throws IOException {
		assertTrue(textPreviewGenerator.supportsContentType(TabCsvPreviewGenerator.TEXT_TAB_SEPARATED_VALUES));
		assertFalse(textPreviewGenerator.supportsContentType(TabCsvPreviewGenerator.TEXT_CSV_SEPARATED_VALUES));
		assertFalse(textPreviewGenerator.supportsContentType("TEXT/XML"));
	}
}
