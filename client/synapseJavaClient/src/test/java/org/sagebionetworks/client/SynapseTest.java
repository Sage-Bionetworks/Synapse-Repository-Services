package org.sagebionetworks.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseTermsOfUseException;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessApproval;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.versionInfo.SynapseVersionInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

/**
 * Unit test for Synapse.
 * @author jmhill
 *
 */
public class SynapseTest {
	
	HttpClientProvider mockProvider;
	HttpResponse mockResponse;
	
	SynapseClientImpl synapse;
	
	@SuppressWarnings("unchecked")
	@Before
	public void before() throws Exception{
		// The mock provider
		mockProvider = Mockito.mock(HttpClientProvider.class);
		mockResponse = Mockito.mock(HttpResponse.class);
		when(mockProvider.performRequest(any(String.class),any(String.class),any(String.class),(Map<String,String>)anyObject())).thenReturn(mockResponse);
		synapse = new SynapseClientImpl(new SharedClientConnection(mockProvider));
		// mock the session token returned when logging in
		configureMockHttpResponse(201, "{\"sessionToken\":\"some-session-token\"}");
		synapse.login("foo", "bar");
	}
	
	@Test
	public void testOriginatingClient() throws Exception {
		SharedClientConnection connection = synapse.getSharedClientConnection();

		connection.setDomain(DomainType.SYNAPSE);
		String url = connection.createRequestUrl("http://localhost:8888/", "createUser", null);
		Assert.assertEquals("Origin client value appended as query string", "http://localhost:8888/createUser?domain=SYNAPSE", url);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateNullEntity() throws Exception{
		synapse.createEntity(null);
	}
	
	private static Header mockHeader(final String name, final String value) {
		Header header = Mockito.mock(Header.class);
		when(header.getName()).thenReturn(name);
		when(header.getValue()).thenReturn(value);
		HeaderElement he = Mockito.mock(HeaderElement.class);
		when(he.getName()).thenReturn(name);
		when(he.getValue()).thenReturn(value);
		when(header.getElements()).thenReturn(new HeaderElement[]{he});
		return header;
	}
	
	private void configureMockHttpResponse(final int statusCode, final String responseBody) {
		StatusLine statusLine = Mockito.mock(StatusLine.class);
		when(statusLine.getStatusCode()).thenReturn(statusCode);
		when(mockResponse.getStatusLine()).thenReturn(statusLine);
		
		HttpEntity entity = new HttpEntity() {
			@Override
			public boolean isRepeatable() {return false;}
			@Override
			public boolean isChunked() {return false;}
			@Override
			public long getContentLength() {return responseBody.getBytes().length;}
			@Override
			public Header getContentType() {return mockHeader("ContentType", "text/plain");}
			@Override
			public Header getContentEncoding() {return mockHeader("ContentEncoding", "application/json");}
			@Override
			public InputStream getContent() throws IOException, IllegalStateException 
					{return new ByteArrayInputStream(responseBody.getBytes());}
			@Override
			public void writeTo(OutputStream outstream) throws IOException {}
			@Override
			public boolean isStreaming() {return false;}
			@Override
			public void consumeContent() throws IOException {}
		};
		when(mockResponse.getEntity()).thenReturn(entity);
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
				
		// setup mock
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		paginatedResult.writeToJSONObject(adapter);		
		configureMockHttpResponse(200, adapter.toJSONString());		
		
		PaginatedResults<EntityHeader> realResults = synapse.getEntityReferencedBy(proj2);
		
		assertEquals(1, realResults.getTotalNumberOfResults());
		EntityHeader firstHeader = realResults.getResults().get(0);
		assertEquals(proj1Header, firstHeader);
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
		TermsOfUseAccessApproval aa = new TermsOfUseAccessApproval();
		aa.setConcreteType(aa.getClass().getName());
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
		a.addAnnotation("doubleAnno", new Double(45.0001));
		a.addAnnotation("string", "A string");		
		JSONObjectAdapter adapter0 = new JSONObjectAdapterImpl();
		a.writeToJSONObject(adapter0);
		
		// We want the mock response to return JSON
		configureMockHttpResponse(200, adapter0.toJSONString());
		synapse.updateAnnotations(s.getId(), a);
		
		// Assemble the bundle
		EntityBundle eb = new EntityBundle();
		eb.setEntity(s);
		eb.setAnnotations(a);
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
		eb.writeToJSONObject(adapter);
		
		// We want the mock response to return JSON
		configureMockHttpResponse(200, adapter.toJSONString());
		
		// Get the bundle, verify contents
		int mask =  EntityBundle.ENTITY + EntityBundle.ANNOTATIONS;
		EntityBundle eb2 = synapse.getEntityBundle(s.getId(), mask);
		
		Folder s2 = (Folder) eb2.getEntity();
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
		String response = createActivityString(id, act);
		configureMockHttpResponse(200, response);
		
		Activity clone = synapse.getActivity(id);
		assertNotNull(clone);
		assertEquals(act, clone);
	}	
	
	@Test
	public void testGetUnmetEvaluationAccessRequirements() throws Exception {
		PaginatedResults<AccessRequirement> result = 
			new PaginatedResults<AccessRequirement>();
		JSONObjectAdapter adapter = result.writeToJSONObject(new JSONObjectAdapterImpl());
		configureMockHttpResponse(200, adapter.toJSONString());
		RestrictableObjectDescriptor subjectId = new RestrictableObjectDescriptor();
		subjectId.setType(RestrictableObjectType.EVALUATION);
		subjectId.setId("12345");
		PaginatedResults<AccessRequirement> clone = 
			synapse.getUnmetAccessRequirements(subjectId, ACCESS_TYPE.PARTICIPATE);
		assertNotNull(clone);
		assertEquals(result, clone);
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
		verify(mockResponse, times(2)).getEntity();
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
		verify(mockResponse, atLeast(1)).getEntity();
	}
	
	@Test
	public void testUserAgent() throws SynapseException, JSONObjectAdapterException, UnsupportedEncodingException{
		// The user agent 
		SynapseVersionInfo info = new SynapseVersionInfo();
		info.setVersion("someversion");
		configureMockHttpResponse(200, EntityFactory.createJSONStringForEntity(info));
		StubHttpClientProvider stubProvider = new StubHttpClientProvider(mockResponse);
		synapse = new SynapseClientImpl(new SharedClientConnection(stubProvider));
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
		configureMockHttpResponse(200, EntityFactory.createJSONStringForEntity(info));
		StubHttpClientProvider stubProvider = new StubHttpClientProvider(mockResponse);
		synapse = new SynapseClientImpl(new SharedClientConnection(stubProvider));
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
	
	@Test
	public void testEndpointForTypeRepo(){
		synapse.setRepositoryEndpoint("repo-endpoint");
		assertEquals("repo-endpoint", synapse.getEndpointForType(RestEndpointType.repo));
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
	
	/**
	 * Used to check URLs for {@link #testBuildOpenIDUrl()}
	 */
	private String expectedURL;
	
	@SuppressWarnings("unchecked")
	@Test
	public void testBuildOpenIDUrl() throws Exception {
		when(mockProvider.performRequest(any(String.class), any(String.class), any(String.class), (Map<String,String>)anyObject())).thenAnswer(new Answer<HttpResponse>() {

			@Override
			public HttpResponse answer(
					InvocationOnMock invocation) throws Throwable {
				String url = (String) invocation.getArguments()[0];
				expectedURL = url;
				return mockResponse;
			}
			
		});
		String response = EntityFactory.createJSONStringForEntity(new Session());
		configureMockHttpResponse(200, response);
		
		// One variation of the parameters that can be passed in
		synapse.passThroughOpenIDParameters("some=openId&paramters=here", true, DomainType.SYNAPSE);
		assertTrue("Incorrect URL: " + expectedURL, expectedURL.endsWith("/openIdCallback?some=openId&paramters=here&org.sagebionetworks.createUserIfNecessary=true&domain=SYNAPSE"));
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
