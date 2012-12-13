package org.sagebionetworks.logging.collate;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.sagebionetworks.logging.collate.LogCollationUtils.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sagebionetworks.logging.reader.ActivityLogReader;
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
		@SuppressWarnings("unchecked")
		LogReaderFactory<LogReader> mockFactory = mock(LogReaderFactory.class, RETURNS_DEEP_STUBS);

		List<LogReader> logReaders = LogCollationUtils.initializeReaders(mockFactory, files);
		ArgumentCaptor<BufferedReader> captor = ArgumentCaptor.forClass(BufferedReader.class);
		verify(mockFactory, times(files.size())).create(captor.capture());
		List<BufferedReader> readers = captor.getAllValues();

		assertEquals(files.size(), readers.size());
		assertEquals(readers.size(), logReaders.size());
	}

	@Test
	public void testCollate() throws Exception {
		File dir = new File("src/test/resources");
		if (!dir.exists() || !dir.isDirectory())
			fail("Missing necessary test resource directory.");

		List<File> asList = Arrays.asList(dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				String shouldMatch = "repo-activity.2012-09-16-10-30.log";
				return name.equals(shouldMatch);
			}
		}));

		File tempFile = File.createTempFile("logOut", ".collated", dir);
		tempFile.deleteOnExit();
		collateLogs(primeCollationMap(initializeReaders(new ActivityLogReader.ActivityLogReaderFactory(),
														asList)),
					new BufferedWriter(new FileWriter(tempFile)));
	}

	@Test(expected=FileNotFoundException.class)
	public void testInitializeReaderFakeFiles() throws Exception {
		files.add(new File(""));
		LogReaderFactory<LogReader> mockFactory = mock(LogReaderFactory.class, RETURNS_DEEP_STUBS);

		LogCollationUtils.initializeReaders(mockFactory, files);
	}
}
