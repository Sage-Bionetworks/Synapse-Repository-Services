package org.sagebionetworks.repo.manager.ses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ses.QuarantineReason;
import org.sagebionetworks.repo.model.ses.QuarantinedEmailBatch;
import org.sagebionetworks.repo.model.ses.SESJsonNotificationDetails;
import org.sagebionetworks.repo.model.ses.SESJsonRecipient;
import org.sagebionetworks.repo.model.ses.SESNotificationType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

@ExtendWith(MockitoExtension.class)
public class EmailQuarantineComplaintProviderTest {

	@Mock
	private SESJsonNotificationDetails mockDetails;

	@Mock
	private SESJsonRecipient mockRecipient;

	@InjectMocks
	private EmailQuarantineComplaintProvider provider;

	private String messageId = "000001378603177f-7a5433e7-8edb-42ae-af10-f0181f34d6ee-000000";

	@Test
	public void testGetSupportedType() {
		assertEquals(SESNotificationType.COMPLAINT, provider.getSupportedType());
	}

	@Test
	public void getQuarantinedEmailsWithEmptyReason() {

		QuarantineReason reason = QuarantineReason.COMPLAINT;

		when(mockDetails.getReason()).thenReturn(Optional.empty());

		QuarantinedEmailBatch expected = new QuarantinedEmailBatch()
				.withReason(reason)
				.withSesMessageId(messageId);

		QuarantinedEmailBatch result = provider.getQuarantinedEmails(mockDetails, messageId);

		assertEquals(expected, result);

	}

	@Test
	public void getQuarantinedEmailsWithUnknownReason() {

		QuarantineReason reason = QuarantineReason.COMPLAINT;
		String reasonDetails = "Unknown Reason";

		when(mockDetails.getReason()).thenReturn(Optional.of(reasonDetails));

		QuarantinedEmailBatch expected = new QuarantinedEmailBatch()
				.withReason(reason)
				.withSesMessageId(messageId);

		QuarantinedEmailBatch result = provider.getQuarantinedEmails(mockDetails, messageId);

		assertEquals(expected, result);

	}

	@Test
	public void getQuarantinedEmailsWithComplaintFeedbackType() {

		Set<String> reasons = ImmutableSet.of("abuse", "auth-failure", "fraud", "other", "virus");

		for (String reasonDetail : reasons) {
			String recipientEmail = "recipient1@test.com";
			QuarantineReason reason = QuarantineReason.COMPLAINT;

			when(mockRecipient.getEmailAddress()).thenReturn(recipientEmail);
			when(mockDetails.getRecipients()).thenReturn(ImmutableList.of(mockRecipient));
			when(mockDetails.getReason()).thenReturn(Optional.of(reasonDetail));

			QuarantinedEmailBatch expected = new QuarantinedEmailBatch()
					.withReason(reason)
					.withReasonDetails(reasonDetail.toUpperCase())
					.withSesMessageId(messageId);

			expected.add(recipientEmail);

			QuarantinedEmailBatch result = provider.getQuarantinedEmails(mockDetails, messageId);

			assertEquals(expected, result);
		}

	}

	@Test
	public void getQuarantinedEmailsWithEmptyEmailAddress() {
		
		String recipientEmail = null;
		QuarantineReason reason = QuarantineReason.COMPLAINT;
		String reasonDetails = "abuse";
		
		when(mockRecipient.getEmailAddress()).thenReturn(recipientEmail);
		when(mockDetails.getRecipients()).thenReturn(ImmutableList.of(mockRecipient));
		when(mockDetails.getReason()).thenReturn(Optional.of(reasonDetails));
		
		QuarantinedEmailBatch expected = new QuarantinedEmailBatch()
				.withReason(reason)
				.withReasonDetails(reasonDetails.toUpperCase())
				.withSesMessageId(messageId);
		
		QuarantinedEmailBatch result = provider.getQuarantinedEmails(mockDetails, messageId);
		
		assertEquals(expected, result);
		
	}

}
