package org.sagebionetworks.repo.manager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import org.apache.commons.net.util.Base64;
import org.apache.http.entity.ContentType;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.manager.token.TokenGeneratorSingleton;
import org.sagebionetworks.repo.model.message.multipart.Attachment;
import org.sagebionetworks.repo.model.message.multipart.MessageBody;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.utils.ContentTypeUtil;

/*
 * If sender is null then the 'notification email address' is used
 * If unsubscribeLink is null then no such link is added.
 * 
 */
public class SendRawEmailRequestBuilder {
	private String recipientEmail=null;
	private String subject=null;
	private String to=null;
	private String cc=null;
	private String bcc=null;
	private String body=null;
	private BodyType bodyType=null;
	private String senderDisplayName=null;
	private String senderUserName=null;
	private String notificationUnsubscribeEndpoint=null;
	private String userProfileSettingEndpoint=null;
	private String userId=null;
	private boolean withUnsubscribeLink=false;
	private boolean withProfileSettingLink=false;
	private boolean isNotificationMessage=false;
	
	public enum BodyType{JSON, PLAIN_TEXT, HTML};


	public SendRawEmailRequestBuilder withRecipientEmail(String recipientEmail) {
		this.recipientEmail=recipientEmail;
		return this;
	}

	public SendRawEmailRequestBuilder withSubject(String subject) {
		this.subject=subject;
		return this;
	}

	public SendRawEmailRequestBuilder withTo(String to) {
		this.to=to;
		return this;
	}

	public SendRawEmailRequestBuilder withCc(String cc) {
		this.cc=cc;
		return this;
	}

	public SendRawEmailRequestBuilder withBcc(String bcc) {
		this.bcc=bcc;
		return this;
	}

