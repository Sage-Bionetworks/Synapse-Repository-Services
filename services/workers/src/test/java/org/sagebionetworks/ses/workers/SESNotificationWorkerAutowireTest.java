package org.sagebionetworks.ses.workers;

import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.dbo.ses.EmailQuarantineDao;
import org.sagebionetworks.repo.model.dbo.ses.SESNotificationDao;
import org.sagebionetworks.repo.model.ses.QuarantinedEmail;
import org.sagebionetworks.repo.model.ses.SESNotificationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.services.sqs.AmazonSQS;
import com.google.common.collect.ImmutableSet;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class SESNotificationWorkerAutowireTest {

	private static final String QUEUE_NAME = "SES_NOTIFICATIONS";
	private static final int TIMEOUT = 60 * 1000;
	private static final int WAIT_INTERVAL = 2000;

	@Autowired
	private SESNotificationDao notificationDao;
	
	@Autowired
	private EmailQuarantineDao emailQuarantineDao;

	@Autowired
	private StackConfiguration stackConfig;

	@Autowired
	private AmazonSQS sqsClient;

	private String sesMessageId = "000001378603177f-7a5433e7-8edb-42ae-af10-f0181f34d6ee-000000";
	private String queueUrl;
	private String messageBody;

	@BeforeEach
	public void before() throws Exception {
		emailQuarantineDao.clearAll();
		notificationDao.clearAll();
		queueUrl = sqsClient.getQueueUrl(stackConfig.getQueueName(QUEUE_NAME)).getQueueUrl();
		messageBody = SESNotificationUtils.loadMessageFromClasspath("permanent_general");
	}

	@AfterEach
	public void after() {
		notificationDao.clearAll();
		emailQuarantineDao.clearAll(); 
	}

	@Test
	public void testMessageProcessed() throws Exception {
		// Send the notification to the queue
		sqsClient.sendMessage(queueUrl, messageBody);

		waitForMessage(sesMessageId);
		
		Set<String> expedctedEmails = ImmutableSet.of("recipient1@example.com", "recipient2@example.com");
		
		for (String expected : expedctedEmails) {
			Optional<QuarantinedEmail> quarantinedEmail = emailQuarantineDao.getQuarantinedEmail(expected);
			assertTrue(quarantinedEmail.isPresent());
		}

	}

	void waitForMessage(String messageId) throws Exception {
		long start = System.currentTimeMillis();
		long count = 0L;

		while (count == 0L) {
			assertTrue("Timed out waiting for message processing", (System.currentTimeMillis() - start) < TIMEOUT);

			Thread.sleep(WAIT_INTERVAL);

			count = notificationDao.countBySesMessageId(sesMessageId);
		}
	}

}
