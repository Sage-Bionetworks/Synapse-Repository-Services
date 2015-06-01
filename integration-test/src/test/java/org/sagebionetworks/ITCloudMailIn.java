package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
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
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserSessionData;
import org.sagebionetworks.repo.model.message.cloudmailin.Message;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

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
		requestHeaders.put("Content-Type", "application/json"); // Note, without this header we get a 415 response code
		for (String sampleFileName : SAMPLE_MESSAGES) {
			InputStream is = ITCloudMailIn.class.getClassLoader().
			         getResourceAsStream("CloudMailInMessages/"+sampleFileName);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			UserProfile userProfile = synapseOne.getUserProfile(user1ToDelete.toString());
			String myemail = userProfile.getEmails().get(0);
			String myusername = userProfile.getUserName();
			try {
				IOUtils.copy(is, out);
				String messageJson = out.toString("utf-8");
				Message message = EntityFactory.createEntityFromJSONString(messageJson, Message.class);
				JSONObject origHeaders = new JSONObject(message.getHeaders());
				JSONObject newHeaders = new JSONObject();
				newHeaders.put("Subject", origHeaders.get("Subject")); // can copy other headers too
				newHeaders.put("From", myemail);
				newHeaders.put("To", myusername+"@synapse.org");
				message.setHeaders(newHeaders.toString());
				
				
				String emailMessageKey = EmailValidationUtil.getBucketKeyForEmail(myemail);
				if (EmailValidationUtil.doesFileExist(emailMessageKey)) 
					EmailValidationUtil.deleteFile(emailMessageKey);

				HttpResponse response = conn.performRequest(
						repoEndpoint+URI+"?notificationUnsubscribeEndpoint=https://www.synapse.org/#:unsubscribe"
						, "POST", EntityFactory.createJSONStringForEntity(message), requestHeaders);
				assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusLine().getStatusCode());
			
				// check that message is sent (file is created)
				assertTrue(EmailValidationUtil.doesFileExist(emailMessageKey));
			} finally {
				is.close();
				out.close();
			}
		}

	}
	
	// CloudMailIn uses basic authentication over https to authenticate itself.
	// This tests that the Synapse authentication filter rejects bad credentials.
	@Test
	public void testCloudMailInMessageWrongCredentials() throws Exception {
		String repoEndpoint = StackConfiguration.getRepositoryServiceEndpoint();
		String username = StackConfiguration.getCloudMailInUser();
		String password = "ThisIsTheWrongPassword!!!";

		// get the underlying SharedClientConnection so we can add the basic authentication header
		SharedClientConnection conn = synapseOne.getSharedClientConnection();
		Map<String, String> requestHeaders = new HashMap<String, String>();
		requestHeaders.put("Authorization", "Basic "+Base64.encodeBase64((username+":"+password).getBytes()));
		UserSessionData userSessionData = synapseOne.getUserSessionData();
		String sessionToken = userSessionData.getSession().getSessionToken();
		requestHeaders.put("sessionToken", sessionToken);
		requestHeaders.put("Content-Type", "application/json"); // Note, without this header we get a 415 response code
		String sampleFileName = SAMPLE_MESSAGES[0];
		InputStream is = ITCloudMailIn.class.getClassLoader().
		         getResourceAsStream("CloudMailInMessages/"+sampleFileName);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		UserProfile userProfile = synapseOne.getUserProfile(user1ToDelete.toString());
		String myemail = userProfile.getEmails().get(0);
		String myusername = userProfile.getUserName();
		try {
			IOUtils.copy(is, out);
			String messageJson = out.toString("utf-8");			
			Message message = EntityFactory.createEntityFromJSONString(messageJson, Message.class);
			JSONObject origHeaders = new JSONObject(message.getHeaders());
			JSONObject newHeaders = new JSONObject();
			newHeaders.put("Subject", origHeaders.get("Subject")); // can copy other headers too
			newHeaders.put("From", myemail);
			newHeaders.put("To", myusername+"@synapse.org");
			message.setHeaders(newHeaders.toString());
			HttpResponse response = conn.performRequest(
					repoEndpoint+URI+"?notificationUnsubscribeEndpoint=https://www.synapse.org/#:unsubscribe"
					, "POST", EntityFactory.createJSONStringForEntity(message), requestHeaders);
			assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
				} finally {
			is.close();
			out.close();
		}
	}
}
