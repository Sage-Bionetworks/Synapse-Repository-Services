package org.sagebionetworks.repo.model.dbo.file.download;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.file.DownloadList;
import org.sagebionetworks.repo.model.file.DownloadOrder;
import org.sagebionetworks.repo.model.file.DownloadOrderSummary;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
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

	DownloadOrder downloadOrder;

	@Before
	public void before() {
		UserGroup ug = new UserGroup();
		ug.setCreationDate(new Date(System.currentTimeMillis()));
		ug.setIsIndividual(true);
		userOneIdLong = userGroupDao.create(ug);
		userOneId = "" + userOneIdLong;
		// second user
		ug = new UserGroup();
		ug.setCreationDate(new Date(System.currentTimeMillis()));
		ug.setIsIndividual(true);
		userTwoIdLong = userGroupDao.create(ug);
		userTwoId = "" + userTwoIdLong;

		fileHandles = new LinkedList<>();
		for (int i = 0; i < 4; i++) {
			S3FileHandle fileHandle = TestUtils.createS3FileHandle(userOneId, idGenerator.generateNewId((IdType.FILE_IDS)).toString());
			fileHandle.setBucketName("someBucket");
			fileHandle.setKey("key-"+i);
			fileHandle.setFileName("name-" + i);
			fileHandle = (S3FileHandle) fileHandleDao.createFile(fileHandle);
			fileHandles.add(fileHandle);
		}

		fileHandleAssociations = new LinkedList<>();
		long index = 0;
		for (FileHandle file : fileHandles) {
			FileHandleAssociation fha = new FileHandleAssociation();
			fha.setAssociateObjectId(KeyFactory.keyToString(index++));
			fha.setAssociateObjectType(FileHandleAssociateType.FileEntity);
			fha.setFileHandleId(file.getId());
			fileHandleAssociations.add(fha);
		}

		downloadOrder = new DownloadOrder();
		downloadOrder.setCreatedBy(userOneId);
		downloadOrder.setCreatedOn(new Date());
		downloadOrder.setFiles(fileHandleAssociations);
		downloadOrder.setOrderId("123");
		downloadOrder.setTotalNumberOfFiles(new Long(fileHandleAssociations.size()));
		downloadOrder.setTotalSizeBytes(8888L);
		downloadOrder.setZipFileName("SomeFileName");
	}

	@After
	public void after() {
		bulkDownlaodDao.truncateAllDownloadDataForAllUsers();
		if (userOneId != null) {
			userGroupDao.delete(userOneId);
		}
		if (userTwoId != null) {
			userGroupDao.delete(userTwoId);
		}

		if (fileHandles != null) {
			for (FileHandle file : fileHandles) {
				fileHandleDao.delete(file.getId());
			}
		}
	}

	@Test
	public void testTranslateFromDBOtoDTOFileAssociation() {
		DBODownloadListItem item = new DBODownloadListItem();
		item.setPrincipalId(userOneIdLong);
		item.setAssociatedObjectId(123L);
		item.setAssociatedObjectType(FileHandleAssociateType.TeamAttachment.name());
		item.setFileHandleId(456L);
		// call under test
		FileHandleAssociation fha = BulkDownloadDAOImpl.translateFromDBOtoDTO(item);
		assertNotNull(fha);
		assertEquals(item.getAssociatedObjectId().toString(), fha.getAssociateObjectId());
		assertEquals(item.getAssociatedObjectType(), fha.getAssociateObjectType().name());
		assertEquals("" + item.getFileHandleId(), fha.getFileHandleId());
	}
	
	@Test
	public void testTranslateFromDBOtoDTOFileAssociationFileEntity() {
		DBODownloadListItem item = new DBODownloadListItem();
		item.setPrincipalId(userOneIdLong);
		item.setAssociatedObjectId(123L);
		item.setAssociatedObjectType(FileHandleAssociateType.FileEntity.name());
		item.setFileHandleId(456L);
		// call under test
		FileHandleAssociation fha = BulkDownloadDAOImpl.translateFromDBOtoDTO(item);
		assertNotNull(fha);
		assertEquals(KeyFactory.keyToString(item.getAssociatedObjectId()), fha.getAssociateObjectId());
		assertEquals(item.getAssociatedObjectType(), fha.getAssociateObjectType().name());
		assertEquals(item.getFileHandleId().toString(), fha.getFileHandleId());
	}
	
	@Test
	public void testTranslateFromDBOtoDTOFileAssociationTable() {
		DBODownloadListItem item = new DBODownloadListItem();
		item.setPrincipalId(userOneIdLong);
		item.setAssociatedObjectId(123L);
		item.setAssociatedObjectType(FileHandleAssociateType.TableEntity.name());
		item.setFileHandleId(456L);
		// call under test
		FileHandleAssociation fha = BulkDownloadDAOImpl.translateFromDBOtoDTO(item);
		assertNotNull(fha);
		assertEquals(KeyFactory.keyToString(item.getAssociatedObjectId()), fha.getAssociateObjectId());
		assertEquals(item.getAssociatedObjectType(), fha.getAssociateObjectType().name());
		assertEquals(item.getFileHandleId().toString(), fha.getFileHandleId());
	}


	@Test
	public void testTranslateFromDBOtoDTOFileAssociationList() {
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
		assertEquals(KeyFactory.keyToString(item.getAssociatedObjectId()), fha.getAssociateObjectId());
		assertEquals(item.getAssociatedObjectType(), fha.getAssociateObjectType().name());
		assertEquals("" + item.getFileHandleId(), fha.getFileHandleId());
	}

	@Test
	public void testTranslateFromDBOtoDTOFileAssociationListNull() {
		List<DBODownloadListItem> items = null;
		// call under test
		List<FileHandleAssociation> list = BulkDownloadDAOImpl.translateFromDBOtoDTO(items);
		assertEquals(null, list);
	}

	@Test
	public void testTranslateFromDBOtoDTO() {
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
	public void testTranslateFromDBOtoDTONullItems() {
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
		assertNotNull(list.getFilesToDownload());
		assertTrue(list.getFilesToDownload().isEmpty());
		assertEquals(this.userOneId, list.getOwnerId());
	}
	
	@Test
	public void testGetUsersDownloadListFoUpdateNeverCreated() {
		// There is no data for this user yet.
		// call under test
		DownloadList list = bulkDownlaodDao.getUsersDownloadListForUpdate(userOneId);
		assertNotNull(list);
		assertNotNull(list.getUpdatedOn());
		assertNotNull(list.getEtag());
		assertNotNull(list.getFilesToDownload());
		assertTrue(list.getFilesToDownload().isEmpty());
		assertEquals(this.userOneId, list.getOwnerId());
	}
	
	@Test
	public void testGetUsersDownloadListFoUpdateWithData() {
		DownloadList start = bulkDownlaodDao.addFilesToDownloadList(userOneId, fileHandleAssociations);
		// call under test
		DownloadList list = bulkDownlaodDao.getUsersDownloadListForUpdate(userOneId);
		assertNotNull(list);
		assertEquals(start, list);
	}

	@Test(expected = IllegalArgumentException.class)
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
	
	@Test
	public void testCreateDBOFileHandleAssociationPLFM_5135() {
		FileHandleAssociation fha = new FileHandleAssociation();
		fha.setAssociateObjectId("syn123");
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

	@Test(expected = IllegalArgumentException.class)
	public void testCreateDBOFileHandleAssociationNull() {
		FileHandleAssociation fha = null;
		// call under test
		BulkDownloadDAOImpl.translateFromDTOtoDBO(userOneIdLong, fha);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateDBOFileHandleAssociationObjectIdNull() {
		FileHandleAssociation fha = new FileHandleAssociation();
		fha.setAssociateObjectId(null);
		fha.setAssociateObjectType(FileHandleAssociateType.FileEntity);
		fha.setFileHandleId("456");
		// call under test
		BulkDownloadDAOImpl.translateFromDTOtoDBO(userOneIdLong, fha);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateDBOFileHandleAssociationTypeNull() {
		FileHandleAssociation fha = new FileHandleAssociation();
		fha.setAssociateObjectId("123");
		fha.setAssociateObjectType(null);
		fha.setFileHandleId("456");
		// call under test
		BulkDownloadDAOImpl.translateFromDTOtoDBO(userOneIdLong, fha);
	}

	@Test(expected = IllegalArgumentException.class)
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

	@Test(expected = IllegalArgumentException.class)
	public void testCreateDBOFileHandleAssociationListNullList() {
		List<FileHandleAssociation> list = null;
		// call under test
		BulkDownloadDAOImpl.translateFromDTOtoDBO(userOneIdLong, list);
	}

	@Test(expected = IllegalArgumentException.class)
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
	public void testAddFilesPLFM_5135File() {
		FileHandleAssociation association = new FileHandleAssociation();
		association.setAssociateObjectId("syn123");
		association.setAssociateObjectType(FileHandleAssociateType.FileEntity);
		association.setFileHandleId("567");
		
		DownloadList result = bulkDownlaodDao.addFilesToDownloadList(userOneId, Lists.newArrayList(association));
		assertNotNull(result);
		assertNotNull(result.getFilesToDownload());
		assertEquals(1, result.getFilesToDownload().size());
		FileHandleAssociation returned = result.getFilesToDownload().get(0);
		assertEquals("syn123", returned.getAssociateObjectId());
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

	@Test(expected = IllegalArgumentException.class)
	public void testAddFilesNullUser() {
		userOneId = null;
		// call under test
		bulkDownlaodDao.addFilesToDownloadList(userOneId, fileHandleAssociations);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddFilesNullList() {
		fileHandleAssociations = null;
		// call under test
		bulkDownlaodDao.addFilesToDownloadList(userOneId, fileHandleAssociations);
	}

	@Test
	public void testAddFilesEmptyList() {
		// Add all of the files to this users
		DownloadList start = bulkDownlaodDao.addFilesToDownloadList(userOneId, fileHandleAssociations);

		List<FileHandleAssociation> toAdd = new LinkedList<>();
		// call under test
		DownloadList result = bulkDownlaodDao.addFilesToDownloadList(userOneId, toAdd);
		assertNotNull(result);
		assertEquals(userOneId, result.getOwnerId());
		// etag should change even though no rows were added.
		assertFalse(start.getEtag().equals(result.getEtag()));
		assertNotNull(result.getUpdatedOn());
		assertEquals(start.getFilesToDownload(), result.getFilesToDownload());
	}

	@Test
	public void testCreateDeleteSQL() {
		// call under test
		String sql = BulkDownloadDAOImpl.createDeleteSQL(2);
		assertEquals("DELETE FROM DOWNLOAD_LIST_ITEM "
				+ "WHERE (PRINCIPAL_ID,ASSOCIATED_OBJECT_ID,ASSOCIATED_OBJECT_TYPE,FILE_HANDLE_ID)"
				+ " IN ((?,?,?,?),(?,?,?,?)) LIMIT 2", sql);
	}

	@Test
	public void testRemoveFilesFromDownloadList() throws InterruptedException {
		// Add all of the files to this users
		DownloadList start = bulkDownlaodDao.addFilesToDownloadList(userOneId, fileHandleAssociations);
		// remove first and last
		List<FileHandleAssociation> toRemove = Lists.newArrayList(fileHandleAssociations.get(0),
				fileHandleAssociations.get(3));
		DownloadList result = bulkDownlaodDao.removeFilesFromDownloadList(userOneId, toRemove);
		assertNotNull(result);
		// validate the etag and updated are changed
		assertNotNull(result.getEtag());
		assertNotNull(result.getUpdatedOn());
		assertFalse(start.getEtag().equals(result.getEtag()));
		assertEquals(fileHandleAssociations.subList(1, 3), result.getFilesToDownload());
	}
	
	@Test
	public void testRemoveFilesPLFM_5135() {
		FileHandleAssociation association = new FileHandleAssociation();
		association.setAssociateObjectId("syn123");
		association.setAssociateObjectType(FileHandleAssociateType.FileEntity);
		association.setFileHandleId("567");
		
		DownloadList result = bulkDownlaodDao.removeFilesFromDownloadList(userOneId, Lists.newArrayList(association));
		assertNotNull(result);
		assertNotNull(result.getFilesToDownload());
		assertEquals(0, result.getFilesToDownload().size());
	}

	@Test
	public void testRemoveFilesFromDownloadListMulitpleUsers() {
		// 0-2 added to user one
		List<FileHandleAssociation> oneFiles = fileHandleAssociations.subList(0, 2);
		bulkDownlaodDao.addFilesToDownloadList(userOneId, oneFiles);
		// 1-3 added to user two
		List<FileHandleAssociation> twoFiles = fileHandleAssociations.subList(1, 3);
		bulkDownlaodDao.addFilesToDownloadList(userTwoId, twoFiles);

		// remove the second set of files from user one.
		// call under test
		DownloadList results = bulkDownlaodDao.removeFilesFromDownloadList(userOneId, twoFiles);
		assertNotNull(results);
		// only the first file should remain on user one.
		assertEquals(fileHandleAssociations.subList(0, 1), results.getFilesToDownload());
		// user two's data should be unchanged
		DownloadList userTwosList = bulkDownlaodDao.getUsersDownloadList(userTwoId);
		assertEquals(twoFiles, userTwosList.getFilesToDownload());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRemoveFilesFromDownloadListUserNull() {
		userOneId = null;
		// call under test
		bulkDownlaodDao.removeFilesFromDownloadList(userOneId, fileHandleAssociations);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testRemoveFilesFromDownloadListUserListNull() {
		fileHandleAssociations = null;
		// call under test
		bulkDownlaodDao.removeFilesFromDownloadList(userOneId, fileHandleAssociations);
	}

	@Test
	public void testRemoveFilesFromDownloadListEmpty() throws InterruptedException {
		// Add all of the files to this users
		DownloadList start = bulkDownlaodDao.addFilesToDownloadList(userOneId, fileHandleAssociations);
		// remove empty list.
		List<FileHandleAssociation> toRemove = new LinkedList<>();
		// call under test
		DownloadList result = bulkDownlaodDao.removeFilesFromDownloadList(userOneId, toRemove);
		assertNotNull(result);
		// validate the etag and updated are changed
		assertNotNull(result.getEtag());
		assertNotNull(result.getUpdatedOn());
		assertFalse(start.getEtag().equals(result.getEtag()));
		assertEquals(fileHandleAssociations, result.getFilesToDownload());
	}

	@Test
	public void testClearDownloadList() throws InterruptedException {
		// add all of the files to a user's list
		DownloadList start = bulkDownlaodDao.addFilesToDownloadList(userOneId, fileHandleAssociations);
		// call under test
		DownloadList result = bulkDownlaodDao.clearDownloadList(userOneId);
		assertNotNull(result);
		assertNotNull(result.getEtag());
		assertNotNull(result.getUpdatedOn());
		assertFalse(start.getEtag().equals(result.getEtag()));
		assertEquals(new LinkedList<>(), result.getFilesToDownload());
	}

	@Test
	public void testClearDownloadListMultipleUsers() throws InterruptedException {
		// add all files to both users
		bulkDownlaodDao.addFilesToDownloadList(userOneId, fileHandleAssociations);
		bulkDownlaodDao.addFilesToDownloadList(userTwoId, fileHandleAssociations);
		// call under test
		DownloadList result = bulkDownlaodDao.clearDownloadList(userOneId);
		assertNotNull(result);
		assertEquals(new LinkedList<>(), result.getFilesToDownload());
		// user two's list should remain unchanged
		DownloadList userTwosList = bulkDownlaodDao.getUsersDownloadList(userTwoId);
		assertEquals(fileHandleAssociations, userTwosList.getFilesToDownload());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testClearDownloadListNull() throws InterruptedException {
		// call under test
		bulkDownlaodDao.clearDownloadList(null);
	}

	@Test
	public void testGetDownloadListFileCount() {
		// call under test
		long count = bulkDownlaodDao.getDownloadListFileCount(userOneId);
		assertEquals(0L, count);
		// add files to both users.
		bulkDownlaodDao.addFilesToDownloadList(userOneId, fileHandleAssociations.subList(0, 2));
		bulkDownlaodDao.addFilesToDownloadList(userTwoId, fileHandleAssociations);
		// call under test
		assertEquals(2, bulkDownlaodDao.getDownloadListFileCount(userOneId));
		// call under test
		assertEquals(fileHandleAssociations.size(), bulkDownlaodDao.getDownloadListFileCount(userTwoId));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetDownloadListFileCountNull() {
		// call under test
		bulkDownlaodDao.getDownloadListFileCount(null);
	}

	@Test
	public void testTranslateFilesRoundTrip() {
		List<FileHandleAssociation> files = fileHandleAssociations;
		// call under test
		byte[] bytes = BulkDownloadDAOImpl.translateFilesToBytes(files);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
		// call under test
		List<FileHandleAssociation> clone = BulkDownloadDAOImpl.translateBytesToFiles(bytes);
		assertEquals(fileHandleAssociations, clone);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTranslateFilesNullList() {
		List<FileHandleAssociation> files = null;
		// call under test
		BulkDownloadDAOImpl.translateFilesToBytes(files);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTranslateFilesEmptyList() {
		List<FileHandleAssociation> files = new LinkedList<>();
		// call under test
		BulkDownloadDAOImpl.translateFilesToBytes(files);
	}

	@Test
	public void testTranslateDownloadOrderRoundTrip() {
		DownloadOrder dto = downloadOrder;
		// call under test
		DBODownloadOrder dbo = BulkDownloadDAOImpl.translateFromDTOtoDBO(dto);
		// call under test
		DownloadOrder clone = BulkDownloadDAOImpl.translateFromDBOtoDTO(dbo);
		assertEquals(downloadOrder, clone);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTranslateFromDTOtoDBONull() {
		DownloadOrder dto = null;
		// call under test
		BulkDownloadDAOImpl.translateFromDTOtoDBO(dto);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTranslateFromDTOtoDBONullCreatedBy() {
		DownloadOrder dto = downloadOrder;
		dto.setCreatedBy(null);
		// call under test
		BulkDownloadDAOImpl.translateFromDTOtoDBO(dto);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTranslateFromDTOtoDBONullCreatedOn() {
		DownloadOrder dto = downloadOrder;
		dto.setCreatedOn(null);
		// call under test
		BulkDownloadDAOImpl.translateFromDTOtoDBO(dto);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTranslateFromDTOtoDBONullFile() {
		DownloadOrder dto = downloadOrder;
		dto.setFiles(null);
		// call under test
		BulkDownloadDAOImpl.translateFromDTOtoDBO(dto);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTranslateFromDTOtoDBONullOrderId() {
		DownloadOrder dto = downloadOrder;
		dto.setOrderId(null);
		// call under test
		BulkDownloadDAOImpl.translateFromDTOtoDBO(dto);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTranslateFromDTOtoDBONullTotalNumberFiles() {
		DownloadOrder dto = downloadOrder;
		dto.setTotalNumberOfFiles(null);
		// call under test
		BulkDownloadDAOImpl.translateFromDTOtoDBO(dto);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTranslateFromDTOtoDBONullTotalSize() {
		DownloadOrder dto = downloadOrder;
		dto.setTotalSizeBytes(null);
		// call under test
		BulkDownloadDAOImpl.translateFromDTOtoDBO(dto);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTranslateFromDTOtoDBONullFileName() {
		DownloadOrder dto = downloadOrder;
		dto.setZipFileName(null);
		// call under test
		BulkDownloadDAOImpl.translateFromDTOtoDBO(dto);
	}

	@Test
	public void testTranslateFromDTOtoDBOFileNameMax() {
		DownloadOrder dto = downloadOrder;
		dto.setZipFileName(createStringOfSize(BulkDownloadDAOImpl.MAX_NAME_CHARS));
		// call under test
		DBODownloadOrder result = BulkDownloadDAOImpl.translateFromDTOtoDBO(dto);
		assertNotNull(result);
		assertEquals(dto.getZipFileName(), result.getZipFileName());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTranslateFromDTOtoDBOFileNameOverMax() {
		DownloadOrder dto = downloadOrder;
		dto.setZipFileName(createStringOfSize(BulkDownloadDAOImpl.MAX_NAME_CHARS + 1));
		// call under test
		DBODownloadOrder result = BulkDownloadDAOImpl.translateFromDTOtoDBO(dto);
		assertNotNull(result);
		assertEquals(dto.getZipFileName(), result.getZipFileName());
	}

	@Test
	public void testCreateAndGetDownloadOrder() {
		// use largest possible name.
		downloadOrder.setZipFileName(createStringOfSize(BulkDownloadDAOImpl.MAX_NAME_CHARS));
		// call under test
		DownloadOrder created = this.bulkDownlaodDao.createDownloadOrder(downloadOrder);
		assertNotNull(created);
		assertNotNull(created.getOrderId());
		assertEquals(downloadOrder.getCreatedBy(), created.getCreatedBy());
		assertEquals(downloadOrder.getCreatedOn(), created.getCreatedOn());
		assertEquals(downloadOrder.getFiles(), created.getFiles());
		assertEquals(downloadOrder.getTotalNumberOfFiles(), created.getTotalNumberOfFiles());
		assertEquals(downloadOrder.getTotalSizeBytes(), created.getTotalSizeBytes());
		assertEquals(downloadOrder.getZipFileName(), created.getZipFileName());

		// Fetch the same order again
		// call under test
		DownloadOrder fetched = this.bulkDownlaodDao.getDownloadOrder(created.getOrderId());
		assertEquals(created, fetched);
	}

	@Test
	public void testGetUsersDownloadOrders() {
		DownloadOrder order = createDownloadOrder(userOneId, "one", fileHandleAssociations);
		long limit = Long.MAX_VALUE;
		long offset = 0L;
		// call under test
		List<DownloadOrderSummary> summaries = this.bulkDownlaodDao.getUsersDownloadOrders(userOneId, limit, offset);
		assertNotNull(summaries);
		assertEquals(1, summaries.size());
		DownloadOrderSummary summary = summaries.get(0);
		assertEquals(order.getOrderId(), summary.getOrderId());
		assertEquals(order.getCreatedBy(), summary.getCreatedBy());
		assertEquals(order.getCreatedOn(), summary.getCreatedOn());
		assertEquals(order.getTotalNumberOfFiles(), summary.getTotalNumberOfFiles());
		assertEquals(order.getTotalSizeBytes(), summary.getTotalSizeBytes());
		assertEquals(order.getZipFileName(), summary.getZipFileName());
	}

	@Test
	public void testGetUsersDownloadOrdersMultiples() throws InterruptedException {
		// one
		List<DownloadOrder> onesOrders = new LinkedList<>();
		for (int i = 0; i < 4; i++) {
			Thread.sleep(10L);
			DownloadOrder order = createDownloadOrder(userOneId, "one" + i, fileHandleAssociations);
			onesOrders.add(order);
		}
		// two
		List<DownloadOrder> twosOrders = new LinkedList<>();
		for (int i = 0; i < 4; i++) {
			Thread.sleep(10L);
			DownloadOrder order = createDownloadOrder(userTwoId, "two" + i, fileHandleAssociations);
			twosOrders.add(order);
		}

		long limit = 2;
		long offset = 0L;
		// call under test
		List<DownloadOrderSummary> summaries = this.bulkDownlaodDao.getUsersDownloadOrders(userOneId, limit, offset);
		assertNotNull(summaries);
		assertEquals(2, summaries.size());
		// last added should be the first listed
		assertEquals(onesOrders.get(3).getOrderId(), summaries.get(0).getOrderId());
		assertEquals(onesOrders.get(2).getOrderId(), summaries.get(1).getOrderId());

		offset = 1;
		// call under test
		summaries = this.bulkDownlaodDao.getUsersDownloadOrders(userTwoId, limit, offset);
		assertNotNull(summaries);
		assertEquals(2, summaries.size());
		// first should be skipped.
		assertEquals(twosOrders.get(2).getOrderId(), summaries.get(0).getOrderId());
		assertEquals(twosOrders.get(1).getOrderId(), summaries.get(1).getOrderId());
	}

	/**
	 * Helper to create a quick download order.
	 * 
	 * @param userId
	 * @param name
	 * @param toOrder
	 * @return
	 */
	DownloadOrder createDownloadOrder(String userId, String name, List<FileHandleAssociation> toOrder) {
		DownloadOrder order = new DownloadOrder();
		order.setCreatedBy(userId);
		order.setCreatedOn(new Date());
		order.setFiles(toOrder);
		order.setTotalNumberOfFiles(new Long(toOrder.size()));
		order.setTotalSizeBytes(order.getTotalNumberOfFiles() + 10L);
		order.setZipFileName(name);
		return this.bulkDownlaodDao.createDownloadOrder(order);
	}

	/**
	 * Helper to create a test string of the given size.
	 * 
	 * @param size
	 * @return
	 */
	static String createStringOfSize(int size) {
		char[] chars = new char[size];
		for (int i = 0; i < chars.length; i++) {
			chars[i] = 'a';
		}
		return new String(chars);
	}
}
