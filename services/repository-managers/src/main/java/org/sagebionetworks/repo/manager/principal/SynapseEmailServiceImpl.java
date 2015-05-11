package org.sagebionetworks.repo.manager.principal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.file.preview.PreviewManagerImpl;
import org.springframework.beans.factory.annotation.Autowired;

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
	
	public void sendEmail(SendEmailRequest emailRequest) {
		if (StackConfiguration.isProductionStack() || StackConfiguration.getDeliverEmail()) {
			amazonSESClient.sendEmail(emailRequest);
		} else {
			writeToFile(emailRequest);
		}
	}
	
	public static void writeToFile(SendEmailRequest emailRequest) {
		String to = emailRequest.getDestination().getToAddresses().get(0);
		// Note: We used to use System.getProperty("java.io.tmpdir")
		// but found that this varies between the tomcat test container and
		// the JVM running the integration test
		String tmpDir = "/tmp";
		File file = new File(tmpDir, to+".json");
		PrintWriter pw;
		try {
			pw = new PrintWriter(file);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		try {
			(new JSONObject(emailRequest)).write(pw);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		} finally {
			pw.close();
		}
		log.info("\n\nWrote email to file: "+file.getAbsolutePath()+"\n\n");
	}
}
