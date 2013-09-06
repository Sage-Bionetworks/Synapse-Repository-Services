package org.sagebionetworks.log.worker;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.logging.s3.LogEntry;
import org.sagebionetworks.logging.s3.LogKeyUtils;
import org.sagebionetworks.logging.s3.LogReader;

/**
 * Test for CollateUtils
 * 
 * @author John
 *
 */
public class CollateUtilsTest {
	
	@Test
	public void testCollateNoOverLap() throws IOException{
		// For this simple test collate two logs with no overlap
		LogReader one = createTestLogReader(new String[]{"one", "two","three"}, 0);
		LogReader two = createTestLogReader(new String[]{"four", "five","six"}, 1000);
		LogReader[] toCollate = new LogReader[]{two, one};
		StringWriter strWriter = new StringWriter();
		BufferedWriter buffered = new BufferedWriter(strWriter);
		CollateUtils.collateLogs(toCollate, buffered);
		buffered.flush();
		System.out.println(strWriter.toString());
		// Validate the results;
		List<LogEntry> results = readLogEntries(strWriter.toString());
		assertNotNull(results);
		assertEquals(6, results.size());
		assertTrue(results.get(0).getEntryString().contains("one"));
		assertTrue(results.get(1).getEntryString().contains("two"));
		assertTrue(results.get(2).getEntryString().contains("three"));
		assertTrue(results.get(3).getEntryString().contains("four"));
		assertTrue(results.get(4).getEntryString().contains("five"));
		assertTrue(results.get(5).getEntryString().contains("six"));
	}
	
	@Test
	public void testCollateOverLap() throws IOException{
		// For this simple test collate two logs with no overlap
		LogReader one = createTestLogReader(new String[]{"one.1", "two.1","three.1"}, 0);
		LogReader two = createTestLogReader(new String[]{"one.2", "two.2","three.2"}, 1);
		LogReader[] toCollate = new LogReader[]{two, one};
		StringWriter strWriter = new StringWriter();
		BufferedWriter buffered = new BufferedWriter(strWriter);
		CollateUtils.collateLogs(toCollate, buffered);
		buffered.flush();
		System.out.println(strWriter.toString());
		// Validate the results;
		List<LogEntry> results = readLogEntries(strWriter.toString());
		assertNotNull(results);
		assertEquals(6, results.size());
		assertTrue(results.get(0).getEntryString().contains("one.1"));
		assertTrue(results.get(1).getEntryString().contains("one.2"));
		assertTrue(results.get(2).getEntryString().contains("two.1"));
		assertTrue(results.get(3).getEntryString().contains("two.2"));
		assertTrue(results.get(4).getEntryString().contains("three.1"));
		assertTrue(results.get(5).getEntryString().contains("three.2"));
	}
	
	/**
	 * Extract all of the {@link LogEntry} from the passed string.
	 * @param raw
	 * @return
	 * @throws IOException
	 */
	public static List<LogEntry> readLogEntries(String raw) throws IOException{
		LogReader results = new LogReader(new BufferedReader(new StringReader(raw)));
		List<LogEntry> list= new LinkedList<LogEntry>();
		LogEntry entry = null;
		do{
			entry = results.read();
			if(entry != null){
				list.add(entry);
			}
		}while(entry != null);
		return list;
	}

	/**
	 * Helper to create a log reader for some sample log data
	 * @param data
	 * @param timestamp
	 * @return
	 */
	public static LogReader createTestLogReader(String[] data, long timestamp){
		// Add each value to the log
		StringBuilder builder = new StringBuilder();
		for(int i=0; i<data.length; i++){
			builder.append(LogKeyUtils.createISO8601GMTLogString(timestamp+i));
			builder.append(" ");
			builder.append(data[i]);
			builder.append("\n");
		}
		BufferedReader reader = new BufferedReader(new StringReader(builder.toString()));
		return new LogReader(reader);
	}
}
