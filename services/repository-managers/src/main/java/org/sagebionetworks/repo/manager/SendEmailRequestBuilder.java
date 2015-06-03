package org.sagebionetworks.repo.manager;

import org.sagebionetworks.StackConfiguration;

import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;

/*
 * If sender is null then the 'notification email address' is used.
 * If unsubscribeLink is null then no such link is added.
 */
public class SendEmailRequestBuilder {
	private String recipientEmail=null;
	private String subject=null;
	private String body=null;
	private boolean isHtml=false;
	private String senderDisplayName=null;
	private String senderUserName=null;
	private String notificationUnsubscribeEndpoint=null;
	private String userId=null;
	
	/*
	 * The specified encoding for the generated email message sent to the end user
	 */
	private static final String EMAIL_CHARSET = "UTF-8";
		
	public SendEmailRequestBuilder withRecipientEmail(String recipientEmail) {
		this.recipientEmail=recipientEmail;
		return this;
	}
	
	public SendEmailRequestBuilder withSubject(String subject) {
		this.subject=subject;
		return this;
	}
	
	public SendEmailRequestBuilder withBody(String body) {
		this.body=body;
		return this;
	}
	
	public SendEmailRequestBuilder withIsHtml(boolean isHtml) {
		this.isHtml=isHtml;
		return this;
	}
	
	public SendEmailRequestBuilder withSenderDisplayName(String senderDisplayName) {
		this.senderDisplayName=senderDisplayName;
		return this;
	}
	
	public SendEmailRequestBuilder withSenderUserName(String senderUserName) {
		this.senderUserName=senderUserName;
		return this;
	}
	
	public SendEmailRequestBuilder withNotificationUnsubscribeEndpoint(String notificationUnsubscribeEndpoint) {
		this.notificationUnsubscribeEndpoint=notificationUnsubscribeEndpoint;
		return this;
	}
	
	public SendEmailRequestBuilder withUserId(String userId) {
		this.userId=userId;
		return this;
	}
	
	public SendEmailRequest build() {
		String source = EmailUtils.createSource(senderDisplayName, senderUserName);
		// Construct an object to contain the recipient address
		if (recipientEmail==null) throw new IllegalStateException("recipient is missing");
        Destination destination = new Destination().withToAddresses(recipientEmail);
        
        // Create the subject and body of the message
        if (subject == null) subject = "";
    
        Content textSubject = new Content().withData(subject);
        
        String unsubscribeLink = null;
        if (notificationUnsubscribeEndpoint==null) {
        	notificationUnsubscribeEndpoint = StackConfiguration.getDefaultPortalNotificationEndpoint();
        }
        if (userId!=null) {
        	unsubscribeLink = EmailUtils.
    			createOneClickUnsubscribeLink(notificationUnsubscribeEndpoint, userId);
        }
       
        // we specify the text encoding to use when sending the email
        Body messageBody = new Body();
        if (isHtml) {
            Content bodyContent = new Content().
            		withData(EmailUtils.createEmailBodyFromHtml(body, unsubscribeLink)).
            		withCharset(EMAIL_CHARSET);
        	messageBody.setHtml(bodyContent);
        } else {
            Content bodyContent = new Content().
            		withData(EmailUtils.createEmailBodyFromText(body, unsubscribeLink)).
            		withCharset(EMAIL_CHARSET);
            messageBody.setText(bodyContent);
        }
        
        // Create a message with the specified subject and body
        com.amazonaws.services.simpleemail.model.Message message = 
        		new com.amazonaws.services.simpleemail.model.Message().
        		withSubject(textSubject).withBody(messageBody);
        
        // Assemble the email
		SendEmailRequest request = new SendEmailRequest()
				.withSource(source)
				.withDestination(destination)
				.withMessage(message);
		return request;
		
	}

}
