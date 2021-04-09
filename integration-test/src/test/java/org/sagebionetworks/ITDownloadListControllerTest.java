package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.client.AsynchJobType;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListResponse;
import org.sagebionetworks.repo.model.download.AvailableFilesRequest;
import org.sagebionetworks.repo.model.download.AvailableFilesResponse;
import org.sagebionetworks.repo.model.download.DownloadListItem;
import org.sagebionetworks.repo.model.download.DownloadListItemResult;
import org.sagebionetworks.repo.model.download.DownloadListQueryRequest;
import org.sagebionetworks.repo.model.download.DownloadListQueryResponse;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListRequest;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListResponse;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;

public class ITDownloadListControllerTest {

	public static final long MAX_WAIT_MS = 1000 * 30;

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
	public void beforeEach() {

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
			assertTrue(response.getReponseDetails() instanceof AvailableFilesResponse);
			AvailableFilesResponse details = (AvailableFilesResponse) response.getReponseDetails();
			assertNotNull(details.getPage());
			assertEquals(1, details.getPage().size());
			DownloadListItemResult item = details.getPage().get(0);
			assertEquals(file.getId(), item.getFileEntityId());
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

		// all under test
		synapse.clearDownloadList();

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
