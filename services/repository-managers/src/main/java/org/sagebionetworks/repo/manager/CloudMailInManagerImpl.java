package org.sagebionetworks.repo.manager;

import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_DISPLAY_NAME;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_EMAIL;
import static org.sagebionetworks.repo.manager.EmailUtils.TEMPLATE_KEY_ORIGINAL_EMAIL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.json.JSONException;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dbo.principal.AliasUtils;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.message.MessageToUserUtils;
import org.sagebionetworks.repo.model.message.cloudmailin.AuthorizationCheckHeader;
import org.sagebionetworks.repo.model.message.cloudmailin.Envelope;
import org.sagebionetworks.repo.model.message.cloudmailin.Headers;
import org.sagebionetworks.repo.model.message.cloudmailin.Message;
import org.sagebionetworks.repo.model.message.multipart.Attachment;
import org.sagebionetworks.repo.model.message.multipart.MessageBody;
import org.sagebionetworks.repo.model.principal.AliasEnum;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

public class CloudMailInManagerImpl implements CloudMailInManager {
	private static final String FROM_HEADER = "From";
	private static final String SUBJECT_HEADER = "Subject";
	private static final String TO_HEADER = "To";
	private static final String CC_HEADER = "Cc";
	private static final String BCC_HEADER = "Bcc";
	
	private static final String EMAIL_SUFFIX_LOWER_CASE = StackConfigurationSingleton.singleton().getNotificationEmailSuffix().toLowerCase();
	
	private static final String INVALID_EMAIL_ADDRESSES_SUBJECT = "Message Failure Notification";
	private static final String INVALID_EMAIL_ADDRESSES_TEMPLATE = "message/InvalidEmailAddressesTemplate.html";
	
	@Autowired
	PrincipalAliasDAO principalAliasDAO;
	
	@Autowired
	private UserProfileManager userProfileManager;
	

	
	public CloudMailInManagerImpl() {}
	
	public CloudMailInManagerImpl(PrincipalAliasDAO principalAliasDAO, UserProfileManager userProfileManager) {
		this.principalAliasDAO=principalAliasDAO;
		this.userProfileManager=userProfileManager;
	}

