package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
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
	private EntityServletTestHelper entityServletHelper;
	@Autowired
	private UserManager userManager;
	@Autowired
	private NodeManager nodeManager;
	@Autowired
	private FileHandleDao fileMetadataDao;
	
	private String userName;
	private String adminId;
	
	Project entity;
	S3FileHandle handleOne;
	PreviewFileHandle preview;
	long startFileCount;
	
	@Before
	public void before() throws Exception{
		// get user IDs
		userName = TestUserDAO.ADMIN_USER_NAME;
		adminId = userManager.getUserInfo(userName).getIndividualGroup().getId();
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
			UserInfo userInfo = userManager.getUserInfo(userName);
			nodeManager.delete(userInfo, entity.getId());
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
		long fileMaxId = 0;
		for(MigrationTypeCount type: counts.getList()){
			if(type.getType() == MigrationType.FILE_HANDLE){
				fileCount = type.getCount();
				fileMaxId = type.getMaxid();
			}
		}
		assertEquals(startFileCount+2, fileCount);
		assertEquals(Long.parseLong(preview.getId()), fileMaxId);
	}
	
	@Test
	public void testRowMetadata() throws ServletException, IOException, JSONObjectAdapterException{
		// First list the values for files
		RowMetadataResult results = entityServletHelper.getRowMetadata(userName, MigrationType.FILE_HANDLE, Long.MAX_VALUE, startFileCount);
		assertNotNull(results);
		assertNotNull(results.getList());
		assertEquals(new Long(startFileCount+2), results.getTotalCount());
		assertEquals(2, results.getList().size());
		// They should be ordered by ID
		assertEquals(handleOne.getId(), ""+results.getList().get(0).getId());
		assertEquals(preview.getId(), ""+results.getList().get(1).getId());
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
