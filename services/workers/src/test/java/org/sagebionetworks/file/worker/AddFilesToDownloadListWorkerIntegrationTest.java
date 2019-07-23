package org.sagebionetworks.file.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.download.BulkDownloadManager;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.AddFileToDownloadListRequest;
import org.sagebionetworks.repo.model.file.AddFileToDownloadListResponse;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.utils.ContentTypeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class AddFilesToDownloadListWorkerIntegrationTest {

	public static final Long MAX_WAIT_MS = 1000L * 30L;

	@Autowired
	AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;

	@Autowired
	EntityManager entityManager;
	@Autowired
	UserManager userManager;
	@Autowired
	BulkDownloadManager bulkDownloadManager;
	@Autowired
	FileHandleManager fileUploadManager;
	@Autowired
	TableManagerSupport tableMangerSupport;

	UserInfo adminUserInfo;
	Project project;
	Folder folder;
	
	S3FileHandle fileHandle;
	FileEntity file;
	
	FileHandleAssociation expectedAssociation;

	@Before
	public void before() throws UnsupportedEncodingException, IOException {
		
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		bulkDownloadManager.truncateAllDownloadDataForAllUsers(adminUserInfo);
		
		String activityId = null;
		project = new Project();
		project.setName("AddFilesToDownloadListWorkerIntegrationTest");
		String id = entityManager.createEntity(adminUserInfo, project, activityId);
		project = entityManager.getEntity(adminUserInfo, id, Project.class);

		folder = new Folder();
		folder.setName("aFolder");
		folder.setParentId(project.getId());
		id = entityManager.createEntity(adminUserInfo, folder, activityId);
		folder = entityManager.getEntity(adminUserInfo, id, Folder.class);
		
		fileHandle = fileUploadManager.createFileFromByteArray(adminUserInfo
				.getId().toString(), new Date(), "contents".getBytes(StandardCharsets.UTF_8), "foo.txt",
				ContentTypeUtil.TEXT_PLAIN_UTF8, null);
		
		file = new FileEntity();
		file.setName("foo.txt");
		file.setParentId(folder.getId());
		file.setDataFileHandleId(fileHandle.getId());
		id = entityManager.createEntity(adminUserInfo, file, activityId);
		file = entityManager.getEntity(adminUserInfo, id, FileEntity.class);
		
		expectedAssociation = new FileHandleAssociation();
		expectedAssociation.setAssociateObjectId(file.getId());
		expectedAssociation.setAssociateObjectType(FileHandleAssociateType.FileEntity);
		expectedAssociation.setFileHandleId(fileHandle.getId());
		
	}

	@After
	public void after() {
		if (project != null) {
			entityManager.deleteEntity(adminUserInfo, project.getId());
		}
	}

	@Test
	public void testAddFolder() throws InterruptedException {
		AddFileToDownloadListRequest request = new AddFileToDownloadListRequest();
		request.setFolderId(folder.getId());

		// call under test
		AddFileToDownloadListResponse response = asynchronousJobWorkerHelper.startAndWaitForJob(adminUserInfo, request,
				MAX_WAIT_MS, AddFileToDownloadListResponse.class);
		assertNotNull(response);
		assertNotNull(response.getDownloadList());
		List<FileHandleAssociation> list = response.getDownloadList().getFilesToDownload();
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals(expectedAssociation, list.get(0));
	}

	@Test
	public void testAddQuery() throws InterruptedException {
		// create a view of the project.
		Long viewTypeMask = 0x01L;
		List<String> scope = Lists.newArrayList(project.getId());
		EntityView view = asynchronousJobWorkerHelper.createView(adminUserInfo, "aView", project.getParentId(), scope, viewTypeMask);
		
		// Wait for the file to appear in the table's database.
		asynchronousJobWorkerHelper.waitForEntityReplication(adminUserInfo, view.getId(), file.getId(), MAX_WAIT_MS);
		
		AddFileToDownloadListRequest request = new AddFileToDownloadListRequest();
		Query query = new Query();
		query.setSql("select * from "+view.getId());
		request.setQuery(query);

		// call under test
		AddFileToDownloadListResponse response = asynchronousJobWorkerHelper.startAndWaitForJob(adminUserInfo, request,
				MAX_WAIT_MS, AddFileToDownloadListResponse.class);
		assertNotNull(response);
		assertNotNull(response.getDownloadList());
		List<FileHandleAssociation> list = response.getDownloadList().getFilesToDownload();
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals(expectedAssociation, list.get(0));
	}
}
