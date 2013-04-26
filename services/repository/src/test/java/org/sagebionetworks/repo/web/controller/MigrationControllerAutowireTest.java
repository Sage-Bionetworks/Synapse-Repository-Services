package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.migration.IdList;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MigrationControllerAutowireTest {
	
	public static final long MAX_WAIT_MS = 10*1000; // 10 sec.
	
	@Autowired
	EntityServletTestHelper entityServletHelper;
	@Autowired
	UserManager userManager;
	
	@Autowired
	FileHandleDao fileMetadataDao;
	
	private String userName;
	private String adminId;
	
	Project entity;
	Evaluation evaluation;
	List<WikiPageKey> toDelete;
	S3FileHandle handleOne;
	PreviewFileHandle preview;
	long startFileCount;
	
	@Before
	public void before() throws Exception{
		// get user IDs
		userName = TestUserDAO.ADMIN_USER_NAME;
		adminId = userManager.getUserInfo(userName).getIndividualGroup().getId();
		toDelete = new LinkedList<WikiPageKey>();
		startFileCount = fileMetadataDao.getCount();
		// Create a file handle
		handleOne = new S3FileHandle();
		handleOne.setCreatedBy(adminId);
		handleOne.setCreatedOn(new Date());
		handleOne.setBucketName("bucket");
		handleOne.setKey("mainFileKey");
		handleOne.setEtag("etag");
		handleOne.setFileName("foo.bar");
		handleOne = fileMetadataDao.createFile(handleOne);
		// Create a preview
		preview = new PreviewFileHandle();
		preview.setCreatedBy(adminId);
		preview.setCreatedOn(new Date());
		preview.setBucketName("bucket");
		preview.setKey("previewFileKey");
		preview.setEtag("etag");
		preview.setFileName("bar.txt");
		preview = fileMetadataDao.createFile(preview);
		// Set two as the preview of one
		fileMetadataDao.setPreviewId(handleOne.getId(), preview.getId());
	}
	
	
	@After
	public void after() throws Exception{
		// Delete the project
		if(entity != null){
			entityServletHelper.deleteEntity(entity.getId(), userName);
		}
		if(evaluation != null){
			entityServletHelper.deleteEvaluation(evaluation.getId(), userName);
		}
		for(WikiPageKey key: toDelete){
			entityServletHelper.deleteWikiPage(key, userName);
		}
		if(handleOne != null && handleOne.getId() != null){
			fileMetadataDao.delete(handleOne.getId());
		}
		if(preview != null && preview.getId() != null){
			fileMetadataDao.delete(preview.getId());
		}
	}
	
	@Test
	public void testGetCounts() throws Exception {
		MigrationTypeCounts counts = entityServletHelper.getMigrationTypeCounts(userName);
		assertNotNull(counts);
		assertNotNull(counts.getList());
		assertEquals(MigrationType.values().length, counts.getList().size());
		System.out.println(counts);
		long fileCount = 0;
		for(MigrationTypeCount type: counts.getList()){
			if(type.getType() == MigrationType.FILE_HANDLE){
				fileCount = type.getCount();
			}
		}
		assertEquals(startFileCount+2, fileCount);
	}
	
	@Test
	public void testRowMetadata() throws ServletException, IOException, JSONObjectAdapterException{
		// First list the values for files
		RowMetadataResult results = entityServletHelper.getRowMetadata(userName, MigrationType.FILE_HANDLE, Long.MAX_VALUE, startFileCount);
		assertNotNull(results);
		assertNotNull(results.getList());
		assertEquals(new Long(startFileCount+2), results.getTotalCount());
		assertEquals(2, results.getList().size());
		// The first should be the preview
		assertEquals(preview.getId(), results.getList().get(0).getId());
		assertEquals(handleOne.getId(), results.getList().get(1).getId());
		// Use the list of ids to get the delta
		List<String> ids = new LinkedList<String>();
		for(RowMetadata row: results.getList()){
			ids.add(row.getId());
		}
		IdList deltaRequest = new IdList();
		deltaRequest.setList(ids);
		RowMetadataResult delta = entityServletHelper.getRowMetadataDelta(userName, MigrationType.FILE_HANDLE, deltaRequest);
		assertNotNull(delta);
		assertEquals(results.getList(), delta.getList());
	}
	
	@Test
	public void testRoundTrip() throws Exception{
		// Get the list of primary types
		MigrationTypeList primaryTypesList = entityServletHelper.getPrimaryMigrationTypes(userName);
		assertNotNull(primaryTypesList);
		assertNotNull(primaryTypesList.getList());
		assertTrue(primaryTypesList.getList().size() > 0);
		// Get the counts before we start
		MigrationTypeCounts startCounts = entityServletHelper.getMigrationTypeCounts(userName);
		// This test will backup all data, delete it, then restore it.
		Map<MigrationType, String> map = new HashMap<MigrationType, String>();
		for(MigrationType type: primaryTypesList.getList()){
			// Backup each type
			BackupRestoreStatus status = backupAllOfType(type);
			if(status != null){
				assertNotNull(status.getBackupUrl());
				String fileName = getFileNameFromUrl(status.getBackupUrl());
				map.put(type, fileName);
			}
		}
		// We will delete the data when all object are ready
		
//		// Now delete all data in reverse order
//		for(int i=primaryTypesList.getList().size()-1; i >= 0; i--){
//			deleteAllOfType(MigrationType.values()[i]);
//		}
//		// after deleting, the counts should be null
//		MigrationTypeCounts afterDeleteCounts = entityServletHelper.getMigrationTypeCounts(userName);
//		assertNotNull(afterDeleteCounts);
//		assertNotNull(afterDeleteCounts.getList());
//		for(int i=0; i<afterDeleteCounts.getList().size(); i++){
//			assertEquals(new Long(0), afterDeleteCounts.getList().get(i).getCount());
//		}
		
		// Now restore all of the data
		for(MigrationType type: primaryTypesList.getList()){
			String fileName = map.get(type);
			if(fileName != null){
				restoreFromBackup(type, fileName);
			}
		}
		// The counts should all be back
		MigrationTypeCounts finalCounts = entityServletHelper.getMigrationTypeCounts(userName);
		assertEquals(startCounts, finalCounts);
	}
	
	/**
	 * Extract the filename from the full url.
	 * @param fullUrl
	 * @return
	 */
	public String getFileNameFromUrl(String fullUrl){;
		int index = fullUrl.lastIndexOf("/");
		return fullUrl.substring(index+1, fullUrl.length());
	}
	
	/**
	 * Backup all data
	 * @param type
	 * @return
	 * @throws Exception
	 */
	private BackupRestoreStatus backupAllOfType(MigrationType type) throws Exception {
		IdList idList = getIdListOfAllOfType(type);
		if(idList == null) return null;
		// Start the backup job
		BackupRestoreStatus status = entityServletHelper.startBackup(userName, type, idList);
		// wait for it..
		waitForDaemon(status);
		return entityServletHelper.getBackupRestoreStatus(userName, status.getId());
	}
	
	private void restoreFromBackup(MigrationType type, String fileName) throws ServletException, IOException, JSONObjectAdapterException, InterruptedException{
		RestoreSubmission sub = new RestoreSubmission();
		sub.setFileName(fileName);
		BackupRestoreStatus status = entityServletHelper.startRestore(userName, type, sub);
		// wait for it
		waitForDaemon(status);
	}
	
	/**
	 * Delete all data for a type.
	 * @param type
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	private void deleteAllOfType(MigrationType type) throws ServletException, IOException, JSONObjectAdapterException{
		IdList idList = getIdListOfAllOfType(type);
		if(idList == null) return;
		MigrationTypeCount result = entityServletHelper.deleteMigrationType(userName, type, idList);
		System.out.print("Deleted: "+result);
	}
	
	/**
	 * List all of the IDs for a type.
	 * @param type
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	private IdList getIdListOfAllOfType(MigrationType type) throws ServletException, IOException, JSONObjectAdapterException{
		RowMetadataResult list = entityServletHelper.getRowMetadata(userName, type, Long.MAX_VALUE, 0);
		if(list.getTotalCount() < 1) return null;
		// Create the backup list
		List<String> toBackup = new LinkedList<String>();
		for(RowMetadata row: list.getList()){
			toBackup.add(row.getId());
		}
		IdList idList = new IdList();
		idList.setList(toBackup);
		return idList;
	}
	
	/**
	 * Wait for a deamon to process a a job.
	 * @param status
	 * @throws InterruptedException 
	 * @throws JSONObjectAdapterException 
	 * @throws IOException 
	 * @throws ServletException 
	 */
	private void waitForDaemon(BackupRestoreStatus status) throws InterruptedException, ServletException, IOException, JSONObjectAdapterException{
		long start = System.currentTimeMillis();
		while(DaemonStatus.COMPLETED != status.getStatus()){
			assertFalse("Daemon failed", DaemonStatus.FAILED == status.getStatus());
			System.out.println("Waiting for backup/restore daemon.  Message: "+status.getProgresssMessage());
			Thread.sleep(1000);
			long elapse = System.currentTimeMillis() - start;
			assertTrue("Timed out waiting for a backup/restore daemon",elapse < MAX_WAIT_MS);
			status = entityServletHelper.getBackupRestoreStatus(userName, status.getId());
		}
	}

}
