package org.sagebionetworks.repo.model.dbo.persistence.subscription;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;

public class SubscriptionUtilsTest {
	private long subscriptionId;
	private String subscriberId;
	private String objectId;
	private SubscriptionObjectType objectType;
	private Date createdOn;

	@Before
	public void before() {
		subscriptionId = 1L;
		subscriberId = "2";
		objectId = "3";
		objectType = SubscriptionObjectType.FORUM;
		createdOn = new Date();
	}

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
