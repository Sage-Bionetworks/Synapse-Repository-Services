package org.sagebionetworks.bridge.model.dbo.dao;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import java.io.StringReader;

import org.junit.Test;

public class CsvNullReaderTest {

	@Test
	public void testReadNothing() throws Exception {
		CsvNullReader reader = new CsvNullReader(new StringReader(""));
		assertNull(reader.readNext());
		reader.close();
	}

	@Test
	public void testReadLines() throws Exception {
		CsvNullReader reader = new CsvNullReader(new StringReader("a,b\nd,e"));
		assertArrayEquals(new String[] { "a", "b" }, reader.readNext());
		assertArrayEquals(new String[] { "d", "e" }, reader.readNext());
		assertNull(reader.readNext());
		reader.close();
	}

	@Test
	public void testReadLinesWithEmptyAndNull() throws Exception {
		CsvNullReader reader = new CsvNullReader(new StringReader("##,##,##\n##,,##\n,##,\n,,"), ',', '#');
		assertArrayEquals(new String[] { "", "", "" }, reader.readNext());
		assertArrayEquals(new String[] { "", null, "" }, reader.readNext());
		assertArrayEquals(new String[] { null, "", null }, reader.readNext());
		assertArrayEquals(new String[] { null, null, null }, reader.readNext());
		assertNull(reader.readNext());
		reader.close();
	}

	@Test
	public void testReadEndNull() throws Exception {
		CsvNullReader reader = new CsvNullReader(new StringReader(","), ',', '#');
		assertArrayEquals(new String[] { null, null }, reader.readNext());
		assertNull(reader.readNext());
		reader.close();
	}

	@Test
	public void testReadLinesMix() throws Exception {
		CsvNullReader reader = new CsvNullReader(new StringReader("a,##,\n,a,##\n##,,a\n##,a,"), ',', '#');
		assertArrayEquals(new String[] { "a", "", null }, reader.readNext());
		assertArrayEquals(new String[] { null, "a", "" }, reader.readNext());
		assertArrayEquals(new String[] { "", null, "a" }, reader.readNext());
		assertArrayEquals(new String[] { "", "a", null }, reader.readNext());
		assertNull(reader.readNext());
		reader.close();
	}
}
