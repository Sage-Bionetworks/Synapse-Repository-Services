package org.sagebionetworks.repo.manager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import org.apache.http.entity.ContentType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.cloudmailin.Attachment;
import org.sagebionetworks.repo.model.message.cloudmailin.Message;
import org.sagebionetworks.repo.model.principal.AliasEnum;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class CloudMailInManagerImpl implements CloudMailInManager {
	
	private static final String FROM_HEADER = "From";
	private static final String TO_HEADER = "To";
	private static final String SUBJECT_HEADER = "Subject";
	
	@Autowired
	PrincipalAliasDAO principalAliasDAO;
	
	/**
	 * Craft a message based on content in the CloudMailIn format.
	 * See http://docs.cloudmailin.com/
	 * The returned message must be sent.
	 * @param message
	 * @throws NotFoundException
	 */
	@Override
	public MessageToUserAndBody convertMessage(Message message, String notificationUnsubscribeEndpoint) throws NotFoundException {
		try {
			List<String> to = new ArrayList<String>();
			String from = null;
			String subject = null;
			JSONObject headers = new JSONObject(message.getHeaders());
			Iterator<String> it = headers.keys();
			while (it.hasNext()) {
				String key = it.next();
				if (SUBJECT_HEADER.equals(key)) {
					subject = headers.getString(key);
				} else if (TO_HEADER.equals(key)) {
					try {
						JSONArray array = headers.getJSONArray(key);
						for (int i=0; i<array.length(); i++) {
							to.add(array.getString(i));
						}
					} catch (JSONException e) {
						// it's a singleton, not an array
						to.add(headers.getString(key));
					}
				} else if (FROM_HEADER.equals(key)) {
					from = headers.getString(key);
				}
			}
			if (from==null) throw new IllegalArgumentException("Sender ('From') is required.");
			if (to.isEmpty()) throw new IllegalArgumentException("There must be at least one recipient.");
			MessageToUser mtu = new MessageToUser();
			mtu.setSubject(subject);
			Set<String> recipients = new HashSet<String>();
			for (String email : to) {
				recipients.add(lookupPrincipalIdForSynapseEmailAddress(email).toString());
			}
			mtu.setRecipients(recipients);
			mtu.setCreatedBy(lookupPrincipalIdForRegisteredEmailAddress(from).toString());
			// TODO just serialize the CloudMailIn json and use mime-type application/x-cloudmailin-json
			MessageToUserAndBody mtub = createEmailBody(
					message.getPlain(), message.getHtml(), 
					message.getAttachments());
			mtub.setMetadata(mtu);
	
			return mtub;
		} catch (JSONException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (MessagingException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	/*
	 * Allow user name, principal ID, 'scrubbed' name
	 */
	private Long lookupPrincipalIdForSynapseEmailAddress(String email) {
		// first, make sure it's actually an email address
		AliasEnum.USER_EMAIL.validateAlias(email);
		String emailLowerCase = email.toLowerCase();
		if (!emailLowerCase.endsWith(EMAIL_SUFFIX_LOWER_CASE))
			throw new IllegalArgumentException("Email must end with "+EMAIL_SUFFIX_LOWER_CASE);
		PrincipalAlias alias = principalAliasDAO.findPrincipalWithAlias(emailLowerCase.substring(0,  
				emailLowerCase.length()-EMAIL_SUFFIX_LOWER_CASE.length()));
		return alias.getPrincipalId();
	}
	
	private Long lookupPrincipalIdForRegisteredEmailAddress(String email) {
		// first, make sure it's actually an email address
		AliasEnum.USER_EMAIL.validateAlias(email);
		PrincipalAlias alias = principalAliasDAO.findPrincipalWithAlias(email);
		return alias.getPrincipalId();
	}
	
	public static MessageToUserAndBody createEmailBody(
			String plain, 
			String html, 
			List<Attachment> attachments, 
			String userId,
			String notificationUnsubscribeEndpoint) throws MessagingException, IOException {
		
    	String unsubscribeLink = EmailUtils.
    			createOneClickUnsubscribeLink(notificationUnsubscribeEndpoint, userId);

	    MimeMultipart mp = new MimeMultipart();
	    if (html!=null) {
	    	BodyPart part = new MimeBodyPart();
	    	StringBuilder bodyWithFooter = new StringBuilder(html);
	    	bodyWithFooter.append("<div>This message was forwarded by Synapse.  To unsubscribe, follow <a href=\"");
	    	bodyWithFooter.append(unsubscribeLink);
	    	bodyWithFooter.append("\">this link</a>.</div>");
	    	part.setContent(bodyWithFooter.toString(), ContentType.TEXT_HTML.getMimeType());
	    	mp.addBodyPart(part);
	    }
	 
	    if (plain!=null) {
	    	BodyPart part = new MimeBodyPart();
	    	StringBuilder bodyWithFooter = new StringBuilder(plain);
	    	bodyWithFooter.append("\r\n.  This message was forwarded by Synapse.  To unsubscribe, follow this link:\r\n");
	    	bodyWithFooter.append(unsubscribeLink);
	    	bodyWithFooter.append("\r\n");
	    	part.setContent(bodyWithFooter.toString(), ContentType.TEXT_PLAIN.getMimeType());
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
	    
		MessageToUserAndBody result = new MessageToUserAndBody();
		result.setBody(baos.toString());
		result.setMimeType(mp.getContentType());
	 
		return result;
	}

}
