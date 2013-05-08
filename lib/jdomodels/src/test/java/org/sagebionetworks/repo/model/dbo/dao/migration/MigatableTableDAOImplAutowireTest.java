package org.sagebionetworks.repo.model.dbo.dao.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.dbo.migration.MigatableTableDAO;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class MigatableTableDAOImplAutowireTest {

	@Autowired
	FileHandleDao fileHandleDao;
	
	@Autowired
	private UserGroupDAO userGroupDAO;

	@Autowired
	MigatableTableDAO migatableTableDAO;
	
	private List<String> toDelete;
	String creatorUserGroupId;
	
	@Before
	public void before(){
		toDelete = new LinkedList<String>();
		creatorUserGroupId = userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId();
		assertNotNull(creatorUserGroupId);
	}
	
	@After
	public void after(){
		if(fileHandleDao != null && toDelete != null){
			for(String id: toDelete){
				fileHandleDao.delete(id);
			}
		}
	}
	
	@Test
	public void testMigrationRoundTrip() throws Exception {
		long startCount = fileHandleDao.getCount();
		long migrationCount = migatableTableDAO.getCount(MigrationType.FILE_HANDLE);
		assertEquals(startCount, migrationCount);
		// The one will have a preview
		S3FileHandle withPreview = TestUtils.createS3FileHandle(creatorUserGroupId);
		withPreview.setFileName("withPreview.txt");
		withPreview = fileHandleDao.createFile(withPreview);
		assertNotNull(withPreview);
		toDelete.add(withPreview.getId());
		// The Preview
		PreviewFileHandle preview = TestUtils.createPreviewFileHandle(creatorUserGroupId);
		preview.setFileName("preview.txt");
		preview = fileHandleDao.createFile(preview);
		assertNotNull(preview);
		toDelete.add(preview.getId());
		// Assign it as a preview
		fileHandleDao.setPreviewId(withPreview.getId(), preview.getId());
		// The etag should have changed
		withPreview = (S3FileHandle) fileHandleDao.get(withPreview.getId());
		
		// Now list all of the objects
		RowMetadataResult totalList = migatableTableDAO.listRowMetadata(MigrationType.FILE_HANDLE, 1000, startCount);
		assertNotNull(totalList);
		assertEquals(new Long(startCount+2),  totalList.getTotalCount());
		assertNotNull(totalList.getList());
		assertEquals(2, totalList.getList().size());
		System.out.println(totalList.getList());
		// The preview should be first
		RowMetadata row = totalList.getList().get(0);
		assertEquals(preview.getId(), row.getId());
		assertEquals(preview.getEtag(), row.getEtag());
		// Followed by the withPreview
		row = totalList.getList().get(1);
		assertEquals(withPreview.getId(), row.getId());
		assertEquals(withPreview.getEtag(), row.getEtag());
		
		// Now list the deltas
		List<String> idsToFind = new LinkedList<String>();
		// This should not exist
		idsToFind.add(""+(Long.MAX_VALUE - 10));
		idsToFind.add(preview.getId());
		idsToFind.add(withPreview.getId());
		// This should not exist
		idsToFind.add(""+(Long.MAX_VALUE - 101));
		// Get the detla
		List<RowMetadata> delta = migatableTableDAO.listDeltaRowMetadata(MigrationType.FILE_HANDLE, idsToFind);
		assertNotNull(delta);
		assertEquals(2, delta.size());
		// The preview should be first
		row = delta.get(0);
		assertEquals(preview.getId(), row.getId());
		assertEquals(preview.getEtag(), row.getEtag());
		// Followed by the withPreview
		row = delta.get(1);
		assertEquals(withPreview.getId(), row.getId());
		assertEquals(withPreview.getEtag(), row.getEtag());
		
		// Get the full back object
		List<String> idsToBackup = new LinkedList<String>();
		idsToBackup.add(preview.getId());
		idsToBackup.add(withPreview.getId());
		List<DBOFileHandle> backupList = migatableTableDAO.getBackupBatch(DBOFileHandle.class, idsToBackup);
		assertNotNull(backupList);
		assertEquals(2, backupList.size());
		// preview
		DBOFileHandle dbfh = backupList.get(0);
		assertEquals(preview.getId(), ""+dbfh.getId());
		//with preview.
		dbfh = backupList.get(1);
		assertEquals(withPreview.getId(), ""+dbfh.getId());
		// Now delete all of the data
		int count = migatableTableDAO.deleteObjectsById(MigrationType.FILE_HANDLE, idsToBackup);
		assertEquals(2, count);
		assertEquals(startCount, migatableTableDAO.getCount(MigrationType.FILE_HANDLE));
		// Now restore the data
		int[] result = migatableTableDAO.createOrUpdateBatch(backupList);
		assertNotNull(result);
		assertEquals(2, result.length);
		assertEquals(1, result[0]);
		assertEquals(1, result[1]);
		// Now make sure if we update again it works
		backupList.get(0).setBucketName("updateBucketName");
		result = migatableTableDAO.createOrUpdateBatch(backupList);
		assertNotNull(result);
		assertEquals(2, result.length);
		// Check final counts
		delta = migatableTableDAO.listDeltaRowMetadata(MigrationType.FILE_HANDLE, idsToFind);
		assertNotNull(delta);
		assertEquals(2, delta.size());
		// The preview should be first
		row = delta.get(0);
		assertEquals(preview.getId(), row.getId());
		assertEquals(preview.getEtag(), row.getEtag());
		// Followed by the withPreview
		row = delta.get(1);
		assertEquals(withPreview.getId(), row.getId());
		assertEquals(withPreview.getEtag(), row.getEtag());
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
		expectedPrimaryTypes.add(MigrationType.USER_PROFILE);
		expectedPrimaryTypes.add(MigrationType.FILE_HANDLE);
		expectedPrimaryTypes.add(MigrationType.WIKI_PAGE);
		expectedPrimaryTypes.add(MigrationType.WIKI_OWNERS);
		expectedPrimaryTypes.add(MigrationType.ACTIVITY);
		expectedPrimaryTypes.add(MigrationType.NODE);
		expectedPrimaryTypes.add(MigrationType.ACCESS_REQUIREMENT);
		expectedPrimaryTypes.add(MigrationType.ACCESS_APPROVAL);
		expectedPrimaryTypes.add(MigrationType.EVALUATION);
		expectedPrimaryTypes.add(MigrationType.PARTICIPANT);
		expectedPrimaryTypes.add(MigrationType.SUBMISSION);
		expectedPrimaryTypes.add(MigrationType.SUBMISSION_STATUS);
		expectedPrimaryTypes.add(MigrationType.FAVORITE);
		expectedPrimaryTypes.add(MigrationType.TRASH_CAN);
		expectedPrimaryTypes.add(MigrationType.DOI);
		expectedPrimaryTypes.add(MigrationType.CHANGE);
		// Get the list
		List<MigrationType> primary = migatableTableDAO.getPrimaryMigrationTypes();
		System.out.println(primary);
		assertEquals(expectedPrimaryTypes, primary);
	}
}
