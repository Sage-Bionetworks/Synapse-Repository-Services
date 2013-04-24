package org.sagebionetworks.repo.manager.migration;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.migration.MigratableTableType;
import org.sagebionetworks.repo.model.migration.RowMetadata;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MigrationManagerImplAutowireTest {
	
	@Autowired
	FileHandleDao fileHandleDao;
	
	@Autowired
	private UserManager userManager;	
	
	@Autowired
	MigrationManager migrationManager;
	
	private List<String> toDelete;
	UserInfo adminUser;
	String creatorUserGroupId;
	S3FileHandle withPreview;
	PreviewFileHandle preview;
	long startCount;
	
	@Before
	public void before() throws Exception {
		toDelete = new LinkedList<String>();
		adminUser = userManager.getUserInfo(TestUserDAO.ADMIN_USER_NAME);
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
	public void after(){
		if(fileHandleDao != null && toDelete != null){
			for(String id: toDelete){
				fileHandleDao.delete(id);
			}
		}
	}
	
	@Test
	public void testGetCount(){
		long count = migrationManager.getCount(adminUser, MigratableTableType.FILE_HANDLE);
		assertEquals(startCount+2, count);
	}
	
	@Test
	public void testListRowMetadata(){
		QueryResults<RowMetadata> result = migrationManager.listRowMetadata(adminUser, MigratableTableType.FILE_HANDLE, Long.MAX_VALUE, startCount);
		assertNotNull(result);
		assertEquals(startCount+2, result.getTotalNumberOfResults());
		assertNotNull(result.getResults());
		assertEquals(2, result.getResults().size());
		// List the delta
		List<String> ids = new LinkedList<String>();
		for(RowMetadata rm: result.getResults()){
			ids.add(rm.getId());
		}
		RowMetadataResult delta = migrationManager.listDeltaRowMetadata(adminUser, MigratableTableType.FILE_HANDLE, ids);
		assertNotNull(delta);
		assertNotNull(delta.getList());
		assertEquals(result.getResults(), delta.getList());
	}
	
	@Test
	public void testRoundTrip() throws Exception{
		QueryResults<RowMetadata> result = migrationManager.listRowMetadata(adminUser, MigratableTableType.FILE_HANDLE, Long.MAX_VALUE, startCount);
		assertNotNull(result);
		// List the delta
		List<String> ids = new LinkedList<String>();
		for(RowMetadata rm: result.getResults()){
			ids.add(rm.getId());
		}
		// Write the backup data
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		migrationManager.writeBackupBatch(adminUser, MigratableTableType.FILE_HANDLE, ids, out);
		String xml = new String(out.toByteArray(), "UTF-8");
		System.out.println(xml);
		// Now delete the rows
		migrationManager.deleteObjectsById(adminUser, MigratableTableType.FILE_HANDLE, ids);
		// The count should be the same as start
		assertEquals(startCount, migrationManager.getCount(adminUser, MigratableTableType.FILE_HANDLE));
		// Now restore them from the xml
		ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes("UTF-8"));
		migrationManager.createOrUpdateBatch(adminUser, MigratableTableType.FILE_HANDLE, in);
		// The count should be backup
		assertEquals(startCount+2, migrationManager.getCount(adminUser, MigratableTableType.FILE_HANDLE));
		// Calling again should not fail
		in = new ByteArrayInputStream(xml.getBytes("UTF-8"));
		migrationManager.createOrUpdateBatch(adminUser, MigratableTableType.FILE_HANDLE, in);
		// Now get the data
		QueryResults<RowMetadata> afterResult = migrationManager.listRowMetadata(adminUser, MigratableTableType.FILE_HANDLE, Long.MAX_VALUE, startCount);
		assertNotNull(result);
		assertEquals(result.getResults(), afterResult.getResults());
	}
	

}
