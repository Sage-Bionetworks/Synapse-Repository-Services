package org.sagebionetworks.repo.manager.ses;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dbo.ses.EmailQuarantineDao;
import org.sagebionetworks.repo.model.dbo.ses.SESNotificationDao;
import org.sagebionetworks.repo.model.ses.QuarantinedEmailBatch;
import org.sagebionetworks.repo.model.ses.SESJsonBounce;
import org.sagebionetworks.repo.model.ses.SESJsonComplaint;
import org.sagebionetworks.repo.model.ses.SESJsonMail;
import org.sagebionetworks.repo.model.ses.SESJsonNotification;
import org.sagebionetworks.repo.model.ses.SESJsonNotificationDetails;
import org.sagebionetworks.repo.model.ses.SESJsonRecipient;
import org.sagebionetworks.repo.model.ses.SESNotificationRecord;
import org.sagebionetworks.repo.model.ses.SESNotificationType;

@ExtendWith(MockitoExtension.class)
public class SESNotificationManagerTest {

	@Mock
	private SESNotificationDao mockNotificationDao;

	@Mock
	private EmailQuarantineDao mockEmailQuaranineDao;

	@Mock
	private SESJsonMail mockMail;

	@Mock
	private SESJsonBounce mockBounce;

	@Mock
	private SESJsonComplaint mockComplaint;
	
	@Mock
	private SESJsonRecipient mockRecipient;
	
	@Mock
	private SESJsonNotification mockNotification;

	@Mock
	private QuarantinedEmailBatch mockBatch;
	
	@Mock
	private EmailQuarantineProvider mockProvider;

	private SESNotificationManagerImpl manager;
	
	private String messageBody;

	@BeforeEach
	public void before() {
		when(mockProvider.getSupportedType()).thenReturn(SESNotificationType.BOUNCE);
		messageBody = "{}";
		manager = new SESNotificationManagerImpl(mockNotificationDao, mockEmailQuaranineDao, Collections.singletonList(mockProvider));
	}

	@Test
	public void testParseNotificationTypeWithNullType() {
		String notificationType = null;
		SESNotificationType expected = SESNotificationType.UNKNOWN;

		// Call under test
		SESNotificationType result = manager.parseNotificationType(notificationType);

		assertEquals(expected, result);
	}

	@Test
	public void testParseNotificationTypeWithEmptyType() {

		String notificationType = "    ";
		SESNotificationType expected = SESNotificationType.UNKNOWN;

		// Call under test
		SESNotificationType result = manager.parseNotificationType(notificationType);

		assertEquals(expected, result);
	}

	@Test
	public void testParseNotificationTypeWithUnknownType() {

		String notificationType = "Unknown Type";
		SESNotificationType expected = SESNotificationType.UNKNOWN;

		// Call under test
		SESNotificationType result = manager.parseNotificationType(notificationType);

		assertEquals(expected, result);
	}

	@Test
	public void testParseNotificationTypeWithCasedType() {

		String notificationType = "BounCe";
		SESNotificationType expected = SESNotificationType.BOUNCE;

		// Call under test
		SESNotificationType result = manager.parseNotificationType(notificationType);

		assertEquals(expected, result);
	}

