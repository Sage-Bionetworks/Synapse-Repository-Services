package org.sagebionetworks.repo.model.dbo.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.schema.SchemaInfo;
import org.sagebionetworks.repo.web.NotFoundException;
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

	Organization organization;
	String organizationName;
	private SchemaInfo schemaInfo;

	private Long adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();

	@BeforeEach
	public void before() {
		organizationName = "Foo.baR";
		organization = organizationDao.createOrganization(organizationName, adminUserId);

		schemaInfo = new SchemaInfo();
		schemaInfo.setOrganizationId("Foo.Org");
		schemaInfo.setName("path.Name");
		schemaInfo.setOrganizationId(organization.getId());
		schemaInfo.setCreatedBy(adminUserId.toString());
		schemaInfo.setCreatedOn(new Date());
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
	public void testGetSchemaInfoForUpdate() {
		SchemaInfo result = jsonSchemaDao.createSchemaIfDoesNotExist(schemaInfo);
		assertNotNull(result);
		SchemaInfo fromGet = jsonSchemaDao.getSchemaInfoForUpdate(schemaInfo.getOrganizationId(), schemaInfo.getName());
		assertEquals(result, fromGet);
	}

	@Test
	public void testGetSchemaInfoForUpdateNotFound() {
		schemaInfo.setOrganizationId("-1");
		String message = assertThrows(NotFoundException.class, () -> {
			jsonSchemaDao.getSchemaInfoForUpdate(schemaInfo.getOrganizationId(), schemaInfo.getName());
		}).getMessage();
		assertEquals("JsonSchema not found for organizationId: '-1' and schemaName: 'path.Name'", message);
	}
}
