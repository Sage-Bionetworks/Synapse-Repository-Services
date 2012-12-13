package org.sagebionetworks.logging.collate;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sagebionetworks.logging.reader.LogReader;
import org.sagebionetworks.logging.reader.LogReader.LogReaderFactory;

public class LogCollationUtilsTest {

	private List<File> files;

	@Before
	public void setUp() throws IOException {
		List<String> fileNames = Arrays.asList("test1", "test2", "test3");

		files = new ArrayList<File>();
		for (String fileName : fileNames) {
			File e = new File(fileName);
			e.createNewFile();
			files.add(e);
		}
	}

	@After
	public void tearDown() {
		for (File file : files) {
			file.delete();
		}
	}

	@Test
	public void testInitializeReader() throws IOException {
		LogReaderFactory<LogReader> mockFactory = mock(LogReaderFactory.class, RETURNS_DEEP_STUBS);

		List<LogReader> logReaders = LogCollationUtils.initializeReaders(mockFactory, files);
		ArgumentCaptor<BufferedReader> captor = ArgumentCaptor.forClass(BufferedReader.class);
		verify(mockFactory, times(files.size())).create(captor.capture());
		List<BufferedReader> readers = captor.getAllValues();

		assertEquals(files.size(), readers.size());
		assertEquals(readers.size(), logReaders.size());
	}

	@Test(expected=FileNotFoundException.class)
	public void testInitializeReaderFakeFiles() throws Exception {
		files.add(new File(""));
		LogReaderFactory<LogReader> mockFactory = mock(LogReaderFactory.class, RETURNS_DEEP_STUBS);

		List<LogReader> logReaders = LogCollationUtils.initializeReaders(mockFactory, files);
	}
}
