package org.sagebionetworks.repo.model.dbo.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class ValidationJsonSchemaIndexDaoImplTest {
	
	@Autowired
	ValidationJsonSchemaIndexDao validationDao;
	
	@Autowired
	JsonSchemaDao jsonSchemaDao;
	
	@Autowired
	OrganizationDao organizationDao;
	
	String versionId;
	JsonSchema schema;
	String organizationName;
	String organizationId;
	Long creator;
	Organization org;
	String schemaName;
	
	@BeforeEach
	public void before() {
		validationDao.truncateAll();
		jsonSchemaDao.truncateAll();
		organizationDao.truncateAll();
		organizationName = "testOrg";
		schemaName = "testName";
		creator = 2L;
		schema = new JsonSchema();
		schema.set_const("foo");
		// create new organization
		org = organizationDao.createOrganization(organizationName, creator);
		organizationId = org.getId();
		// add to the JSON_SCHEMA_VERSION table
		NewSchemaVersionRequest request = new NewSchemaVersionRequest();
		request = request.withOrganizationId(organizationId).withCreatedBy(creator)
				.withSchemaName(schemaName).withJsonSchema(schema);
		JsonSchemaVersionInfo info = jsonSchemaDao.createNewSchemaVersion(request);
		versionId = info.getVersionId();
	}
	
	@AfterEach
	public void after() {
		validationDao.truncateAll();
		jsonSchemaDao.truncateAll();
		organizationDao.truncateAll();
	}
	
	@Test
	public void testBasicCRUD() {
		// create the validation schema for versionId
		validationDao.createOrUpdate(versionId, schema);
		
		// get the schema for the versionId
		JsonSchema resultSchema = validationDao.getValidationSchema(versionId);
		assertEquals(resultSchema.get_const(), schema.get_const());
		
		// update the versionId to a different schema
		JsonSchema newSchema = new JsonSchema();
		newSchema.set_const("bar");
		validationDao.createOrUpdate(versionId, newSchema);
		resultSchema = validationDao.getValidationSchema(versionId);
		assertNotEquals(resultSchema.get_const(), schema.get_const());
		
		// delete the schema for the id
		validationDao.delete(versionId);
		assertThrows(NotFoundException.class, () -> {
			validationDao.getValidationSchema(versionId);
		});
	}
	
	@Test
	public void testGetValidationSchemaWithNotFound() {
		assertThrows(NotFoundException.class, () -> {
			validationDao.getValidationSchema("randomId");
		});
	}
}
