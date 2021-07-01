package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.client.AsynchJobType;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListResponse;
import org.sagebionetworks.repo.model.download.AddToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddToDownloadListResponse;
import org.sagebionetworks.repo.model.download.AvailableFilesRequest;
import org.sagebionetworks.repo.model.download.AvailableFilesResponse;
import org.sagebionetworks.repo.model.download.DownloadListItem;
import org.sagebionetworks.repo.model.download.DownloadListItemResult;
import org.sagebionetworks.repo.model.download.DownloadListManifestRequest;
import org.sagebionetworks.repo.model.download.DownloadListManifestResponse;
import org.sagebionetworks.repo.model.download.DownloadListPackageRequest;
import org.sagebionetworks.repo.model.download.DownloadListPackageResponse;
import org.sagebionetworks.repo.model.download.DownloadListQueryRequest;
import org.sagebionetworks.repo.model.download.DownloadListQueryResponse;
import org.sagebionetworks.repo.model.download.FilesStatisticsRequest;
import org.sagebionetworks.repo.model.download.FilesStatisticsResponse;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListRequest;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListResponse;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.ViewType;

public class ITDownloadListControllerTest {

	public static final long MAX_WAIT_MS = 1000 * 30;
	public static final int MAX_RETIES = 10;

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userId;

	private Project project;

	@BeforeAll
	public static void beforeClass() throws Exception {
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		synapse = new SynapseClientImpl();
		userId = SynapseClientHelper.createUser(adminSynapse, synapse);
		SynapseClientHelper.setEndpoints(synapse);
	}

	@BeforeEach
	public void beforeEach() throws SynapseException {
		synapse.clearUsersDownloadList();
	}

	@AfterEach
	public void afterEach() throws SynapseException {
		if (project != null) {
			adminSynapse.deleteEntity(project);
		}
	}

	@Test
	public void testAddFilesToDownloadList() throws Exception {
		FileEntity file = setupFileEntity();
		AddBatchOfFilesToDownloadListRequest request = new AddBatchOfFilesToDownloadListRequest()
				.setBatchToAdd(Arrays.asList(new DownloadListItem().setFileEntityId(file.getId())));
		// call under test
		AddBatchOfFilesToDownloadListResponse response = synapse.addFilesToDownloadList(request);
		AddBatchOfFilesToDownloadListResponse expected = new AddBatchOfFilesToDownloadListResponse()
				.setNumberOfFilesAdded(1L);
		assertEquals(expected, response);
	}

	@Test
	public void testRemoveFilesToDownloadList() throws Exception {
		FileEntity file = setupFileEntity();
		AddBatchOfFilesToDownloadListRequest request = new AddBatchOfFilesToDownloadListRequest()
				.setBatchToAdd(Arrays.asList(new DownloadListItem().setFileEntityId(file.getId())));
		AddBatchOfFilesToDownloadListResponse addResponse = synapse.addFilesToDownloadList(request);
		AddBatchOfFilesToDownloadListResponse expectedAddResponse = new AddBatchOfFilesToDownloadListResponse()
				.setNumberOfFilesAdded(1L);
		assertEquals(expectedAddResponse, addResponse);

		// all under test
		RemoveBatchOfFilesFromDownloadListResponse removeResponse = synapse
				.removeFilesFromDownloadList(new RemoveBatchOfFilesFromDownloadListRequest()
						.setBatchToRemove(Arrays.asList(new DownloadListItem().setFileEntityId(file.getId()))));

		RemoveBatchOfFilesFromDownloadListResponse expectedRemoveResponse = new RemoveBatchOfFilesFromDownloadListResponse()
				.setNumberOfFilesRemoved(1L);
		assertEquals(expectedRemoveResponse, removeResponse);
	}

