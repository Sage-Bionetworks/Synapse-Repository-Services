package org.sagebionetworks.repo.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.JoinTeamSignedToken;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.message.NotificationSettingsSignedToken;
import org.sagebionetworks.repo.model.message.Settings;
import org.sagebionetworks.repo.util.SignedTokenUtil;
import org.sagebionetworks.util.SerializationUtils;

public class EmailUtils {
	//////////////////////////////////////////
	// Email template constants and methods //
	//////////////////////////////////////////
	
	public static final String TEMPLATE_KEY_ORIGIN_CLIENT = "#domain#";
	public static final String TEMPLATE_KEY_DISPLAY_NAME = "#displayName#";
	public static final String TEMPLATE_KEY_USERNAME = "#username#";
	public static final String TEMPLATE_KEY_WEB_LINK = "#link#";
	public static final String TEMPLATE_KEY_HTML_SAFE_WEB_LINK = "#htmlSafelink#";
	public static final String TEMPLATE_KEY_MESSAGE_ID = "#messageid#";
	public static final String TEMPLATE_KEY_DETAILS = "#details#";
	public static final String TEMPLATE_KEY_EMAIL = "#email#";
	public static final String TEMPLATE_KEY_TEAM_NAME = "#teamName#";
	public static final String TEMPLATE_KEY_TEAM_ID = "#teamId#";
	public static final String TEMPLATE_KEY_TEAM_WEB_LINK = "#teamWebLink#";
	public static final String TEMPLATE_KEY_ONE_CLICK_JOIN = "#oneClickJoin#";
	public static final String TEMPLATE_KEY_ONE_CLICK_UNSUBSCRIBE = "#oneClickUnsubscribe#";
	public static final String TEMPLATE_KEY_INVITER_MESSAGE = "#inviterMessage#";
	public static final String TEMPLATE_KEY_REQUESTER_MESSAGE = "#requesterMessage#";
	public static final String TEMPLATE_KEY_ORIGINAL_EMAIL = "#originalEmail#";
	
	public static final String TEMPLATE_KEY_CHALLENGE_NAME = "#challengeName#";
	public static final String TEMPLATE_KEY_CHALLENGE_WEB_LINK = "#challengeWebLink#";
	
	/*
	 * The default local/name part of the email address
	 */
	public static final String DEFAULT_EMAIL_ADDRESS_LOCAL_PART = "noreply";

	public static String getDisplayName(UserProfile userProfile) {
		String firstName = userProfile.getFirstName();
		String lastName = userProfile.getLastName();
		if (firstName==null && lastName==null) return null;
		StringBuilder displayName = new StringBuilder();
		if (firstName!=null) displayName.append(firstName);
		if (lastName!=null) {
			if (firstName!=null) displayName.append(" ");
			displayName.append(lastName);
		}
		return displayName.toString();
	}

	public static String getDisplayNameWithUserName(UserProfile userProfile) {
		String userName = userProfile.getUserName();
		if (userName==null) throw new IllegalArgumentException("userName is required");
		String displayName = getDisplayName(userProfile);
		if (displayName!=null) {
			displayName = displayName+" ("+userName+")";
		} else {
			displayName = userName;
		}
		return displayName;
	}

	/**
	 * 
	 * Reads a resource into a string
	 */
	public static String readMailTemplate(String filename, Map<String,String> fieldValues) {
		try {
			InputStream is = MessageManagerImpl.class.getClassLoader().getResourceAsStream(filename);
			if (is==null) throw new RuntimeException("Could not find file "+filename);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			StringBuilder sb = new StringBuilder();
			try {
				String s = br.readLine();
				while (s != null) {
					sb.append(s + "\r\n");
					s = br.readLine();
				}
				String template = sb.toString();
				for (String fieldMarker : fieldValues.keySet()) {
					template = template.replaceAll(fieldMarker, fieldValues.get(fieldMarker));
				}
				return template;
			} finally {
				br.close();
				is.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	public static void validateSynapsePortalHost(String urlString) {
		URL url = null;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("The provided endpoint creates an invalid URL");
		}
		String portalHost = url.getHost();
		portalHost = portalHost.toLowerCase().trim();
		if (portalHost.endsWith("synapse.org")) return;
		if (portalHost.endsWith("sagebase.org")) return;
		if (portalHost.equals("localhost") || portalHost.equals("127.0.0.1")) return;
		throw new IllegalArgumentException("The provided parameter is not a valid Synapse endpoint.");
	}
	
	public static String createSource(String senderDisplayName, String senderUserName) {
		if (senderUserName==null) senderUserName=DEFAULT_EMAIL_ADDRESS_LOCAL_PART;
		String senderEmailAddress = senderUserName+StackConfiguration.getNotificationEmailSuffix();
		// Construct whom the email is from 
		String source;
		if (senderDisplayName==null) {
			source = senderEmailAddress;
		} else {
			source = senderDisplayName + " <" + senderEmailAddress + ">";
		}
		return source;
	}
	

	
	public static String createOneClickJoinTeamLink(String endpoint, String userId, String memberId, String teamId) {
		JoinTeamSignedToken token = new JoinTeamSignedToken();
		token.setCreatedOn(new Date());
		token.setUserId(userId);
		token.setMemberId(memberId);
		token.setTeamId(teamId);
		SignedTokenUtil.signToken(token);
		String serializedToken = SerializationUtils.serializeAndHexEncode(token);
		String result = endpoint+serializedToken;
		validateSynapsePortalHost(result);
		return result;
	}
	
	public static String createOneClickUnsubscribeLink(String endpoint, String userId) {
		if (endpoint==null || userId==null) throw new IllegalArgumentException("endpoint and userId are required.");
		NotificationSettingsSignedToken token = new NotificationSettingsSignedToken();
		token.setCreatedOn(new Date());
		token.setUserId(userId);
		Settings settings = new Settings();
		settings.setSendEmailNotifications(false);
		token.setSettings(settings);
		SignedTokenUtil.signToken(token);
		String serializedToken = SerializationUtils.serializeAndHexEncode(token);
		String result = endpoint+serializedToken;
		validateSynapsePortalHost(result);
		return result;
	}
	
	public static String createHtmlUnsubscribeFooter(String unsubscribeLink) {
		Map<String,String> fieldValues = new HashMap<String,String>();
		fieldValues.put(TEMPLATE_KEY_ONE_CLICK_UNSUBSCRIBE, unsubscribeLink);
		return readMailTemplate("message/unsubscribeFooter.html",fieldValues);
	}
	
	public static String createTextUnsubscribeFooter(String unsubscribeLink) {
		Map<String,String> fieldValues = new HashMap<String,String>();
		fieldValues.put(TEMPLATE_KEY_ONE_CLICK_UNSUBSCRIBE, unsubscribeLink);
		return readMailTemplate("message/unsubscribeFooter.txt",fieldValues);
	}
	
	public static String createEmailBodyFromHtml(String messageBody,
			String unsubscribeLink) {
		if (unsubscribeLink==null) return messageBody;
	   	StringBuilder bodyWithFooter = new StringBuilder();
	   	bodyWithFooter.append(messageBody);
	    bodyWithFooter.append(createHtmlUnsubscribeFooter(unsubscribeLink));
   	return bodyWithFooter.toString();
	}
	
	public static String createEmailBodyFromText(String messageBody,
			String unsubscribeLink) {
		if (unsubscribeLink==null) return messageBody;
    	StringBuilder bodyWithFooter = new StringBuilder(messageBody);
    	bodyWithFooter.append(createTextUnsubscribeFooter(unsubscribeLink));
    	return bodyWithFooter.toString();
	}
	


}
