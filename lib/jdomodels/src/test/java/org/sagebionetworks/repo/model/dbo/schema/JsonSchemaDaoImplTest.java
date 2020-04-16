package org.sagebionetworks.repo.model.dbo.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

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

		schemaInfo = new SchemaInfo();
		schemaInfo.setOrganizationId("Foo.Org");
		schemaInfo.setName("path.Name");
		schemaInfo.setOrganizationId(organization.getId());
		schemaInfo.setCreatedBy(adminUserId.toString());
		schemaInfo.setCreatedOn(new Date());
		semanticVersion = "1.2.3";

		schema = new JsonSchema();
		schema.set$id(schemaInfo.getOrganizationId() + "/" + schemaInfo.getName());
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
		SchemaInfo result = jsonSchemaDao.createSchemaIfDoesNotExist(schemaInfo);
		assertNotNull(result);
		assertNotNull(result.getNumericId());
		assertEquals(schemaInfo.getName(), result.getName());
		assertEquals(schemaInfo.getOrganizationId(), result.getOrganizationId());
		assertEquals(schemaInfo.getCreatedBy(), result.getCreatedBy());
		assertEquals(schemaInfo.getCreatedOn(), result.getCreatedOn());
	}

	@Test
	public void testCreateSchemaIfDoesNotExistDuplicate() {
		long now = System.currentTimeMillis();
		schemaInfo.setCreatedOn(new Date(now));
		// call under test
		SchemaInfo first = jsonSchemaDao.createSchemaIfDoesNotExist(schemaInfo);
		// this createdOn should be ignored since the row already exists.
		schemaInfo.setCreatedOn(new Date(now + 1));
		SchemaInfo second = jsonSchemaDao.createSchemaIfDoesNotExist(schemaInfo);
		assertEquals(first, second);
	}

	@Test
	public void testCreateSchemaIfDoesNotExistNull() {
		schemaInfo = null;
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createSchemaIfDoesNotExist(schemaInfo);
		});
	}

	@Test
	public void testCreateSchemaIfDoesNotExistNullOrganization() {
		schemaInfo.setOrganizationId(null);
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createSchemaIfDoesNotExist(schemaInfo);
		});
	}

	@Test
	public void testCreateSchemaIfDoesNotExistNullName() {
		schemaInfo.setName(null);
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createSchemaIfDoesNotExist(schemaInfo);
		});
	}

	@Test
	public void testCreateSchemaIfDoesNotExistNullCreatedBy() {
		schemaInfo.setCreatedBy(null);
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createSchemaIfDoesNotExist(schemaInfo);
		});
	}

	@Test
	public void testCreateSchemaIfDoesNotExistNullCreatedOn() {
		schemaInfo.setCreatedOn(null);
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.createSchemaIfDoesNotExist(schemaInfo);
		});
	}

	@Test
	public void testGetSchemaInfoForUpdate() {
		SchemaInfo result = jsonSchemaDao.createSchemaIfDoesNotExist(schemaInfo);
		assertNotNull(result);
		// call under test
		SchemaInfo fromGet = jsonSchemaDao.getSchemaInfoForUpdate(schemaInfo.getOrganizationId(), schemaInfo.getName());
		assertEquals(result, fromGet);
	}

	@Test
	public void testGetSchemaInfoForUpdateNullOrganizationId() {
		schemaInfo.setOrganizationId(null);
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.getSchemaInfoForUpdate(schemaInfo.getOrganizationId(), schemaInfo.getName());
		});
	}

	@Test
	public void testGetSchemaInfoForUpdateNullName() {
		schemaInfo.setName(null);
		assertThrows(IllegalArgumentException.class, () -> {
			jsonSchemaDao.getSchemaInfoForUpdate(schemaInfo.getOrganizationId(), schemaInfo.getName());
		});
	}

	@Test
	public void testGetSchemaInfoForUpdateNotFound() {
		schemaInfo.setOrganizationId("-1");
		String message = assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getSchemaInfoForUpdate(schemaInfo.getOrganizationId(), schemaInfo.getName());
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

	@Test
	public void testCreateNewVersion() {
		NewVersionRequest request = setupValidNewVersionRequest();

		// call under test
		String versionId = jsonSchemaDao.createNewVersion(request);
		assertNotNull(versionId);
		JsonSchemaVersionInfo info = jsonSchemaDao.getVersionInfo(versionId);
		assertNotNull(info);
		assertEquals(versionId, info.getVersionId());
		assertEquals(request.getCreatedBy().toString(), info.getCreatedBy());
		assertEquals(request.getCreatedOn(), info.getCreatedOn());
		assertEquals(schemaJsonSha256Hex, info.getJsonSHA256Hex());
		assertEquals(semanticVersion, info.getSemanticVersion());
	}

	public NewVersionRequest setupValidNewVersionRequest() {
		schemaInfo = jsonSchemaDao.createSchemaIfDoesNotExist(schemaInfo);
		assertNotNull(schemaInfo);
		String blobId = jsonSchemaDao.createJsonBlobIfDoesNotExist(schemaJson, schemaJsonSha256Hex);
		return new NewVersionRequest().withSchemaId(schemaInfo.getNumericId()).withSemanticVersion(semanticVersion)
				.withCreatedBy(adminUserId).withCreatedOn(schemaInfo.getCreatedOn()).withBlobId(blobId);
	}

}
