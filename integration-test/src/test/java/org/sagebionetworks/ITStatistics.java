package org.sagebionetworks;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.statistics.ObjectStatisticsResponse;
import org.sagebionetworks.repo.model.statistics.ProjectFilesStatisticsRequest;
import org.sagebionetworks.repo.model.statistics.ProjectFilesStatisticsResponse;

@ExtendWith(ITTestExtension.class)
public class ITStatistics {

	private static final int MONTHS_COUNT = 12;
	
	private Project project;

	private SynapseClient synapse;
    
    public ITStatistics(SynapseClient synapse) {
    	this.synapse = synapse;
	}
	
	@BeforeEach
	public void before() throws SynapseException {
		project = synapse.createEntity(new Project());
	}

	@AfterEach
	public void after() throws Exception {
		try {
			synapse.deleteEntity(project);
		} catch (SynapseNotFoundException e) {
		}
	}

	@Test
	public void testGetProjectFilesStatistics() throws SynapseException {

		ProjectFilesStatisticsRequest request = new ProjectFilesStatisticsRequest();

		request.setObjectId(project.getId());
		request.setFileDownloads(true);
		request.setFileUploads(true);

		ObjectStatisticsResponse response = synapse.getStatistics(request);
		
		assertNotNull(response);
		assertEquals(project.getId(), response.getObjectId());
		assertTrue(response instanceof ProjectFilesStatisticsResponse);
		
		ProjectFilesStatisticsResponse projectFilesStatistics = (ProjectFilesStatisticsResponse) response;
		
		assertNotNull(projectFilesStatistics.getFileDownloads());
		assertNotNull(projectFilesStatistics.getFileUploads());
		
		assertEquals(MONTHS_COUNT, projectFilesStatistics.getFileDownloads().getMonths().size());
		assertEquals(MONTHS_COUNT, projectFilesStatistics.getFileUploads().getMonths().size()); 
		
	}

}
