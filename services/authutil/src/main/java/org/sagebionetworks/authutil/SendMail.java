package org.sagebionetworks.authutil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.OriginatingClient;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.utils.EmailUtils;

//http://www.mkyong.com/java/javamail-api-sending-email-via-gmail-smtp-example/
public class SendMail {
	
	private static Log log = LogFactory.getLog(SendMail.class);
    
    private static Map<OriginatingClient, Properties> propertiesMap = new HashMap<OriginatingClient, Properties>();
	static {
    	propertiesMap.put(OriginatingClient.SYNAPSE, loadProperties(OriginatingClient.SYNAPSE));
    	propertiesMap.put(OriginatingClient.BRIDGE, loadProperties(OriginatingClient.BRIDGE));
	}

	private String baseURL;
	private String resetPasswordURL;
	private String passwordEmailFile;
	private String resetPasswordEmailFile;
	private String apiPasswordResetFile;
	private String welcomeEmailFile;
	private String welcomeEmailSubject;
	private String resetPasswordSubject;
	
	public SendMail() {
		this(OriginatingClient.SYNAPSE);
	}
	
	public SendMail(OriginatingClient originClient) {
		Properties props = propertiesMap.get(originClient);
		baseURL = props.getProperty("org.sagebionetworks.baseURL");
		resetPasswordURL = props.getProperty("org.sagebionetworks.resetPasswordURI");
		passwordEmailFile = props.getProperty("org.sagebionetworks.passwordEmail");
		resetPasswordEmailFile = props.getProperty("org.sagebionetworks.resetPasswordEmail");
		apiPasswordResetFile = props.getProperty("org.sagebionetworks.APIPasswordEmail");
		welcomeEmailFile = props.getProperty("org.sagebionetworks.welcomeEmail");
		welcomeEmailSubject = props.getProperty("org.sagebionetworks.welcomeEmailSubject");
		resetPasswordSubject = props.getProperty("org.sagebionetworks.resetPasswordSubject");
	}
	
    public String readMailTemplate(String fname) {
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
    	sendUserMail(user, sessionToken, passwordEmailFile);
    } 
    
    public void sendResetPasswordMail(NewUser user, String sessionToken) {
    	sendUserMail(user, sessionToken, resetPasswordEmailFile);
    } 
    
    public void sendSetAPIPasswordMail(NewUser user, String sessionToken) {
    	sendUserMail(user, sessionToken, apiPasswordResetFile);
    } 
    
    public void sendUserMail(NewUser user, String sessionToken, String fname) {
    	String templateText = baseURL + resetPasswordURL + sessionToken;
    	// read in email template
    	String msg = readMailTemplate(fname);
    	// fill in display name and user name
    	msg = msg.replaceAll("#displayname#", user.getDisplayName());
    	msg = msg.replaceAll("#username#", user.getEmail());
    	try {
    		msg = msg.replaceAll("#link#", templateText);
    	} catch (IllegalArgumentException e) {
    		throw new IllegalArgumentException("replacement string=<"+templateText+">");
    	}
    	// fill in link, with token
    	sendMail(user.getEmail(), resetPasswordSubject, msg);
    }
    
    /**
     * Sends a welcome email to the given user
     * 
     * @param user Requires email and displayName
     */
    public void sendWelcomeMail(NewUser user) {
    	// Read in email template
    	String msg = readMailTemplate( welcomeEmailFile );
    	
    	// fill in display name and user name
    	msg = msg.replaceAll("#displayname#", user.getDisplayName());
    	msg = msg.replaceAll("#username#", user.getEmail());
		
    	
    	// fill in link, with token
    	sendMail(user.getEmail(), welcomeEmailSubject, msg);
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
    
	private static Properties loadProperties(OriginatingClient client) {
		Properties properties = new Properties();
		// read values from the properties files
        try {
            InputStream is = EmailUtils.class.getClassLoader().getResourceAsStream("authutil-"+client.name().toLowerCase()+".properties");
        	properties.load(is);
        	is.close();
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }
        return properties;
	}
    
}
