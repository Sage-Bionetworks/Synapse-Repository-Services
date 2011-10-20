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
import org.sagebionetworks.Dataset;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

/**
 * Unit test for Synapse.
 * @author jmhill
 *
 */
public class SynapseTest {
	
	HttpClientProvider mockProvider = null;
	HttpResponse mockResponse;
	
	Synapse synapse;
	
	@Before
	public void before() throws Exception{
		// The mock provider
		mockProvider = Mockito.mock(HttpClientProvider.class);
		mockResponse = Mockito.mock(HttpResponse.class);
		when(mockProvider.performRequest(any(String.class),any(String.class),any(String.class),(Map<String,String>)anyObject())).thenReturn(mockResponse);
		synapse = new Synapse(mockProvider);
	}
	
	@Test
	public void testCreateDataset() throws Exception {
		Dataset ds = EntityCreator.createNewDataset();
		// This is what we want returned.
		String jsonString = EntityFactory.createJSONStringForEntity(ds);
		System.out.println(jsonString);
		StringEntity responseEntity = new StringEntity(jsonString);
		// We want the mock response to return JSON for this entity.
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		// Now create an entity
		Dataset clone = synapse.createEntity(ds);
		// For this test we want return 
		assertNotNull(clone);
		// The clone should equal the original ds
		assertEquals(ds, clone);
	}

}