	@Test
	public void testProcessNotificationWithInvalidNotificationBody() {

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			String notificationBody = null;
			// Call under test
			manager.processMessage(mockNotification, notificationBody);
		});

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			String notificationBody = "";
			manager.processMessage(mockNotification, notificationBody);
		});

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			String notificationBody = "   ";
			manager.processMessage(mockNotification, notificationBody);
		});

	}

	@Test
	public void testGetNotificationDetailsFromBounce() {

		SESNotificationType notificationType = SESNotificationType.BOUNCE;

		when(mockNotification.getBounce()).thenReturn(mockBounce);

		// Call under test
		Optional<SESJsonNotificationDetails> result = manager.getNotificationDetails(notificationType, mockNotification);

		assertEquals(mockBounce, result.get());

		verify(mockNotification).getBounce();
		verifyNoMoreInteractions(mockNotification);

	}

	@Test
	public void testGetNotificationDetailsFromComplaint() {

		SESNotificationType notificationType = SESNotificationType.COMPLAINT;

		when(mockNotification.getComplaint()).thenReturn(mockComplaint);

		// Call under test
		Optional<SESJsonNotificationDetails> result = manager.getNotificationDetails(notificationType, mockNotification);

		assertEquals(mockComplaint, result.get());

		verify(mockNotification).getComplaint();
		verifyNoMoreInteractions(mockNotification);
	}
	
	@Test
	public void testGetNotificationDetailsWithNullContent() {

		SESNotificationType notificationType = SESNotificationType.COMPLAINT;

		// Call under test
		Optional<SESJsonNotificationDetails> result = manager.getNotificationDetails(notificationType, mockNotification);

		assertFalse(result.isPresent());

		verify(mockNotification).getComplaint();
		verifyNoMoreInteractions(mockNotification);
		
		notificationType = SESNotificationType.BOUNCE;

		// Call under test
		result = manager.getNotificationDetails(notificationType, mockNotification);

		assertFalse(result.isPresent());

		verify(mockNotification).getBounce();
		verifyNoMoreInteractions(mockNotification);

	}

	@Test
	public void testGetNotificationDetailsWithOtherTypes() {

		for (SESNotificationType type : SESNotificationType.values()) {

			if (SESNotificationType.BOUNCE.equals(type) || SESNotificationType.COMPLAINT.equals(type)) {
				continue;
			}

			// Call under test
			Optional<SESJsonNotificationDetails> result = manager.getNotificationDetails(type, mockNotification);

			assertFalse(result.isPresent());
		}

		verifyZeroInteractions(mockNotification);

	}

	@Test
	public void testProcessNotification() throws IOException {

		String messageId = "000001378603177f-7a5433e7-8edb-42ae-af10-f0181f34d6ee-000000";
		String feedbackId = "000001378603176d-5a4b5ad9-6f30-4198-a8c3-b1eb0c270a1d-000000";

		when(mockNotification.getMail()).thenReturn(mockMail);
		when(mockNotification.getBounce()).thenReturn(mockBounce);
		when(mockNotification.getNotificationType()).thenReturn("bounce");
		when(mockMail.getMessageId()).thenReturn(messageId);
		when(mockBounce.getRecipients()).thenReturn(Arrays.asList(mockRecipient));
		when(mockBounce.getSubType()).thenReturn(Optional.of("Permanent"));
		when(mockBounce.getReason()).thenReturn(Optional.of("General"));
		when(mockBounce.getFeedbackId()).thenReturn(feedbackId);

		SESNotificationRecord expected = new SESNotificationRecord(SESNotificationType.BOUNCE, messageBody)
				.withNotificationSubType("Permanent")
				.withNotificationReason("General")
				.withSesFeedbackId(feedbackId)
				.withSesMessageId(messageId);
		
		when(mockProvider.getQuarantinedEmails(any(), any())).thenReturn(mockBatch);
		when(mockBatch.isEmpty()).thenReturn(false);

		// Call under test
		manager.processMessage(mockNotification, messageBody);

		verify(mockNotificationDao).saveNotification(expected);
		verify(mockProvider).getQuarantinedEmails(mockBounce, messageId);
		verify(mockEmailQuaranineDao).addToQuarantine(mockBatch);
	}
	
	@Test
	public void testProcessNotificationWithEmtpyBatch() throws IOException {

		String messageId = "000001378603177f-7a5433e7-8edb-42ae-af10-f0181f34d6ee-000000";

		when(mockNotification.getMail()).thenReturn(mockMail);
		when(mockNotification.getBounce()).thenReturn(mockBounce);
		when(mockNotification.getNotificationType()).thenReturn("bounce");
		when(mockMail.getMessageId()).thenReturn(messageId);
		when(mockBounce.getRecipients()).thenReturn(Arrays.asList(mockRecipient));
		when(mockBounce.getSubType()).thenReturn(Optional.of("Permanent"));
		when(mockBounce.getReason()).thenReturn(Optional.of("General"));

		when(mockProvider.getQuarantinedEmails(any(), any())).thenReturn(mockBatch);
		when(mockBatch.isEmpty()).thenReturn(true);
		
		SESNotificationRecord expected = new SESNotificationRecord(SESNotificationType.BOUNCE, messageBody)
			.withNotificationSubType("Permanent")
			.withNotificationReason("General")
			.withSesMessageId(messageId);

		// Call under test
		manager.processMessage(mockNotification, messageBody);

		verify(mockNotificationDao).saveNotification(expected);
		verify(mockProvider).getQuarantinedEmails(mockBounce, messageId);
		verifyZeroInteractions(mockEmailQuaranineDao);
	}
	
	@Test
	public void testProcessNotificationWithNoMail() throws IOException {

		when(mockNotification.getMail()).thenReturn(null);
		when(mockNotification.getBounce()).thenReturn(mockBounce);
		when(mockNotification.getNotificationType()).thenReturn("bounce");
		when(mockBounce.getRecipients()).thenReturn(Arrays.asList(mockRecipient));
		when(mockBounce.getSubType()).thenReturn(Optional.of("Permanent"));
		when(mockBounce.getReason()).thenReturn(Optional.of("General"));

		when(mockProvider.getQuarantinedEmails(any(), any())).thenReturn(mockBatch);
		when(mockBatch.isEmpty()).thenReturn(false);

		// Call under test
		manager.processMessage(mockNotification, messageBody);

		verify(mockNotificationDao).saveNotification(any(SESNotificationRecord.class));
		verify(mockProvider).getQuarantinedEmails(mockBounce, null);
		verify(mockEmailQuaranineDao).addToQuarantine(mockBatch);
	}
	
	@Test
	public void testGetEmailQuarantineBatchWithNoQuarantineSet() {
		SESNotificationType notificationType = SESNotificationType.UNKNOWN;
		String messageId = null;
		
		// Call under test
		QuarantinedEmailBatch result = manager.getEmailQuarantineBatch(notificationType, mockBounce, messageId);
		
		assertEquals(QuarantinedEmailBatch.EMPTY_BATCH, result);
		
		verifyZeroInteractions(mockBounce);
		verifyZeroInteractions(mockProvider);
		
	}
	
	@Test
	public void testGetEmailQuarantineBatchWithNoRecipients() {
		SESNotificationType notificationType = SESNotificationType.BOUNCE;
		String messageId = null;
		
		when(mockBounce.getRecipients()).thenReturn(Collections.emptyList());
		
		// Call under test
		QuarantinedEmailBatch result = manager.getEmailQuarantineBatch(notificationType, mockBounce, messageId);
		
		assertEquals(QuarantinedEmailBatch.EMPTY_BATCH, result);
		
		verify(mockBounce).getRecipients();
		verifyZeroInteractions(mockProvider);	
	}
	
	@Test
	public void testGetEmailQuarantineBatchWithRecipients() {
		SESNotificationType notificationType = SESNotificationType.BOUNCE;
		String messageId = null;
		
		when(mockBounce.getRecipients()).thenReturn(Collections.singletonList(mockRecipient));
		when(mockProvider.getQuarantinedEmails(mockBounce, messageId)).thenReturn(mockBatch);
		
		// Call under test
		QuarantinedEmailBatch result = manager.getEmailQuarantineBatch(notificationType, mockBounce, messageId);
		
		assertEquals(mockBatch, result);
		
		verify(mockBounce).getRecipients();
		verify(mockProvider).getQuarantinedEmails(mockBounce, messageId);
		
	}

}
