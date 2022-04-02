package org.sagebionetworks.download.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sagebionetworks.repo.model.util.AccessControlListUtil.createResourceAccess;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.file.download.v2.DownloadListDAO;
import org.sagebionetworks.repo.model.download.ActionRequiredRequest;
import org.sagebionetworks.repo.model.download.ActionRequiredCount;
import org.sagebionetworks.repo.model.download.ActionRequiredResponse;
import org.sagebionetworks.repo.model.download.AddToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddToDownloadListResponse;
import org.sagebionetworks.repo.model.download.AvailableFilesRequest;
import org.sagebionetworks.repo.model.download.AvailableFilesResponse;
import org.sagebionetworks.repo.model.download.DownloadListItem;
import org.sagebionetworks.repo.model.download.DownloadListItemResult;
import org.sagebionetworks.repo.model.download.DownloadListQueryRequest;
import org.sagebionetworks.repo.model.download.DownloadListQueryResponse;
import org.sagebionetworks.repo.model.download.RequestDownload;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.helper.AccessControlListObjectHelper;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.DatasetItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DownloadListWorkerIntegrationTest {

	public static final long MAX_WAIT_MS = 1000 * 30;
	
	public static final int MAX_RETRIES = 25;

	@Autowired
	private UserManager userManager;
	@Autowired
	private DownloadListDAO downloadListDao;
	@Autowired
	private DaoObjectHelper<Node> nodeDaoHelper;
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private DaoObjectHelper<S3FileHandle> fileHandleDaoHelper;
	@Autowired
	private FileHandleDao fileHandleDao;
	@Autowired
	private AccessControlListObjectHelper aclHelper;
	@Autowired
	private AccessControlListDAO aclDao;

	@Autowired
	AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;

	UserInfo adminUser;
	UserInfo user;

	@BeforeEach
	public void before() {
		aclDao.truncateAll();
		nodeDao.truncateAll();
		fileHandleDao.truncateTable();
		downloadListDao.truncateAllData();

		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		boolean acceptsTermsOfUse = true;
		String userName = UUID.randomUUID().toString();
		user = userManager.createOrGetTestUser(adminUser,
				new NewUser().setUserName(userName).setEmail(userName + "@foo.org"), acceptsTermsOfUse);
	}

	@AfterEach
	public void after() {
		aclDao.truncateAll();
		nodeDao.truncateAll();
		fileHandleDao.truncateTable();
		downloadListDao.truncateAllData();
		if (user != null) {
			userManager.deletePrincipal(adminUser, user.getId());
		}
	}

	@Test
	public void testQueryWorkerWithAvailable() throws Exception {
		Node file = createFileHierarchy(ACCESS_TYPE.DOWNLOAD);
		List<DownloadListItem> batch = Arrays.asList(new DownloadListItem().setFileEntityId(file.getId()));
		downloadListDao.addBatchOfFilesToDownloadList(user.getId(), batch);

		DownloadListQueryRequest request = new DownloadListQueryRequest().setRequestDetails(new AvailableFilesRequest());
		// call under test
		asynchronousJobWorkerHelper.assertJobResponse(user, request, (DownloadListQueryResponse response) -> {
			assertNotNull(response);
			assertNotNull(response.getResponseDetails());
			assertTrue(response.getResponseDetails() instanceof AvailableFilesResponse);
			AvailableFilesResponse details = (AvailableFilesResponse) response.getResponseDetails();
			assertNotNull(details.getPage());
			List<DownloadListItemResult> page = details.getPage();
			assertEquals(1, page.size());
			DownloadListItemResult item = page.get(0);
			assertEquals(file.getId(), item.getFileEntityId());
		}, MAX_WAIT_MS, MAX_RETRIES);
	}
	
	@Test
	public void testQueryWorkerWithActionRequired() throws Exception {
		// The user is only granted read and not download on the file.
		Node file = createFileHierarchy(ACCESS_TYPE.READ);
		Long benefactorId = KeyFactory.stringToKey(file.getParentId());
		List<DownloadListItem> batch = Arrays.asList(new DownloadListItem().setFileEntityId(file.getId()));
		downloadListDao.addBatchOfFilesToDownloadList(user.getId(), batch);

		DownloadListQueryRequest request = new DownloadListQueryRequest().setRequestDetails(new ActionRequiredRequest());
		// call under test
		asynchronousJobWorkerHelper.assertJobResponse(user, request, (DownloadListQueryResponse response) -> {
			assertNotNull(response);
			assertNotNull(response.getResponseDetails());
			assertTrue(response.getResponseDetails() instanceof ActionRequiredResponse);
			ActionRequiredResponse details = (ActionRequiredResponse) response.getResponseDetails();
			assertNotNull(details.getPage());
			List<ActionRequiredCount> expected = Arrays
					.asList(new ActionRequiredCount().setCount(1L)
							.setAction(new RequestDownload().setBenefactorId(benefactorId)));
			assertEquals(expected, details.getPage());
		}, MAX_WAIT_MS, MAX_RETRIES);
	}
	
	@Test
	public void testQueryWorkerWithActionRequired_FileDeleted() throws Exception {
		// The user is only granted read and not download on the file.
		Node file = createFileHierarchy(ACCESS_TYPE.READ);
		Long benefactorId = KeyFactory.stringToKey(file.getParentId());
		List<DownloadListItem> batch = Arrays.asList(new DownloadListItem().setFileEntityId(file.getId()));
		downloadListDao.addBatchOfFilesToDownloadList(user.getId(), batch);
		
		// delete the file!
		nodeDao.delete(file.getId());

		DownloadListQueryRequest request = new DownloadListQueryRequest().setRequestDetails(new ActionRequiredRequest());
		// call under test
		asynchronousJobWorkerHelper.assertJobResponse(user, request, (DownloadListQueryResponse response) -> {
			assertNotNull(response);
			assertNotNull(response.getResponseDetails());
			assertTrue(response.getResponseDetails() instanceof ActionRequiredResponse);
			ActionRequiredResponse details = (ActionRequiredResponse) response.getResponseDetails();
			assertNotNull(details.getPage());
			List<ActionRequiredCount> expected = Arrays.asList(); // empty list
			assertEquals(expected, details.getPage());
		}, MAX_WAIT_MS, MAX_RETRIES);
	}
	
	@Test
	public void testAddToDownloadListDatasetItems() throws Exception {
		// create file in a project with read access
		Node file = createFileHierarchy(ACCESS_TYPE.DOWNLOAD, ACCESS_TYPE.READ);
		// add file to a data set
		List<DatasetItem> items = Arrays.asList(new DatasetItem().setEntityId(file.getId())
				.setVersionNumber(file.getVersionNumber()));
		Node dataset = nodeDaoHelper.create(n -> {
			n.setName("aDataset");
			n.setParentId(file.getParentId());
			n.setNodeType(EntityType.dataset);
			n.setItems(items);
		});
		AddToDownloadListRequest addRequest = new AddToDownloadListRequest().setParentId(dataset.getId());
		// call under test
		asynchronousJobWorkerHelper.assertJobResponse(user, addRequest, (AddToDownloadListResponse response) -> {
			assertNotNull(response);
			assertEquals(1L, response.getNumberOfFilesAdded());
		}, MAX_WAIT_MS, MAX_RETRIES);
		
		// query to show it's there
		DownloadListQueryRequest queryRequest = new DownloadListQueryRequest().setRequestDetails(new AvailableFilesRequest());
		asynchronousJobWorkerHelper.assertJobResponse(user, queryRequest, (DownloadListQueryResponse response) -> {
			assertNotNull(response);
			assertNotNull(response.getResponseDetails());
			assertTrue(response.getResponseDetails() instanceof AvailableFilesResponse);
			AvailableFilesResponse details = (AvailableFilesResponse) response.getResponseDetails();
			assertNotNull(details.getPage());
			List<DownloadListItemResult> page = details.getPage();
			assertEquals(1, page.size());
			DownloadListItemResult item = page.get(0);
			assertEquals(file.getId(), item.getFileEntityId());
		}, MAX_WAIT_MS, MAX_RETRIES);
	}
	
	@Test
	public void testAddToDownloadListWorkerWithFolder() throws Exception {
		
		Node file = createFileHierarchy(ACCESS_TYPE.READ);
		
		AddToDownloadListRequest request = new AddToDownloadListRequest().setParentId(file.getParentId());
		// call under test
		asynchronousJobWorkerHelper.assertJobResponse(user, request, (AddToDownloadListResponse response) -> {
			assertNotNull(response);
			assertEquals(1L, response.getNumberOfFilesAdded());
		}, MAX_WAIT_MS, MAX_RETRIES);
		
	}
	

	/**
	 * Helper to create a single file in a project with an ACL that grants download
	 * to the user.
	 * 
	 * @return
	 */
	public Node createFileHierarchy(ACCESS_TYPE... accessTypes) {
		Node project = nodeDaoHelper.create((n) -> {
			n.setNodeType(EntityType.project);
			n.setName("project");
		});
		aclHelper.create((a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(user.getId(), accessTypes));
		});
		FileHandle fh = fileHandleDaoHelper.create((f) -> {
			f.setFileName("someFile");
			f.setContentSize(123L);
		});
		Node file = nodeDaoHelper.create((n) -> {
			n.setParentId(project.getId());
			n.setNodeType(EntityType.file);
			n.setName("FileName");
			n.setFileHandleId(fh.getId());
		});
		return file;
	}

}
