package org.sagebionetworks.repo.model.dbo.persistence.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;

public class DBOTableTransactionTest {
	
	@Test
	public void testAddEtagIfNull() {
		MigratableTableTranslation<DBOTableTransaction, DBOTableTransaction> translator = new DBOTableTransaction().getTranslator();
		DBOTableTransaction dbo = new DBOTableTransaction();
		dbo.setEtag(null);
		// call under test
		DBOTableTransaction translated = translator.createDatabaseObjectFromBackup(dbo);
		assertNotNull(translated);
		// etag should get assigned.
		assertNotNull(translated.getEtag());
	}

	@Test
	public void testAddHasEtag() {
		MigratableTableTranslation<DBOTableTransaction, DBOTableTransaction> translator = new DBOTableTransaction().getTranslator();
		DBOTableTransaction dbo = new DBOTableTransaction();
		String expectedEtag = "should not change";
		dbo.setEtag(expectedEtag);
		// call under test
		DBOTableTransaction translated = translator.createDatabaseObjectFromBackup(dbo);
		assertNotNull(translated);
		// the etag should not change.
		assertEquals(expectedEtag, translated.getEtag());
	}
}
