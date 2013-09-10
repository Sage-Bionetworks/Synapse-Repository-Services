package org.sagebionetworks.log.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.logging.s3.LogDAO;
import org.sagebionetworks.logging.s3.LogEntry;
import org.sagebionetworks.logging.s3.LogReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class LogCollateIntegrationTest {
	
	public static final int MAX_WAIT_MS = 1000*60*2;
	
	Logger log = Logger.getLogger(LogCollateIntegrationTest.class);
	
	@Autowired
	LogDAO logDAO;
	
	@After
	public void after(){
		// Delete all log data created by this test.
		logDAO.deleteAllStackInstanceLogs();
	}
	
	@Test
	public void integrationTest() throws IOException, InterruptedException{
		// Start with no logs in S3
		logDAO.deleteAllStackInstanceLogs();
		
		long now = 1;
		String type = "logCollateIntegrationTest".toLowerCase();
		String tempDir = System.getProperty("java.io.tmpdir");
		// Create three simple log files
		File[] localFiles = new File[3];
		try {
			// one
			localFiles[0] = LogTestUtils.createSampleLogFile(new File(tempDir).getAbsolutePath(), type, new String[]{"one.1", "one.2", "one.3"}, now);
			logDAO.saveLogFile(localFiles[0], now);
			// two
			localFiles[1] = LogTestUtils.createSampleLogFile(new File(tempDir).getAbsolutePath(), type, new String[]{"two.2", "two.3"}, now+1);
			logDAO.saveLogFile(localFiles[1], now+1);
			// three
			localFiles[2] = LogTestUtils.createSampleLogFile(new File(tempDir).getAbsolutePath(), type, new String[]{"three.3", "three.4"}, now+2);
			logDAO.saveLogFile(localFiles[2], now+2);
		} finally {
			// Delete the local files.
			for(int i=0; i<localFiles.length; i++){
				if(localFiles[i] != null){
					localFiles[i].delete();
				}
			}
		}
		// Now we want to wait for the collator to merge all three files into one.
		String singleKey = null;
		long start = System.currentTimeMillis();
		do{
			ObjectListing listing = logDAO.listAllStackInstanceLogs(null);
			// Find all the keys for this test
			List<String> allKeys = new LinkedList<String>();
			for(S3ObjectSummary sum: listing.getObjectSummaries()){
				if(sum.getKey().contains(type)){
					allKeys.add(sum.getKey());
				}
			}
			if(allKeys.size() == 1){
				singleKey = allKeys.get(0);
			}
			if(singleKey == null){
				log.debug("Waiting for logs to be collated.  There are currently : "+allKeys.size()+" log files...");
				Thread.sleep(1000);
				long elpase = System.currentTimeMillis()-start;
				assertTrue("Timed out waiting for the LogCollateWorker to collate all of the logs into a single file.",elpase < MAX_WAIT_MS);
			}
		}while(singleKey == null);
		
		// Now get the reader for the single log
		LogReader reader = logDAO.getLogFileReader(singleKey);
		assertNotNull(reader);
		List<LogEntry> allEntries = LogTestUtils.getAllLogEntiresFromReader(reader);
		assertNotNull(allEntries);
		assertEquals(7, allEntries.size());
		// Validate the single log files is collated as expected.
		assertTrue(allEntries.get(0).getEntryString().contains("one.1"));
		assertTrue(allEntries.get(1).getEntryString().contains("one.2"));
		assertTrue(allEntries.get(2).getEntryString().contains("two.2"));
		assertTrue(allEntries.get(3).getEntryString().contains("one.3"));
		assertTrue(allEntries.get(4).getEntryString().contains("two.3"));
		assertTrue(allEntries.get(5).getEntryString().contains("three.3"));
		assertTrue(allEntries.get(6).getEntryString().contains("three.4"));
	}

}
