package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.dataaccess.Submission;

public class DBOSubmissionTest {

	/**
	 * A fully populated submission serialized as it was stored before the columns existed (gzip of the XStream
	 * xml): see the legacy bridge in {@link DBOSubmission#getTranslator()}
	 */
	private static final String LEGACY_BLOB_FIXTURE = "dataaccess/submission-blob-v1.b64";

	private static byte[] legacyBlob() throws IOException {
		try (InputStream in = DBOSubmissionTest.class.getClassLoader().getResourceAsStream(LEGACY_BLOB_FIXTURE)) {
			return Base64.getDecoder().decode(new String(in.readAllBytes(), StandardCharsets.US_ASCII).trim());
		}
	}

	private static DBOSubmission backupWithLegacyBlob() throws IOException {
		DBOSubmission backup = new DBOSubmission();
		backup.setId(11L);
		backup.setAccessRequirementId(22L);
		backup.setAccessRequirementVersion(3L);
		backup.setDataAccessRequestId(44L);
		backup.setCreatedBy(555L);
		backup.setCreatedOn(1700000002000L);
		backup.setEtag("etag-s");
		backup.setResearchProjectId(66L);
		backup.setSubmissionSerialized(legacyBlob());
		return backup;
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithLegacyBlob() throws IOException {
		DBOSubmission expected = new DBOSubmission();
		expected.setId(11L);
		expected.setAccessRequirementId(22L);
		expected.setAccessRequirementVersion(3L);
		expected.setDataAccessRequestId(44L);
		expected.setCreatedBy(555L);
		expected.setCreatedOn(1700000002000L);
		expected.setEtag("etag-s");
		expected.setResearchProjectId(66L);
		expected.setDucFileHandleId(101L);
		expected.setIrbFileHandleId(102L);
		expected.setIsRenewalSubmission(true);
		expected.setPublication("pub");
		expected.setSummaryOfUse("summary");
		expected.setSubjectId("syn123");
		expected.setSubjectType(RestrictableObjectType.ENTITY.name());
		expected.setAttachmentsJson("[\"103\",\"104\"]");

		// call under test
		DBOSubmission result = backupWithLegacyBlob().getTranslator().createDatabaseObjectFromBackup(backupWithLegacyBlob());

		assertNull(result.getSubmissionSerialized());
		assertEquals("use \"quoted\" <xml> & more",
				new JSONObject(result.getResearchProjectSnapshotJson()).getString("intendedDataUseStatement"));
		assertEquals("66", new JSONObject(result.getResearchProjectSnapshotJson()).getString("id"));
		expected.setResearchProjectSnapshotJson(result.getResearchProjectSnapshotJson());
		assertEquals(expected, result);
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithLegacyBlobAndMissingVersion() throws IOException {
		DBOSubmission backup = backupWithLegacyBlob();
		backup.setAccessRequirementVersion(null);

		// call under test
		DBOSubmission result = backup.getTranslator().createDatabaseObjectFromBackup(backup);

		assertEquals(3L, result.getAccessRequirementVersion());
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithBlobAndColumns() throws IOException {
		DBOSubmission backup = backupWithLegacyBlob();
		backup.setPublication("already set");

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			backup.getTranslator().createDatabaseObjectFromBackup(backup);
		}).getMessage();

		assertEquals("Both 'submissionSerialized' and the submission columns are not null", message);
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithoutBlob() {
		DBOSubmission backup = new DBOSubmission();
		backup.setId(1L);
		backup.setAccessRequirementVersion(1L);
		backup.setPublication("pub");
		DBOSubmission expected = new DBOSubmission();
		expected.setId(1L);
		expected.setAccessRequirementVersion(1L);
		expected.setPublication("pub");

		// call under test
		DBOSubmission result = backup.getTranslator().createDatabaseObjectFromBackup(backup);

		assertEquals(expected, result);
	}

	@Test
	public void testReadLegacySerializedFieldWithLegacyBlob() throws IOException {
		// call under test
		Submission submission = SubmissionUtils.readLegacySerializedField(legacyBlob());

		assertEquals("syn123", submission.getSubjectId());
	}
}
