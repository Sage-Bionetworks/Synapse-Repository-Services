package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.team.EmailParseUtil;
import org.sagebionetworks.repo.manager.token.TokenGenerator;
import org.sagebionetworks.repo.model.JoinTeamSignedToken;
import org.sagebionetworks.repo.model.SignedTokenInterface;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.message.NotificationSettingsSignedToken;
import org.sagebionetworks.util.SerializationUtils;

@ExtendWith(MockitoExtension.class)
public class EmailUtilsTest {
	
	@Mock
	private TokenGenerator mockTokenGenerator;
	
	@BeforeEach
	public void before() {
		
	}
	
	@Test
	public void testReadMailTemplate() {
		Map<String,String> fieldValues = new HashMap<String,String>();
		fieldValues.put("#displayName#", "Foo Bar");
		fieldValues.put("#domain#", "Synapse");
		fieldValues.put("#username#", "foobar");
		// Call under test
		String actual = EmailUtils.readMailTemplate("message/WelcomeTemplate.txt", fieldValues);
		
		String expected = "Hello Foo Bar,\r\n" + 
				"\r\n" + 
				"Welcome to Synapse!\r\n" + 
				"\r\n" + 
				"Here are the details of your account:\r\n" + 
				"User Name: foobar\r\n" + 
				"Full Name: Foo Bar\r\n" + 
				"\r\n" + 
				"If you did not mean to create this account, please contact us at synapseInfo@synapse.org.\r\n" + 
				"\r\n" + 
				"Synapse Administrator\r\n";
		assertEquals(expected, actual);
	}
	
	@Test
	public void testReadMailTemplateWithNullFieldReplacement() {
		Map<String,String> fieldValues = new HashMap<String,String>();
		fieldValues.put("#displayName#", "Foo Bar");
		fieldValues.put("#domain#", "Synapse");
		fieldValues.put("#username#", null);
		// Call under test
		String actual = EmailUtils.readMailTemplate("message/WelcomeTemplate.txt", fieldValues);
		
		String expected = "Hello Foo Bar,\r\n" + 
				"\r\n" + 
				"Welcome to Synapse!\r\n" + 
				"\r\n" + 
				"Here are the details of your account:\r\n" + 
				"User Name: \r\n" + 
				"Full Name: Foo Bar\r\n" + 
				"\r\n" + 
				"If you did not mean to create this account, please contact us at synapseInfo@synapse.org.\r\n" + 
				"\r\n" + 
				"Synapse Administrator\r\n";
		assertEquals(expected, actual);
	}
	
	@Test
	public void testGetDisplayName() {
		UserProfile up = new UserProfile();
		up.setUserName("jh");
		
		// Call under test
		assertNull(EmailUtils.getDisplayName(up));
		assertEquals("jh", EmailUtils.getDisplayNameWithUsername(up));
		
		up.setFirstName("J");
		
		// Call under test
		assertEquals("J", EmailUtils.getDisplayName(up));
		assertEquals("J (jh)", EmailUtils.getDisplayNameWithUsername(up));
		
		up.setLastName("H");
		
		// Call under test
		assertEquals("J H", EmailUtils.getDisplayName(up));
		assertEquals("J H (jh)", EmailUtils.getDisplayNameWithUsername(up));
	}
	
	@Test
	public void testGetDisplayNameOrUsernameWithoutFirstLast() {
		UserProfile up = new UserProfile();
		up.setUserName("user");
		
		// Call under test
		assertEquals("user", EmailUtils.getDisplayNameOrUsername(up));
	}
	
	@Test
	public void testGetDisplayNameOrUsernameWithEmptyFirstLast() {
		UserProfile up = new UserProfile();
		up.setFirstName("");
		up.setLastName(null);
		up.setUserName("user");
		
		// Call under test
		assertEquals("user", EmailUtils.getDisplayNameOrUsername(up));
	}
	
