package org.sagebionetworks;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.junit.After;
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
import org.sagebionetworks.repo.model.UserSessionData;

import com.amazonaws.util.IOUtils;

public class ITCloudMailIn {
	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapseOne;
	private static Long user1ToDelete;

	@BeforeClass
	public static void beforeClass() throws Exception {
		// Create 2 users
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapseOne = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(synapseOne);
		user1ToDelete = SynapseClientHelper.createUser(adminSynapse, synapseOne);
		
	}
	
	@Before
	public void before() throws Exception {
	}

	@After
	public void after() throws Exception {
	}
	
	@AfterClass
	public static void afterClass() throws Exception {	
		try {
			adminSynapse.deleteUser(user1ToDelete);
		} catch (SynapseException e) { }

	}
	
	private static final String[] SAMPLE_MESSAGES = {
		"SimpleMessage.json",
		"MessageWithAttachment.json"
	};
	
	private static final String URI = "/cloudMailInMessage";
	
	@Test
	public void testCloudMailInMessage() throws Exception {
		String repoEndpoint = StackConfiguration.getRepositoryServiceEndpoint();
		String username = StackConfiguration.getCloudMailInUser();
		String password = StackConfiguration.getCloudMailInPassword();

		
		// get the underlying SharedClientConnection so we can add the basic authentication header
		SharedClientConnection conn = synapseOne.getSharedClientConnection();
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.put("Authorization", "Basic: "+Base64.encodeBase64((username+":"+password).getBytes()));
		UserSessionData userSessionData = synapseOne.getUserSessionData();
		String sessionToken = userSessionData.getSession().getSessionToken();
		requestHeaders.put("sessionToken", sessionToken);
		for (String sampleFileName : SAMPLE_MESSAGES) {
			InputStream is = ITCloudMailIn.class.getClassLoader().
			         getResourceAsStream("CloudMailInMessages/"+sampleFileName);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try {
				IOUtils.copy(is, out);
				String messageJson = out.toString("utf-8");
				HttpResponse response = conn.performRequest(
						repoEndpoint+URI+"?notificationUnsubscribeEndpoint=https://www.synapse.org/#:unsubscribe"
						, "POST", messageJson, requestHeaders);
				assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusLine().getStatusCode());
			
				// TODO check that file is created
			} finally {
				is.close();
				out.close();
			}
		}

	}
	

}
