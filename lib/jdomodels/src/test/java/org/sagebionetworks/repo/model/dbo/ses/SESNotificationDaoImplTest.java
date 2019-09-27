package org.sagebionetworks.repo.model.dbo.ses;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.ses.SESNotification;
import org.sagebionetworks.repo.model.ses.SESNotificationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class SESNotificationDaoImplTest {
	
	// @formatter:off

	private static String notificationBody = "{ \r\n" + 
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
	
	// @formatter:on
	
	@Autowired
	private SESNotificationDao dao;

	
	@BeforeEach
	public void before() {
		dao.clearAll();
	}
	
	@AfterEach
	public void after() {
		dao.clearAll();
	}
	
	@Test
	public void testCreateWithInvalidInput() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			SESNotification notification = null;
			// Call under test
			dao.create(notification);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			SESNotification notification = getRandomNotification();
			notification.setNotificationType(null);
			// Call under test
			dao.create(notification);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			SESNotification notification = getRandomNotification();
			notification.setNotificationBody(null);
			// Call under test
			dao.create(notification);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			SESNotification notification = getRandomNotification();
			notification.setNotificationBody("");
			// Call under test
			dao.create(notification);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			SESNotification notification = getRandomNotification();
			notification.setNotificationBody("    ");
			// Call under test
			dao.create(notification);
		});
	}
	
	@Test
	public void testCreateWithNullableFields() {
		SESNotification notification = getRandomNotification();
		notification.setSesFeedbackId(null);
		notification.setNotificationSubType(null);
		notification.setNotificationReason(null);
		notification.setSesMessageId(null);
		
		// Call under test
		SESNotification result = dao.create(notification);
		
		notification.setId(result.getId());
		notification.setCreatedOn(result.getCreatedOn());
		
		assertEquals(notification, result);
	}
	
	@Test
	public void testCreateNotification() {
		SESNotification notification = getRandomNotification();
		
		// Call under test
		SESNotification result = dao.create(notification);
		
		assertNotNull(result.getId());
		assertNotNull(result.getCreatedOn());
		
		notification.setId(result.getId());
		notification.setCreatedOn(result.getCreatedOn());
		
		assertEquals(notification, result);
	}
	
	@Test
	public void testCountBySesMessageId() {
		int notificationsCount = 10;
		List<SESNotification> notifications = getRandomNotifications(notificationsCount);

		notifications.forEach(dao::create);
		
		String sesMessageId = notifications.iterator().next().getSesMessageId();
		
		Long result = dao.countBySesMessageId(sesMessageId);
		
		assertEquals(1L, result);
	}
	
	private List<SESNotification> getRandomNotifications(int count) {
		List<SESNotification> list = new ArrayList<>(count);
		IntStream.range(0, count).forEach( _index -> {
			list.add(getRandomNotification());
		});
		return list;
	}
	
	private SESNotification getRandomNotification() {
		SESNotification notification = new SESNotification();
		notification.setNotificationType(SESNotificationType.BOUNCE);
		notification.setSesFeedbackId(UUID.randomUUID().toString());
		notification.setSesMessageId(UUID.randomUUID().toString());
		notification.setNotificationBody(notificationBody);
		notification.setNotificationSubType("Permanent");
		notification.setNotificationReason("General");
		return notification;
	}
	
}
