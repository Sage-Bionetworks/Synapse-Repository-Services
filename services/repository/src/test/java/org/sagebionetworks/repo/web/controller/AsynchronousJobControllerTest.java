package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Date;
import java.util.UUID;

import javax.servlet.ServletException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.AsynchUploadRequestBody;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Autowired test for AsynchronousJobController
 * @author John
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AsynchronousJobControllerTest {
	
	private Entity parent;
	private TableEntity table;
	private Long adminUserId;
	
	@Autowired
	private FileHandleDao fileMetadataDao;
	private S3FileHandle fileHandle;
	
	@Before
	public void before() throws Exception {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		parent = new Project();
		parent.setName(UUID.randomUUID().toString());
		parent = ServletTestHelper.createEntity(DispatchServletSingleton.getInstance(), parent, adminUserId);
		Assert.assertNotNull(parent);
		// Create a table
		table = new TableEntity();
		table.setName("TableEntity");
		table.setParentId(parent.getId());
		table = ServletTestHelper.createEntity(DispatchServletSingleton.getInstance(), table, adminUserId);
		// Create a file handle
		fileHandle = new S3FileHandle();
		fileHandle.setCreatedBy(adminUserId.toString());
		fileHandle.setCreatedOn(new Date());
		fileHandle.setBucketName("bucket");
		fileHandle.setKey("mainFileKey");
		fileHandle.setEtag("etag");
		fileHandle.setFileName("foo.bar");
		fileHandle = fileMetadataDao.createFile(fileHandle);
	}
	
	@After
	public void after(){
		if(parent != null){
			try {
				ServletTestHelper.deleteEntity(DispatchServletSingleton.getInstance(), Project.class, parent.getId(), adminUserId);
			} catch (Exception e) {} 
		}
		if(fileHandle != null){
			fileMetadataDao.delete(fileHandle.getId());
		}
	}
	
	@Test
	public void testStartUploadJob() throws ServletException, Exception{
		AsynchUploadRequestBody body = new AsynchUploadRequestBody();
		body.setTableId(table.getId());
		body.setUploadFileHandleId(fileHandle.getId());
		// Start the job
		AsynchronousJobStatus status = ServletTestHelper.startAsynchJob(DispatchServletSingleton.getInstance(), adminUserId, body);
		assertNotNull(status);
		assertNotNull(status.getJobId());
		assertEquals(body, status.getRequestBody());
		// Now get the status again using the ID
		AsynchronousJobStatus clone = ServletTestHelper.getAsynchJobStatus(DispatchServletSingleton.getInstance(), adminUserId, status.getJobId());
		assertEquals(status, clone);
	}

}
