package org.sagebionetworks.log.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.logging.s3.LogEntry;
import org.sagebionetworks.logging.s3.LogReader;

/**
 * Test for CollateUtils
 * 
 * @author John
 *
 */
public class CollateUtilsTest {
	
	ProgressCallback mockCallback;
	
	@Before
	public void before(){
		mockCallback = Mockito.mock(ProgressCallback.class);
	}
	
	@Test
	public void testCollateNoOverLap() throws IOException{
		// For this simple test collate two logs with no overlap
		LogReader one = LogTestUtils.createTestLogReader(new String[]{"one", "two","three"}, 0);
		LogReader two = LogTestUtils.createTestLogReader(new String[]{"four", "five","six"}, 1000);
		LogReader[] toCollate = new LogReader[]{two, one};
		StringWriter strWriter = new StringWriter();
		BufferedWriter buffered = new BufferedWriter(strWriter);
		CollateUtils.collateLogs(toCollate, buffered, mockCallback);
		buffered.flush();
		// Validate the results;
		List<LogEntry> results = LogTestUtils.readLogEntries(strWriter.toString());
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
		LogReader one = LogTestUtils.createTestLogReader(new String[]{"one.1", "two.1","three.1"}, 0);
		LogReader two = LogTestUtils.createTestLogReader(new String[]{"one.2", "two.2","three.2"}, 1);
		LogReader[] toCollate = new LogReader[]{two, one};
		StringWriter strWriter = new StringWriter();
		BufferedWriter buffered = new BufferedWriter(strWriter);
		CollateUtils.collateLogs(toCollate, buffered, mockCallback);
		buffered.flush();
		// Validate the results;
		List<LogEntry> results = LogTestUtils.readLogEntries(strWriter.toString());
		assertNotNull(results);
		assertEquals(6, results.size());
		assertTrue(results.get(0).getEntryString().contains("one.1"));
		assertTrue(results.get(1).getEntryString().contains("one.2"));
		assertTrue(results.get(2).getEntryString().contains("two.1"));
		assertTrue(results.get(3).getEntryString().contains("two.2"));
		assertTrue(results.get(4).getEntryString().contains("three.1"));
		assertTrue(results.get(5).getEntryString().contains("three.2"));
	}
	
	@Test
	public void testUneveneOverLap() throws IOException{
		// For this simple test collate two logs with no overlap
		LogReader one = LogTestUtils.createTestLogReader(new String[]{"one.0", "two.1","three.2"}, 0);
		LogReader two = LogTestUtils.createTestLogReader(new String[]{"one.1"}, 1);
		LogReader three = LogTestUtils.createTestLogReader(new String[]{"one.2", "two.3","three.4", "four.5"}, 2);
		LogReader[] toCollate = new LogReader[]{two, one, three};
		StringWriter strWriter = new StringWriter();
		BufferedWriter buffered = new BufferedWriter(strWriter);
		CollateUtils.collateLogs(toCollate, buffered, mockCallback);
		buffered.flush();
		// Validate the results;
		List<LogEntry> results = LogTestUtils.readLogEntries(strWriter.toString());
		assertNotNull(results);
		assertEquals(8, results.size());
		assertTrue(results.get(0).getEntryString().contains("one.0"));
		assertTrue(results.get(2).getEntryString().contains("two.1"));
		assertTrue(results.get(7).getEntryString().contains("four.5"));
	}
	
}
