package org.sagebionetworks.authutil;

import org.junit.Assert;
import org.junit.Test;
import org.sagebionetworks.repo.model.OriginatingClient;
import org.springframework.test.util.ReflectionTestUtils;

public class SendMailTest {
	
	@Test
	public void loadsClientProperties() {
		SendMail sendMail = new SendMail(OriginatingClient.BRIDGE);
		String baseURL = (String)ReflectionTestUtils.getField(sendMail, "baseURL");
		Assert.assertEquals("Values reflect Bridge enum", "https://bridge.synapse.org/webapp", baseURL);
		
		sendMail = new SendMail(OriginatingClient.SYNAPSE);
		baseURL = (String)ReflectionTestUtils.getField(sendMail, "baseURL");
		Assert.assertEquals("Values reflect Synapse enum", "https://www.synapse.org", baseURL);
		
		sendMail = new SendMail();
		baseURL = (String)ReflectionTestUtils.getField(sendMail, "baseURL");
		Assert.assertEquals("Values reflect Synapse enum", "https://www.synapse.org", baseURL);
	}
	
	@Test
	public void testSendMailSendsCorrectMail() {
		SendMail sendMail = new SendMail(OriginatingClient.BRIDGE);
		String welcomeFile = (String)ReflectionTestUtils.getField(sendMail, "welcomeEmailFile");
		String templateText = sendMail.readMailTemplate(welcomeFile);
		Assert.assertTrue("Refers to Bridge", templateText.contains("Bridge"));
		Assert.assertFalse("Doesn't refer to Synapse", templateText.contains("Synapse"));
		
		sendMail = new SendMail();
		welcomeFile = (String)ReflectionTestUtils.getField(sendMail, "welcomeEmailFile");
		templateText = sendMail.readMailTemplate(welcomeFile);
		Assert.assertTrue("Refers to Synapse", templateText.contains("Synapse"));
		Assert.assertFalse("Doesn't refer to Bridge", templateText.contains("Bridge"));

	}
}