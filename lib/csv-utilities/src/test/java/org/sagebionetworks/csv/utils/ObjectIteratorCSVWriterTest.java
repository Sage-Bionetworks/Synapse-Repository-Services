package org.sagebionetworks.csv.utils;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ObjectIteratorCSVWriterTest {

	Iterator<ExampleObject> mockIterator;
	List<ExampleObject> list;
	String fileName = "testFile";
	String[] headers = new String[]{"aString", "aLong", "aBoolean", "aDouble", "anInteger", "aFloat", "someEnum"};

	@BeforeEach
	public void before() {
		mockIterator = Mockito.mock(Iterator.class);
		list = ExampleObject.buildExampleObjectList(5);
	}

	@Test
	public void test() throws IOException {
		Mockito.when(mockIterator.hasNext()).thenReturn(true, true, true, true, true, false);
		Mockito.when(mockIterator.next()).thenReturn(list.get(0), list.get(1), list.get(2), list.get(3), list.get(4), null);
		File file = null;
		ObjectCSVReader<ExampleObject> reader = null;
		try {
			file = File.createTempFile(fileName, ".csv");
			FileOutputStream fos = new FileOutputStream(file);
			ObjectIteratorCSVWriter.write(mockIterator, fos, headers, ExampleObject.class);
			reader = new ObjectCSVReader<ExampleObject>(new FileReader(file), ExampleObject.class, headers);
			List<ExampleObject> actual = new ArrayList<ExampleObject>();
			ExampleObject record = null;
			while ((record = reader.next()) != null) {
				actual.add(record);
			}
			assertEquals(new HashSet<ExampleObject>(list), new HashSet<ExampleObject>(actual));
		} finally {
			if (reader != null) reader.close();
			if (file != null) file.delete();
		}
	}

}