	@Test
	public void testGetDisplayNameOrUsernameWithFirstLast() {
		UserProfile up = new UserProfile();
		up.setFirstName("First");
		up.setLastName("Last");
		up.setUserName("user");
		
		// Call under test
		assertEquals("First Last", EmailUtils.getDisplayNameOrUsername(up));
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
		EmailUtils.validateSynapsePortalHost("https://synapse.org");
		EmailUtils.validateSynapsePortalHost("http://localhost");
		EmailUtils.validateSynapsePortalHost("http://127.0.0.1");
		EmailUtils.validateSynapsePortalHost("https://synapse-staging.sagebase.org");
		EmailUtils.validateSynapsePortalHost("https://staging.accounts.sagebionetworks.org/register2?emailValidationSignedToken=");
		EmailUtils.validateSynapsePortalHost("https://www.synapse.org/Portal.html#!PasswordReset:");
	}
	
	@Test
	public void testValidateSynapsePortalHostWithAllowedDomainsWithoutProtocol() {
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			EmailUtils.validateSynapsePortalHost("www.synapse.org");	
		}).getMessage();
			
		assertEquals("The provided endpoint creates an invalid URL. Potential causes are: no protocol is specified, an unknown protocol is found, the spec is null, the parsed URL fails to comply with the specific syntax of the associated protocol.", errorMessage);
	}
	
	@Test
	public void testValidateSynapsePortalHostWithNullUrlString() {
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			EmailUtils.validateSynapsePortalHost(null);
		}).getMessage();
		
		assertEquals("urlString is required.", errorMessage);
	}

	@Test
	public void testValidateSynapsePortalHostWithNotAllowedDomain() {
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			EmailUtils.validateSynapsePortalHost("https://www.spam.com");
		}).getMessage();
		
		assertEquals("The provided parameter is not a valid Synapse endpoint.", errorMessage);
	}

	@Test
	public void testValidateSynapsePortalHostWithNotAllowedDomainContainedAllowedDomain() {
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			EmailUtils.validateSynapsePortalHost("https://www.notSynapse.org");
		}).getMessage();
		
		assertEquals("The provided parameter is not a valid Synapse endpoint.", errorMessage);
	}
	
	@Test
	public void testValidateSynapsePortalHostWithInvalidSyntax() {
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			EmailUtils.validateSynapsePortalHost("https//:www.synapse.org");
		}).getMessage();
		
		assertEquals("The provided endpoint creates an invalid URL. Potential causes are: no protocol is specified, an unknown protocol is found, the spec is null, the parsed URL fails to comply with the specific syntax of the associated protocol.", errorMessage);
	}
	
	@Test
	public void testValidateSynapsePortalHostWithInvalidBaseDomain() {
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			EmailUtils.validateSynapsePortalHost("https://www.syn://apse.org");
		}).getMessage();
		
		assertEquals("The provided endpoint does not contain a valid base domain.", errorMessage);
	}
	
	@Test
	public void testValidateSynapsePortalHostWithFile() {
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			EmailUtils.validateSynapsePortalHost("file:///path/to/file");
		}).getMessage();
		
		assertEquals("The provided endpoint does not contain a valid base domain.", errorMessage);
	}
	
	@Test
	public void testValidateSynapsePortalHostWithInvalidProtocol() {
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			EmailUtils.validateSynapsePortalHost("sptth://www.synapse.org");
		}).getMessage();
		
		assertEquals("The provided endpoint creates an invalid URL. Potential causes are: no protocol is specified, an unknown protocol is found, the spec is null, the parsed URL fails to comply with the specific syntax of the associated protocol.", errorMessage);
	}
	
	@Test
	public void testCreateOneClickJoinTeamLink() throws Exception {
		String endpoint = "https://synapse.org/#";
		String userId = "111";
		String memberId = "222";
		String teamId = "333";
		Date createdOn = new Date();
		
		doAnswer(invocation -> {
			SignedTokenInterface token = (SignedTokenInterface) invocation.getArguments()[0];
			token.setHmac("signed");
			return null;
		}).when(mockTokenGenerator).signToken(any());
		
		// Call under test
		String link = EmailUtils.createOneClickJoinTeamLink(endpoint, userId, memberId, teamId, createdOn, mockTokenGenerator);
		
		verify(mockTokenGenerator).signToken(any());
		assertTrue(link.startsWith(endpoint));
		
		JoinTeamSignedToken token = SerializationUtils.hexDecodeAndDeserialize(
				link.substring(endpoint.length()), JoinTeamSignedToken.class);
		assertEquals(userId, token.getUserId());
		assertEquals(memberId, token.getMemberId());
		assertEquals(teamId, token.getTeamId());
		assertEquals(createdOn, token.getCreatedOn());
		assertNotNull(token.getHmac());
	}

	@Test
	public void testCreateOneClickUnsubscribeLink() throws Exception {
		String endpoint = "https://synapse.org/#";
		String userId = "111";
		
		doAnswer(invocation -> {
			SignedTokenInterface token = (SignedTokenInterface) invocation.getArguments()[0];
			token.setHmac("signed");
			return null;
		}).when(mockTokenGenerator).signToken(any());
		
		// Call under test
		String link = EmailUtils.createOneClickUnsubscribeLink(endpoint, userId, mockTokenGenerator);
		
		verify(mockTokenGenerator).signToken(any());
		assertTrue(link.startsWith(endpoint));
		NotificationSettingsSignedToken token = SerializationUtils.hexDecodeAndDeserialize(
				link.substring(endpoint.length()), NotificationSettingsSignedToken.class);
		assertEquals(userId, token.getUserId());
		assertNotNull(token.getCreatedOn());
		assertNotNull(token.getHmac());
		assertNull(token.getSettings().getMarkEmailedMessagesAsRead());
		assertFalse(token.getSettings().getSendEmailNotifications());
	}

	@Test
	public void testCreateHtmlUnsubscribeLink() throws Exception {
		String unsubscribeLink = "https://foo.bar.com#baz:12345";
		
		// Call under test
		String footer = EmailUtils.createHtmlUnsubscribeLink(unsubscribeLink);
		
		List<String> delims = Arrays.asList(new String[] {EmailUtils.TEMPLATE_KEY_ONE_CLICK_UNSUBSCRIBE});
		List<String> templatePieces = EmailParseUtil.splitEmailTemplate("message/unsubscribeLink.html", delims);
		assertEquals(3, templatePieces.size());
		assertTrue(footer.startsWith(templatePieces.get(0)));
		assertTrue(footer.endsWith(templatePieces.get(2)));
		assertEquals(unsubscribeLink, EmailParseUtil.getTokenFromString(
				footer, templatePieces.get(0), templatePieces.get(2)));
	}

	@Test
	public void testCreateHtmlProfileSettingLink() throws Exception {
		String profileSettingLink = "https://synapse.org/!#Profile:edit";

		// Call under test
		String footer = EmailUtils.createHtmlUserProfileSettingLink(profileSettingLink);
		
		List<String> delims = Arrays.asList(new String[] {EmailUtils.TEMPLATE_KEY_PROFILE_SETTING_LINK});
		List<String> templatePieces = EmailParseUtil.splitEmailTemplate("message/userProfileSettingLink.html", delims);
		assertEquals(3, templatePieces.size());
		assertTrue(footer.startsWith(templatePieces.get(0)));
		assertTrue(footer.endsWith(templatePieces.get(2)));
		assertEquals(profileSettingLink, EmailParseUtil.getTokenFromString(
				footer, templatePieces.get(0), templatePieces.get(2)));
	}

	@Test
	public void testCreateTextUnsubscribeLink() throws Exception {
		String unsubscribeLink = "https://foo.bar.com#baz:12345";
		
		// Call under test
		String footer = EmailUtils.createTextUnsubscribeLink(unsubscribeLink);
		
		List<String> delims = Arrays.asList(new String[] {EmailUtils.TEMPLATE_KEY_ONE_CLICK_UNSUBSCRIBE});
		List<String> templatePieces = EmailParseUtil.splitEmailTemplate("message/unsubscribeLink.txt", delims);
		assertEquals(3, templatePieces.size());
		assertTrue(footer.startsWith(templatePieces.get(0)));
		assertTrue(footer.endsWith(templatePieces.get(2)));
		assertEquals(unsubscribeLink, EmailParseUtil.getTokenFromString(
				footer, templatePieces.get(0), templatePieces.get(2)));
	}

	@Test
	public void testCreateTextProfileSettingLink() throws Exception {
		String profileSettingLink = "https://synapse.org/!#Profile:edit";
		
		// Call under test
		String footer = EmailUtils.createTextUserProfileSettingLink(profileSettingLink);
		
		List<String> delims = Arrays.asList(new String[] {EmailUtils.TEMPLATE_KEY_PROFILE_SETTING_LINK});
		List<String> templatePieces = EmailParseUtil.splitEmailTemplate("message/userProfileSettingLink.txt", delims);
		assertEquals(3, templatePieces.size());
		assertTrue(footer.startsWith(templatePieces.get(0)));
		assertTrue(footer.endsWith(templatePieces.get(2)));
		assertEquals(profileSettingLink, EmailParseUtil.getTokenFromString(
				footer, templatePieces.get(0), templatePieces.get(2)));
	}

	@Test
	public void testCreateEmailBodyFromHtml() throws Exception {
		assertEquals("foo", EmailUtils.createEmailBodyFromHtml("foo", null, null));

		String messageWithUnsubscribeLinkFooter = EmailUtils.createEmailBodyFromHtml("foo", "link", null);
		assertTrue(messageWithUnsubscribeLinkFooter.contains(EmailUtils.createHtmlUnsubscribeLink("link")));

		String messageWithUserProfileLinkFooter = EmailUtils.createEmailBodyFromHtml("foo", null, "link");
		assertTrue(messageWithUserProfileLinkFooter.contains(EmailUtils.createHtmlUserProfileSettingLink("link")));

		String messageWithBothLinksFooter = EmailUtils.createEmailBodyFromHtml("foo", "link1", "link2");
		assertTrue(messageWithBothLinksFooter.contains(EmailUtils.createHtmlUnsubscribeLink("link1")));
		assertTrue(messageWithBothLinksFooter.contains(EmailUtils.createHtmlUserProfileSettingLink("link2")));
	}

	@Test
	public void testCreateEmailBodyFromText() throws Exception {
		assertEquals("foo", EmailUtils.createEmailBodyFromText("foo", null, null));

		String messageWithUnsubscribeLinkFooter = EmailUtils.createEmailBodyFromText("foo", "link", null);
		assertTrue(messageWithUnsubscribeLinkFooter.contains(EmailUtils.createTextUnsubscribeLink("link")));

		String messageWithUserProfileLinkFooter = EmailUtils.createEmailBodyFromText("foo", null, "link");
		assertTrue(messageWithUserProfileLinkFooter.contains(EmailUtils.createTextUserProfileSettingLink("link")));

		String messageWithBothLinksFooter = EmailUtils.createEmailBodyFromText("foo", "link1", "link2");
		assertTrue(messageWithBothLinksFooter.contains(EmailUtils.createTextUnsubscribeLink("link1")));
		assertTrue(messageWithBothLinksFooter.contains(EmailUtils.createTextUserProfileSettingLink("link2")));
	}
	
	@Test
	public void testgetEmailAddressForPrincipalName() {
		assertEquals("Foo Bar <foobar@synapse.org>", EmailUtils.getEmailAddressForPrincipalName("Foo Bar"));
	}
	
}
