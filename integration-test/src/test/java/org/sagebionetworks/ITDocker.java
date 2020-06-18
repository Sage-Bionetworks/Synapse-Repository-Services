package org.sagebionetworks;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.client.ClientUtils;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityChildrenResponse;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerCommitSortBy;
import org.sagebionetworks.repo.model.docker.DockerRegistryEvent;
import org.sagebionetworks.repo.model.docker.DockerRegistryEventList;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.docker.RegistryEventAction;
import org.sagebionetworks.repo.model.docker.RegistryEventActor;
import org.sagebionetworks.repo.model.docker.RegistryEventRequest;
import org.sagebionetworks.repo.model.docker.RegistryEventTarget;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientImpl;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;

import com.google.common.collect.Lists;

public class ITDocker {
	private static final String SCOPE_PARAM = "scope";
	private static final String SERVICE_PARAM = "service";
	private static final String DOCKER_AUTHORIZATION = "/bearerToken";

	private static final String TYPE = "repository";
	private static final String ACCESS_TYPES_STRING="push,pull";
	private static final String MEDIA_TYPE = "application/vnd.docker.distribution.manifest.v2+json";
	public static final String DOCKER_REGISTRY_EVENTS = "/events";

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapseOne;
	private static Long userToDelete;
	private static String username;
	private static String password;

	private String projectId;
	
	private static StackConfiguration config;

	private static SimpleHttpClient simpleClient;
	
	private String synapseDockerAuthorizationUrl;
	private OAuthClient oauthClient;
	private String oauthClientSecret;

