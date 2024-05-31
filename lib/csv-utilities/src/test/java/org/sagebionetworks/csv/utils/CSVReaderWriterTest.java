package org.sagebionetworks.csv.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import au.com.bytecode.opencsv.Constants;

public class CSVReaderWriterTest {

	@Test
	public void testReadNextEmptyString() throws Exception {
		CSVReader reader = new CSVReader(new StringReader(""));
		assertNull(reader.readNext());
		reader.close();
	}

	@Test
	public void testReadNextTwoLines() throws Exception {
		CSVReader reader = new CSVReader(new StringReader("a,b\nd,e"));
		assertArrayEquals(new String[] { "a", "b" }, reader.readNext());
		assertArrayEquals(new String[] { "d", "e" }, reader.readNext());
		assertNull(reader.readNext());
		reader.close();
	}

	@Test
	public void testReadNextWithNullValues() throws Exception {
		CSVReader reader = new CSVReader(new StringReader("##,##,##\n##,,##\n,##,\n,,"), ',', '#');
		assertArrayEquals(new String[] { "", "", "" }, reader.readNext());
		assertArrayEquals(new String[] { "", null, "" }, reader.readNext());
		assertArrayEquals(new String[] { null, "", null }, reader.readNext());
		assertArrayEquals(new String[] { null, null, null }, reader.readNext());
		assertNull(reader.readNext());
		reader.close();
	}

	@Test
	public void testReadNextWithEndingNull() throws Exception {
		CSVReader reader = new CSVReader(new StringReader(","), ',', '#');
		assertArrayEquals(new String[] { null, null }, reader.readNext());
		assertNull(reader.readNext());
		reader.close();
	}

	@Test
	public void testReadNext() throws Exception {
		CSVReader reader = new CSVReader(new StringReader("a,##,\n,a,##\n##,,a\n##,a,"), ',', '#');
		assertArrayEquals(new String[] { "a", "", null }, reader.readNext());
		assertArrayEquals(new String[] { null, "a", "" }, reader.readNext());
		assertArrayEquals(new String[] { "", null, "a" }, reader.readNext());
		assertArrayEquals(new String[] { "", "a", null }, reader.readNext());
		assertNull(reader.readNext());
		reader.close();
	}

	@Test
	public void testReadNextWithQuote() throws IOException {
		char escapse = '\\';
		char quote = '\"';
		String toTest = "\"has "+escapse+quote+"quote"+escapse+quote+"\"";
		CSVReader reader = new CSVReader(new StringReader(toTest));
		String[] back = reader.readNext();
		String expected = "has \"quote\"";
		reader.close();
		assertEquals(expected, back[0]);
	}

	@Test
	public void testReadAllWithEmptyString() throws IOException {
		String toTest = "";
		CSVReader reader = new CSVReader(new StringReader(toTest));
		List<String[]> result = reader.readAll();
		reader.close();
		assertTrue(result.isEmpty());
	}

	@Test
	public void testReadAllWithOnlyNewLine() throws IOException {
		String toTest = "\n";
		CSVReader reader = new CSVReader(new StringReader(toTest));
		List<String[]> result = reader.readAll();
		reader.close();
		assertTrue(result.isEmpty());
	}

	@Test
	public void testReadAllWithDoubleNewLine() throws IOException {
		String toTest = "\n\n";
		CSVReader reader = new CSVReader(new StringReader(toTest));
		List<String[]> result = reader.readAll();
		reader.close();
		assertTrue(result.isEmpty());
	}

	@Test
	public void testReadAllWithOneLine() throws IOException {
		String toTest = "a,b";
		CSVReader reader = new CSVReader(new StringReader(toTest));
		List<String[]> result = reader.readAll();
		reader.close();
		assertEquals(1, result.size());
		assertEquals(2, result.get(0).length);
		assertEquals("a", result.get(0)[0]);
		assertEquals("b", result.get(0)[1]);
	}

	@Test
	public void testReadAllWithNewLine() throws IOException {
		String toTest = "a,b\n";
		CSVReader reader = new CSVReader(new StringReader(toTest));
		List<String[]> result = reader.readAll();
		reader.close();
		assertEquals(1, result.size());
		assertEquals(2, result.get(0).length);
		assertEquals("a", result.get(0)[0]);
		assertEquals("b", result.get(0)[1]);
	}

