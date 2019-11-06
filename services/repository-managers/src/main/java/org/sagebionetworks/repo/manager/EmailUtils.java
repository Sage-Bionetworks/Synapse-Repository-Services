package org.sagebionetworks.repo.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeUtility;

import com.google.common.net.InternetDomainName;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.manager.token.TokenGenerator;
import org.sagebionetworks.repo.model.JoinTeamSignedToken;
import org.sagebionetworks.repo.model.MembershipInvtnSignedToken;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.dbo.principal.AliasUtils;
import org.sagebionetworks.repo.model.message.NotificationSettingsSignedToken;
import org.sagebionetworks.repo.model.message.Settings;
import org.sagebionetworks.util.SerializationUtils;
import org.sagebionetworks.util.ValidateArgument;

public class EmailUtils {
	//////////////////////////////////////////
	// Email template constants and methods //
	//////////////////////////////////////////
	
	public static final String TEMPLATE_KEY_ORIGIN_CLIENT = "#domain#";
	public static final String TEMPLATE_KEY_DISPLAY_NAME = "#displayName#";
	public static final String TEMPLATE_KEY_USER_ID = "#userid#";
	public static final String TEMPLATE_KEY_USERNAME = "#username#";
	public static final String TEMPLATE_KEY_WEB_LINK = "#link#";
	public static final String TEMPLATE_KEY_HTML_SAFE_WEB_LINK = "#htmlSafelink#";
	public static final String TEMPLATE_KEY_MESSAGE_SUBJECT = "#messageSubject#";
	public static final String TEMPLATE_KEY_DETAILS = "#details#";
	public static final String TEMPLATE_KEY_EMAIL = "#email#";
	public static final String TEMPLATE_KEY_TEAM_NAME = "#teamName#";
	public static final String TEMPLATE_KEY_TEAM_ID = "#teamId#";
	public static final String TEMPLATE_KEY_TEAM_WEB_LINK = "#teamWebLink#";
	public static final String TEMPLATE_KEY_ONE_CLICK_JOIN = "#oneClickJoin#";
	public static final String TEMPLATE_KEY_ONE_CLICK_UNSUBSCRIBE = "#oneClickUnsubscribe#";
	public static final String TEMPLATE_KEY_PROFILE_SETTING_LINK = "#userProfileSettingLink#";
	public static final String TEMPLATE_KEY_LINKS = "#links#";
	public static final String TEMPLATE_KEY_INVITER_MESSAGE = "#inviterMessage#";
	public static final String TEMPLATE_KEY_REQUESTER_MESSAGE = "#requesterMessage#";
	public static final String TEMPLATE_KEY_ORIGINAL_EMAIL = "#originalEmail#";
	
	public static final String TEMPLATE_KEY_CHALLENGE_NAME = "#challengeName#";
	public static final String TEMPLATE_KEY_CHALLENGE_WEB_LINK = "#challengeWebLink#";
	public static final String TEMPLATE_KEY_EVAL_QUEUE_NAME = "#evalQueueName#";
	
	public static final String TEMPLATE_KEY_REASON = "#reason#";
	
	
	/*
	 * The default local/name part of the email address
	 */
	public static final String DEFAULT_EMAIL_ADDRESS_LOCAL_PART = "noreply";

	public static String getDisplayName(UserProfile userProfile) {
		String firstName = userProfile.getFirstName();
		String lastName = userProfile.getLastName();
		return getDisplayName(firstName, lastName);
	}

	public static String getDisplayName(String firstName, String lastName) {
		if (firstName==null && lastName==null) return null;
		StringBuilder displayName = new StringBuilder();
		if (firstName!=null) displayName.append(firstName);
		if (lastName!=null) {
			if (firstName!=null) displayName.append(" ");
			displayName.append(lastName);
		}
		return displayName.toString();
	}

	public static String getDisplayNameWithUsername(String firstName, String lastName, String userName) {
		ValidateArgument.required(userName, "userName");
		String displayName = getDisplayName(firstName, lastName);
		if (displayName!=null) {
			displayName = displayName+" ("+userName+")";
		} else {
			displayName = userName;
		}
		return displayName;
	}

