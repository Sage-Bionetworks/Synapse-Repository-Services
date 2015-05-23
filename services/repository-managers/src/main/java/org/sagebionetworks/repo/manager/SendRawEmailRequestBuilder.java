package org.sagebionetworks.repo.manager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.http.entity.ContentType;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.message.cloudmailin.Attachment;
import org.sagebionetworks.repo.model.message.cloudmailin.Message;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;

/*
 * If sender is null then the 'notification email address' is used
 * If unsubscribeLink is null then no such link is added.
 * 
 */
public class SendRawEmailRequestBuilder {
	private String recipientEmail=null;
	private String subject=null;
	private String body=null;
	private String senderDisplayName=null;
	private String senderUserName=null;
	private String notificationUnsubscribeEndpoint=null;
	private String userId=null;
	
		
	public SendRawEmailRequestBuilder withRecipientEmail(String recipientEmail) {
		this.recipientEmail=recipientEmail;
		return this;
	}
	
	public SendRawEmailRequestBuilder withSubject(String subject) {
		this.subject=subject;
		return this;
	}
	
	public SendRawEmailRequestBuilder withBody(String body) {
		this.body=body;
		return this;
	}
	
	public SendRawEmailRequestBuilder withSenderDisplayName(String senderDisplayName) {
		this.senderDisplayName=senderDisplayName;
		return this;
	}
	
	public SendRawEmailRequestBuilder withSenderUserName(String senderUserName) {
		this.senderUserName=senderUserName;
		return this;
	}
	
	public SendRawEmailRequestBuilder withNotificationUnsubscribeEndpoint(String notificationUnsubscribeEndpoint) {
		this.notificationUnsubscribeEndpoint=notificationUnsubscribeEndpoint;
		return this;
	}
	
	public SendRawEmailRequestBuilder withUserId(String userId) {
		this.userId=userId;
		return this;
	}
		
	public SendRawEmailRequest build() {
		String source = EmailUtils.createSource(senderDisplayName, senderUserName);        
        // Create the subject and body of the message
        if (subject == null) subject = "";
        
        String unsubscribeLink = null;
        if (notificationUnsubscribeEndpoint!=null && userId!=null) {
        	unsubscribeLink = EmailUtils.
    			createOneClickUnsubscribeLink(notificationUnsubscribeEndpoint, userId);
        }

        String multipart = createEmailBodyFromJSON(body, unsubscribeLink);
        
        RawMessage rawMessage = new RawMessage(); // TODO set content from 'body', 'subject', 'recipientEmail'
    
         // Assemble the email
		SendRawEmailRequest request = new SendRawEmailRequest()
				.withSource(source)
				.withRawMessage(rawMessage);
		
		return request;
	}
	
	// TODO http://mintylikejava.blogspot.hk/2014/05/example-of-sending-email-with-multipal.html
	public static String createEmailBodyFromJSON(String messageBody, String unsubscribeLink) {
		try {
			Message message = null;
			try {
				message = EntityFactory.createEntityFromJSONString(messageBody, Message.class);
			} catch (JSONObjectAdapterException e) {
				throw new RuntimeException(e);
			}
			String plain = message.getPlain();
			String html = message.getHtml();
			List<Attachment> attachments = message.getAttachments();
			
		    MimeMultipart mp = new MimeMultipart();
		    if (html!=null) {
		    	BodyPart part = new MimeBodyPart();
		    	part.setContent(EmailUtils.createEmailBodyFromHtml(html, unsubscribeLink), 
		    			ContentType.TEXT_HTML.getMimeType());
		    	mp.addBodyPart(part);
		    }
		 
		    if (plain!=null) {
		    	BodyPart part = new MimeBodyPart();
		    	part.setContent(EmailUtils.createEmailBodyFromText(plain, unsubscribeLink), 
		    			ContentType.TEXT_PLAIN.getMimeType());
		    	mp.addBodyPart(part);
		    }
		    
		    if (attachments!=null) {
		    	for (Attachment attachment : attachments) {
			    	BodyPart part = new MimeBodyPart();
			    	// TODO what do we do with the other attachment metadata?
			    	part.setContent(attachment.getContent(), attachment.getContent_type());
			    	mp.addBodyPart(part);
		    	}
		    }
		    
		    ByteArrayOutputStream baos = new ByteArrayOutputStream();
		    try {
		    	mp.writeTo(baos);
		    } finally {
		    	baos.close();
		    }
		    
			return baos.toString();
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
