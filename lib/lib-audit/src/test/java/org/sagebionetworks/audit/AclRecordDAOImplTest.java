package org.sagebionetworks.audit;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.audit.dao.AclRecordDAO;
import org.sagebionetworks.repo.model.audit.AclRecord;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.AmazonS3Client;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:audit-dao.spb.xml" })
public class AclRecordDAOImplTest {

	@Autowired
	AclRecordDAO aclRecordDao;
	
	@Autowired
	private AmazonS3Client s3Client;
	
	@Before
	public void before(){
		assertNotNull(aclRecordDao);
		assertNull(aclRecordDao.getCurrentFile());
		assertEquals(0, aclRecordDao.getLineCount());
		
		assertNotNull(s3Client);
		assertTrue(s3Client.doesBucketExist("prod.acl.record.sagebase.org"));
	}
	
	@After
	public void after(){
		aclRecordDao.cleanUp();
	}
	
	@Test
	public void test() throws IOException{
		List<AclRecord> records = createAclRecordList(5);
		for (AclRecord record : records) {
			aclRecordDao.write(record);
		}
		assertNotNull(aclRecordDao.getCurrentFile());
		assertEquals(5, aclRecordDao.getLineCount());
		
		records = createAclRecordList(1995);
		for (AclRecord record : records) {
			aclRecordDao.write(record);
		}
		assertNull(aclRecordDao.getCurrentFile());
		assertEquals(0, aclRecordDao.getLineCount());
		
		records = createAclRecordList(1);
		for (AclRecord record : records) {
			aclRecordDao.write(record);
		}
		assertNotNull(aclRecordDao.getCurrentFile());
		assertEquals(1, aclRecordDao.getLineCount());
 		
	}

	private List<AclRecord> createAclRecordList(int numberOfRecords) {
		List<AclRecord> list = new ArrayList<AclRecord>();
		Random random = new Random();
		for (int i = 0; i < numberOfRecords; i++) {
			AclRecord newRecord = new AclRecord();
			newRecord.setTimestamp(System.currentTimeMillis());
			newRecord.setChangeNumber(Integer.toString(random.nextInt()));
			newRecord.setChangeType(ChangeType.CREATE.name());
			newRecord.setObjectId(Integer.toString(random.nextInt()));
			newRecord.setEtag("etag");
			list.add(newRecord);
		}
		return list;
	}

}
