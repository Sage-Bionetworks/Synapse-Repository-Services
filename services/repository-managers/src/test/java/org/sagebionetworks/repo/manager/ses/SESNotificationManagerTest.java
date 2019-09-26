package org.sagebionetworks.repo.manager.ses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dbo.ses.SESNotificationDao;
import org.sagebionetworks.repo.model.ses.SESJsonBounce;
import org.sagebionetworks.repo.model.ses.SESJsonComplaint;
import org.sagebionetworks.repo.model.ses.SESJsonMail;
import org.sagebionetworks.repo.model.ses.SESJsonNotification;
import org.sagebionetworks.repo.model.ses.SESNotification;
import org.sagebionetworks.repo.model.ses.SESNotificationType;

@ExtendWith(MockitoExtension.class)
public class SESNotificationManagerTest {

	@Mock
	private SESNotificationDao mockNotificationDao;

	@Mock
	private SESJsonMail mockMail;

	@Mock
	private SESJsonBounce mockBounce;

	@Mock
	private SESJsonComplaint mockComplaint;

	@Mock
	private SESJsonNotification mockNotification;

	@InjectMocks
	private SESNotificationManagerImpl manager;

	@Test
	public void testMapJsonNotificationWithNullType() {

		String notificationType = null;

		when(mockNotification.getNotificationType()).thenReturn(notificationType);

		SESNotification expected = new SESNotification();

		expected.setNotificationType(SESNotificationType.UNKNOWN);

		// Call under test
		SESNotification result = manager.map(mockNotification);

		assertEquals(expected, result);

		verify(mockNotification).getNotificationType();
	}

	@Test
	public void testMapJsonNotificationWithEmptyType() {

		String notificationType = " ";

		when(mockNotification.getNotificationType()).thenReturn(notificationType);

		SESNotification expected = new SESNotification();

		expected.setNotificationType(SESNotificationType.UNKNOWN);

		// Call under test
		SESNotification result = manager.map(mockNotification);

		assertEquals(expected, result);

		verify(mockNotification).getNotificationType();
	}

	@Test
	public void testMapJsonNotificationWithUnknownType() {

		String notificationType = "Unknown Type";

		when(mockNotification.getNotificationType()).thenReturn(notificationType);

		SESNotification expected = new SESNotification();

		expected.setNotificationType(SESNotificationType.UNKNOWN);

		// Call under test
		SESNotification result = manager.map(mockNotification);

		assertEquals(expected, result);

		verify(mockNotification).getNotificationType();
	}
	
	@Test
	public void testMapJsonNotificationWithCasedType() {

		String notificationType = "BounCe";

		when(mockNotification.getNotificationType()).thenReturn(notificationType);

		SESNotification expected = new SESNotification();

		expected.setNotificationType(SESNotificationType.BOUNCE);

		// Call under test
		SESNotification result = manager.map(mockNotification);

		assertEquals(expected, result);

		verify(mockNotification).getNotificationType();
	}

	@Test
	public void testMapJsonNotificationWithMessageId() {

		SESNotificationType notificationType = SESNotificationType.BOUNCE;
		String messageId = UUID.randomUUID().toString();

		when(mockMail.getMessageId()).thenReturn(messageId);
		when(mockNotification.getNotificationType()).thenReturn(notificationType.toString());
		when(mockNotification.getMail()).thenReturn(mockMail);

		SESNotification expected = new SESNotification();

		expected.setNotificationType(notificationType);
		expected.setSesMessageId(messageId);

		// Call under test
		SESNotification result = manager.map(mockNotification);

		assertEquals(expected, result);

		verify(mockMail).getMessageId();
	}

	@Test
	public void testMapJsonNotificationWithBounceFeedbackId() {

		SESNotificationType notificationType = SESNotificationType.BOUNCE;
		String feedbackId = UUID.randomUUID().toString();

		when(mockBounce.getFeedbackId()).thenReturn(feedbackId);
		when(mockNotification.getNotificationType()).thenReturn(notificationType.toString());
		when(mockNotification.getBounce()).thenReturn(mockBounce);

		SESNotification expected = new SESNotification();

		expected.setNotificationType(notificationType);
		expected.setSesFeedbackId(feedbackId);

		// Call under test
		SESNotification result = manager.map(mockNotification);

		assertEquals(expected, result);

		verify(mockBounce).getFeedbackId();
		verifyZeroInteractions(mockComplaint);
	}

	@Test
	public void testMapJsonNotificationWithComplaintFeedbackId() {

		SESNotificationType notificationType = SESNotificationType.COMPLAINT;
		String feedbackId = UUID.randomUUID().toString();

		when(mockComplaint.getFeedbackId()).thenReturn(feedbackId);
		when(mockNotification.getNotificationType()).thenReturn(notificationType.toString());
		when(mockNotification.getComplaint()).thenReturn(mockComplaint);

		SESNotification expected = new SESNotification();

		expected.setNotificationType(notificationType);
		expected.setSesFeedbackId(feedbackId);

		// Call under test
		SESNotification result = manager.map(mockNotification);

		assertEquals(expected, result);

		verify(mockComplaint).getFeedbackId();
		verifyZeroInteractions(mockBounce);
	}

	@Test
	public void testProcessNotificationWithInvalidInput() {

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			SESJsonNotification notification = null;
			// Call under test
			manager.processNotification(notification);
		});

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String notificationBody = null;
			when(mockNotification.getNotificationBody()).thenReturn(notificationBody);
			// Call under test
			manager.processNotification(mockNotification);
			verify(mockNotification).getNotificationBody();
		});

	}

	@Test
	public void testProcessNotification() {
		String notificationBody = "Some notification";
		SESNotificationType notificationType = SESNotificationType.BOUNCE;

		when(mockNotification.getNotificationBody()).thenReturn(notificationBody);
		when(mockNotification.getNotificationType()).thenReturn(notificationType.toString());

		SESNotification dto = new SESNotification();

		dto.setNotificationType(notificationType);
		dto.setNotificationBody(notificationBody);

		// Call under test
		manager.processNotification(mockNotification);

		verify(mockNotification).getNotificationType();
		verify(mockNotification, times(2)).getNotificationBody();
		verify(mockNotificationDao).create(dto);

	}

}
