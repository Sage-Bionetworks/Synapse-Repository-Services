package org.sagebionetworks.repo.model.dbo.ses;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.sagebionetworks.repo.model.ses.SESNotificationUtils.loadMessageFromClasspath;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.ses.SESNotificationRecord;
import org.sagebionetworks.repo.model.ses.SESNotificationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class SESNotificationDaoImplTest {

	@Autowired
	private SESNotificationDao dao;
	
	private String messageBody;

	@BeforeEach
	public void before() throws IOException {
		messageBody = loadMessageFromClasspath("permanent_general");
		dao.clearAll();
	}

	@AfterEach
	public void after() {
		dao.clearAll();
	}

	@Test
	public void testCreateWithInvalidInput() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			SESNotificationRecord notification = null;
			// Call under test
			dao.saveNotification(notification);
		});
	}

	@Test
	public void testCreateWithNullableFields() {
		SESNotificationRecord notification = getRandomNotification()
				.withSesFeedbackId(null)
				.withNotificationSubType(null)
				.withNotificationReason(null)
				.withSesMessageId(null);

		// Call under test
		SESNotificationRecord result = dao.saveNotification(notification);

		notification.withId(result.getId());
		notification.withCreatedOn(result.getCreatedOn());
		notification.withInstanceNumber(result.getInstanceNumber());

		assertEquals(notification, result);
	}

	@Test
	public void testCreateNotification() {
		SESNotificationRecord notification = getRandomNotification();

		// Call under test
		SESNotificationRecord result = dao.saveNotification(notification);

		assertNotNull(result.getId());
		assertNotNull(result.getCreatedOn());

		notification.withId(result.getId());
		notification.withCreatedOn(result.getCreatedOn());
		notification.withInstanceNumber(result.getInstanceNumber());

		assertEquals(notification, result);
	}

	@Test
	public void testCountBySesMessageId() {
		int notificationsCount = 10;
		List<SESNotificationRecord> notifications = getRandomNotifications(notificationsCount);

		notifications.forEach(dao::saveNotification);

		String sesMessageId = notifications.iterator().next().getSesMessageId();

		Long result = dao.countBySesMessageId(sesMessageId);

		assertEquals(1L, result);
	}

	private List<SESNotificationRecord> getRandomNotifications(int count) {
		List<SESNotificationRecord> list = new ArrayList<>(count);
		
		for (int i=0; i< count; i++) {
			list.add(getRandomNotification());
		}
		
		return list;
	}

	private SESNotificationRecord getRandomNotification() {
		return new SESNotificationRecord(SESNotificationType.BOUNCE, messageBody)
				.withSesMessageId(UUID.randomUUID().toString())
				.withSesFeedbackId(UUID.randomUUID().toString())
				.withNotificationSubType("Permanent")
				.withNotificationReason("General");
	}

}
