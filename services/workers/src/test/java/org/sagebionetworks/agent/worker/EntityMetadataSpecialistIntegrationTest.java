package org.sagebionetworks.agent.worker;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.agent.Agent;
import org.sagebionetworks.repo.manager.agent.AgentToolContextKey;
import org.sagebionetworks.repo.manager.agent.CodeSessionSupplier;
import org.sagebionetworks.repo.manager.agent.specialist.entitymetadata.EntityMetadataSpecialistFactory;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springaicommunity.agentcore.codeinterpreter.AgentCoreCodeInterpreterClient;
import org.springaicommunity.agentcore.codeinterpreter.CodeExecutionResult;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EntityMetadataSpecialistIntegrationTest {

	private static final long MAX_WAIT_MS = 1000 * 80;
	private static final int MAX_RESPONSE_CHARS = 4000;

	@Autowired
	private EntityMetadataSpecialistFactory specialistFactory;

	@Autowired
	private AgentCoreCodeInterpreterClient codeInterpreterClient;

	@Autowired
	private UserManager userManager;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private FileHandleManager fileHandleManager;

	@Autowired
	private JsonSchemaManager jsonSchemaManager;

	@Autowired
	private AsynchronousJobWorkerHelper asyncHelper;

	private UserInfo adminUser;
	private String projectId;
	private String folderId;
	private String fileId;
	private String schema$id;

	@BeforeEach
	public void setup() throws Exception {
		jsonSchemaManager.truncateAll();
		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());

		Project project = new Project().setName("EntityMetadataIT-" + UUID.randomUUID());
		projectId = entityManager.createEntity(adminUser, project, null);

		// A child folder carrying real annotations we can ask the specialist about.
		Folder folder = new Folder().setParentId(projectId).setName("annotated-folder");
		folderId = entityManager.createEntity(adminUser, folder, null);
		folder = entityManager.getEntity(adminUser, folderId, Folder.class);
		Annotations annotations = new Annotations().setId(folder.getId()).setEtag(folder.getEtag());
		AnnotationsV2TestUtils.putAnnotations(annotations, "tissue", "liver", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annotations, "readCount", "12345", AnnotationsValueType.LONG);
		entityManager.updateAnnotations(adminUser, folderId, annotations);

		// A file entity backed by real content so it can be staged into the session.
		S3FileHandle fileHandle = fileHandleManager.createFileFromByteArray(adminUser.getId().toString(), new Date(),
				"col_a,col_b\n1,2\n3,4\n".getBytes(StandardCharsets.UTF_8), "metadata-file.csv",
				ContentType.create("text/csv"), null);
		fileId = entityManager.createEntity(adminUser, new FileEntity().setName("metadata-file")
				.setParentId(projectId).setDataFileHandleId(fileHandle.getId()), null);

		// Bind a schema to the project so the specialist can report the binding. Use self-contained
		// schemas (Person $refs only Address) so registration does not depend on the bootstrapped
		// Synapse entity schemas, which are not registered in this test's clean schema state.
		asyncHelper.getOrCreateOrganization(adminUser.getId(), "my.specialist.org");
		registerSchema(getSchemaFromClasspath("schemaSpecialist/Address.json"));
		schema$id = registerSchema(getSchemaFromClasspath("schemaSpecialist/Person.json"));
		BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
		bindRequest.setEntityId(projectId);
		bindRequest.setSchema$id(schema$id);
		entityManager.bindSchemaToEntity(adminUser, bindRequest);
	}

	@AfterEach
	public void cleanup() {
		if (projectId != null) {
			try {
				entityManager.deleteEntity(adminUser, projectId);
			} catch (Exception e) { }
		}
		try {
			jsonSchemaManager.truncateAll();
		} catch (Exception e) { }
	}

	@Test
	public void testGetAnnotations() {
		Agent specialist = specialistFactory.create();

		// call under test
		String response = specialist.chat("What annotations does " + folderId + " have?", toolContext(null));

		assertNotNull(response);
		assertTrue(response.length() <= MAX_RESPONSE_CHARS, "Response should be within the cap. Got: " + response);
		assertTrue(response.toLowerCase().contains("tissue") || response.toLowerCase().contains("liver"),
				"Response should mention an annotation that was set. Got: " + response);
	}

	@Test
	public void testGetEntityDetails() {
		Agent specialist = specialistFactory.create();

		// call under test
		String response = specialist.chat("Describe the entity " + folderId + " including its type and name.",
				toolContext(null));

		assertNotNull(response);
		assertTrue(response.length() <= MAX_RESPONSE_CHARS, "Response should be within the cap. Got: " + response);
		assertTrue(response.toLowerCase().contains("folder") || response.toLowerCase().contains("annotated-folder"),
				"Response should mention the entity's type or name. Got: " + response);
	}

	@Test
	public void testGetSchemaBinding() {
		Agent specialist = specialistFactory.create();

		// call under test
		String response = specialist.chat("Is there a JSON schema bound to " + projectId + "? If so, which one?",
				toolContext(null));

		assertNotNull(response);
		assertTrue(response.length() <= MAX_RESPONSE_CHARS, "Response should be within the cap. Got: " + response);
		assertTrue(response.contains("Person") || response.toLowerCase().contains("my.specialist.org"),
				"Response should mention the bound schema. Got: " + response);
	}

	@Test
	public void testGetChildren() {
		Agent specialist = specialistFactory.create();

		// call under test
		String response = specialist.chat("What entities are inside " + projectId + "?", toolContext(null));

		assertNotNull(response);
		assertTrue(response.length() <= MAX_RESPONSE_CHARS, "Response should be within the cap. Got: " + response);
		assertTrue(response.contains("annotated-folder") || response.contains("metadata-file"),
				"Response should mention a child entity. Got: " + response);
	}

	@Test
	public void testGetFilesMetadata() {
		Agent specialist = specialistFactory.create();

		// call under test
		String response = specialist.chat("Report the file metadata for " + fileId
				+ ". Include its content type, its size in bytes, and whether it can be added to a session.",
				toolContext(null));

		assertNotNull(response);
		assertTrue(response.length() <= MAX_RESPONSE_CHARS, "Response should be within the cap. Got: " + response);
		// The file is a small, downloadable CSV, so the specialist should report its type and that it is
		// eligible to be added to a session.
		assertTrue(response.toLowerCase().contains("csv"),
				"Response should mention the CSV content type. Got: " + response);
		assertTrue(response.toLowerCase().contains("add") || response.toLowerCase().contains("eligible"),
				"Response should mention the file can be added to a session. Got: " + response);
	}

	@Test
	public void testAddFileToSession() {
		String sessionId = codeInterpreterClient.startSession("entityMetadataIT-" + System.nanoTime());
		try {
			Agent specialist = specialistFactory.create();

			// call under test
			String response = specialist.chat(
					"Add the file " + fileId + " to the session at entity_metadata_specialist/data.csv",
					toolContext(sessionId));

			assertNotNull(response);
			assertTrue(response.length() <= MAX_RESPONSE_CHARS, "Response should be within the cap. Got: " + response);

			// Verify the file actually landed in the session with its real content.
			CodeExecutionResult readResult = codeInterpreterClient.executeCode(sessionId, "python",
					"print(open('entity_metadata_specialist/data.csv').read())");
			assertFalse(readResult.isError(), "Should read the file without error. Got: " + readResult.textOutput());
			assertTrue(readResult.textOutput().contains("col_a"),
					"File should contain the staged content. Got: " + readResult.textOutput());
		} finally {
			codeInterpreterClient.stopSession(sessionId);
		}
	}

	/**
	 * Builds the tool context the caller hands to the specialist: the acting user and, when the specialist
	 * needs to stage a file, an already-started code session.
	 */
	private ToolContext toolContext(String sessionId) {
		Map<String, Object> context = new HashMap<>();
		AgentToolContextKey.USER_INFO.put(context, adminUser);
		if (sessionId != null) {
			AgentToolContextKey.CODE_SESSION_SUPPLIER.put(context, CodeSessionSupplier.of(sessionId));
		}
		return new ToolContext(context);
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
