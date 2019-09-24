package org.sagebionetworks;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.statistics.ObjectStatisticsResponse;
import org.sagebionetworks.repo.model.statistics.ProjectFilesStatisticsRequest;
import org.sagebionetworks.repo.model.statistics.ProjectFilesStatisticsResponse;

public class ITStatistics {

	private static SynapseAdminClient adminClient;
	private static SynapseClient anonymousClient;
	private static SynapseClient client;

	private static Long userId;

	private Project project;

	@BeforeAll
	public static void beforeClass() throws Exception {
		adminClient = new SynapseAdminClientImpl();
		client = new SynapseClientImpl();
		anonymousClient = new SynapseClientImpl();
		
		SynapseClientHelper.setEndpoints(adminClient);
		SynapseClientHelper.setEndpoints(client);
		SynapseClientHelper.setEndpoints(anonymousClient);

		adminClient.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminClient.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		adminClient.clearAllLocks();
		
		// Associate the client with the user session
		userId = SynapseClientHelper.createUser(adminClient, client);
	}

	@BeforeEach
	public void before() throws SynapseException {
		project = client.createEntity(new Project());
	}

	@AfterEach
	public void after() throws Exception {
		try {
			adminClient.deleteAndPurgeEntityById(project.getId());
		} catch (SynapseNotFoundException e) {
		}
	}

	@AfterAll
	public static void afterClass() throws Exception {
		try {
			adminClient.deleteUser(userId);
		} catch (SynapseException e) {
		}
	}

	@Test
	public void testGetProjectFilesStatistics() throws SynapseException {

		ProjectFilesStatisticsRequest request = new ProjectFilesStatisticsRequest();

		request.setObjectId(project.getId());
		request.setFileDownloads(true);
		request.setFileUploads(true);

		Assertions.assertThrows(SynapseUnauthorizedException.class, () -> {
			anonymousClient.getStatistics(request);
		});

		ObjectStatisticsResponse response = client.getStatistics(request);
		
		assertNotNull(response);
		assertEquals(project.getId(), response.getObjectId());
		assertTrue(response instanceof ProjectFilesStatisticsRequest);
		
		ProjectFilesStatisticsResponse projectFilesStatistics = (ProjectFilesStatisticsResponse) response;
		
		assertNotNull(projectFilesStatistics.getFileDownloads());
		assertNotNull(projectFilesStatistics.getFileUploads());
		
		assertEquals(12, projectFilesStatistics.getFileDownloads().getMonths());
		assertEquals(12, projectFilesStatistics.getFileUploads().getMonths()); 
		
		assertNull(projectFilesStatistics.getFileDownloads().getLastUpdatedOn());
		assertNull(projectFilesStatistics.getFileUploads().getLastUpdatedOn());
		
	}

}
