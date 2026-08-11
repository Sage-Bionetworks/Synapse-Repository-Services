package org.sagebionetworks.agent.worker;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.java_websocket.WebSocket;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.agent.specialist.gridquery.GridQuerySpecialist;
import org.sagebionetworks.repo.manager.agent.specialist.gridquery.GridQuerySpecialistFactory;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.agent.GridAgentSessionContext;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.grid.CreateGridPresignedUrlRequest;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.CreateGridResponse;
import org.sagebionetworks.repo.model.grid.CreateReplicaRequest;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.ReplicaSelectionModel;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessage;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.repo.model.grid.patch.compact.PatchCompactSerializable;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.InsertObjectBuilder;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.NewConstantBuilder;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.NewObjectBuilder;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.service.EntityService;
import org.sagebionetworks.repo.service.GridService;
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
 * Real-LLM integration tests for {@link GridQuerySpecialist} — one test per example in
 * {@code GridExamples.getQueryExamples()}. These tests verify the only thing not already covered by
 * the deterministic query/tool tests: that the specialist's prompt, examples, and generated tool
 * input schema are sufficient for the model to construct the correct {@code QueryRequest} for a
 * natural-language instruction. Every test is a live round trip to the model.
 * <p>
 * The grid fixture (a RecordSet-backed grid with a bound JSON schema and a user selection) is built
 * once for the whole class and shared across all tests, because fixture setup dominates the per-test
 * cost. Data is chosen so a wrong request construction produces a visibly different answer.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
@TestInstance(Lifecycle.PER_CLASS)
public class GridQuerySpecialistIntegrationTest {

	private static final long MAX_WAIT_MS = 1000L * 60 * 2;
	private static final long INTERNAL_REPLICA_ID = 66534L;

	private static final String SCHEMA_PATH = "schema/GridSpecialist.json";

	@Autowired
	private GridQuerySpecialistFactory specialistFactory;
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
	private GridService gridService;
	@Autowired
	private GridIndexDao gridIndexDao;

	private UserInfo admin;
	private GridSession session;
	private GridAgentSessionContext gridContext;

	@BeforeAll
	public void beforeAll() throws Exception {
		admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		entityService.truncateAll();
		gridIndexDao.truncateAll();

		// Six rows over columns: a, species, weight, age, color, subspecies. 'weight' is a required
		// integer, so row a4 (weight omitted) is the single invalid row while still carrying a valid
		// age > 25 for the "age over 25 AND invalid" example. Row a1 leaves subspecies as a bare empty
		// field, which becomes UNDEFINED (subspecies is optional) for the IS_UNDEFINED example.
		File temp = File.createTempFile("GridQuerySpecialistIntegrationTest", ".csv");
		CsvTableDescriptor csvDescriptor = new CsvTableDescriptor().setIsFirstLineHeader(true);
		try (CSVWriter writer = new CSVWriterProviderImpl().createWriter(new FileWriter(temp), csvDescriptor)) {
			writer.writeNext(new String[] { "a", "species", "weight", "age", "color", "subspecies" });
			writer.writeNext(new String[] { "a1", "cat", "10", "30", "red", null });
			writer.writeNext(new String[] { "a2", "dog", "20", "20", "green", "eskimo" });
			writer.writeNext(new String[] { "a3", "fish", "5", "10", "blue", "beta" });
			writer.writeNext(new String[] { "a4", "bird", null, "40", "red", "finch" });
			writer.writeNext(new String[] { "a5", "cat", "15", "50", "green", "tabby" });
			writer.writeNext(new String[] { "a6", "dog", "8", "28", "yellow", "lab" });
		}

		S3FileHandle fh = fileHandleManager.uploadLocalFile(new LocalFileUploadRequest().withFileToUpload(temp)
				.withContentType("text/csv").withFileName(temp.getName()).withUserId(admin.getId().toString()));
		temp.delete();

		Project project = entityService.createEntity(admin.getId(),
				new Project().setName("GridQuerySpecialistTest"), null);

		RecordSet recordSet = entityService.createEntity(admin.getId(), new RecordSet().setName("query-specialist-set")
				.setParentId(project.getId()).setDataFileHandleId(fh.getId()).setUpsertKey(List.of("a")), null);

		String schema$id = createJsonSchema();
		entityService.bindSchemaToEntity(admin.getId(),
				new BindSchemaToEntityRequest().setEntityId(recordSet.getId()).setSchema$id(schema$id));

		session = asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateGridRequest().setRecordSetId(recordSet.getId()), (CreateGridResponse response) -> {
					assertNotNull(response.getGridSession());
				}, MAX_WAIT_MS).getResponse().getGridSession();
		assertNotNull(session);

