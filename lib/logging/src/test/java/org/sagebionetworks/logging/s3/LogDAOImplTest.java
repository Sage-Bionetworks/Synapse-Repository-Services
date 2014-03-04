package org.sagebionetworks.logging.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.ObjectListing;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:log-sweeper.spb.xml" })
public class LogDAOImplTest {
	
	@Autowired
	LogDAO logDAO;
	@Autowired
	StackConfiguration config;

	@Before
	public void before(){
		// Clear all log data
		logDAO.deleteAllStackInstanceLogs();
	}
	
	@Test
	public void testRoundTrip() throws IOException{
		// Create a small log that can be saved to S3
		long now = System.currentTimeMillis();
		File temp = File.createTempFile("tempLogFile", ".date.log.gz");
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(temp))));
		String key = null;
		try{
			// Write some log entries to the file
			writer.write(LogKeyUtils.createISO8601GMTLogString(now)+" entry one");
			writer.newLine();
			writer.write(LogKeyUtils.createISO8601GMTLogString(now+1)+" entry two");
			writer.newLine();
			writer.write(LogKeyUtils.createISO8601GMTLogString(now+2)+" entry three");
			writer.newLine();
			writer.close();
			// Now save this file to S3
			key = logDAO.saveLogFile(temp, now);
			assertNotNull(key);
		}finally{
			try {
				writer.close();
			} catch (IOException e) {}
			temp.delete();
		}
		
		// Now we should be able to find this file when we list the files
		ObjectListing listing = logDAO.listAllStackInstanceLogs(null);
		assertNotNull(listing);
		// There should only be one file here
		assertEquals(1, listing.getObjectSummaries().size());
		assertEquals(key, listing.getObjectSummaries().get(0).getKey());
		// Now get the reader for this log
		LogReader reader = logDAO.getLogFileReader(key);
		List<LogEntry> entries = LogWriterUtil.streamEntiresForKey(reader);
		assertNotNull(entries);
		assertEquals("There should be three entries for this file", 3, entries.size());
		LogEntry entry = entries.get(0);
		assertEquals(now, entry.getTimeStamp());
		String entryString = entry.getEntryString();
		assertNotNull(entryString);
		System.out.println(entryString);
		assertTrue(entryString.endsWith("entry one"));
		//two
		entry = entries.get(1);
		assertEquals(now+1, entry.getTimeStamp());
		entryString = entry.getEntryString();
		assertNotNull(entryString);
		System.out.println(entryString);
		assertTrue(entryString.endsWith("entry two"));
		//three
		entry = entries.get(2);
		assertEquals(now+2, entry.getTimeStamp());
		entryString = entry.getEntryString();
		assertNotNull(entryString);
		System.out.println(entryString);
		assertTrue(entryString.endsWith("entry three"));
		// Now delete the file
		logDAO.deleteLogFile(key);
		try{
			logDAO.getLogFileReader(key);
			fail("This should have failed");
		}catch(AmazonClientException e){
			// This is expected
		}
		
	}
	
	@Test
	public void testFindLogContainingUUID() throws IOException, InterruptedException{
		String uuid = UUID.randomUUID().toString();
		String type = "findTest";
		String tempDir = System.getProperty("java.io.tmpdir");
		File sampleLog = LogWriterUtil.createSampleLogFile(new File(tempDir).getAbsolutePath(), type, new String[]{uuid});
		// Save the file to s3
		String key = this.logDAO.saveLogFile(sampleLog, System.currentTimeMillis());
		// Now make sure we can find this file again using this key
		String keySearchResult = logDAO.findLogContainingUUID(uuid);
		assertNotNull("Failed to find a log contain a UUID", keySearchResult);
		assertEquals(key, keySearchResult);
		logDAO.deleteLogFile(key);
	}

	
}
