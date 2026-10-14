package org.sagebionetworks.agent.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.AgentAccessLevel;
import org.sagebionetworks.repo.model.agent.AgentChatRequest;
import org.sagebionetworks.repo.model.agent.AgentChatResponse;
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.CreateAgentSessionRequest;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.CreateGridResponse;
import org.sagebionetworks.repo.model.grid.CreateReplicaRequest;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.service.AgentService;
import org.sagebionetworks.repo.service.EntityService;
import org.sagebionetworks.util.ClasspathUtil;
import org.sagebionetworks.util.JsonEntityUtils;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.sagebionetworks.util.csv.CSVWriterProviderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import au.com.bytecode.opencsv.CSVWriter;

/**
 * End-to-end test that Curie can actually drive its specialists to curate a grid: given a
 * RecordSet-backed grid with a bound JSON schema and one row that fails validation, Curie must
 * (1) discover the invalid row and explain why it is invalid — which it does through the grid query
 * specialist, whose results carry each row's validation messages — and (2) fix the row through the
 * grid update specialist so the row becomes valid. The verification is the real grid state (read back
 * from the merged replica view), not just Curie's prose, so a wrong or un-applied update fails the
 * test. A second scenario asks Curie to explain the grid's schema (property names, types, required
 * columns) — a question answerable only by describing the schema rules, which forces the JSON schema
 * specialist path that the diagnose-and-fix flow deliberately skips. Requires live Bedrock + code
 * interpreter + AgentCore Memory access.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
@TestInstance(Lifecycle.PER_CLASS)
public class CurieGridCurationIntegrationTest {

	private static final long MAX_WAIT_MS = 1000L * 60 * 3;
	private static final long INTERNAL_REPLICA_ID = 66534L;
	private static final String SCHEMA_PATH = "schema/GridSpecialist.json";

	@Autowired
	private AgentService agentService;
	@Autowired
	private UserManager userManager;
	@Autowired
	private EntityService entityService;
	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	private AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;
	@Autowired
	private GridReplicaViewManager gridReplicaViewManager;
	@Autowired
	private GridManager gridManager;
	@Autowired
	private GridIndexDao gridIndexDao;

	private UserInfo admin;
	private GridSession session;
	private Long usersReplicaId;

	@BeforeAll
	public void beforeAll() throws Exception {
		admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		entityService.truncateAll();
		gridIndexDao.truncateAll();

		// Four rows over columns a, species, weight, age. 'weight' is a required integer, so row a2
		// (weight omitted) is the single invalid row; the other three are valid.
		File temp = File.createTempFile("CurieGridCurationIntegrationTest", ".csv");
		CsvTableDescriptor csvDescriptor = new CsvTableDescriptor().setIsFirstLineHeader(true);
		try (CSVWriter writer = new CSVWriterProviderImpl().createWriter(new FileWriter(temp), csvDescriptor)) {
			writer.writeNext(new String[] { "a", "species", "weight", "age" });
			writer.writeNext(new String[] { "a1", "cat", "10", "30" });
			writer.writeNext(new String[] { "a2", "dog", null, "20" });
			writer.writeNext(new String[] { "a3", "fish", "5", "10" });
			writer.writeNext(new String[] { "a4", "bird", "8", "40" });
		}

		S3FileHandle fh = fileHandleManager.uploadLocalFile(new LocalFileUploadRequest().withFileToUpload(temp)
				.withContentType("text/csv").withFileName(temp.getName()).withUserId(admin.getId().toString()));
		temp.delete();

		Project project = entityService.createEntity(admin.getId(),
				new Project().setName("CurieGridCurationTest"), null);

		RecordSet recordSet = entityService.createEntity(admin.getId(), new RecordSet().setName("curie-curation-set")
				.setParentId(project.getId()).setDataFileHandleId(fh.getId()).setUpsertKey(List.of("a")), null);

		String schema$id = createJsonSchema();
		entityService.bindSchemaToEntity(admin.getId(),
				new BindSchemaToEntityRequest().setEntityId(recordSet.getId()).setSchema$id(schema$id));

		session = asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateGridRequest().setRecordSetId(recordSet.getId()), (CreateGridResponse response) -> {
					assertNotNull(response.getGridSession());
				}, MAX_WAIT_MS).getResponse().getGridSession();
		assertNotNull(session);

		// Wait for all four rows to load and for validation to settle to the single invalid row (a2).
		TimeUtils.waitFor(MAX_WAIT_MS, 2000L, () -> {
			Optional<GridHeader> header = gridReplicaViewManager.readHeader(session.getSessionId(), INTERNAL_REPLICA_ID);
			if (header.isEmpty()) {
				return Pair.create(false, null);
			}
			List<RowView> rows = gridReplicaViewManager.querySinglePage(header.get(), 100L, 0L);
			long invalid = rows.stream()
					.filter(r -> r.getRowValidationResults() != null && !r.getRowValidationResults().getIsValid())
					.count();
			return Pair.create(rows.size() == 4 && invalid == 1, null);
		});

		// The human user's replica; each agent session is created against it. Creating the agent session
		// through AgentService runs GridContextValidatorHandler, which creates the agent's own replica and
		// populates agentsReplicaId — so the test no longer seeds that itself.
		usersReplicaId = gridManager
				.createReplica(admin, new CreateReplicaRequest().setGridSessionId(session.getSessionId())).getReplica()
				.getReplicaId();
	}

	/**
	 * Create an experimental grid agent session bound to the shared grid session, so chat turns route
	 * through the Curie supervisor rather than the default Bedrock agent.
	 */
	private AgentSession createCurieSession() {
		GridAgentSessionContext context = new GridAgentSessionContext().setGridSessionId(session.getSessionId())
				.setUsersReplicaId(usersReplicaId).setExperimental(true);
		AgentSession agentSession = agentService.createSession(admin.getId(), new CreateAgentSessionRequest()
				.setSessionContext(context).setAgentAccessLevel(AgentAccessLevel.WRITE_YOUR_PRIVATE_DATA));
		assertNotNull(agentSession);
		assertNotNull(context.getAgentsReplicaId());
		return agentSession;
	}

	/**
	 * Send one chat turn through the agent chat async job and return the supervisor's response text.
	 */
	private String chat(AgentSession agentSession, String chatText) throws Exception {
		return asynchronousJobWorkerHelper.assertJobResponse(admin,
				new AgentChatRequest().setSessionId(agentSession.getSessionId()).setChatText(chatText)
						.setEnableTrace(true),
				(AgentChatResponse response) -> {
					assertNotNull(response);
					assertEquals(agentSession.getSessionId(), response.getSessionId());
					assertNotNull(response.getResponseText());
					System.out.println(response.getResponseText());
				}, MAX_WAIT_MS).getResponse().getResponseText();
	}

	@Test
	public void testCurieDiagnosesAndFixesInvalidRow() throws Exception {
		// One durable agent session across all turns so Curie's memory carries the diagnosis into the fix.
		AgentSession agentSession = createCurieSession();

		// Turn 1 — diagnosis. Curie is not told which row is bad: it must query the grid (the grid query
		// specialist returns each row's data alongside its validation messages) to discover the invalid
		// row and explain why. We deliberately do NOT name the row, because a grid row is addressed
		// internally by a CRDT rowId (replicaId.sequenceNumber), not by the "a" column value — naming
		// "row a2" would push the specialist to treat "a2" as a rowId. Left to its own discretion Curie
		// may identify the row by that internal rowId, so we explicitly ask it to report the offending
		// row's "a" column value, which the query result surfaces.
		String diagnosis = chat(agentSession,
				"Exactly one row in this grid is failing schema validation. Query the grid to find which row it "
						+ "is. Report the value of its 'a' column, and explain which schema rule it violates and why.");
		assertNotNull(diagnosis);
		String lowerDiagnosis = diagnosis.toLowerCase();
		assertTrue(lowerDiagnosis.contains("a2"),
				"Curie should identify the invalid row by its 'a' value a2. Got: " + diagnosis);
		assertTrue(lowerDiagnosis.contains("weight"),
				"Curie should name the offending 'weight' property. Got: " + diagnosis);
		assertTrue(lowerDiagnosis.contains("integer") || lowerDiagnosis.contains("expected type")
				|| lowerDiagnosis.contains("required") || lowerDiagnosis.contains("missing"),
				"Curie should explain weight must be an integer that is absent. Got: " + diagnosis);

		// Turn 2 — request the fix, addressing the row by its "a" column value (not as a rowId) so the
		// grid update specialist selects it with a column-value filter. Curie's most important rule is to
		// preview and confirm before committing, so this turn is expected to propose the change and ask
		// for confirmation rather than applying it.
		String proposal = chat(agentSession,
				"Please fix the row where column a is a2 by setting its weight to 42 so it passes validation.");
		assertNotNull(proposal);

		// Turn 3 — explicit confirmation, so Curie delegates the actual update to the grid update specialist.
		String applied = chat(agentSession, "Yes, I confirm. Please apply that update now.");
		assertNotNull(applied);

		// Verify against real grid state: row a2 now carries weight 42 and passes validation. Polls
		// because the update flows through the hub and the validation worker re-runs asynchronously.
		TimeUtils.waitFor(MAX_WAIT_MS, 3000L, () -> {
			RowView a2 = readRow("a2");
			boolean fixed = a2 != null && a2.getRowValidationResults() != null
					&& a2.getRowValidationResults().getIsValid()
					&& a2.getRowObject().getData().getRowJsonDocument().optInt("weight", -1) == 42;
			return Pair.create(fixed, null);
		});
	}

	@Test
	public void testCurieDescribesGridSchema() throws Exception {
		AgentSession agentSession = createCurieSession();

		// A question that can only be answered by describing the schema rules themselves — the property
		// names, their types, and which are required. None of this is derivable from a validation query
		// (which only reports whether each row is valid and, on request, a violation message), so Curie
		// must route through the JSON schema specialist. We never give it a schema $id: the specialist
		// resolves the grid's bound schema on its own from the live session.
		String description = chat(agentSession,
				"Help me understand this grid's schema. Which columns does the schema define, what type "
						+ "is each, and which columns are required?");
		assertNotNull(description);
		String lower = description.toLowerCase();
		// The required set (a, weight) and weight's integer type come only from the schema, not from any
		// row's validation state — asserting on them proves the schema specialist path was exercised.
		assertTrue(lower.contains("weight"),
				"Curie should describe the 'weight' property from the schema. Got: " + description);
		assertTrue(lower.contains("required"),
				"Curie should report which columns are required. Got: " + description);
		assertTrue(lower.contains("integer"),
				"Curie should report weight's integer type from the schema. Got: " + description);
	}

	/**
	 * Read a single row from the merged replica view by its "a" key, or null if not yet present.
	 */
	private RowView readRow(String key) {
		Optional<GridHeader> header = gridReplicaViewManager.readHeader(session.getSessionId(), INTERNAL_REPLICA_ID);
		if (header.isEmpty()) {
			return null;
		}
		List<RowView> rows = gridReplicaViewManager.querySinglePage(header.get(), 100L, 0L);
		return rows.stream().filter(r -> {
			JSONObject doc = r.getRowObject().getData().getRowJsonDocument();
			return doc.has("a") && key.equals(String.valueOf(doc.get("a")));
		}).findFirst().orElse(null);
	}

	private String createJsonSchema() throws Exception {
		Organization org = asynchronousJobWorkerHelper.getOrCreateOrganization(admin.getId(),
				"CurieGridCurationIntegrationTest");
		JsonSchema jsonSchema = JsonEntityUtils.fromJsonString(ClasspathUtil.loadFromClasspath(SCHEMA_PATH),
				JsonSchema.class);
		jsonSchema.set$id(org.getName() + "-curiegrid");
		return asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateSchemaRequest().setDryRun(false).setSchema(jsonSchema), (CreateSchemaResponse response) -> {
					assertNotNull(response.getNewVersionInfo());
					assertNotNull(response.getNewVersionInfo().get$id());
				}, MAX_WAIT_MS).getResponse().getNewVersionInfo().get$id();
	}
}
