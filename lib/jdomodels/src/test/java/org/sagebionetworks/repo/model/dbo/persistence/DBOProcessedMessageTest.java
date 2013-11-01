package org.sagebionetworks.repo.model.dbo.persistence;

import org.junit.Test;

public class DBOProcessedMessageTest {
	@Test(expected=NullPointerException.class)
	public void testSetChangeNum() {
		DBOProcessedMessage pm = new DBOProcessedMessage();
		pm.setChangeNumber(null);
	}

	@Test(expected=NullPointerException.class)
	public void testSetProcessedBy() {
		DBOProcessedMessage pm = new DBOProcessedMessage();
		pm.setQueueName(null);
	}

	@Test(expected=NullPointerException.class)
	public void testSetTimeStamp() {
		DBOProcessedMessage pm = new DBOProcessedMessage();
		pm.setTimeStamp(null);
	}
}
