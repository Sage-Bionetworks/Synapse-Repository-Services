package org.sagebionetworks.util.csv;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;

import org.junit.jupiter.api.Test;

import au.com.bytecode.opencsv.CSVReader;

public class CSVReaderIteratorTest {

	CSVReader reader;

	@Test
	public void testIterator() {
		CSVReaderIterator iterator = new CSVReaderIterator(new CSVReader(new StringReader("foo,bar\n1,2")));
		assertTrue(iterator.hasNext());
		assertArrayEquals(new String[] { "foo", "bar" }, iterator.next());
		assertTrue(iterator.hasNext());
		assertArrayEquals(new String[] { "1", "2" }, iterator.next());
		assertFalse(iterator.hasNext());
	}

}
