package org.sagebionetworks.repo.manager.file.download;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.DownloadList;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class BulkDownloadManagerImplAutowireTest {

	@Autowired
	UserGroupDAO userGroupDao;
	
	@Autowired
	BulkDownloadManager bulkDownloadManager;
	
	@Autowired
	EntityManager entityManager;
	
	Long userOneIdLong;
	String userOneId;
	UserInfo user;
	UserInfo admin;
	
	@Before
	public void before() {
		UserGroup ug = new UserGroup();
		ug.setCreationDate(new Date(System.currentTimeMillis()));
		ug.setIsIndividual(true);
		userOneIdLong = userGroupDao.create(ug);
		userOneId = ""+userOneIdLong;
		boolean isAdmin = false;
		user = new UserInfo(isAdmin, userOneIdLong);
		
		isAdmin = true;
		admin = new UserInfo(isAdmin, 123L);
		
	}
	
	
	@After
	public void after() {
		bulkDownloadManager.truncateAllDownloadDataForAllUsers(admin);
		if(userOneId != null) {
			userGroupDao.delete(userOneId);
		}
	}
	
	/**
	 * Validate that an over-the-limit exception will roll-back the transaction.
	 */
	@Test
	public void testAddOverLimit() {
		try {
			// call under test
			bulkDownloadManager.addFileHandleAssociations(user, createFileHandleAssociations(BulkDownloadManagerImpl.MAX_FILES_PER_DOWNLOAD_LIST+1));
			fail();
		} catch (IllegalArgumentException e) {
			assertEquals(BulkDownloadManagerImpl.EXCEEDED_MAX_NUMBER_ROWS, e.getMessage());
		}
		// the users list should be empty
		DownloadList list = bulkDownloadManager.getDownloadList(user);
		assertNotNull(list);
		assertNotNull(list.getFilesToDownload());
		assertTrue(list.getFilesToDownload().isEmpty());
	}
	
	/**
	 * Test helper
	 * @param size
	 * @return
	 */
	List<FileHandleAssociation> createFileHandleAssociations(int size){
		List<FileHandleAssociation> result = new LinkedList<>();
		for (int i = 0; i < size; i++) {
			FileHandleAssociation fha = new FileHandleAssociation();
			fha.setAssociateObjectId("" + i);
			fha.setAssociateObjectType(FileHandleAssociateType.FileEntity);
			String indexString = "" + i;
			fha.setFileHandleId(indexString + indexString + indexString);
			result.add(fha);
		}
		return result;
	}

}
