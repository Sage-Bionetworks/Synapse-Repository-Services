package org.sagebionetworks.repo.manager.principal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class SynapseEmailServiceImplTest {
	
	private File fileToDelete = null;

	@Autowired
	private SynapseEmailService sesClient;
	
	@After
	public void after() throws Exception {
		if (fileToDelete!=null) {
			fileToDelete.delete();
			fileToDelete = null;
		}
	}

	@Test
	public void testWriteToFile() {
		
		String to = "you@foo.bar";
		String tmpDir = "/tmp";
		File file = new File(tmpDir, to+".json");
		fileToDelete = file;
		assertFalse(file.exists());
		SendEmailRequest emailRequest = new SendEmailRequest();
		Destination destination = new Destination();
		destination.setToAddresses(Collections.singletonList(to));
		emailRequest.setDestination(destination);
		Message message = new Message();
		Body body = new Body();
		Content content = new Content();
		content.setData("my dog has fleas");
		body.setText(content);
		message.setBody(body);
		emailRequest.setMessage(message);
		emailRequest.setSource("me@foo.bar");
		sesClient.sendEmail(emailRequest);
		
		assertTrue(file.exists());
	}

}
