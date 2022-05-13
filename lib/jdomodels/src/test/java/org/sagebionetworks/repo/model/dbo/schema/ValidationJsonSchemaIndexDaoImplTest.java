package org.sagebionetworks.repo.model.dbo.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
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
	private ValidationJsonSchemaIndexDao validationIndexDao;
	
	@Autowired
	private JsonSchemaDao jsonSchemaDao;
	
	@Autowired
	private OrganizationDao organizationDao;
	
	@Autowired
	private JsonSchemaTestHelper jsonSchemaTestHelper;
	
	private JsonSchema schema;
	private String organizationName;
	private String organizationId;
	private Long createdBy;
	private Organization organization;
	private String schemaName;
	private String semanticVersion;
	private NewSchemaVersionRequest newSchemaVersionRequest;
	private Long adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
	
	@BeforeEach
	public void before() throws Exception {
		validationIndexDao.truncateAll();
		jsonSchemaTestHelper.truncateAll();
		createdBy = adminUserId;
		organizationName = "Foo.baR";
		organization = organizationDao.createOrganization(organizationName, adminUserId);
		organizationId = organization.getId();
		schemaName = "path.Name";
		semanticVersion = "1.2.3";
		schema = new JsonSchema();
		schema.set_const("foo");
		schema.set$id(organizationName + "-" + schemaName);

		newSchemaVersionRequest = new NewSchemaVersionRequest().withCreatedBy(createdBy)
				.withOrganizationId(organizationId).withJsonSchema(schema).withSchemaName(schemaName)
				.withSemanticVersion(semanticVersion);
	}
	
	@AfterEach
	public void after() {
		validationIndexDao.truncateAll();
		jsonSchemaTestHelper.truncateAll();
	}
	
	@Test
	public void testBasicCRUD() {
		// create the new Json schema
		JsonSchemaVersionInfo info = jsonSchemaDao.createNewSchemaVersion(newSchemaVersionRequest);
		String versionId = info.getVersionId();
		
		// create the validation schema for versionId
		validationIndexDao.createOrUpdate(versionId, schema);
		
		// get the schema for the versionId
		JsonSchema resultSchema = validationIndexDao.getValidationSchema(versionId);
		assertEquals(resultSchema.get_const(), schema.get_const());
		
		// update the versionId to a different schema
		JsonSchema newSchema = new JsonSchema();
		newSchema.set_const("bar");
		validationIndexDao.createOrUpdate(versionId, newSchema);
		resultSchema = validationIndexDao.getValidationSchema(versionId);
		assertNotEquals(resultSchema.get_const(), schema.get_const());
		
		// delete the schema for the id
		validationIndexDao.delete(versionId);
		assertThrows(NotFoundException.class, () -> {
			validationIndexDao.getValidationSchema(versionId);
		});
	}
	
	@Test
	public void testGetValidationSchemaWithNotFound() {
		String message = assertThrows(NotFoundException.class, () -> {
			validationIndexDao.getValidationSchema("randomId");
		}).getMessage();
		assertEquals("Validation schema for version: 'randomId' does not exist", message);
	}
}
