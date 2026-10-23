package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.dataaccess.Renewal;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class DBORequestTest {

	private static final String SCHEMA_DATA = "{\"institution\":{\"name\":\"Sage\",\"tags\":[\"a\",2]},\"count\":42,\"flag\":true}";
	private static final String ACCESSOR_CHANGES = "[{\"userId\":\"6\",\"type\":\"GAIN_ACCESS\"},{\"userId\":\"7\",\"type\":\"REVOKE_ACCESS\"}]";
	private static final String PRINCIPAL_INVESTIGATOR = "{\"userId\":\"606\",\"name\":\"Ada PI\",\"institutionalEmail\":\"ada@sage.org\"}";
	private static final String SIGNING_OFFICIAL = "{\"name\":\"Sam Official\",\"institutionalEmail\":\"sam@sage.org\"}";

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

	private static DBORequest createExpectedRequest() {
		DBORequest expected = createBackup(null);
		expected.setConcreteType(Request.class.getName());
		expected.setAccessRequirementVersionNumber(3L);
		expected.setDucFileHandleId(9001L);
		expected.setIrbFileHandleId(9002L);
		expected.setAttachmentsJson("[\"9003\",\"9004\"]");
		expected.setAccessorChangesJson(ACCESSOR_CHANGES);
		expected.setInstitution("Sage Bionetworks");
		expected.setPrincipalInvestigatorJson(PRINCIPAL_INVESTIGATOR);
		expected.setSigningOfficialJson(SIGNING_OFFICIAL);
		expected.setEDucEnvelopeId("envelope-abc");
		return expected;
	}

	private static void assertSchemaData(DBORequest result) {
		assertTrue(new JSONObject(SCHEMA_DATA).similar(new JSONObject(result.getSchemaDataJson())),
				() -> "Unexpected schemaData: " + result.getSchemaDataJson());
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithRequestBlob() throws IOException {
		DBORequest backup = createBackup(loadBlob("request-blob-v1.b64"));
		DBORequest expected = createExpectedRequest();

		// call under test
		DBORequest result = backup.getTranslator().createDatabaseObjectFromBackup(backup);

		assertSchemaData(result);
		// The order of the keys in the stored JSON is not relevant, it was verified above
		expected.setSchemaDataJson(result.getSchemaDataJson());
		assertEquals(expected, result);
		assertNull(result.getRequestSerialized());
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithRenewalBlob() throws IOException {
		DBORequest backup = createBackup(loadBlob("renewal-blob-v1.b64"));
		DBORequest expected = createExpectedRequest();
		expected.setConcreteType(Renewal.class.getName());
		expected.setPublication("publication-link");
		expected.setSummaryOfUse("summary of use");

		// call under test
		DBORequest result = backup.getTranslator().createDatabaseObjectFromBackup(backup);

		assertSchemaData(result);
		expected.setSchemaDataJson(result.getSchemaDataJson());
		assertEquals(expected, result);
		assertNull(result.getRequestSerialized());
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithBlobAndEnvelopeColumn() throws IOException {
		DBORequest backup = createBackup(loadBlob("request-blob-v1.b64"));
		backup.setEDucEnvelopeId("env-existing");

		// call under test
		DBORequest result = backup.getTranslator().createDatabaseObjectFromBackup(backup);

		assertEquals("env-existing", result.getEDucEnvelopeId());
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithBlobAndNoEnvelope() throws IOException {
		DBORequest backup = createBackup(writeBlob(new Request()));

		// call under test
		DBORequest result = backup.getTranslator().createDatabaseObjectFromBackup(backup);

		assertNull(result.getEDucEnvelopeId());
		assertEquals(Request.class.getName(), result.getConcreteType());
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithBlobAndContentColumns() throws IOException {
		DBORequest backup = createBackup(loadBlob("request-blob-v1.b64"));
		backup.setAttachmentsJson("[\"1\"]");

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			backup.getTranslator().createDatabaseObjectFromBackup(backup);
		}).getMessage();

		assertEquals("Both 'requestSerialized' and the request content columns are not null", message);
	}

	@Test
	public void testCreateDatabaseObjectFromBackupWithNoBlob() {
		DBORequest backup = createExpectedRequest();
		DBORequest expected = createExpectedRequest();

		// call under test
		DBORequest result = backup.getTranslator().createDatabaseObjectFromBackup(backup);

		assertSame(backup, result);
		assertEquals(expected, result);
	}

	@Test
	public void testCreateBackupFromDatabaseObject() {
		DBORequest dbo = createExpectedRequest();

		// call under test
		DBORequest result = dbo.getTranslator().createBackupFromDatabaseObject(dbo);

		assertSame(dbo, result);
	}
}