	@Test
	public void testReadAllWithDoubleNewLineEnding() throws IOException {
		String toTest = "a,b\n\n";
		CSVReader reader = new CSVReader(new StringReader(toTest));
		List<String[]> result = reader.readAll();
		reader.close();
		assertEquals(1, result.size());
		assertEquals(2, result.get(0).length);
		assertEquals("a", result.get(0)[0]);
		assertEquals("b", result.get(0)[1]);
	}

	@Test
	public void testReadAllWithTripleNewLineEnding() throws IOException {
		String toTest = "a,b\n\n\n";
		CSVReader reader = new CSVReader(new StringReader(toTest));
		List<String[]> result = reader.readAll();
		reader.close();
		assertEquals(1, result.size());
		assertEquals(2, result.get(0).length);
		assertEquals("a", result.get(0)[0]);
		assertEquals("b", result.get(0)[1]);
	}

	@Test
	public void testReadAllWithQuadrupleNewLineEnding() throws IOException {
		String toTest = "a,b\n\n\n\n";
		CSVReader reader = new CSVReader(new StringReader(toTest));
		List<String[]> result = reader.readAll();
		reader.close();
		assertEquals(1, result.size());
		assertEquals(2, result.get(0).length);
		assertEquals("a", result.get(0)[0]);
		assertEquals("b", result.get(0)[1]);
	}

	@Test
	public void testReadAllWithNewLineInBetween() throws IOException {
		String toTest = "a,b\nc,d";
		CSVReader reader = new CSVReader(new StringReader(toTest));
		List<String[]> result = reader.readAll();
		reader.close();
		assertEquals(2, result.size());
		assertEquals(2, result.get(0).length);
		assertEquals("a", result.get(0)[0]);
		assertEquals("b", result.get(0)[1]);
		assertEquals(2, result.get(1).length);
		assertEquals("c", result.get(1)[0]);
		assertEquals("d", result.get(1)[1]);
	}

	@Test
	public void testReadAllWithDoubleNewLineInBetween() throws IOException {
		String toTest = "a,b\n\nc,d";
		CSVReader reader = new CSVReader(new StringReader(toTest));
		List<String[]> result = reader.readAll();
		reader.close();
		assertEquals(2, result.size());
		assertEquals(2, result.get(0).length);
		assertEquals("a", result.get(0)[0]);
		assertEquals("b", result.get(0)[1]);
		assertEquals(2, result.get(1).length);
		assertEquals("c", result.get(1)[0]);
		assertEquals("d", result.get(1)[1]);
	}

	@Test
	public void testReadAllWithTripleNewLineInBetween() throws IOException {
		String toTest = "a,b\n\n\nc,d";
		CSVReader reader = new CSVReader(new StringReader(toTest));
		List<String[]> result = reader.readAll();
		reader.close();
		assertEquals(2, result.size());
		assertEquals(2, result.get(0).length);
		assertEquals("a", result.get(0)[0]);
		assertEquals("b", result.get(0)[1]);
		assertEquals(2, result.get(1).length);
		assertEquals("c", result.get(1)[0]);
		assertEquals("d", result.get(1)[1]);
	}

	@Test
	public void testWriteNextWithNull() throws IOException {
		StringWriter sw = new StringWriter();
		CSVWriter writer = new CSVWriter(sw);
		writer.writeNext(null);
		assertEquals("", sw.toString());
		sw.close();
		writer.close();
	}

	@Test
	public void testWriteNextWithEmpty() throws IOException {
		StringWriter sw = new StringWriter();
		CSVWriter writer = new CSVWriter(sw);
		writer.writeNext(new String[]{});
		assertEquals("\n", sw.toString());
		sw.close();
		writer.close();
	}

	@Test
	public void testWriteNextWithNullValues() throws IOException {
		StringWriter sw = new StringWriter();
		CSVWriter writer = new CSVWriter(sw);
		writer.writeNext(new String[]{null});
		assertEquals("\n", sw.toString());
		sw.close();
		writer.close();
	}

	@Test
	public void testWriteNextWithEmptyValues() throws IOException {
		StringWriter sw = new StringWriter();
		CSVWriter writer = new CSVWriter(sw);
		writer.writeNext(new String[]{null,});
		assertEquals("\n", sw.toString());
		sw.close();
		writer.close();
	}

	@Test
	public void testWriteNextWithMixValues() throws IOException {
		StringWriter sw = new StringWriter();
		CSVWriter writer = new CSVWriter(sw);
		writer.writeNext(new String[]{null, null,"a", null});
		assertEquals(",,\"a\",\n", sw.toString());
		sw.close();
		writer.close();
	}

