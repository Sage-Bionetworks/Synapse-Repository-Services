package org.sagebionetworks.repo.model.dbo.persistence.subscription;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;

public class SubscriptionUtilsTest {
	private final long subscriptionId = 1L;
	private final String subscriberId = "2";
	private final String objectId = "3";
	private final SubscriptionObjectType objectType = SubscriptionObjectType.FORUM;
	private final Date createdOn = new Date();

	@Test (expected=IllegalArgumentException.class)
	public void testcreateDBOWithNullSubscriberId() {
		SubscriptionUtils.createDBO(subscriptionId, null, objectId, objectType, createdOn);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testcreateDBOWithNullObjectId() {
		SubscriptionUtils.createDBO(subscriptionId, subscriberId, null, objectType, createdOn);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testcreateDBOWithNullObjectType() {
		SubscriptionUtils.createDBO(subscriptionId, subscriberId, objectId, null, createdOn);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testcreateDBOWithNullCreatedOn() {
		SubscriptionUtils.createDBO(subscriptionId, subscriberId, objectId, objectType, null);
	}

	@Test
	public void testcreateDBO() {
		DBOSubscription dbo = SubscriptionUtils.createDBO(subscriptionId, subscriberId, objectId, objectType, createdOn);
		assertEquals((Long) subscriptionId, dbo.getId());
		assertEquals(subscriberId, dbo.getSubscriberId().toString());
		assertEquals(objectId, dbo.getObjectId().toString());
		assertEquals(objectType.name(), dbo.getObjectType());
		assertEquals((Long) createdOn.getTime(), dbo.getCreatedOn());
	}
}
