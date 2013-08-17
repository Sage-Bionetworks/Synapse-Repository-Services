package org.sagebionetworks.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.audit.dao.AccessRecordDAO;
import org.sagebionetworks.repo.model.audit.AccessRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
}