	public static String getDisplayNameWithUsername(UserProfile userProfile) {
		return getDisplayNameWithUsername(userProfile.getFirstName(), userProfile.getLastName(), userProfile.getUserName());
	}
	
	public static String getEmailAddressForPrincipalName(String principalAlias) {
		String actEmailAddress = 
				AliasUtils.getUniqueAliasName(principalAlias)+
				StackConfigurationSingleton.singleton().getNotificationEmailSuffix();
		try {
			return (new InternetAddress(actEmailAddress, principalAlias)).toString();
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * 
	 * Reads a resource into a string
	 */
	public static String readMailTemplate(String filename, Map<String,String> fieldValues) {
		try {
			InputStream is = MessageManagerImpl.class.getClassLoader().getResourceAsStream(filename);
			if (is==null) throw new RuntimeException("Could not find file "+filename);
			try {
				return readMailTemplate(is, fieldValues);
			} finally {
				is.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Read a resource into a string.
	 * @param input
	 * @param fieldValues
	 * @return
	 */
	public static String readMailTemplate(InputStream input, Map<String,String> fieldValues) {
		ValidateArgument.required(input, "input");
		ValidateArgument.required(fieldValues, "fieldValues");
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(input));
			StringBuilder sb = new StringBuilder();
			try {
				String s = br.readLine();
				while (s != null) {
					sb.append(s + "\r\n");
					s = br.readLine();
				}
				String template = sb.toString();
				return buildMailFromTemplate(template, fieldValues);
			} finally {
				br.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Build a message from the given template
	 * @param template
	 * @param fieldValues
	 * @return
	 */
	public static String buildMailFromTemplate(String template, Map<String,String> fieldValues) {
		ValidateArgument.required(template, "input");
		ValidateArgument.required(fieldValues, "fieldValues");
		for (String fieldMarker : fieldValues.keySet()) {
			String replacementValue = fieldValues.get(fieldMarker);
			if (replacementValue==null) replacementValue = "";
			template = template.replaceAll(fieldMarker, replacementValue);
		}
		return template;
	}
	
	public static void validateSynapsePortalHost(String urlString) {
		URI uri = null;
		try {
			uri = new URI(urlString);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("The provided endpoint creates an invalid URL");
		}

		//check for local build endpoints
		final String portalHost = uri.getHost().toLowerCase().trim();
		if (portalHost.equals("localhost") || portalHost.equals("127.0.0.1")) return;

		// If not for local build, find the host's base domain.
		// For example, the base domain for "staging.synapse.org" would be "synapse.org"
		// It is VERY IMPORTANT to use .equals() and NOT .endsWith().
		// Otherwise a domain such as notsynapse.org would pass the validation
		final String baseDomain = InternetDomainName.from(portalHost).topPrivateDomain().toString();
		if (baseDomain.equals("synapse.org") || baseDomain.equals("sagebase.org")) return;

		throw new IllegalArgumentException("The provided parameter is not a valid Synapse endpoint.");
	}
	
	/*
	 * Note:   senderDisplayName is RFC-2047 encoded if not ascii
	 */
	public static String createSource(String senderDisplayName, String senderUserName) {
		if (senderUserName==null) senderUserName=DEFAULT_EMAIL_ADDRESS_LOCAL_PART;
		String senderEmailAddress = senderUserName+StackConfigurationSingleton.singleton().getNotificationEmailSuffix();
		// Construct whom the email is from 
		String source;
		if (senderDisplayName==null) {
			source = senderEmailAddress;
		} else {
			try {
				source = MimeUtility.encodeWord(senderDisplayName, "utf-8", null) + " <" + senderEmailAddress + ">";
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
		return source;
	}
	

	
	public static String createOneClickJoinTeamLink(String endpoint, String userId, String memberId, String teamId, Date createdOn, TokenGenerator tokenGenerator) {
		JoinTeamSignedToken token = new JoinTeamSignedToken();
		token.setCreatedOn(createdOn);
		token.setUserId(userId);
		token.setMemberId(memberId);
		token.setTeamId(teamId);
		tokenGenerator.signToken(token);
		String serializedToken = SerializationUtils.serializeAndHexEncode(token);
		String result = endpoint+serializedToken;
		validateSynapsePortalHost(result);
		return result;
	}

	public static String createMembershipInvtnLink(String endpoint, String membershipInvitationId, TokenGenerator tokenGenerator) {
		MembershipInvtnSignedToken token = new MembershipInvtnSignedToken();
		token.setMembershipInvitationId(membershipInvitationId);
		tokenGenerator.signToken(token);
		String serializedToken = SerializationUtils.serializeAndHexEncode(token);
		String result = endpoint+serializedToken;
		validateSynapsePortalHost(result);
		return result;
	}

	public static String createOneClickUnsubscribeLink(String endpoint, String userId, TokenGenerator tokenGenerator) {
		if (endpoint==null || userId==null) throw new IllegalArgumentException("endpoint and userId are required.");
		NotificationSettingsSignedToken token = new NotificationSettingsSignedToken();
		token.setCreatedOn(new Date());
		token.setUserId(userId);
		Settings settings = new Settings();
		settings.setSendEmailNotifications(false);
		token.setSettings(settings);
		tokenGenerator.signToken(token);
		String serializedToken = SerializationUtils.serializeAndHexEncode(token);
		String result = endpoint+serializedToken;
		validateSynapsePortalHost(result);
		return result;
	}

	public static String createHtmlUnsubscribeLink(String unsubscribeLink) {
		Map<String,String> fieldValues = new HashMap<String,String>();
		fieldValues.put(TEMPLATE_KEY_ONE_CLICK_UNSUBSCRIBE, unsubscribeLink);
		return readMailTemplate("message/unsubscribeLink.html",fieldValues);
	}
	
	public static String createTextUnsubscribeLink(String unsubscribeLink) {
		Map<String,String> fieldValues = new HashMap<String,String>();
		fieldValues.put(TEMPLATE_KEY_ONE_CLICK_UNSUBSCRIBE, unsubscribeLink);
		return readMailTemplate("message/unsubscribeLink.txt",fieldValues);
	}

	public static String createHtmlFooterWithLinks(String links) {
		Map<String,String> fieldValues = new HashMap<String,String>();
		fieldValues.put(TEMPLATE_KEY_LINKS, links);
		return readMailTemplate("message/footerWithLinks.html",fieldValues);
	}
	
	public static String createEmailBodyFromHtml(String messageBody,
			String unsubscribeLink, String userProfileSettingLink) {
		if (unsubscribeLink==null && userProfileSettingLink==null) return messageBody;
		StringBuilder bodyWithFooter = new StringBuilder(messageBody);
		StringBuilder links = new StringBuilder("");
		if (unsubscribeLink!=null) links.append(createHtmlUnsubscribeLink(unsubscribeLink));
		if (userProfileSettingLink!=null) links.append(createHtmlUserProfileSettingLink(userProfileSettingLink));
		bodyWithFooter.append(createHtmlFooterWithLinks(links.toString()));
	return bodyWithFooter.toString();
	}

	public static String createEmailBodyFromText(String messageBody,
			String unsubscribeLink, String userProfileSettingLink) {
		if (unsubscribeLink==null && userProfileSettingLink==null) return messageBody;
		StringBuilder bodyWithFooter = new StringBuilder(messageBody);
		if (unsubscribeLink!=null) bodyWithFooter.append(createTextUnsubscribeLink(unsubscribeLink));
		if (userProfileSettingLink!=null) bodyWithFooter.append(createTextUserProfileSettingLink(userProfileSettingLink));
		return bodyWithFooter.toString();
	}

	public static String createHtmlUserProfileSettingLink(String userProfileSettingLink) {
		Map<String,String> fieldValues = new HashMap<String,String>();
		fieldValues.put(TEMPLATE_KEY_PROFILE_SETTING_LINK, userProfileSettingLink);
		return readMailTemplate("message/userProfileSettingLink.html",fieldValues);
	}

	public static String createTextUserProfileSettingLink(String userProfileSettingLink) {
		Map<String,String> fieldValues = new HashMap<String,String>();
		fieldValues.put(TEMPLATE_KEY_PROFILE_SETTING_LINK, userProfileSettingLink);
		return readMailTemplate("message/userProfileSettingLink.txt",fieldValues);
	}
}
