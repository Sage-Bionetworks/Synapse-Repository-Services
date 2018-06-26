package org.sagebionetworks.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.audit.dao.AccessRecordDAO;
import org.sagebionetworks.aws.utils.s3.KeyGeneratorUtil;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:audit-dao.spb.xml" })
public class AccessRecordDAOImplTest {

	public static final long MAX_TIMEOUT_MS = 30*1000;
	@Autowired
	AccessRecordDAO accessRecordDAO;
	
	
	@Before
	public void before(){
		// Start with no batches
		accessRecordDAO.deleteAllStackInstanceBatches();
	}
	
	@After
	public void after(){
		// Clear the data for this test instance.
		accessRecordDAO.deleteAllStackInstanceBatches();
	}
	
	
	@Test
	public void testRoundTrip() throws IOException{
		List<AccessRecord> toTest = AuditTestUtils.createAccessRecordList(5, 100);
		// create the batch
		String key = accessRecordDAO.saveBatch(toTest, true);
		assertNotNull(key);
		assertTrue(key.contains(KeyGeneratorUtil.ROLLING));

		List<AccessRecord> back = accessRecordDAO.getBatch(key);
		assertEquals(toTest, back);
	}
	
	@Test
	public void testSaveWithTimestamp() throws IOException{
	    Calendar cal = KeyGeneratorUtil.getClaendarUTC();
		cal.set(1982, 4, 20, 22, 49);
		String expected = "1982-05-20";
		List<AccessRecord> toTest = AuditTestUtils.createAccessRecordList(1, 100);
		// create the batch
		String key = accessRecordDAO.saveBatch(toTest, cal.getTimeInMillis(), false);
		assertTrue(key.indexOf(expected) > 0);
		assertFalse(key.contains(KeyGeneratorUtil.ROLLING));
	}
	
	@Test
	public void testListBatchKeys() throws IOException, InterruptedException{
		List<AccessRecord> toTest = AuditTestUtils.createAccessRecordList(1, 2001);
		String key = accessRecordDAO.saveBatch(toTest, true);
		assertNotNull(key);
		long start = System.currentTimeMillis();
		while(true) {
			assertTrue("Timeout waiting for key to appear in S3.", System.currentTimeMillis()-start < MAX_TIMEOUT_MS);
			Set<String> foundKeys = accessRecordDAO.listAllKeys();
			if(foundKeys.contains(key)) {
				break;
			}else {
				Thread.sleep(1000);
			}
		}
	}
}
