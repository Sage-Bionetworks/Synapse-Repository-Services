package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.Submission;

public class DBOSubmissionTest {

	/**
	 * A fully populated submission serialized as it was stored before the JSON document existed (gzip of the XStream
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

	/**
	 * @return The submission stored in the legacy blob fixture, with the columns of {@link #backupWithLegacyBlob()}
	 */
	private static Submission createExpectedDto() {
		ResearchProject snapshot = new ResearchProject().setId("66").setAccessRequirementId("22").setInstitution("inst")
				.setProjectLead("lead").setIntendedDataUseStatement("use \"quoted\" <xml> & more").setCreatedOn(new Date(1700000000000L))
				.setModifiedOn(new Date(1700000001000L)).setCreatedBy("555").setModifiedBy("555").setEtag("etag-rp");
		return new Submission().setId("11").setAccessRequirementId("22").setAccessRequirementVersion(3L).setRequestId("44")
				.setSubmittedBy("555").setSubmittedOn(new Date(1700000002000L)).setEtag("etag-s").setDucFileHandleId("101")
				.setIrbFileHandleId("102").setAttachments(Arrays.asList("103", "104")).setResearchProjectSnapshot(snapshot)
				.setIsRenewalSubmission(true).setPublication("pub").setSummaryOfUse("summary").setSubjectId("syn123")
				.setSubjectType(RestrictableObjectType.ENTITY);
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithLegacyBlob() throws IOException {
		DBOSubmission expected = new DBOSubmission();
		SubmissionUtils.copyDtoToDbo(createExpectedDto(), expected);
		DBOSubmission backup = backupWithLegacyBlob();

		// call under test
		DBOSubmission result = backup.getTranslator().createDatabaseObjectFromBackup(backup);

		assertNull(result.getSubmissionSerialized());
		// The status fields and accessor changes in the blob are stored in their own tables, not in the document
		assertTrue(new JSONObject(expected.getSubmissionJson()).similar(new JSONObject(result.getSubmissionJson())),
				() -> "Unexpected document: " + result.getSubmissionJson());
		// The order of the keys in the document is not relevant, it was verified above
		expected.setSubmissionJson(result.getSubmissionJson());
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
	public void testCreateDatabaseObjectFromBackupWithBlobAndFileHandleColumn() throws IOException {
		DBOSubmission backup = backupWithLegacyBlob();
		backup.setDucFileHandleId(1L);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			backup.getTranslator().createDatabaseObjectFromBackup(backup);
		}).getMessage();

		assertEquals("Both 'submissionSerialized' and the submission document or file handle columns are not null", message);
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithBlobAndDocument() throws IOException {
		DBOSubmission backup = backupWithLegacyBlob();
		backup.setSubmissionJson("{}");

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			backup.getTranslator().createDatabaseObjectFromBackup(backup);
		}).getMessage();

		assertEquals("Both 'submissionSerialized' and the submission document or file handle columns are not null", message);
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithoutBlob() {
		DBOSubmission backup = new DBOSubmission();
		SubmissionUtils.copyDtoToDbo(createExpectedDto(), backup);
		DBOSubmission expected = new DBOSubmission();
		SubmissionUtils.copyDtoToDbo(createExpectedDto(), expected);

		// call under test
		DBOSubmission result = backup.getTranslator().createDatabaseObjectFromBackup(backup);

		assertSame(backup, result);
		assertEquals(expected, result);
	}

	@Test
	public void testReadLegacySerializedFieldWithLegacyBlob() throws IOException {
		// call under test
		Submission submission = SubmissionUtils.readLegacySerializedField(legacyBlob());

		assertEquals("syn123", submission.getSubjectId());
	}
}
