package org.sagebionetworks.audit.worker;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.audit.dao.AccessRecordDAO;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.model.ObjectListing;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MergeWorkerFactoryTest {
	
	/**
	 * This test needs more time to run
	 */
	private long MAX_WAIT = 1000*60*2;

	@Autowired
	private AccessRecordDAO accessRecordDAO;
	
	@After
	public void after(){
		// Delete all data created by this test.
		accessRecordDAO.deleteAllStackInstanceBatches();
	}
	
	@Test
	public void testIntegration() throws IOException, InterruptedException{
		// Start this test with no data in the bucket
		accessRecordDAO.deleteAllStackInstanceBatches();
		// Setup the times
	    Calendar cal = Calendar.getInstance();
		cal.set(1982, 4, 20, 0, 0);
		long dayOneTimeStamp = cal.getTimeInMillis();

		int count = 10;
		// Create batches for day one
		Set<String> dayOneSessionIds = createBatchesForDay(dayOneTimeStamp, count, 2);
		
		// Now if everything is wired correctly the MergeWorkerFactory timer will fire 
		// and start the MegeWorker which should convert the 10 files into 2 files
		long start = System.currentTimeMillis();
		int fileCount = 10;
		do{
			ObjectListing listing = accessRecordDAO.listBatchKeys(null);
			fileCount = listing.getObjectSummaries().size();
			System.out.println("Waiting for MegeWorkerFactory to merge audit records.  There are currently "+fileCount+" files...");
			Thread.sleep(10000);
			long elapse = System.currentTimeMillis()-start;
			assertTrue("Timed out waiting for the MegeWorkerFactory to merge audit record files",elapse < MAX_WAIT);
		}while(fileCount != 2);
		System.out.println("Done!");
	}

	/**
	 * @param dayOneTimeStamp
	 * @param count
	 * @param dayOneSessionIds
	 * @throws IOException
	 */
	public Set<String> createBatchesForDay(long dayOneTimeStamp, int count, int numberHours) throws IOException {
		Set<String> sessionIds = new HashSet<String>();
		int msPerHour = 1000*60*60;
		long start = System.currentTimeMillis();
		for(int i=0; i< count; i++){
			List<AccessRecord> toTest = AuditTestUtils.createList(1, dayOneTimeStamp+(i%numberHours*msPerHour));
			// Keep track of the session IDs for this day.
			for(AccessRecord ar: toTest){
				sessionIds.add(ar.getSessionId());
			}
			String key = accessRecordDAO.saveBatch(toTest, toTest.get(0).getTimestamp());
			assertNotNull(key);
		}
		long elapse = System.currentTimeMillis()-start;
		System.out.println("Created batches at: "+(elapse/count)+" ms/batch");
		return sessionIds;
	}
	
	/**
	 * Get all session IDs for a batch
	 * @param key
	 * @return
	 * @throws IOException
	 */
	public Set<String> getSessionIdsForKey(String key) throws IOException{
		Set<String> result = new HashSet<String>();
		List<AccessRecord> batch = accessRecordDAO.getBatch(key);
		for(AccessRecord ar: batch){
			result.add(ar.getSessionId());
		}
		return result;
	}
	
}
