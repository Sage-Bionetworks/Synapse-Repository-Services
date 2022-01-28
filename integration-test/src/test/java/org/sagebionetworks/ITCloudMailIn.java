package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.message.cloudmailin.AuthorizationCheckHeader;
import org.sagebionetworks.repo.model.message.cloudmailin.Envelope;
import org.sagebionetworks.repo.model.message.cloudmailin.Headers;
import org.sagebionetworks.repo.model.message.cloudmailin.Message;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientImpl;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;

import com.amazonaws.util.IOUtils;

public class ITCloudMailIn extends BaseITTest {
	
	private static SimpleHttpClient simpleHttpClient;
	private static String repoEndpoint;
	private static String username;
	private static String password;
	private static final String[] SAMPLE_MESSAGES = { "SimpleMessage.json", "MessageWithAttachment.json" };

	private static final String MESSAGE_URI = "/cloudMailInMessage";
	private static final String AUTHORIZATION_URI = "/cloudMailInAuthorization";

	Map<String, String> requestHeaders;

	@BeforeAll
	public static void beforeClass() throws Exception {
		repoEndpoint = config.getRepositoryServiceEndpoint();
		username = config.getCloudMailInUser();
		password = config.getCloudMailInPassword();
		simpleHttpClient = new SimpleHttpClientImpl();
	}

	@BeforeEach
	public void before() throws Exception {
		// get the underlying SharedClientConnection so we can add the basic
		// authentication header
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

	private SimpleHttpResponse sendMessage(String jsonFileName, String fromemail)
			throws Exception {
		InputStream is = ITCloudMailIn.class.getClassLoader()
				.getResourceAsStream(jsonFileName);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		UserProfile userProfile = synapse.getUserProfile(userToDelete
				.toString());
		String myusername = userProfile.getUserName();
		try {
			IOUtils.copy(is, out);
			String messageJson = out.toString("utf-8");
			Message message = EntityFactory.createEntityFromJSONString(
					messageJson, Message.class);
			Headers newHeaders = new Headers();
			newHeaders.setSubject(message.getHeaders().getSubject()); // can copy other header too
			String toemail = userProfile.getEmails().get(0);
			message.setHeaders(newHeaders);

			Envelope newEnvelope = new Envelope();
			newEnvelope.setFrom(fromemail);
			newEnvelope.setRecipients(Collections.singletonList(myusername + "@synapse.org"));
			message.setEnvelope(newEnvelope);
			String emailMessageKey = EmailValidationUtil
					.getBucketKeyForEmail(toemail);
			if (EmailValidationUtil.doesFileExist(emailMessageKey, 2000L))
				EmailValidationUtil.deleteFile(emailMessageKey);

			URL url = new URL(repoEndpoint + MESSAGE_URI);
			String body = EntityFactory.createJSONStringForEntity(message);
			SimpleHttpRequest simpleRequest = new SimpleHttpRequest();
			simpleRequest.setHeaders(requestHeaders);
			simpleRequest.setUri(url.toString());
			SimpleHttpResponse response = simpleHttpClient.post(simpleRequest , body);

			if (response.getStatusCode() == HttpStatus.SC_NO_CONTENT) {
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
			UserProfile userProfile = synapse.getUserProfile(userToDelete
					.toString());
			String fromemail = userProfile.getEmails().get(0);
			SimpleHttpResponse response = sendMessage("CloudMailInMessages/"
					+ sampleFileName, fromemail);
			assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());
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
		UserProfile userProfile = synapse.getUserProfile(userToDelete
				.toString());
		String fromemail = userProfile.getEmails().get(0);
		SimpleHttpResponse response = sendMessage("CloudMailInMessages/"
				+ sampleFileName, fromemail);
		assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatusCode());
	}

	@Test
	public void testCloudMailAuthorizationOK() throws Exception {
		UserProfile fromUserProfile = synapse.getUserProfile(userToDelete
				.toString());
		String fromemail = fromUserProfile.getEmails().get(0);

		UserProfile toUserProfile = synapse.getUserProfile(userToDelete
				.toString());
		String toUsername = toUserProfile.getUserName();

		AuthorizationCheckHeader ach = new AuthorizationCheckHeader();
		ach.setFrom(fromemail);
		ach.setTo(toUsername+"@synapse.org");

		URL url = new URL(repoEndpoint + AUTHORIZATION_URI);
		String body = EntityFactory.createJSONStringForEntity(ach);
		SimpleHttpRequest simpleRequest = new SimpleHttpRequest();
		simpleRequest.setHeaders(requestHeaders);
		simpleRequest.setUri(url.toString());
		SimpleHttpResponse response = simpleHttpClient.post(simpleRequest , body);

		assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusCode());

	}

	@Test
	public void testCloudMailAuthorizationBadTo() throws Exception {
		UserProfile fromUserProfile = synapse.getUserProfile(userToDelete
				.toString());
		String fromemail = fromUserProfile.getEmails().get(0);

		// this is not a valid recipient
		String toUsername = UUID.randomUUID().toString();

		AuthorizationCheckHeader ach = new AuthorizationCheckHeader();
		ach.setFrom(fromemail);
		ach.setTo(toUsername+"@synapse.org");

		URL url = new URL(repoEndpoint + AUTHORIZATION_URI);
		String body = EntityFactory.createJSONStringForEntity(ach);
		SimpleHttpRequest simpleRequest = new SimpleHttpRequest();
		simpleRequest.setHeaders(requestHeaders);
		simpleRequest.setUri(url.toString());
		SimpleHttpResponse response = simpleHttpClient.post(simpleRequest , body);

		assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusCode());
		String contentType = response.getFirstHeader("Content-Type").getValue();
		assertTrue(contentType.startsWith("text/plain"), contentType);
	}

}
