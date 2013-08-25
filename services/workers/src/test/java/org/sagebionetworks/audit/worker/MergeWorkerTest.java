package org.sagebionetworks.audit.worker;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.audit.dao.AccessRecordDAO;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:audit-dao.spb.xml" })
public class MergeWorkerTest {

	@Autowired
	private AccessRecordDAO accessRecordDAO;
	
	@After
	public void after(){
		// Delete all data created by this test.
//		accessRecordDAO.deleteAllStackInstanceBatches();
	}
	
	@Ignore
	@Test
	public void testIntegration() throws IOException{
		// Start this test with no data in the bucket
//		accessRecordDAO.deleteAllStackInstanceBatches();
		// Setup the times
	    Calendar cal = Calendar.getInstance();
		cal.set(1982, 4, 20, 0, 0);
		long dayOneTimeStamp = cal.getTimeInMillis();
		// Day two
	    cal = Calendar.getInstance();
		cal.set(1982, 4, 21, 0, 0);
		long daytwoTime = cal.getTimeInMillis();
		int count = 1;
		int totalCount = count;
		// Create three days worth of data
		long start = System.currentTimeMillis();
		for(int i=0; i< count; i++){
			List<AccessRecord> toTest = AuditTestUtils.createList(1, dayOneTimeStamp);
			System.out.println("day one create: "+i);
			String key = accessRecordDAO.saveBatch(toTest, toTest.get(0).getTimestamp());
			assertNotNull(key);
		}
		long elapse = System.currentTimeMillis()-start;
		System.out.println("Created batches at: "+(elapse/count)+" ms/batch");
		// Now create day two
		count = 1;
		totalCount += count;
		// Create three days worth of data
		start = System.currentTimeMillis();
		for(int i=0; i< count; i++){
			List<AccessRecord> toTest = AuditTestUtils.createList(1, daytwoTime);
			System.out.println("day two create: "+i);
			String key = accessRecordDAO.saveBatch(toTest, toTest.get(0).getTimestamp());
			assertNotNull(key);
		}
		elapse = System.currentTimeMillis()-start;
		System.out.println("Created batches at: "+(elapse/count)+" ms/batch");
		// Now let the worker do the its thing
		MergeWorker worker = new MergeWorker(accessRecordDAO, 1000*40);
		start = System.currentTimeMillis();
		worker.run();
		elapse = System.currentTimeMillis()-start;
		System.out.println("Merged batches at: "+(elapse/totalCount)+" ms/batch");
		System.out.println("whoot");
	}
	
	@Test
	public void testProd(){
		System.out.println(StackConfiguration.getStack());
		System.out.println(StackConfiguration.getStackInstance());
		System.out.println(StackConfiguration.getIAMUserId());
		System.out.println(StackConfiguration.getIAMUserKey());
		MergeWorker worker = new MergeWorker(accessRecordDAO, 1000*100);
		long start = System.currentTimeMillis();
		worker.run();
		long elapse = System.currentTimeMillis()-start;
		System.out.println("Finished in "+(elapse)+" ms");
	}
}
