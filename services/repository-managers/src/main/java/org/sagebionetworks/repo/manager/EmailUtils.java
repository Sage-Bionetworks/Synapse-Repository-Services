package org.sagebionetworks.repo.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Map;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.JoinTeamSignedToken;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.message.NotificationSettingsSignedToken;
import org.sagebionetworks.repo.model.message.Settings;
import org.sagebionetworks.repo.util.SignedTokenUtil;
import org.sagebionetworks.util.SerializationUtils;

import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;

public class EmailUtils {
	/**
	 * The specified encoding for the generated email message sent to the end user
	 */
	private static final String EMAIL_CHARSET = "UTF-8";
		
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
	
	public static final String TEMPLATE_KEY_CHALLENGE_NAME = "#challengeName#";
	public static final String TEMPLATE_KEY_CHALLENGE_WEB_LINK = "#challengeWebLink#";

	public static String getDisplayName(UserProfile userProfile) {
		String userName = userProfile.getUserName();
		if (userName==null) throw new IllegalArgumentException("userName is required");
		String inviteeFirstName = userProfile.getFirstName();
		String inviteeLastName = userProfile.getLastName();
		StringBuilder displayName = new StringBuilder();
		if (inviteeFirstName!=null || inviteeLastName!=null) {
			if (inviteeFirstName!=null) displayName.append(inviteeFirstName+" ");
			if (inviteeLastName!=null) displayName.append(inviteeLastName+" ");
			displayName.append("("+userName+")");
		} else {
			displayName.append(userName);
		}
		return displayName.toString();
	}

	public static SendEmailRequest createEmailRequest(String recipientEmail, String subject, String body, boolean isHtml, String sender) {
		// Construct whom the email is from 
		String source = StackConfiguration.getNotificationEmailAddress();
		if (sender != null) {
			source = sender + " <" + source + ">";
		}
		
		// Construct an object to contain the recipient address
        Destination destination = new Destination().withToAddresses(recipientEmail);
        
        // Create the subject and body of the message
        if (subject == null) {
        	subject = "";
        }
        Content textSubject = new Content().withData(subject);
        
        // we specify the text encoding to use when sending the email
        Content bodyContent = new Content().withData(body).withCharset(EMAIL_CHARSET);
        Body messageBody = new Body();
        if (isHtml) {
        	messageBody.setHtml(bodyContent);
        } else {
        	messageBody.setText(bodyContent);
        }
        
        // Create a message with the specified subject and body
        Message message = new Message().withSubject(textSubject).withBody(messageBody);
        
        // Assemble the email
		SendEmailRequest request = new SendEmailRequest()
				.withSource(source)
				.withDestination(destination)
				.withMessage(message);
		return request;
	}
	
	/**
	 * 
	 * Reads a resource into a string
	 */
	public static String readMailTemplate(String filename, Map<String,String> fieldValues) {
		try {
			InputStream is = MessageManagerImpl.class.getClassLoader().getResourceAsStream(filename);
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
	


}
