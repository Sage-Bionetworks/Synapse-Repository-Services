package org.sagebionetworks.repo.manager.ses;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ses.QuarantinedEmailBatch;
import org.sagebionetworks.repo.model.ses.SESJsonNotificationDetails;
import org.sagebionetworks.repo.model.ses.SESJsonRecipient;
import org.sagebionetworks.repo.model.ses.SESNotificationType;

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
	public void getQuarantinedEmails() {

		QuarantinedEmailBatch result = provider.getQuarantinedEmails(mockDetails, messageId);

		assertEquals(QuarantinedEmailBatch.EMPTY_BATCH, result);

	}

}
