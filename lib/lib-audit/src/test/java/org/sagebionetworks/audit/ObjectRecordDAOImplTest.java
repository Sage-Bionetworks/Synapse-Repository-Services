package org.sagebionetworks.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.audit.utils.KeyGeneratorUtil;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:audit-dao.spb.xml" })
public class ObjectRecordDAOImplTest {

	@Autowired
	private ObjectRecordDAO objectRecordDao;
	
	@Before
	public void before(){
		assertNotNull(objectRecordDao);
	}

	@After
	public void after(){
		// Clear the data for this test instance.
		objectRecordDao.deleteAllStackInstanceBatches();
	}

	@Test
	public void testRoundTrip() throws IOException{
		List<ObjectRecord> toTest = AuditTestUtils.createObjectRecordList(5);
		// create the batch
		String key = objectRecordDao.saveBatch(toTest);
		assertNotNull(key);
		assertFalse(key.contains(KeyGeneratorUtil.ROLLING));

		List<ObjectRecord> back = objectRecordDao.getBatch(key);
		assertEquals(toTest, back);
	}

}
