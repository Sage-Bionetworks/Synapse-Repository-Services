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

//http://www.mkyong.com/java/javamail-api-sending-email-via-gmail-smtp-example/
public class SendMail {
    public static void sendGmail(
    		String host, 
    		int port, 
    		final String user, 
    		final String pass, 
    		String from, 
    		String to, 
    		String subj, 
    		String msg) {
        Properties props = System.getProperties();
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.socketFactory.port", "465");
		props.put("mail.smtp.socketFactory.class",
				"javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.port", "465");
		Session session = Session.getDefaultInstance(props,
				new javax.mail.Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(user, pass);
					}
				});
        
        MimeMessage message = new MimeMessage(session);
        try {
	    	message.setFrom(new InternetAddress(from));
	    	message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));        
	    	message.setSubject(subj);
	    	message.setText(msg);
	    	message.saveChanges();
	
			Transport.send(message);
        } catch (Exception e) {
        	throw new RuntimeException(e);
        }

	}
    
    private String mailhost;
    private int mailhostPort = -1;
    private String mailUser;
    private String mailPW;
    private String mailFrom;
    private String synapseURL;
    private String resetPWURI;
    
    public void sendMail(String to, String subj, String msg) {
    	sendGmail(mailhost, mailhostPort, mailUser, mailPW, mailFrom, to, subj, msg);
    }

    public SendMail() {
    	synapseURL = StackConfiguration.getPortalEndpoint();
    	mailPW = StackConfiguration.getMailPassword();
		
		// read values from the properties file
        Properties props = new Properties();
        InputStream is = SendMail.class.getClassLoader().getResourceAsStream("authutil.properties");
        try {
        	props.load(is);
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }

        mailhost = props.getProperty("org.sagebionetworks.mailhost");
        String mailhostPortString = props.getProperty("org.sagebionetworks.mailhostPort");
       	if (mailhostPortString!=null) mailhostPort = Integer.parseInt(mailhostPortString);
       	mailUser = props.getProperty("org.sagebionetworks.mailUser");
        mailFrom = props.getProperty("org.sagebionetworks.mailFrom");
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
    	sendMail(user.getEmail(), "reset Synapse password", msg);
    }
    
}
