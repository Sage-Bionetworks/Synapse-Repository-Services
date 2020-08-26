package org.sagebionetworks.repo.model.dbo.schema;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.schema.BoundObjectType;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaInfo;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.schema.NormalizedJsonSchema;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Lists;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class JsonSchemaDaoImplTest {

	@Autowired
	private JsonSchemaDaoImpl jsonSchemaDao;

	@Autowired
	private OrganizationDao organizationDao;
	
	@Autowired
	private JsonSchemaTestHelper jsonSchemaTestHelper;

	private Organization organization;
	private String organizationName;
	private String organizationId;
	private String schemaName;
	private JsonSchema schema;
	private String schemaJson;
	private String schemaJsonSha256Hex;
	private String semanticVersion;
	private Long createdBy;
	private String schemaId;
	private String blobId;
	private NewSchemaVersionRequest newSchemaVersionRequest;
	private BindSchemaRequest bindSchemaRequest;

	private Long adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();

	@BeforeEach
	public void before() throws JSONObjectAdapterException {
		jsonSchemaTestHelper.truncateAll();

		createdBy = adminUserId;
		organizationName = "Foo.baR";
		organization = organizationDao.createOrganization(organizationName, adminUserId);
		organizationId = organization.getId();

		schemaName = "path.Name";
		semanticVersion = "1.2.3";

		schema = new JsonSchema();
		schema.set$id(organizationName + "-" + schemaName);
		schemaJson = EntityFactory.createJSONStringForEntity(schema);
		schemaJsonSha256Hex = DigestUtils.sha256Hex(schemaJson);

		newSchemaVersionRequest = new NewSchemaVersionRequest().withCreatedBy(createdBy)
				.withOrganizationId(organizationId).withJsonSchema(schema).withSchemaName(schemaName)
				.withSemanticVersion(semanticVersion);

		bindSchemaRequest = new BindSchemaRequest().withCreatedBy(adminUserId).withObjectId(123L)
				.withObjectType(BoundObjectType.entity);

	}

	@AfterEach
	public void after() {
		jsonSchemaTestHelper.truncateAll();
	}

	@Test
	public void testCreateSchemaIfDoesNotExist() {
		// call under test
		String schemaId = jsonSchemaDao.createSchemaIfDoesNotExist(organizationId, schemaName, createdBy);
		assertNotNull(schemaId);
	}

	@Test
	public void testCreateSchemaIfDoesNotExistDuplicate() {
		// call under test
		String first = jsonSchemaDao.createSchemaIfDoesNotExist(organizationId, schemaName, createdBy);
		String second = jsonSchemaDao.createSchemaIfDoesNotExist(organizationId, schemaName, createdBy);
		assertEquals(first, second);
	}

	@Test
	public void testCreateSchemaIfDoesNotExistNullOrganization() {
		organizationId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createSchemaIfDoesNotExist(organizationId, schemaName, createdBy);
		});
	}

	@Test
	public void testCreateSchemaIfDoesNotExistNullName() {
		schemaName = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createSchemaIfDoesNotExist(organizationId, schemaName, createdBy);
		});
	}

	@Test
	public void testCreateSchemaIfDoesNotExistNullCreatedBy() {
		createdBy = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createSchemaIfDoesNotExist(organizationId, schemaName, createdBy);
		});
	}

	@Test
	public void testGetSchemaInfoForUpdate() {
		String schemaId = jsonSchemaDao.createSchemaIfDoesNotExist(organizationId, schemaName, createdBy);
		assertNotNull(schemaId);
		// call under test
		String fetchedSchemaId = jsonSchemaDao.getSchemaInfoForUpdate(organizationId, schemaName);
		assertEquals(schemaId, fetchedSchemaId);
	}

	@Test
	public void testGetSchemaInfoForUpdateNullOrganizationId() {
		organizationId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.getSchemaInfoForUpdate(organizationId, schemaName);
		});
	}

	@Test
	public void testGetSchemaInfoForUpdateNullName() {
		schemaName = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.getSchemaInfoForUpdate(organizationId, schemaName);
		});
	}

	@Test
	public void testGetSchemaInfoForUpdateNotFound() {
		organizationId = "-1";
		String message = assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getSchemaInfoForUpdate(organizationId, schemaName);
		}).getMessage();
		assertEquals("JsonSchema not found for organizationId: '-1' and schemaName: 'path.Name'", message);
	}

	@Test
	public void testCreateJsonBlobIfDoesNotExist() {
		// call under test
		String blobIdOne = jsonSchemaDao.createJsonBlobIfDoesNotExist(schema);
		String blobIdTwo = jsonSchemaDao.createJsonBlobIfDoesNotExist(schema);
		assertEquals(blobIdOne, blobIdTwo);
	}

	@Test
	public void testCreateJsonBlobIfDoesNotExistNullJson() {
		schema = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createJsonBlobIfDoesNotExist(schema);
		});
	}

	@Test
	public void testGetJsonBlobId() {
		String blobIdOne = jsonSchemaDao.createJsonBlobIfDoesNotExist(schema);
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

	public void setupSchemaIdAndBlobId() {
		schemaId = jsonSchemaDao.createSchemaIfDoesNotExist(organizationId, schemaName, adminUserId);
		blobId = jsonSchemaDao.createJsonBlobIfDoesNotExist(schema);
	}

	@Test
	public void testCreateNewVersion() {
		setupSchemaIdAndBlobId();

		// call under test
		JsonSchemaVersionInfo info = jsonSchemaDao.createNewVersion(schemaId, semanticVersion, createdBy, blobId);
		assertNotNull(info);
		assertNotNull(info.getVersionId());
		assertEquals(createdBy.toString(), info.getCreatedBy());
		assertNotNull(info.getCreatedOn());
		assertEquals(schemaJsonSha256Hex, info.getJsonSHA256Hex());
		assertEquals(semanticVersion, info.getSemanticVersion());
	}

	@Test
	public void testCreateNewVersionDuplicate() {
		setupSchemaIdAndBlobId();

		jsonSchemaDao.createNewVersion(schemaId, semanticVersion, createdBy, blobId);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			jsonSchemaDao.createNewVersion(schemaId, semanticVersion, createdBy, blobId);
		}).getMessage();
		assertEquals("Semantic version: '1.2.3' already exists for this JSON schema", message);
	}

	@Test
	public void testCreateNewVersionDuplicateNullVersion() {
		// can have as many null versions as desired.
		setupSchemaIdAndBlobId();

		semanticVersion = null;
		JsonSchemaVersionInfo one = jsonSchemaDao.createNewVersion(schemaId, semanticVersion, createdBy, blobId);
		String firstEtag = jsonSchemaDao.getLatestVersionEtag(one.getSchemaId());
		assertNotNull(firstEtag);
		JsonSchemaVersionInfo two = jsonSchemaDao.createNewVersion(schemaId, semanticVersion, createdBy, blobId);
		String secondEtag = jsonSchemaDao.getLatestVersionEtag(one.getSchemaId());
		assertNotNull(secondEtag);
		// two versions are not the same
		assertFalse(one.getVersionId().equals(two.getVersionId()));
		// The latest version etag should have changed
		assertFalse(firstEtag.equals(secondEtag));
	}

	@Test
	public void testCreateNewVersionNullSchemaId() {
		setupSchemaIdAndBlobId();
		schemaId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createNewVersion(schemaId, semanticVersion, createdBy, blobId);
		});
	}

	@Test
	public void testCreateNewVersionNullBlobId() {
		setupSchemaIdAndBlobId();
		blobId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createNewVersion(schemaId, semanticVersion, createdBy, blobId);
		});
	}

	@Test
	public void testCreateNewVersionNullCreatedBy() {
		setupSchemaIdAndBlobId();
		createdBy = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createNewVersion(schemaId, semanticVersion, createdBy, blobId);
		});
	}

	@Test
	public void testCreateNewVersionNullSemanticVersion() {
		setupSchemaIdAndBlobId();
		semanticVersion = null;
		JsonSchemaVersionInfo info = jsonSchemaDao.createNewVersion(schemaId, semanticVersion, createdBy, blobId);
		assertNotNull(info);
		assertNull(info.getSemanticVersion());
	}

	@Test
	public void testGetVersionInfo() throws JSONObjectAdapterException {
		JsonSchemaVersionInfo info = createNewSchemaVersion("my.org.edu-foo.bar-1.0.1", 1);
		// call under test
		JsonSchemaVersionInfo getInfo = jsonSchemaDao.getVersionInfo(info.getVersionId());
		assertNotNull(getInfo.getOrganizationId());
		assertEquals("my.org.edu", getInfo.getOrganizationName());
		assertNotNull(getInfo.getSchemaId());
		assertEquals("foo.bar", getInfo.getSchemaName());
		assertNotNull(getInfo.getVersionId());
		assertEquals("1.0.1", getInfo.getSemanticVersion());
		assertEquals("my.org.edu-foo.bar-1.0.1", getInfo.get$id());
		assertEquals(info, getInfo);
	}

	@Test
	public void testGetVersionInfoWithNullSemanticVersion() throws JSONObjectAdapterException {
		JsonSchemaVersionInfo info = createNewSchemaVersion("my.org.edu-foo.bar", 1);
		// call under test
		JsonSchemaVersionInfo getInfo = jsonSchemaDao.getVersionInfo(info.getVersionId());
		assertNotNull(getInfo.getOrganizationId());
		assertEquals("my.org.edu", getInfo.getOrganizationName());
		assertNotNull(getInfo.getSchemaId());
		assertEquals("foo.bar", getInfo.getSchemaName());
		assertNotNull(getInfo.getVersionId());
		assertEquals(null, getInfo.getSemanticVersion());
		assertEquals("my.org.edu-foo.bar", getInfo.get$id());
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

	@Test
	public void testCreateNewSchemaVersion() {
		// Call under test
		JsonSchemaVersionInfo info = jsonSchemaDao.createNewSchemaVersion(newSchemaVersionRequest);
		assertNotNull(info);
		assertEquals(organizationId, info.getOrganizationId());
		assertEquals(organizationName, info.getOrganizationName());
		assertNotNull(info.getSchemaId());
		assertEquals(schemaName, info.getSchemaName());
		assertEquals(createdBy.toString(), info.getCreatedBy());
		assertEquals(semanticVersion, info.getSemanticVersion());
		assertNotNull(info.getVersionId());
		assertEquals(schemaJsonSha256Hex, info.getJsonSHA256Hex());
	}

	@Test
	public void testCreateNewSchemaVersionSchemaNameAtLimit() {
		String schemaName = StringUtils.repeat("a", JsonSchemaDaoImpl.MAX_SCHEMA_NAME_CHARS);
		newSchemaVersionRequest.withSchemaName(schemaName);
		JsonSchemaVersionInfo info = jsonSchemaDao.createNewSchemaVersion(newSchemaVersionRequest);
		assertNotNull(info);
		assertEquals(schemaName, info.getSchemaName());
	}

	@Test
	public void testCreateNewSchemaVersionSchemaNameOverLimit() {
		String schemaName = StringUtils.repeat("a", JsonSchemaDaoImpl.MAX_SCHEMA_NAME_CHARS + 1);
		newSchemaVersionRequest.withSchemaName(schemaName);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createNewSchemaVersion(newSchemaVersionRequest);
		}).getMessage();
		assertEquals("Schema name must be " + JsonSchemaDaoImpl.MAX_SCHEMA_NAME_CHARS + " characters or less", message);
	}

	@Test
	public void testCreateNewSchemaVersionSemanticVersionAtLimit() {
		semanticVersion = StringUtils.repeat("a", JsonSchemaDaoImpl.MAX_SEMANTIC_VERSION_CHARS);
		newSchemaVersionRequest.withSemanticVersion(semanticVersion);
		JsonSchemaVersionInfo info = jsonSchemaDao.createNewSchemaVersion(newSchemaVersionRequest);
		assertNotNull(info);
		assertEquals(semanticVersion, info.getSemanticVersion());
	}

	@Test
	public void testCreateNewSchemaVersionSemanticVersionOverLimit() {
		semanticVersion = StringUtils.repeat("a", JsonSchemaDaoImpl.MAX_SEMANTIC_VERSION_CHARS + 1);
		newSchemaVersionRequest.withSemanticVersion(semanticVersion);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createNewSchemaVersion(newSchemaVersionRequest);
		}).getMessage();
		assertEquals("Semantic version must be " + JsonSchemaDaoImpl.MAX_SEMANTIC_VERSION_CHARS + " characters or less",
				message);
	}

	@Test
	public void testCreateNewSchemaVerionNullRequest() {
		newSchemaVersionRequest = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createNewSchemaVersion(newSchemaVersionRequest);
		});
	}

	@Test
	public void testCreateNewSchemaVerionNullRequestNullOrganizationId() {
		newSchemaVersionRequest.withOrganizationId(null);
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createNewSchemaVersion(newSchemaVersionRequest);
		});
	}

	@Test
	public void testCreateNewSchemaVerionNullRequestNullSchemaName() {
		newSchemaVersionRequest.withSchemaName(null);
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createNewSchemaVersion(newSchemaVersionRequest);
		});
	}

	@Test
	public void testCreateNewSchemaVerionNullRequestNullCreatedBy() {
		newSchemaVersionRequest.withCreatedBy(null);
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createNewSchemaVersion(newSchemaVersionRequest);
		});
	}

	@Test
	public void testCreateNewSchemaVersionNullSemanticVersion() {
		semanticVersion = null;
		newSchemaVersionRequest.withSemanticVersion(semanticVersion);
		JsonSchemaVersionInfo info = jsonSchemaDao.createNewSchemaVersion(newSchemaVersionRequest);
		assertNotNull(info);
		assertEquals(semanticVersion, info.getSemanticVersion());
	}

	@Test
	public void testCreateNewSchemaVerionNullSchema() {
		newSchemaVersionRequest.withJsonSchema(null);
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createNewSchemaVersion(newSchemaVersionRequest);
		});
	}

	/**
	 * Helper to create a new JSON schema with the given $id
	 * 
	 * @param id
	 * @param index
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	JsonSchemaVersionInfo createNewSchemaVersion(String id, int index) throws JSONObjectAdapterException {
		return jsonSchemaTestHelper.createNewSchemaVersion(adminUserId, id, index);
	}

	/**
	 * Helper to create a new JSON schema with the given $id
	 * 
	 * @param schema
	 * @return
	 * @throws JSONObjectAdapterException
	 */
	JsonSchemaVersionInfo createNewSchemaVersion(String id, int index, ArrayList<SchemaDependency> dependencies)
			throws JSONObjectAdapterException {
		return jsonSchemaTestHelper.createNewSchemaVersion(adminUserId, id, index, dependencies);
	}

	/**
	 * Create or get an organization with the given name
	 * 
	 * @param organizationName
	 * @return
	 */
	Organization createOrganization(String organizationName) {
		return jsonSchemaTestHelper.createOrganization(adminUserId, organizationName);
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
		createNewSchemaVersion("my.org.edu-foo.bar", index++);
		createNewSchemaVersion("my.org.edu-foo.bar", index++);
		createNewSchemaVersion("my.org.edu-foo.bar-1.0.1", index++);
		JsonSchemaVersionInfo lastVersion = createNewSchemaVersion("my.org.edu-foo.bar", index++);
		createNewSchemaVersion("other.org-foo.bar", index++);
		createNewSchemaVersion("my.org.edu-foo.bar.other", index++);
		// Call under test
		String versionId = jsonSchemaDao.getLatestVersionId("my.org.edu", "foo.bar");
		assertEquals(lastVersion.getVersionId(), versionId);
	}

	@Test
	public void testGetLatestVersionOrganziationNotFound() throws JSONObjectAdapterException {
		int index = 0;
		createNewSchemaVersion("my.org.edu-foo.bar", index++);
		String message = assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getLatestVersionId("other.org", "foo.bar");
		}).getMessage();
		assertEquals("JSON Schema not found for organizationName: 'other.org' and schemaName: 'foo.bar'", message);
	}

	@Test
	public void testGetLatestVersionSchemaNameNotFound() throws JSONObjectAdapterException {
		int index = 0;
		createNewSchemaVersion("my.org.edu-foo.bar", index++);
		String message = assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getLatestVersionId("my.org.edu", "other.schema.name");
		}).getMessage();
		assertEquals("JSON Schema not found for organizationName: 'my.org.edu' and schemaName: 'other.schema.name'",
				message);
	}

	@Test
	public void testGetSchemaLatestVersionNullOrganization() {
		String organizationName = null;
		String schemaName = "other.schema.name";
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.getLatestVersionId(organizationName, schemaName);
		});
	}

	@Test
	public void testGetSchemaLatestVersionNullSchema() {
		String organizationName = "my.org.edu";
		String schemaName = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.getLatestVersionId(organizationName, schemaName);
		});
	}

	@Test
	public void testGetLatestVersionId() throws JSONObjectAdapterException {
		int index = 0;
		createNewSchemaVersion("my.org.edu-foo.bar", index++);
		createNewSchemaVersion("my.org.edu-foo.bar", index++);
		createNewSchemaVersion("my.org.edu-foo.bar-1.0.1", index++);
		JsonSchemaVersionInfo lastVersion = createNewSchemaVersion("my.org.edu-foo.bar", index++);
		createNewSchemaVersion("other.org-foo.bar", index++);
		createNewSchemaVersion("my.org.edu-foo.bar.other", index++);
		// Call under test
		String versionId = jsonSchemaDao.getLatestVersionId(lastVersion.getSchemaId());
		assertEquals(lastVersion.getVersionId(), versionId);
	}

	@Test
	public void testGetLatestVersionIdWithNotFound() throws JSONObjectAdapterException {
		String schemaId = "-1";
		String message = assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getLatestVersionId(schemaId);
		}).getMessage();
		assertEquals("JSON Schema not found for schemaId: '-1'", message);
	}

	@Test
	public void testGetLatestVersionIdWithNullId() throws JSONObjectAdapterException {
		String schemaId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.getLatestVersionId(schemaId);
		});
	}

	@Test
	public void testGetVersionId() throws JSONObjectAdapterException {
		int index = 0;
		createNewSchemaVersion("my.org.edu-foo.bar-0.0.1", index++);
		createNewSchemaVersion("my.org.edu-foo.bar", index++);
		JsonSchemaVersionInfo expected = createNewSchemaVersion("my.org.edu-foo.bar-1.0.1", index++);
		createNewSchemaVersion("my.org.edu-foo.bar", index++);
		createNewSchemaVersion("other.org-foo.bar", index++);
		createNewSchemaVersion("my.org.edu-foo.bar.other", index++);

		String organizationName = "my.org.edu";
		String schemaName = "foo.bar";
		String semanticVersion = "1.0.1";
		// call under test
		String versionId = jsonSchemaDao.getVersionId(organizationName, schemaName, semanticVersion);
		assertNotNull(versionId);
		assertEquals(expected.getVersionId(), versionId);
	}

	@Test
	public void testGetVersionIdNotFound() throws JSONObjectAdapterException {
		int index = 0;
		createNewSchemaVersion("my.org.edu-foo.bar-0.0.1", index++);

		String organizationName = "my.org.edu";
		String schemaName = "foo.bar";
		String semanticVersion = "1.0.1";
		// call under test
		String message = assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getVersionId(organizationName, schemaName, semanticVersion);
		}).getMessage();
		assertEquals(
				"JSON Schema not found for organizationName: 'my.org.edu' and schemaName: 'foo.bar' and semanticVersion: '1.0.1'",
				message);
	}

	@Test
	public void testGetVersionIdNullOrgName() throws JSONObjectAdapterException {
		String organizationName = null;
		String schemaName = "foo.bar";
		String semanticVersion = "1.0.1";
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.getVersionId(organizationName, schemaName, semanticVersion);
		});
	}

	@Test
	public void testGetVersionIdNullSchemaName() throws JSONObjectAdapterException {
		String organizationName = "my.org.edu";
		String schemaName = null;
		String semanticVersion = "1.0.1";
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.getVersionId(organizationName, schemaName, semanticVersion);
		});
	}

	@Test
	public void testGetVersionIdNullSemanticVersion() throws JSONObjectAdapterException {
		String organizationName = "my.org.edu";
		String schemaName = "foo.bar";
		String semanticVersion = null;
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.getVersionId(organizationName, schemaName, semanticVersion);
		});
	}

	@Test
	public void testGetVersionInfoTripple() throws JSONObjectAdapterException {
		int index = 0;
		createNewSchemaVersion("my.org.edu-foo.bar-0.0.1", index++);
		createNewSchemaVersion("my.org.edu-foo.bar", index++);
		JsonSchemaVersionInfo expected = createNewSchemaVersion("my.org.edu-foo.bar-1.0.1", index++);
		createNewSchemaVersion("my.org.edu-foo.bar", index++);
		createNewSchemaVersion("other.org-foo.bar", index++);
		createNewSchemaVersion("my.org.edu-foo.bar.other", index++);

		// call under test
		JsonSchemaVersionInfo resultInfo = jsonSchemaDao.getVersionInfo("my.org.edu", "foo.bar", "1.0.1");
		assertEquals(expected, resultInfo);
	}

	@Test
	public void testGetSchemaVersionOrganizationNotFound() throws JSONObjectAdapterException {
		int index = 0;
		createNewSchemaVersion("my.org.edu-foo.bar-1.0.1", index++);
		String message = assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getVersionInfo("other.org", "foo.bar", "1.0.1");
		}).getMessage();
		assertEquals(
				"JSON Schema not found for organizationName: 'other.org' and schemaName: 'foo.bar' and semanticVersion: '1.0.1'",
				message);
	}

	@Test
	public void testGetSchemaVersionSchemaNameNotFound() throws JSONObjectAdapterException {
		int index = 0;
		createNewSchemaVersion("my.org.edu-foo.bar-1.0.1", index++);
		String message = assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getVersionInfo("my.org.edu", "foo.bar.bar", "1.0.1");
		}).getMessage();
		assertEquals(
				"JSON Schema not found for organizationName: 'my.org.edu' and schemaName: 'foo.bar.bar' and semanticVersion: '1.0.1'",
				message);
	}

	@Test
	public void testGetVersionInfoWithVersionNotFound() throws JSONObjectAdapterException {
		int index = 0;
		createNewSchemaVersion("my.org.edu-foo.bar-1.0.1", index++);
		String message = assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getVersionInfo("my.org.edu", "foo.bar", "1.0.2");
		}).getMessage();
		assertEquals(
				"JSON Schema not found for organizationName: 'my.org.edu' and schemaName: 'foo.bar' and semanticVersion: '1.0.2'",
				message);
	}

	@Test
	public void testGetVersionInfoWithVersionNullOrganization() throws JSONObjectAdapterException {
		String organizationName = null;
		String schemaName = "other.schema.name";
		String semanticVersion = "1.0.2";
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.getVersionInfo(organizationName, schemaName, semanticVersion);
		});
	}

	@Test
	public void testGetVersionInfoWithVersionNullSchema() throws JSONObjectAdapterException {
		String organizationName = "my.org.edu";
		String schemaName = null;
		String semanticVersion = "1.0.2";
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.getVersionInfo(organizationName, schemaName, semanticVersion);
		});
	}

	@Test
	public void testGetVersionInfoWithVersionNullVersion() throws JSONObjectAdapterException {
		String organizationName = "my.org.edu";
		String schemaName = "other.schema.name";
		String semanticVersion = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.getVersionInfo(organizationName, schemaName, semanticVersion);
		});
	}

	@Test
	public void testDeleteSchema() throws JSONObjectAdapterException {
		int index = 0;
		createNewSchemaVersion("my.org.edu-foo.bar-1.0.1", index++);
		createNewSchemaVersion("my.org.edu-foo.bar-1.0.2", index++);
		JsonSchemaVersionInfo stillExists = createNewSchemaVersion("other.org-foo.bar-1.0.2", index++);
		JsonSchemaVersionInfo lastInfo = createNewSchemaVersion("my.org.edu-foo.bar", index++);
		String schemaName = "foo.bar";
		String schemaId = jsonSchemaDao.getSchemaInfoForUpdate(lastInfo.getOrganizationId(), schemaName);
		// call under test
		jsonSchemaDao.deleteSchema(schemaId);
		assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getSchemaInfoForUpdate(organizationName, schemaName);
		});
		// should still exist
		assertEquals(stillExists, jsonSchemaDao.getVersionInfo(stillExists.getVersionId()));
	}

	@Test
	public void testDeleteSchemaNotFound() throws JSONObjectAdapterException {
		String schemaId = "-1";
		// call under test
		jsonSchemaDao.deleteSchema(schemaId);
	}

	@Test
	public void testDeleteSchemaNullId() throws JSONObjectAdapterException {
		String schemaId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.deleteSchema(schemaId);
		});
	}

	@Test
	public void testFindLatestVersion() throws JSONObjectAdapterException {
		int index = 0;
		JsonSchemaVersionInfo one = createNewSchemaVersion("my.org.edu-foo.bar-1.0.1", index++);
		JsonSchemaVersionInfo two = createNewSchemaVersion("my.org.edu-foo.bar-1.0.2", index++);
		JsonSchemaVersionInfo three = createNewSchemaVersion("my.org.edu-foo.bar", index++);
		JsonSchemaVersionInfo otherOrg = createNewSchemaVersion("another.org-foo.bar", index++);
		JsonSchemaVersionInfo otherName = createNewSchemaVersion("my.org.edu-other.name", index++);
		// Call under test
		Optional<Long> versionIdOptional = jsonSchemaDao.findLatestVersionId(one.getSchemaId());
		assertTrue(versionIdOptional.isPresent());
		assertEquals(three.getVersionId(), versionIdOptional.get().toString());
		// call under test
		versionIdOptional = jsonSchemaDao.findLatestVersionId(otherOrg.getSchemaId());
		assertTrue(versionIdOptional.isPresent());
		assertEquals(otherOrg.getVersionId(), versionIdOptional.get().toString());
		// call under test
		versionIdOptional = jsonSchemaDao.findLatestVersionId(otherName.getSchemaId());
		assertTrue(versionIdOptional.isPresent());
		assertEquals(otherName.getVersionId(), versionIdOptional.get().toString());
	}

	@Test
	public void testFindLatestVersionDoesNotExist() {
		String schemaId = "-1";
		Optional<Long> versionIdOptional = jsonSchemaDao.findLatestVersionId(schemaId);
		assertFalse(versionIdOptional.isPresent());
	}

	@Test
	public void testFindLatestVersionNullSchemaId() {
		String schemaId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.findLatestVersionId(schemaId);
		});
	}
	
	@Test
	public void testGetSchemaId() throws JSONObjectAdapterException {
		int index = 0;
		createNewSchemaVersion("my.org.edu-foo.bar-1.0.1", index++);
		JsonSchemaVersionInfo two = createNewSchemaVersion("my.org.edu-foo.bar", index++);
		createNewSchemaVersion("other.org.edu-foo.bar-1.0.1", index++);
		// call under test
		String schemaId = jsonSchemaDao.getSchemaId(two.getOrganizationName(), two.getSchemaName());
		assertEquals(two.getSchemaId(), schemaId);
	}
	
	@Test
	public void testGetSchemaIdWithNotFound() throws JSONObjectAdapterException {
		String organizationName = "some.org";
		String schemaName = "some.schema";
		String message = assertThrows(NotFoundException.class, ()->{
			// call under test
			jsonSchemaDao.getSchemaId(organizationName, schemaName);
		}).getMessage();
		assertEquals("JSON schema not found for organization name 'some.org' and schema name: 'some.schema'", message);
	}
	
	@Test
	public void testGetSchemaIdWithNullOrganizationName() throws JSONObjectAdapterException {
		String organizationName = null;
		String schemaName = "some.schema";
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			jsonSchemaDao.getSchemaId(organizationName, schemaName);
		});
	}
	
	@Test
	public void testGetSchemaIdWithNullSchemaName() throws JSONObjectAdapterException {
		String organizationName = "some.org";
		String schemaName = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			jsonSchemaDao.getSchemaId(organizationName, schemaName);
		});
	}

	@Test
	public void testGetSchemaIdForUpdate() throws JSONObjectAdapterException {
		int index = 0;
		JsonSchemaVersionInfo one = createNewSchemaVersion("my.org.edu-foo.bar-1.0.1", index++);
		// call under test
		String schemaId = jsonSchemaDao.getSchemaIdForUpdate(one.getVersionId());
		assertEquals(one.getSchemaId(), schemaId);
	}

	@Test
	public void testGetSchemaIdForUpdateNotFound() throws JSONObjectAdapterException {
		String versionId = "-1";
		String message = assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getSchemaIdForUpdate(versionId);
		}).getMessage();
		assertEquals("JSON schema not found for versionId: -1", message);
	}

	@Test
	public void testDeleteVersionNotLatest() throws JSONObjectAdapterException {
		int index = 0;
		JsonSchemaVersionInfo stillExists = createNewSchemaVersion("my.org.edu-foo.bar-1.0.1", index++);
		JsonSchemaVersionInfo toDelete = createNewSchemaVersion("my.org.edu-foo.bar-1.0.2", index++);
		createNewSchemaVersion("my.org.edu-foo.bar", index++);
		// call under test
		jsonSchemaDao.deleteSchemaVersion(toDelete.getVersionId());
		assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getVersionInfo(toDelete.getVersionId());
		});

		// this version should still exist
		JsonSchemaVersionInfo existResult = jsonSchemaDao.getVersionInfo(stillExists.getVersionId());
		assertEquals(stillExists, existResult);
		// the schema should still exist
		String schemaId = jsonSchemaDao.getSchemaInfoForUpdate(stillExists.getOrganizationId(),
				stillExists.getSchemaName());
		assertEquals(stillExists.getSchemaId(), schemaId);
	}

	@Test
	public void testDeleteVersionLatest() throws JSONObjectAdapterException {
		int index = 0;
		JsonSchemaVersionInfo stillExists = createNewSchemaVersion("my.org.edu-foo.bar-1.0.1", index++);
		JsonSchemaVersionInfo toDelete = createNewSchemaVersion("my.org.edu-foo.bar-1.0.2", index++);

		// call under test
		jsonSchemaDao.deleteSchemaVersion(toDelete.getVersionId());
		assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getVersionInfo("my.org.edu", "foo.bar", "1.0.2");
		});
		// the version that still exists should become the latest version.
		String latestVersionId = jsonSchemaDao.getLatestVersionId("my.org.edu", "foo.bar");
		assertEquals(stillExists.getVersionId(), latestVersionId);
		// the schema should still exist
		String schemaId = jsonSchemaDao.getSchemaInfoForUpdate(stillExists.getOrganizationId(),
				stillExists.getSchemaName());
		assertEquals(stillExists.getSchemaId(), schemaId);
	}
	
	@Test
	public void testDeleteVersionWithVersionBoundToObject() throws JSONObjectAdapterException {
		int index = 0;
		JsonSchemaVersionInfo one = createNewSchemaVersion("my.org.edu-foo.bar-1.0.1", index++);
		bindSchemaRequest.withSchemaId(one.getSchemaId());
		bindSchemaRequest.withVersionId(one.getVersionId());
		JsonSchemaObjectBinding result = jsonSchemaDao.bindSchemaToObject(bindSchemaRequest);
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			jsonSchemaDao.deleteSchemaVersion(one.getVersionId());
		});
		assertEquals("Cannot delete a schema version that is bound to an object", e.getMessage());
		assertTrue(e.getCause() instanceof DataIntegrityViolationException);
		jsonSchemaDao.clearBoundSchema(result.getObjectId(), result.getObjectType());
		// call under test
		jsonSchemaDao.deleteSchemaVersion(one.getVersionId());
	}
	
	@Test
	public void testDeleteVersionWithSchemaBoundToObject() throws JSONObjectAdapterException {
		int index = 0;
		JsonSchemaVersionInfo one = createNewSchemaVersion("my.org.edu-foo.bar", index++);
		bindSchemaRequest.withSchemaId(one.getSchemaId());
		bindSchemaRequest.withVersionId(null);
		JsonSchemaObjectBinding result = jsonSchemaDao.bindSchemaToObject(bindSchemaRequest);
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			jsonSchemaDao.deleteSchemaVersion(one.getVersionId());
		});
		assertEquals("Cannot delete a schema that is bound to an object", e.getMessage());
		assertTrue(e.getCause() instanceof DataIntegrityViolationException);
		jsonSchemaDao.clearBoundSchema(result.getObjectId(), result.getObjectType());
		// call under test
		jsonSchemaDao.deleteSchemaVersion(one.getVersionId());
	}
	
	@Test
	public void testDeleteSchemaWithVersionBoundToObject() throws JSONObjectAdapterException {
		int index = 0;
		JsonSchemaVersionInfo one = createNewSchemaVersion("my.org.edu-foo.bar-1.0.1", index++);
		bindSchemaRequest.withSchemaId(one.getSchemaId());
		bindSchemaRequest.withVersionId(one.getVersionId());
		JsonSchemaObjectBinding result = jsonSchemaDao.bindSchemaToObject(bindSchemaRequest);
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			jsonSchemaDao.deleteSchema(one.getSchemaId());
		});
		assertEquals("Cannot delete a schema that is bound to an object", e.getMessage());
		assertTrue(e.getCause() instanceof DataIntegrityViolationException);
		jsonSchemaDao.clearBoundSchema(result.getObjectId(), result.getObjectType());
		// call under test
		jsonSchemaDao.deleteSchema(one.getVersionId());
	}
	
	@Test
	public void testDeleteSchemaWithSchemaBoundToObject() throws JSONObjectAdapterException {
		int index = 0;
		JsonSchemaVersionInfo one = createNewSchemaVersion("my.org.edu-foo.bar", index++);
		bindSchemaRequest.withSchemaId(one.getSchemaId());
		bindSchemaRequest.withVersionId(null);
		JsonSchemaObjectBinding result = jsonSchemaDao.bindSchemaToObject(bindSchemaRequest);
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			jsonSchemaDao.deleteSchema(one.getSchemaId());
		});
		assertEquals("Cannot delete a schema that is bound to an object", e.getMessage());
		assertTrue(e.getCause() instanceof DataIntegrityViolationException);
		jsonSchemaDao.clearBoundSchema(result.getObjectId(), result.getObjectType());
		// call under test
		jsonSchemaDao.deleteSchema(one.getVersionId());
	}

	/**
	 * Case where the version to be deleted is the only version of that schema.
	 * 
	 * @throws JSONObjectAdapterException
	 */
	@Test
	public void testDeleteVersionOnlyVersion() throws JSONObjectAdapterException {
		int index = 0;
		JsonSchemaVersionInfo onlyVersion = createNewSchemaVersion("my.org.edu-foo.bar-1.0.1", index++);

		// call under test
		jsonSchemaDao.deleteSchemaVersion(onlyVersion.getVersionId());
		assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getVersionInfo("my.org.edu", "foo.bar", "1.0.2");
		});
		assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getSchemaInfoForUpdate(onlyVersion.getOrganizationId(), onlyVersion.getSchemaName());
		});
	}

	@Test
	public void testDeleteVersionNotFound() {
		String versionId = "-1";
		assertThrows(NotFoundException.class, () -> {
			// call under test
			jsonSchemaDao.deleteSchemaVersion(versionId);
		});
	}

	@Test
	public void testDeleteVersionNullVersionId() {
		String versionId = null;
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.deleteSchemaVersion(versionId);
		});
	}

	@Test
	public void testGetSchema() throws JSONObjectAdapterException {
		int index = 0;
		JsonSchemaVersionInfo info = createNewSchemaVersion("my.org.edu-foo.bar-1.0.1", index++);
		assertNotNull(info);
		JsonSchema schema = jsonSchemaDao.getSchema(info.getVersionId());
		assertNotNull(schema);
		assertEquals("my.org.edu-foo.bar-1.0.1", schema.get$id());
		assertEquals("index:0", schema.getDescription());
	}

	@Test
	public void testGetSchemaNotFound() {
		String versionId = "-1";
		String message = assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getSchema(versionId);
		}).getMessage();
		assertEquals("JSON Schema not found for versionId: '-1'", message);
	}

	@Test
	public void testGetSchemaNullId() {
		String versionId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.getSchema(versionId);
		});
	}

	@Test
	public void testPropertyOrderPreserved() {
		// Setup the schema with properties in a specific order
		LinkedHashMap<String, JsonSchema> properties = new LinkedHashMap<String, JsonSchema>(4);
		properties.put("one", createSchemaWithDescription("value of 1"));
		properties.put("two", createSchemaWithDescription("value of 2"));
		properties.put("three", createSchemaWithDescription("value of 3"));
		properties.put("four", createSchemaWithDescription("value of 4"));
		properties.put("five", createSchemaWithDescription("value of 5"));
		JsonSchema schemaWihtOrderedProperties = new JsonSchema();
		schemaWihtOrderedProperties.setProperties(properties);
		String schemaName = "preservePropertyOrder";
		schemaWihtOrderedProperties.set$id(organizationName + "/" + schemaName);
		// the has must not change
		String startSHA256 = new NormalizedJsonSchema(schemaWihtOrderedProperties).getSha256Hex();

		Organization organization = createOrganization(organizationName);
		// save the schema
		JsonSchemaVersionInfo resultInfo = jsonSchemaDao.createNewSchemaVersion(
				new NewSchemaVersionRequest().withCreatedBy(adminUserId).withJsonSchema(schemaWihtOrderedProperties)
						.withOrganizationId(organization.getId()).withSemanticVersion(null).withSchemaName(schemaName));
		// load the schema
		JsonSchema loaded = jsonSchemaDao.getSchema(resultInfo.getVersionId());
		assertNotNull(loaded);
		String loadedSHA256 = new NormalizedJsonSchema(loaded).getSha256Hex();
		assertEquals(startSHA256, loadedSHA256);
		List<String> keyOrder = loaded.getProperties().keySet().stream().collect(Collectors.toList());
		List<String> expectedKeyOrder = Lists.newArrayList("one", "two", "three", "four", "five");
		assertEquals(expectedKeyOrder, keyOrder);
	}

	/**
	 * Helper to create a schema with a type.string and the provided description.
	 * 
	 * @param description
	 * @return
	 */
	JsonSchema createSchemaWithDescription(String description) {
		JsonSchema schema = new JsonSchema();
		schema.setDescription("value of a");
		schema.setType(Type.string);
		return schema;
	}

	@Test
	public void testListSchemas() throws JSONObjectAdapterException {
		int index = 0;
		List<JsonSchemaVersionInfo> versions = new ArrayList<JsonSchemaVersionInfo>();
		versions.add(createNewSchemaVersion("other.org.edu-b", index++));
		versions.add(createNewSchemaVersion("my.org.edu-d", index++));
		versions.add(createNewSchemaVersion("my.org.edu-c", index++));
		versions.add(createNewSchemaVersion("my.org.edu-a", index++));
		versions.add(createNewSchemaVersion("my.org.edu-b", index++));

		long limit = 2;
		long offset = 1;
		String organizationName = "my.org.edu";
		// Call under test
		List<JsonSchemaInfo> page = jsonSchemaDao.listSchemas(organizationName, limit, offset);
		assertNotNull(page);
		assertEquals(2, page.size());
		assertInfoMatch(versions.get(2), page.get(0));
		assertInfoMatch(versions.get(3), page.get(1));
		
		Organization org = organizationDao.getOrganizationByName(organizationName);
		JsonSchemaInfo info = page.get(0);
		// See PLFM-6411.  The created on of the schema should not match the organization created on.
		assertFalse(org.getCreatedOn().equals(info.getCreatedOn()));
	}

	@Test
	public void testListSchemasWithNullName() throws JSONObjectAdapterException {
		String organizationName = null;
		long limit = 2;
		long offset = 1;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.listSchemas(organizationName, limit, offset);
		});
	}

	public void assertInfoMatch(JsonSchemaVersionInfo versionInfo, JsonSchemaInfo schemaInfo) {
		assertNotNull(versionInfo);
		assertNotNull(schemaInfo);
		assertEquals(versionInfo.getOrganizationId(), schemaInfo.getOrganizationId());
		assertEquals(versionInfo.getOrganizationName(), schemaInfo.getOrganizationName());
		assertEquals(versionInfo.getSchemaId(), schemaInfo.getSchemaId());
		assertEquals(versionInfo.getSchemaName(), schemaInfo.getSchemaName());
		assertNotNull(schemaInfo.getCreatedBy());
		assertNotNull(schemaInfo.getCreatedOn());
	}

	@Test
	public void testListSchemaVersions() throws JSONObjectAdapterException {
		int index = 0;
		List<JsonSchemaVersionInfo> versions = new ArrayList<JsonSchemaVersionInfo>();
		versions.add(createNewSchemaVersion("other.org.edu-b", index++));
		versions.add(createNewSchemaVersion("my.org.edu-c-1.0.0", index++));
		versions.add(createNewSchemaVersion("my.org.edu-b-1.0.0", index++));
		versions.add(createNewSchemaVersion("my.org.edu-b-1.0.1", index++));
		versions.add(createNewSchemaVersion("my.org.edu-b-1.0.2", index++));
		versions.add(createNewSchemaVersion("my.org.edu-b-1.0.3", index++));

		long limit = 2;
		long offset = 1;
		String organizationName = "my.org.edu";
		String schemaName = "b";
		// Call under test
		List<JsonSchemaVersionInfo> page = jsonSchemaDao.listSchemaVersions(organizationName, schemaName, limit,
				offset);
		assertNotNull(page);
		assertEquals(2, page.size());
		assertEquals(versions.get(3), page.get(0));
		assertEquals(versions.get(4), page.get(1));
	}

	@Test
	public void testListSchemaVersionsWithNullOrganizationName() {
		long limit = 2;
		long offset = 1;
		String organizationName = null;
		String schemaName = "b";
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.listSchemaVersions(organizationName, schemaName, limit, offset);
		});
	}

	@Test
	public void testListSchemaVersionsWithNullSchemaName() {
		long limit = 2;
		long offset = 1;
		String organizationName = "my.org.edu";
		String schemaName = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.listSchemaVersions(organizationName, schemaName, limit, offset);
		});
	}

	@Test
	public void testBindDependencies() throws JSONObjectAdapterException {
		int index = 0;
		JsonSchemaVersionInfo one = createNewSchemaVersion("my.org.edu-one", index++);
		// two depends on one
		ArrayList<SchemaDependency> dependencies = new ArrayList<SchemaDependency>();
		dependencies.add(new SchemaDependency().withDependsOnSchemaId(one.getSchemaId()));
		JsonSchemaVersionInfo two = createNewSchemaVersion("my.org.edu-two", index++, dependencies);
		// Three depends on one and two
		dependencies = new ArrayList<SchemaDependency>();
		dependencies.add(new SchemaDependency().withDependsOnSchemaId(one.getSchemaId()));
		dependencies.add(new SchemaDependency().withDependsOnSchemaId(two.getSchemaId()));
		JsonSchemaVersionInfo three = createNewSchemaVersion("my.org.edu-three", index++, dependencies);

		// Should block delete of one and two
		String message = assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.deleteSchema(one.getSchemaId());
		}).getMessage();
		assertEquals("Cannot delete a schema that is referenced by another schema", message);

		message = assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.deleteSchema(two.getSchemaId());
		}).getMessage();
		assertEquals("Cannot delete a schema that is referenced by another schema", message);

		// can delete three
		jsonSchemaDao.deleteSchema(three.getSchemaId());
		// can now delete two
		jsonSchemaDao.deleteSchema(two.getSchemaId());
		// can now delete one
		jsonSchemaDao.deleteSchema(one.getSchemaId());
	}

	@Test
	public void testBindDependenciesWithVersions() throws JSONObjectAdapterException {
		int index = 0;
		JsonSchemaVersionInfo one = createNewSchemaVersion("my.org.edu-one-1.0.0", index++);
		// two depends on one
		ArrayList<SchemaDependency> dependencies = new ArrayList<SchemaDependency>();
		dependencies.add(new SchemaDependency().withDependsOnSchemaId(one.getSchemaId())
				.withDependsOnVersionId(one.getVersionId()));
		JsonSchemaVersionInfo two = createNewSchemaVersion("my.org.edu-two-1.0.0", index++, dependencies);
		// Three depends on one and two
		dependencies = new ArrayList<SchemaDependency>();
		dependencies.add(new SchemaDependency().withDependsOnSchemaId(one.getSchemaId())
				.withDependsOnVersionId(one.getVersionId()));
		dependencies.add(new SchemaDependency().withDependsOnSchemaId(two.getSchemaId())
				.withDependsOnVersionId(two.getVersionId()));
		// call under test
		JsonSchemaVersionInfo three = createNewSchemaVersion("my.org.edu-three-1.0.0", index++, dependencies);

		List<DBOJsonSchemaDependency> dependencyResults = jsonSchemaDao.getDependencies(three.getVersionId());
		assertNotNull(dependencyResults);
		assertEquals(2, dependencyResults.size());

		// Should block delete of one and two
		String message = assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.deleteSchema(one.getSchemaId());
		}).getMessage();
		assertEquals("Cannot delete a schema that is referenced by another schema", message);
		message = assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.deleteSchemaVersion(one.getVersionId());
		}).getMessage();
		assertEquals("Cannot delete a schema version that is referenced by another schema", message);

		// can delete three
		jsonSchemaDao.deleteSchema(three.getSchemaId());
		// can now delete two
		jsonSchemaDao.deleteSchema(two.getSchemaId());
		// can now delete one
		jsonSchemaDao.deleteSchema(one.getSchemaId());
	}

	@Test
	public void testBindDependenciesWithSameSchemaWithAndWithoutVersion() throws JSONObjectAdapterException {
		int index = 0;
		JsonSchemaVersionInfo one = createNewSchemaVersion("my.org.edu-one-1.0.0", index++);
		// two depends on one
		ArrayList<SchemaDependency> dependencies = new ArrayList<SchemaDependency>();
		// one with a version
		dependencies.add(new SchemaDependency().withDependsOnSchemaId(one.getSchemaId())
				.withDependsOnVersionId(one.getVersionId()));
		// one without a version
		dependencies.add(new SchemaDependency().withDependsOnSchemaId(one.getSchemaId()));
		JsonSchemaVersionInfo two = createNewSchemaVersion("my.org.edu-two-1.0.0", index++, dependencies);

		// Should block delete of one and two
		String message = assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.deleteSchema(one.getSchemaId());
		}).getMessage();
		assertEquals("Cannot delete a schema that is referenced by another schema", message);
		message = assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.deleteSchemaVersion(one.getVersionId());
		}).getMessage();
		assertEquals("Cannot delete a schema version that is referenced by another schema", message);
		// can now delete two
		jsonSchemaDao.deleteSchema(two.getSchemaId());
		// can now delete one
		jsonSchemaDao.deleteSchema(one.getSchemaId());
	}

	@Test
	public void testBindDependenciesWithSameSchemaWithDuplicate() throws JSONObjectAdapterException {
		int index = 0;
		JsonSchemaVersionInfo one = createNewSchemaVersion("my.org.edu-one-1.0.0", index++);
		// two depends on one
		ArrayList<SchemaDependency> dependencies = new ArrayList<SchemaDependency>();
		// one with a version
		dependencies.add(new SchemaDependency().withDependsOnSchemaId(one.getSchemaId())
				.withDependsOnVersionId(one.getVersionId()));
		// same binding twice
		dependencies.add(new SchemaDependency().withDependsOnSchemaId(one.getSchemaId())
				.withDependsOnVersionId(one.getVersionId()));
		JsonSchemaVersionInfo two = createNewSchemaVersion("my.org.edu-two-1.0.0", index++, dependencies);

		// Should block delete of one and two
		String message = assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.deleteSchema(one.getSchemaId());
		}).getMessage();
		assertEquals("Cannot delete a schema that is referenced by another schema", message);
		message = assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.deleteSchemaVersion(one.getVersionId());
		}).getMessage();
		assertEquals("Cannot delete a schema version that is referenced by another schema", message);
		// can now delete two
		jsonSchemaDao.deleteSchema(two.getSchemaId());
		// can now delete one
		jsonSchemaDao.deleteSchema(one.getSchemaId());
	}

	@Test
	public void testBindDependenciesWithNullDependencies() throws JSONObjectAdapterException {
		ArrayList<SchemaDependency> dependencies = null;
		String versionId = "123";
		// call under test
		jsonSchemaDao.bindDependencies(versionId, dependencies);
	}

	@Test
	public void testBindDependenciesWithEmptyDependencies() throws JSONObjectAdapterException {
		ArrayList<SchemaDependency> dependencies = new ArrayList<SchemaDependency>();
		String versionId = "123";
		// call under test
		jsonSchemaDao.bindDependencies(versionId, dependencies);
	}

	@Test
	public void testBindDependenciesWithNullVersionId() throws JSONObjectAdapterException {
		ArrayList<SchemaDependency> dependencies = new ArrayList<SchemaDependency>();
		String versionId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			jsonSchemaDao.bindDependencies(versionId, dependencies);
		});
	}

	@Test
	public void testGetDependencies() throws JSONObjectAdapterException {
		int index = 0;
		JsonSchemaVersionInfo one = createNewSchemaVersion("my.org.edu-one-1.0.0", index++);
		// two depends on one
		ArrayList<SchemaDependency> dependencies = new ArrayList<SchemaDependency>();
		dependencies.add(new SchemaDependency().withDependsOnSchemaId(one.getSchemaId())
				.withDependsOnVersionId(one.getVersionId()));
		JsonSchemaVersionInfo two = createNewSchemaVersion("my.org.edu-two-1.0.0", index++, dependencies);

		// call under test
		List<DBOJsonSchemaDependency> result = jsonSchemaDao.getDependencies(two.getVersionId());
		assertNotNull(result);
		assertEquals(1, result.size());
		DBOJsonSchemaDependency dbo = result.get(0);
		assertNotNull(dbo.getDependsOnSchemaId());
		assertEquals(one.getSchemaId(), dbo.getDependsOnSchemaId().toString());
		assertNotNull(dbo.getVersionId());
		assertEquals(one.getVersionId(), dbo.getDependsOnVersionId().toString());
	}

	@Test
	public void testGetDependenciesWithNullVersionId() throws JSONObjectAdapterException {
		int index = 0;
		JsonSchemaVersionInfo one = createNewSchemaVersion("my.org.edu-one-1.0.0", index++);
		// two depends on one with a null version Id
		ArrayList<SchemaDependency> dependencies = new ArrayList<SchemaDependency>();
		dependencies.add(new SchemaDependency().withDependsOnSchemaId(one.getSchemaId()).withDependsOnVersionId(null));
		JsonSchemaVersionInfo two = createNewSchemaVersion("my.org.edu-two-1.0.0", index++, dependencies);

		// call under test
		List<DBOJsonSchemaDependency> result = jsonSchemaDao.getDependencies(two.getVersionId());
		assertNotNull(result);
		assertEquals(1, result.size());
		DBOJsonSchemaDependency dbo = result.get(0);
		assertNotNull(dbo.getDependsOnSchemaId());
		assertEquals(one.getSchemaId(), dbo.getDependsOnSchemaId().toString());
		assertNull(dbo.getDependsOnVersionId());
	}

	@Test
	public void testBindSchemaToObject() throws JSONObjectAdapterException {
		int index = 0;
		JsonSchemaVersionInfo one = createNewSchemaVersion("my.org.edu-one-1.0.0", index++);
		JsonSchemaVersionInfo two = createNewSchemaVersion("my.org.edu-one-2.0.0", index++);
		bindSchemaRequest.withSchemaId(one.getSchemaId());
		bindSchemaRequest.withVersionId(one.getVersionId());
		// call under test
		JsonSchemaObjectBinding result = jsonSchemaDao.bindSchemaToObject(bindSchemaRequest);
		assertNotNull(result);
		assertEquals(one, result.getJsonSchemaVersionInfo());
		assertEquals(adminUserId.toString(), result.getCreatedBy());
		assertNotNull(result.getCreatedOn());
		assertEquals(bindSchemaRequest.getObjectId(), result.getObjectId());
		assertEquals(bindSchemaRequest.getObjectType(), result.getObjectType());

		// should be able to bind the object to another version
		bindSchemaRequest.withSchemaId(two.getSchemaId());
		bindSchemaRequest.withVersionId(two.getVersionId());
		// call under test
		result = jsonSchemaDao.bindSchemaToObject(bindSchemaRequest);
		assertNotNull(result);
		assertEquals(two, result.getJsonSchemaVersionInfo());
		assertEquals(adminUserId.toString(), result.getCreatedBy());
		assertNotNull(result.getCreatedOn());
		assertEquals(bindSchemaRequest.getObjectId(), result.getObjectId());
		assertEquals(bindSchemaRequest.getObjectType(), result.getObjectType());
	}

	@Test
	public void testBindSchemaToObjectWithNullVersionId() throws JSONObjectAdapterException {
		int index = 0;
		JsonSchemaVersionInfo one = createNewSchemaVersion("my.org.edu-one", index++);
		JsonSchemaVersionInfo two = createNewSchemaVersion("my.org.edu-one", index++);
		bindSchemaRequest.withSchemaId(one.getSchemaId());
		bindSchemaRequest.withVersionId(null);
		// call under test
		JsonSchemaObjectBinding result = jsonSchemaDao.bindSchemaToObject(bindSchemaRequest);
		assertNotNull(result);
		assertEquals(two, result.getJsonSchemaVersionInfo());
		assertEquals(adminUserId.toString(), result.getCreatedBy());
		assertNotNull(result.getCreatedOn());
		assertEquals(bindSchemaRequest.getObjectId(), result.getObjectId());
		assertEquals(bindSchemaRequest.getObjectType(), result.getObjectType());

	}

	@Test
	public void testBindSchemaToObjectWithNullRequest() throws JSONObjectAdapterException {
		bindSchemaRequest = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			jsonSchemaDao.bindSchemaToObject(bindSchemaRequest);
		});
	}

	@Test
	public void testBindSchemaToObjectWithNullCreatedBy() throws JSONObjectAdapterException {
		bindSchemaRequest.withCreatedBy(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			jsonSchemaDao.bindSchemaToObject(bindSchemaRequest);
		});
	}

	@Test
	public void testBindSchemaToObjectWithNullObjectId() throws JSONObjectAdapterException {
		bindSchemaRequest.withObjectId(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			jsonSchemaDao.bindSchemaToObject(bindSchemaRequest);
		});
	}

	@Test
	public void testBindSchemaToObjectWithNullObjectType() throws JSONObjectAdapterException {
		bindSchemaRequest.withObjectType(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			jsonSchemaDao.bindSchemaToObject(bindSchemaRequest);
		});
	}

	@Test
	public void testBindSchemaToObjectWithNullSchemaId() throws JSONObjectAdapterException {
		bindSchemaRequest.withSchemaId(null);
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			jsonSchemaDao.bindSchemaToObject(bindSchemaRequest);
		});
	}

	@Test
	public void testGetSchemaBindingForObject() throws JSONObjectAdapterException {
		int index = 0;
		JsonSchemaVersionInfo one = createNewSchemaVersion("my.org.edu-one-1.0.0", index++);
		bindSchemaRequest.withSchemaId(one.getSchemaId());
		bindSchemaRequest.withVersionId(one.getVersionId());
		JsonSchemaObjectBinding bindOne = jsonSchemaDao.bindSchemaToObject(bindSchemaRequest);

		JsonSchemaVersionInfo two = createNewSchemaVersion("my.org.edu-two-1.0.0", index++);
		bindSchemaRequest.withSchemaId(two.getSchemaId());
		bindSchemaRequest.withVersionId(two.getVersionId());
		bindSchemaRequest.withObjectId(456L);
		JsonSchemaObjectBinding bindTwo = jsonSchemaDao.bindSchemaToObject(bindSchemaRequest);

		// call under test
		JsonSchemaObjectBinding result = jsonSchemaDao.getSchemaBindingForObject(bindSchemaRequest.getObjectId(),
				bindSchemaRequest.getObjectType());
		assertEquals(bindTwo, result);
	}

	@Test
	public void testGetSchemaBindingForObjectWithNullVersionId() throws JSONObjectAdapterException {
		int index = 0;
		JsonSchemaVersionInfo one = createNewSchemaVersion("my.org.edu-one-1.0.0", index++);
		bindSchemaRequest.withSchemaId(one.getSchemaId());
		bindSchemaRequest.withVersionId(null);
		JsonSchemaObjectBinding bindOne = jsonSchemaDao.bindSchemaToObject(bindSchemaRequest);

		// call under test
		JsonSchemaObjectBinding result = jsonSchemaDao.getSchemaBindingForObject(bindSchemaRequest.getObjectId(),
				bindSchemaRequest.getObjectType());
		assertEquals(bindOne, result);
	}

	@Test
	public void testGetSchemaBindingForObjectWithNotFound() throws JSONObjectAdapterException {
		Long objectId = -1L;
		BoundObjectType type = BoundObjectType.entity;

		String message = assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getSchemaBindingForObject(objectId, type);
		}).getMessage();
		assertEquals("JSON Schema binding was not found for ObjectId: '-1' ObjectType: 'entity'", message);
	}
	
	@Test
	public void testGetSchemaBindingForObjectWithNullObjectId() throws JSONObjectAdapterException {
		Long objectId = null;
		BoundObjectType type = BoundObjectType.entity;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.getSchemaBindingForObject(objectId, type);
		});
	}
	
	@Test
	public void testGetSchemaBindingForObjectWithNullObjectType() throws JSONObjectAdapterException {
		Long objectId = -1L;
		BoundObjectType type = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.getSchemaBindingForObject(objectId, type);
		});
	}
	
	@Test
	public void testClearBoundSchema() throws JSONObjectAdapterException {
		int index = 0;
		JsonSchemaVersionInfo one = createNewSchemaVersion("my.org.edu-one-1.0.0", index++);
		bindSchemaRequest.withSchemaId(one.getSchemaId());
		bindSchemaRequest.withVersionId(null);
		jsonSchemaDao.bindSchemaToObject(bindSchemaRequest);
		JsonSchemaObjectBinding binding = jsonSchemaDao.getSchemaBindingForObject(bindSchemaRequest.getObjectId(),
				bindSchemaRequest.getObjectType());
		assertNotNull(binding);
		// call under test
		jsonSchemaDao.clearBoundSchema(binding.getObjectId(), binding.getObjectType());
		assertThrows(NotFoundException.class, ()->{
			jsonSchemaDao.getSchemaBindingForObject(bindSchemaRequest.getObjectId(),
					bindSchemaRequest.getObjectType());
		});
	}
	
	@Test
	public void testClearBoundSchemaWithNullObjectId() throws JSONObjectAdapterException {
		Long objectId = null;
		BoundObjectType objectType = BoundObjectType.entity;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			jsonSchemaDao.clearBoundSchema(objectId, objectType);
		});
	}
	
	@Test
	public void testClearBoundSchemaWithNullObjectType() throws JSONObjectAdapterException {
		Long objectId = 123L;
		BoundObjectType objectType = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			jsonSchemaDao.clearBoundSchema(objectId, objectType);
		});
	}
}
