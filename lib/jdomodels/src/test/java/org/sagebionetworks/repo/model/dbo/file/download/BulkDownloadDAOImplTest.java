package org.sagebionetworks.repo.model.dbo.file.download;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.DownloadList;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class BulkDownloadDAOImplTest {
	
	@Autowired
	UserGroupDAO userGroupDao;
	@Autowired
	FileHandleDao fileHandleDao;
	@Autowired
	IdGenerator idGenerator;
	@Autowired
	BulkDownloadDAO bulkDownlaodDao;
	
	Long userOneIdLong;
	String userOneId;
	
	Long userTwoIdLong;
	String userTwoId;
	
	List<FileHandle> fileHandles;
	List<FileHandleAssociation> fileHandleAssociations;
	
	@Before
	public void before() {
		UserGroup ug = new UserGroup();
		ug.setCreationDate(new Date(System.currentTimeMillis()));
		ug.setIsIndividual(true);
		userOneIdLong = userGroupDao.create(ug);
		userOneId = ""+userOneIdLong;
		// second user
		ug = new UserGroup();
		ug.setCreationDate(new Date(System.currentTimeMillis()));
		ug.setIsIndividual(true);
		userTwoIdLong = userGroupDao.create(ug);
		userTwoId = ""+userTwoIdLong;
		
		fileHandles = new LinkedList<>();
		for(int i=0; i<4; i++) {
			S3FileHandle fileHandle = new S3FileHandle();
			fileHandle.setBucketName("someBucket");
			fileHandle.setContentMd5("MD5");
			fileHandle.setCreatedBy(""+userOneIdLong);
			fileHandle.setCreatedOn(new Date(System.currentTimeMillis()) );
			fileHandle.setKey("key-"+i);
			fileHandle.setFileName("name-"+i);
			fileHandle.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
			fileHandle.setEtag(UUID.randomUUID().toString());
			fileHandle = (S3FileHandle) fileHandleDao.createFile(fileHandle);
			fileHandles.add(fileHandle);
		}
		
		fileHandleAssociations = new LinkedList<>();
		long index = 0;
		for(FileHandle file: fileHandles) {
			FileHandleAssociation fha = new FileHandleAssociation();
			fha.setAssociateObjectId(""+index++);
			fha.setAssociateObjectType(FileHandleAssociateType.FileEntity);
			fha.setFileHandleId(file.getId());
			fileHandleAssociations.add(fha);
		}
	}
	

	@After
	public void after() {
		if(userOneId != null) {
			userGroupDao.delete(userOneId);
		}
		if(userTwoId != null) {
			userGroupDao.delete(userTwoId);
		}
		
		if(fileHandles != null) {
			for(FileHandle file: fileHandles) {
				fileHandleDao.delete(file.getId());
			}
		}
	}
	
	@Test
	public void testCreateFromDBOFileAssociation() {
		DBODownloadListItem item = new DBODownloadListItem();
		item.setPrincipalId(userOneIdLong);
		item.setAssociatedObjectId(123L);
		item.setAssociatedObjectType(FileHandleAssociateType.FileEntity.name());
		item.setFileHandleId(456L);
		// call under test
		FileHandleAssociation fha = BulkDownloadDAOImpl.translateFromDBOtoDTO(item);
		assertNotNull(fha);
		assertEquals(""+item.getAssociatedObjectId(), fha.getAssociateObjectId());
		assertEquals(item.getAssociatedObjectType(), fha.getAssociateObjectType().name());
		assertEquals(""+item.getFileHandleId(), fha.getFileHandleId());
	}
	
	@Test
	public void testCreateFromDBOFileAssociationList() {
		DBODownloadListItem item = new DBODownloadListItem();
		item.setPrincipalId(userOneIdLong);
		item.setAssociatedObjectId(123L);
		item.setAssociatedObjectType(FileHandleAssociateType.FileEntity.name());
		item.setFileHandleId(456L);
		// call under test
		List<FileHandleAssociation> list = BulkDownloadDAOImpl.translateFromDBOtoDTO(Lists.newArrayList(item));
		assertNotNull(list);
		assertEquals(1, list.size());
		FileHandleAssociation fha = list.get(0);
		assertEquals(""+item.getAssociatedObjectId(), fha.getAssociateObjectId());
		assertEquals(item.getAssociatedObjectType(), fha.getAssociateObjectType().name());
		assertEquals(""+item.getFileHandleId(), fha.getFileHandleId());
	}

	@Test
	public void testCreateFromDBOFileAssociationListNull() {
		List<DBODownloadListItem> items = null;
		// call under test
		List<FileHandleAssociation> list = BulkDownloadDAOImpl.translateFromDBOtoDTO(items);
		assertEquals(null, list);
	}
	
	@Test
	public void testCraeteFromDBODownloadList() {
		DBODownloadList list = new DBODownloadList();
		list.setEtag("etag");
		list.setPrincipalId(userOneIdLong);
		list.setUpdatedOn(System.currentTimeMillis());
		DBODownloadListItem item = new DBODownloadListItem();
		item.setAssociatedObjectId(123L);
		item.setAssociatedObjectType(FileHandleAssociateType.FileEntity.name());
		item.setFileHandleId(456L);
		List<DBODownloadListItem> items = Lists.newArrayList(item);
		// call under test
		DownloadList result = BulkDownloadDAOImpl.translateFromDBOtoDTO(list, items);
		assertNotNull(result);
		assertEquals(userOneId, result.getOwnerId());
		assertEquals(new Date(list.getUpdatedOn()), result.getUpdatedOn());
		assertNotNull(result.getFilesToDownload());
		assertEquals(1, result.getFilesToDownload().size());
		assertEquals("etag", result.getEtag());
	}
	
	@Test
	public void testCraeteFromDBODownloadListNullItems() {
		DBODownloadList list = new DBODownloadList();
		list.setEtag("etag");
		list.setPrincipalId(userOneIdLong);
		list.setUpdatedOn(System.currentTimeMillis());
		DBODownloadListItem item = new DBODownloadListItem();
		item.setAssociatedObjectId(123L);
		item.setAssociatedObjectType(FileHandleAssociateType.FileEntity.name());
		item.setFileHandleId(456L);
		List<DBODownloadListItem> items = null;
		// call under test
		DownloadList result = BulkDownloadDAOImpl.translateFromDBOtoDTO(list, items);
		assertNotNull(result);
		assertEquals(userOneId, result.getOwnerId());
		assertEquals(new Date(list.getUpdatedOn()), result.getUpdatedOn());
		assertEquals(null, result.getFilesToDownload());
		assertEquals("etag", result.getEtag());
	}
	
	@Test
	public void testGetUsersDownloadListNeverCreated() {
		// There is no data for this user yet.
		// call under test
		DownloadList list = bulkDownlaodDao.getUsersDownloadList(userOneId);
		assertNotNull(list);
		assertNotNull(list.getUpdatedOn());
		assertNotNull(list.getEtag());
		assertEquals(null, list.getFilesToDownload());
		assertEquals(this.userOneId, list.getOwnerId());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetUsersDownloadListNullUser() {
		// call under test
		bulkDownlaodDao.getUsersDownloadList(null);
	}
	
	@Test
	public void testCreateDBOFileHandleAssociation() {
		FileHandleAssociation fha = new FileHandleAssociation();
		fha.setAssociateObjectId("123");
		fha.setAssociateObjectType(FileHandleAssociateType.FileEntity);
		fha.setFileHandleId("456");
		// call under test
		DBODownloadListItem item = BulkDownloadDAOImpl.translateFromDTOtoDBO(userOneIdLong, fha);
		assertNotNull(item);
		assertEquals(new Long(123), item.getAssociatedObjectId());
		assertEquals(FileHandleAssociateType.FileEntity.name(), item.getAssociatedObjectType());
		assertEquals(new Long(456), item.getFileHandleId());
		assertEquals(userOneIdLong, item.getPrincipalId());
	}
	

	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateDBOFileHandleAssociationNull() {
		FileHandleAssociation fha = null;
		// call under test
		BulkDownloadDAOImpl.translateFromDTOtoDBO(userOneIdLong, fha);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateDBOFileHandleAssociationObjectIdNull() {
		FileHandleAssociation fha = new FileHandleAssociation();
		fha.setAssociateObjectId(null);
		fha.setAssociateObjectType(FileHandleAssociateType.FileEntity);
		fha.setFileHandleId("456");
		// call under test
		BulkDownloadDAOImpl.translateFromDTOtoDBO(userOneIdLong, fha);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateDBOFileHandleAssociationTypeNull() {
		FileHandleAssociation fha = new FileHandleAssociation();
		fha.setAssociateObjectId("123");
		fha.setAssociateObjectType(null);
		fha.setFileHandleId("456");
		// call under test
		BulkDownloadDAOImpl.translateFromDTOtoDBO(userOneIdLong, fha);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateDBOFileHandleAssociationFileNull() {
		FileHandleAssociation fha = new FileHandleAssociation();
		fha.setAssociateObjectId("123");
		fha.setAssociateObjectType(FileHandleAssociateType.FileEntity);
		fha.setFileHandleId(null);
		// call under test
		BulkDownloadDAOImpl.translateFromDTOtoDBO(userOneIdLong, fha);
	}

	@Test
	public void testCreateDBOFileHandleAssociationList() {
		FileHandleAssociation fha = new FileHandleAssociation();
		fha.setAssociateObjectId("123");
		fha.setAssociateObjectType(FileHandleAssociateType.FileEntity);
		fha.setFileHandleId("456");
		List<FileHandleAssociation> list = Lists.newArrayList(fha);
		// call under test
		List<DBODownloadListItem> items = BulkDownloadDAOImpl.translateFromDTOtoDBO(userOneIdLong, list);
		assertNotNull(items);
		assertEquals(1, items.size());
		DBODownloadListItem item = items.get(0);
		assertEquals(new Long(123), item.getAssociatedObjectId());
		assertEquals(FileHandleAssociateType.FileEntity.name(), item.getAssociatedObjectType());
		assertEquals(new Long(456), item.getFileHandleId());
		assertEquals(userOneIdLong, item.getPrincipalId());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateDBOFileHandleAssociationListNullList() {
		List<FileHandleAssociation> list = null;
		// call under test
		BulkDownloadDAOImpl.translateFromDTOtoDBO(userOneIdLong, list);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateDBOFileHandleAssociationListEmpty() {
		List<FileHandleAssociation> list = new LinkedList<>();
		// call under test
		BulkDownloadDAOImpl.translateFromDTOtoDBO(userOneIdLong, list);
	}
	
	@Test
	public void testTouchUsersDownloadList() throws InterruptedException {
		DownloadList old = bulkDownlaodDao.getUsersDownloadList(userOneId);
		Thread.sleep(10L);
		assertNotNull(old);
		// call under test
		bulkDownlaodDao.touchUsersDownloadList(userOneIdLong);
		
		DownloadList updated = bulkDownlaodDao.getUsersDownloadList(userOneId);
		assertNotNull(updated.getEtag());
		assertNotNull(updated.getUpdatedOn());
		assertFalse(updated.getEtag().equals(old.getEtag()));
		assertTrue(updated.getUpdatedOn().getTime() > old.getUpdatedOn().getTime());
		
		// touch again should update again
		// call under test
		bulkDownlaodDao.touchUsersDownloadList(userOneIdLong);
		updated = bulkDownlaodDao.getUsersDownloadList(userOneId);
		assertNotNull(updated.getEtag());
		assertNotNull(updated.getUpdatedOn());
		assertFalse(updated.getEtag().equals(old.getEtag()));
		assertTrue(updated.getUpdatedOn().getTime() > old.getUpdatedOn().getTime());
	}
	
	@Test
	public void testAddFiles() {
		// Add a few files to a user's list
		List<FileHandleAssociation> toAdd = fileHandleAssociations.subList(1, 3);
		// call under test
		DownloadList result = bulkDownlaodDao.addFilesToDownloadList(userOneId, toAdd);
		assertNotNull(result);
		assertEquals(userOneId, result.getOwnerId());
		assertNotNull(result.getEtag());
		assertNotNull(result.getUpdatedOn());
		assertEquals(toAdd, result.getFilesToDownload());
	}
	
	@Test
	public void testAddFilesOverlapSameUser() {
		// add 1-3
		List<FileHandleAssociation> toAdd = fileHandleAssociations.subList(1, 3);
		// call under test
		DownloadList result = bulkDownlaodDao.addFilesToDownloadList(userOneId, toAdd);
		// add overlap
		toAdd = fileHandleAssociations.subList(2, 4);
		// add 2-4
		result = bulkDownlaodDao.addFilesToDownloadList(userOneId, toAdd);
		assertEquals(fileHandleAssociations.subList(1, 4), result.getFilesToDownload());
	}
	
	@Test
	public void testAddFilesOverLapMultipleUsers() {
		// 0-2 added user one
		List<FileHandleAssociation> oneFiles = fileHandleAssociations.subList(0, 2);
		// call under test
		DownloadList resultOne = bulkDownlaodDao.addFilesToDownloadList(userOneId, oneFiles);
		assertEquals(fileHandleAssociations.subList(0, 2), resultOne.getFilesToDownload());
		// 1-3 added to user two
		List<FileHandleAssociation> twoFiles = fileHandleAssociations.subList(1, 3);
		// call under test
		DownloadList resultTwo = bulkDownlaodDao.addFilesToDownloadList(userTwoId, twoFiles);
		assertEquals(fileHandleAssociations.subList(1, 3), resultTwo.getFilesToDownload());

	}
}
