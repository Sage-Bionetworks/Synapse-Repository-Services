package org.sagebionetworks.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

/**
 * Unit test for Synapse.
 * @author jmhill
 *
 */
public class SynapseTest {
	
	HttpClientProvider mockProvider = null;
	DataUploader mockUploader = null;
	HttpResponse mockResponse;
	
	Synapse synapse;
	
	@Before
	public void before() throws Exception{
		// The mock provider
		mockProvider = Mockito.mock(HttpClientProvider.class);
		mockUploader = Mockito.mock(DataUploaderMultipartImpl.class);
		mockResponse = Mockito.mock(HttpResponse.class);
		when(mockProvider.performRequest(any(String.class),any(String.class),any(String.class),(Map<String,String>)anyObject())).thenReturn(mockResponse);
		synapse = new Synapse(mockProvider, mockUploader);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateNullEntity() throws Exception{
		synapse.createEntity(null);
	}
	
	@Test
	public void testCreateStudyEntity() throws Exception {
		Study ds = EntityCreator.createNewDataset();
		// This is what we want returned.
		String jsonString = EntityFactory.createJSONStringForEntity(ds);
//		System.out.println(jsonString);
		StringEntity responseEntity = new StringEntity(jsonString);
		// We want the mock response to return JSON for this entity.
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		// Now create an entity
		Study clone = synapse.createEntity(ds);
		// For this test we want return 
		assertNotNull(clone);
		// The clone should equal the original ds
		assertEquals(ds, clone);
	}
	
	@Test
	public void testCreateFolderEntity() throws Exception {
		Folder fl = EntityCreator.createNewFolder();
		// This is what we want returned.
		String jsonString = EntityFactory.createJSONStringForEntity(fl);
		StringEntity responseEntity = new StringEntity(jsonString);
		// We want the mock response to return JSON for this entity.
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		// Now create an entity
		Folder clone = synapse.createEntity(fl);
		// For this test we want return 
		assertNotNull(clone);
		// The clone should equal the original fl
		assertEquals(fl, clone);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetEntityNullId() throws Exception{
		synapse.getEntity(null, Study.class);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetEntityNullClass() throws Exception{
		synapse.getEntity("123", null);
	}
	
	@Test
	public void testGetEntity() throws Exception {
		Study ds = EntityCreator.createNewDataset();
		// This is what we want returned.
		String jsonString = EntityFactory.createJSONStringForEntity(ds);

		StringEntity responseEntity = new StringEntity(jsonString);
		// We want the mock response to return JSON for this entity.
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		Study clone = synapse.getEntity(ds.getId(), Study.class);
		// For this test we want return 
		assertNotNull(clone);
		// The clone should equal the original ds
		assertEquals(ds, clone);
	}
	
	@Test
	public void testGetBenefactor() throws Exception {
		EntityHeader eh = new EntityHeader();
		eh.setId("syn101");
		// This is what we want returned.
		String jsonString = EntityFactory.createJSONStringForEntity(eh);

		StringEntity responseEntity = new StringEntity(jsonString);
		// We want the mock response to return JSON for this entity.
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		EntityHeader clone = synapse.getEntityBenefactor("syn101");
		// For this test we want return 
		assertNotNull(clone);
		// The clone should equal the original ds
		assertEquals(eh, clone);
	}
	
	@Test
	public void testCanAccess() throws Exception {
		Study ds = EntityCreator.createNewDataset();
		String jsonString = "{\"result\":true}";
		StringEntity responseEntity = new StringEntity(jsonString);
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		assertTrue(synapse.canAccess(ds.getId(), ACCESS_TYPE.READ));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testPutEntityNull() throws Exception {
		synapse.putEntity(null);
	}
	
	@Test
	public void testPutEntity() throws Exception {
		Study ds = EntityCreator.createNewDataset();
		// This is what we want returned.
		String jsonString = EntityFactory.createJSONStringForEntity(ds);
//		System.out.println(jsonString);
		StringEntity responseEntity = new StringEntity(jsonString);
		// We want the mock response to return JSON for this entity.
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		Study clone = synapse.putEntity(ds);
		// For this test we want return 
		assertNotNull(clone);
		// The clone should equal the original ds
		assertEquals(ds, clone);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testDeleteEntityNull() throws Exception{
		synapse.deleteEntity((Entity)null);
	}

	@Test
	public void testGetEntityPath() throws Exception {
		Data layer = new Data();
		// create test hierarchy
		
		EntityHeader layerHeader = new EntityHeader();
		layerHeader.setId("layerid");
		layerHeader.setName("layer name");
		layerHeader.setType("/layer");	
		List<EntityHeader> entityHeaders = new ArrayList<EntityHeader>();
		entityHeaders.add(layerHeader);
		EntityPath entityPath = new EntityPath();
		entityPath.setPath(entityHeaders);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		entityPath.writeToJSONObject(adapter);
		
		// We want the mock response to return JSON for the hierarchy.
		StringEntity responseEntity = new StringEntity(adapter.toJSONString());
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		
		// execute and verify
		EntityPath returnedEntityPath = synapse.getEntityPath(layer);
		List<EntityHeader> returnedHeaders = returnedEntityPath.getPath();
		
		assertEquals(1, returnedHeaders.size());
		EntityHeader firstHeader = returnedHeaders.get(0);
		assertEquals(layerHeader, firstHeader);
		
	}
	
	@Test
	public void testGetEntityReferencedBy() throws Exception {							
		Project proj2 = new Project();
		proj2.setId("5");
		proj2.setName("proj2");
		
		EntityHeader proj1Header = new EntityHeader();
		proj1Header.setId("id");
		proj1Header.setName("name");
		proj1Header.setType("type");		
		List<EntityHeader> eHeaderList = new ArrayList<EntityHeader>();
		eHeaderList.add(proj1Header);		
		
		PaginatedResults<EntityHeader> paginatedResult = new PaginatedResults<EntityHeader>();
		paginatedResult.setResults(eHeaderList);
		paginatedResult.setTotalNumberOfResults(1);
		paginatedResult.setPaging(new HashMap<String, String>());
				
		// setup mock
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		paginatedResult.writeToJSONObject(adapter);		
		StringEntity responseEntity = new StringEntity(adapter.toJSONString());
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		
		
		PaginatedResults<EntityHeader> realResults = synapse.getEntityReferencedBy(proj2);
		
		assertEquals(1, realResults.getTotalNumberOfResults());
		EntityHeader firstHeader = realResults.getResults().get(0);
		assertEquals(proj1Header, firstHeader);
	}		
	
	@Test
	public void testCreateAccessRequirement() throws Exception {
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		ar.setEntityType(ar.getClass().getName());
		ar.setEntityIds(new ArrayList<String>());
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setTermsOfUse("foo");
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		ar.writeToJSONObject(adapter);
		StringEntity responseEntity = new StringEntity(adapter.toJSONString());
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		synapse.createAccessRequirement(ar);
	}
	
	@Test
	public void testCreateAccessApproval() throws Exception {
		TermsOfUseAccessApproval aa = new TermsOfUseAccessApproval();
		aa.setEntityType(aa.getClass().getName());
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		aa.writeToJSONObject(adapter);
		StringEntity responseEntity = new StringEntity(adapter.toJSONString());
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		synapse.createAccessApproval(aa);
	}
}
