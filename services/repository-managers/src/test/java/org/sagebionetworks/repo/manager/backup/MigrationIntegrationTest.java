package org.sagebionetworks.repo.manager.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.backup.daemon.BackupDaemonLauncher;
import org.sagebionetworks.repo.manager.backup.migration.DependencyManager;
import org.sagebionetworks.repo.manager.trash.TrashConstants;
import org.sagebionetworks.repo.manager.wiki.WikiManager;
import org.sagebionetworks.repo.model.DaemonStatusUtil;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.MigratableObjectType;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.DaemonStatus;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * This test is designed to test the entire migration process.
 * 
 * @author John
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MigrationIntegrationTest {
	
	public static final long MAX_WAIT = 10*1000;
	
	@Autowired
	private BackupDaemonLauncher backupDaemonLauncher;
	
	@Autowired
	DependencyManager dependencyManager;
	
	@Autowired 
	NodeDAO nodeDao;
	
	@Autowired
	FileHandleDao fileHandleDao;
	
	@Autowired
	EntityManager entityManager;
	
	@Autowired
	WikiManager wikiManager;
	
	@Autowired
	WikiPageDao wikiPageDao;
	
	@Autowired
	UserProvider testUserProvider;
	UserInfo adminUser;
	String adminUserId;
	
	Set<String> bootstrapNodes;
	
	S3FileHandle s3handle;
	PreviewFileHandle preview;
	FileEntity fileEntity;
	WikiPage rootWiki;
	WikiPageKey rootWikiKey;
	WikiPage childWiki;
	WikiPageKey childWikiKey;
	
	@Before
	public void before() throws Exception{
		adminUser = testUserProvider.getTestAdminUserInfo();
		adminUserId = adminUser.getIndividualGroup().getId();
		bootstrapNodes = new HashSet<String>();
		// Before we start the test delete all non-boostrap data
		String rootId = nodeDao.getNodeIdForPath(NodeConstants.ROOT_FOLDER_PATH);
		String trashCanId = nodeDao.getNodeIdForPath(TrashConstants.TRASH_FOLDER_PATH);
		bootstrapNodes.add(trashCanId);
		bootstrapNodes.add(rootId);
		// Start with a clean database
		deleteAllNonBootStrapData();
	}
	
	@After
	public void after() throws UnauthorizedException, DatastoreException, NotFoundException{
		// Delete all non-bootstrap data
		deleteAllNonBootStrapData();
	}
	
	/**
	 * This test will do the following:
	 * 1. Created examples of all object types (FileHandles, Entites, WikiPages, Activity...)
	 * 2. Simulate migration by backing up all objects to S3.
	 * 3. Delete all objects from the database.
	 * 4. Validate the database is empty.
	 * 5. Restore all Objects from their S3 Backup files.
	 * 6. Validate each object from step 1 are restored to their original state.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMigration() throws Exception{
		// Create all objects to Migrate.
		createFileHandleData();
		createFileEntity();
		createWikiPage();
		// Migrate and clear
		// Now move all of the data to S3 and clear the database
		List<MigrationData> migrationData = backupToS3AndClearDatabase();
		
		// The database should now be empty
		// There should be no file handles
		validateDatabaseIsEmpty();
		
		// Restore from S3
		restoreAllData(migrationData);
		
		// Validate everything is back
		validateFileHandlesRestored();
		validateFileEntityRestored();
		validateWikiPagesRestored();
	}

	/**
	 * Validate that the database is empty.
	 */
	private void validateDatabaseIsEmpty() {
		assertEquals(0l, fileHandleDao.getCount());
		assertEquals(0l, nodeDao.getCount());
		assertEquals(0l, wikiPageDao.getCount());
	}
	
	/**
	 * Validate that the wikiPages are restored.
	 * @throws NotFoundException 
	 * @throws Exception 
	 */
	private void validateWikiPagesRestored() throws Exception {
		WikiPage clone = wikiManager.getWikiPage(adminUser, rootWikiKey);
		assertEquals(rootWiki, clone);
		clone = wikiManager.getWikiPage(adminUser, childWikiKey);
		assertEquals(childWiki, clone);
	}

	/**
	 * Create a small wiki page hierarchy.
	 * @throws Exception
	 */
	private void createWikiPage() throws Exception {
		// Create a wiki page using the file entity as the owner
		rootWiki = new WikiPage();
		rootWiki.setTitle("MigrationIntegrationTest.RootWiki");
		rootWiki.setMarkdown("root markdown");
		rootWiki.setAttachmentFileHandleIds(new LinkedList<String>());
		rootWiki.getAttachmentFileHandleIds().add(s3handle.getId());
		rootWiki = wikiManager.createWikiPage(adminUser, fileEntity.getId(), ObjectType.ENTITY, rootWiki);
		rootWikiKey = new WikiPageKey(fileEntity.getId(), ObjectType.ENTITY, rootWiki.getId());
		// Create a child wiki
		childWiki = new WikiPage();
		childWiki.setTitle("MigrationIntegrationTest.ChildWiki");
		childWiki.setMarkdown("child markdown");
		childWiki.setParentWikiId(rootWiki.getId());
		childWiki = wikiManager.createWikiPage(adminUser, fileEntity.getId(), ObjectType.ENTITY, childWiki);
		childWikiKey = new WikiPageKey(fileEntity.getId(), ObjectType.ENTITY, childWiki.getId());
	}

	/**
	 * Validate all of the FileHandles are restored.
	 * 
	 * @throws Exception
	 */
	private void validateFileHandlesRestored() throws Exception {
		assertEquals(2l, fileHandleDao.getCount());
		S3FileHandle s3Clone = (S3FileHandle) fileHandleDao.get(s3handle.getId());
		assertEquals(s3handle, s3Clone);
	}

	/**
	 * Validate the FileEntity is restored.
	 * @throws NotFoundException
	 */
	private void validateFileEntityRestored() throws NotFoundException {
		FileEntity restoredFile = entityManager.getEntity(adminUser, fileEntity.getId(), FileEntity.class);
		assertEquals(fileEntity, restoredFile);
	}
	
	/**
	 * Helper to populate the database with FileHandle data.
	 * @throws Exception
	 */
	public void createFileHandleData() throws Exception {
		s3handle = createS3FileHandle();
		s3handle = fileHandleDao.createFile(s3handle);
		preview = createPreviewFileHandle();
		preview = fileHandleDao.createFile(preview);
		fileHandleDao.setPreviewId(s3handle.getId(), preview.getId());
		s3handle = (S3FileHandle) fileHandleDao.get(s3handle.getId());
	}
	
	public void createFileEntity() throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException{
		fileEntity = new FileEntity();
		fileEntity.setEntityType(FileEntity.class.getName());
		fileEntity.setName("MigrationIntegrationTest.fileEntity");
		fileEntity.setDataFileHandleId(s3handle.getId());
		String id = entityManager.createEntity(adminUser, fileEntity, null);
		fileEntity = entityManager.getEntity(adminUser, id, FileEntity.class);
	}
	
	
	/**
	 * Delete all non-boostrap data in the database.
	 * 
	 * @throws UnauthorizedException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void deleteAllNonBootStrapData() throws UnauthorizedException, DatastoreException, NotFoundException{
		// First get the full list of all of the data in the database.
		QueryResults<MigratableObjectData> allData = dependencyManager.getAllObjects(0, Long.MAX_VALUE, true);
		// Delete in reverse order.
		for(int i=allData.getResults().size()-1 ; i>-1; i--){
			MigratableObjectData mod = allData.getResults().get(i);
			// Skip principals
			if(mod.getId().getType() == MigratableObjectType.PRINCIPAL){
				System.out.println("Skipping principal: "+mod.getId());
				continue;
			}else if(mod.getId().getType() == MigratableObjectType.ENTITY){
				if(bootstrapNodes.contains(mod.getId().getId())) {
					System.out.println("Skipping bootstrap entity: "+mod.getId());
					continue;
				}
			}
			// Delete all others
			System.out.println("Deleting: "+mod.getId());
			backupDaemonLauncher.delete(adminUser, mod.getId());
		}
	}
	
	/**
	 * Restore all of the data from S3.
	 * @param migrationData
	 * @throws UnauthorizedException
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws InterruptedException
	 */
	private void restoreAllData(List<MigrationData> list) throws UnauthorizedException, DatastoreException, NotFoundException, InterruptedException{		
		// Now restore all data one at a time
		for(MigrationData md: list){
			MigratableObjectData mod = md.getMigratableObject();
			String fileName = getFileNameFromUrl(md.getStatus().getBackupUrl());
			BackupRestoreStatus restoreStatus = backupDaemonLauncher.startRestore(adminUser, fileName, mod.getId().getType());
			waitForStatus(DaemonStatus.COMPLETED, restoreStatus.getId());
		}
	}
	
	/**
	 * Backup everything to S3 and clear the Database.
	 * @return
	 * @throws Exception
	 */
	private List<MigrationData> backupToS3AndClearDatabase() throws Exception{
		// First get the full list of all of the data in the database.
		QueryResults<MigratableObjectData> allData = dependencyManager.getAllObjects(0, Long.MAX_VALUE, true);
		// Note: In the real world it is possible for a child entity to have a parent ID that is larger than itself 
		// (i.e. create the child, then create a container, then move the child to the container).  So normally we cannot
		// simply depend on the order of list.  However, all objects used in this test were created in order. 
		// Create a backup of each object.
		List<MigrationData> statusList = new LinkedList<MigrationData>();
		for(MigratableObjectData mod: allData.getResults()){
			// skip principals
			if(mod.getId().getType() == MigratableObjectType.PRINCIPAL) continue;
			Set<String> set = new HashSet<String>(1);
			set.add(mod.getId().getId());
			BackupRestoreStatus status = backupDaemonLauncher.startBackup(adminUser, set, mod.getId().getType());
			statusList.add(new MigrationData(status, mod));
		}
		// Wait for everything to finish
		List<MigrationData> finished = new LinkedList<MigrationData>();
		long start = System.currentTimeMillis();
		while(statusList.size() > 0){
			MigrationData md = statusList.get(0);
			BackupRestoreStatus status = md.getStatus();
			status = backupDaemonLauncher.getStatus(adminUser, status.getId());
			if(status.getStatus() == DaemonStatus.FAILED){
				fail(status.getErrorDetails());
			}else if( DaemonStatus.COMPLETED == status.getStatus()){
				// Done
				statusList.remove(0);
				finished.add(new MigrationData(status, md.getMigratableObject()));
			}else{
				// Wait
				System.out.println(status.getProgresssMessage());
				Thread.sleep(1000);
				assertTrue("Timed out waiting for a backup.",System.currentTimeMillis() - start < MAX_WAIT);
			}
		}
		// delete everything in reverse order to so dependencies are deleted first.
		for(int i=allData.getResults().size()-1 ; i>-1; i--){
			MigratableObjectData mod = allData.getResults().get(i);
			if(mod.getId().getType() == MigratableObjectType.PRINCIPAL) continue;
			backupDaemonLauncher.delete(adminUser, mod.getId());
		}
		return finished;
	}
	
	/**
	 * Captures all of the data needed to restore the entire database.
	 * @author John
	 *
	 */
	private static class MigrationData{
		BackupRestoreStatus status;
		MigratableObjectData migratableObject;
		public MigrationData(BackupRestoreStatus status,
				MigratableObjectData migratableObject) {
			super();
			this.status = status;
			this.migratableObject = migratableObject;
		}
		public BackupRestoreStatus getStatus() {
			return status;
		}
		public MigratableObjectData getMigratableObject() {
			return migratableObject;
		}
		
	}
	
	/**
	 * Helper method to wait for a given status of the Daemon
	 * @param lookinFor
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws InterruptedException
	 * @throws UnauthorizedException 
	 */
	private BackupRestoreStatus waitForStatus(DaemonStatus lookinFor, String id) throws DatastoreException, NotFoundException, InterruptedException, UnauthorizedException{
		BackupRestoreStatus status = backupDaemonLauncher.getStatus(testUserProvider.getTestAdminUserInfo(), id);
		long start = System.currentTimeMillis();
		while(!lookinFor.equals(status.getStatus())){
			// Wait for it to complete
			Thread.sleep(1000);
			assertTrue("Timed out waiting for the backup deamon to finish", System.currentTimeMillis() - start < MAX_WAIT);
			status = backupDaemonLauncher.getStatus(testUserProvider.getTestAdminUserInfo(), id);
			assertEquals(id, status.getId());
			System.out.println(DaemonStatusUtil.printStatus(status));
			if(DaemonStatus.FAILED != lookinFor && DaemonStatus.FAILED.equals(status.getStatus())){
				fail("Unexpected failure: "+status.getErrorMessage()+" "+status.getErrorDetails());
			}
		}
		return status;
	}

	/**
	 * Helper to create a S3FileHandle
	 * 
	 * @return
	 */
	public S3FileHandle createS3FileHandle() {
		S3FileHandle meta = new S3FileHandle();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		meta.setCreatedBy(adminUserId);
		meta.setFileName("foobar.txt");
		return meta;
	}

	/**
	 * Helper to create a PreviewFileHandle
	 * @return
	 */
	public PreviewFileHandle createPreviewFileHandle() {
		PreviewFileHandle meta = new PreviewFileHandle();
		meta.setBucketName("bucketName");
		meta.setKey("key");
		meta.setContentType("content type");
		meta.setContentSize(123l);
		meta.setContentMd5("md5");
		meta.setCreatedBy(adminUserId);
		meta.setFileName("preview.jpg");
		return meta;
	}

	public static String getFileNameFromUrl(String fullUrl) {
		int index = fullUrl.lastIndexOf("/");
		return fullUrl.substring(index + 1, fullUrl.length());
	}
	
}
