package org.sagebionetworks.repo.manager.principal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.entity.ContentType;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;

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
	
	public void sendEmail(SendEmailRequest emailRequest) {
		if (StackConfiguration.isProductionStack() || StackConfiguration.getDeliverEmail()) {
			amazonSESClient.sendEmail(emailRequest);
		} else {
			writeToFile(emailRequest);
		}
	}
	
	public void writeToFile(SendEmailRequest emailRequest) {
		String to = emailRequest.getDestination().getToAddresses().get(0);
		String fileName = to+".json";
		StringWriter writer=null;
		InputStream is;
		try {
			writer = new StringWriter();
			(new JSONObject(emailRequest)).write(writer);
			is = new ByteArrayInputStream(writer.toString().getBytes());
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(writer.toString().length());
			s3Client.putObject(StackConfiguration.getS3Bucket(), fileName, is, metadata);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (writer!=null) writer.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		log.info("\n\nWrote email to S3 file: "+fileName+"\n\n");
	}
}
