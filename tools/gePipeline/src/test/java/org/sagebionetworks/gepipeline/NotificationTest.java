package org.sagebionetworks.gepipeline;

import org.junit.Assert;
import org.junit.Test;


public class NotificationTest {

	@Test
	public void testTruncateMessageToMaxLength() throws Exception {
		String message = "The quick brown fox jumped over the lazy dog.";
		Assert.assertEquals(message, Notification.truncateMessageToMaxLength(message));
		while (message.length()<Notification.MAX_MESSAGE_BYTE_LENGTH) {
			message = message + message;
		}
		Assert.assertEquals(Notification.MAX_MESSAGE_BYTE_LENGTH, Notification.truncateMessageToMaxLength(message).length());
	}
}
