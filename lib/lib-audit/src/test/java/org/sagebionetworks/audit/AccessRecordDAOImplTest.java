package org.sagebionetworks.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
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
public class AccessRecordDAOImplTest {

	@Autowired
	AccessRecordDAO accessRecordDAO;
	
	
	@Before
	public void before(){

	}
	
	@After
	public void after(){
		// Clear the data for this test instance.
		accessRecordDAO.deleteAllStackInstanceBatches();
	}
	
	
	@Test
	public void testRoundTrip() throws IOException{
		List<AccessRecord> toTest = AuditTestUtils.createList(5, 100);
		// create the batch
		String key = accessRecordDAO.saveBatch(toTest);
		assertNotNull(key);

		List<AccessRecord> back = accessRecordDAO.getBatch(key);
		assertEquals(toTest, back);
	}
	
	@Test
	public void testSaveWithTimestamp() throws IOException{
	    Calendar cal = Calendar.getInstance();
		cal.set(1982, 4, 20, 22, 49);
		String expected = "1982-05-20";
		List<AccessRecord> toTest = AuditTestUtils.createList(1, 100);
		// create the batch
		String key = accessRecordDAO.saveBatch(toTest, cal.getTimeInMillis());
		assertTrue(key.indexOf(expected) > 0);
	}
	
	@Test
	public void testListBatchKeys() throws IOException{
		// Start with no batches
		accessRecordDAO.deleteAllStackInstanceBatches();
		int count = 5;
		Set<String> keys = new HashSet<String>();
		for(int i=0; i< count; i++){
			List<AccessRecord> toTest = AuditTestUtils.createList(1, 2001);
			String key = accessRecordDAO.saveBatch(toTest);
			assertNotNull(key);
			keys.add(key);
		}
		// Now iterate over all key and ensure all keys are found
		Set<String> foundKeys = new HashSet<String>();
		String marker = null;
		do{
			System.out.println("marker: "+marker);
			ObjectListing listing = accessRecordDAO.listBatchKeys(marker);
			marker = listing.getNextMarker();
			for(S3ObjectSummary summ: listing.getObjectSummaries()){
				foundKeys.add(summ.getKey());
			}
		}while(marker != null);
		// the two set should be equal
		assertEquals(keys, foundKeys);
	}
}
