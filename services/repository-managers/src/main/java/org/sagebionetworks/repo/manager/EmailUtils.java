package org.sagebionetworks.repo.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.UserProfile;

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
	public static final String TEMPLATE_KEY_DISPLAY_NAME = "#displayname#";
	public static final String TEMPLATE_KEY_USERNAME = "#username#";
	public static final String TEMPLATE_KEY_WEB_LINK = "#link#";
	public static final String TEMPLATE_KEY_HTML_SAFE_WEB_LINK = "#htmlSafelink#";
	public static final String TEMPLATE_KEY_MESSAGE_ID = "#messageid#";
	public static final String TEMPLATE_KEY_DETAILS = "#details#";
	public static final String TEMPLATE_KEY_EMAIL = "#email#";
	
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

}
