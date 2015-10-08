package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.sagebionetworks.repo.manager.team.EmailParseUtil;
import org.sagebionetworks.repo.model.JoinTeamSignedToken;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.message.NotificationSettingsSignedToken;
import org.sagebionetworks.repo.util.SignedTokenUtil;
import org.sagebionetworks.util.SerializationUtils;

public class EmailUtilsTest {
	
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
		
		assertNull(EmailUtils.getDisplayName(up));
		assertEquals("jh", EmailUtils.getDisplayNameWithUserName(up));
		
		up.setFirstName("J");
		assertEquals("J", EmailUtils.getDisplayName(up));
		assertEquals("J (jh)", EmailUtils.getDisplayNameWithUserName(up));
		
		up.setLastName("H");
		assertEquals("J H", EmailUtils.getDisplayName(up));
		assertEquals("J H (jh)", EmailUtils.getDisplayNameWithUserName(up));
	}
	
	@Test
	public void testCreateSource() {
		assertEquals("noreply@synapse.org", EmailUtils.createSource(null, null));
		assertEquals("someuser@synapse.org", EmailUtils.createSource(null, "someuser"));
		assertEquals("Some User <noreply@synapse.org>", EmailUtils.createSource("Some User", null));
		assertEquals("Some User <someuser@synapse.org>", EmailUtils.createSource("Some User", "someuser"));
		assertEquals("=?utf-8?Q?Some_=C3=BC_User?= <someuser@synapse.org>", EmailUtils.createSource("Some ü User", "someuser"));
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

	@Test
	public void testCreateHtmlUnsubscribeFooter() throws Exception {
		String unsubscribeLink = "https://foo.bar.com#baz:12345";
		String footer = EmailUtils.createHtmlUnsubscribeFooter(unsubscribeLink);
		List<String> delims = Arrays.asList(new String[] {EmailUtils.TEMPLATE_KEY_ONE_CLICK_UNSUBSCRIBE});
		List<String> templatePieces = EmailParseUtil.splitEmailTemplate("message/unsubscribeFooter.html", delims);
		assertEquals(3, templatePieces.size());
		assertTrue(footer.startsWith(templatePieces.get(0)));
		assertTrue(footer.endsWith(templatePieces.get(2)));
		assertEquals(unsubscribeLink, EmailParseUtil.getTokenFromString(
				footer, templatePieces.get(0), templatePieces.get(2)));
	}

	@Test
	public void testCreateTextUnsubscribeFooter() throws Exception {
		String unsubscribeLink = "https://foo.bar.com#baz:12345";
		String footer = EmailUtils.createTextUnsubscribeFooter(unsubscribeLink);
		List<String> delims = Arrays.asList(new String[] {EmailUtils.TEMPLATE_KEY_ONE_CLICK_UNSUBSCRIBE});
		List<String> templatePieces = EmailParseUtil.splitEmailTemplate("message/unsubscribeFooter.txt", delims);
		assertEquals(3, templatePieces.size());
		assertTrue(footer.startsWith(templatePieces.get(0)));
		assertTrue(footer.endsWith(templatePieces.get(2)));
		assertEquals(unsubscribeLink, EmailParseUtil.getTokenFromString(
				footer, templatePieces.get(0), templatePieces.get(2)));
	}

	@Test
	public void testCreateEmailBodyFromHtml() throws Exception {
		assertEquals("foo", EmailUtils.createEmailBodyFromHtml("foo", null));		
		String messageWithFooter = EmailUtils.createEmailBodyFromHtml("foo", "link");
		assertTrue(messageWithFooter.startsWith(messageWithFooter));
		assertTrue(messageWithFooter.endsWith(EmailUtils.createHtmlUnsubscribeFooter("link")));
	}

	@Test
	public void testCreateEmailBodyFromText() throws Exception {
		assertEquals("foo", EmailUtils.createEmailBodyFromText("foo", null));
		String messageWithFooter = EmailUtils.createEmailBodyFromText("foo", "link");
		assertTrue(messageWithFooter.startsWith(messageWithFooter));
		assertTrue(messageWithFooter.endsWith(EmailUtils.createTextUnsubscribeFooter("link")));		
	}
}
