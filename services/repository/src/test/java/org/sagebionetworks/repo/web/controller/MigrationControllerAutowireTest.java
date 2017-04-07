package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.migration.MigrationRangeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.springframework.beans.factory.annotation.Autowired;

public class MigrationControllerAutowireTest extends AbstractAutowiredControllerTestBase {
	
	public static final long MAX_WAIT_MS = 10*1000; // 10 sec.
	
	@Autowired
	private UserManager userManager;
	@Autowired
	private NodeManager nodeManager;
	@Autowired
	private FileHandleDao fileHandleDao;
	@Autowired
	private StackStatusDao stackStatusDao;
	@Autowired
	private IdGenerator idGenerator;
	
	private Long adminUserId;
	
	Project entity;
	S3FileHandle handleOne;
	PreviewFileHandle preview;
	long startFileCount;
	long startMinId;
	
	@Before
	public void before() throws Exception{
		// get user IDs
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		String adminUserIdString = adminUserId.toString();

		startFileCount = fileHandleDao.getCount();

		// Create a file handle
		handleOne = new S3FileHandle();
		handleOne.setCreatedBy(adminUserIdString);
		handleOne.setCreatedOn(new Date());
		handleOne.setBucketName("bucket");
		handleOne.setKey("mainFileKey");
		handleOne.setEtag("etag");
		handleOne.setFileName("foo.bar");
		handleOne.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handleOne.setEtag(UUID.randomUUID().toString());
		// Create a preview
		preview = new PreviewFileHandle();
		preview.setCreatedBy(adminUserIdString);
		preview.setCreatedOn(new Date());
		preview.setBucketName("bucket");
		preview.setKey("previewFileKey");
		preview.setEtag("etag");
		preview.setFileName("bar.txt");
		preview.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		preview.setEtag(UUID.randomUUID().toString());

		List<FileHandle> fileHandleToCreate = new LinkedList<FileHandle>();
		fileHandleToCreate.add(handleOne);
		fileHandleToCreate.add(preview);
		fileHandleDao.createBatch(fileHandleToCreate);

		handleOne = (S3FileHandle) fileHandleDao.get(handleOne.getId());
		preview = (PreviewFileHandle) fileHandleDao.get(preview.getId());
		// Set two as the preview of one
		fileHandleDao.setPreviewId(handleOne.getId(), preview.getId());
	}
	
	
	@After
	public void after() throws Exception{
		// Delete the project
		if(entity != null){
			UserInfo userInfo = userManager.getUserInfo(adminUserId);
			nodeManager.delete(userInfo, entity.getId());
		}
		if(handleOne != null && handleOne.getId() != null){
			fileHandleDao.delete(handleOne.getId());
		}
		if(preview != null && preview.getId() != null){
			fileHandleDao.delete(preview.getId());
		}
	}
	
	@Test
	public void testGetCounts() throws Exception {
		MigrationTypeCounts counts = entityServletHelper.getMigrationTypeCounts(adminUserId);
		assertNotNull(counts);
		assertNotNull(counts.getList());
		assertTrue(counts.getList().size() <= MigrationType.values().length);
		System.out.println(counts);
	}
	
	@Test
	public void testGetCount() throws Exception {
		MigrationTypeCount expectedCount = new MigrationTypeCount();
		expectedCount.setType(MigrationType.FILE_HANDLE);
		MigrationTypeCount mtc = entityServletHelper.getMigrationTypeCount(adminUserId, MigrationType.FILE_HANDLE);
		assertNotNull(mtc);
	}
	
	@Test
	public void testRowMetadata() throws Exception {
		// First list the values for files
		RowMetadataResult results = entityServletHelper.getRowMetadata(adminUserId, MigrationType.FILE_HANDLE, Long.MAX_VALUE, startFileCount);
		assertNotNull(results);
		assertNotNull(results.getList());
	}
	
	@Test
	public void testRowMetadataByRange() throws Exception {
		// First list the values for files
		RowMetadataResult results = entityServletHelper.getRowMetadataByRange(adminUserId, MigrationType.FILE_HANDLE, Long.parseLong(handleOne.getId()), Long.parseLong(preview.getId()), Long.MAX_VALUE, 0L);
		assertNotNull(results);
		assertNotNull(results.getList());
	}
	
	@Test
	public void testGetChecksumForIdRange() throws Exception {
		MigrationRangeChecksum checksum = entityServletHelper.getChecksumForIdRange(adminUserId, MigrationType.FILE_HANDLE, "salt", "0", handleOne.getId());
		assertNotNull(checksum);
	}
	
	@Test
	public void testGetChecksumForType() throws Exception {
		try {
			StackStatus sStatus = new StackStatus();
			sStatus.setStatus(StatusEnum.READ_ONLY);
			sStatus.setCurrentMessage("Stack in read-only mode");
			stackStatusDao.updateStatus(sStatus);
			MigrationTypeChecksum checksum = entityServletHelper.getChecksumForType(adminUserId, MigrationType.FILE_HANDLE);
			assertNotNull(checksum);
			assertNotNull(checksum.getChecksum());
			assertEquals(MigrationType.FILE_HANDLE, checksum.getType());
		} finally {
			StackStatus sStatus = new StackStatus();
			sStatus.setStatus(StatusEnum.READ_WRITE);
			sStatus.setCurrentMessage("Stack in read/write mode");
			stackStatusDao.updateStatus(sStatus);
		}
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

}