		// Wait for all six rows to load and for validation to settle to the single invalid row (a4).
		TimeUtils.waitFor(MAX_WAIT_MS, 2000L, () -> {
			Optional<GridHeader> header = gridReplicaViewManager.readHeader(session.getSessionId(), INTERNAL_REPLICA_ID);
			if (header.isEmpty()) {
				return Pair.create(false, null);
			}
			List<RowView> rows = gridReplicaViewManager.querySinglePage(header.get(), 100L, 0L);
			long invalid = rows.stream()
					.filter(r -> r.getRowValidationResults() != null && !r.getRowValidationResults().getIsValid())
					.count();
			return Pair.create(rows.size() == 6 && invalid == 1, null);
		});

		GridReplica usersReplica = gridManager
				.createReplica(admin, new CreateReplicaRequest().setGridSessionId(session.getSessionId())).getReplica();

		gridContext = new GridAgentSessionContext().setGridSessionId(session.getSessionId())
				.setUsersReplicaId(usersReplica.getReplicaId());

		// Select rows a4 (invalid) and a5 (valid), and the species + age columns. The selection-based
		// examples (Q6 count-of-selected-with-error, Q7 select-selected-columns) read from it.
		setUserSelection(usersReplica.getReplicaId(), Set.of("a4", "a5"), Set.of("species", "age"));
	}

	@Test
	public void testSelectAll() {
		GridQuerySpecialist specialist = specialistFactory.create();

		// call under test
		String response = specialist.chat("Show me up to 10 rows from the grid.", admin, null, gridContext);

		assertNotNull(response);
		String lower = response.toLowerCase();
		assertTrue(lower.contains("cat") || lower.contains("dog") || lower.contains("bird") || lower.contains("fish"),
				"SelectAll should surface species values. Got: " + response);
		assertTrue(lower.contains("red") || lower.contains("green") || lower.contains("blue")
				|| lower.contains("yellow"), "SelectAll should surface color values from a second column. Got: "
				+ response);
	}

	@Test
	public void testSelectByName() {
		GridQuerySpecialist specialist = specialistFactory.create();

		// call under test
		String response = specialist.chat("List only the species and weight for the first 10 rows.", admin, null,
				gridContext);

		assertNotNull(response);
		String lower = response.toLowerCase();
		assertTrue(lower.contains("cat") || lower.contains("dog") || lower.contains("bird") || lower.contains("fish"),
				"Should include species values. Got: " + response);
		assertTrue(!lower.contains("yellow"),
				"'yellow' is a color value and must not appear when only species and weight are selected. Got: "
						+ response);
	}

	@Test
	public void testCountStar() {
		GridQuerySpecialist specialist = specialistFactory.create();

		// call under test
		String response = specialist.chat("How many rows are in this grid?", admin, null, gridContext);

		assertNotNull(response);
		assertTrue(response.contains("6") || response.toLowerCase().contains("six"),
				"Should report the exact row count of 6. Got: " + response);
	}

	@Test
	public void testInvalidRowsWithValidationMessages() {
		GridQuerySpecialist specialist = specialistFactory.create();

		// call under test
		String response = specialist.chat("Which rows have age over 25 and fail schema validation, and why?", admin,
				null, gridContext);

		assertNotNull(response);
		String lower = response.toLowerCase();
		assertTrue(lower.contains("a4"),
				"Only row a4 has age > 25 and is invalid; it should be identified. Got: " + response);
		assertTrue(lower.contains("integer") || lower.contains("expected type"),
				"Should explain the validation error via the included messages. Got: " + response);
	}

	@Test
	public void testInFilter() {
		GridQuerySpecialist specialist = specialistFactory.create();

		// call under test
		String response = specialist.chat("Show the rows where color is red or green.", admin, null, gridContext);

		assertNotNull(response);
		String lower = response.toLowerCase();
		assertTrue(!lower.contains("blue"),
				"The blue row (a3) must be excluded by the color IN (red, green) filter. Got: " + response);
		assertTrue(!lower.contains("yellow"),
				"The yellow row (a6) must be excluded by the color IN (red, green) filter. Got: " + response);
		assertTrue(lower.contains("red") && lower.contains("green"),
				"The matching red and green rows should be reported. Got: " + response);
	}

	@Test
	public void testCountSelectedWithValidationError() {
		GridQuerySpecialist specialist = specialistFactory.create();

		// call under test
		String response = specialist.chat(
				"Of the rows I have selected, how many have a validation error mentioning 'expected type'?", admin,
				null, gridContext);

		assertNotNull(response);
		assertTrue(response.contains("1") || response.toLowerCase().contains("one"),
				"Exactly one selected row (a4) has an 'expected type' validation error. Got: " + response);
	}

	@Test
	public void testSelectSelection() {
		GridQuerySpecialist specialist = specialistFactory.create();

		// call under test
		String response = specialist.chat("Show me the values for just the columns I have currently selected.", admin,
				null, gridContext);

		assertNotNull(response);
		String lower = response.toLowerCase();
		assertTrue(lower.contains("bird"),
				"The selected 'species' column should surface its values, including a4's distinctive 'bird'. Got: "
						+ response);
		assertTrue(!lower.contains("yellow"),
				"'yellow' is a color value; the color column is not selected and must not appear. Got: " + response);
	}

	@Test
	public void testUndefinedAndNotNull() {
		GridQuerySpecialist specialist = specialistFactory.create();

		// call under test
		String response = specialist.chat("Which rows have no subspecies value set at all but do have a species?",
				admin, null, gridContext);

		assertNotNull(response);
		String lower = response.toLowerCase();
		assertTrue(lower.contains("a1"),
				"Only row a1 has an undefined subspecies with a species set. Got: " + response);
		assertTrue(!lower.contains("a2"),
				"Row a2 has a defined subspecies and must not be returned. Got: " + response);
	}

	/**
	 * Set the user replica's selection to the given row keys (by "a" value) and column names, over the
	 * grid websocket protocol, then wait until the selection is visible on the internal replica.
	 */
	private void setUserSelection(Long usersReplicaId, Set<String> rowKeys, Set<String> columnNames) throws Exception {
		String url = gridService.createPresignedUrl(admin.getId(), new CreateGridPresignedUrlRequest()
				.setGridSessionId(session.getSessionId()).setReplicaId(usersReplicaId)).getPresignedUrl();
		BlockingQueue<String> incomingMessages = new LinkedBlockingQueue<>();
		WebSocket websocket = asynchronousJobWorkerHelper.createConnection(url, incomingMessages);

		GridHeader header = gridReplicaViewManager.readHeader(session.getSessionId(), INTERNAL_REPLICA_ID).get();
		List<RowView> rows = gridReplicaViewManager.querySinglePage(header, 100L, 0L);

		ReplicaSelectionModel selection = new ReplicaSelectionModel()
				.setRowSelection(rows.stream().filter(r -> {
					JSONObject doc = r.getRowObject().getData().getRowJsonDocument();
					return doc.has("a") && rowKeys.contains(String.valueOf(doc.get("a")));
				}).map(r -> RowView.createCrdtIdFromLogical(r.getArrNodeId())).collect(Collectors.toList()))
				.setColumnSelection(header.getOrderedColumns().stream()
						.filter(c -> columnNames.contains(c.getName())).map(Column::getColumnOrderNodeId)
						.collect(Collectors.toList()));

		JsonRxMessage message = createSetSelectionMessage(header, selection,
				new LogicalTimestamp().setReplicaId(usersReplicaId).setSequenceNumber(1L));
		websocket.send(message.toJson());
		asynchronousJobWorkerHelper.waitForMessage((a) -> a.optInt(0) == 5 && a.optInt(1) == message.getId().get(),
				incomingMessages);

		TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
			ReplicaSelectionModel curSelection = gridReplicaViewManager
					.readHeader(session.getSessionId(), INTERNAL_REPLICA_ID, usersReplicaId).get()
					.getReplicaSelectionModel();
			return Pair.create(curSelection != null, null);
		});
	}

	/**
	 * Build a "patch" websocket message that writes the given {@link ReplicaSelectionModel} into the
	 * grid document's "selection" object for the sending replica. Mirrors the selection-patch shape
	 * used by {@code GridAgentChatWorkerIntegrationTest}.
	 */
	private JsonRxMessage createSetSelectionMessage(GridHeader header, ReplicaSelectionModel selection,
			LogicalTimestamp clock) {
		LogicalTimestamp rootObjId = header.getNodeId();
		Patch patch = new Patch();
		patch.setPatchId(clock);
		LogicalTimestamp selectionObj = patch.addNewOperation(new NewObjectBuilder());
		JSONObject selectionJson = JDOSecondaryPropertyUtils.createJSONObjectForEntity(selection);
		LogicalTimestamp conId = patch
				.addNewOperation(new NewConstantBuilder().setValue(new ConValue(ConType.JSON_OBJECT, selectionJson)));
		patch.addNewOperation(new InsertObjectBuilder().setObjectId(selectionObj)
				.setMap(Map.of(clock.getReplicaId().toString(), conId)));
		patch.addNewOperation(new InsertObjectBuilder().setObjectId(rootObjId).setMap(Map.of("selection", selectionObj)));
		return new JsonRxMessage(JsonRxMessageType.RequestData).setBody(PatchCompactSerializable.serialize(patch))
				.setId(987).setMethod("patch");
	}

	private String createJsonSchema() throws Exception {
		Organization org = asynchronousJobWorkerHelper.getOrCreateOrganization(admin.getId(),
				"GridQuerySpecialistIntegrationTest");
		JsonSchema jsonSchema = JsonEntityUtils.fromJsonString(ClasspathUtil.loadFromClasspath(SCHEMA_PATH),
				JsonSchema.class);
		jsonSchema.set$id(org.getName() + "-gridspecialist");
		return asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateSchemaRequest().setDryRun(false).setSchema(jsonSchema), (CreateSchemaResponse response) -> {
					assertNotNull(response.getNewVersionInfo());
					assertNotNull(response.getNewVersionInfo().get$id());
				}, MAX_WAIT_MS).getResponse().getNewVersionInfo().get$id();
	}
}
