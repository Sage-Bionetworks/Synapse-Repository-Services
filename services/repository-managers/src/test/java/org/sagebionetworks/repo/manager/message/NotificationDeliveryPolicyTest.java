package org.sagebionetworks.repo.manager.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.feature.FeatureManager;
import org.sagebionetworks.repo.manager.stack.ProdDetector;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

@ExtendWith(MockitoExtension.class)
public class NotificationDeliveryPolicyTest {

	@Mock
	private FeatureManager mockFeatureManager;
	@Mock
	private ProdDetector mockProdDetector;
	@Mock
	private UserInfo mockRecipient;

	@InjectMocks
	private NotificationDeliveryPolicy policy;

	@Test
	public void testShouldDeliverWithTestingUser() throws RecoverableMessageException {
		when(mockFeatureManager.isUserInTestingGroup(mockRecipient)).thenReturn(true);

		// call under test
		assertTrue(policy.shouldDeliver(mockRecipient));

		// A testing-group member is delivered to on any stack, so the stack is not even checked
		verifyNoInteractions(mockProdDetector);
	}

	@Test
	public void testShouldDeliverWithProd() throws RecoverableMessageException {
		when(mockFeatureManager.isUserInTestingGroup(mockRecipient)).thenReturn(false);
		when(mockProdDetector.isProductionStack()).thenReturn(Optional.of(true));

		// call under test
		assertTrue(policy.shouldDeliver(mockRecipient));
	}

	@Test
	public void testShouldDeliverWithStaging() throws RecoverableMessageException {
		when(mockFeatureManager.isUserInTestingGroup(mockRecipient)).thenReturn(false);
		when(mockProdDetector.isProductionStack()).thenReturn(Optional.of(false));

		// call under test
		assertFalse(policy.shouldDeliver(mockRecipient));
	}

	@Test
	public void testShouldDeliverWithNoDetection() {
		when(mockFeatureManager.isUserInTestingGroup(mockRecipient)).thenReturn(false);
		when(mockProdDetector.isProductionStack()).thenReturn(Optional.empty());

		String message = assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			policy.shouldDeliver(mockRecipient);
		}).getMessage();

		assertEquals("Could not detect current stack version.", message);
	}
}
