package org.sagebionetworks.repo.model.dbo.ses;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;

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
			dao.create(notification);
		});
		
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			SESNotification notification = getRandomNotification();
			notification.setIspTimestamp(null);
			dao.create(notification);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			SESNotification notification = getRandomNotification();
			notification.setMessageTimestamp(null);
			dao.create(notification);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			SESNotification notification = getRandomNotification();
			notification.setSesEmailId(null);
			dao.create(notification);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			SESNotification notification = getRandomNotification();
			notification.setSesFeedbackId(null);
			dao.create(notification);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			SESNotification notification = getRandomNotification();
			notification.setNotificationType(null);
			dao.create(notification);
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			SESNotification notification = getRandomNotification();
			notification.setNotificationBody(null);
			dao.create(notification);
		});
	}
	
	@Test
	public void testCreateNotification() {
		SESNotification notification = getRandomNotification();
		
		SESNotification result = dao.create(notification);
		
		assertNotNull(result.getId());
		assertNotNull(result.getCreatedOn());
		
		notification.setId(result.getId());
		notification.setCreatedOn(result.getCreatedOn());
		
		assertEquals(notification, result);
	}
	
	private SESNotification getRandomNotification() {
		SESNotification notification = new SESNotification();
		notification.setIspTimestamp(Instant.now());
		notification.setMessageTimestamp(Instant.now());
		notification.setNotificationType(SESNotificationType.Bounce);
		notification.setSesFeedbackId(UUID.randomUUID().toString());
		notification.setSesEmailId(UUID.randomUUID().toString());
		notification.setNotificationBody("Some notification body in JSON");
		return notification;
	}
	
}
