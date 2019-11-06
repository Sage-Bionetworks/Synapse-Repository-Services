package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.UUID;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.UploadToTableRequest;
import org.springframework.beans.factory.annotation.Autowired;

import junit.framework.Assert;

/**
 * Autowired test for AsynchronousJobController
 * @author John
 *
 */
public class AsynchronousJobControllerTest extends AbstractAutowiredControllerTestBase {
	
	private Entity parent;
	private TableEntity table;
	private Long adminUserId;
	
	@Autowired
	private FileHandleDao fileMetadataDao;
	@Autowired
	private IdGenerator idGenerator;
	private S3FileHandle fileHandle;

	@Before
	public void before() throws Exception {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		parent = new Project();
		parent.setName(UUID.randomUUID().toString());
		parent = servletTestHelper.createEntity(dispatchServlet, parent, adminUserId);
		Assert.assertNotNull(parent);
		// Create a table
		table = new TableEntity();
		table.setName("TableEntity");
		table.setParentId(parent.getId());
		table = servletTestHelper.createEntity(dispatchServlet, table, adminUserId);
		// Create a file handle
		fileHandle = TestUtils.createS3FileHandle(adminUserId.toString(), idGenerator.generateNewId(IdType.FILE_IDS).toString());
		fileHandle = (S3FileHandle) fileMetadataDao.createFile(fileHandle);
	}
	
	@After
	public void after(){
		if(parent != null){
			try {
				servletTestHelper.deleteEntity(dispatchServlet, Project.class, parent.getId(), adminUserId);
			} catch (Exception e) {} 
		}
		if(fileHandle != null){
			fileMetadataDao.delete(fileHandle.getId());
		}
	}
	
	@Test
	public void testStartUploadJob() throws ServletException, Exception{
		TableUpdateTransactionRequest body = new TableUpdateTransactionRequest();
		UploadToTableRequest uploadToTableRequest = new UploadToTableRequest();
		uploadToTableRequest.setTableId(table.getId());
		uploadToTableRequest.setUploadFileHandleId(fileHandle.getId());
		body.setChanges(Collections.singletonList(uploadToTableRequest));
		// Start the job
		AsynchronousJobStatus status = servletTestHelper.startAsynchJob(dispatchServlet, adminUserId, body);
		assertNotNull(status);
		assertNotNull(status.getJobId());
		assertEquals(body, status.getRequestBody());
		// Now get the status again using the ID
		AsynchronousJobStatus clone = servletTestHelper.getAsynchJobStatus(dispatchServlet, adminUserId,
				status.getJobId());
		assertEquals(status, clone);
	}

}
