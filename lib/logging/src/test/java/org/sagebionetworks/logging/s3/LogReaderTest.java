package org.sagebionetworks.logging.s3;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
public class LogReaderTest {

	@Test
	public void testReading() throws IOException{
		String fileName = "Example1.log";
		InputStream in = LogReaderTest.class.getClassLoader().getResourceAsStream(fileName);
		assertNotNull("failed to find: "+fileName+" on the classpath",in);
		LogReader reader = new LogReader(new BufferedReader(new InputStreamReader(in)));
		List<LogEntry> entries = new LinkedList<LogEntry>();
		StringWriter writer = new StringWriter();
		try{
			// Read all Entries from the stream
			LogEntry entry = null;
			do{
				entry = reader.read();
				if(entry != null){
					entries.add(entry);
					writer.append(entry.getEntryString());
					writer.append("\n");
				}
			}while(entry != null);
			
		}finally{
			reader.close();
		}
		System.out.println(writer.toString());
		assertEquals("The sample log should contain 8 entries.",8, entries.size());
		// The first entry should be a stack trace
		assertTrue(entries.get(0).getEntryString().indexOf("java.lang.RuntimeException: java.lang.IllegalArgumentException: Bad mojo!") > 0);
		assertTrue(entries.get(1).getEntryString().indexOf("7931fe99-d376-4937-8366-ebeaab73a170 [main] DEBUG org.sagebionetworks.logging.LoggingTest - This is a debug message") > 0);
		assertTrue(entries.get(7).getEntryString().indexOf("[1] ELAPSE: 00:05:001 METHOD: this is a debug frame") > 0);
	}
}
