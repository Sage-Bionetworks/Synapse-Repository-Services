package org.sagebionetworks.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.MigratableObjectCount;
import org.sagebionetworks.repo.model.MigratableObjectData;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;


public class SynapseAdministrationTest {
	HttpClientProvider mockProvider = null;
	DataUploader mockUploader = null;
	HttpResponse mockResponse;
	
	SynapseAdministration synapse;
	
	@Before
	public void before() throws Exception{
		// The mock provider
		mockProvider = Mockito.mock(HttpClientProvider.class);
		mockUploader = Mockito.mock(DataUploaderMultipartImpl.class);
		mockResponse = Mockito.mock(HttpResponse.class);
		when(mockProvider.performRequest(any(String.class),any(String.class),any(String.class),(Map<String,String>)anyObject())).thenReturn(mockResponse);
		synapse = new SynapseAdministration(mockProvider, mockUploader);
	}
	
	@Test
	public void testGetAllMigratableObjectsPaginated() throws Exception {
		PaginatedResults<MigratableObjectData> p = new PaginatedResults<MigratableObjectData>();
		// This is what we want returned.
		String jsonString = EntityFactory.createJSONStringForEntity(p);

		StringEntity responseEntity = new StringEntity(jsonString);
		// We want the mock response to return JSON for this entity.
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		PaginatedResults<MigratableObjectData> clone = synapse.getAllMigratableObjectsPaginated(0, 100, true);
		// For this test we want return 
		assertNotNull(clone);
		// The clone should equal the original ds
		assertEquals(p, clone);
	}
	
	@Test
	public void testGetMigratableObjectCounts() throws Exception {
		PaginatedResults<MigratableObjectCount> p = new PaginatedResults<MigratableObjectCount>();
		String expectedJSONResult = EntityFactory.createJSONStringForEntity(p);
		StringEntity responseEntity = new StringEntity(expectedJSONResult);
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		PaginatedResults<MigratableObjectCount> clone = synapse.getMigratableObjectCounts(0, 100, true);
		assertNotNull(clone);
		assertEquals(p, clone);
	}
}
