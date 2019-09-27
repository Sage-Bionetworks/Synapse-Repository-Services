package org.sagebionetworks.repo.model.ses;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class SESNotificationUtilsTest {

	@Test
	public void testParseBounceNotification() throws IOException {

		String notificationBody = "{ \r\n" + 
				"  \"notificationType\":\"Bounce\",\r\n" + 
				"  \"mail\":{ \r\n" + 
				"    \"timestamp\":\"2018-10-08T14:05:45 +0000\",\r\n" + 
				"    \"messageId\":\"000001378603177f-7a5433e7-8edb-42ae-af10-f0181f34d6ee-000000\",\r\n" + 
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
		
		SESJsonNotification expected = new SESJsonNotification();
		
		expected.setNotificationType("Bounce");
		expected.setMail(new SESJsonMail());
		expected.setBounce(new SESJsonBounce());
		
		expected.getMail().setMessageId("000001378603177f-7a5433e7-8edb-42ae-af10-f0181f34d6ee-000000");
		expected.getMail().setOtherProperties(ImmutableMap.of(
				"timestamp", "2018-10-08T14:05:45 +0000",
				"source", "sender@example.com",
				"destination", ImmutableList.of("recipient@example.com"),
				"headersTruncated", true
		));
		expected.getBounce().setBounceType("Permanent");
		expected.getBounce().setBounceSubType("General");
		expected.getBounce().setFeedbackId("000001378603176d-5a4b5ad9-6f30-4198-a8c3-b1eb0c270a1d-000000");
		expected.getBounce().setOtherProperty("timestamp", "2012-05-25T14:59:38.605Z");
		expected.getBounce().setBouncedRecipients(ImmutableList.of(
				getRecipient("recipient1@example.com", "5.0.0", "failed", "smtp; 550 user unknown"),
				getRecipient("recipient2@example.com", "4.0.0", "delayed", null)
		));
		
		SESJsonNotification result = SESNotificationUtils.parseNotification(notificationBody);

		assertEquals(expected, result);
	}
	
	@Test
	public void testParseComplaintNotification() throws IOException {

		String notificationBody = "{ \r\n" + 
				"  \"notificationType\":\"Complaint\",\r\n" + 
				"  \"mail\":{ \r\n" + 
				"    \"timestamp\":\"2018-10-08T14:05:45 +0000\",\r\n" + 
				"    \"messageId\":\"000001378603177f-7a5433e7-8edb-42ae-af10-f0181f34d6ee-000000\",\r\n" + 
				"    \"source\":\"sender@example.com\",\r\n" + 
				"    \"destination\":[ \r\n" + 
				"      \"recipient@example.com\"\r\n" + 
				"    ],\r\n" + 
				"    \"headersTruncated\":true\r\n" + 
				"  },\r\n" + 
				"  \"complaint\":{ \r\n" + 
				"    \"userAgent\":\"AnyCompany Feedback Loop (V0.01)\",\r\n" + 
				"    \"complainedRecipients\":[ \r\n" + 
				"      { \r\n" + 
				"        \"emailAddress\":\"recipient1@example.com\"\r\n" + 
				"      }\r\n" + 
				"    ],\r\n" + 
				"    \"complaintFeedbackType\":\"abuse\",\r\n" + 
				"    \"arrivalDate\":\"2009-12-03T04:24:21.000-05:00\",\r\n" + 
				"    \"timestamp\":\"2012-05-25T14:59:38.623Z\",\r\n" + 
				"    \"feedbackId\":\"000001378603177f-18c07c78-fa81-4a58-9dd1-fedc3cb8f49a-000000\"\r\n" + 
				"  }\r\n" + 
				"}";
		
		SESJsonNotification expected = new SESJsonNotification();
		
		expected.setNotificationType("Complaint");
		expected.setMail(new SESJsonMail());
		expected.setComplaint(new SESJsonComplaint());
		
		expected.getMail().setMessageId("000001378603177f-7a5433e7-8edb-42ae-af10-f0181f34d6ee-000000");
		expected.getMail().setOtherProperties(ImmutableMap.of(
				"timestamp", "2018-10-08T14:05:45 +0000",
				"source", "sender@example.com",
				"destination", ImmutableList.of("recipient@example.com"),
				"headersTruncated", true
		));
		expected.getComplaint().setUserAgent("AnyCompany Feedback Loop (V0.01)");
		expected.getComplaint().setFeedbackId("000001378603177f-18c07c78-fa81-4a58-9dd1-fedc3cb8f49a-000000");
		expected.getComplaint().setComplaintFeedbackType("abuse");
		expected.getComplaint().setComplainedRecipients(ImmutableList.of(
				getRecipient("recipient1@example.com")
		));
		expected.getComplaint().setOtherProperty("arrivalDate", "2009-12-03T04:24:21.000-05:00");
		expected.getComplaint().setOtherProperty("timestamp", "2012-05-25T14:59:38.623Z");
		
		SESJsonNotification result = SESNotificationUtils.parseNotification(notificationBody);

		assertEquals(expected, result);
	}
	
	@Test
	public void testParseWithMissingMappedProperty() throws IOException {

		String notificationBody = "{ \r\n" + 
				"  \"notificationType\":\"Complaint\",\r\n" + 
				"  \"mail\":{ \r\n" + 
				"    \"timestamp\":\"2018-10-08T14:05:45 +0000\",\r\n" + 
				"    \"messageId\":\"000001378603177f-7a5433e7-8edb-42ae-af10-f0181f34d6ee-000000\",\r\n" + 
				"    \"source\":\"sender@example.com\",\r\n" + 
				"    \"destination\":[ \r\n" + 
				"      \"recipient@example.com\"\r\n" + 
				"    ],\r\n" + 
				"    \"headersTruncated\":true\r\n" + 
				"  },\r\n" + 
				"  \"complaint\":{ \r\n" + 
				"    \"arrivalDate\":\"2009-12-03T04:24:21.000-05:00\",\r\n" + 
				"    \"timestamp\":\"2012-05-25T14:59:38.623Z\",\r\n" + 
				"    \"feedbackId\":\"000001378603177f-18c07c78-fa81-4a58-9dd1-fedc3cb8f49a-000000\"\r\n" + 
				"  }\r\n" + 
				"}";
		
		SESJsonNotification expected = new SESJsonNotification();
		
		expected.setNotificationType("Complaint");
		expected.setMail(new SESJsonMail());
		expected.setComplaint(new SESJsonComplaint());
		
		expected.getMail().setMessageId("000001378603177f-7a5433e7-8edb-42ae-af10-f0181f34d6ee-000000");
		expected.getMail().setOtherProperties(ImmutableMap.of(
				"timestamp", "2018-10-08T14:05:45 +0000",
				"source", "sender@example.com",
				"destination", ImmutableList.of("recipient@example.com"),
				"headersTruncated", true
		));
		expected.getComplaint().setFeedbackId("000001378603177f-18c07c78-fa81-4a58-9dd1-fedc3cb8f49a-000000");
		expected.getComplaint().setOtherProperty("arrivalDate", "2009-12-03T04:24:21.000-05:00");
		expected.getComplaint().setOtherProperty("timestamp", "2012-05-25T14:59:38.623Z");
		
		SESJsonNotification result = SESNotificationUtils.parseNotification(notificationBody);

		assertEquals(expected, result);
	}
	
	@Test
	public void testEncodeDecodeRoundTrip() {
		String notificationBody = "{ \r\n" + 
				"  \"notificationType\":\"Bounce\",\r\n" + 
				"  \"mail\":{ \r\n" + 
				"    \"timestamp\":\"2018-10-08T14:05:45 +0000\",\r\n" + 
				"    \"messageId\":\"000001378603177f-7a5433e7-8edb-42ae-af10-f0181f34d6ee-000000\",\r\n" + 
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
		
		byte[] encoded = SESNotificationUtils.encodeBody(notificationBody);
		String decoded = SESNotificationUtils.decodeBody(encoded);
		
		assertEquals(notificationBody, decoded);
	}
	
	SESJsonRecipient getRecipient(String emailAddress) {
		return getRecipient(emailAddress, null, null, null);
	}
	
	SESJsonRecipient getRecipient(String emailAddress, String status, String action, String diagnosticCode) {
		SESJsonRecipient recipient = new SESJsonRecipient();
		recipient.setEmailAddress(emailAddress);
		recipient.setAction(action);
		recipient.setStatus(status);
		recipient.setDiagnosticCode(diagnosticCode);
		return recipient;
	}

}
