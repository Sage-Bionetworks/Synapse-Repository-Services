package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.junit.Test;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.message.multipart.Attachment;
import org.sagebionetworks.repo.model.message.multipart.MessageBody;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.sun.jersey.core.util.Base64;

public class SendRawEmailRequestBuilderTest {
	
	private static final String UNSUBSCRIBE_ENDPOINT = "https://www.synapse.org/#unsub:";

	@Test
	public void testCreateRawEmailRequest() throws Exception {
		String body = "this is the message body";
		SendRawEmailRequest request = (new SendRawEmailRequestBuilder())
				.withRecipientEmail("foo@bar.com")
				.withSubject("subject")
				.withBody(body)
				.withSenderUserName("foobar")
				.withSenderDisplayName("Foo Bar")
				.withNotificationUnsubscribeEndpoint(UNSUBSCRIBE_ENDPOINT)
				.withUserId("101")
				.build();
		assertEquals("Foo Bar <foobar@synapse.org>", request.getSource());
		assertEquals(1, request.getDestinations().size());
		assertEquals("foo@bar.com", request.getDestinations().get(0));
		MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()),
				new ByteArrayInputStream(request.getRawMessage().getData().array()));
		assertEquals(1, mimeMessage.getFrom().length);
		assertEquals("Foo Bar <foobar@synapse.org>", mimeMessage.getFrom()[0].toString());
		assertEquals("subject", mimeMessage.getSubject());

		assertTrue(mimeMessage.getContentType().startsWith("multipart/related"));
		MimeMultipart content = (MimeMultipart)mimeMessage.getContent();
		assertEquals(1, content.getCount());
		assertTrue(content.getContentType().startsWith("multipart/related"));
		BodyPart bodyPart = content.getBodyPart(0);
		assertTrue(bodyPart.getContentType().startsWith("text/plain"));
		String bodyContent = ((String)bodyPart.getContent());
		assertTrue(bodyContent.startsWith(body));
		assertTrue(bodyContent.indexOf(UNSUBSCRIBE_ENDPOINT)>0);
	}
	
	@Test
	public void testCreateRawEmailRequestWithOneTextAttachment() throws Exception {
		String body = "{\"html\":\"<div>text</div>\",\"attachments\":[{\"content\":\"MjAwCg==\",\"file_name\":\"in.txt\",\"content_type\":\"text/plain\",\"disposition\":\"attachment\",\"size\":\"4\"}]}";
		new SendRawEmailRequestBuilder()
				.withRecipientEmail("foo@bar.com")
				.withSubject("subject")
				.withBody(body)
				.withSenderUserName("foobar")
				.withSenderDisplayName("Foo Bar")
				.withNotificationUnsubscribeEndpoint(UNSUBSCRIBE_ENDPOINT)
				.withUserId("101")
				.build();
	}
	
	@Test
	public void testCreateRawEmailRequestNoUnsubEndpoint() throws Exception {
		String body = "this is the message body";
		SendRawEmailRequest request = (new SendRawEmailRequestBuilder())
				.withRecipientEmail("foo@bar.com")
				.withSubject("subject")
				.withBody(body)
				.withSenderUserName("foobar")
				.withSenderDisplayName("Foo Bar")
				.withUserId("101")
				.build();
		assertEquals("Foo Bar <foobar@synapse.org>", request.getSource());
		assertEquals(1, request.getDestinations().size());
		assertEquals("foo@bar.com", request.getDestinations().get(0));
		MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()),
				new ByteArrayInputStream(request.getRawMessage().getData().array()));
		assertEquals(1, mimeMessage.getFrom().length);
		assertEquals("Foo Bar <foobar@synapse.org>", mimeMessage.getFrom()[0].toString());
		assertEquals("subject", mimeMessage.getSubject());

		assertTrue(mimeMessage.getContentType().startsWith("multipart/related"));
		MimeMultipart content = (MimeMultipart)mimeMessage.getContent();
		assertEquals(1, content.getCount());
		assertTrue(content.getContentType().startsWith("multipart/related"));
		BodyPart bodyPart = content.getBodyPart(0);
		assertTrue(bodyPart.getContentType().startsWith("text/plain"));
		String bodyContent = ((String)bodyPart.getContent());
		assertTrue(bodyContent.startsWith(body));
		assertTrue(bodyContent.indexOf(StackConfiguration.getDefaultPortalNotificationEndpoint())>0);
	}
	
	@Test
	public void testCreateEmailBodyFromPlainTextJSON() throws Exception {
		String unsubscribeLink = UNSUBSCRIBE_ENDPOINT+"abcdef";
		MessageBody messageBody = new MessageBody();
		String somePlainText = "this is the message content.";
		messageBody.setPlain(somePlainText);
		MimeMultipart mm = SendRawEmailRequestBuilder.createEmailBodyFromJSON(
				EntityFactory.createJSONStringForEntity(messageBody), unsubscribeLink);
		mm = saveChanges(mm);

		assertEquals(1, mm.getCount());
		assertTrue(mm.getContentType().startsWith("multipart/related"));
		MimeMultipart bodyMM = (MimeMultipart)mm.getBodyPart(0).getContent();
		assertEquals(1, bodyMM.getCount());
		assertTrue(bodyMM.getContentType().startsWith("multipart/alternative"));
		assertEquals(1, bodyMM.getCount());
		BodyPart alternativePart = bodyMM.getBodyPart(0);
		assertTrue(alternativePart.getContentType().startsWith("text/plain"));
		assertTrue(((String)alternativePart.getContent()).startsWith(somePlainText));
	}
	
	@Test
	public void testCreateEmailBodyFromHtmlTextJSON() throws Exception {
		String unsubscribeLink = UNSUBSCRIBE_ENDPOINT+"abcdef";
		MessageBody messageBody = new MessageBody();
		String someHtmlText = "<div>this is the message content.</div>";
		messageBody.setHtml(someHtmlText);
		MimeMultipart mm = SendRawEmailRequestBuilder.createEmailBodyFromJSON(
				EntityFactory.createJSONStringForEntity(messageBody), unsubscribeLink);
		mm = saveChanges(mm);

		assertEquals(1, mm.getCount());
		assertTrue(mm.getContentType().startsWith("multipart/related"));
		MimeMultipart bodyMM = (MimeMultipart)mm.getBodyPart(0).getContent();
		assertEquals(1, bodyMM.getCount());
		assertTrue(bodyMM.getContentType().startsWith("multipart/alternative"));
		assertEquals(1, bodyMM.getCount());
		BodyPart alternativePart = bodyMM.getBodyPart(0);
		assertTrue(alternativePart.getContentType().startsWith("text/html"));
		assertTrue(((String)alternativePart.getContent()).startsWith(someHtmlText));
	}
	
	private static MimeMultipart saveChanges(MimeMultipart mm) throws MessagingException, IOException {
		// we have to ingest the body into a message to propagate the settings
		MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
		mimeMessage.setContent(mm);
		mimeMessage.saveChanges();
		return (MimeMultipart)mimeMessage.getContent();
	}
	
	@Test
	public void testCreateEmailBodyWithImageAttachment() throws Exception {
		String unsubscribeLink = UNSUBSCRIBE_ENDPOINT+"abcdef";
		MessageBody messageBody = new MessageBody();
		String someHtmlText = "<div>this is the message content.</div>";
		messageBody.setHtml(someHtmlText);
		Attachment attachment = new Attachment();
		String content = "abcdefghijklmnop";
		String base64Encoded = new String(Base64.encode(content));
		attachment.setContent(new String(base64Encoded));
		attachment.setContent_type("image/jpeg");
		attachment.setContent_id("101");
		attachment.setDisposition("foo");
		attachment.setFile_name("bar.jpg");
		attachment.setSize("1000");
		attachment.setUrl("http://foo.bar.com");
		messageBody.setAttachments(Collections.singletonList(attachment));
		MimeMultipart mm = SendRawEmailRequestBuilder.createEmailBodyFromJSON(
				EntityFactory.createJSONStringForEntity(messageBody), unsubscribeLink);
		mm = saveChanges(mm);

		assertTrue(mm.getContentType().startsWith("multipart/related"));
		assertEquals(2, mm.getCount());
		
		MimeMultipart bodyMM = (MimeMultipart)mm.getBodyPart(0).getContent();
		assertEquals(1, bodyMM.getCount());
		assertTrue(bodyMM.getContentType().startsWith("multipart/alternative"));
		assertEquals(1, bodyMM.getCount());
		BodyPart alternativePart = bodyMM.getBodyPart(0);
		assertTrue(((String)alternativePart.getContent()).startsWith(someHtmlText));
		
		BodyPart attachmentPart = mm.getBodyPart(1);
		String attachmentString = new String((byte[])attachmentPart.getContent());
		assertEquals(content, attachmentString);
		assertTrue(attachmentPart.getContentType(), attachmentPart.getContentType().startsWith("image/jpeg"));
		assertEquals(attachmentPart.getContentType(), attachmentPart.getHeader("Content-Type")[0]);
		assertEquals("foo; filename=bar.jpg", attachmentPart.getHeader("Content-Disposition")[0]);
		assertEquals("101", attachmentPart.getHeader("Content-ID")[0]);
		assertEquals("1000", attachmentPart.getHeader("size")[0]);
		assertEquals("http://foo.bar.com", attachmentPart.getHeader("url")[0]);
	}
	
}