	@Override
	public List<MessageToUserAndBody> convertMessage(Message message,
			String notificationUnsubscribeEndpoint) throws NotFoundException {

		try {
			Headers headers = message.getHeaders();
			String headerFrom = headers.getFrom();
			String subject = headers.getSubject();
			String to = headers.getTo();
			String cc = headers.getCc();
			String bcc = headers.getBcc();
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
			Long fromPrincipalId = lookupPrincipalIdForRegisteredEmailAddressAndAlternate(envelopeFrom, headerFrom);
			mtu.setCreatedBy(fromPrincipalId.toString());
			mtu.setSubject(subject);
			mtu.setTo(to);
			mtu.setCc(cc);
			mtu.setBcc(bcc);
			Set<String> recipients = new HashSet<String>();
			PrincipalLookupResults principalLookupResults = 
					lookupPrincipalIdsForSynapseEmailAddresses(new HashSet<String>(envelopeRecipients));
			Collection<String> recipientPrincipals = principalLookupResults.getPrincipalIds();
			if (recipientPrincipals.isEmpty()) throw new IllegalArgumentException(
					"Invalid recipient(s): "+envelopeRecipients);
			recipients.addAll(recipientPrincipals);
			mtu.setRecipients(recipients);
			mtu.setNotificationUnsubscribeEndpoint(notificationUnsubscribeEndpoint);
			mtu = MessageToUserUtils.setUserGeneratedMessageFooter(mtu);
			MessageToUserAndBody convertedMessage = new MessageToUserAndBody();
			convertedMessage.setMetadata(mtu);
			convertedMessage.setMimeType(ContentType.APPLICATION_JSON.getMimeType());
			MessageBody messageBody = copyMessageToMessageBody(message);
			convertedMessage.setBody(EntityFactory.createJSONStringForEntity(messageBody));
			List<MessageToUserAndBody> result = new ArrayList<MessageToUserAndBody>();
			result.add(convertedMessage);
			List<String> invalidEmails = principalLookupResults.getInvalidEmails();
			if (!invalidEmails.isEmpty()) {
				// create a notification back to the sender, listing the invalid email addresses
				// and including the original message
				MessageToUser errorMessage = new MessageToUser();
				errorMessage.setCreatedBy(fromPrincipalId.toString());
				errorMessage.setRecipients(Collections.singleton(fromPrincipalId.toString()));
				errorMessage.setSubject(INVALID_EMAIL_ADDRESSES_SUBJECT);
				errorMessage.setNotificationUnsubscribeEndpoint(notificationUnsubscribeEndpoint);
				Map<String,String> fieldValues = new HashMap<String,String>();
				UserProfile fromUserProfile = userProfileManager.getUserProfile(fromPrincipalId.toString());
				fieldValues.put(TEMPLATE_KEY_DISPLAY_NAME, EmailUtils.getDisplayNameWithUsername(fromUserProfile));
				fieldValues.put(TEMPLATE_KEY_EMAIL, invalidEmails.toString());
				String originalMessage = StringUtils.isEmpty(message.getHtml()) ? message.getPlain() : message.getHtml();
				fieldValues.put(TEMPLATE_KEY_ORIGINAL_EMAIL, originalMessage);
				String messageContent = EmailUtils.readMailTemplate(INVALID_EMAIL_ADDRESSES_TEMPLATE, fieldValues);
				result.add(new MessageToUserAndBody(errorMessage, messageContent, ContentType.TEXT_HTML.getMimeType()));
			}
			return result;
		} catch (AddressException e) {
			throw new IllegalArgumentException("Invalid address encountered.", e);
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
	public PrincipalLookupResults lookupPrincipalIdsForSynapseEmailAddresses(Set<String> emails) throws AddressException {
		Set<String> extractedAddresses = new HashSet<String>();
		List<String> invalidEmails = new ArrayList<String>();
		for (String email : emails) {
			InternetAddress[] addresses = null;
			try {
				addresses = InternetAddress.parse(email);
			} catch (AddressException e) {
				invalidEmails.add(email);
				continue;
			}
			for (InternetAddress address : addresses) {
				extractedAddresses.add(address.getAddress());
			}
		}		
		Map<String, String> aliasToEmailMap = new HashMap<String, String>();
		for (String email : extractedAddresses) {
			// first, make sure it's actually an email address
			try {
				AliasEnum.USER_EMAIL.validateAlias(email);
			} catch (IllegalArgumentException e) {
				invalidEmails.add(email);
				continue;
			}
			String emailLowerCase = email.toLowerCase();
			if (!emailLowerCase.endsWith(EMAIL_SUFFIX_LOWER_CASE)) {
				invalidEmails.add(email);
				continue;
			}
			String aliasString = emailLowerCase.substring(0,  
					emailLowerCase.length()-EMAIL_SUFFIX_LOWER_CASE.length());
			String uniqueAliasName = AliasUtils.getUniqueAliasName(aliasString);
			if (uniqueAliasName.equals(EmailUtils.DEFAULT_EMAIL_ADDRESS_LOCAL_PART)) {
				invalidEmails.add(email);
				continue; // someone's trying to email 'noreply@synapse.org'
			}
			aliasToEmailMap.put(uniqueAliasName, email);
		}
		// the aliases to user names and team names.
		List<Long> principalIds = principalAliasDAO.findPrincipalsWithAliases(
				aliasToEmailMap.keySet(),
				Lists.newArrayList(AliasType.USER_NAME, AliasType.TEAM_NAME));
		List<PrincipalAlias> aliases = principalAliasDAO
				.listPrincipalAliases(principalIds);
		Map<String,String> aliasToPrincipalIdMap = new HashMap<String,String>();
		for (PrincipalAlias alias : aliases) {
			aliasToPrincipalIdMap.put(AliasUtils.getUniqueAliasName(alias.getAlias()), alias.getPrincipalId().toString());
		}
		for (String uniqueAlias : aliasToEmailMap.keySet()) {
			if (!aliasToPrincipalIdMap.containsKey(uniqueAlias)) 
				invalidEmails.add(aliasToEmailMap.get(uniqueAlias));
		}
		return new PrincipalLookupResults(aliasToPrincipalIdMap.values(), invalidEmails);
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
			if (lookupPrincipalIdsForSynapseEmailAddresses(Collections.singleton(header.getTo())).getPrincipalIds().isEmpty()) {
				throw new IllegalArgumentException(header.getTo()+" is not a known Synapse email recipient.");
			}
		} catch (AddressException e) {
			throw new IllegalArgumentException("Invalid address encounted.", e);
		}
	}
}
