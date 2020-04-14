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
		schemaInfo.setName("path/Name");
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
}
