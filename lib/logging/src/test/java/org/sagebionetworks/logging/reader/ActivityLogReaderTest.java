package org.sagebionetworks.logging.reader;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;
import org.sagebionetworks.logging.collate.LogEvent;
import org.sagebionetworks.logging.reader.ActivityLogReader.ActivityLogReaderFactory;

public class ActivityLogReaderTest {

	@Test
	public void readLogEvent() throws IOException {
		String pathname = "src/test/resources/ActivityLogReaderTestLogFile.log";
		File logFile = new File(pathname);
		if (!logFile.exists())
			fail("Necessary resource: "+pathname+", is mising.");

		ActivityLogReaderFactory factory = new ActivityLogReader.ActivityLogReaderFactory();
		ActivityLogReader logReader = factory.create(new BufferedReader(new FileReader(logFile)));
		
		LogEvent event;
		LogEvent previous = null;
		while((event = logReader.readLogEvent()) != null) {
			if (previous != null) {
				assertTrue("The previous event should be \"less than\" the current.",
						0 > previous.compareTo(event));
			}
			previous = event;
		}
	}

}
