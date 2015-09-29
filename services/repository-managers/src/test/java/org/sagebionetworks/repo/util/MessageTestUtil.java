package org.sagebionetworks.repo.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

public class MessageTestUtil {

	public static String getSubjectFromRawMessage(SendRawEmailRequest request) {
		try {
			MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()),
					new ByteArrayInputStream(request.getRawMessage().getData().array()));
			return mimeMessage.getSubject();
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
	}

	public static String getBodyFromRawMessage(SendRawEmailRequest request, String expectedMimeType) {
		try {
			MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()),
					new ByteArrayInputStream(request.getRawMessage().getData().array()));
			assertTrue(mimeMessage.getContentType().startsWith("multipart/related"));
			MimeMultipart content = (MimeMultipart)mimeMessage.getContent();
			assertEquals(1, content.getCount());
			assertTrue(content.getContentType().startsWith("multipart/related"));
			BodyPart bodyPart = content.getBodyPart(0);
			assertTrue(bodyPart.getContentType(), bodyPart.getContentType().startsWith(expectedMimeType));
			return ((String)bodyPart.getContent());
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String getHeaderFromRawMessage(SendRawEmailRequest request, String headerName) {
		try {
			MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()),
					new ByteArrayInputStream(request.getRawMessage().getData().array()));
			assertTrue(mimeMessage.getContentType().startsWith("multipart/related"));
			String[] headerValues = mimeMessage.getHeader(headerName);
			assertEquals(1, headerValues.length);
			return headerValues[0];
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
		
	}

}
