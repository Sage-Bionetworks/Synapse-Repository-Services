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
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.dataaccess.AccessType;
import org.sagebionetworks.repo.model.dataaccess.AccessorChange;
import org.sagebionetworks.repo.model.dataaccess.PrincipalInvestigator;
import org.sagebionetworks.repo.model.dataaccess.Renewal;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.SigningOfficial;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class DBORequestTest {

	private static final String SCHEMA_DATA = "{\"institution\":{\"name\":\"Sage\",\"tags\":[\"a\",2]},\"count\":42,\"flag\":true}";

	/**
	 * Loads a blob that was written by the XStream based implementation of the previous stack for a request with all the fields
	 * set
	 */
	private static byte[] loadBlob(String fixtureName) throws IOException {
		try (InputStream in = DBORequestTest.class.getClassLoader().getResourceAsStream("dataaccess/" + fixtureName)) {
			return Base64.getDecoder().decode(new String(in.readAllBytes(), StandardCharsets.UTF_8).trim());
		}
	}

	private static byte[] writeBlob(Request request) throws IOException {
		return JDOSecondaryPropertyUtils.compressObject(UnmodifiableXStream.builder()
				.allowTypeHierarchy(RequestInterface.class)
				.allowTypesByWildcard(new String[] { "org.sagebionetworks.repo.model.**" }).build(), request);
	}

	/**
	 * @return The backup of a row written by the previous stack: the base columns and the blob only
	 */
	private static DBORequest createBackup(byte[] blob) {
		DBORequest backup = new DBORequest();
		backup.setId(101L);
		backup.setAccessRequirementId(202L);
		backup.setResearchProjectId(303L);
		backup.setCreatedBy(404L);
		backup.setCreatedOn(1700000000000L);
		backup.setModifiedBy(505L);
		backup.setModifiedOn(1700000100000L);
		backup.setEtag("etag-1234");
		backup.setEDucContentHash("hash");
		backup.setRequestSerialized(blob);
		return backup;
	}

	/**
	 * @return The request stored in the blob fixtures, with the base columns of {@link #createBackup(byte[])}
	 */
	private static <T extends RequestInterface> T createExpectedDto(T dto) {
		dto.setId("101");
		dto.setAccessRequirementId("202");
		dto.setResearchProjectId("303");
		dto.setCreatedBy("404");
		dto.setCreatedOn(new Date(1700000000000L));
		dto.setModifiedBy("505");
		dto.setModifiedOn(new Date(1700000100000L));
		dto.setEtag("etag-1234");
		dto.setAccessRequirementVersionNumber(3L);
		dto.setDucFileHandleId("9001");
		dto.setIrbFileHandleId("9002");
		dto.setAttachments(Arrays.asList("9003", "9004"));
		dto.setAccessorChanges(Arrays.asList(new AccessorChange().setUserId("6").setType(AccessType.GAIN_ACCESS),
				new AccessorChange().setUserId("7").setType(AccessType.REVOKE_ACCESS)));
		dto.setInstitution("Sage Bionetworks");
		dto.setPrincipalInvestigator(
				new PrincipalInvestigator().setUserId("606").setName("Ada PI").setInstitutionalEmail("ada@sage.org"));
		dto.setSigningOfficial(new SigningOfficial().setName("Sam Official").setInstitutionalEmail("sam@sage.org"));
		dto.setEDucSignatureEnvelopeId("envelope-abc");
		dto.setSchemaData(new JSONObject(SCHEMA_DATA));
		return dto;
	}

	/**
	 * @return The row the DAO writes for the given request, with the content hash of {@link #createBackup(byte[])}
	 */
	private static DBORequest createExpectedRequest(RequestInterface dto) {
		DBORequest expected = RequestUtils.toDbo(dto);
		expected.setEDucContentHash("hash");
		return expected;
	}

	private static void assertTranslated(DBORequest expected, DBORequest result) {
		assertTrue(new JSONObject(expected.getRequestJson()).similar(new JSONObject(result.getRequestJson())),
				() -> "Unexpected document: " + result.getRequestJson());
		// The order of the keys in the document is not relevant, it was verified above
		expected.setRequestJson(result.getRequestJson());
		assertEquals(expected, result);
		assertNull(result.getRequestSerialized());
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithRequestBlob() throws IOException {
		DBORequest backup = createBackup(loadBlob("request-blob-v1.b64"));
		DBORequest expected = createExpectedRequest(createExpectedDto(new Request()));

		// call under test
		DBORequest result = backup.getTranslator().createDatabaseObjectFromBackup(backup);

		assertTranslated(expected, result);
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithRenewalBlob() throws IOException {
		DBORequest backup = createBackup(loadBlob("renewal-blob-v1.b64"));
		Renewal renewal = createExpectedDto(new Renewal());
		renewal.setPublication("publication-link");
		renewal.setSummaryOfUse("summary of use");
		DBORequest expected = createExpectedRequest(renewal);

		// call under test
		DBORequest result = backup.getTranslator().createDatabaseObjectFromBackup(backup);

		assertTranslated(expected, result);
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithLegacyBlobAndEnvelopeColumnKeepsColumn() throws IOException {
		DBORequest backup = createBackup(loadBlob("request-blob-v1.b64"));
		backup.setEDucEnvelopeId("env-existing");

		// call under test
		DBORequest result = backup.getTranslator().createDatabaseObjectFromBackup(backup);

		assertEquals("env-existing", result.getEDucEnvelopeId());
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithLegacyBlobAndDifferentColumnsKeepsColumns() throws IOException {
		DBORequest backup = createBackup(loadBlob("request-blob-v1.b64"));
		// The blob holds etag-1234, 404 and 505
		backup.setEtag("etag-backup");
		backup.setCreatedBy(999L);
		backup.setModifiedBy(998L);
		Request dto = createExpectedDto(new Request());
		dto.setEtag("etag-backup");
		dto.setCreatedBy("999");
		dto.setModifiedBy("998");
		DBORequest expected = createExpectedRequest(dto);

		// call under test
		DBORequest result = backup.getTranslator().createDatabaseObjectFromBackup(backup);

		assertTranslated(expected, result);
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithEmptyLegacyBlob() throws IOException {
		DBORequest backup = createBackup(writeBlob(new Request()));
		DBORequest expected = createBackup(null);
		expected.setRequestJson("{\"concreteType\":\"" + Request.class.getName() + "\"}");

		// call under test
		DBORequest result = backup.getTranslator().createDatabaseObjectFromBackup(backup);

		assertTranslated(expected, result);
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithLegacyBlobAndNewFileHandleColumnRejected() throws IOException {
		DBORequest backup = createBackup(loadBlob("request-blob-v1.b64"));
		backup.setAttachmentsJson("[\"1\"]");

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			backup.getTranslator().createDatabaseObjectFromBackup(backup);
		}).getMessage();

		assertEquals("Both 'requestSerialized' and the request document or file handle columns are not null", message);
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithLegacyBlobAndNewDocumentColumnRejected() throws IOException {
		DBORequest backup = createBackup(loadBlob("request-blob-v1.b64"));
		backup.setRequestJson("{}");

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			backup.getTranslator().createDatabaseObjectFromBackup(backup);
		}).getMessage();

		assertEquals("Both 'requestSerialized' and the request document or file handle columns are not null", message);
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithNoBlob() {
		DBORequest backup = createExpectedRequest(createExpectedDto(new Request()));
		DBORequest expected = createExpectedRequest(createExpectedDto(new Request()));

		// call under test
		DBORequest result = backup.getTranslator().createDatabaseObjectFromBackup(backup);

		assertSame(backup, result);
		assertEquals(expected, result);
	}

	@Test
	public void testCreateBackupFromDatabaseObject() {
		DBORequest dbo = createExpectedRequest(createExpectedDto(new Request()));

		// call under test
		DBORequest result = dbo.getTranslator().createBackupFromDatabaseObject(dbo);

		assertSame(dbo, result);
	}
}