	@Test
	public void testWriteNextWithQuote() throws IOException {
		StringWriter sw = new StringWriter();
		CSVWriter writer = new CSVWriter(sw);
		writer.writeNext(new String[]{"has \"quote\""});
		assertEquals("\"has \"\"quote\"\"\"\n", sw.toString());
		sw.close();
		writer.close();
	}

	@Test
	public void testWriteAllWithEmptyList() throws IOException {
		StringWriter sw = new StringWriter();
		CSVWriter writer = new CSVWriter(sw);
		List<String[]> list = new ArrayList<String[]>(0);
		writer.writeAll(list);
		assertEquals("", sw.toString());
		sw.close();
		writer.close();
	}

	@Test
	public void testWriteAllWithEmptyLine() throws IOException {
		StringWriter sw = new StringWriter();
		CSVWriter writer = new CSVWriter(sw);
		List<String[]> list = new ArrayList<String[]>(1);
		list.add(new String[]{});
		writer.writeAll(list);
		assertEquals("\n", sw.toString());
		sw.close();
		writer.close();
	}

	@Test
	public void testWriteAllWithDoubleEmptyLine() throws IOException {
		StringWriter sw = new StringWriter();
		CSVWriter writer = new CSVWriter(sw);
		List<String[]> list = new ArrayList<String[]>(1);
		list.add(new String[]{});
		list.add(new String[]{});
		writer.writeAll(list);
		assertEquals("\n\n", sw.toString());
		sw.close();
		writer.close();
	}

	@Test
	public void testWriteAllWithTripleEmptyLine() throws IOException {
		StringWriter sw = new StringWriter();
		CSVWriter writer = new CSVWriter(sw);
		List<String[]> list = new ArrayList<String[]>(1);
		list.add(new String[]{});
		list.add(new String[]{});
		list.add(new String[]{});
		writer.writeAll(list);
		assertEquals("\n\n\n", sw.toString());
		sw.close();
		writer.close();
	}

	@Test
	public void testWriteAllWithMixedData() throws IOException {
		StringWriter sw = new StringWriter();
		CSVWriter writer = new CSVWriter(sw);
		List<String[]> list = new ArrayList<String[]>(1);
		list.add(new String[]{null,"a","b"});
		list.add(new String[]{"c",null,"d"});
		list.add(new String[]{});
		list.add(new String[]{});
		list.add(new String[]{"e","f",null});
		writer.writeAll(list);
		assertEquals(",\"a\",\"b\"\n\"c\",,\"d\"\n\n\n\"e\",\"f\",\n", sw.toString());
		sw.close();
		writer.close();
	}

	@Test
	public void testWriteNextReadNext() throws IOException {
		StringWriter sw = new StringWriter();
		CSVWriter writer = new CSVWriter(sw);
		String toWrite = "has \"quote\"";
		writer.writeNext(new String[]{toWrite});
		String toRead = sw.toString();
		sw.close();
		writer.close();
		CSVReader reader = new CSVReader(new StringReader(toRead));
		String[] back = reader.readNext();
		reader.close();
		assertEquals(toWrite, back[0]);
	}

	@Test
	public void testWriteAllReadAll() throws IOException {
		StringWriter sw = new StringWriter();
		CSVWriter writer = new CSVWriter(sw);
		List<String[]> list = new ArrayList<String[]>(1);
		list.add(new String[]{null,"a","b"});
		list.add(new String[]{"c",null,"d"});
		list.add(new String[]{});
		list.add(new String[]{});
		list.add(new String[]{"e","f",null});
		writer.writeAll(list);
		String toRead = sw.toString(); // ",\"a\",\"b\"\n\"c\",,\"d\"\n\n\n\"e\",\"f\",\n"
		sw.close();
		writer.close();
		CSVReader reader = new CSVReader(new StringReader(toRead));
		List<String[]> back = reader.readAll();
		reader.close();
		assertEquals(3, back.size());
		assertArrayEquals(list.get(0), back.get(0));
		assertArrayEquals(list.get(1), back.get(1));
		assertArrayEquals(list.get(4), back.get(2));
	}

