package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class StorageLocationDAOImplTest {

	@Autowired
	private StorageLocationDAO storageLocationDAO;

	@Autowired
	private FileHandleDao fileHandleDao;

	@Autowired
	private UserGroupDAO userGroupDAO;

	private String userId;

	private List<String> toDelete;

	@Before
	public void before(){
		assertNotNull(storageLocationDAO);
		assertNotNull(fileHandleDao);
		assertNotNull(userGroupDAO);
		toDelete = new ArrayList<String>();
		userId = userGroupDAO.findGroup(
				AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId();
		assertNotNull(userId);
	}

	@After
	public void after(){
		for(String id: toDelete){
			fileHandleDao.delete(id);
		}
	}

	@Test
	public void testSizeAndCount() throws DatastoreException, NotFoundException{

		// Get baselines
		final int totalSize = storageLocationDAO.getTotalSize().intValue();
		assertTrue(totalSize >=0 );
		final int totalSizeForUser = storageLocationDAO.getTotalSizeForUser(userId).intValue();
		assertTrue(totalSizeForUser >= 0);
		final int totalCount = storageLocationDAO.getTotalCount().intValue();
		assertTrue(totalCount >= 0);
		final int totalCountForUser = storageLocationDAO.getTotalCountForUser(userId).intValue();
		assertTrue(totalCountForUser >= 0);

		// Create the files -- only S3 files count here
		final int size = 50;
		S3FileHandle s3 = TestUtils.createS3FileHandle(userId, size);
		s3 = fileHandleDao.createFile(s3);
		assertNotNull(s3);
		final String s3Id = s3.getId();
		assertNotNull(s3Id);
		toDelete.add(s3Id);

		PreviewFileHandle preview = TestUtils.createPreviewFileHandle(userId);
		preview = fileHandleDao.createFile(preview);
		assertNotNull(preview);
		final String previewId = preview.getId();
		assertNotNull(previewId);
		toDelete.add(previewId);

		ExternalFileHandle external = TestUtils.createExternalFileHandle(userId);
		external = fileHandleDao.createFile(external);
		assertNotNull(external);
		final String extId = external.getId();
		assertNotNull(extId);
		toDelete.add(extId);

		assertEquals(totalSize + size, storageLocationDAO.getTotalSize().intValue());
		assertEquals(totalSizeForUser + size, storageLocationDAO.getTotalSizeForUser(userId).intValue());
		assertEquals(totalCount + 1, storageLocationDAO.getTotalCount().intValue());
		assertEquals(totalCountForUser + 1, storageLocationDAO.getTotalCountForUser(userId).intValue());
		assertEquals(0, storageLocationDAO.getTotalCountForNode("syn123").intValue());
	}
}
