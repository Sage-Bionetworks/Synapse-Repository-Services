package org.sagebionetworks.repo.manager.file.preview;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

public class TabCsvTextPreviewTest {
	TabCsvPreviewGenerator tabPreviewGenerator, csvPreviewGenerator;
	
	
	@Before
	public void before() throws IOException, ServiceUnavailableException{
		tabPreviewGenerator = new TabCsvPreviewGenerator(TabCsvPreviewGenerator.TAB);
		csvPreviewGenerator = new TabCsvPreviewGenerator(TabCsvPreviewGenerator.COMMA);
	}
	
	@Test
	public void testTabGeneratePreview() throws IOException {
		String tabTestInputString = "\"some quoted text\"\t \"second\tcolumn\"\t third column\n2nd row first column\t2nd row second column\t2nd row third column";
		InputStream from = IOUtils.toInputStream(tabTestInputString);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PreviewOutputMetadata type = tabPreviewGenerator.generatePreview(from, baos);
		assertEquals(TabCsvPreviewGenerator.TEXT_CSV_SEPARATED_VALUES, type.getContentType());
		String output = baos.toString();
		String[] lines = output.split("\n");
		//input csv has 2 rows
		assertEquals(2, lines.length);
		//3 columns
		assertEquals(3, lines[0].split(",").length);
	}
	@Test
	public void testCsvGeneratePreview() throws IOException {
		String csvTestInputString = "\"contains, both delimiters\t inside of double quotes and is very long so it should be abbreviated\", second column, third column\n2nd row first column, 2nd row second column, 2nd row third column";
		InputStream from = IOUtils.toInputStream(csvTestInputString);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PreviewOutputMetadata type = csvPreviewGenerator.generatePreview(from, baos);
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
		InputStream from = IOUtils.toInputStream(sb.toString());
		PreviewOutputMetadata type = tabPreviewGenerator.generatePreview(from, baos);
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
		InputStream	from = IOUtils.toInputStream(sb.toString());
		PreviewOutputMetadata type = tabPreviewGenerator.generatePreview(from, baos);
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
		InputStream from = IOUtils.toInputStream(sb.toString());
		PreviewOutputMetadata type = tabPreviewGenerator.generatePreview(from, baos);
		assertEquals(TabCsvPreviewGenerator.TEXT_CSV_SEPARATED_VALUES, type.getContentType());
		String output = baos.toString();
		String[] lines = output.split("\n");
		//input csv has more than MAX_ROW_COUNT, so output should have been truncated to MAX_ROW_COUNT+1 rows
		assertEquals(TabCsvPreviewGenerator.MAX_ROW_COUNT+1, lines.length);
		//input csv has more than MAX_COLUMN_COUNT
		assertEquals(TabCsvPreviewGenerator.MAX_COLUMN_COUNT+1, lines[0].split(",").length);
	}

	@Test
	public void testVaryingColumnLengthGeneratePreview() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 5; i++) {
			sb.append("row 0 column " + i + "\t");
		}
		sb.append("\n");
		
		for (int i = 0; i < 10; i++) {
			sb.append("row 1 column " + i + "\t");
		}
		sb.append("\n");
		
		for (int i = 0; i < 2; i++) {
			sb.append("row 2 column " + i + "\t");
		}
		
		InputStream	from = IOUtils.toInputStream(sb.toString());
		PreviewOutputMetadata type = tabPreviewGenerator.generatePreview(from, baos);
		assertEquals(TabCsvPreviewGenerator.TEXT_CSV_SEPARATED_VALUES, type.getContentType());
		String output = baos.toString();
		String[] lines = output.split("\n");
		//input tsv has 3 lines
		assertEquals(3, lines.length);
		//input tsv has varying column lengths
		assertEquals(5, lines[0].split(",").length);
		assertEquals(10, lines[1].split(",").length);
		assertEquals(2, lines[2].split(",").length);
	}
	
	@Test
	public void testNewlinePreview() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		String s = "col 0, \"col\n1\", col 2";
		InputStream	from = IOUtils.toInputStream(s);
		csvPreviewGenerator.generatePreview(from, baos);
		String output = baos.toString();
		String[] lines = output.split("\n");
		//input csv has 1 lines
		assertEquals(1, lines.length);
		
		//input csv has 3 columns
		assertEquals(3, lines[0].split(",").length);
	}
	
	@Test
	public void testContentType() throws IOException {
		//tab preview generator supports tab
		assertTrue(tabPreviewGenerator.supportsContentType(TabCsvPreviewGenerator.TEXT_TAB_SEPARATED_VALUES));
		assertFalse(tabPreviewGenerator.supportsContentType(TabCsvPreviewGenerator.TEXT_CSV_SEPARATED_VALUES));
		//csv preview generator support csv
		assertTrue(csvPreviewGenerator.supportsContentType(TabCsvPreviewGenerator.TEXT_CSV_SEPARATED_VALUES));
		assertFalse(csvPreviewGenerator.supportsContentType(TabCsvPreviewGenerator.TEXT_TAB_SEPARATED_VALUES));
		
		assertFalse(tabPreviewGenerator.supportsContentType("text/xml"));
	}
}