	@Test
	public void testOldData() throws IOException {
		String oldData = "\"1462840117000\",\"userprofile\",\"{\"\"position\"\":\"\"\"\",\"\"lastName\"\":\"\"Geissler\"\",\"\"etag\"\":\"\"3a0bc974-344e-4efd-85a6-c945ecafcdd5\"\",\"\"location\"\":\"\"Tacoma, WA\"\",\"\"ownerId\"\":\"\"3340403\"\",\"\"emails\"\":[\"\"axolotl2@uw.edu\"\"],\"\"url\"\":\"\"\"\",\"\"openIds\"\":[],\"\"notificationSettings\"\":{\"\"sendEmailNotifications\"\":true},\"\"company\"\":\"\"University of Washington\"\",\"\"userName\"\":\"\"egeissler\"\",\"\"displayName\"\":\"\"Emma Geissler\"\",\"\"industry\"\":\"\"\"\",\"\"firstName\"\":\"\"Emma\"\"}\"";
		CSVReader reader = new CSVReader(new StringReader(oldData));
		List<String[]> toWrite = reader.readAll();
		reader.close();
		StringWriter sw = new StringWriter();
		CSVWriter writer = new CSVWriter(sw);
		writer.writeAll(toWrite);
		String toRead = sw.toString();
		sw.close();
		writer.close();
		reader = new CSVReader(new StringReader(toRead));
		List<String[]> back = reader.readAll();
		reader.close();
		sw = new StringWriter();
		writer = new CSVWriter(sw);
		writer.writeAll(back);
		String second = sw.toString();
		sw.close();
		writer.close();
		assertEquals(toRead, second);
	}

	@Test
	public void testNewData() throws IOException {
		String newData = "\"1446751166000\",\"userprofile\",\"{\\\"openIds\\\":[],\\\"lastName\\\":\\\"Admin\\\",\\\"notificationSettings\\\":{\\\"sendEmailNotifications\\\":true},\\\"etag\\\":\\\"44a8adc7-75fc-492c-9c7f-d6a487097420\\\",\\\"ownerId\\\":\\\"1\\\",\\\"userName\\\":\\\"migrationAdmin\\\",\\\"emails\\\":[\\\"migrationAdmin@sagebase.org\\\"],\\\"firstName\\\":\\\"Migration\\\"}\"";
		CSVReader reader = new CSVReader(new StringReader(newData));
		List<String[]> toWrite = reader.readAll();
		reader.close();
		StringWriter sw = new StringWriter();
		CSVWriter writer = new CSVWriter(sw);
		writer.writeAll(toWrite);
		String toRead = sw.toString();
		sw.close();
		writer.close();
		reader = new CSVReader(new StringReader(toRead));
		List<String[]> back = reader.readAll();
		reader.close();
		sw = new StringWriter();
		writer = new CSVWriter(sw);
		writer.writeAll(back);
		String second = sw.toString();
		sw.close();
		writer.close();
		assertEquals(toRead, second);
	}
	
    /**
     * See PLFM-5989
     * @throws IOException 
     */
    @Test
    public void testReadUTF_8BOMIgnore() throws IOException {
        StringBuilder sb = new StringBuilder(CSVParser.INITIAL_READ_SIZE);
        // Start the string with the UTF-8 byte order marker
        sb.append(Constants.UTF_8_BYTE_ORDER_MARKER);
        sb.append("a,b");

        CSVReader c = new CSVReader(new StringReader(sb.toString()));

        String[] nextLine = c.readNext();
        // the parser should ignore the marker
        assertEquals("a", nextLine[0]);
        assertEquals("b", nextLine[1]);
        c.close();
    }
    
    /**
     * See PLFM-5989
     * @throws IOException 
     */
    @Test
    public void testReadUTF_8BOMIgnoreQuotes() throws IOException {
        StringBuilder sb = new StringBuilder(CSVParser.INITIAL_READ_SIZE);
        // Start the string with the UTF-8 byte order marker
        sb.append(Constants.UTF_8_BYTE_ORDER_MARKER);
        sb.append("\"a\",\"b\"");

        CSVReader c = new CSVReader(new StringReader(sb.toString()));

        String[] nextLine = c.readNext();
        // the parser should ignore the marker
        assertEquals("a", nextLine[0]);
        assertEquals("b", nextLine[1]);
        c.close();
    }
    
    /**
     * See PLFM-5989
     * @throws IOException 
     */
    @Test
    public void testReadUTF_8BOMIgnoreOnly() throws IOException {
        StringBuilder sb = new StringBuilder(CSVParser.INITIAL_READ_SIZE);
        // Start the string with the UTF-8 byte order marker
        sb.append(Constants.UTF_8_BYTE_ORDER_MARKER);

        CSVReader c = new CSVReader(new StringReader(sb.toString()));

        String[] nextLine = c.readNext();
        // the parser should ignore the marker
        assertEquals(null, nextLine[0]);
        c.close();
    }
}
