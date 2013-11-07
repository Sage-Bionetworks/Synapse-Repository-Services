package org.sagebionetworks.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.sagebionetworks.StackConfiguration;

public class EmailUtils {
    private static String mailUser;
    private static String mailPW;
    private static String mailFrom;

    static {
    	mailPW = StackConfiguration.getMailPassword();
		
		// read values from the properties file
        Properties props = new Properties();
        InputStream is = EmailUtils.class.getClassLoader().getResourceAsStream("email.properties");
        try {
        	props.load(is);
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }

       	mailUser = props.getProperty("org.sagebionetworks.mailUser");
        mailFrom = props.getProperty("org.sagebionetworks.mailFrom");
    }
    
    private static void sendGmail(
    		final String user, 
    		final String pass, 
    		String from, 
    		String to, 
    		String subj, 
    		String msg) {
        Properties props = new Properties(); //System.getProperties();
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
	    session.setProtocolForAddress("rfc822", "smtp");
        
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

    
    public static void sendMail(String to, String subj, String msg) {
    	sendGmail(mailUser, mailPW, mailFrom, to, subj, msg);
    }

 
    
}