	public SendRawEmailRequestBuilder withBody(String body, BodyType bodyType) {
		this.body=body;
		this.bodyType=bodyType;
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

	public SendRawEmailRequestBuilder withUnsubscribeLink(Boolean withUnsubscribeLink) {
		if (withUnsubscribeLink != null) this.withUnsubscribeLink=withUnsubscribeLink;
		return this;
	}

	public SendRawEmailRequestBuilder withUserProfileSettingEndpoint(String userProfileSettingEndpoint) {
		this.userProfileSettingEndpoint=userProfileSettingEndpoint;
		return this;
	}

	public SendRawEmailRequestBuilder withProfileSettingLink(Boolean withProfileSettingLink) {
		if (withProfileSettingLink != null) this.withProfileSettingLink=withProfileSettingLink;
		return this;
	}
	
	public SendRawEmailRequestBuilder withIsNotificationMessage(Boolean isNotificationMessage) {
		if (isNotificationMessage != null) this.isNotificationMessage=isNotificationMessage;
		return this;
	}

	public SendRawEmailRequestBuilder withUserId(String userId) {
		this.userId=userId;
		return this;
	}

	public SendRawEmailRequest build()  {
		String source = null;
		if (isNotificationMessage) {
			source = EmailUtils.createSource(null, null);
		} else {
			source = EmailUtils.createSource(senderDisplayName, senderUserName);
		}
		// Create the subject and body of the message
		if (subject == null) subject = "";

		StackConfiguration config = StackConfigurationSingleton.singleton();
		String unsubscribeLink = null;
		String profileSettingLink = null;
		if (withUnsubscribeLink && notificationUnsubscribeEndpoint==null) {
			notificationUnsubscribeEndpoint = config.getDefaultPortalNotificationEndpoint();
		}
		if (withProfileSettingLink && userProfileSettingEndpoint == null) {
			userProfileSettingEndpoint = config.getDefaultPortalProfileSettingEndpoint();
		}
		if (withUnsubscribeLink && userId!=null) {
			unsubscribeLink = EmailUtils.
					createOneClickUnsubscribeLink(notificationUnsubscribeEndpoint, userId, TokenGeneratorSingleton.singleton());
		}
		if (withProfileSettingLink && userId != null) {
			profileSettingLink = userProfileSettingEndpoint;
		}

		MimeMultipart multipart;
		try {
			switch (bodyType) {
				case JSON:
					multipart = createEmailBodyFromJSON(body, unsubscribeLink, profileSettingLink);
					break;
				case PLAIN_TEXT:
					multipart = createEmailBodyFromPLAINText(body, unsubscribeLink, profileSettingLink);
					break;
				case HTML:
					multipart = createEmailBodyFromHTMLText(body, unsubscribeLink, profileSettingLink);
					break;
				default:
					throw new IllegalStateException("Unexpected type " + bodyType);
			}
		} catch (MessagingException e){
			throw new RuntimeException(e);
		}

		Properties props = new Properties();
		// sets SMTP server properties
		props.setProperty("mail.transport.protocol", "aws");

		Session mailSession = Session.getInstance(props);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			MimeMessage msg = new MimeMessage(mailSession);
			msg.setFrom(new InternetAddress(source));
			msg.setRecipient( Message.RecipientType.TO, new InternetAddress(recipientEmail));
			if (subject!=null) msg.setSubject(subject); // note: setSubject will encode non-ascii characters for us
			if (to!=null) msg.setRecipients(RecipientType.TO, to);
			if (cc!=null) msg.setRecipients(RecipientType.CC, cc);
			if (bcc!=null) msg.setRecipients(RecipientType.BCC, bcc);
			msg.setContent(multipart);
			msg.writeTo(out);
		} catch (AddressException e) {
			throw new RuntimeException(e);
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		RawMessage rawMessage = new RawMessage();
		rawMessage.setData(ByteBuffer.wrap(out.toByteArray()));
		// Assemble the email
		SendRawEmailRequest request = new SendRawEmailRequest()
		.withSource(source)
		.withRawMessage(rawMessage)
		.withDestinations(recipientEmail);

		return request;
	}
	
	static MimeMultipart createEmailBodyFromJSON(String messageBodyString, String unsubscribeLink, String userProfileSettingLink) throws MessagingException {
		MessageBody messageBody = null;
		boolean canDeserializeJSON;
		try {
			messageBody = EntityFactory.createEntityFromJSONString(messageBodyString, MessageBody.class);
			canDeserializeJSON=true;
		} catch (JSONObjectAdapterException e) {
			canDeserializeJSON = false;
			// just send the content as plain text
		}

		MimeMultipart mp = new MimeMultipart("related");

		if (canDeserializeJSON) {
			String plain = messageBody.getPlain();
			String html = messageBody.getHtml();
			List<Attachment> attachments = messageBody.getAttachments();

			if (html!=null || plain!=null) {
				MimeBodyPart alternativeBodyPart = new MimeBodyPart();
				MimeMultipart alternativeMultiPart = new MimeMultipart("alternative");
				alternativeBodyPart.setContent(alternativeMultiPart);
				if (html!=null) {
					BodyPart part = new MimeBodyPart();
					part.setContent(EmailUtils.createEmailBodyFromHtml(html, unsubscribeLink, userProfileSettingLink),
						ContentTypeUtil.TEXT_HTML_UTF8.toString().toString());
					alternativeMultiPart.addBodyPart(part);
				} else if (plain!=null) {
					BodyPart part = new MimeBodyPart();
					part.setContent(EmailUtils.createEmailBodyFromText(plain, unsubscribeLink, userProfileSettingLink),
							ContentTypeUtil.TEXT_PLAIN_UTF8.toString());
					alternativeMultiPart.addBodyPart(part);
				}
				mp.addBodyPart(alternativeBodyPart);
			}

			if (attachments!=null) {
				for (Attachment attachment : attachments) {
					MimeBodyPart part = new MimeBodyPart();
					String content = attachment.getContent();
					ContentType contentType = ContentType.parse(attachment.getContent_type());
					// CloudMailIn doesn't provide the Content-Transfer-Encoding
					// header, so we assume it's base64 encoded, which is the norm
					byte[] contentBytes;
					try {
						contentBytes = Base64.decodeBase64(content);
					} catch (Exception e) {
						contentBytes = content.getBytes();
					}
					if (contentType.getMimeType().toLowerCase().startsWith("text")) {
						// default charset when not specified for "text/" is ISO_8859_1
						Charset contentCharset = (contentCharset = contentType.getCharset()) == null ? StandardCharsets.ISO_8859_1 : contentCharset;
						part.setContent(new String(contentBytes, contentCharset), contentType.toString());
					} else {
						part.setContent(contentBytes, contentType.toString());
					}
					if (attachment.getDisposition()!=null) part.setDisposition(attachment.getDisposition());
					if (attachment.getContent_id()!=null) part.setContentID(attachment.getContent_id());
					if (attachment.getFile_name()!=null) part.setFileName(attachment.getFile_name());
					if (attachment.getSize()!=null) part.setHeader("size", attachment.getSize());
					if (attachment.getUrl()!=null) part.setHeader("url", attachment.getUrl());
					mp.addBodyPart(part);
				}
			}

		} else {
			BodyPart part = new MimeBodyPart();
			part.setContent(EmailUtils.createEmailBodyFromText(messageBodyString, unsubscribeLink, userProfileSettingLink),
					ContentTypeUtil.TEXT_PLAIN_UTF8.toString());
			mp.addBodyPart(part);
		}
		return mp;
	}

	static MimeMultipart createEmailBodyFromHTMLText(String messageBodyString, String unsubscribeLink, String userProfileSettingLink) throws MessagingException {
		MimeMultipart mp = new MimeMultipart("related");
		BodyPart part = new MimeBodyPart();
		part.setContent(EmailUtils.createEmailBodyFromHtml(messageBodyString, unsubscribeLink, userProfileSettingLink),
				ContentTypeUtil.TEXT_HTML_UTF8.toString());
		mp.addBodyPart(part);
		return mp;
	}

	static MimeMultipart createEmailBodyFromPLAINText(String messageBodyString, String unsubscribeLink, String userProfileSettingLink) throws MessagingException {
		MimeMultipart mp = new MimeMultipart("related");
		BodyPart part = new MimeBodyPart();

		//wrap text in html
		String content = "<html>\n<body>\n" + "<div style=\"white-space: pre-wrap;\">\n" +
				EmailUtils.createEmailBodyFromHtml(messageBodyString, unsubscribeLink, userProfileSettingLink) +
				"\n</div>" +
				"\n</body>\n</html>\n";
		part.setContent(content, ContentTypeUtil.TEXT_HTML_UTF8.toString());
		mp.addBodyPart(part);
		return mp;
	}


}
