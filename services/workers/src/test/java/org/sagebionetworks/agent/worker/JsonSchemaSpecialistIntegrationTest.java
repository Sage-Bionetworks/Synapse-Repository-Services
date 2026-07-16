package org.sagebionetworks.agent.worker;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.agent.specialist.jsonschema.JsonSchemaSpecialist;
import org.sagebionetworks.repo.manager.agent.specialist.jsonschema.JsonSchemaSpecialistFactory;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterClient;
import org.springaicommunity.agentcore.codeinterpreter.CodeExecutionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class JsonSchemaSpecialistIntegrationTest {

	private static final long MAX_WAIT_MS = 1000 * 80;

	@Autowired
	private JsonSchemaSpecialistFactory specialistFactory;

	@Autowired
	private AgentCoreCodeInterpreterClient codeInterpreterClient;

	@Autowired
	private UserManager userManager;

	@Autowired
	private JsonSchemaManager jsonSchemaManager;

	@Autowired
	private AsynchronousJobWorkerHelper asyncHelper;

	private UserInfo adminUser;
	private String parentSchema$id;

	@BeforeEach
	public void setup() throws Exception {
		jsonSchemaManager.truncateAll();
		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		asyncHelper.getOrCreateOrganization(adminUser.getId(), "my.specialist.org");

		// Register the referenced child schema first, then the parent that $refs it.
		registerSchema(getSchemaFromClasspath("schemaSpecialist/Address.json"));
		parentSchema$id = registerSchema(getSchemaFromClasspath("schemaSpecialist/Person.json"));
	}

	@AfterEach
	public void cleanup() {
		try {
			jsonSchemaManager.truncateAll();
		} catch (Exception e) { }
	}

	@Test
	public void testDescribeSchema() {
		JsonSchemaSpecialist specialist = specialistFactory.create();

		// call under test
		String response = specialist.chat("Describe the JSON schema " + parentSchema$id, adminUser, null);

		assertNotNull(response);
		assertTrue(response.toLowerCase().contains("name") || response.toLowerCase().contains("address")
				|| response.toLowerCase().contains("propert"),
				"Response should mention the schema's properties. Got: " + response);
	}

	@Test
	public void testDescribeReferencedType() {
		JsonSchemaSpecialist specialist = specialistFactory.create();

		// call under test — the referenced Address type is inlined into definitions
		String response = specialist.chat(
				"What fields are defined on the address property of schema " + parentSchema$id + "?",
				adminUser, null);

		assertNotNull(response);
		assertTrue(response.toLowerCase().contains("street") || response.toLowerCase().contains("city"),
				"Response should mention a field from the referenced Address schema. Got: " + response);
	}

	@Test
	public void testWriteSchemaToSession() {
		String sessionId = codeInterpreterClient.startSession("schemaSpecialistIT-" + System.nanoTime());
		try {
			JsonSchemaSpecialist specialist = specialistFactory.create();

			// call under test
			String response = specialist.chat(
					"Write the schema " + parentSchema$id + " to schema_specialist/person.json",
					adminUser, sessionId);

			assertNotNull(response);
			assertTrue(response.contains("schema_specialist") || response.contains("person") || response.contains("json"),
					"Response should mention the file path. Got: " + response);

			// Verify the file was written to the session and contains the resolved definitions
			CodeExecutionResult readResult = codeInterpreterClient.executeCode(sessionId, "python",
					"print(open('schema_specialist/person.json').read())");
			assertFalse(readResult.isError(), "Should read the file without error. Got: " + readResult.textOutput());
			assertTrue(readResult.textOutput().contains("definitions"),
					"File should contain the resolved definitions section. Got: " + readResult.textOutput());
			assertTrue(readResult.textOutput().contains("streetAddress"),
					"File should contain the inlined referenced schema. Got: " + readResult.textOutput());
		} finally {
			codeInterpreterClient.stopSession(sessionId);
		}
	}

	private JsonSchema getSchemaFromClasspath(String name) throws Exception {
		try (InputStream in = getClass().getClassLoader().getResourceAsStream(name)) {
			if (in == null) {
				throw new IllegalArgumentException("Cannot find: '" + name + "' on the classpath");
			}
			return EntityFactory.createEntityFromJSONString(IOUtils.toString(in, "UTF-8"), JsonSchema.class);
		}
	}

	private String registerSchema(JsonSchema schema) throws Exception {
		CreateSchemaRequest request = new CreateSchemaRequest();
		request.setSchema(schema);
		CreateSchemaResponse response = asyncHelper.assertJobResponse(adminUser, request,
				(CreateSchemaResponse r) -> assertNotNull(r.getNewVersionInfo()), MAX_WAIT_MS).getResponse();
		return response.getNewVersionInfo().get$id();
	}
}
