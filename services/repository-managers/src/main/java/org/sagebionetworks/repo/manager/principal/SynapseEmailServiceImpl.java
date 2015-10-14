package org.sagebionetworks.repo.manager.principal;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.amazonaws.util.StringInputStream;

/**
 * This wrapper around the Amazon SES client allows us to suppress sending
 * email in dev' instances.  A configuration parameter allows one to override
 * the default for dev instances and send email.  When email is suppressed the
 * message is written to a file.  This supports integration testing.
 * 
 * @author brucehoff
 *
 */
public class SynapseEmailServiceImpl implements SynapseEmailService {
	static private Log log = LogFactory.getLog(SynapseEmailServiceImpl.class);

	@Autowired
	private AmazonSimpleEmailService amazonSESClient;
	
	@Autowired
	private AmazonS3Client s3Client;
	
	@Override
	public void sendEmail(SendEmailRequest emailRequest) {
		if (StackConfiguration.isProductionStack() || StackConfiguration.getDeliverEmail()) {
			amazonSESClient.sendEmail(emailRequest);
		} else {
			writeToFile(emailRequest);
		}
	}
	
	@Override
	public void sendRawEmail(SendRawEmailRequest sendRawEmailRequest) {
		if (StackConfiguration.isProductionStack() || StackConfiguration.getDeliverEmail()) {
			amazonSESClient.sendRawEmail(sendRawEmailRequest);
		} else {
			writeToFile(sendRawEmailRequest);
		}
	}

	public void writeToFile(SendEmailRequest emailRequest) {
		String to = emailRequest.getDestination().getToAddresses().get(0);
		String body = null;
		Content textContent = emailRequest.getMessage().getBody().getText();
		if (textContent!=null && textContent.getData()!=null && textContent.getData().length()>0) {
			body = textContent.getData();
		}
		Content htmlContent = emailRequest.getMessage().getBody().getHtml();
		if (htmlContent!=null && htmlContent.getData()!=null && htmlContent.getData().length()>0) {
			if (body==null) {
				body = htmlContent.getData();
			} else {
				body += htmlContent.getData();
			}
		}
		writeObjectToFile(body, to);
	}
	
	public void writeToFile(SendRawEmailRequest rawEmailRequest) {
		String to = rawEmailRequest.getDestinations().get(0);
		writeObjectToFile(new String(rawEmailRequest.getRawMessage().getData().array()), to);
	}
	
	public void writeObjectToFile(String emailBody, String to) {
		String fileName = to+".json";
		InputStream is;
		try {
			is = new StringInputStream(emailBody);
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(emailBody.length());
			s3Client.putObject(StackConfiguration.getS3Bucket(), fileName, is, metadata);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		log.info("\n\nWrote email to S3 file: "+fileName+"\n\n");
	}
	
	
}
