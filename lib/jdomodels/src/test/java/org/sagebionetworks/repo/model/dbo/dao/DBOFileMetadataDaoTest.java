package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertNotNull;

import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.FileMetadataDao;
import org.sagebionetworks.repo.model.file.FileMetadata;
import org.sagebionetworks.repo.model.file.S3FileMetadata;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOFileMetadataDaoTest {

	@Autowired
	FileMetadataDao fileMetadataDao;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	private List<Long> toDelete;
	Long creatorUserGroupId;
	
	@Before
	public void before(){
		toDelete = new LinkedList<Long>();
		creatorUserGroupId = Long.parseLong(userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId());
		assertNotNull(creatorUserGroupId);
	}
	
	@After
	public void after(){
		if(fileMetadataDao != null && toDelete != null){
			for(Long id: toDelete){
				fileMetadataDao.delete(id);
			}
		}
	}
	
	@Test
	public void testS3FileCURD() throws DatastoreException, NotFoundException{
		// Create the metadata
		S3FileMetadata meta = new S3FileMetadata();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		meta.setCreatedByPrincipalId(creatorUserGroupId);
		// Save it
		Long id = fileMetadataDao.create(meta);
		assertNotNull(id);
		toDelete.add(id);
		FileMetadata clone = fileMetadataDao.get(id);
		assertNotNull(clone);
	}
}
