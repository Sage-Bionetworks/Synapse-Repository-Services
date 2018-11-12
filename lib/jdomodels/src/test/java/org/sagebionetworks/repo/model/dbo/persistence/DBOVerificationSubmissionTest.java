package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.doi.DoiStatus;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;

public class DBOVerificationSubmissionTest {

	private static long dboId = 1234L;
	private static String dtoId = "4321";
	private static long createdOn = 1000000L;
	private static long createdBy = 43L;

	private static DBOVerificationSubmission newDBO(String orcidUrl) {
		DBOVerificationSubmission dbo = new DBOVerificationSubmission();
		dbo.setId(dboId);
		dbo.setCreatedOn(createdOn);
		dbo.setCreatedBy(createdBy);

		VerificationSubmission dto = new VerificationSubmission();
		dto.setOrcid(orcidUrl);
		dbo.setSerialized(getSerializedDto(dto));
		return dbo;
	}

	private static VerificationSubmission getDtoFromDbo(DBOVerificationSubmission dbo) {
		VerificationSubmission dto;
		try {
			return (VerificationSubmission) JDOSecondaryPropertyUtils.decompressedObject(dbo.getSerialized());
		} catch (IOException e) {
			throw new RuntimeException("Could not deserialize existing DBO for migration", e);
		}
	}

	private static byte[] getSerializedDto(VerificationSubmission dto) {
		try {
			return JDOSecondaryPropertyUtils.compressObject(dto);
		} catch (IOException e) {
			throw new RuntimeException("Could not deserialize existing DBO for migration", e);
		}
	}


	@Test
	public void testTranslatorNoORCID() {
		DBOVerificationSubmission dbo = new DBOVerificationSubmission();
		MigratableTableTranslation<DBOVerificationSubmission, DBOVerificationSubmission> translator = dbo.getTranslator();
		DBOVerificationSubmission backup = newDBO(null);
		dbo = translator.createDatabaseObjectFromBackup(backup);
		assertEquals(dbo, backup);
	}

	@Test
	public void testTranslatorORCIDHTTP() {
		DBOVerificationSubmission dbo = new DBOVerificationSubmission();
		MigratableTableTranslation<DBOVerificationSubmission, DBOVerificationSubmission> translator = dbo.getTranslator();
		DBOVerificationSubmission backup = newDBO("http://orcid.org/000-111-222");
		dbo = translator.createDatabaseObjectFromBackup(backup);
		VerificationSubmission dto = getDtoFromDbo(dbo);
		assertEquals("https://orcid.org/000-111-222", dto.getOrcid());

		// everything else should be the same
		VerificationSubmission backupDto = getDtoFromDbo(backup);
		backupDto.setOrcid(dto.getOrcid());
		backup.setSerialized(getSerializedDto(backupDto));

		assertEquals(dbo, backup);
	}


	@Test
	public void testTranslatorORCIDHTTPS() {
		DBOVerificationSubmission dbo = new DBOVerificationSubmission();
		MigratableTableTranslation<DBOVerificationSubmission, DBOVerificationSubmission> translator = dbo.getTranslator();
		DBOVerificationSubmission backup = newDBO("https://orcid.org/000-111-222");
		dbo = translator.createDatabaseObjectFromBackup(backup);
		assertEquals(dbo, backup);
	}
}
