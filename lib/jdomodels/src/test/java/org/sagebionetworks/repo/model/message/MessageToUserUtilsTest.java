package org.sagebionetworks.repo.model.message;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MessageToUserUtilsTest {

	@Test (expected = IllegalArgumentException.class)
	public void testSetUserGeneratedMessageFooterWithNullObject() {
		MessageToUserUtils.setUserGeneratedMessageFooter(null);
	}

	@Test
	public void testSetUserGeneratedMessageFooter() {
		MessageToUser result = MessageToUserUtils.setUserGeneratedMessageFooter(new MessageToUser());
		assertTrue(result.getWithProfileSettingLink());
		assertFalse(result.getWithUnsubscribeLink());
		assertFalse(result.getIsNotificationMessage());
	}
}
