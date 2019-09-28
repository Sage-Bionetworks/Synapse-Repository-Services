package org.sagebionetworks.repo.manager.ses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;
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
import org.sagebionetworks.repo.model.ses.SESNotificationUtils;

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
	public void testMapJsonNotificationWithNotificationDetails() {

		SESNotificationType notificationType = SESNotificationType.BOUNCE;
		String feedbackId = UUID.randomUUID().toString();
		String subType = "Permanent";
		String reason = "General";

		when(mockBounce.getSubType()).thenReturn(Optional.of(subType));
		when(mockBounce.getReason()).thenReturn(Optional.of(reason));
		when(mockBounce.getFeedbackId()).thenReturn(feedbackId);
		
		when(mockNotification.getNotificationType()).thenReturn(notificationType.toString());
		when(mockNotification.getBounce()).thenReturn(mockBounce);

		SESNotification expected = new SESNotification();

		expected.setNotificationType(notificationType);
		expected.setSesFeedbackId(feedbackId);
		expected.setNotificationSubType(subType);
		expected.setNotificationReason(reason);

		// Call under test
		SESNotification result = manager.map(mockNotification);

		assertEquals(expected, result);

		verify(mockBounce).getFeedbackId();
		verify(mockBounce).getSubType();
		verify(mockBounce).getReason();
		
		verifyZeroInteractions(mockComplaint);
	}
		
	@Test
	public void testProcessNotificationWithInvalidInput() {

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String notificationBody = null;
			// Call under test
			manager.processNotification(notificationBody);
		});
		
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			String notificationBody = "";
			manager.processNotification(notificationBody);
		});
		
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			String notificationBody = "   ";
			manager.processNotification(notificationBody);
		});
		
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			String notificationBody = "Malformed json";
			manager.processNotification(notificationBody);
		});
	}

	@Test
	public void testProcessNotification() throws IOException {
		
		String messageId = "000001378603177f-7a5433e7-8edb-42ae-af10-f0181f34d6ee-000000";
		
		String notificationBody = SESNotificationUtils.loadNotificationFromClasspath(messageId);
		
		SESNotificationType notificationType = SESNotificationType.BOUNCE;

		SESNotification expected = new SESNotification();

		expected.setNotificationType(notificationType);
		expected.setNotificationBody(notificationBody);
		expected.setNotificationSubType("Permanent");
		expected.setNotificationReason("General");

		// Call under test
		manager.processNotification(notificationBody);

		verify(mockNotificationDao).create(expected);

	}

}
