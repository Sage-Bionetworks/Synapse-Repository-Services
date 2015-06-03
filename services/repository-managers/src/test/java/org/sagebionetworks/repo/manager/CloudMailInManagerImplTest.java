package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.cloudmailin.Attachment;
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
		JSONObject headers = new JSONObject();
		headers.put("From", "foo@bar.com");
		headers.put("To", "baz@synapse.org");
		headers.put("Cc", "baz2@synapse.org");
		headers.put("Bcc", "baz3@synapse.org");
		headers.put("Subject", "test subject");
		message.setHeaders(headers.toString());
		String html = "<html><body>html content</body></html>";
		message.setHtml(html);
		String plain = "plain content";
		message.setPlain(plain);
		Attachment attachment = new Attachment();
		message.setAttachments(Collections.singletonList(attachment));
		
		Set<String> expectedRecipients = new HashSet<String>();
		PrincipalAlias toAlias = new PrincipalAlias();
		toAlias.setAlias("baz");
		toAlias.setPrincipalId(101L);
		when(principalAliasDAO.findPrincipalWithAlias("baz")).thenReturn(toAlias);
		expectedRecipients.add("101");
		
		PrincipalAlias ccAlias = new PrincipalAlias();
		ccAlias.setAlias("baz2");
		ccAlias.setPrincipalId(102L);
		when(principalAliasDAO.findPrincipalWithAlias("baz2")).thenReturn(ccAlias);
		expectedRecipients.add("102");
		
		PrincipalAlias bccAlias = new PrincipalAlias();
		bccAlias.setAlias("baz3");
		bccAlias.setPrincipalId(103L);
		when(principalAliasDAO.findPrincipalWithAlias("baz3")).thenReturn(bccAlias);
		expectedRecipients.add("103");
		
		PrincipalAlias fromAlias = new PrincipalAlias();
		fromAlias.setAlias("foo@bar.com");
		fromAlias.setPrincipalId(104L);
		when(principalAliasDAO.findPrincipalWithAlias("foo@bar.com")).thenReturn(fromAlias);
		
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
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testConvertMessageNoFrom() throws Exception {
		Message message = new Message();
		JSONObject headers = new JSONObject();
		headers.put("To", "baz@synapse.org");
		headers.put("Subject", "test subject");
		message.setHeaders(headers.toString());
		
		cloudMailInManager.convertMessage(message, NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testConvertMessageNoTo() throws Exception {
		Message message = new Message();
		JSONObject headers = new JSONObject();
		headers.put("From", "foo@bar.com");
		headers.put("Subject", "test subject");
		message.setHeaders(headers.toString());
		
		cloudMailInManager.convertMessage(message, NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
	}
	
	@Test
	public void testCopyMessageToMessageBody() throws Exception {
		Message message = new Message();
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
		assertNull(messageBody.getHtml());
		assertEquals(reply, messageBody.getPlain());
	}
	
	
	@Test
	public void testLookupPrincipalIdForSynapseEmailAddress() throws Exception {
		PrincipalAlias toAlias = new PrincipalAlias();
		toAlias.setAlias("baz");
		Long principalId = 101L;
		toAlias.setPrincipalId(principalId);
		when(principalAliasDAO.findPrincipalWithAlias("baz")).thenReturn(toAlias);
		
		// check that case doesn't matter
		assertEquals(Collections.singleton(principalId.toString()), cloudMailInManager.
				lookupPrincipalIdsForSynapseEmailAddresses(Collections.singleton("bAz@syNapse.oRg")));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testLookupPrincipalIdForSynapseEmailAddressUnknwonAlias() throws Exception {
		cloudMailInManager.lookupPrincipalIdsForSynapseEmailAddresses(Collections.singleton("bAz@syNapse.oRg"));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testLookupPrincipalIdForSynapseEmailAddressBADADDRESS() throws Exception {
		cloudMailInManager.lookupPrincipalIdsForSynapseEmailAddresses(Collections.singleton("bazXXXsynapse.org"));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testLookupPrincipalIdForSynapseEmailAddressWRONGdomain() throws Exception {
		cloudMailInManager.lookupPrincipalIdsForSynapseEmailAddresses(Collections.singleton("baz@google.com"));
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

}
