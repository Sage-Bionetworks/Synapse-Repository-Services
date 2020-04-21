package org.sagebionetworks.repo.model.dbo.schema;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.schema.SchemaInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.id.SchemaId;
import org.sagebionetworks.schema.parser.SchemaIdParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class JsonSchemaDaoImplTest {

	@Autowired
	private JsonSchemaDao jsonSchemaDao;

	@Autowired
	private OrganizationDao organizationDao;

	private Organization organization;
	private String organizationName;
	private NewSchemaRequest newSchemaRequest;
	private SchemaInfo schemaInfo;
	private JsonSchema schema;
	private String schemaJson;
	private String schemaJsonSha256Hex;
	private String semanticVersion;

	private Long adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();

	@BeforeEach
	public void before() throws JSONObjectAdapterException {
		organizationName = "Foo.baR";
		organization = organizationDao.createOrganization(organizationName, adminUserId);

		newSchemaRequest = new NewSchemaRequest().withSchemaName("path.Name").withOrganizationId(organization.getId())
				.withCreatedBy(adminUserId);
		semanticVersion = "1.2.3";

		schema = new JsonSchema();
		schema.set$id(newSchemaRequest.getOrganizationId() + "/" + newSchemaRequest.getSchemaName());
		schemaJson = EntityFactory.createJSONStringForEntity(schema);
		schemaJsonSha256Hex = DigestUtils.sha256Hex(schemaJson);
	}

	@AfterEach
	public void after() {
		jsonSchemaDao.trunacteAll();
		organizationDao.truncateAll();
	}

	@Test
	public void testCreateSchemaIfDoesNotExist() {
		// call under test
		SchemaInfo result = jsonSchemaDao.createSchemaIfDoesNotExist(newSchemaRequest);
		assertNotNull(result);
		assertNotNull(result.getNumericId());
		assertEquals(newSchemaRequest.getSchemaName(), result.getName());
		assertEquals(newSchemaRequest.getOrganizationId(), result.getOrganizationId());
		assertEquals(newSchemaRequest.getCreatedBy().toString(), result.getCreatedBy());
		assertNotNull(result.getCreatedOn());
	}

	@Test
	public void testCreateSchemaIfDoesNotExistDuplicate() {
		// call under test
		SchemaInfo first = jsonSchemaDao.createSchemaIfDoesNotExist(newSchemaRequest);
		SchemaInfo second = jsonSchemaDao.createSchemaIfDoesNotExist(newSchemaRequest);
		assertEquals(first, second);
	}

	@Test
	public void testCreateSchemaIfDoesNotExistNull() {
		newSchemaRequest = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createSchemaIfDoesNotExist(newSchemaRequest);
		});
	}

	@Test
	public void testCreateSchemaIfDoesNotExistNullOrganization() {
		newSchemaRequest.withOrganizationId(null);
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createSchemaIfDoesNotExist(newSchemaRequest);
		});
	}

	@Test
	public void testCreateSchemaIfDoesNotExistNullName() {
		newSchemaRequest.withSchemaName(null);
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createSchemaIfDoesNotExist(newSchemaRequest);
		});
	}

	@Test
	public void testCreateSchemaIfDoesNotExistNullCreatedBy() {
		newSchemaRequest.withCreatedBy(null);
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createSchemaIfDoesNotExist(newSchemaRequest);
		});
	}

	@Test
	public void testGetSchemaInfoForUpdate() {
		SchemaInfo result = jsonSchemaDao.createSchemaIfDoesNotExist(newSchemaRequest);
		assertNotNull(result);
		// call under test
		SchemaInfo fromGet = jsonSchemaDao.getSchemaInfoForUpdate(newSchemaRequest.getOrganizationId(),
				newSchemaRequest.getSchemaName());
		assertEquals(result, fromGet);
	}

	@Test
	public void testGetSchemaInfoForUpdateNullOrganizationId() {
		newSchemaRequest.withOrganizationId(null);
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.getSchemaInfoForUpdate(newSchemaRequest.getOrganizationId(),
					newSchemaRequest.getSchemaName());
		});
	}

	@Test
	public void testGetSchemaInfoForUpdateNullName() {
		newSchemaRequest.withSchemaName(null);
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.getSchemaInfoForUpdate(newSchemaRequest.getOrganizationId(),
					newSchemaRequest.getSchemaName());
		});
	}

	@Test
	public void testGetSchemaInfoForUpdateNotFound() {
		newSchemaRequest.withOrganizationId("-1");
		String message = assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getSchemaInfoForUpdate(newSchemaRequest.getOrganizationId(),
					newSchemaRequest.getSchemaName());
		}).getMessage();
		assertEquals("JsonSchema not found for organizationId: '-1' and schemaName: 'path.Name'", message);
	}

	@Test
	public void testCreateJsonBlobIfDoesNotExist() {
		// call under test
		String blobIdOne = jsonSchemaDao.createJsonBlobIfDoesNotExist(schemaJson, schemaJsonSha256Hex);
		String blobIdTwo = jsonSchemaDao.createJsonBlobIfDoesNotExist(schemaJson, schemaJsonSha256Hex);
		assertEquals(blobIdOne, blobIdTwo);
	}

	@Test
	public void testCreateJsonBlobIfDoesNotExistNullJson() {
		schemaJson = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createJsonBlobIfDoesNotExist(schemaJson, schemaJsonSha256Hex);
		});
	}

	@Test
	public void testCreateJsonBlobIfDoesNotExistNullSchemaJsonSha256Hex() {
		schemaJsonSha256Hex = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createJsonBlobIfDoesNotExist(schemaJson, schemaJsonSha256Hex);
		});
	}

	@Test
	public void testGetJsonBlobId() {
		String blobIdOne = jsonSchemaDao.createJsonBlobIfDoesNotExist(schemaJson, schemaJsonSha256Hex);
		// call under test
		String blobIdTwo = jsonSchemaDao.getJsonBlobId(schemaJsonSha256Hex);
		assertEquals(blobIdOne, blobIdTwo);
	}

	@Test
	public void testGetJsonBlobIdNllSha() {
		schemaJsonSha256Hex = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.getJsonBlobId(schemaJsonSha256Hex);
		});
	}

	@Test
	public void testGetJsonBlobIdNotFound() {
		schemaJsonSha256Hex = "fffeee111";
		String message = assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getJsonBlobId(schemaJsonSha256Hex);
		}).getMessage();
		assertEquals("JSON blob does not exist for sha256Hex: fffeee111", message);
	}

	public NewVersionRequest setupValidNewVersionRequest() {
		schemaInfo = jsonSchemaDao.createSchemaIfDoesNotExist(newSchemaRequest);
		assertNotNull(schemaInfo);
		String blobId = jsonSchemaDao.createJsonBlobIfDoesNotExist(schemaJson, schemaJsonSha256Hex);
		return new NewVersionRequest().withSchemaId(schemaInfo.getNumericId()).withSemanticVersion(semanticVersion)
				.withCreatedBy(adminUserId).withBlobId(blobId);
	}

	@Test
	public void testCreateNewVersion() {
		NewVersionRequest request = setupValidNewVersionRequest();

		// call under test
		JsonSchemaVersionInfo info = jsonSchemaDao.createNewVersion(request);
		assertNotNull(info);
		assertNotNull(info.getVersionId());
		assertEquals(request.getCreatedBy().toString(), info.getCreatedBy());
		assertNotNull(info.getCreatedOn());
		assertEquals(schemaJsonSha256Hex, info.getJsonSHA256Hex());
		assertEquals(semanticVersion, info.getSemanticVersion());
	}

	@Test
	public void testCreateNewVersionDuplicate() {
		NewVersionRequest request = setupValidNewVersionRequest();
		jsonSchemaDao.createNewVersion(request);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			jsonSchemaDao.createNewVersion(request);
		}).getMessage();
		assertEquals("Semantic version: '1.2.3' already exists for this JSON schema", message);
	}

	@Test
	public void testCreateNewVersionDuplicateNullVersion() {
		// can have as many null versions as desired.
		NewVersionRequest request = setupValidNewVersionRequest();
		request.withSemanticVersion(null);
		JsonSchemaVersionInfo one = jsonSchemaDao.createNewVersion(request);
		JsonSchemaVersionInfo two = jsonSchemaDao.createNewVersion(request);
		// two versions are not the same
		assertFalse(one.getVersionId().equals(two.getVersionId()));
	}

	@Test
	public void testCreateNewVersionNullRequest() {
		NewVersionRequest request = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createNewVersion(request);
		});
	}

	@Test
	public void testCreateNewVersionNullSchemaId() {
		NewVersionRequest request = setupValidNewVersionRequest();
		request.withSchemaId(null);
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createNewVersion(request);
		});
	}

	@Test
	public void testCreateNewVersionNullBlobId() {
		NewVersionRequest request = setupValidNewVersionRequest();
		request.withBlobId(null);
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createNewVersion(request);
		});
	}

	@Test
	public void testCreateNewVersionNullCreatedBy() {
		NewVersionRequest request = setupValidNewVersionRequest();
		request.withCreatedBy(null);
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createNewVersion(request);
		});
	}

	@Test
	public void testCreateNewVersionNullSemanticVersion() {
		NewVersionRequest request = setupValidNewVersionRequest();
		request.withSemanticVersion(null);
		JsonSchemaVersionInfo info = jsonSchemaDao.createNewVersion(request);
		assertNotNull(info);
		assertNull(info.getSemanticVersion());
	}

	@Test
	public void testGetVersionInfo() {
		NewVersionRequest request = setupValidNewVersionRequest();
		JsonSchemaVersionInfo info = jsonSchemaDao.createNewVersion(request);
		// call under test
		JsonSchemaVersionInfo getInfo = jsonSchemaDao.getVersionInfo(info.getVersionId());
		assertEquals(info, getInfo);
	}

	@Test
	public void testGetVersionInfoNotFound() {
		String versionId = "-1";
		String message = assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getVersionInfo(versionId);
		}).getMessage();
		assertEquals("JSON version not found for versionId: -1", message);
	}

	@Test
	public void testGetVersionInfoNullId() {
		String versionId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.getVersionInfo(versionId);
		});
	}

	/**
	 * Helper to create a new JSON schema with the given $id
	 * 
	 * @param schema
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	JsonSchemaVersionInfo createNewSchemaVersion(String id, int index) throws JSONObjectAdapterException {
		SchemaId schemaId = SchemaIdParser.parseSchemaId(id);
		String organizationName = schemaId.getOrganizationName().toString();
		String schemaName = schemaId.getSchemaName().toString();
		String semanticVersion = null;
		if (schemaId.getSemanticVersion() != null) {
			semanticVersion = schemaId.getSemanticVersion().toString();
		}
		Organization organization = null;
		try {
			organization = organizationDao.getOrganizationByName(organizationName);
		} catch (NotFoundException e) {
			organization = organizationDao.createOrganization(organizationName, adminUserId);
		}
		SchemaInfo schemaInfo = jsonSchemaDao.createSchemaIfDoesNotExist(new NewSchemaRequest()
				.withCreatedBy(adminUserId).withOrganizationId(organization.getId()).withSchemaName(schemaName));
		JsonSchema schema = new JsonSchema();
		schema.set$id(id);
		schema.setDescription("index:" + index);
		String json = EntityFactory.createJSONStringForEntity(schema);
		String sha256hex = DigestUtils.sha256Hex(json);
		String blobId = jsonSchemaDao.createJsonBlobIfDoesNotExist(json, sha256hex);
		return jsonSchemaDao.createNewVersion(new NewVersionRequest().withBlobId(blobId).withCreatedBy(adminUserId)
				.withSchemaId(schemaInfo.getNumericId()).withSemanticVersion(semanticVersion));
	}

	/**
	 * Helper to get the SHA-256 hex string for a given schema.
	 * 
	 * @param schema
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	static String getSchemaSHA256Hex(JsonSchema schema) throws JSONObjectAdapterException {
		String json = EntityFactory.createJSONStringForEntity(schema);
		return DigestUtils.sha256Hex(json);
	}

	@Test
	public void testGetLatestVersion() throws JSONObjectAdapterException {
		int index = 0;
		createNewSchemaVersion("my.org.edu/foo.bar", index++);
		createNewSchemaVersion("my.org.edu/foo.bar", index++);
		createNewSchemaVersion("my.org.edu/foo.bar/1.0.1", index++);
		createNewSchemaVersion("my.org.edu/foo.bar", index++);
		createNewSchemaVersion("other.org/foo.bar", index++);
		createNewSchemaVersion("my.org.edu/foo.bar.other", index++);
		// Call under test
		JsonSchema schema = jsonSchemaDao.getSchemaLatestVersion("my.org.edu", "foo.bar");
		assertNotNull(schema);
		assertEquals("my.org.edu/foo.bar", schema.get$id());
		assertEquals("index:3", schema.getDescription());
	}

	@Test
	public void testGetLatestVersionOrganziationNotFound() throws JSONObjectAdapterException {
		int index = 0;
		createNewSchemaVersion("my.org.edu/foo.bar", index++);
		String message = assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getSchemaLatestVersion("other.org", "foo.bar");
		}).getMessage();
		assertEquals("JSON Schema not found for organizationName: 'other.org' and schemaName: 'foo.bar'", message);
	}

	@Test
	public void testGetLatestVersionSchemaNameNotFound() throws JSONObjectAdapterException {
		int index = 0;
		createNewSchemaVersion("my.org.edu/foo.bar", index++);
		String message = assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getSchemaLatestVersion("my.org.edu", "other.schema.name");
		}).getMessage();
		assertEquals("JSON Schema not found for organizationName: 'my.org.edu' and schemaName: 'other.schema.name'",
				message);
	}

	@Test
	public void testGetSchemaLatestVersionNullOrganization() {
		String organizationName = null;
		String schemaName ="other.schema.name";
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.getSchemaLatestVersion(organizationName, schemaName);
		});
	}
	
	@Test
	public void testGetSchemaLatestVersionNullSchema() {
		String organizationName = "my.org.edu";
		String schemaName = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.getSchemaLatestVersion(organizationName, schemaName);
		});
	}

	@Test
	public void testGetSchemaVersion() throws JSONObjectAdapterException {
		int index = 0;
		createNewSchemaVersion("my.org.edu/foo.bar/0.0.1", index++);
		createNewSchemaVersion("my.org.edu/foo.bar", index++);
		createNewSchemaVersion("my.org.edu/foo.bar/1.0.1", index++);
		createNewSchemaVersion("my.org.edu/foo.bar", index++);
		createNewSchemaVersion("other.org/foo.bar", index++);
		createNewSchemaVersion("my.org.edu/foo.bar.other", index++);

		// call under test
		JsonSchema schema = jsonSchemaDao.getSchemaVersion("my.org.edu", "foo.bar", "1.0.1");
		assertNotNull(schema);
		assertEquals("my.org.edu/foo.bar/1.0.1", schema.get$id());
		assertEquals("index:2", schema.getDescription());
	}

	@Test
	public void testGetSchemaVersionOrganizationNotFound() throws JSONObjectAdapterException {
		int index = 0;
		createNewSchemaVersion("my.org.edu/foo.bar/1.0.1", index++);
		String message = assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getSchemaVersion("other.org", "foo.bar", "1.0.1");
		}).getMessage();
		assertEquals(
				"JSON Schema not found for organizationName: 'other.org' and schemaName: 'foo.bar' and semanticVersion: '1.0.1'",
				message);
	}

	@Test
	public void testGetSchemaVersionSchemaNameNotFound() throws JSONObjectAdapterException {
		int index = 0;
		createNewSchemaVersion("my.org.edu/foo.bar/1.0.1", index++);
		assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getSchemaVersion("my.org.edu", "foo.bar.bar", "1.0.1");
		});
	}

	@Test
	public void testGetSchemaVersionSchemaVersionNotFound() throws JSONObjectAdapterException {
		int index = 0;
		createNewSchemaVersion("my.org.edu/foo.bar/1.0.1", index++);
		assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getSchemaVersion("my.org.edu", "foo.bar", "1.0.2");
		});
	}
	
	@Test
	public void testGetSchemaVersionSchemaVersionNullOrganization() throws JSONObjectAdapterException {
		String organizationName = null;
		String schemaName ="other.schema.name";
		String semanticVersion = "1.0.2";
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.getSchemaVersion(organizationName, schemaName, semanticVersion);
		});
	}
	
	@Test
	public void testGetSchemaVersionSchemaVersionNullSchema() throws JSONObjectAdapterException {
		String organizationName = "my.org.edu";
		String schemaName = null;
		String semanticVersion = "1.0.2";
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.getSchemaVersion(organizationName, schemaName, semanticVersion);
		});
	}
	
	@Test
	public void testGetSchemaVersionSchemaVersionNullVersion() throws JSONObjectAdapterException {
		String organizationName = "my.org.edu";
		String schemaName ="other.schema.name";
		String semanticVersion = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.getSchemaVersion(organizationName, schemaName, semanticVersion);
		});
	}

}
