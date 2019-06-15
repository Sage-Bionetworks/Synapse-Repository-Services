package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;

public class DBOForumTranslatorTest {

	@Test
	public void testCreateBackupFromDatabaseObject() {
		DBOForum dbo = new DBOForum();
		dbo.setId(1L);
		dbo.setProjectId(2L);
		MigratableTableTranslation<DBOForum, DBOForum> translator = dbo.getTranslator();
		DBOForum dbo2 = translator.createBackupFromDatabaseObject(dbo);
		assertEquals(dbo.getId(), dbo2.getId());
		assertEquals(dbo.getProjectId(), dbo2.getProjectId());
		assertNotNull(dbo2.getEtag());
	}

	@Test
	public void testCreateDatabaseObjectFromBackup() {
		DBOForum dbo = new DBOForum();
		dbo.setId(1L);
		dbo.setProjectId(2L);
		MigratableTableTranslation<DBOForum, DBOForum> translator = dbo.getTranslator();
		DBOForum dbo2 = translator.createDatabaseObjectFromBackup(dbo);
		assertEquals(dbo.getId(), dbo2.getId());
		assertEquals(dbo.getProjectId(), dbo2.getProjectId());
		assertNotNull(dbo2.getEtag());
	}
}
