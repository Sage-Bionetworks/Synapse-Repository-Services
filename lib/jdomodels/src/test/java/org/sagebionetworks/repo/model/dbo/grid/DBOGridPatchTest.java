package org.sagebionetworks.repo.model.dbo.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.Test;

public class DBOGridPatchTest {

	private static final Timestamp EXPIRES_ON = Timestamp.from(Instant.parse("2026-07-10T00:00:00Z"));

	@Test
	public void testCreateDatabaseObjectFromBackupWithCreatedOnBeforeCutoff() {
		// the newest patch whose S3 file was deleted by the retention rule
		Instant createdOn = Instant.parse("2026-03-12T18:17:08Z");

		// call under test
		DBOGridPatch dbo = new DBOGridPatch().getTranslator().createDatabaseObjectFromBackup(patch(createdOn, EXPIRES_ON));

		assertEquals(patch(createdOn, EXPIRES_ON), dbo);
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithCreatedOnJustBeforeCutoff() {
		Instant createdOn = DBOGridPatch.PATCH_FILES_RETAINED_SINCE.minusMillis(1);

		// call under test
		DBOGridPatch dbo = new DBOGridPatch().getTranslator().createDatabaseObjectFromBackup(patch(createdOn, EXPIRES_ON));

		assertEquals(patch(createdOn, EXPIRES_ON), dbo);
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithCreatedOnAtCutoff() {
		Instant createdOn = DBOGridPatch.PATCH_FILES_RETAINED_SINCE;

		// call under test
		DBOGridPatch dbo = new DBOGridPatch().getTranslator().createDatabaseObjectFromBackup(patch(createdOn, EXPIRES_ON));

		assertEquals(patch(createdOn, null), dbo);
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithCreatedOnAfterCutoff() {
		// the oldest patch whose S3 file was retained
		Instant createdOn = Instant.parse("2026-03-13T11:49:26Z");

		// call under test
		DBOGridPatch dbo = new DBOGridPatch().getTranslator().createDatabaseObjectFromBackup(patch(createdOn, EXPIRES_ON));

		assertEquals(patch(createdOn, null), dbo);
	}

	private static DBOGridPatch patch(Instant createdOn, Timestamp expiresOn) {
		DBOGridPatch patch = new DBOGridPatch();
		patch.setId(123L);
		patch.setSessionId("MTE5NjY5Ng==");
		patch.setPatchIdRep(66537L);
		patch.setPatchIdSeq(303L);
		patch.setCreatedOn(Timestamp.from(createdOn));
		patch.setExpiresOn(expiresOn);
		patch.setS3Key("27a22f2c-cfdd-4501-9d4f-5bf9e62bd7a0.json");
		patch.setSizeBytes(456L);
		return patch;
	}
}
