package org.sagebionetworks.repo.model.dbo.dao.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableDAO;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class MigratableTableDAOImplAutowireTest {

	@Autowired
	private UserProfileDAO userProfileDAO;
	
	@Autowired
	private FileHandleDao fileHandleDao;
	
	@Autowired
	private MigratableTableDAO migratableTableDAO;

	@Autowired
	private IdGenerator idGenerator;
	
	private List<String> filesToDelete;
	
	private String creatorUserGroupId;
	
	@Before
	public void before(){
		filesToDelete = new LinkedList<String>();
		creatorUserGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();
		assertNotNull(creatorUserGroupId);
	}
	
	@After
	public void after(){
		if(fileHandleDao != null && filesToDelete != null){
			for(String id: filesToDelete){
				fileHandleDao.delete(id);
			}
		}
	}
	
	@Test
	public void testMigrationRoundTrip() throws Exception {
		long startCount = fileHandleDao.getCount();
		long migrationCount = migratableTableDAO.getCount(MigrationType.FILE_HANDLE);
		assertEquals(startCount, migrationCount);
		long startMax = fileHandleDao.getMaxId();
		// The one will have a preview
		S3FileHandle withPreview = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		withPreview.setFileName("withPreview.txt");
		S3FileHandle withPreview2 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		withPreview2.setFileName("withPreview2.txt");
		// The Preview
		PreviewFileHandle preview = TestUtils.createPreviewFileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		preview.setFileName("preview.txt");
		// Preview 2
		PreviewFileHandle preview2 = TestUtils.createPreviewFileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		preview2.setFileName("preview.txt");

		List<FileHandle> fileHandleToCreate = new LinkedList<FileHandle>();
		fileHandleToCreate.add(withPreview);
		fileHandleToCreate.add(withPreview2);
		fileHandleToCreate.add(preview);
		fileHandleToCreate.add(preview2);
		fileHandleDao.createBatch(fileHandleToCreate);
		
		withPreview = (S3FileHandle) fileHandleDao.get(withPreview.getId());
		assertNotNull(withPreview);
		withPreview2 = (S3FileHandle) fileHandleDao.get(withPreview2.getId());
		assertNotNull(withPreview2);
		preview = (PreviewFileHandle) fileHandleDao.get(preview.getId());
		assertNotNull(preview);
		preview2 = (PreviewFileHandle) fileHandleDao.get(preview2.getId());
		assertNotNull(preview2);
		filesToDelete.add(withPreview.getId());
		filesToDelete.add(withPreview2.getId());
		filesToDelete.add(preview.getId());
		filesToDelete.add(preview2.getId());
		
		assertEquals(Long.parseLong(preview2.getId()), fileHandleDao.getMaxId());
		
		// Assign it as a preview
		fileHandleDao.setPreviewId(withPreview.getId(), preview.getId());
		fileHandleDao.setPreviewId(withPreview2.getId(), preview2.getId());
		// The etag should have changed
		withPreview = (S3FileHandle) fileHandleDao.get(withPreview.getId());
		withPreview2 = (S3FileHandle) fileHandleDao.get(withPreview2.getId());
		
		// Now list all of the objects
		RowMetadataResult totalList = migratableTableDAO.listRowMetadata(MigrationType.FILE_HANDLE, 1000, startCount);
		assertNotNull(totalList);
		assertEquals(new Long(startCount+4),  totalList.getTotalCount());
		assertNotNull(totalList.getList());
		assertEquals(4, totalList.getList().size());
		System.out.println(totalList.getList());
		
		// The withPreview should be first.
		RowMetadata row = totalList.getList().get(0);
		assertEquals(withPreview.getId(), ""+row.getId());
		assertEquals(withPreview.getEtag(), row.getEtag());
		assertEquals(preview.getId(), ""+row.getParentId());
		// 2
		row = totalList.getList().get(1);
		assertEquals(withPreview2.getId(), ""+row.getId());
		assertEquals(withPreview2.getEtag(), row.getEtag());
		assertEquals(preview2.getId(), ""+row.getParentId());
		// previews
		row = totalList.getList().get(2);
		assertEquals(preview.getId(), ""+row.getId());
		assertEquals(preview.getEtag(), row.getEtag());
		assertEquals(null, row.getParentId());
		// 2
		row = totalList.getList().get(3);
		assertEquals(preview2.getId(), ""+row.getId());
		assertEquals(preview2.getEtag(), row.getEtag());
		assertEquals(null, row.getParentId());
		
		// Get the full back object
		List<Long> idsToBackup1 = new LinkedList<Long>();
		idsToBackup1.add(Long.parseLong(preview.getId()));
		idsToBackup1.add(Long.parseLong(preview2.getId()));
		List<DBOFileHandle> backupList1 = migratableTableDAO.getBackupBatch(DBOFileHandle.class, idsToBackup1);
		assertNotNull(backupList1);
		assertEquals(2, backupList1.size());
		//with preview.
		DBOFileHandle dbfh = backupList1.get(0);
		assertEquals(preview.getId(), ""+dbfh.getId());
		// preview.
		dbfh = backupList1.get(1);
		assertEquals(preview2.getId(), ""+dbfh.getId());
		
		// Second backup
		List<Long> idsToBackup2 = new LinkedList<Long>();
		idsToBackup2.add(Long.parseLong(withPreview.getId()));
		idsToBackup2.add(Long.parseLong(withPreview2.getId()));
		List<DBOFileHandle> backupList2 = migratableTableDAO.getBackupBatch(DBOFileHandle.class, idsToBackup2);
		assertNotNull(backupList2);
		assertEquals(2, backupList2.size());
		// withPreview.
		dbfh = backupList2.get(0);
		assertEquals(withPreview.getId(), ""+dbfh.getId());
		// withPreview2.
		dbfh = backupList2.get(1);
		assertEquals(withPreview2.getId(), ""+dbfh.getId());
		
		// Now delete all of the data
		int count = migratableTableDAO.deleteObjectsById(MigrationType.FILE_HANDLE, idsToBackup1);
		assertEquals(2, count);
		count = migratableTableDAO.deleteObjectsById(MigrationType.FILE_HANDLE, idsToBackup2);
		assertEquals(2, count);
		assertEquals(startCount, migratableTableDAO.getCount(MigrationType.FILE_HANDLE));
		assertEquals(startMax, migratableTableDAO.getMaxId(MigrationType.FILE_HANDLE));
		
		// Now restore the data
		List<Long> results = migratableTableDAO.createOrUpdateBatch(backupList1);
		assertNotNull(results);
		assertEquals(idsToBackup1, results);
		results = migratableTableDAO.createOrUpdateBatch(backupList2);
		assertNotNull(results);
		assertEquals(idsToBackup2, results);
		// Now make sure if we update again it works
		backupList1.get(0).setBucketName("updateBucketName");
		results = migratableTableDAO.createOrUpdateBatch(backupList1);
		assertNotNull(results);
		assertEquals(idsToBackup1, results);
	}
	
	@Test
	public void testPLFM_1978_listDeltaRowMetadata(){
		// For PLFM-1978, calling listDeltaRowMetadata() with an empty list causes a BadSqlGrammarException
		// because the resulting 'in' clause is empty.
		List<Long> list = new LinkedList<Long>();
		// pass the empty list should return an empty result
		List<RowMetadata> results = migratableTableDAO.listDeltaRowMetadata(MigrationType.ACCESS_APPROVAL, list);
		assertNotNull(results);
		assertEquals(0, results.size());
	}
	
	@Test
	public void testPLFM_1978_deleteObjectsById(){
		// For PLFM-1978, calling deleteObjectsById() with an empty list causes a BadSqlGrammarException
		// because the resulting 'in' clause is empty.
		List<Long> list = new LinkedList<Long>();
		// pass the empty list should return an empty result
		int result = migratableTableDAO.deleteObjectsById(MigrationType.ACCESS_APPROVAL, list);
		assertEquals(0, result);
	}
	
	@Test
	public void testPLFM_1978_getBackupBatch(){
		// For PLFM-1978, calling getBackupBatch() with an empty list causes a BadSqlGrammarException
		// because the resulting 'in' clause is empty.
		List<Long> list = new LinkedList<Long>();
		// pass the empty list should return an empty result
		List<DBOFileHandle> results = migratableTableDAO.getBackupBatch(DBOFileHandle.class, list);
		assertNotNull(results);
		assertEquals(0, results.size());
	}
	
	@Test
	public void testPLFM_1978_createOrUpdateBatch(){
		// For PLFM-1978, calling createOrUpdateBatch() with an empty list causes a BadSqlGrammarException
		// because the resulting 'in' clause is empty.
		List<DBOFileHandle> list = new LinkedList<DBOFileHandle>();
		// pass the empty list should return an empty result
		List<Long> results = migratableTableDAO.createOrUpdateBatch(list);
		assertNotNull(results);
		assertEquals(0, results.size());
	}
	
	@Test
	public void testRunWithForeignKeyIgnored() throws Exception{
		final S3FileHandle fh = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		fh.setFileName("withPreview.txt");
		// This does not exists but we should be able to set while foreign keys are ignored.
		fh.setPreviewId("-123");
		// This should fail
		try{
			fileHandleDao.createFile(fh);
			fail("A foreign key should have prevented this change.");
		}catch(Exception e){
			// expected
		}
		// While the check is off we can violate foreign keys.
		Boolean result = migratableTableDAO.runWithForeignKeyIgnored(new Callable<Boolean>(){
			@Override
			public Boolean call() throws Exception {
				// We should be able to do this now that foreign keys are disabled.
				S3FileHandle updated = (S3FileHandle) fileHandleDao.createFile(fh);
				filesToDelete.add(updated.getId());
				return true;
			}});
		assertTrue(result);
		
		// This should fail if constraints are back on.
		final S3FileHandle fh2 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		fh2.setFileName("withPreview2.txt");
		// This does not exists but we should be able to set while foreign keys are ignored.
		fh2.setPreviewId("-123");
		// This should fail
		try{
			fileHandleDao.createFile(fh2);
			fail("A foreign key should have prevented this change.");
		}catch(Exception e){
			// expected
		}
	}
	

	/**
	 * This test exists to ensure only Primary types are listed.  This test will break each type a new 
	 * primary type is added, but that ensures we check that the types are truly primary.
	 * Migration will break if secondary types are added to this list.
	 */
	@Test
	public void testGetPrimaryMigrationTypes(){
		// Only primary migration types should be returned.
		List<MigrationType> expectedPrimaryTypes = new LinkedList<MigrationType>();
		expectedPrimaryTypes.add(MigrationType.PRINCIPAL);
		expectedPrimaryTypes.add(MigrationType.PRINCIPAL_ALIAS);
		expectedPrimaryTypes.add(MigrationType.NOTIFICATION_EMAIL);
		expectedPrimaryTypes.add(MigrationType.USER_PROFILE);
		expectedPrimaryTypes.add(MigrationType.STORAGE_LOCATION);
		expectedPrimaryTypes.add(MigrationType.FILE_HANDLE);
		expectedPrimaryTypes.add(MigrationType.MULTIPART_UPLOAD);
		expectedPrimaryTypes.add(MigrationType.MESSAGE_CONTENT);
		expectedPrimaryTypes.add(MigrationType.V2_WIKI_PAGE);
		expectedPrimaryTypes.add(MigrationType.V2_WIKI_OWNERS);
		expectedPrimaryTypes.add(MigrationType.ACTIVITY);
		expectedPrimaryTypes.add(MigrationType.NODE);
		expectedPrimaryTypes.add(MigrationType.DOCKER_REPOSITORY_NAME);
		expectedPrimaryTypes.add(MigrationType.DOCKER_COMMIT);
		expectedPrimaryTypes.add(MigrationType.TEAM);
		expectedPrimaryTypes.add(MigrationType.MEMBERSHIP_INVITATION_SUBMISSION);
		expectedPrimaryTypes.add(MigrationType.MEMBERSHIP_REQUEST_SUBMISSION);
		expectedPrimaryTypes.add(MigrationType.EVALUATION);
		expectedPrimaryTypes.add(MigrationType.EVALUATION_SUBMISSIONS);
		expectedPrimaryTypes.add(MigrationType.SUBMISSION);
		expectedPrimaryTypes.add(MigrationType.SUBMISSION_CONTRIBUTOR);
		expectedPrimaryTypes.add(MigrationType.SUBMISSION_STATUS);
		expectedPrimaryTypes.add(MigrationType.PROJECT_SETTINGS);
		expectedPrimaryTypes.add(MigrationType.PROJECT_STATS);
		expectedPrimaryTypes.add(MigrationType.ACCESS_REQUIREMENT);
		expectedPrimaryTypes.add(MigrationType.ACCESS_APPROVAL);
		expectedPrimaryTypes.add(MigrationType.ACL);
		expectedPrimaryTypes.add(MigrationType.FAVORITE);
		expectedPrimaryTypes.add(MigrationType.TRASH_CAN);
		expectedPrimaryTypes.add(MigrationType.DOI);
		expectedPrimaryTypes.add(MigrationType.CHALLENGE);
		expectedPrimaryTypes.add(MigrationType.CHALLENGE_TEAM);
		expectedPrimaryTypes.add(MigrationType.COLUMN_MODEL);
		expectedPrimaryTypes.add(MigrationType.BOUND_COLUMN_OWNER);
		expectedPrimaryTypes.add(MigrationType.TABLE_SEQUENCE);
		expectedPrimaryTypes.add(MigrationType.QUIZ_RESPONSE);
		expectedPrimaryTypes.add(MigrationType.VERIFICATION_SUBMISSION);
		expectedPrimaryTypes.add(MigrationType.VERIFICATION_STATE);
		expectedPrimaryTypes.add(MigrationType.FORUM);
		expectedPrimaryTypes.add(MigrationType.DISCUSSION_THREAD);
		expectedPrimaryTypes.add(MigrationType.DISCUSSION_REPLY);
		expectedPrimaryTypes.add(MigrationType.SUBSCRIPTION);
		expectedPrimaryTypes.add(MigrationType.BROADCAST_MESSAGE);
		expectedPrimaryTypes.add(MigrationType.VIEW_TYPE);
		expectedPrimaryTypes.add(MigrationType.AUTHENTICATION_RECEIPT);
		expectedPrimaryTypes.add(MigrationType.THROTTLE_RULE);
		expectedPrimaryTypes.add(MigrationType.RESEARCH_PROJECT);
		expectedPrimaryTypes.add(MigrationType.DATA_ACCESS_REQUEST);
		expectedPrimaryTypes.add(MigrationType.DATA_ACCESS_SUBMISSION);
		expectedPrimaryTypes.add(MigrationType.DATA_ACCESS_SUBMISSION_ACCESSOR);
		expectedPrimaryTypes.add(MigrationType.CHANGE);
		// Get the list
		List<MigrationType> primary = migratableTableDAO.getPrimaryMigrationTypes();
		System.out.println(primary);
		assertEquals(expectedPrimaryTypes, primary);
	}
	
	@Deprecated
	@Test
	public void testGetMigrationTypeCount() throws Exception {
		long startCount = fileHandleDao.getCount();
		assertEquals(startCount, migratableTableDAO.getCount(MigrationType.FILE_HANDLE));
		S3FileHandle handle = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handle.setFileName("handle");
		handle = (S3FileHandle) fileHandleDao.createFile(handle);
		filesToDelete.add(handle.getId());
		assertEquals(startCount+1, migratableTableDAO.getCount(MigrationType.FILE_HANDLE));
		fileHandleDao.delete(handle.getId());
		assertEquals(startCount, migratableTableDAO.getCount(MigrationType.FILE_HANDLE));
	}
	
	@Test
	public void testGetMigrationTypeCountForType() {
		long startCount = fileHandleDao.getCount();
		assertEquals(startCount, migratableTableDAO.getMigrationTypeCount(MigrationType.FILE_HANDLE).getCount().longValue());
		S3FileHandle handle = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handle.setFileName("handle");
		handle = (S3FileHandle) fileHandleDao.createFile(handle);
		filesToDelete.add(handle.getId());
		assertEquals(startCount+1, migratableTableDAO.getMigrationTypeCount(MigrationType.FILE_HANDLE).getCount().longValue());
		fileHandleDao.delete(handle.getId());
		assertEquals(startCount, migratableTableDAO.getMigrationTypeCount(MigrationType.FILE_HANDLE).getCount().longValue());
	}
	
	@Test
	public void testGetMigrationTypeCountForTypeNoData() {
		MigrationTypeCount mtc = migratableTableDAO.getMigrationTypeCount(MigrationType.VERIFICATION_SUBMISSION);
		assertNotNull(mtc);
		assertNotNull(mtc.getCount());
		assertEquals(0L, mtc.getCount().longValue());
		assertNull(mtc.getMaxid());
		assertNull(mtc.getMinid());
		assertNotNull(mtc.getType());
		assertEquals(MigrationType.VERIFICATION_SUBMISSION, mtc.getType());
	}
	
	@Test
	public void testGetListRowMetadataByRangeOneBatch() throws Exception {
		long startId = fileHandleDao.getMaxId() + 1;
		long startCount = fileHandleDao.getCount();
		RowMetadataResult l = migratableTableDAO.listRowMetadataByRange(MigrationType.FILE_HANDLE, startId, startId+1, 10, 0);
		assertNotNull(l);
		assertEquals(startCount, l.getTotalCount().longValue());
		assertNotNull(l.getList());
		assertEquals(0, l.getList().size());
		// Add a file handle
		S3FileHandle handle1 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handle1.setFileName("handle1");
		handle1 = (S3FileHandle) fileHandleDao.createFile(handle1);
		filesToDelete.add(handle1.getId());
		// Test
		l = migratableTableDAO.listRowMetadataByRange(MigrationType.FILE_HANDLE, startId, Long.parseLong(handle1.getId())-1, 10, 0);
		assertNotNull(l);
		assertEquals(startCount+1, l.getTotalCount().longValue());
		assertNotNull(l.getList());
		assertEquals(0, l.getList().size());
		l = migratableTableDAO.listRowMetadataByRange(MigrationType.FILE_HANDLE, startId, Long.parseLong(handle1.getId()), 10, 0);
		assertNotNull(l);
		assertEquals(startCount+1, l.getTotalCount().longValue());
		assertNotNull(l.getList());
		assertEquals(1, l.getList().size());
		assertEquals(handle1.getId(), l.getList().get(0).getId().toString());
		l = migratableTableDAO.listRowMetadataByRange(MigrationType.FILE_HANDLE, startId, Long.parseLong(handle1.getId())+1, 10, 0);
		assertNotNull(l);
		assertEquals(startCount+1, l.getTotalCount().longValue());
		assertNotNull(l.getList());
		assertEquals(1, l.getList().size());
		assertEquals(handle1.getId(), l.getList().get(0).getId().toString());
		// Add a preview
		PreviewFileHandle previewHandle1 = TestUtils.createPreviewFileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		previewHandle1.setFileName("preview1");
		previewHandle1 = (PreviewFileHandle) fileHandleDao.createFile(previewHandle1);
		filesToDelete.add(previewHandle1.getId());
		// Test
		l = migratableTableDAO.listRowMetadataByRange(MigrationType.FILE_HANDLE, startId, Long.parseLong(previewHandle1.getId()), 10, 0);
		assertNotNull(l);
		assertEquals(startCount+2, l.getTotalCount().longValue());
		assertNotNull(l.getList());
		assertEquals(2, l.getList().size());
		assertEquals(handle1.getId(), l.getList().get(0).getId().toString());
		assertEquals(previewHandle1.getId(), l.getList().get(1).getId().toString());
		// Delete preview
		fileHandleDao.delete(previewHandle1.getId());
		// Test
		l = migratableTableDAO.listRowMetadataByRange(MigrationType.FILE_HANDLE, startId, Long.parseLong(previewHandle1.getId()), 10, 0);
		assertNotNull(l);
		assertEquals(startCount+1, l.getTotalCount().longValue());
		assertNotNull(l.getList());
		assertEquals(1, l.getList().size());
		assertEquals(handle1.getId(), l.getList().get(0).getId().toString());
		
	}
	
	@Test
	public void testGetListRowMetadataByRangeMultipleBatches() {
		long minId = fileHandleDao.getMaxId()+1;
		long startCount = fileHandleDao.getCount();
		List<Long> ids = new LinkedList<Long>();
		// Create 5 file handles
		for (int i = 1; i < 6; i++) {
			S3FileHandle handle = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
			handle.setFileName("handle"+i);
			handle = (S3FileHandle) fileHandleDao.createFile(handle);
			filesToDelete.add(handle.getId());
			ids.add(Long.parseLong(handle.getId()));
		}
		// First batch
		RowMetadataResult b = migratableTableDAO.listRowMetadataByRange(MigrationType.FILE_HANDLE, minId, ids.get(4)+1, 2, 0);
		assertNotNull(b);
		assertEquals(startCount+5L, b.getTotalCount().longValue());
		assertNotNull(b.getList());
		assertEquals(2, b.getList().size());
		assertEquals(ids.get(0), b.getList().get(0).getId());
		assertEquals(ids.get(1), b.getList().get(1).getId());
		// Second batch
		b = migratableTableDAO.listRowMetadataByRange(MigrationType.FILE_HANDLE, minId, ids.get(4)+1, 2, 2);
		assertNotNull(b);
		assertEquals(startCount+5L, b.getTotalCount().longValue());
		assertNotNull(b.getList());
		assertEquals(2, b.getList().size());
		assertEquals(ids.get(2), b.getList().get(0).getId());
		assertEquals(ids.get(3), b.getList().get(1).getId());
		// Last batch
		b = migratableTableDAO.listRowMetadataByRange(MigrationType.FILE_HANDLE, minId, ids.get(4)+1, 2, 4);
		assertNotNull(b);
		assertEquals(startCount+5L, b.getTotalCount().longValue());
		assertNotNull(b.getList());
		assertEquals(1, b.getList().size());
		assertEquals(ids.get(4), b.getList().get(0).getId());
		// Beyond
		b = migratableTableDAO.listRowMetadataByRange(MigrationType.FILE_HANDLE, minId, ids.get(4)+1, 2, 5);
		assertNotNull(b);
		assertEquals(startCount+5L, b.getTotalCount().longValue());
		assertNotNull(b.getList());
		assertEquals(0, b.getList().size());
	}
	
	@Test
	public void testGetChecksumForType() throws Exception {
		// Start checksum
		String checksum1 = migratableTableDAO.getChecksumForType(MigrationType.FILE_HANDLE);
		// Add file handle
		S3FileHandle handle1 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handle1.setFileName("handle1");
		handle1 = (S3FileHandle) fileHandleDao.createFile(handle1);
		filesToDelete.add(handle1.getId());
		// Checksum again
		String checksum2 = migratableTableDAO.getChecksumForType(MigrationType.FILE_HANDLE);
		// Test
		assertFalse(checksum1.equals(checksum2));
		// Delete file handle
		fileHandleDao.delete(handle1.getId());
		checksum2 = migratableTableDAO.getChecksumForType(MigrationType.FILE_HANDLE);
		// Test
		assertEquals(checksum1, checksum2);
	}
	
	@Test
	public void testGetChecksumForIdRange1() throws Exception {
		long startId = fileHandleDao.getMaxId() + 1;
		long startCount = fileHandleDao.getCount();

		// Add file handle
		S3FileHandle handle1 = TestUtils.createS3FileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handle1.setFileName("handle1");
		handle1 = (S3FileHandle) fileHandleDao.createFile(handle1);
		filesToDelete.add(handle1.getId());
		// Add a preview
		PreviewFileHandle previewHandle1 = TestUtils.createPreviewFileHandle(creatorUserGroupId, idGenerator.generateNewId(IdType.FILE_IDS).toString());
		previewHandle1.setFileName("preview1");
		previewHandle1 = (PreviewFileHandle) fileHandleDao.createFile(previewHandle1);
		filesToDelete.add(previewHandle1.getId());
		
		// Checksum file only
		String checksum1 = migratableTableDAO.getChecksumForIdRange(MigrationType.FILE_HANDLE, "X", startId, Long.parseLong(handle1.getId()));
		// Checksum file and preview
		String checksum2 = migratableTableDAO.getChecksumForIdRange(MigrationType.FILE_HANDLE, "X", startId, Long.parseLong(previewHandle1.getId()));
		// Test
		assertFalse(checksum1.equals(checksum2));
		// Checksum preview only
		String checksum3 = migratableTableDAO.getChecksumForIdRange(MigrationType.FILE_HANDLE, "X", Long.parseLong(handle1.getId())+1, Long.parseLong(previewHandle1.getId()));
		// Test
		assertFalse(checksum1.equals(checksum3));
		assertFalse(checksum2.equals(checksum3));
		// Different salt
		String checksum4 = migratableTableDAO.getChecksumForIdRange(MigrationType.FILE_HANDLE, "Y", startId, Long.parseLong(handle1.getId()));
		assertFalse(checksum4.equals(checksum1));
		String checksum5 = migratableTableDAO.getChecksumForIdRange(MigrationType.FILE_HANDLE, "Y", startId, Long.parseLong(previewHandle1.getId()));
		assertFalse(checksum5.equals(checksum2));
		// Different way to specify range
		String checksum6 = migratableTableDAO.getChecksumForIdRange(MigrationType.FILE_HANDLE, "X", Long.parseLong(handle1.getId()), Long.parseLong(previewHandle1.getId()));
		assertEquals(checksum2, checksum6);
		checksum6 = migratableTableDAO.getChecksumForIdRange(MigrationType.FILE_HANDLE, "X", Long.parseLong(handle1.getId()), Long.parseLong(previewHandle1.getId())+100);
		assertEquals(checksum2, checksum6);
		// Empty range
		String checksum7 = migratableTableDAO.getChecksumForIdRange(MigrationType.FILE_HANDLE, "X", Long.parseLong(previewHandle1.getId())+1, Long.parseLong(previewHandle1.getId())+100);
		assertNull(checksum7);
		
	}
	
	@Test
	public void testGetChecksumForType2() throws Exception {
		// Before
		UserProfile profile = userProfileDAO.get(creatorUserGroupId);
		String etag1 = profile.getEtag();
		String checksum1 = migratableTableDAO.getChecksumForType(MigrationType.USER_PROFILE);

		// Update
		profile.setCompany("newCompany");
		profile = userProfileDAO.update(profile);
		String etag2 = profile.getEtag();
		String checksum2 = migratableTableDAO.getChecksumForType(MigrationType.USER_PROFILE);
		
		// Test
		assertFalse(etag1.equals(etag2));
		assertFalse(checksum1.equals(checksum2));
		
	}

	@Test
	public void testGetChecksumForRange2() throws Exception {
		// Before
		UserProfile profile = userProfileDAO.get(creatorUserGroupId);
		String etag1 = profile.getEtag();
		String checksum1 = migratableTableDAO.getChecksumForIdRange(MigrationType.USER_PROFILE, "X", Long.parseLong(profile.getOwnerId()), Long.parseLong(profile.getOwnerId()));

		// Update
		profile.setCompany("newCompany");
		profile = userProfileDAO.update(profile);
		String etag2 = profile.getEtag();
		String checksum2 = migratableTableDAO.getChecksumForIdRange(MigrationType.USER_PROFILE, "X", Long.parseLong(profile.getOwnerId()), Long.parseLong(profile.getOwnerId()));
		
		// Test
		assertFalse(etag1.equals(etag2));
		assertFalse(checksum1.equals(checksum2));
		
	}
	
	@Test
	public void testGetChecksumForRangeNoData() {
		String checksum = migratableTableDAO.getChecksumForIdRange(MigrationType.VERIFICATION_SUBMISSION, "salt", 0, 10);
		assertNull(checksum);
	}
	
	@Test
	public void testAllMigrationTypesRegistered() {
		for (MigrationType t: MigrationType.values()) {
		    assertTrue(migratableTableDAO.isMigrationTypeRegistered(t));
		}
	}
}
