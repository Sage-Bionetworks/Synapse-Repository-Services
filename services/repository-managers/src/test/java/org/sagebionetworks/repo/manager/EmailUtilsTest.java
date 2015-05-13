package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.sagebionetworks.repo.model.JoinTeamSignedToken;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.message.NotificationSettingsSignedToken;
import org.sagebionetworks.repo.util.SignedTokenUtil;
import org.sagebionetworks.util.SerializationUtils;

import com.amazonaws.services.simpleemail.model.SendEmailRequest;

public class EmailUtilsTest {

	@Test
	public void testCreateEmailRequest() {
		SendEmailRequest request = EmailUtils.
				createEmailRequest("foo@bar.com", "foo", "bar", false, "foobar");
		assertEquals(Collections.singletonList("foo@bar.com"), request.getDestination().getToAddresses());
		assertEquals("foo", request.getMessage().getSubject().getData());
		assertEquals("bar", request.getMessage().getBody().getText().getData());
		assertNull(request.getMessage().getBody().getHtml());
		assertEquals("foobar <notifications@sagebase.org>", request.getSource());

		request = EmailUtils.
				createEmailRequest("foo@bar.com", "foo", "<html>bar</html>", true, "foobar");
		assertEquals(Collections.singletonList("foo@bar.com"), request.getDestination().getToAddresses());
		assertEquals("foo", request.getMessage().getSubject().getData());
		assertEquals("<html>bar</html>", request.getMessage().getBody().getHtml().getData());
		assertNull(request.getMessage().getBody().getText());
		assertEquals("foobar <notifications@sagebase.org>", request.getSource());
}
	
	@Test
	public void testReadMailTemplate() {
		Map<String,String> fieldValues = new HashMap<String,String>();
		fieldValues.put("#displayName#", "Foo Bar");
		fieldValues.put("#domain#", "Synapse");
		fieldValues.put("#username#", "foobar");
		String message = EmailUtils.readMailTemplate("message/WelcomeTemplate.txt", fieldValues);
		assertTrue(message.indexOf("#")<0); // all fields have been replaced
		assertTrue(message.indexOf("Foo Bar")>=0);
		assertTrue(message.indexOf("Synapse") >= 0);
		assertTrue(message.indexOf("foobar")>=0);
	}
	
	@Test
	public void testGetDisplayName() {
		UserProfile up = new UserProfile();
		up.setUserName("jh");
		assertEquals("jh", EmailUtils.getDisplayName(up));
		
		up.setFirstName("J");
		assertEquals("J (jh)", EmailUtils.getDisplayName(up));
		
		up.setLastName("H");
		assertEquals("J H (jh)", EmailUtils.getDisplayName(up));
	}
	
	
	@Test
	public void testValidateSynapsePortalHostOK() throws Exception {
		EmailUtils.validateSynapsePortalHost("https://www.synapse.org");
		EmailUtils.validateSynapsePortalHost("http://localhost");
		EmailUtils.validateSynapsePortalHost("http://127.0.0.1");
		EmailUtils.validateSynapsePortalHost("https://synapse-staging.sagebase.org");
	}

	@Test(expected=IllegalArgumentException.class)
	public void testValidateSynapsePortalHostNotOk() throws Exception {
		EmailUtils.validateSynapsePortalHost("www.spam.com");
	}
	
	@Test
	public void testCreateOneClickJoinTeamLink() throws Exception {
		String endpoint = "https://synapse.org/#";
		String userId = "111";
		String memberId = "222";
		String teamId = "333";
		String link = EmailUtils.createOneClickJoinTeamLink(endpoint, userId, memberId, teamId);
		assertTrue(link.startsWith(endpoint));
		
		JoinTeamSignedToken token = SerializationUtils.hexDecodeAndDeserialize(
				link.substring(endpoint.length()), JoinTeamSignedToken.class);
		SignedTokenUtil.validateToken(token);
		assertEquals(userId, token.getUserId());
		assertEquals(memberId, token.getMemberId());
		assertEquals(teamId, token.getTeamId());
		assertNotNull(token.getCreatedOn());
		assertNotNull(token.getHmac());
	}

	@Test
	public void testCreateOneUnsubscribeLink() throws Exception {
		String endpoint = "https://synapse.org/#";
		String userId = "111";
		String link = EmailUtils.createOneClickUnsubscribeLink(endpoint, userId);
		assertTrue(link.startsWith(endpoint));
		NotificationSettingsSignedToken token = SerializationUtils.hexDecodeAndDeserialize(
				link.substring(endpoint.length()), NotificationSettingsSignedToken.class);
		SignedTokenUtil.validateToken(token);
		assertEquals(userId, token.getUserId());
		assertNotNull(token.getCreatedOn());
		assertNotNull(token.getHmac());
		assertNull(token.getSettings().getMarkEmailedMessagesAsRead());
		assertFalse(token.getSettings().getSendEmailNotifications());
	}


}
