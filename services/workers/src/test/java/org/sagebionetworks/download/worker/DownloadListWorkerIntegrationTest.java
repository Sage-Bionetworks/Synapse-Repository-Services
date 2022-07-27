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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.download.DownloadListManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.trash.TrashManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityRef;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.file.download.v2.DownloadListDAO;
import org.sagebionetworks.repo.model.download.ActionRequiredCount;
import org.sagebionetworks.repo.model.download.ActionRequiredRequest;
import org.sagebionetworks.repo.model.download.ActionRequiredResponse;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListResponse;
import org.sagebionetworks.repo.model.download.AddToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddToDownloadListResponse;
import org.sagebionetworks.repo.model.download.AvailableFilesRequest;
import org.sagebionetworks.repo.model.download.AvailableFilesResponse;
import org.sagebionetworks.repo.model.download.DownloadListItem;
import org.sagebionetworks.repo.model.download.DownloadListItemResult;
import org.sagebionetworks.repo.model.download.DownloadListQueryRequest;
import org.sagebionetworks.repo.model.download.DownloadListQueryResponse;
import org.sagebionetworks.repo.model.download.FilesStatisticsRequest;
import org.sagebionetworks.repo.model.download.FilesStatisticsResponse;
import org.sagebionetworks.repo.model.download.RequestDownload;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.helper.AccessControlListObjectHelper;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.Dataset;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.SnapshotRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
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
	private DownloadListManager downloadListmanager;
	@Autowired
	private TrashManager trashManager;
	@Autowired
	private ColumnModelManager columnModelManager;
	
	@Autowired
	AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;

	UserInfo adminUser;
	UserInfo user;
	ColumnModel datasetColumn;

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
		datasetColumn = new ColumnModel();
		datasetColumn.setName("aString");
		datasetColumn.setColumnType(ColumnType.STRING);
		datasetColumn.setMaximumSize(50L);
		datasetColumn = columnModelManager.createColumnModel(user, datasetColumn);
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
		// PLFM-70523 generates a NPE here:
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
		List<EntityRef> items = Arrays.asList(new EntityRef().setEntityId(file.getId())
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
	
	// Set of tests to reproduce issues in https://sagebionetworks.jira.com/browse/PLFM-7263
	
	// Case when we add a DELETED file using a batch action
	@Test
	public void testAddBatchWithDeletedItem() throws Exception {
		Node file = createFileHierarchy(ACCESS_TYPE.READ, ACCESS_TYPE.DOWNLOAD);
		
		// Add another file to the hierarchy, we will delete this
		Node deletedFile = nodeDaoHelper.create((n) -> {
			n.setParentId(file.getParentId());
			n.setNodeType(EntityType.file);
			n.setName("DeletedFile");
			n.setFileHandleId(file.getFileHandleId());
		});
		
		// We delete the file before adding it
		nodeDao.delete(deletedFile.getId());
				
		// Call under test
		AddBatchOfFilesToDownloadListResponse result = downloadListmanager.addBatchOfFilesToDownloadList(user, new AddBatchOfFilesToDownloadListRequest().setBatchToAdd(List.of(
			new DownloadListItem().setFileEntityId(file.getId()),
			new DownloadListItem().setFileEntityId(deletedFile.getId())
		)));
		
		// Only one should have been added
		assertEquals(1, result.getNumberOfFilesAdded());
		
	}
	
	// Case when we add a TRASHED file using a batch action
	@Test
	// This was discovered while researching https://sagebionetworks.jira.com/browse/PLFM-7263, should be enabled to test the fix for https://sagebionetworks.jira.com/browse/PLFM-7416 
	// or adjusted if we decide to keep it this way
	@Disabled
	public void testAddBatchWithTrashedItem() throws Exception {
		Node file = createFileHierarchy(ACCESS_TYPE.READ, ACCESS_TYPE.DOWNLOAD, ACCESS_TYPE.DELETE);
		
		// Add another file to the hierarchy, we will delete this
		Node trashedFile = nodeDaoHelper.create((n) -> {
			n.setParentId(file.getParentId());
			n.setNodeType(EntityType.file);
			n.setName("DeletedFile");
			n.setFileHandleId(file.getFileHandleId());
		});
		
		// We trash the file before adding it
		trashManager.moveToTrash(user, trashedFile.getId(), false);
				
		// Call under test
		AddBatchOfFilesToDownloadListResponse result = downloadListmanager.addBatchOfFilesToDownloadList(user, new AddBatchOfFilesToDownloadListRequest().setBatchToAdd(List.of(
			new DownloadListItem().setFileEntityId(file.getId()),
			new DownloadListItem().setFileEntityId(trashedFile.getId())
		)));
		
		// PLFM-7263: This returns 2 instead of 1
		assertEquals(1, result.getNumberOfFilesAdded());
		
	}
	
	// Case when we add dataset items with a deleted file in it
	@Test
	// This was discovered while researching https://sagebionetworks.jira.com/browse/PLFM-7263, should be enabled to test the fix for https://sagebionetworks.jira.com/browse/PLFM-7416
	// or adjusted if we decide to keep it this way
	@Disabled
	public void testAddFromDatasetWithDeletedFile() throws Exception {
		// create file in a project with read access
		Node file = createFileHierarchy(ACCESS_TYPE.DOWNLOAD, ACCESS_TYPE.READ);
		
		// Add another file to the hierarchy, we will delete this
		Node deletedFile = nodeDaoHelper.create((n) -> {
			n.setParentId(file.getParentId());
			n.setNodeType(EntityType.file);
			n.setName("DeletedFile");
			n.setFileHandleId(file.getFileHandleId());
		});
		
		// add file to a data set
		List<EntityRef> items = Arrays.asList(
			new EntityRef().setEntityId(file.getId()).setVersionNumber(file.getVersionNumber()), 
			new EntityRef().setEntityId(deletedFile.getId()).setVersionNumber(deletedFile.getVersionNumber())
		);
		
		Node dataset = nodeDaoHelper.create(n -> {
			n.setName("aDataset");
			n.setParentId(file.getParentId());
			n.setNodeType(EntityType.dataset);
			n.setItems(items);
		});

		// We delete the file before adding it
		nodeDao.delete(deletedFile.getId());
		
		AddToDownloadListRequest addRequest = new AddToDownloadListRequest().setParentId(dataset.getId());
		
		// call under test
		asynchronousJobWorkerHelper.assertJobResponse(user, addRequest, (AddToDownloadListResponse response) -> {
			// PLFM-7263: This fails returning 2 instead of 1
			assertEquals(1L, response.getNumberOfFilesAdded());
		}, MAX_WAIT_MS, MAX_RETRIES);
	}
	
	// Case when we add dataset items with a trashed file in it
	@Test
	// This was discovered while researching https://sagebionetworks.jira.com/browse/PLFM-7263, should be enabled to test the fix for https://sagebionetworks.jira.com/browse/PLFM-7416
	// or adjusted if we decide to keep it this way
	@Disabled
	public void testAddFromDatasetWithTrashedFile() throws Exception {
		// create file in a project with read access
		Node file = createFileHierarchy(ACCESS_TYPE.DOWNLOAD, ACCESS_TYPE.READ, ACCESS_TYPE.DELETE);
		
		// Add another file to the hierarchy, we will delete this
		Node trashedFile = nodeDaoHelper.create((n) -> {
			n.setParentId(file.getParentId());
			n.setNodeType(EntityType.file);
			n.setName("DeletedFile");
			n.setFileHandleId(file.getFileHandleId());
		});
		
		// add file to a data set
		List<EntityRef> items = Arrays.asList(
			new EntityRef().setEntityId(file.getId()).setVersionNumber(file.getVersionNumber()), 
			new EntityRef().setEntityId(trashedFile.getId()).setVersionNumber(trashedFile.getVersionNumber())
		);
		
		Node dataset = nodeDaoHelper.create(n -> {
			n.setName("aDataset");
			n.setParentId(file.getParentId());
			n.setNodeType(EntityType.dataset);
			n.setItems(items);
		});

		// We trash the file before adding it
		trashManager.moveToTrash(user, trashedFile.getId(), false);
		
		AddToDownloadListRequest addRequest = new AddToDownloadListRequest().setParentId(dataset.getId());
		
		// call under test
		asynchronousJobWorkerHelper.assertJobResponse(user, addRequest, (AddToDownloadListResponse response) -> {
			// PLFM-7263: This fails returning 2 instead of 1
			assertEquals(1L, response.getNumberOfFilesAdded());
		}, MAX_WAIT_MS, MAX_RETRIES);
	}
	
	// Case when we add items from a query on a snapshot and there are deleted files in it
	@Test
	// This was discovered while researching https://sagebionetworks.jira.com/browse/PLFM-7263, should be enabled to test the fix for https://sagebionetworks.jira.com/browse/PLFM-7416
	// or adjusted if we decide to keep it this way
	@Disabled
	public void testAddFromQueryOnSnapshotWithDeletedFile() throws Exception {
		// create file in a project with read access
		Node file = createFileHierarchy(ACCESS_TYPE.DOWNLOAD, ACCESS_TYPE.READ, ACCESS_TYPE.DELETE);
		
		// Add another file to the hierarchy, we will delete this
		Node deletedFile = nodeDaoHelper.create((n) -> {
			n.setParentId(file.getParentId());
			n.setNodeType(EntityType.file);
			n.setName("DeletedFile");
			n.setFileHandleId(file.getFileHandleId());
		});
		
		// add file to a data set
		List<EntityRef> items = Arrays.asList(
			new EntityRef().setEntityId(file.getId()).setVersionNumber(file.getVersionNumber()), 
			new EntityRef().setEntityId(deletedFile.getId()).setVersionNumber(deletedFile.getVersionNumber())
		);

		Dataset dataset = asynchronousJobWorkerHelper.createDataset(adminUser, 
			new Dataset().setParentId(file.getParentId()).setName("aDataset").setItems(items).setColumnIds(List.of(datasetColumn.getId())));

		asynchronousJobWorkerHelper.assertQueryResult(adminUser, "SELECT * FROM " + dataset.getId(), (QueryResultBundle result) -> {
			assertEquals(2L, result.getQueryResult().getQueryResults().getRows().size());
		}, MAX_WAIT_MS);		
		
		// We create a snapshot
		TableUpdateTransactionRequest transactionRequest = new TableUpdateTransactionRequest();
		transactionRequest.setEntityId(dataset.getId());
		transactionRequest.setCreateSnapshot(true);
		transactionRequest.setSnapshotOptions(new SnapshotRequest().setSnapshotComment("snapshot"));
		
		TableUpdateTransactionResponse snapshotResponse = asynchronousJobWorkerHelper.assertJobResponse(adminUser, transactionRequest, (TableUpdateTransactionResponse response) -> {
			assertNotNull(response.getSnapshotVersionNumber());
		}, MAX_WAIT_MS).getResponse();

		asynchronousJobWorkerHelper.assertQueryResult(adminUser, "SELECT * FROM " + dataset.getId()  + "." + snapshotResponse.getSnapshotVersionNumber(), (QueryResultBundle result) -> {
			assertEquals(2L, result.getQueryResult().getQueryResults().getRows().size());
		}, MAX_WAIT_MS);
		
		// Now delete one of the files
		nodeDao.delete(deletedFile.getId());

		AddToDownloadListRequest addRequest = new AddToDownloadListRequest().setQuery(new Query().setSql("SELECT * FROM " + dataset.getId() + "." + snapshotResponse.getSnapshotVersionNumber()));
		
		// call under test
		asynchronousJobWorkerHelper.assertJobResponse(adminUser, addRequest, (AddToDownloadListResponse response) -> {
			// PLFM-7263: This fails returning 2 instead of 1
			assertEquals(1L, response.getNumberOfFilesAdded());
		}, MAX_WAIT_MS, MAX_RETRIES);
	}
	
	// Case when we add items from a query on a snapshot and there are trashed files in it
	@Test
	// This was discovered while researching https://sagebionetworks.jira.com/browse/PLFM-7263, should be enabled to test the fix for https://sagebionetworks.jira.com/browse/PLFM-7416
	// or adjusted if we decide to keep it this way
	@Disabled
	public void testAddFromQueryOnSnapshotWithTrashedFile() throws Exception {
		// create file in a project with read access
		Node file = createFileHierarchy(ACCESS_TYPE.DOWNLOAD, ACCESS_TYPE.READ, ACCESS_TYPE.DELETE);
		
		// Add another file to the hierarchy, we will move this to the trash
		Node trashedFile = nodeDaoHelper.create((n) -> {
			n.setParentId(file.getParentId());
			n.setNodeType(EntityType.file);
			n.setName("DeletedFile");
			n.setFileHandleId(file.getFileHandleId());
		});
		
		// add file to a data set
		List<EntityRef> items = Arrays.asList(
			new EntityRef().setEntityId(file.getId()).setVersionNumber(file.getVersionNumber()), 
			new EntityRef().setEntityId(trashedFile.getId()).setVersionNumber(trashedFile.getVersionNumber())
		);

		Dataset dataset = asynchronousJobWorkerHelper.createDataset(adminUser, 
			new Dataset().setParentId(file.getParentId()).setName("aDataset").setItems(items).setColumnIds(List.of(datasetColumn.getId())));

		asynchronousJobWorkerHelper.assertQueryResult(adminUser, "SELECT * FROM " + dataset.getId(), (QueryResultBundle result) -> {
			assertEquals(2L, result.getQueryResult().getQueryResults().getRows().size());
		}, MAX_WAIT_MS);		
		
		// We create a snapshot
		TableUpdateTransactionRequest transactionRequest = new TableUpdateTransactionRequest();
		transactionRequest.setEntityId(dataset.getId());
		transactionRequest.setCreateSnapshot(true);
		transactionRequest.setSnapshotOptions(new SnapshotRequest().setSnapshotComment("snapshot"));
		
		TableUpdateTransactionResponse snapshotResponse = asynchronousJobWorkerHelper.assertJobResponse(adminUser, transactionRequest, (TableUpdateTransactionResponse response) -> {
			assertNotNull(response.getSnapshotVersionNumber());
		}, MAX_WAIT_MS).getResponse();

		asynchronousJobWorkerHelper.assertQueryResult(adminUser, "SELECT * FROM " + dataset.getId()  + "." + snapshotResponse.getSnapshotVersionNumber(), (QueryResultBundle result) -> {
			assertEquals(2L, result.getQueryResult().getQueryResults().getRows().size());
		}, MAX_WAIT_MS);

		// Now trash one of the files
		trashManager.moveToTrash(user, trashedFile.getId(), false);

		AddToDownloadListRequest addRequest = new AddToDownloadListRequest().setQuery(new Query().setSql("SELECT * FROM " + dataset.getId() + "." + snapshotResponse.getSnapshotVersionNumber()));
		
		// call under test
		asynchronousJobWorkerHelper.assertJobResponse(adminUser, addRequest, (AddToDownloadListResponse response) -> {
			// PLFM-7263: This fails returning 2 instead of 1
			assertEquals(1L, response.getNumberOfFilesAdded());
		}, MAX_WAIT_MS, MAX_RETRIES);
	}
	
	// Case for statistics on files when a file on the download list is deleted 
	@Test
	// This was discovered while researching https://sagebionetworks.jira.com/browse/PLFM-7263, the behavior should be consistent with trashed case (See testStatisticsWithTrashedItem)
	// no matter which way we decide to go
	@Disabled
	public void testStatisticsWithDeletedItem() throws Exception {
		Node file = createFileHierarchy(ACCESS_TYPE.READ, ACCESS_TYPE.DOWNLOAD);
		
		// Add another file to the hierarchy, we will delete this
		Node deletedFile = nodeDaoHelper.create((n) -> {
			n.setParentId(file.getParentId());
			n.setNodeType(EntityType.file);
			n.setName("DeletedFile");
			n.setFileHandleId(file.getFileHandleId());
		});
				
		// Call under test
		AddBatchOfFilesToDownloadListResponse result = downloadListmanager.addBatchOfFilesToDownloadList(user, new AddBatchOfFilesToDownloadListRequest().setBatchToAdd(List.of(
			new DownloadListItem().setFileEntityId(file.getId()),
			new DownloadListItem().setFileEntityId(deletedFile.getId())
		)));

		// Both files should have been added
		assertEquals(2, result.getNumberOfFilesAdded());
		
		// We now delete the file after adding it
		nodeDao.delete(deletedFile.getId());		
		
		asynchronousJobWorkerHelper.assertJobResponse(user, new DownloadListQueryRequest().setRequestDetails(new FilesStatisticsRequest()), (DownloadListQueryResponse response) -> {
			FilesStatisticsResponse details = (FilesStatisticsResponse) response.getResponseDetails();
			assertEquals(1, details.getNumberOfFilesAvailableForDownload());
			// The deleted file is not available for download and needs to be removed from the download list
			assertEquals(1, details.getNumberOfFilesRequiringAction());
			// Note that the deleted file is still in the download list
			assertEquals(2, details.getTotalNumberOfFiles());
		}, MAX_WAIT_MS, MAX_RETRIES);
		
		// One file should now be available for download
		asynchronousJobWorkerHelper.assertJobResponse(user, new DownloadListQueryRequest().setRequestDetails(new AvailableFilesRequest()), (DownloadListQueryResponse response) -> {
			AvailableFilesResponse details = (AvailableFilesResponse) response.getResponseDetails();
			assertEquals(1, details.getPage().size());
		}, MAX_WAIT_MS, MAX_RETRIES);
		
		// No file should require any action for download
		asynchronousJobWorkerHelper.assertJobResponse(user, new DownloadListQueryRequest().setRequestDetails(new ActionRequiredRequest()), (DownloadListQueryResponse response) -> {
			ActionRequiredResponse details = (ActionRequiredResponse) response.getResponseDetails();
			// PLFM-7263: This fails with a 0 and it is not consistent with the FilesStatisticsRequest
			assertEquals(1, details.getPage().size());
		}, MAX_WAIT_MS, MAX_RETRIES);
		
	}
	
	@Test
	public void testStatisticsWithTrashedItem() throws Exception {
		Node file = createFileHierarchy(ACCESS_TYPE.READ, ACCESS_TYPE.DOWNLOAD, ACCESS_TYPE.DELETE);
		
		// Add another file to the hierarchy, we will delete this
		Node trashedFile = nodeDaoHelper.create((n) -> {
			n.setParentId(file.getParentId());
			n.setNodeType(EntityType.file);
			n.setName("DeletedFile");
			n.setFileHandleId(file.getFileHandleId());
		});
				
		// Call under test
		AddBatchOfFilesToDownloadListResponse result = downloadListmanager.addBatchOfFilesToDownloadList(user, new AddBatchOfFilesToDownloadListRequest().setBatchToAdd(List.of(
			new DownloadListItem().setFileEntityId(file.getId()),
			new DownloadListItem().setFileEntityId(trashedFile.getId())
		)));

		// Both files should have been added
		assertEquals(2, result.getNumberOfFilesAdded());
		
		// We now trash the file after adding it
		trashManager.moveToTrash(user, trashedFile.getId(), false);
		
		asynchronousJobWorkerHelper.assertJobResponse(user, new DownloadListQueryRequest().setRequestDetails(new FilesStatisticsRequest()), (DownloadListQueryResponse response) -> {
			FilesStatisticsResponse details = (FilesStatisticsResponse) response.getResponseDetails();
			assertEquals(1, details.getNumberOfFilesAvailableForDownload());
			// The trashed file is not available for download and needs to be removed from the download list
			assertEquals(1, details.getNumberOfFilesRequiringAction());
			// Note that the trashed file is still in the download list
			assertEquals(2, details.getTotalNumberOfFiles());
		}, MAX_WAIT_MS, MAX_RETRIES);
		
		// One file should now be available for download
		asynchronousJobWorkerHelper.assertJobResponse(user, new DownloadListQueryRequest().setRequestDetails(new AvailableFilesRequest()), (DownloadListQueryResponse response) -> {
			AvailableFilesResponse details = (AvailableFilesResponse) response.getResponseDetails();
			assertEquals(1, details.getPage().size());
		}, MAX_WAIT_MS, MAX_RETRIES);
		
		// No file should require any action for download
		asynchronousJobWorkerHelper.assertJobResponse(user, new DownloadListQueryRequest().setRequestDetails(new ActionRequiredRequest()), (DownloadListQueryResponse response) -> {
			ActionRequiredResponse details = (ActionRequiredResponse) response.getResponseDetails();
			assertEquals(1, details.getPage().size());
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