	@Test
	public void testQueryAvailableFiles() throws SynapseException {
		FileEntity file = setupFileEntity();
		AddBatchOfFilesToDownloadListRequest addRequest = new AddBatchOfFilesToDownloadListRequest()
				.setBatchToAdd(Arrays.asList(new DownloadListItem().setFileEntityId(file.getId())));
		AddBatchOfFilesToDownloadListResponse addResponse = synapse.addFilesToDownloadList(addRequest);
		AddBatchOfFilesToDownloadListResponse expectedAddResponse = new AddBatchOfFilesToDownloadListResponse()
				.setNumberOfFilesAdded(1L);
		assertEquals(expectedAddResponse, addResponse);

		DownloadListQueryRequest queryRequest = new DownloadListQueryRequest()
				.setRequestDetails(new AvailableFilesRequest());
		AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.QueryDownloadList, queryRequest, body -> {
			assertTrue(body instanceof DownloadListQueryResponse);
			DownloadListQueryResponse response = (DownloadListQueryResponse) body;
			assertTrue(response.getResponseDetails() instanceof AvailableFilesResponse);
			AvailableFilesResponse details = (AvailableFilesResponse) response.getResponseDetails();
			assertNotNull(details.getPage());
			assertEquals(1, details.getPage().size());
			DownloadListItemResult item = details.getPage().get(0);
			assertEquals(file.getId(), item.getFileEntityId());
		}, MAX_WAIT_MS).getResponse();
	}
	
	@Test
	public void testQueryStatistics() throws SynapseException {
		FileEntity file = setupFileEntity();

		Long fileSize = synapse.getRawFileHandle(file.getDataFileHandleId()).getContentSize();

		AddBatchOfFilesToDownloadListRequest addRequest = new AddBatchOfFilesToDownloadListRequest()
				.setBatchToAdd(Arrays.asList(new DownloadListItem().setFileEntityId(file.getId())));
		AddBatchOfFilesToDownloadListResponse addResponse = synapse.addFilesToDownloadList(addRequest);
		AddBatchOfFilesToDownloadListResponse expectedAddResponse = new AddBatchOfFilesToDownloadListResponse()
				.setNumberOfFilesAdded(1L);
		assertEquals(expectedAddResponse, addResponse);

		DownloadListQueryRequest queryRequest = new DownloadListQueryRequest()
				.setRequestDetails(new FilesStatisticsRequest());
		// call under test
		AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.QueryDownloadList, queryRequest, body -> {
			assertTrue(body instanceof DownloadListQueryResponse);
			DownloadListQueryResponse response = (DownloadListQueryResponse) body;
			assertTrue(response.getResponseDetails() instanceof FilesStatisticsResponse);
			FilesStatisticsResponse details = (FilesStatisticsResponse) response.getResponseDetails();
			FilesStatisticsResponse expected = new FilesStatisticsResponse().setNumberOfFilesAvailableForDownload(1L)
					.setNumberOfFilesRequiringAction(0L).setSumOfFileSizesAvailableForDownload(fileSize)
					.setTotalNumberOfFiles(1L);
			assertEquals(expected, details);
		}, MAX_WAIT_MS).getResponse();
	}

	@Test
	public void testClearDownloadList() throws Exception {
		FileEntity file = setupFileEntity();
		AddBatchOfFilesToDownloadListRequest request = new AddBatchOfFilesToDownloadListRequest()
				.setBatchToAdd(Arrays.asList(new DownloadListItem().setFileEntityId(file.getId())));
		AddBatchOfFilesToDownloadListResponse addResponse = synapse.addFilesToDownloadList(request);
		AddBatchOfFilesToDownloadListResponse expectedAddResponse = new AddBatchOfFilesToDownloadListResponse()
				.setNumberOfFilesAdded(1L);
		assertEquals(expectedAddResponse, addResponse);

		// call under test
		synapse.clearUsersDownloadList();
		
		DownloadListQueryRequest queryRequest = new DownloadListQueryRequest()
				.setRequestDetails(new FilesStatisticsRequest());
		// call under test
		AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.QueryDownloadList, queryRequest, body -> {
			assertTrue(body instanceof DownloadListQueryResponse);
			DownloadListQueryResponse response = (DownloadListQueryResponse) body;
			assertTrue(response.getResponseDetails() instanceof FilesStatisticsResponse);
			FilesStatisticsResponse details = (FilesStatisticsResponse) response.getResponseDetails();
			FilesStatisticsResponse expected = new FilesStatisticsResponse().setNumberOfFilesAvailableForDownload(0L)
					.setNumberOfFilesRequiringAction(0L).setSumOfFileSizesAvailableForDownload(0L)
					.setTotalNumberOfFiles(0L);
			assertEquals(expected, details);
		}, MAX_WAIT_MS).getResponse();
	}
	
	@Test
	public void testAddToDownloadListWithParent() throws SynapseException {
		FileEntity file = setupFileEntity();
		AddToDownloadListRequest request = new AddToDownloadListRequest().setParentId(file.getParentId());
		// call under test
		AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.AddToDownloadList, request, body -> {
			assertTrue(body instanceof AddToDownloadListResponse);
			AddToDownloadListResponse response = (AddToDownloadListResponse) body;
			assertEquals(1L, response.getNumberOfFilesAdded());
		}, MAX_WAIT_MS).getResponse();
	}
	
	@Test
	public void testAddToDownloadListWithViewQuery() throws SynapseException {
		long viewTypeMask = 0x01L;
		FileEntity file = setupFileEntity();
		// create a view of the file.
		List<String> columnIds = synapse.getDefaultColumnsForView(ViewType.file).stream().map(c -> c.getId())
				.collect(Collectors.toList());
		EntityView view = synapse.createEntity(new EntityView().setParentId(file.getParentId())
				.setScopeIds(Arrays.asList(file.getParentId())).setColumnIds(columnIds).setViewTypeMask(viewTypeMask));
		Query query = new Query().setSql("select * from " + view.getId());
		// Wait for the view to contain the file
		QueryBundleRequest queryRequest = new QueryBundleRequest().setQuery(query).setPartMask(viewTypeMask)
				.setEntityId(view.getId());
		AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.TableQuery, queryRequest, body -> {
			assertTrue(body instanceof QueryResultBundle);
			QueryResultBundle response = (QueryResultBundle) body;
			assertNotNull(response.getQueryResult());
			assertNotNull(response.getQueryResult().getQueryResults());
			assertNotNull(response.getQueryResult().getQueryResults().getRows());
			List<Row> rows = response.getQueryResult().getQueryResults().getRows();
			assertEquals(1L, rows.size());
		}, MAX_WAIT_MS, MAX_RETIES);

		AddToDownloadListRequest request = new AddToDownloadListRequest().setQuery(query);
		// call under test
		AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.AddToDownloadList, request, body -> {
			assertTrue(body instanceof AddToDownloadListResponse);
			AddToDownloadListResponse response = (AddToDownloadListResponse) body;
			assertEquals(1L, response.getNumberOfFilesAdded());
		}, MAX_WAIT_MS, MAX_RETIES).getResponse();
	}
	
	@Test
	public void testDownloadListPackage() throws Exception {
		synapse.clearUsersDownloadList();
		
		DownloadListPackageRequest request = new DownloadListPackageRequest();
		
		String message = assertThrows(SynapseBadRequestException.class, ()->{
			// call under test
			AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.DownloadPackageList, request, body -> {
				assertTrue(body instanceof DownloadListPackageResponse);
				DownloadListPackageResponse response = (DownloadListPackageResponse) body;

			}, MAX_WAIT_MS, MAX_RETIES).getResponse();
		}).getMessage();
		assertEquals("No files are eligible for packaging.", message);
	}
	
	@Test
	public void testDownloadListManifest() throws Exception {
		synapse.clearUsersDownloadList();
		
		DownloadListManifestRequest request = new DownloadListManifestRequest();
		
		String message = assertThrows(SynapseBadRequestException.class, ()->{
			// call under test
			AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.DownloadListManifest, request, body -> {
				assertTrue(body instanceof DownloadListManifestResponse);
			}, MAX_WAIT_MS, MAX_RETIES).getResponse();
		}).getMessage();
		assertEquals("No files available for download.", message);
	}

	/**
	 * Setup a single file in a project.
	 * 
	 * @return
	 * @throws SynapseException
	 */
	FileEntity setupFileEntity() throws SynapseException {
		project = new Project();
		project.setName("ITDownloadListControllerTest.Project");
		project = synapse.createEntity(project);

		String fileHandleId = uploadFile("file's contents").getId();
		FileEntity file = new FileEntity();
		file.setDataFileHandleId(fileHandleId);
		file.setParentId(project.getId());
		return synapse.createEntity(file);
	}

	/**
	 * Helper to upload a file with the given file contents.
	 * 
	 * @param contents
	 * @return
	 * @throws SynapseException
	 */
	CloudProviderFileHandleInterface uploadFile(String contents) throws SynapseException {
		byte[] bytes = contents.getBytes(StandardCharsets.UTF_8);
		InputStream input = new ByteArrayInputStream(bytes);
		long fileSize = bytes.length;
		String fileName = "SomeFileName";
		String contentType = "text/plain; charset=us-ascii";
		Long storageLocationId = null;
		Boolean generatePreview = false;
		Boolean forceRestart = false;
		return synapse.multipartUpload(input, fileSize, fileName, contentType, storageLocationId, generatePreview,
				forceRestart);
	}
}
