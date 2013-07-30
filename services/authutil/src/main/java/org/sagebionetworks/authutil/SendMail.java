package org.sagebionetworks.authutil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.auth.User;
import org.sagebionetworks.utils.EmailUtils;

//http://www.mkyong.com/java/javamail-api-sending-email-via-gmail-smtp-example/
public class SendMail {
    private static String synapseURL;
    private static String resetPWURI;

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
    
    public void sendSetPasswordMail(User user, String sessionToken) {
    	sendUserMail(user, sessionToken, "setpasswordEmail.txt");
    } 
    
    public void sendResetPasswordMail(User user, String sessionToken) {
    	sendUserMail(user, sessionToken, "resetpasswordEmail.txt");
    } 
    
    public void sendSetAPIPasswordMail(User user, String sessionToken) {
    	sendUserMail(user, sessionToken, "setAPIpasswordEmail.txt");
    } 
    
    public void sendUserMail(User user, String sessionToken, String fname) {
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
    	EmailUtils.sendMail(user.getEmail(), "Set Synapse password", msg);
    }
    
}
