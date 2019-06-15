package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.persistence.subscription.DBOSubscription;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;

public class DBOSubscriptionTranslatorTest {

	@Test
	public void testCreateBackupFromDatabaseObject() {
		DBOSubscription dbo = new DBOSubscription();
		dbo.setId(1L);
		dbo.setObjectId(2L);
		dbo.setObjectType(SubscriptionObjectType.THREAD.name());
		dbo.setSubscriberId(3L);
		dbo.setCreatedOn(4L);
		MigratableTableTranslation<DBOSubscription, DBOSubscription> translator = dbo.getTranslator();
		DBOSubscription dbo2 = translator.createBackupFromDatabaseObject(dbo);
		assertTrue(dbo2.getObjectType().equals(SubscriptionObjectType.THREAD.name()));
	}

	@Test
	public void testCreateDatabaseObjectFromBackup() {
		DBOSubscription dbo = new DBOSubscription();
		dbo.setId(1L);
		dbo.setObjectId(2L);
		dbo.setObjectType(SubscriptionObjectType.THREAD.name());
		dbo.setSubscriberId(3L);
		dbo.setCreatedOn(4L);
		MigratableTableTranslation<DBOSubscription, DBOSubscription> translator = dbo.getTranslator();
		DBOSubscription dbo2 = translator.createDatabaseObjectFromBackup(dbo);
		assertTrue(dbo2.getObjectType().equals(SubscriptionObjectType.THREAD.name()));
	}
}