	@BeforeAll
	public static void beforeClass() throws Exception {
		config = StackConfigurationSingleton.singleton();
		// Create 2 users
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse
		.setUsername(config.getMigrationAdminUsername());
		adminSynapse.setApiKey(config.getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapseOne = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(synapseOne);

		username = UUID.randomUUID().toString();
		password = UUID.randomUUID().toString();
		userToDelete = SynapseClientHelper
				.createUser(adminSynapse, synapseOne, username, password);
		simpleClient = new SimpleHttpClientImpl();
	}

	@BeforeEach
	public void before() throws Exception {
		Project project = new Project();
		project = synapseOne.createEntity(project);
		projectId = project.getId();
		
		// create the OAuth client
		oauthClient = new OAuthClient();
		oauthClient.setClient_name(UUID.randomUUID().toString());
		oauthClient.setRedirect_uris(Collections.singletonList("https://foo.bar.com"));
		oauthClient = synapseOne.createOAuthClient(oauthClient);
		// Sets the verified status of the client (only admins and ACT can do this)
		oauthClient = adminSynapse.updateOAuthClientVerifiedStatus(oauthClient.getClient_id(), oauthClient.getEtag(), true);
		oauthClientSecret = synapseOne.createOAuthClientSecret(oauthClient.getClient_id()).getClient_secret();

		String service = "docker.synapse.org";
		String repoPath = projectId+"/reponame";
		String scope = TYPE+":"+repoPath+":"+ACCESS_TYPES_STRING;
		synapseDockerAuthorizationUrl = config.getDockerServiceEndpoint() + DOCKER_AUTHORIZATION;
		synapseDockerAuthorizationUrl += "?" + SERVICE_PARAM + "=" + URLEncoder.encode(service, "UTF-8");
		synapseDockerAuthorizationUrl += "&" + SCOPE_PARAM + "=" + URLEncoder.encode(scope, "UTF-8");

	}
	
	@AfterEach
	public void after() throws Exception {
		if (projectId!=null) synapseOne.deleteEntityById(projectId);
		projectId=null;
	}

	@AfterAll
	public static void afterClass() throws Exception {
		try {
			adminSynapse.deleteUser(userToDelete);
		} catch (SynapseException e) {
		}
	}
	
	@Test
	public void testDockerClientAuthorization() throws Exception {
		Map<String, String> requestHeaders = new HashMap<String, String>();
		// Note, without this header  we get a 415 response code
		requestHeaders.put("Content-Type", "application/json"); 
		requestHeaders.put(
				AuthorizationConstants.AUTHORIZATION_HEADER_NAME,
				ClientUtils.createBasicAuthorizationHeader(username, password));
		
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri(synapseDockerAuthorizationUrl);
		request.setHeaders(requestHeaders);
		SimpleHttpResponse response = simpleClient.get(request);
		assertNotNull(response.getContent());
		assertEquals(HttpStatus.SC_OK, response.getStatusCode());
	}

	@Test
	public void testDockerClientAuthorizationWithAccessToken() throws Exception {
		String accessToken = OAuthHelper.getAccessToken(
				synapseOne, 
				synapseOne, 
				oauthClient.getClient_id(), 
				oauthClientSecret, 
				oauthClient.getRedirect_uris().get(0),
				"authorize"
			);
		Map<String, String> requestHeaders = new HashMap<String, String>();
		// Note, without this header  we get a 415 response code
		requestHeaders.put("Content-Type", "application/json"); 
		requestHeaders.put(
				AuthorizationConstants.AUTHORIZATION_HEADER_NAME,
				ClientUtils.createBasicAuthorizationHeader(username, accessToken));
		
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri(synapseDockerAuthorizationUrl);
		request.setHeaders(requestHeaders);
		SimpleHttpResponse response = simpleClient.get(request);
		assertNotNull(response.getContent());
		assertEquals(HttpStatus.SC_OK, response.getStatusCode());
	}

	private static DockerCommit createCommit(String tag, String digest) {
		DockerCommit commit = new DockerCommit();
		Date createdOn = new Date();
		commit.setCreatedOn(createdOn);
		commit.setDigest(digest);
		commit.setTag(tag);
		return commit;
	}

	@Test
	public void testUnmanagedRepository() throws Exception {
		DockerRepository dockerRepo = new DockerRepository();
		dockerRepo.setParentId(projectId);
		dockerRepo.setRepositoryName("uname/reponame");
		dockerRepo = synapseOne.createEntity(dockerRepo);
		String tag = "tag";
		DockerCommit commit1 = createCommit(tag, UUID.randomUUID().toString());
		synapseOne.addDockerCommit(dockerRepo.getId(), commit1);
		Thread.sleep(10L);
		// now reassign the tag to a new commit
		DockerCommit commit2 = createCommit(tag, UUID.randomUUID().toString());
		synapseOne.addDockerCommit(dockerRepo.getId(), commit2);
		PaginatedResults<DockerCommit> result = synapseOne.listDockerTags(dockerRepo.getId(), 10L, 0L, DockerCommitSortBy.TAG, true);
		assertEquals(1L, result.getTotalNumberOfResults());
		assertEquals(1, result.getResults().size());
		DockerCommit retrieved = result.getResults().get(0);
		assertNotNull(retrieved.getCreatedOn());
		assertEquals(commit2.getDigest(), retrieved.getDigest());
		assertEquals(tag, retrieved.getTag());

		// make sure optional params are optional
		assertEquals(
				result,
				synapseOne.listDockerTags(dockerRepo.getId(), null, null, null, null)
				);
	}
	
	// helper function to construct registry events in the prescribed format
	private static DockerRegistryEventList createDockerRegistryEvent(
			RegistryEventAction action, String host, long userId, String repositoryPath, String tag, String digest) {
		DockerRegistryEvent event = new DockerRegistryEvent();
		event.setAction(action);
		RegistryEventRequest eventRequest = new RegistryEventRequest();
		event.setRequest(eventRequest);
		eventRequest.setHost(host);
		RegistryEventActor eventActor = new RegistryEventActor();
		event.setActor(eventActor);
		eventActor.setName(""+userId);
		RegistryEventTarget target = new RegistryEventTarget();
		target.setRepository(repositoryPath);
		target.setMediaType(MEDIA_TYPE);
		target.setTag(tag);
		target.setDigest(digest);
		event.setTarget(target);
		DockerRegistryEventList eventList = new DockerRegistryEventList();
		List<DockerRegistryEvent> events = new ArrayList<DockerRegistryEvent>();
		eventList.setEvents(events);
		events.add(event);
		return eventList;
	}


	@Test
	public void testSendRegistryEvents() throws Exception {
		String registryUserName = config.getDockerRegistryUser();
		String registryPassword =config.getDockerRegistryPassword();
		Map<String, String> requestHeaders = new HashMap<String, String>();
		// Note, without this header  we get a 415 response code
		requestHeaders.put("Content-Type", "application/json"); 
		requestHeaders.put(
				AuthorizationConstants.AUTHORIZATION_HEADER_NAME,
				ClientUtils.createBasicAuthorizationHeader(registryUserName, registryPassword));
		String host = "docker.synapse.org";
		String repositorySuffix = "reponame";
		String repositoryPath = projectId+"/"+repositorySuffix;
		String tag = "latest";
		String digest = UUID.randomUUID().toString(); // usu. a SHA256, but not required
		DockerRegistryEventList registryEvents = createDockerRegistryEvent(
				RegistryEventAction.push,  host,  userToDelete,  repositoryPath,  tag,  digest);
		URL url = new URL(config.getDockerRegistryListenerEndpoint() + 
				DOCKER_REGISTRY_EVENTS);
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri(url.toString());
		request.setHeaders(requestHeaders);
		String body = EntityFactory.createJSONStringForEntity(registryEvents);
		simpleClient.post(request, body);
		// check that repo was created
		EntityChildrenRequest childRequest = new EntityChildrenRequest();
		childRequest.setParentId(projectId);
		childRequest.setIncludeTypes(Lists.newArrayList(EntityType.dockerrepo));
		EntityChildrenResponse response = synapseOne.getEntityChildren(childRequest);
		assertNotNull(response);
		assertNotNull(response.getPage());
		assertEquals(1, response.getPage().size());
	}

	@Test
	public void testSendRegistryEventsWrongCredentials() throws Exception {
		Map<String, String> requestHeaders = new HashMap<String, String>();
		// Note, without this header  we get a 415 response code
		requestHeaders.put("Content-Type", "application/json"); 
		requestHeaders.put(
				AuthorizationConstants.AUTHORIZATION_HEADER_NAME,
				ClientUtils.createBasicAuthorizationHeader("wrong user name", "wrong password"));
		DockerRegistryEventList registryEvents = new DockerRegistryEventList();
		URL url = new URL(config.getDockerRegistryListenerEndpoint() + 
				DOCKER_REGISTRY_EVENTS);
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri(url.toString());
		request.setHeaders(requestHeaders);
		String body = EntityFactory.createJSONStringForEntity(registryEvents);
		SimpleHttpResponse response = simpleClient.post(request, body);
		assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatusCode());
		EntityChildrenRequest childRequest = new EntityChildrenRequest();
		childRequest.setParentId(projectId);
		childRequest.setIncludeTypes(Lists.newArrayList(EntityType.dockerrepo));
		EntityChildrenResponse childResponse = synapseOne.getEntityChildren(childRequest);
		assertNotNull(childResponse);
		assertNotNull(childResponse.getPage());
		assertEquals(0, childResponse.getPage().size());
	}
	
	// Test for PLFM-6189 and PLFM-6188
	@Test
	public void testSendRegistryEventsInvalidEncodedCredentials() throws Exception {
		Map<String, String> requestHeaders = new HashMap<String, String>();
		// Note, without this header  we get a 415 response code
		requestHeaders.put("Content-Type", "application/json"); 
		requestHeaders.put(
				AuthorizationConstants.AUTHORIZATION_HEADER_NAME,
				ClientUtils.createBasicAuthorizationHeader("wrong user name", "wrong password") + "_wrong");
		
		DockerRegistryEventList registryEvents = new DockerRegistryEventList();
		
		URL url = new URL(config.getDockerRegistryListenerEndpoint() + 
				DOCKER_REGISTRY_EVENTS);
		
		SimpleHttpRequest request = new SimpleHttpRequest();
		request.setUri(url.toString());
		request.setHeaders(requestHeaders);
		String body = EntityFactory.createJSONStringForEntity(registryEvents);
		SimpleHttpResponse response = simpleClient.post(request, body);
		assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatusCode());
	}


}
