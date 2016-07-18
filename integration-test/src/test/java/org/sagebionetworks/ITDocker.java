package org.sagebionetworks;

import static org.junit.Assert.*;

import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SharedClientConnection;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Project;

public class ITDocker {
	private static final String SCOPE_PARAM = "scope";
	private static final String SERVICE_PARAM = "service";
	private static final String DOCKER_AUTHORIZATION = "/bearerToken";

	private static final String TYPE = "repository";
	private static final String ACCESS_TYPES_STRING="push,pull";

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapseOne;
	private static Long userToDelete;
	private static String dockerEndpoint;
	private static String username;
	private static String password;

	private String projectId;

	private SharedClientConnection conn;
	Map<String, String> requestHeaders;

	@BeforeClass
	public static void beforeClass() throws Exception {
		// Create 2 users
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse
				.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapseOne = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(synapseOne);

		username = UUID.randomUUID().toString();
		password = UUID.randomUUID().toString();
		userToDelete = SynapseClientHelper
				.createUser(adminSynapse, synapseOne, username, password);
		dockerEndpoint = StackConfiguration.getDockerServiceEndpoint();

	}

	@Before
	public void before() throws Exception {
		// get the underlying SharedClientConnection so we can add the basic
		// authentication header
		conn = synapseOne.getSharedClientConnection();
		requestHeaders = new HashMap<String, String>();
		requestHeaders.put("Content-Type", "application/json"); // Note, without
																// this header
																// we get a 415
																// response code
		requestHeaders.put(
				"Authorization",
				"Basic "
						+ (new String(Base64
								.encodeBase64((username + ":" + password)
										.getBytes()))));

		Project project = new Project();
		project = synapseOne.createEntity(project);
		projectId = project.getId();
	}

	@AfterClass
	public static void afterClass() throws Exception {
		try {
			adminSynapse.deleteUser(userToDelete);
		} catch (SynapseException e) {
		}
	}

	@Test
	public void testAuthorization() throws Exception {
		String service = "docker.synapse.org";
		String repoPath = projectId+"/reponame";
		String scope = TYPE+":"+repoPath+":"+ACCESS_TYPES_STRING;
		String urlString = dockerEndpoint + DOCKER_AUTHORIZATION;
		urlString += "?" + SERVICE_PARAM + "=" + URLEncoder.encode(service, "UTF-8");
		urlString += "&" + SCOPE_PARAM + "=" + URLEncoder.encode(scope, "UTF-8");
		HttpResponse response = conn.performRequest(urlString, "GET", null,
				requestHeaders);

		assertNotNull(EntityUtils.toString(response.getEntity()));

		assertEquals(HttpStatus.SC_OK, response.getStatusLine()
				.getStatusCode());
	}
}
