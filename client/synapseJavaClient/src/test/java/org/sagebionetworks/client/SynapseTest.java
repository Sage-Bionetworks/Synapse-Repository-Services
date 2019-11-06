package org.sagebionetworks.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseTermsOfUseException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundle;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundleRequest;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.auth.LoginRequest;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.versionInfo.SynapseVersionInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.simpleHttpClient.Header;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;

/**
 * Unit test for Synapse.
 * @author jmhill
 *
 */
public class SynapseTest {
	@Mock
	SimpleHttpResponse mockResponse;
	@Mock
	SimpleHttpClient mockClient;
	@Mock
	Header mockHeader;
	
	SynapseClientImpl synapse;
	
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";
	
	@Before
	public void before() throws Exception{
		MockitoAnnotations.initMocks(this);
		synapse = new SynapseClientImpl();
		synapse.setSimpleHttpClient(mockClient);

		when(mockClient.get(any())).thenReturn(mockResponse);
		when(mockClient.delete(any())).thenReturn(mockResponse);
		when(mockClient.put(any(), any())).thenReturn(mockResponse);
		when(mockClient.post(any(), any())).thenReturn(mockResponse);

		configureMockHttpResponse(201, "{\"sessionToken\":\"some-session-token\"}");
		LoginRequest request = new LoginRequest();
		request.setUsername("foo");
		request.setPassword("bar");
		synapse.login(request);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateNullEntity() throws Exception{
		synapse.createEntity(null);
	}
	
	private void configureMockHttpResponse(final int statusCode, final String responseBody) {
		when(mockResponse.getStatusCode()).thenReturn(statusCode);
		when(mockResponse.getContent()).thenReturn(responseBody);
		when(mockHeader.getValue()).thenReturn(CONTENT_TYPE_APPLICATION_JSON);
		when(mockResponse.getFirstHeader(CONTENT_TYPE)).thenReturn(mockHeader);
	}
	
	@Test (expected=SynapseTermsOfUseException.class)
	public void testTermsOfUseNotAccepted() throws Exception{
		configureMockHttpResponse(403, "{\"reason\":\"foo\"}");
		synapse.revalidateSession();
	}
	
	@Test
	public void testCreateStudyEntity() throws Exception {
		Folder ds = EntityCreator.createNewFolder();
		// This is what we want returned.
		String jsonString = EntityFactory.createJSONStringForEntity(ds);
		configureMockHttpResponse(201, jsonString);
		// Now create an entity
		Folder clone = synapse.createEntity(ds);
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
		configureMockHttpResponse(201, jsonString);
		// Now create an entity
		Folder clone = synapse.createEntity(fl);
		// For this test we want return 
		assertNotNull(clone);
		// The clone should equal the original fl
		assertEquals(fl, clone);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetEntityNullId() throws Exception{
		synapse.getEntity(null, Folder.class);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetEntityNullClass() throws Exception{
		synapse.getEntity("123", null);
	}
	
	@Test
	public void testGetEntity() throws Exception {
		Folder ds = EntityCreator.createNewFolder();
		// This is what we want returned.
		String jsonString = EntityFactory.createJSONStringForEntity(ds);

		configureMockHttpResponse(200, jsonString);
		Folder clone = synapse.getEntity(ds.getId(), Folder.class);
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
		configureMockHttpResponse(201, jsonString);
		EntityHeader clone = synapse.getEntityBenefactor("syn101");
		// For this test we want return 
		assertNotNull(clone);
		// The clone should equal the original ds
		assertEquals(eh, clone);
	}
	
	@Test
	public void testCanAccess() throws Exception {
		Folder ds = EntityCreator.createNewFolder();
		String jsonString = "{\"result\":true}";
		configureMockHttpResponse(201, jsonString);
		assertTrue(synapse.canAccess(ds.getId(), ACCESS_TYPE.READ));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testPutEntityNull() throws Exception {
		synapse.putEntity(null);
	}
	
	@Test
	public void testPutEntity() throws Exception {
		Folder ds = EntityCreator.createNewFolder();
		// This is what we want returned.
		String jsonString = EntityFactory.createJSONStringForEntity(ds);
		configureMockHttpResponse(201, jsonString);
		Folder clone = synapse.putEntity(ds);
		// For this test we want return 
		assertNotNull(clone);
		// The clone should equal the original ds
		assertEquals(ds, clone);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testDeleteEntityNull() throws Exception{
		synapse.deleteEntity((Entity)null, null);
	}

	@Test
	public void testGetEntityPath() throws Exception {
		Folder folder = new Folder();
		// create test hierarchy
		
		EntityHeader layerHeader = new EntityHeader();
		layerHeader.setId("layerid");
		layerHeader.setName("layer name");
		layerHeader.setType("/folder");	
		List<EntityHeader> entityHeaders = new ArrayList<EntityHeader>();
		entityHeaders.add(layerHeader);
		EntityPath entityPath = new EntityPath();
		entityPath.setPath(entityHeaders);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		entityPath.writeToJSONObject(adapter);
		
		// We want the mock response to return JSON for the hierarchy.
		configureMockHttpResponse(200, adapter.toJSONString());
		
		// execute and verify
		EntityPath returnedEntityPath = synapse.getEntityPath(folder);
		List<EntityHeader> returnedHeaders = returnedEntityPath.getPath();
		
		assertEquals(1, returnedHeaders.size());
		EntityHeader firstHeader = returnedHeaders.get(0);
		assertEquals(layerHeader, firstHeader);
		
	}

	@Test
	public void testCreateAccessRequirement() throws Exception {
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		ar.setConcreteType(ar.getClass().getName());
		ar.setSubjectIds(new ArrayList<RestrictableObjectDescriptor>());
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setTermsOfUse("foo");
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		ar.writeToJSONObject(adapter);
		configureMockHttpResponse(201, adapter.toJSONString());
		synapse.createAccessRequirement(ar);
	}
	
	@Test
	public void updateAccessRequirement() throws Exception {
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		ar.setConcreteType(ar.getClass().getName());
		ar.setSubjectIds(new ArrayList<RestrictableObjectDescriptor>());
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setTermsOfUse("foo");
		ar.setId(101L);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		ar.writeToJSONObject(adapter);
		configureMockHttpResponse(200, adapter.toJSONString());
		synapse.updateAccessRequirement(ar);
	}
	
	@Test
	public void testCreateAccessApproval() throws Exception {
		AccessApproval aa = new AccessApproval();
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		aa.writeToJSONObject(adapter);
		configureMockHttpResponse(201, adapter.toJSONString());
		synapse.createAccessApproval(aa);
	}
	
	@Test
	public void testGetVersionInfo() throws Exception {
		SynapseVersionInfo expectedVersion = new SynapseVersionInfo();
		expectedVersion.setVersion("versionString");
		String jsonString = EntityFactory.createJSONStringForEntity(expectedVersion);
		configureMockHttpResponse(200, jsonString);
		SynapseVersionInfo vi = synapse.getVersionInfo();
		assertNotNull(vi);
		assertEquals(vi, expectedVersion);
	};

	@Test
	public void testGetEntityBundle() throws NameConflictException, JSONObjectAdapterException, IOException, NotFoundException, DatastoreException, SynapseException {
		// Create an entity
		Folder s = EntityCreator.createNewFolder();
		
		// Get/add/update annotations for this entity
		Annotations a = new Annotations();
		a.setEtag(s.getEtag());
		AnnotationsV2TestUtils.putAnnotations(a, "doubleAnno", "45.0001", AnnotationsValueType.DOUBLE);
		AnnotationsV2TestUtils.putAnnotations(a, "string", "A string", AnnotationsValueType.STRING);
		JSONObjectAdapter adapter0 = new JSONObjectAdapterImpl();
		a.writeToJSONObject(adapter0);
		
		// We want the mock response to return JSON
		configureMockHttpResponse(200, adapter0.toJSONString());
		synapse.updateAnnotationsV2(s.getId(), a);
		
		// Assemble the bundle
		EntityBundle eb = new EntityBundle();
		eb.setEntity(s);
		eb.setAnnotations(a);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		eb.writeToJSONObject(adapter);
		
		// We want the mock response to return JSON
		configureMockHttpResponse(200, adapter.toJSONString());
		
		// Get the bundle, verify contents
		EntityBundleRequest request = new EntityBundleRequest();
		request.setIncludeEntity(true);
		request.setIncludeAnnotations(true);
		EntityBundle eb2 = synapse.getEntityBundleV2(s.getId(), request);
		
		Folder s2 = (Folder) eb2.getEntity();
		assertEquals("Retrieved Entity in bundle does not match original one", s, s2);
		
		Annotations a2 = eb2.getAnnotations();
		assertEquals("Retrieved Annotations in bundle do not match original ones", a, a2);
		
		UserEntityPermissions uep = eb2.getPermissions();
		assertNull("Permissions were not requested, but were returned in bundle", uep);
		
		EntityPath path = eb2.getPath();
		assertNull("Path was not requested, but was returned in bundle", path);
	}

	
	@Test
	public void testGetActivity() throws Exception {
		Activity act = new Activity();
		String id = "123";
		String response = createActivityString(id, act);
		configureMockHttpResponse(200, response);
		
		Activity clone = synapse.getActivity(id);
		assertNotNull(clone);
		assertEquals(act, clone);
	}
	
	@Test
	public void testPutActivity() throws Exception {
		Activity act = new Activity();
		String id = "123";
		String response = createActivityString(id, act);
		configureMockHttpResponse(200, response);
		
		Activity clone = synapse.putActivity(act);
		assertNotNull(clone);
		assertEquals(act, clone);
	}	
		
	@Test
	public void testDeleteActivity() throws Exception {
		String id = "123";
		configureMockHttpResponse(204, "");
		synapse.deleteActivity(id);
		verify(mockResponse, times(4)).getStatusCode();
	}		
	
	@Test
	public void testGetActivityForEntity() throws Exception {
		Activity act = new Activity();
		String id = "123";
		String response = createActivityString(id, act);
		configureMockHttpResponse(200, response);
		
		String entityId = "syn456";
		Activity clone = synapse.getActivityForEntity(entityId); 
		assertNotNull(clone);
		assertEquals(act, clone);		
	}

	@Test
	public void testGetActivityForEntityVersion() throws Exception {
		Activity act = new Activity();
		String id = "123";
		String response = createActivityString(id, act);
		configureMockHttpResponse(200, response);
		
		String entityId = "syn456";
		Activity clone = synapse.getActivityForEntityVersion(entityId, 1L); 
		assertNotNull(clone);
		assertEquals(act, clone);
	}

	@Test
	public void testSetActivityForEntity() throws Exception {
		Activity act = new Activity();
		String id = "123";
		String response = createActivityString(id, act);
		configureMockHttpResponse(200, response);
		
		String entityId = "syn456";
		Activity clone = synapse.setActivityForEntity(entityId, id); 
		assertNotNull(clone);
		assertEquals(act, clone);				
	}

	@Test
	public void testDeleteGeneratedByForEntity() throws Exception {
		Activity act = new Activity();
		String id = "123";
		String response = createActivityString(id, act);
		configureMockHttpResponse(200, response);
		
		String entityId = "syn456";
		synapse.deleteGeneratedByForEntity(entityId); 
		verify(mockResponse, atLeast(1)).getStatusCode();
	}
	
	@Test
	public void testUserAgent() throws Exception{
		// The user agent 
		SynapseVersionInfo info = new SynapseVersionInfo();
		info.setVersion("someversion");
		configureMockHttpResponse(200, EntityFactory.createJSONStringForEntity(info));
		// Make a call and ensure 
		synapse.getVersionInfo();
		ArgumentCaptor<SimpleHttpRequest> requestCaptor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).get(requestCaptor.capture());
		String value = requestCaptor.getValue().getHeaders().get("User-Agent");
		assertNotNull(value);
		assertTrue(value.startsWith(SynapseClientImpl.SYNAPSE_JAVA_CLIENT));
 	}
	
	@Test
	public void testAppendUserAgent() throws Exception{
		// The user agent 
		SynapseVersionInfo info = new SynapseVersionInfo();
		info.setVersion("someversion");
		configureMockHttpResponse(200, EntityFactory.createJSONStringForEntity(info));
		// Append some user agent data
		String appended = "Appended to the User-Agent";
		synapse.appendUserAgent(appended);
		// Make a call and ensure 
		synapse.getVersionInfo();
		ArgumentCaptor<SimpleHttpRequest> requestCaptor = ArgumentCaptor.forClass(SimpleHttpRequest.class);
		verify(mockClient).get(requestCaptor.capture());
		String value = requestCaptor.getValue().getHeaders().get("User-Agent");
		assertNotNull(value);
		assertTrue(value.startsWith(SynapseClientImpl.SYNAPSE_JAVA_CLIENT));
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
	
	@Test
	public void testEndpointForTypeRepo(){
		synapse.setRepositoryEndpoint("https://repo-endpoint.com");
		assertEquals("https://repo-endpoint.com", synapse.getEndpointForType(RestEndpointType.repo));
	}
	
	@Test
	public void testEndpointForTypeAuth(){
		synapse.setAuthEndpoint("auth-endpoint");
		assertEquals("auth-endpoint", synapse.getEndpointForType(RestEndpointType.auth));
	}
	
	@Test
	public void testEndpointForTypeFile(){
		synapse.setFileEndpoint("file-endpoint");
		assertEquals("file-endpoint", synapse.getEndpointForType(RestEndpointType.file));
	}
	
	/*
	 * Private methods
	 */
	private String createActivityString(String id, Activity act)
			throws JSONObjectAdapterException, UnsupportedEncodingException {
		act.setId(id);
		// Setup return
		JSONObjectAdapter adapter = act.writeToJSONObject(new JSONObjectAdapterImpl());
		return adapter.toJSONString();
	}
}
