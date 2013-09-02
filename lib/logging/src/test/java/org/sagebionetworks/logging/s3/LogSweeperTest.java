package org.sagebionetworks.logging.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * This is a an integration test for the log sweeper.
 * 
 * @author John
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:log-sweeper.spb.xml" })
public class LogSweeperTest {

	@Autowired
	LogSweeperFactory logSweeperFactory;
	@Autowired
	private LogDAO logDAO;
	@Autowired
	StackConfiguration config;
	
	@Before
	public void before(){
		// Make sure all logs are cleared before we start
		logDAO.deleteAllStackInstanceLogs();
	}
	
	@Test
	public void testSweep() throws IOException{
		// Create a sample log in the logging directory
		String[] entries = new String[]{"entry one", "this is the second entry"};
		String type = "logsweeptest";
		File sampleLog = LogWriterUtil.createSampleLogFile(config.getLocalLoggingDirectory(), type, entries);
		try{
			// Sweeping the logs should catch our new log
			List<String> keys = logSweeperFactory.sweepLogs();
			System.out.println(keys);
			assertNotNull(keys);
			assertTrue(keys.size() > 0);
			// find the key for this test
			String testKey = null;
			for(String key: keys){
				if(key.contains(type)){
					testKey = key;
					break;
				}
			}
			assertNotNull("failed to find the test key from the sweept logs",testKey);
			// Now make sure we can read the log
			LogReader reader = logDAO.getLogFileReader(testKey);
			List<LogEntry> returned = LogWriterUtil.streamEntiresForKey(reader);
			assertNotNull(entries);
			assertEquals("There should be two entries for this file", 2, returned.size());
			// one
			LogEntry entry = returned.get(0);
			String entryString = entry.getEntryString();
			assertNotNull(entryString);
			System.out.println(entryString);
			assertTrue(entryString.endsWith(entries[0]));
			// two
			entry = returned.get(1);
			entryString = entry.getEntryString();
			assertNotNull(entryString);
			System.out.println(entryString);
			assertTrue(entryString.endsWith(entries[1]));
		}finally{
			sampleLog.delete();
		}

	}
}
