package org.sagebionetworks.audit.worker;

import static org.junit.Assert.assertEquals;
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
import com.amazonaws.services.s3.model.S3ObjectSummary;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:audit-dao.spb.xml" })
public class MergeWorkerTest {
	
	private long MAX_WAIT = 1000*208;

	@Autowired
	private AccessRecordDAO accessRecordDAO;
	
	@After
	public void after(){
		// Delete all data created by this test.
		accessRecordDAO.deleteAllStackInstanceBatches();
	}
	
	@Test
	public void testIntegration() throws IOException{
		// Start this test with no data in the bucket
		accessRecordDAO.deleteAllStackInstanceBatches();
		// Setup the times
	    Calendar cal = Calendar.getInstance();
		cal.set(1982, 4, 20, 0, 0);
		long dayOneTimeStamp = cal.getTimeInMillis();

		int count = 10;
		// Create batches for day one
		Set<String> dayOneSessionIds = createBatchesForDay(dayOneTimeStamp, count, 2);

		// Now create day two
		count = 5;
		// Day two
	    cal = Calendar.getInstance();
		cal.set(1982, 4, 21, 0, 0);
		long dayTwoTimeStamp = cal.getTimeInMillis();
		// Create three days worth of data
		
		Set<String> dayTwoSessionIds = createBatchesForDay(dayTwoTimeStamp, count, 1);
		// Now let the worker do the its thing
		MergeWorker worker = new MergeWorker(accessRecordDAO);
		long start = System.currentTimeMillis();
		boolean hasMore = true;
		// Wait until the worker merges all files
		while(hasMore){
			hasMore = worker.mergeOneBatch();
			long elapse = System.currentTimeMillis()-start;
			assertTrue("Timed out waiting for merge worker to finish",elapse < MAX_WAIT);
		}
		
		// Now list the files for each day.
		ObjectListing listing = accessRecordDAO.listBatchKeys(null);
		assertNotNull(listing);
		assertNotNull(listing.getObjectSummaries());
		assertEquals("The worker should have merged the data into 3 files",3, listing.getObjectSummaries().size());
		S3ObjectSummary sumOne = listing.getObjectSummaries().get(0);
		assertNotNull(sumOne);
		S3ObjectSummary sumTwo = listing.getObjectSummaries().get(1);
		assertNotNull(sumTwo);
		S3ObjectSummary sumThree = listing.getObjectSummaries().get(2);
		assertNotNull(sumThree);
		// The first two should have the first date
		assertTrue(sumOne.getKey().indexOf("1982-05-20") > 0);
		assertTrue(sumTwo.getKey().indexOf("1982-05-20") > 0);
		// The last file should be from the next day
		assertTrue(sumThree.getKey().indexOf("1982-05-21") > 0);
		// Validate that all session Ids are where they are expected to be
		Set<String> dayOne = getSessionIdsForKey(sumOne.getKey());
		dayOne.addAll(getSessionIdsForKey(sumTwo.getKey()));
		assertEquals("Did not find the expected number of records merged into a single day's folder",dayOneSessionIds.size() , dayOne.size());
		assertEquals("The merged files did not have the expected records", dayOneSessionIds, dayOne);
		Set<String> dayTwo = getSessionIdsForKey(sumThree.getKey());
		assertEquals("Did not find the expected number of records merged into a single day's folder",dayTwoSessionIds.size() , dayTwo.size());
		assertEquals("The merged files did not have the expected records", dayTwoSessionIds, dayTwo);
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
