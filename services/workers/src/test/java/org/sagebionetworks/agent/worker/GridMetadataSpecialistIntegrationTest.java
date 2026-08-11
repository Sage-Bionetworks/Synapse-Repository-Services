package org.sagebionetworks.agent.worker;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.grid.workers.GridIntegrationTestUtils;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.agent.specialist.gridmetadata.GridMetadataSpecialist;
import org.sagebionetworks.repo.manager.agent.specialist.gridmetadata.GridMetadataSpecialistFactory;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.CreateGridResponse;
import org.sagebionetworks.repo.model.grid.CreateReplicaRequest;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridReplicaInfo;
import org.sagebionetworks.repo.model.grid.GridReplicaType;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.ListGridReplicasRequest;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.service.EntityService;
import org.sagebionetworks.util.ClasspathUtil;
import org.sagebionetworks.util.JsonEntityUtils;
import org.sagebionetworks.util.csv.CSVWriterProviderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * Real-LLM integration tests for {@link GridMetadataSpecialist}. Each test verifies the only thing
 * not already covered by the deterministic tool tests: that the specialist's prompt and tool
 * descriptions are sufficient for the model to pick the right tool for a natural-language question
 * about the grid session and its replicas — resolving a single {@code replicaId} to its type and
 * creator, naming that creator, describing the bound schema, and listing who else is participating.
 * Every test is a live round trip to the model.
 * <p>
 * The grid fixture is built once for the whole class (fixture setup dominates the per-test cost). It
 * carries three replica kinds so the type-resolution answers are unambiguous: the SERVICE replica the
 * system created when it loaded the source data, a USER replica, and an AGENT replica.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
@TestInstance(Lifecycle.PER_CLASS)
public class GridMetadataSpecialistIntegrationTest {

	private static final long MAX_WAIT_MS = 1000L * 60 * 2;
	private static final long INTERNAL_REPLICA_ID = 66534L;

	private static final String SCHEMA_PATH = "schema/GridSpecialist.json";

	@Autowired
	private GridMetadataSpecialistFactory specialistFactory;
	@Autowired
	private UserManager userManager;
	@Autowired
	private EntityService entityService;
	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	private AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;
	@Autowired
	private GridManager gridManager;
	@Autowired
	private GridIndexDao gridIndexDao;
	@Autowired
	private GridIntegrationTestUtils gridTestUtils;
	@Autowired
	private PrincipalAliasDAO principalAliasDAO;

	private UserInfo admin;
	private String adminUsername;
	private GridSession session;
	private GridAgentSessionContext gridContext;

	private Long userReplicaId;
	private Long agentReplicaId;
	private Long serviceReplicaId;

	@BeforeAll
	public void beforeAll() throws Exception {
		admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		adminUsername = principalAliasDAO.getUserName(admin.getId());
		entityService.truncateAll();
		gridIndexDao.truncateAll();

		// Two valid rows are enough — this specialist reads session/replica metadata, not row data.
		File temp = File.createTempFile("GridMetadataSpecialistIntegrationTest", ".csv");
		CsvTableDescriptor csvDescriptor = new CsvTableDescriptor().setIsFirstLineHeader(true);
		try (CSVWriter writer = new CSVWriterProviderImpl().createWriter(new FileWriter(temp), csvDescriptor)) {
			writer.writeNext(new String[] { "a", "species", "weight", "age", "color", "subspecies" });
			writer.writeNext(new String[] { "a1", "cat", "10", "30", "red", "tabby" });
			writer.writeNext(new String[] { "a2", "dog", "20", "20", "green", "lab" });
		}

		S3FileHandle fh = fileHandleManager.uploadLocalFile(new LocalFileUploadRequest().withFileToUpload(temp)
				.withContentType("text/csv").withFileName(temp.getName()).withUserId(admin.getId().toString()));
		temp.delete();

		Project project = entityService.createEntity(admin.getId(),
				new Project().setName("GridMetadataSpecialistTest"), null);

		RecordSet recordSet = entityService.createEntity(admin.getId(), new RecordSet().setName("metadata-specialist-set")
				.setParentId(project.getId()).setDataFileHandleId(fh.getId()).setUpsertKey(List.of("a")), null);

		String schema$id = createJsonSchema();
		entityService.bindSchemaToEntity(admin.getId(),
				new BindSchemaToEntityRequest().setEntityId(recordSet.getId()).setSchema$id(schema$id));

		session = asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateGridRequest().setRecordSetId(recordSet.getId()), (CreateGridResponse response) -> {
					assertNotNull(response.getGridSession());
				}, MAX_WAIT_MS).getResponse().getGridSession();
		assertNotNull(session);

		// Wait for both rows to load, which also guarantees the SERVICE replica that loaded them exists.
		gridTestUtils.waitForRowCount(session.getSessionId(), INTERNAL_REPLICA_ID, 2);

