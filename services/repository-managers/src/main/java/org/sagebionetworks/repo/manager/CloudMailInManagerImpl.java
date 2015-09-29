package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.http.entity.ContentType;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.cloudmailin.AuthorizationCheckHeader;
import org.sagebionetworks.repo.model.message.cloudmailin.Envelope;
import org.sagebionetworks.repo.model.message.cloudmailin.Message;
import org.sagebionetworks.repo.model.message.multipart.Attachment;
import org.sagebionetworks.repo.model.message.multipart.MessageBody;
import org.sagebionetworks.repo.model.principal.AliasEnum;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class CloudMailInManagerImpl implements CloudMailInManager {
	private static final String FROM_HEADER = "From";
	private static final String SUBJECT_HEADER = "Subject";
	private static final String TO_HEADER = "To";
	private static final String CC_HEADER = "Cc";
	private static final String BCC_HEADER = "Bcc";
	
	private static final String EMAIL_SUFFIX_LOWER_CASE = StackConfiguration.getNotificationEmailSuffix().toLowerCase();
	
	@Autowired
	PrincipalAliasDAO principalAliasDAO;
	
	public CloudMailInManagerImpl() {}
	
	public CloudMailInManagerImpl(PrincipalAliasDAO principalAliasDAO) {
		this.principalAliasDAO=principalAliasDAO;
	}

	@Override
	public MessageToUserAndBody convertMessage(Message message,
			String notificationUnsubscribeEndpoint) throws NotFoundException {

		try {
			String headerFrom = null;
			String subject = null;
			String to = null;
			String cc = null;
			String bcc = null;
			JSONObject headers = new JSONObject(message.getHeaders());
			Iterator<String> it = headers.keys();
			while (it.hasNext()) {
				String key = it.next();
				if (SUBJECT_HEADER.equalsIgnoreCase(key)) {
					subject = headers.getString(key);
				} else if (FROM_HEADER.equalsIgnoreCase(key)) {
					headerFrom = headers.getString(key);
				} else if (TO_HEADER.equalsIgnoreCase(key)) {
					to = headers.getString(key);
				} else if (CC_HEADER.equalsIgnoreCase(key)) {
					cc = headers.getString(key);
				} else if (BCC_HEADER.equalsIgnoreCase(key)) {
					bcc = headers.getString(key);
				}
			}
			// per CloudMailIn support, the way to determine the recipient ('to') is via the Envelope
			// the way to determine the sender ('from') is by checking the Envelope and then (if not valid)
			// checking the header
			Envelope envelope = message.getEnvelope();
			String envelopeFrom = envelope.getFrom();
			List<String> envelopeRecipients = envelope.getRecipients();
			if (envelopeFrom==null && headerFrom==null) throw new IllegalArgumentException("Sender ('From') is required.");
			if (envelopeRecipients==null || envelopeRecipients.isEmpty()) 
				throw new IllegalArgumentException("Recipients list is required.");
			MessageToUser mtu = new MessageToUser();
			mtu.setCreatedBy(lookupPrincipalIdForRegisteredEmailAddressAndAlternate(envelopeFrom, headerFrom).toString());		
			mtu.setSubject(subject);
			mtu.setTo(to);
			mtu.setCc(cc);
			mtu.setBcc(bcc);
			Set<String> recipients = new HashSet<String>();
			// TODO PLFM-3414 will handle the case in which there is a mix of valid and invalid recipients
			Map<String,String> recipientPrincipals = lookupPrincipalIdsForSynapseEmailAddresses(new HashSet<String>(envelopeRecipients));
			if (recipientPrincipals.isEmpty()) throw new IllegalArgumentException("Invalid recipient(s): "+envelopeRecipients);
			recipients.addAll(recipientPrincipals.values());
			mtu.setRecipients(recipients);
			mtu.setNotificationUnsubscribeEndpoint(notificationUnsubscribeEndpoint);
			MessageToUserAndBody result = new MessageToUserAndBody();
			result.setMetadata(mtu);
			result.setMimeType(ContentType.APPLICATION_JSON.getMimeType());
			MessageBody messageBody = copyMessageToMessageBody(message);
			result.setBody(EntityFactory.createJSONStringForEntity(messageBody));
			return result;
		} catch (AddressException e) {
			throw new RuntimeException(e);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static MessageBody copyMessageToMessageBody(Message message) {
		MessageBody result = new MessageBody();
		result.setPlain(message.getPlain());
		result.setHtml(message.getHtml());
		List<Attachment> attachments = new ArrayList<Attachment>();
		if (message.getAttachments()!=null) {
			for (org.sagebionetworks.repo.model.message.cloudmailin.Attachment cloudMailInAttachment : 
					message.getAttachments()) {
				Attachment attachment = new Attachment();
				attachment.setContent(cloudMailInAttachment.getContent());
				attachment.setContent_id(cloudMailInAttachment.getContent_id());
				attachment.setContent_type(cloudMailInAttachment.getContent_type());
				attachment.setDisposition(cloudMailInAttachment.getDisposition());
				attachment.setFile_name(cloudMailInAttachment.getFile_name());
				attachment.setSize(cloudMailInAttachment.getSize());
				attachment.setUrl(cloudMailInAttachment.getUrl());
				attachments.add(attachment);
			}
		}
		result.setAttachments(attachments);
		return result;
	}

	/**
	 * 
	 * @param emails
	 * @return a map whose keys are the given email addresses and whose values are the 
	 * corresponding principal ids.  Any invalid addresses are skipped.
	 * @throws AddressException 
	 */
	public Map<String,String> lookupPrincipalIdsForSynapseEmailAddresses(Set<String> emails) throws AddressException {
		Set<String> extractedAddresses = new HashSet<String>();
		for (String email : emails) {
			for (InternetAddress address : InternetAddress.parse(email)) {
				extractedAddresses.add(address.getAddress());
			}
		}		
		Set<String> aliasStrings = new HashSet<String>();
		for (String email : extractedAddresses) {
			// first, make sure it's actually an email address
			try {
				AliasEnum.USER_EMAIL.validateAlias(email);
			} catch (IllegalArgumentException e) {
				continue;
			}
			String emailLowerCase = email.toLowerCase();
			if (!emailLowerCase.endsWith(EMAIL_SUFFIX_LOWER_CASE)) continue;
			String aliasString = emailLowerCase.substring(0,  
					emailLowerCase.length()-EMAIL_SUFFIX_LOWER_CASE.length());
			if (aliasString.equals(EmailUtils.DEFAULT_EMAIL_ADDRESS_LOCAL_PART)) 
				continue; // someone's trying to email 'noreply@synapse.org'
			aliasStrings.add(aliasString);
		}
		Set<PrincipalAlias> aliases = principalAliasDAO.findPrincipalsWithAliases(aliasStrings);
		Map<String,String> result = new HashMap<String,String>();
		for (PrincipalAlias alias : aliases) result.put(alias.getAlias(), alias.getPrincipalId().toString());
		return result;
	}
	
	private Long lookupAlternateEmail(String primaryEmail, String alternateEmail) throws AddressException {
		try {
			return lookupPrincipalIdForRegisteredEmailAddress(alternateEmail);
		} catch (IllegalArgumentException e) {
			if (primaryEmail==null) throw e;
			throw new IllegalArgumentException("Neither "+primaryEmail+" nor "+alternateEmail+" is a recognized, registered Synapse address.");
		} catch (AddressException e) {
			if (primaryEmail==null) throw e;
			throw new IllegalArgumentException("Neither "+primaryEmail+" nor "+alternateEmail+" is a recognized, registered Synapse address.");
		}
	}
	
	public Long lookupPrincipalIdForRegisteredEmailAddressAndAlternate(String primaryEmail, String alternateEmail) throws AddressException {
		try {
			return lookupPrincipalIdForRegisteredEmailAddress(primaryEmail);
		} catch (IllegalArgumentException e) {
			if (alternateEmail==null) throw e;
			return lookupAlternateEmail(primaryEmail, alternateEmail);
		} catch (AddressException e) {
			if (alternateEmail==null) throw e;
			return lookupAlternateEmail(primaryEmail, alternateEmail);
		}
	}
	
	public Long lookupPrincipalIdForRegisteredEmailAddress(String email) throws AddressException {
		// first, make sure it's actually an email address
		if (email==null) throw new IllegalArgumentException("email address is missing");
		InternetAddress[] address = InternetAddress.parse(email);
		if (address.length!=1) throw new IllegalArgumentException(
				"Expected one address but found "+address.length+" in "+email);
		String extractedAddress = address[0].getAddress();
		AliasEnum.USER_EMAIL.validateAlias(extractedAddress);
		PrincipalAlias alias = principalAliasDAO.findPrincipalWithAlias(extractedAddress);
		if (alias==null) throw new IllegalArgumentException("Specified address "+extractedAddress+" is not registered with Synapse.");
		return alias.getPrincipalId();
	}
	

	@Override
	public void authorizeMessage(AuthorizationCheckHeader header) {
		try {
			// this will throw an exception if 'from' is invalid
			lookupPrincipalIdForRegisteredEmailAddress(header.getFrom());
			if (lookupPrincipalIdsForSynapseEmailAddresses(Collections.singleton(header.getTo())).isEmpty()) {
				throw new IllegalArgumentException(header.getTo()+" is not a known Synapse email recipient.");
			}
		} catch (AddressException e) {
			throw new IllegalArgumentException("Invalid address encounted.", e);
		}
	}
}
