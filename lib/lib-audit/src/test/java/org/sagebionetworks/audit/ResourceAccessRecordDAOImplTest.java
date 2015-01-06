package org.sagebionetworks.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.audit.dao.ResourceAccessRecordDAO;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.audit.ResourceAccessRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.AmazonS3Client;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:audit-dao.spb.xml" })
public class ResourceAccessRecordDAOImplTest {

	@Autowired
	private ResourceAccessRecordDAO resourceAccessRecordDao;
	
	@Autowired
	private AmazonS3Client s3Client;
	
	private String BUCKET_NAME = "dev.resource.access.record.sagebase.org";
	
	@Before
	public void before(){
		assertNotNull(resourceAccessRecordDao);
		assertNotNull(s3Client);
	}
	
	@Test
	public void test() throws IOException{
		List<ResourceAccessRecord> records = createResourceAccessRecordList(5);
		String key = resourceAccessRecordDao.saveBatch(records);
		assertNotNull(s3Client.getObject(BUCKET_NAME, key));
		assertEquals(records, resourceAccessRecordDao.getBatch(key));
	}

	private List<ResourceAccessRecord> createResourceAccessRecordList(int numberOfRecords) {
		List<ResourceAccessRecord> list = new ArrayList<ResourceAccessRecord>();
		for (int i = 0; i < numberOfRecords; i++) {
			ResourceAccessRecord newRecord = new ResourceAccessRecord();
			newRecord.setAccessType(new HashSet<ACCESS_TYPE>(Arrays.asList(ACCESS_TYPE.DOWNLOAD, ACCESS_TYPE.READ)));
			newRecord.setChangeNumber(-1L);
			newRecord.setPrincipalId(-1L);

			list.add(newRecord);
		}
		return list;
	}
}
