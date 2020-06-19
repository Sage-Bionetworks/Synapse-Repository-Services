package org.sagebionetworks.schema.worker;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.manager.schema.SynapseSchemaBootstrap;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.JsonSchemaConstants;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
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
	private SynapseSchemaBootstrap schemaBootstrap;

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
		basicSchema.set$id(organizationName + JsonSchemaConstants.PATH_DELIMITER + schemaName
				+ JsonSchemaConstants.VERSION_PRFIX + semanticVersion);
		basicSchema.setDescription("basic schema for integration test");
	}

	@AfterEach
	public void after() {
		jsonSchemaManager.truncateAll();
	}

	@Test
	public void testCreateSchema() throws InterruptedException, AssertionError, AsynchJobFailedException {
		CreateSchemaRequest request = new CreateSchemaRequest();
		request.setSchema(basicSchema);
		
		asynchronousJobWorkerHelper.assertJobResponse(adminUserInfo, request, (CreateSchemaResponse response) -> {
			assertNotNull(response);
			assertNotNull(response.getNewVersionInfo());
			assertEquals(adminUserInfo.getId().toString(), response.getNewVersionInfo().getCreatedBy());
			assertEquals(semanticVersion, response.getNewVersionInfo().getSemanticVersion());
		}, MAX_WAIT_MS);
		
		jsonSchemaManager.deleteSchemaById(adminUserInfo, basicSchema.get$id());
		assertThrows(NotFoundException.class, () -> {
			jsonSchemaManager.deleteSchemaById(adminUserInfo, basicSchema.get$id());
		});
	}

	@Test
	public void testCreateSchemaCycle() throws InterruptedException, AssertionError, AsynchJobFailedException {
		// one
		JsonSchema one = createSchema(organizationName, "one");
		one.setDescription("no cycle yet");
		CreateSchemaRequest request = new CreateSchemaRequest();
		request.setSchema(one);
		
		asynchronousJobWorkerHelper.assertJobResponse(adminUserInfo, request, (CreateSchemaResponse response) -> {
			assertNotNull(response);
		}, MAX_WAIT_MS);
		
		// two
		JsonSchema refToOne = create$RefSchema(one);
		JsonSchema two = createSchema(organizationName, "two");
		two.setDescription("depends on one");
		two.setItems(refToOne);
		request = new CreateSchemaRequest();
		request.setSchema(two);
		
		asynchronousJobWorkerHelper.assertJobResponse(adminUserInfo, request, (CreateSchemaResponse response) -> {
			assertNotNull(response);
		}, MAX_WAIT_MS);
		
		// update one to depend on two
		one.setItems(create$RefSchema(two));
		one.setDescription("now has a cycle");
		CreateSchemaRequest cycleRequest = new CreateSchemaRequest();
		cycleRequest.setSchema(one);
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			asynchronousJobWorkerHelper.assertJobResponse(adminUserInfo, cycleRequest, (CreateSchemaResponse response) -> {
				fail("Should have not receive a response");
			}, MAX_WAIT_MS);
		}).getMessage();
		
		assertEquals("Schema $id: 'my.org.net/one' has a circular dependency", message);
	}

	public void registerSchemaFromClasspath(String name) throws Exception {
		try (InputStream in = CreateJsonSchemaWorkerIntegrationTest.class.getClassLoader().getResourceAsStream(name);) {
			if (in == null) {
				throw new IllegalArgumentException("Cannot find: '" + name + "' on the classpath");
			}
			String json = IOUtils.toString(in, "UTF-8");
			JsonSchema schema = EntityFactory.createEntityFromJSONString(json, JsonSchema.class);
			CreateSchemaRequest request = new CreateSchemaRequest();
			request.setSchema(schema);
			System.out.println("Creating schema: '" + schema.get$id() + "'");
			
			asynchronousJobWorkerHelper.assertJobResponse(adminUserInfo, request, (CreateSchemaResponse response) -> {
				assertNotNull(response);
				System.out.println(response.getNewVersionInfo());
			}, MAX_WAIT_MS);
		}
	}

	@Test
	public void testMainUseCase() throws Exception {
		jsonSchemaManager.truncateAll();
		schemaBootstrap.bootstrapSynapseSchemas();
		CreateOrganizationRequest createOrgRequest = new CreateOrganizationRequest();
		createOrgRequest.setOrganizationName("my.organization");
		organization = jsonSchemaManager.createOrganziation(adminUserInfo, createOrgRequest);
		String[] schemasToRegister = { "pets/PetType.json", "pets/Pet.json", "pets/CatBreed.json", "pets/DogBreed.json",
				"pets/Cat.json", "pets/Dog.json", "pets/PetPhoto.json" };
		for (String fileName : schemasToRegister) {
			registerSchemaFromClasspath(fileName);
		}

		JsonSchema validationSchema = jsonSchemaManager.getValidationSchema("my.organization/pets.PetPhoto");
		assertNotNull(schemaBootstrap);
		printJson(validationSchema);
		assertNotNull(validationSchema.getDefinitions());
		assertTrue(validationSchema.getDefinitions().containsKey("#/Definitions/my.organization/pets.PetType"));
		assertTrue(validationSchema.getDefinitions().containsKey("#/Definitions/my.organization/pets.Pet"));
		assertTrue(validationSchema.getDefinitions().containsKey("#/Definitions/my.organization/pets.Pet-1.0.3"));
		assertTrue(validationSchema.getDefinitions().containsKey("#/Definitions/my.organization/pets.dog.Breed"));
		assertTrue(validationSchema.getDefinitions().containsKey("#/Definitions/my.organization/pets.cat.Breed"));
		assertTrue(validationSchema.getDefinitions().containsKey("#/Definitions/my.organization/pets.cat.Cat"));
		assertTrue(validationSchema.getDefinitions().containsKey("#/Definitions/my.organization/pets.dog.Dog"));
		assertTrue(validationSchema.getDefinitions().containsKey("#/Definitions/org.sagebionetworks/repo.model.Entity"));
		assertTrue(validationSchema.getDefinitions().containsKey("#/Definitions/org.sagebionetworks/repo.model.Versionable"));
		assertTrue(validationSchema.getDefinitions().containsKey("#/Definitions/org.sagebionetworks/repo.model.VersionableEntity"));
		assertTrue(validationSchema.getDefinitions().containsKey("#/Definitions/org.sagebionetworks/repo.model.FileEntity"));

	}

	public void printJson(JSONEntity entity) throws JSONException, JSONObjectAdapterException {
		JSONObject object = new JSONObject(EntityFactory.createJSONStringForEntity(entity));
		System.out.println(object.toString(5));
	}

	/**
	 * Helper to create a schema with the given $id.
	 * 
	 * @param $id
	 * @return
	 */
	public JsonSchema createSchema(String organizationName, String schemaName) {
		JsonSchema schema = new JsonSchema();
		schema.set$id(organizationName + JsonSchemaConstants.PATH_DELIMITER + schemaName);
		return schema;
	}

	/**
	 * Helper to create a $ref to the given schema
	 * 
	 * @param toRef
	 * @return
	 */
	public JsonSchema create$RefSchema(JsonSchema toRef) {
		JsonSchema schema = new JsonSchema();
		schema.set$ref(toRef.get$id());
		return schema;
	}
}
