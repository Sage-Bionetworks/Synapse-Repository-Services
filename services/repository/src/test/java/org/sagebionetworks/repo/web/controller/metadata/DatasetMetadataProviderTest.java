package org.sagebionetworks.repo.web.controller.metadata;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.sagebionetworks.repo.model.Dataset;

public class DatasetMetadataProviderTest {
	
	Dataset mockDs;
	HttpServletRequest mockRequest;
	
	@Before
	public void before(){
		// Build the mocks
		mockDs = Mockito.mock(Dataset.class);
		when(mockDs.getId()).thenReturn("101");
		when(mockDs.getVersion()).thenReturn(null);
		// Now the request
		mockRequest = Mockito.mock(HttpServletRequest.class);
		when(mockRequest.getServletPath()).thenReturn("/repo/v1");
		when(mockRequest.getRequestURI()).thenReturn("/dataset");
		
	}
	
	@Test
	public void testValidate(){
		DatasetMetadataProvider provider = new DatasetMetadataProvider();
		// for now datasets must have a version.  If they do not then add it.
		// The provider should set the version on the dataset
		provider.validateEntity(mockDs);
		verify(mockDs).setVersion("1.0.0");
	}
	
	@Test
	public void testAddTypeSpecificMetadata(){
		DatasetMetadataProvider provider = new DatasetMetadataProvider();
		// Mock the dataset and the request
		Dataset ds = new Dataset();
		ds.setId("101");
		provider.addTypeSpecificMetadata(ds, mockRequest);
		assertEquals("/repo/v1/dataset/101/annotations", ds.getAnnotations());
		assertEquals("/repo/v1/dataset/101/layer", ds.getLayer());
	}

}
