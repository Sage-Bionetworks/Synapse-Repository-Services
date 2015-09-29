package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.cloudmailin.Attachment;
import org.sagebionetworks.repo.model.message.cloudmailin.AuthorizationCheckHeader;
import org.sagebionetworks.repo.model.message.cloudmailin.Envelope;
import org.sagebionetworks.repo.model.message.cloudmailin.Message;
import org.sagebionetworks.repo.model.message.multipart.MessageBody;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

import com.amazonaws.util.json.JSONObject;

public class CloudMailInManagerImplTest {
	private CloudMailInManagerImpl cloudMailInManager = null;
	private PrincipalAliasDAO principalAliasDAO = null;
	
	private static final String NOTIFICATION_UNSUBSCRIBE_ENDPOINT = "https://www.synapse.org/#unsub:";
	
	@Before
	public void setUp() throws Exception {
		principalAliasDAO = Mockito.mock(PrincipalAliasDAO.class);
		cloudMailInManager = new CloudMailInManagerImpl(principalAliasDAO);
	}

	@Test
	public void testConvertMessage() throws Exception {
		Message message = new Message();
		Envelope envelope = new Envelope();
		message.setEnvelope(envelope);
		envelope.setFrom("foo@bar.com");
		envelope.setRecipients(Collections.singletonList("baz@synapse.org"));
		JSONObject headers = new JSONObject();
		headers.put("Subject", "test subject");
		headers.put("To", "to");
		headers.put("Cc", "cc");
		headers.put("Bcc", "bcc");
		message.setHeaders(headers.toString());
		String html = "<html><body>html content</body></html>";
		message.setHtml(html);
		String plain = "plain content";
		message.setPlain(plain);
		Attachment attachment = new Attachment();
		message.setAttachments(Collections.singletonList(attachment));
		
		Set<String> expectedRecipients = new HashSet<String>();
		Set<PrincipalAlias> recipientPrincipalAliases = new HashSet<PrincipalAlias>();
		Set<String> recipientUserNames = new HashSet<String>();
		PrincipalAlias toAlias = new PrincipalAlias();
		toAlias.setAlias("baz");
		toAlias.setPrincipalId(101L);
		expectedRecipients.add("101");
		recipientPrincipalAliases.add(toAlias);
		recipientUserNames.add("baz");
				
		PrincipalAlias fromAlias = new PrincipalAlias();
		fromAlias.setAlias("foo@bar.com");
		fromAlias.setPrincipalId(104L);
		when(principalAliasDAO.findPrincipalWithAlias("foo@bar.com")).thenReturn(fromAlias);
		
		when(principalAliasDAO.findPrincipalsWithAliases(eq(recipientUserNames))).thenReturn(recipientPrincipalAliases);
		
		MessageToUserAndBody mtub = 
				cloudMailInManager.convertMessage(message, NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
		
		assertEquals("application/json", mtub.getMimeType());
		MessageBody messageBody = EntityFactory.createEntityFromJSONString(mtub.getBody(), MessageBody.class);
		assertEquals(1, messageBody.getAttachments().size());
		assertEquals(html, messageBody.getHtml());
		assertEquals(plain, messageBody.getPlain());
		
		MessageToUser mtu = mtub.getMetadata();

		assertEquals("104", mtu.getCreatedBy());
		assertEquals("test subject", mtu.getSubject());
		assertEquals(expectedRecipients, mtu.getRecipients());
		assertEquals(NOTIFICATION_UNSUBSCRIBE_ENDPOINT, mtu.getNotificationUnsubscribeEndpoint());
		assertEquals("to", mtu.getTo());
		assertEquals("cc", mtu.getCc());
		assertEquals("bcc", mtu.getBcc());
	}
	
	@Test
	public void testConvertMessageAlternateFrom() throws Exception {
		Message message = new Message();
		Envelope envelope = new Envelope();
		message.setEnvelope(envelope);
		envelope.setRecipients(Collections.singletonList("baz@synapse.org"));
		JSONObject headers = new JSONObject();
		headers.put("From", "foo@bar.com");
		message.setHeaders(headers.toString());
		Set<PrincipalAlias> recipientPrincipalAliases = new HashSet<PrincipalAlias>();
		Set<String> recipientUserNames = new HashSet<String>();
		PrincipalAlias toAlias = new PrincipalAlias();
		toAlias.setAlias("baz");
		toAlias.setPrincipalId(101L);
		recipientPrincipalAliases.add(toAlias);
		recipientUserNames.add("baz");
				
		PrincipalAlias fromAlias = new PrincipalAlias();
		fromAlias.setAlias("foo@bar.com");
		fromAlias.setPrincipalId(104L);
		when(principalAliasDAO.findPrincipalWithAlias("foo@bar.com")).thenReturn(fromAlias);
		
		when(principalAliasDAO.findPrincipalsWithAliases(eq(recipientUserNames))).thenReturn(recipientPrincipalAliases);
		
		MessageToUserAndBody mtub = 
				cloudMailInManager.convertMessage(message, NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
		MessageToUser mtu = mtub.getMetadata();
		assertEquals("104", mtu.getCreatedBy());

	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testConvertMessageNoFrom() throws Exception {
		Message message = new Message();
		Envelope envelope = new Envelope();
		message.setEnvelope(envelope);
		envelope.setRecipients(Collections.singletonList("baz@synapse.org"));
		JSONObject headers = new JSONObject();
		headers.put("Subject", "test subject");
		message.setHeaders(headers.toString());
		
		cloudMailInManager.convertMessage(message, NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testConvertMessageNoTo() throws Exception {
		Message message = new Message();
		Envelope envelope = new Envelope();
		envelope.setFrom("foo@bar.com");
		message.setEnvelope(envelope);
		JSONObject headers = new JSONObject();
		headers.put("Subject", "test subject");
		message.setHeaders(headers.toString());
		
		cloudMailInManager.convertMessage(message, NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
	}
	
	@Test
	public void testCopyMessageToMessageBody() throws Exception {
		Message message = new Message();
		Envelope envelope = new Envelope();
		message.setEnvelope(envelope);
		String html = "<html><body>html content</body></html>";
		message.setHtml(html);
		String plain = "plain content";
		message.setPlain(plain);
		Attachment attachment = new Attachment();
		attachment.setContent("attachment content");
		attachment.setContent_id("999");
		attachment.setContent_type("text/plain");
		attachment.setDisposition("disposition");
		attachment.setFile_name("filename.txt");
		attachment.setSize("100");
		attachment.setUrl("http://foo.bar.com");
		message.setAttachments(Collections.singletonList(attachment));	
		
		MessageBody messageBody = CloudMailInManagerImpl.copyMessageToMessageBody(message);
		assertEquals(1, messageBody.getAttachments().size());
		org.sagebionetworks.repo.model.message.multipart.Attachment actual = 
				 messageBody.getAttachments().get(0);
		assertEquals(attachment.getContent(), actual.getContent());
		assertEquals(attachment.getContent_id(), actual.getContent_id());
		assertEquals(attachment.getContent_type(), actual.getContent_type());
		assertEquals(attachment.getDisposition(), actual.getDisposition());
		assertEquals(attachment.getFile_name(), actual.getFile_name());
		assertEquals(attachment.getSize(), actual.getSize());
		assertEquals(attachment.getUrl(), actual.getUrl());
		assertEquals(html, messageBody.getHtml());
		assertEquals(plain, messageBody.getPlain());
	}

	@Test
	public void testCopyMessageToMessageBodyWithReply() throws Exception {
		Message message = new Message();
		Envelope envelope = new Envelope();
		message.setEnvelope(envelope);
		String html = "<html><body>html content</body></html>";
		message.setHtml(html);
		String plain = "plain content";
		message.setPlain(plain);
		String reply = "reply content";
		message.setReply_plain(reply);
		Attachment attachment = new Attachment();
		attachment.setContent("attachment content");
		attachment.setContent_id("999");
		attachment.setContent_type("text/plain");
		attachment.setDisposition("disposition");
		attachment.setFile_name("filename.txt");
		attachment.setSize("100");
		attachment.setUrl("http://foo.bar.com");
		message.setAttachments(Collections.singletonList(attachment));	
		
		MessageBody messageBody = CloudMailInManagerImpl.copyMessageToMessageBody(message);
		assertEquals(1, messageBody.getAttachments().size());
		org.sagebionetworks.repo.model.message.multipart.Attachment actual = 
				 messageBody.getAttachments().get(0);
		assertEquals(attachment.getContent(), actual.getContent());
		assertEquals(attachment.getContent_id(), actual.getContent_id());
		assertEquals(attachment.getContent_type(), actual.getContent_type());
		assertEquals(attachment.getDisposition(), actual.getDisposition());
		assertEquals(attachment.getFile_name(), actual.getFile_name());
		assertEquals(attachment.getSize(), actual.getSize());
		assertEquals(attachment.getUrl(), actual.getUrl());
		assertEquals(plain, messageBody.getPlain());
		assertEquals(html, messageBody.getHtml());
	}
	
	
	@Test
	public void testLookupPrincipalIdForSynapseEmailAddress() throws Exception {
		Set<String> recipientUserNames = new HashSet<String>();
		Set<PrincipalAlias> recipientPrincipalAliases = new HashSet<PrincipalAlias>();
		PrincipalAlias toAlias = new PrincipalAlias();
		toAlias.setAlias("baz");
		Long principalId = 101L;
		toAlias.setPrincipalId(principalId);
		
		recipientUserNames.add("baz");
		recipientPrincipalAliases.add(toAlias);
		when(principalAliasDAO.findPrincipalsWithAliases(eq(recipientUserNames))).thenReturn(recipientPrincipalAliases);
		
		// check that case doesn't matter
		Map<String,String> expected = Collections.singletonMap("baz", principalId.toString());
		assertEquals(expected, cloudMailInManager.
				lookupPrincipalIdsForSynapseEmailAddresses(Collections.singleton("bAz@syNapse.oRg")));
		
		// make sure that we accept personal name + address format
		assertEquals(expected, cloudMailInManager.
				lookupPrincipalIdsForSynapseEmailAddresses(Collections.singleton("Baz ZZZ <bAz@syNapse.oRg>")));
	}

	@Test
	public void testLookupPrincipalIdForSynapseEmailAddressUnknwonAlias() throws Exception {
		assertTrue(cloudMailInManager.
				lookupPrincipalIdsForSynapseEmailAddresses(Collections.singleton("bAz@syNapse.oRg")).isEmpty());
	}

	@Test
	public void testLookupPrincipalIdForSynapseEmailAddressBADADDRESS() throws Exception {
		assertTrue(
				cloudMailInManager.lookupPrincipalIdsForSynapseEmailAddresses(Collections.singleton("bazXXXsynapse.org"))
				.isEmpty());
	}

	@Test
	public void testLookupPrincipalIdForSynapseEmailAddressWRONGdomain() throws Exception {
		assertTrue(
				cloudMailInManager.lookupPrincipalIdsForSynapseEmailAddresses(Collections.singleton("baz@google.com"))
				.isEmpty());
	}

	@Test
	public void testLookupPrincipalIdForRegisteredEmailAddress() throws Exception {
		String email = "foo@bar.com";
		PrincipalAlias toAlias = new PrincipalAlias();
		toAlias.setAlias(email);
		Long principalId = 101L;
		toAlias.setPrincipalId(principalId);
		when(principalAliasDAO.findPrincipalWithAlias(email)).thenReturn(toAlias);
		
		// check that case doesn't matter
		assertEquals(principalId, cloudMailInManager.lookupPrincipalIdForRegisteredEmailAddress(email));
		
		// make sure that we accept personal name + address format
		String namePlusEmail = "AAA BBB <"+email+">";
		assertEquals(principalId, cloudMailInManager.lookupPrincipalIdForRegisteredEmailAddress(namePlusEmail));
		
	}

	@Test(expected=IllegalArgumentException.class)
	public void testLookupPrincipalIdForRegisteredEmailAddressUnknownAlias() throws Exception {
		String email = "foo@bar.com";
		cloudMailInManager.lookupPrincipalIdForRegisteredEmailAddress(email);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testLookupPrincipalIdForRegisteredEmailAddressBADADDRESS() throws Exception {
		String email = "fooXXXbar.com";

		cloudMailInManager.lookupPrincipalIdForRegisteredEmailAddress(email);
	}

	@Test
	public void testAuthObject() throws Exception {
		String jsonString = "{\"size\":102400,\"to\":\"to@example.net\",\"from\":\"from+test@example.com\",\"helo_domain\":\"localhost\",\"remote_ip\":\"127.0.0.1\",\"spf\":{\"result\":\"pass\",\"domain\":\"example.com\"}}";
		AuthorizationCheckHeader ach = EntityFactory.createEntityFromJSONString(jsonString, AuthorizationCheckHeader.class);
		assertEquals(new Long(102400L), ach.getSize());

		assertEquals("from+test@example.com", ach.getFrom());
		assertEquals("localhost", ach.getHelo_domain());
		assertEquals("127.0.0.1", ach.getRemote_ip());
		assertEquals("{\"result\":\"pass\",\"domain\":\"example.com\"}", ach.getSpf());
		assertEquals("to@example.net", ach.getTo());
	}
	
	@Test
	public void testAuthCheckHappyCase() throws Exception {
		AuthorizationCheckHeader ach = new AuthorizationCheckHeader();
		ach.setFrom("foo@bar.com");
		ach.setTo("baz@synapse.org");
		Set<PrincipalAlias> recipientPrincipalAliases = new HashSet<PrincipalAlias>();
		Set<String> recipientUserNames = new HashSet<String>();
		PrincipalAlias toAlias = new PrincipalAlias();
		toAlias.setAlias("baz");
		toAlias.setPrincipalId(101L);
		recipientPrincipalAliases.add(toAlias);
		recipientUserNames.add("baz");

		PrincipalAlias fromAlias = new PrincipalAlias();
		fromAlias.setAlias("foo@bar.com");
		fromAlias.setPrincipalId(104L);
		when(principalAliasDAO.findPrincipalWithAlias("foo@bar.com")).thenReturn(fromAlias);
		
		when(principalAliasDAO.findPrincipalsWithAliases(eq(recipientUserNames))).thenReturn(recipientPrincipalAliases);
		
		cloudMailInManager.authorizeMessage(ach);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testAuthCheckBadFrom() throws Exception {
		AuthorizationCheckHeader ach = new AuthorizationCheckHeader();
		ach.setFrom("foo@bar.com");
		ach.setTo("baz@synapse.org");
		Set<PrincipalAlias> recipientPrincipalAliases = new HashSet<PrincipalAlias>();
		Set<String> recipientUserNames = new HashSet<String>();
		PrincipalAlias toAlias = new PrincipalAlias();
		toAlias.setAlias("baz");
		toAlias.setPrincipalId(101L);
		recipientPrincipalAliases.add(toAlias);
		recipientUserNames.add("baz");
		when(principalAliasDAO.findPrincipalsWithAliases(eq(recipientUserNames))).thenReturn(recipientPrincipalAliases);
		
		cloudMailInManager.authorizeMessage(ach);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testAuthCheckBadTo() throws Exception {
		AuthorizationCheckHeader ach = new AuthorizationCheckHeader();
		ach.setFrom("foo@bar.com");
		ach.setTo("baz@synapse.org");
		PrincipalAlias fromAlias = new PrincipalAlias();
		fromAlias.setAlias("foo@bar.com");
		fromAlias.setPrincipalId(104L);
		when(principalAliasDAO.findPrincipalWithAlias("foo@bar.com")).thenReturn(fromAlias);

		cloudMailInManager.authorizeMessage(ach);
	}

}
