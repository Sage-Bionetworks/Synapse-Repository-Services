package org.sagebionetworks.repo.model.dbo.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Timestamp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DBOGridReplicaTest {

	private DBOGridReplica backup;

	@BeforeEach
	public void before() {
		backup = new DBOGridReplica();
		backup.setId(123L);
		backup.setReplicaId("66533");
		backup.setCreatedBy(1L);
		backup.setCreatedOn(new Timestamp(1000L));
		backup.setSessionId("MTE5NjY5Ng==");
		backup.setIsAgent(false);
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithNullEtag() {
		// a backup taken before the etag, high-water and source columns existed
		backup.setEtag(null);
		backup.setSequenceHighWater(null);
		backup.setSource(null);

		// call under test
		DBOGridReplica dbo = new DBOGridReplica().getTranslator().createDatabaseObjectFromBackup(backup);

		assertNotNull(dbo.getEtag());
		assertNull(dbo.getSequenceHighWater());
		assertNull(dbo.getSource());
		assertEquals("66533", dbo.getReplicaId());
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithEtag() {
		backup.setEtag("an-etag");
		backup.setSequenceHighWater(60L);
		backup.setSource("VALIDATION");

		// call under test
		DBOGridReplica dbo = new DBOGridReplica().getTranslator().createDatabaseObjectFromBackup(backup);

		assertEquals("an-etag", dbo.getEtag());
		assertEquals(60L, dbo.getSequenceHighWater());
		assertEquals("VALIDATION", dbo.getSource());
	}
}
