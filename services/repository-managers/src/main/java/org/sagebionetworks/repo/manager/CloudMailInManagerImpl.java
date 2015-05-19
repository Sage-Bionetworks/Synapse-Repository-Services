package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
	public MessageToUserAndBody convertMessage(Message message) throws NotFoundException {
		try {
			List<String> to = new ArrayList<String>();
			String from = null;
			String subject = null;
			JSONObject headers = new JSONObject(message.getHeaders());
			Iterator<String> it = headers.keys();
			while (it.hasNext()) {
				String key = it.next();
				// if you know the value's an array you can call headers.getJSONArray(key)
				System.out.println("key: "+key+" value: "+headers.get(key));
				if ("Subject".equals(key)) {
					subject = headers.getString(key);
				} else if ("To".equals(key)) {
					try {
						JSONArray array = headers.getJSONArray(key);
						for (int i=0; i<array.length(); i++) {
							to.add(array.getString(i));
						}
					} catch (JSONException e) {
						// it's a singleton, not an array
						to.add(headers.getString(key));
					}
				} else if ("From".equals(key)) {
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
			MessageToUserAndBody mtub = createEmailBody(message.getPlain(), message.getHtml(), message.getAttachments());
			mtub.setMetadata(mtu);
	
			return mtub;
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		
	}
	
	private static final String EMAIL_SUFFIX_LOWER_CASE = "@synapse.org";
	
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
	
	// TODO add one-click unsubscribe footer
	public static MessageToUserAndBody createEmailBody(String plain, String html, List<Attachment> attachments) {
		MessageToUserAndBody result = new MessageToUserAndBody();
		if (attachments==null || attachments.size()==0) {
			if (html!=null) {
				result.setBody(html);
				result.setMimeType(ContentType.TEXT_HTML.getMimeType());
			} else if (plain!=null) {
				result.setBody(plain);
				result.setMimeType(ContentType.TEXT_PLAIN.getMimeType());
			} else {
				result.setBody("");
				result.setMimeType(ContentType.TEXT_PLAIN.getMimeType());
			}
		} else {
			throw new IllegalArgumentException("TODO");
		}
		return result;
	}

}
