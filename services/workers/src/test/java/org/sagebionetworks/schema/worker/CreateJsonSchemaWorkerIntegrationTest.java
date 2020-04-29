package org.sagebionetworks.schema.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class CreateJsonSchemaWorkerIntegrationTest {

	public static final long MAX_WAIT_MS = 1000 * 30;

	@Autowired
	AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;

	@Autowired
	JsonSchemaManager jsonSchemaManager;

	@Autowired
	UserManager userManager;

	UserInfo adminUserInfo;
	String organizationName;
	String schemaName;
	String semanticVersion;
	JsonSchema basicSchema;
	Organization organization;

	@BeforeEach
	public void before() {
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		organizationName = "my.org.net";
		schemaName = "some.schema";
		semanticVersion = "1.1.1";
		CreateOrganizationRequest createOrgRequest = new CreateOrganizationRequest();
		createOrgRequest.setOrganizationName(organizationName);
		organization = jsonSchemaManager.createOrganziation(adminUserInfo, createOrgRequest);
		basicSchema = new JsonSchema();
		basicSchema.set$id(organizationName + "/" + schemaName + "/" + semanticVersion);
		basicSchema.setDescription("basic schema for integration test");
	}

	@AfterEach
	public void after() {
		jsonSchemaManager.truncateAll();
	}

	@Test
	public void testCreateSchema() throws InterruptedException {
		CreateSchemaRequest request = new CreateSchemaRequest();
		request.setSchema(basicSchema);
		CreateSchemaResponse response = asynchronousJobWorkerHelper.startAndWaitForJob(adminUserInfo, request,
				MAX_WAIT_MS, CreateSchemaResponse.class);
		assertNotNull(response);
		assertNotNull(response.getNewVersionInfo());
		assertEquals(adminUserInfo.getId().toString(), response.getNewVersionInfo().getCreatedBy());
		assertEquals(semanticVersion, response.getNewVersionInfo().getSemanticVersion());
		jsonSchemaManager.deleteSchemaAllVersion(adminUserInfo, organizationName, schemaName);
		assertThrows(NotFoundException.class, ()->{
			jsonSchemaManager.deleteSchemaAllVersion(adminUserInfo, organizationName, schemaName);
		});
	}
}
