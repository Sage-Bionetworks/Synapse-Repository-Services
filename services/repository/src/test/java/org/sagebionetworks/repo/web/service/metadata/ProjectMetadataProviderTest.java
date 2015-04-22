package org.sagebionetworks.repo.web.service.metadata;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.web.service.metadata.EventType;
import org.sagebionetworks.repo.web.service.metadata.ProjectMetadataProvider;

public class ProjectMetadataProviderTest {
	
	Project mockProject;
	HttpServletRequest mockRequest;
	
	@Before
	public void before(){
		// Build the mocks
		mockProject = Mockito.mock(Project.class);
		when(mockProject.getId()).thenReturn("101");
		// Now the request
		mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");
		when(mockRequest.getRequestURI()).thenReturn("/project");
		
	}
	
	// No projec specific metadata anymore
	@Ignore
	@Test
	public void testAddTypeSpecificMetadata(){
		ProjectMetadataProvider provider = new ProjectMetadataProvider();
		// Mock the dataset and the request
		Project project = new Project();
		project.setId("101");
		provider.addTypeSpecificMetadata(project, mockRequest, null, EventType.GET);
		assertEquals("/repo/v1/project/101/annotations", project.getAnnotations());
	}

}
