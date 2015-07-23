package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.json.JSONObject;
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
import org.sagebionetworks.repo.model.message.cloudmailin.AuthorizationCheckHeader;
import org.sagebionetworks.repo.model.message.cloudmailin.Envelope;
import org.sagebionetworks.repo.model.message.cloudmailin.Message;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

import com.amazonaws.util.IOUtils;

public class ITCloudMailIn {
	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapseOne;
	private static Long user1ToDelete;
	private static String repoEndpoint;
	private static String username;
	private static String password;
	private static final String[] SAMPLE_MESSAGES = { "SimpleMessage.json",
			"MessageWithAttachment.json" };

	private static final String MESSAGE_URI = "/cloudMailInMessage";
	private static final String AUTHORIZATION_URI = "/cloudMailInAuthorization";

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
		user1ToDelete = SynapseClientHelper
				.createUser(adminSynapse, synapseOne);
		repoEndpoint = StackConfiguration.getRepositoryServiceEndpoint();
		username = StackConfiguration.getCloudMailInUser();
		password = StackConfiguration.getCloudMailInPassword();

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
	}

	@AfterClass
	public static void afterClass() throws Exception {
		try {
			adminSynapse.deleteUser(user1ToDelete);
		} catch (SynapseException e) {
		}

	}

	private HttpResponse sendMessage(String jsonFileName, String fromemail)
			throws Exception {
		InputStream is = ITCloudMailIn.class.getClassLoader()
				.getResourceAsStream(jsonFileName);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		UserProfile userProfile = synapseOne.getUserProfile(user1ToDelete
				.toString());
		String myusername = userProfile.getUserName();
		try {
			IOUtils.copy(is, out);
			String messageJson = out.toString("utf-8");
			Message message = EntityFactory.createEntityFromJSONString(
					messageJson, Message.class);
			JSONObject origHeaders = new JSONObject(message.getHeaders());
			JSONObject newHeaders = new JSONObject();
			newHeaders.put("Subject", origHeaders.get("Subject")); // can copy
																	// other
																	// headers
																	// too
			String toemail = userProfile.getEmails().get(0);
			message.setHeaders(newHeaders.toString());
			
			Envelope newEnvelope = new Envelope();
			newEnvelope.setFrom(fromemail);
			newEnvelope.setRecipients(Collections.singletonList(myusername + "@synapse.org"));
			message.setEnvelope(newEnvelope);
			String emailMessageKey = EmailValidationUtil
					.getBucketKeyForEmail(toemail);
			if (EmailValidationUtil.doesFileExist(emailMessageKey, 2000L))
				EmailValidationUtil.deleteFile(emailMessageKey);

			URL url = new URL(repoEndpoint + MESSAGE_URI);
			HttpResponse response = conn.performRequest(url.toString(), "POST",
					EntityFactory.createJSONStringForEntity(message),
					requestHeaders);

			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
				// check that message is sent (file is created)
				assertTrue(EmailValidationUtil.doesFileExist(emailMessageKey, 60000L));
			}
			return response;

		} finally {
			is.close();
			out.close();
		}
	}

	@Test
	public void testCloudMailInMessage() throws Exception {
		for (String sampleFileName : SAMPLE_MESSAGES) {
			UserProfile userProfile = synapseOne.getUserProfile(user1ToDelete
					.toString());
			String fromemail = userProfile.getEmails().get(0);
			HttpResponse response = sendMessage("CloudMailInMessages/"
					+ sampleFileName, fromemail);
			assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusLine()
					.getStatusCode());
		}

	}

	// CloudMailIn uses basic authentication over https to authenticate itself.
	// This tests that the Synapse authentication filter rejects bad
	// credentials.
	@Test
	public void testCloudMailInMessageWrongCredentials() throws Exception {
		requestHeaders.put("Authorization",
				"Basic "+ (new String(Base64.encodeBase64(
						(username + ":ThisIsTheWrongPassword!!!")
										.getBytes()))));

		String sampleFileName = SAMPLE_MESSAGES[0];
		UserProfile userProfile = synapseOne.getUserProfile(user1ToDelete
				.toString());
		String fromemail = userProfile.getEmails().get(0);
		HttpResponse response = sendMessage("CloudMailInMessages/"
				+ sampleFileName, fromemail);
		assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatusLine()
				.getStatusCode());
	}

	// send from an invalid email and check that the error comes back
	@Test
	public void testResponseMessage() throws Exception {
		String sampleFileName = SAMPLE_MESSAGES[0];
		String fromemail = "notArealUser@sagebase.org";
		HttpResponse response = sendMessage("CloudMailInMessages/"
				+ sampleFileName, fromemail);
		String responseBody = IOUtils.toString(response.getEntity()
				.getContent());
		assertEquals(
				"Specified address notArealUser@sagebase.org is not registered with Synapse.",
				responseBody);
		String contentType = response.getEntity().getContentType().getValue();
		assertTrue(contentType, contentType.startsWith("text/plain"));
		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine()
				.getStatusCode());
	}
	
	
	@Test
	public void testCloudMailAuthorizationOK() throws Exception {
		UserProfile fromUserProfile = synapseOne.getUserProfile(user1ToDelete
				.toString());
		String fromemail = fromUserProfile.getEmails().get(0);
		
		UserProfile toUserProfile = synapseOne.getUserProfile(user1ToDelete
				.toString());
		String toUsername = toUserProfile.getUserName();
		
		AuthorizationCheckHeader ach = new AuthorizationCheckHeader();
		ach.setFrom(fromemail);
		ach.setTo(toUsername+"@synapse.org");

		URL url = new URL(repoEndpoint + AUTHORIZATION_URI);
		HttpResponse response = conn.performRequest(url.toString(), "POST",
				EntityFactory.createJSONStringForEntity(ach),
				requestHeaders);

		assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusLine()
				.getStatusCode());

	}

	@Test
	public void testCloudMailAuthorizationBadTo() throws Exception {
		UserProfile fromUserProfile = synapseOne.getUserProfile(user1ToDelete
				.toString());
		String fromemail = fromUserProfile.getEmails().get(0);
		
		// this is not a valid recipient
		String toUsername = UUID.randomUUID().toString();
		
		AuthorizationCheckHeader ach = new AuthorizationCheckHeader();
		ach.setFrom(fromemail);
		ach.setTo(toUsername+"@synapse.org");

		URL url = new URL(repoEndpoint + AUTHORIZATION_URI);
		HttpResponse response = conn.performRequest(url.toString(), "POST",
				EntityFactory.createJSONStringForEntity(ach),
				requestHeaders);

		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine()
				.getStatusCode());

	}

}
