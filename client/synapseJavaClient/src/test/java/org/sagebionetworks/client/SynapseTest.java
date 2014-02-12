package org.sagebionetworks.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import junit.framework.Assert;

import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseTermsOfUseException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.VariableContentPaginatedResults;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.versionInfo.SynapseVersionInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.utils.HttpClientHelperException;

/**
 * Unit test for Synapse.
 * @author jmhill
 *
 */
public class SynapseTest {
	
	HttpClientProvider mockProvider = null;
	DataUploader mockUploader = null;
	HttpResponse mockResponse;
	
	SynapseClientImpl synapse;
	
	@SuppressWarnings("unchecked")
	@Before
	public void before() throws Exception{
		// The mock provider
		mockProvider = Mockito.mock(HttpClientProvider.class);
		mockUploader = Mockito.mock(DataUploaderMultipartImpl.class);
		mockResponse = Mockito.mock(HttpResponse.class);
		when(mockProvider.performRequest(any(String.class),any(String.class),any(String.class),(Map<String,String>)anyObject())).thenReturn(mockResponse);
		synapse = new SynapseClientImpl(mockProvider, mockUploader);
	}
	
	@Test
	public void testOriginatingClient() throws Exception {
		SharedClientConnection connection = synapse.getSharedClientConnection();
		Map<String,String> map = new HashMap<String,String>();
		map.put(AuthorizationConstants.DOMAIN_PARAM, DomainType.BRIDGE.name());
		String url = connection.createRequestUrl("http://localhost:8888/", "createUser", map);
		Assert.assertEquals("Origin client value appended as query string", "http://localhost:8888/createUser?originClient=BRIDGE", url);

		map.put(AuthorizationConstants.DOMAIN_PARAM, DomainType.SYNAPSE.name());
		url = connection.createRequestUrl("http://localhost:8888/", "createUser", map);
		Assert.assertEquals("Origin client value appended as query string", "http://localhost:8888/createUser?originClient=SYNAPSE", url);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateNullEntity() throws Exception{
		synapse.createEntity(null);
	}
	
	@SuppressWarnings("unchecked")
	@Test (expected=SynapseTermsOfUseException.class)
	public void testTermsOfUseNotAccepted() throws Exception{
		HttpClientHelperException simulatedHttpException = new HttpClientHelperException(){
			private static final long serialVersionUID = 1L;

			public int getHttpStatus() {return 403;};
		};
		
		when(mockProvider.performRequest(any(String.class),any(String.class),any(String.class),(Map<String,String>)anyObject())).thenThrow(simulatedHttpException);
		synapse.revalidateSession();
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
		ar.setSubjectIds(new ArrayList<RestrictableObjectDescriptor>());
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setTermsOfUse("foo");
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		ar.writeToJSONObject(adapter);
		StringEntity responseEntity = new StringEntity(adapter.toJSONString());
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		synapse.createAccessRequirement(ar);
	}
	
	@Test
	public void updateAccessRequirement() throws Exception {
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		ar.setEntityType(ar.getClass().getName());
		ar.setSubjectIds(new ArrayList<RestrictableObjectDescriptor>());
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setTermsOfUse("foo");
		ar.setId(101L);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		ar.writeToJSONObject(adapter);
		StringEntity responseEntity = new StringEntity(adapter.toJSONString());
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		synapse.updateAccessRequirement(ar);
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
	
	@Test
	public void testGetVersionInfo() throws Exception {
		SynapseVersionInfo expectedVersion = new SynapseVersionInfo();
		expectedVersion.setVersion("versionString");
		String jsonString = EntityFactory.createJSONStringForEntity(expectedVersion);
		StringEntity responseEntity = new StringEntity(jsonString);
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		SynapseVersionInfo vi = synapse.getVersionInfo();
		assertNotNull(vi);
		assertEquals(vi, expectedVersion);
	};

	@Test
	public void testGetEntityBundle() throws NameConflictException, JSONObjectAdapterException, ServletException, IOException, NotFoundException, DatastoreException, SynapseException {
		// Create an entity
		Study s = EntityCreator.createNewDataset();
		
		// Get/add/update annotations for this entity
		Annotations a = new Annotations();
		a.setEtag(s.getEtag());
		a.addAnnotation("doubleAnno", new Double(45.0001));
		a.addAnnotation("string", "A string");		
		JSONObjectAdapter adapter0 = new JSONObjectAdapterImpl();
		a.writeToJSONObject(adapter0);
		
		// We want the mock response to return JSON
		StringEntity response = new StringEntity(adapter0.toJSONString());
		when(mockResponse.getEntity()).thenReturn(response);
		synapse.updateAnnotations(s.getId(), a);
		
		// Assemble the bundle
		EntityBundle eb = new EntityBundle();
		eb.setEntity(s);
		eb.setAnnotations(a);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		eb.writeToJSONObject(adapter);
		
		// We want the mock response to return JSON
		StringEntity responseEntity = new StringEntity(adapter.toJSONString());
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		
		// Get the bundle, verify contents
		int mask =  EntityBundle.ENTITY + EntityBundle.ANNOTATIONS;
		EntityBundle eb2 = synapse.getEntityBundle(s.getId(), mask);
		
		Study s2 = (Study) eb2.getEntity();
		assertEquals("Retrieved Entity in bundle does not match original one", s, s2);
		
		Annotations a2 = eb2.getAnnotations();
		assertEquals("Retrieved Annotations in bundle do not match original ones", a, a2);
		
		UserEntityPermissions uep = eb2.getPermissions();
		assertNull("Permissions were not requested, but were returned in bundle", uep);
		
		EntityPath path = eb2.getPath();
		assertNull("Path was not requested, but was returned in bundle", path);
		
		eb2.getAccessRequirements();
		assertNull("Access Requirements were not requested, but were returned in bundle", path);
	}

	
	@Test
	public void testGetActivity() throws Exception {
		Activity act = new Activity();
		String id = "123";
		StringEntity responseEntity = createActivityStringEntity(id, act);
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		
		Activity clone = synapse.getActivity(id);
		assertNotNull(clone);
		assertEquals(act, clone);
	}	
	
	@Test
	public void testGetUnmetEvaluationAccessRequirements() throws Exception {
		VariableContentPaginatedResults<AccessRequirement> result = 
			new VariableContentPaginatedResults<AccessRequirement>();
		StringEntity responseEntity = createVCPRStringEntity(result);
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setType(RestrictableObjectType.EVALUATION);
		subjectId.setId("12345");
		VariableContentPaginatedResults<AccessRequirement> clone = 
			synapse.getUnmetAccessRequirements(subjectId);
		assertNotNull(clone);
		assertEquals(result, clone);
	}	
	
	@Test
	public void testPutActivity() throws Exception {
		Activity act = new Activity();
		String id = "123";
		StringEntity responseEntity = createActivityStringEntity(id, act);
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		
		Activity clone = synapse.putActivity(act);
		assertNotNull(clone);
		assertEquals(act, clone);
	}	
		
	@Test
	public void testDeleteActivity() throws Exception {
		String id = "123";
		synapse.deleteActivity(id);		
		verify(mockResponse).getEntity();
	}		
	
	@Test
	public void testGetActivityForEntity() throws Exception {
		Activity act = new Activity();
		String id = "123";
		StringEntity responseEntity = createActivityStringEntity(id, act);
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		
		String entityId = "syn456";
		Activity clone = synapse.getActivityForEntity(entityId); 
		assertNotNull(clone);
		assertEquals(act, clone);		
	}

	@Test
	public void testGetActivityForEntityVersion() throws Exception {
		Activity act = new Activity();
		String id = "123";
		StringEntity responseEntity = createActivityStringEntity(id, act);
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		
		String entityId = "syn456";
		Activity clone = synapse.getActivityForEntityVersion(entityId, 1L); 
		assertNotNull(clone);
		assertEquals(act, clone);		
	}

	@Test
	public void testSetActivityForEntity() throws Exception {
		Activity act = new Activity();
		String id = "123";
		StringEntity responseEntity = createActivityStringEntity(id, act);
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		
		String entityId = "syn456";
		Activity clone = synapse.setActivityForEntity(entityId, id); 
		assertNotNull(clone);
		assertEquals(act, clone);				
	}

	@Test
	public void testDeleteGeneratedByForEntity() throws Exception {
		Activity act = new Activity();
		String id = "123";
		StringEntity responseEntity = createActivityStringEntity(id, act);
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		
		String entityId = "syn456";
		synapse.deleteGeneratedByForEntity(entityId); 
		verify(mockResponse, atLeast(1)).getEntity();
	}
	
	@Test
	public void testUserAgent() throws SynapseException, JSONObjectAdapterException, UnsupportedEncodingException{
		// The user agent 
		SynapseVersionInfo info = new SynapseVersionInfo();
		info.setVersion("someversion");
		when(mockResponse.getEntity()).thenReturn(new StringEntity(EntityFactory.createJSONStringForEntity(info)));
		StubHttpClientProvider stubProvider = new StubHttpClientProvider(mockResponse);
		synapse = new SynapseClientImpl(stubProvider, mockUploader);
		// Make a call and ensure 
		synapse.getVersionInfo();
		// Validate that the User-Agent was sent
		Map<String, String> sentHeaders = stubProvider.getSentRequestHeaders();
		String value = sentHeaders.get("User-Agent");
		assertNotNull(value);
		assertTrue(value.startsWith(SynapseClientImpl.SYNPASE_JAVA_CLIENT));
 	}
	
	@Test
	public void testAppendUserAgent() throws SynapseException, JSONObjectAdapterException, UnsupportedEncodingException{
		// The user agent 
		SynapseVersionInfo info = new SynapseVersionInfo();
		info.setVersion("someversion");
		when(mockResponse.getEntity()).thenReturn(new StringEntity(EntityFactory.createJSONStringForEntity(info)));
		StubHttpClientProvider stubProvider = new StubHttpClientProvider(mockResponse);
		synapse = new SynapseClientImpl(stubProvider, mockUploader);
		// Append some user agent data
		String appended = "Appended to the User-Agent";
		synapse.appendUserAgent(appended);
		// Make a call and ensure 
		synapse.getVersionInfo();
		// Validate that the User-Agent was sent
		Map<String, String> sentHeaders = stubProvider.getSentRequestHeaders();
		String value = sentHeaders.get("User-Agent");
		System.out.println(value);
		assertNotNull(value);
		assertTrue(value.startsWith(SynapseClientImpl.SYNPASE_JAVA_CLIENT));
		assertTrue("Failed to append data to the user agent",value.indexOf(appended) > 0);
 	}
	
	@Test
	public void testBuildListColumnModelUrl(){
		String prefix = null;
		Long limit = null;
		Long offset = null;
		String expected ="/column";
		assertEquals(expected, SynapseClientImpl.buildListColumnModelUrl(prefix, limit, offset));
		
		prefix = "abc";
		limit = null;
		offset = null;
		expected ="/column?prefix=abc";
		assertEquals(expected, SynapseClientImpl.buildListColumnModelUrl(prefix, limit, offset));
		
		prefix = null;
		limit = 123l;
		offset = null;
		expected ="/column?limit=123";
		assertEquals(expected, SynapseClientImpl.buildListColumnModelUrl(prefix, limit, offset));
		
		prefix = null;
		limit = null;
		offset = 44l;
		expected ="/column?offset=44";
		assertEquals(expected, SynapseClientImpl.buildListColumnModelUrl(prefix, limit, offset));
		
		prefix = null;
		limit = 123l;
		offset = 44l;
		expected ="/column?limit=123&offset=44";
		assertEquals(expected, SynapseClientImpl.buildListColumnModelUrl(prefix, limit, offset));
		
		prefix = "abc";
		limit = 123l;
		offset = 44l;
		expected ="/column?prefix=abc&limit=123&offset=44";
		assertEquals(expected, SynapseClientImpl.buildListColumnModelUrl(prefix, limit, offset));
	}
	
	/**
	 * Used to check URLs for {@link #testBuildOpenIDUrl()}
	 */
	private String expectedURL;
	
	@SuppressWarnings("unchecked")
	@Test
	public void testBuildOpenIDUrl() throws Exception {
		StringEntity responseEntity = new StringEntity(EntityFactory.createJSONStringForEntity(new Session()));
		when(mockResponse.getEntity()).thenReturn(responseEntity);
		when(mockProvider.performRequest(any(String.class), any(String.class), any(String.class), (Map<String,String>)anyObject())).thenAnswer(new Answer<HttpResponse>() {

			@Override
			public HttpResponse answer(
					InvocationOnMock invocation) throws Throwable {
				String url = (String) invocation.getArguments()[0];
				expectedURL = url;
				return mockResponse;
			}
			
		});
		
		// One variation of the parameters that can be passed in
		synapse.passThroughOpenIDParameters("some=openId&paramters=here", true, DomainType.SYNAPSE);
		assertTrue("Incorrect URL: " + expectedURL, expectedURL.endsWith("/openIdCallback?some=openId&paramters=here&org.sagebionetworks.createUserIfNecessary=true&originClient=SYNAPSE"));
		
		// Another variation
		synapse.passThroughOpenIDParameters("blah=fun", false, DomainType.BRIDGE);
		assertTrue("Incorrect URL: " + expectedURL, expectedURL.endsWith("/openIdCallback?blah=fun&org.sagebionetworks.createUserIfNecessary=false&originClient=BRIDGE"));
	}
	
	
	/*
	 * Private methods
	 */
	private StringEntity createActivityStringEntity(String id, Activity act)
			throws JSONObjectAdapterException, UnsupportedEncodingException {
		act.setId(id);
		// Setup return
		JSONObjectAdapter adapter = act.writeToJSONObject(new JSONObjectAdapterImpl());
		String jsonString = adapter.toJSONString();
		StringEntity responseEntity = new StringEntity(jsonString);
		return responseEntity;
	}
	
	private <T extends JSONEntity> StringEntity createVCPRStringEntity(VariableContentPaginatedResults<T> vcpr)
				throws JSONObjectAdapterException, UnsupportedEncodingException {
		// Setup return
		JSONObjectAdapter adapter = vcpr.writeToJSONObject(new JSONObjectAdapterImpl());
		String jsonString = adapter.toJSONString();
		StringEntity responseEntity = new StringEntity(jsonString);
		return responseEntity;
	}
	
	
}