		GridReplica usersReplica = gridManager
				.createReplica(admin, new CreateReplicaRequest().setGridSessionId(session.getSessionId())).getReplica();
		GridReplica agentReplica = gridManager.createAgentReplica(admin, session);
		userReplicaId = usersReplica.getReplicaId();
		agentReplicaId = agentReplica.getReplicaId();

		// The SERVICE replica id is assigned internally; discover it from the participant list so the test
		// does not depend on the exact value the system chose.
		serviceReplicaId = gridManager
				.listReplicas(admin, new ListGridReplicasRequest().setGridSessionId(session.getSessionId())).getPage()
				.stream().filter(r -> GridReplicaType.SERVICE.equals(r.getReplicaType()))
				.map(GridReplicaInfo::getReplicaId).findFirst().orElse(null);
		assertNotNull(serviceReplicaId, "The grid session should have a SERVICE replica that loaded the source data");

		gridContext = new GridAgentSessionContext().setGridSessionId(session.getSessionId())
				.setUsersReplicaId(userReplicaId).setAgentsReplicaId(agentReplicaId);
	}

	@Test
	public void testDescribeGridSchema() {
		GridMetadataSpecialist specialist = specialistFactory.create();

		// call under test
		String response = specialist.chat("What JSON schema is this grid's data validated against? Give me its id.",
				admin, null, gridContext);

		assertNotNull(response);
		assertTrue(response.toLowerCase().contains("gridmetadataspecialist"),
				"getGridSession should surface the bound schema $id. Got: " + response);
	}

	@Test
	public void testResolveUserReplica() {
		GridMetadataSpecialist specialist = specialistFactory.create();

		// call under test
		String response = specialist.chat("A cell in the grid was last changed by replica " + userReplicaId
				+ ". Is that a person, an AI agent, or the system?", admin, null, gridContext);

		assertNotNull(response);
		assertTrue(response.toLowerCase().contains("user") || response.toLowerCase().contains("person"),
				"Replica " + userReplicaId + " is a USER replica and should be identified as a person. Got: " + response);
	}

	@Test
	public void testResolveAgentReplica() {
		GridMetadataSpecialist specialist = specialistFactory.create();

		// call under test
		String response = specialist.chat("A row in the grid was last changed by replica " + agentReplicaId
				+ ". Is that a person, an AI agent, or the system?", admin, null, gridContext);

		assertNotNull(response);
		assertTrue(response.toLowerCase().contains("agent"),
				"Replica " + agentReplicaId + " is an AGENT replica and should be identified as an AI agent. Got: "
						+ response);
	}

	@Test
	public void testResolveServiceReplica() {
		GridMetadataSpecialist specialist = specialistFactory.create();

		// call under test
		String response = specialist.chat("A row in the grid was last changed by replica " + serviceReplicaId
				+ ". Is that a person, an AI agent, or the system?", admin, null, gridContext);

		assertNotNull(response);
		String lower = response.toLowerCase();
		assertTrue(lower.contains("service") || lower.contains("system"),
				"Replica " + serviceReplicaId + " is a SERVICE replica and should be identified as a system change. Got: "
						+ response);
	}

	@Test
	public void testNameReplicaCreator() {
		GridMetadataSpecialist specialist = specialistFactory.create();

		// call under test
		String response = specialist.chat("Who is the Synapse user that created replica " + userReplicaId
				+ "? Give me their username.", admin, null, gridContext);

		assertNotNull(response);
		assertTrue(response.toLowerCase().contains(adminUsername.toLowerCase()),
				"The creator of replica " + userReplicaId + " is '" + adminUsername + "'; the specialist should resolve "
						+ "the replica's createdBy user id to that username. Got: " + response);
	}

	@Test
	public void testListParticipants() {
		GridMetadataSpecialist specialist = specialistFactory.create();

		// call under test
		String response = specialist.chat("Who else is working on this grid session? List the participants and say "
				+ "what kind each one is.", admin, null, gridContext);

		assertNotNull(response);
		String lower = response.toLowerCase();
		// listReplicas returns the USER, AGENT, and SERVICE replicas; a correct summary distinguishes them.
		assertTrue(lower.contains("user") || lower.contains("person"),
				"The participant list should include the USER replica. Got: " + response);
		assertTrue(lower.contains("agent"), "The participant list should include the AGENT replica. Got: " + response);
	}

	private String createJsonSchema() throws Exception {
		Organization org = asynchronousJobWorkerHelper.getOrCreateOrganization(admin.getId(),
				"GridMetadataSpecialistIntegrationTest");
		JsonSchema jsonSchema = JsonEntityUtils.fromJsonString(ClasspathUtil.loadFromClasspath(SCHEMA_PATH),
				JsonSchema.class);
		jsonSchema.set$id(org.getName() + "-gridmetadataspecialist");
		return asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateSchemaRequest().setDryRun(false).setSchema(jsonSchema), (CreateSchemaResponse response) -> {
					assertNotNull(response.getNewVersionInfo());
					assertNotNull(response.getNewVersionInfo().get$id());
				}, MAX_WAIT_MS).getResponse().getNewVersionInfo().get$id();
	}
}
