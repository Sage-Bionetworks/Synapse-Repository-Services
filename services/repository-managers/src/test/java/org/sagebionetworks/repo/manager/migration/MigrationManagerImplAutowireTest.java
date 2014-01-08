package org.sagebionetworks.repo.manager.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.bootstrap.EntityBootstrapper;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MigrationManagerImplAutowireTest {
	
	@Autowired
	private FileHandleDao fileHandleDao;
	
	@Autowired
	private UserManager userManager;	
	
	@Autowired
	private MigrationManager migrationManager;
	
	@Autowired
	private EntityBootstrapper entityBootstrapper;
	
	private List<String> toDelete;
	private UserInfo adminUser;
	private String creatorUserGroupId;
	private S3FileHandle withPreview;
	private PreviewFileHandle preview;
	private long startCount;
	
	@Before
	public void before() throws Exception {
		toDelete = new LinkedList<String>();
		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		creatorUserGroupId = adminUser.getIndividualGroup().getId();
		assertNotNull(creatorUserGroupId);
		startCount = fileHandleDao.getCount();
		
		// The one will have a preview
		withPreview = TestUtils.createS3FileHandle(creatorUserGroupId);
		withPreview.setFileName("withPreview.txt");
		withPreview = fileHandleDao.createFile(withPreview);
		assertNotNull(withPreview);
		toDelete.add(withPreview.getId());
		// The Preview
		preview = TestUtils.createPreviewFileHandle(creatorUserGroupId);
		preview.setFileName("preview.txt");
		preview = fileHandleDao.createFile(preview);
		assertNotNull(preview);
		toDelete.add(preview.getId());
		// Assign it as a preview
		fileHandleDao.setPreviewId(withPreview.getId(), preview.getId());
		// The etag should have changed
		withPreview = (S3FileHandle) fileHandleDao.get(withPreview.getId());
	}
	
	@After
	public void after() throws Exception{
		// Since this test can delete all data make sure bootstrap data gets put back.
		entityBootstrapper.bootstrapAll();
		// If we have deleted all data make sure the bootstrap process puts it back
		if(fileHandleDao != null && toDelete != null){
			for(String id: toDelete){
				fileHandleDao.delete(id);
			}
		}
	}
	
	@Test
	public void testGetCount(){
		long count = migrationManager.getCount(adminUser, MigrationType.FILE_HANDLE);
		assertEquals(startCount+2, count);
	}
	
	@Test
	public void testGetMaxId() {
		long mx = migrationManager.getMaxId(adminUser, MigrationType.FILE_HANDLE);
		assertEquals(Long.parseLong(preview.getId()), mx);
	}
	
	@Test
	public void testListRowMetadata(){
		RowMetadataResult result = migrationManager.getRowMetadaForType(adminUser, MigrationType.FILE_HANDLE, Long.MAX_VALUE, startCount);
		assertNotNull(result);
		assertEquals(new Long(startCount+2), result.getTotalCount());
		assertNotNull(result.getList());
		assertEquals(2, result.getList().size());
		// List the delta
		List<Long> ids = new LinkedList<Long>();
		for(RowMetadata rm: result.getList()){
			ids.add(rm.getId());
		}
		RowMetadataResult delta = migrationManager.getRowMetadataDeltaForType(adminUser, MigrationType.FILE_HANDLE, ids);
		assertNotNull(delta);
		assertNotNull(delta.getList());
		assertEquals(result.getList(), delta.getList());
	}
	
	@Test
	public void testRoundTrip() throws Exception{
		RowMetadataResult result = migrationManager.getRowMetadaForType(adminUser, MigrationType.FILE_HANDLE, Long.MAX_VALUE, startCount);
		assertNotNull(result);
		// List the delta
		List<Long> ids1 = new LinkedList<Long>();
		ids1.add(Long.parseLong(preview.getId()));
		List<Long> ids2 = new LinkedList<Long>();
		ids2.add(Long.parseLong(withPreview.getId()));
		// Write the backup data
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		migrationManager.writeBackupBatch(adminUser, MigrationType.FILE_HANDLE, ids1, out);
		String xml1 = new String(out.toByteArray(), "UTF-8");
		System.out.println(xml1);
		out = new ByteArrayOutputStream();
		migrationManager.writeBackupBatch(adminUser, MigrationType.FILE_HANDLE, ids2, out);
		String xml2 = new String(out.toByteArray(), "UTF-8");
		System.out.println(xml2);
		// Now delete the rows
		migrationManager.deleteObjectsById(adminUser, MigrationType.FILE_HANDLE, ids1);
		migrationManager.deleteObjectsById(adminUser, MigrationType.FILE_HANDLE, ids2);
		// The count should be the same as start
		assertEquals(startCount, migrationManager.getCount(adminUser, MigrationType.FILE_HANDLE));
		// Now restore them from the xml
		ByteArrayInputStream in = new ByteArrayInputStream(xml1.getBytes("UTF-8"));
		List<Long> ids = migrationManager.createOrUpdateBatch(adminUser, MigrationType.FILE_HANDLE, in);
		assertEquals(ids1, ids);
		in = new ByteArrayInputStream(xml2.getBytes("UTF-8"));
		migrationManager.createOrUpdateBatch(adminUser, MigrationType.FILE_HANDLE, in);
		// The count should be backup
		assertEquals(startCount+2, migrationManager.getCount(adminUser, MigrationType.FILE_HANDLE));
		// Calling again should not fail
		in = new ByteArrayInputStream(xml1.getBytes("UTF-8"));
		migrationManager.createOrUpdateBatch(adminUser, MigrationType.FILE_HANDLE, in);
		// Now get the data
		RowMetadataResult afterResult = migrationManager.getRowMetadaForType(adminUser, MigrationType.FILE_HANDLE, Long.MAX_VALUE, startCount);
		assertNotNull(result);
		assertEquals(result.getList(), afterResult.getList());
	}
	
	@Test
	public void testGetSecondaryTypes(){
		// Node should have revision as a secondary.
		List<MigrationType> result = migrationManager.getSecondaryTypes(MigrationType.NODE);
		List<MigrationType> expected = new LinkedList<MigrationType>();
		expected.add(MigrationType.NODE_REVISION);
		assertEquals(expected, result);
		
		// file handles do not have secondary so null
		result = migrationManager.getSecondaryTypes(MigrationType.FILE_HANDLE);
		assertEquals(null, result);
	}
	
	@Test
	public void testDeleteAll() throws Exception{
		// Delete all data
		migrationManager.deleteAllData(adminUser);
		
		// The counts for all tables should be zero 
		// Except for 3 special cases, which are the minimal required rows to successfully
		//   call userManager.getUserInfo(AuthorizationConstants.MIGRATION_USER_NAME);
		for (MigrationType type : MigrationType.values()) {
			if (type == MigrationType.PRINCIPAL) {
				assertEquals("All non-essential " + type + " should have been deleted", 
						4L, migrationManager.getCount(adminUser, type));
			} else if (type == MigrationType.CREDENTIAL
					|| type == MigrationType.GROUP_MEMBERS) {
				assertEquals("All non-essential " + type + " should have been deleted", 
						1L, migrationManager.getCount(adminUser, type));
			} else if (migrationManager.isMigrationTypeUsed(adminUser, type)) {
				assertEquals("All data of type " + type + " should have been deleted", 
						0L, migrationManager.getCount(adminUser, type));
			}
		}
	}
	
}
