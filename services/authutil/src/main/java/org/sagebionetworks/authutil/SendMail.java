package org.sagebionetworks.authutil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.utils.EmailUtils;

//http://www.mkyong.com/java/javamail-api-sending-email-via-gmail-smtp-example/
public class SendMail {
    private static String synapseURL;
    private static String resetPWURI;
	
	private static Log log = LogFactory.getLog(SendMail.class);

    static {
    	synapseURL = "https://www.synapse.org";
		
		// read values from the properties file
        Properties props = new Properties();
        InputStream is = EmailUtils.class.getClassLoader().getResourceAsStream("authutil.properties");
        try {
        	props.load(is);
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }

        resetPWURI = props.getProperty("org.sagebionetworks.resetPasswordURI");
    }
    

    
    
    public static String readMailTemplate(String fname) {
    	try {
	        InputStream is = SendMail.class.getClassLoader().getResourceAsStream(fname);
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			StringBuilder sb = new StringBuilder();
			String s = br.readLine();
			while (s!=null) {
				sb.append(s+"\r\n");
				s = br.readLine();
			}
			br.close();
			is.close();
			return sb.toString();
    	} catch (IOException e) {
    		throw new RuntimeException(e);
    	}
    }
    
    public void sendSetPasswordMail(NewUser user, String sessionToken) {
    	sendUserMail(user, sessionToken, "setpasswordEmail.txt");
    } 
    
    public void sendResetPasswordMail(NewUser user, String sessionToken) {
    	sendUserMail(user, sessionToken, "resetpasswordEmail.txt");
    } 
    
    public void sendSetAPIPasswordMail(NewUser user, String sessionToken) {
    	sendUserMail(user, sessionToken, "setAPIpasswordEmail.txt");
    } 
    
    public void sendUserMail(NewUser user, String sessionToken, String fname) {
    	// read in email template
    	String msg = readMailTemplate(fname);
    	// fill in display name and user name
    	msg = msg.replaceAll("#displayname#", user.getDisplayName());
    	msg = msg.replaceAll("#username#", user.getEmail());
    	try {
    		msg = msg.replaceAll("#link#", synapseURL+resetPWURI+sessionToken);
    	} catch (IllegalArgumentException e) {
    		throw new IllegalArgumentException("replacement string=<"+synapseURL+resetPWURI+sessionToken+"> "+
    				"org.sagebionetworks.portal.endpoint="+System.getProperty("org.sagebionetworks.portal.endpoint"));
    	}
    	// fill in link, with token
    	sendMail(user.getEmail(), "Set Synapse password", msg);
    }
    
    /**
     * Sends a welcome email to the given user
     * 
     * @param user Requires email and displayName
     */
    public void sendWelcomeMail(NewUser user) {
    	// Read in email template
    	String msg = readMailTemplate("welcomeToSynapseEmail.txt");
    	
    	// fill in display name and user name
    	msg = msg.replaceAll("#displayname#", user.getDisplayName());
    	msg = msg.replaceAll("#username#", user.getEmail());
		
    	
    	// fill in link, with token
    	sendMail(user.getEmail(), "Welcome to Synapse!", msg);
    }
    
    /**
     * Calls the email utility to send a message
     * On non-production stacks, the email is instead logged
     */
    private void sendMail(String to, String subj, String msg) {
		// Don't spam emails for integration tests
		if (!StackConfiguration.isProductionStack()) {
			log.debug("Intercepting email...\nTo: " + to + "\nSubject: " + subj + "\nMessage: " + msg);
			return;
		}

    	EmailUtils.sendMail(to, subj, msg);
    }
}
