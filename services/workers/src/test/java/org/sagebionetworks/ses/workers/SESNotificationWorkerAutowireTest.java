package org.sagebionetworks.ses.workers;

import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.dbo.ses.SESNotificationDao;
import org.sagebionetworks.repo.model.ses.SESJsonNotification;
import org.sagebionetworks.repo.model.ses.SESNotificationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.services.sqs.AmazonSQS;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class SESNotificationWorkerAutowireTest {

	private static final String QUEUE_NAME = "SES_NOTIFICATIONS";
	private static final int TIMEOUT = 60 * 1000;
	private static final int WAIT_INTERVAL = 2000;

	@Autowired
	private SESNotificationDao dao;

	@Autowired
	private StackConfiguration stackConfig;

	@Autowired
	private AmazonSQS sqsClient;

	String sesMessageId = "000001378603177f-7a5433e7-8edb-42ae-af10-f0181f34d6ee-000000";

	// @formatter:off

	String notificationBody = "{ \r\n" + 
			"  \"notificationType\":\"Bounce\",\r\n" + 
			"  \"mail\":{ \r\n" + 
			"    \"timestamp\":\"2018-10-08T14:05:45 +0000\",\r\n" + 
			"    \"messageId\":\"" + sesMessageId + "\",\r\n" + 
			"    \"source\":\"sender@example.com\",\r\n" + 
			"    \"destination\":[ \r\n" + 
			"      \"recipient@example.com\"\r\n" + 
			"    ],\r\n" + 
			"    \"headersTruncated\":true\r\n" + 
			"  },\r\n" + 
			"  \"bounce\":{ \r\n" + 
			"    \"bounceType\":\"Permanent\",\r\n" + 
			"    \"bounceSubType\":\"General\",\r\n" + 
			"    \"bouncedRecipients\":[ \r\n" + 
			"      { \r\n" + 
			"        \"status\":\"5.0.0\",\r\n" + 
			"        \"action\":\"failed\",\r\n" + 
			"        \"diagnosticCode\":\"smtp; 550 user unknown\",\r\n" + 
			"        \"emailAddress\":\"recipient1@example.com\"\r\n" + 
			"      },\r\n" + 
			"      { \r\n" + 
			"        \"status\":\"4.0.0\",\r\n" + 
			"        \"action\":\"delayed\",\r\n" + 
			"        \"emailAddress\":\"recipient2@example.com\"\r\n" + 
			"      }\r\n" + 
			"    ],\r\n" + 
			"    \"timestamp\":\"2012-05-25T14:59:38.605Z\",\r\n" + 
			"    \"feedbackId\":\"000001378603176d-5a4b5ad9-6f30-4198-a8c3-b1eb0c270a1d-000000\"\r\n" + 
			"  }\r\n" + 
			"}";
	
	// @formatter:on

	private String queueUrl;
	private SESJsonNotification notification;

	@BeforeEach
	public void before() throws Exception {
		dao.clearAll();
		queueUrl = sqsClient.getQueueUrl(stackConfig.getQueueName(QUEUE_NAME)).getQueueUrl();
		notification = SESNotificationUtils.parseNotification(notificationBody);
		notification.setNotificationBody(notificationBody);
	}

	@AfterEach
	public void after() {
		dao.clearAll();
	}

	@Test
	public void testMessageProcessed() throws Exception {
		// Send the notification to the queue
		sqsClient.sendMessage(queueUrl, notificationBody);

		waitForMessage(sesMessageId);

	}

	void waitForMessage(String messageId) throws Exception {
		long start = System.currentTimeMillis();
		long count = 0L;

		while (count == 0L) {
			assertTrue("Timed out waiting for message processing", (System.currentTimeMillis() - start) < TIMEOUT);

			Thread.sleep(WAIT_INTERVAL);

			count = dao.countBySesMessageId(sesMessageId);
		}
	}

}
