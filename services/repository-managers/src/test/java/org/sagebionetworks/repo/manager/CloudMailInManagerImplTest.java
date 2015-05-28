package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Collections;

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
		headers.put("Subject", "test subject");
		message.setHeaders(headers.toString());
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
		
		PrincipalAlias toAlias = new PrincipalAlias();
		toAlias.setAlias("baz");
		toAlias.setPrincipalId(101L);
		when(principalAliasDAO.findPrincipalWithAlias("baz")).thenReturn(toAlias);
		
		PrincipalAlias fromAlias = new PrincipalAlias();
		fromAlias.setAlias("foo@bar.com");
		fromAlias.setPrincipalId(102L);
		when(principalAliasDAO.findPrincipalWithAlias("foo@bar.com")).thenReturn(fromAlias);
		
		MessageToUserAndBody mtub = 
				cloudMailInManager.convertMessage(message, NOTIFICATION_UNSUBSCRIBE_ENDPOINT);
		
		assertEquals("application/json", mtub.getMimeType());
		MessageBody messageBody = EntityFactory.createEntityFromJSONString(mtub.getBody(), MessageBody.class);
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
		
		MessageToUser mtu = mtub.getMetadata();

		assertEquals("102", mtu.getCreatedBy());
		assertEquals("test subject", mtu.getSubject());
		assertEquals(Collections.singleton("101"), mtu.getRecipients());
	}

}
