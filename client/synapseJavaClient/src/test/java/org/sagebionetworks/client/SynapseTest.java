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
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.Entity;
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
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateEntityNull() throws Exception{
		synapse.createEntity(null);
	}
	
	@Test
	public void testCreateEntity() throws Exception {
		Dataset ds = EntityCreator.createNewDataset();
		// This is what we want returned.
		String jsonString = EntityFactory.createJSONStringForEntity(ds);
//		System.out.println(jsonString);
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
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetEntityNullId() throws Exception{
		synapse.getEntity(null, Dataset.class);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetEntityNullClass() throws Exception{
		synapse.getEntity("123", Dataset.class);
	}
	
	@Test
	public void testGetEntity() throws Exception {
		Dataset ds = EntityCreator.createNewDataset();
		// This is what we want returned.
		String jsonString = EntityFactory.createJSONStringForEntity(ds);
//		System.out.println(jsonString);
		StringEntity responseEntity = new StringEntity(jsonString);
		// We want the mock response to return JSON for this entity.
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		Dataset clone = synapse.getEntity(ds.getId(), Dataset.class);
		// For this test we want return 
		assertNotNull(clone);
		// The clone should equal the original ds
		assertEquals(ds, clone);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testPutEntityNull() throws Exception {
		synapse.putEntity(null);
	}
	
	@Test
	public void testPutEntity() throws Exception {
		Dataset ds = EntityCreator.createNewDataset();
		// This is what we want returned.
		String jsonString = EntityFactory.createJSONStringForEntity(ds);
//		System.out.println(jsonString);
		StringEntity responseEntity = new StringEntity(jsonString);
		// We want the mock response to return JSON for this entity.
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		Dataset clone = synapse.putEntity(ds);
		// For this test we want return 
		assertNotNull(clone);
		// The clone should equal the original ds
		assertEquals(ds, clone);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testDeleteEntityNull() throws Exception{
		synapse.deleteEntity((Entity)null);
	}

}
