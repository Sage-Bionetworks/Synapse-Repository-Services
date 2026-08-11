package org.sagebionetworks.agent.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
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
import org.sagebionetworks.grid.workers.GridIntegrationTestUtils;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.agent.specialist.gridupdate.GridUpdateSpecialist;
import org.sagebionetworks.repo.manager.agent.specialist.gridupdate.GridUpdateSpecialistFactory;
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
 * Real-LLM integration tests for {@link GridUpdateSpecialist} — one test per example in
 * {@code GridExamples.getUpdateExamples()}. Like the query counterpart, these verify the only thing
 * not covered by the deterministic update/tool tests: that the specialist's prompt, examples, and
 * generated tool input schema are sufficient for the model to construct the correct
 * {@code GridUpdateRequest} for a natural-language instruction. Every test is a live round trip.
 * <p>
 * The grid fixture is built once for the whole class and shared across all tests (fixture setup
 * dominates the per-test cost). Because the grid is mutable and shared, <b>each test owns a disjoint
 * set of source and target columns</b> — no test's filters read a column another test writes — so the
 * tests remain order-independent.
 * <p>
 * A JSON schema is bound to the fixture even though validation itself is not asserted here: numeric
 * columns must be typed so {@code >} / {@code >=} filters compare numerically, and the columns
 * targeted by {@code IS_NULL} filters (u1Age, u5Material, u9Path) must be {@code required} so a bare
 * empty CSV field becomes an explicit JSON null rather than undefined — otherwise those filters would
 * match nothing.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
@TestInstance(Lifecycle.PER_CLASS)
public class GridUpdateSpecialistIntegrationTest {

	private static final long MAX_WAIT_MS = 1000L * 60 * 2;
	private static final long INTERNAL_REPLICA_ID = 66534L;

	private static final String SCHEMA_PATH = "schema/GridUpdateSpecialist.json";

	@Autowired
	private GridUpdateSpecialistFactory specialistFactory;
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
	@Autowired
	private GridIntegrationTestUtils gridTestUtils;

	private UserInfo admin;
	private GridSession session;
	private GridAgentSessionContext gridContext;

	@BeforeAll
	public void beforeAll() throws Exception {
		admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		entityService.truncateAll();
		gridIndexDao.truncateAll();

		// Four rows over a wide column set. Empty fields are written as a bare (Java null) CSV value: for
		// the three required columns (u1Age, u5Material, u9Path) that becomes an explicit JSON null so the
		// IS_NULL filters match; for every other column it becomes UNDEFINED. u5Color is pre-filled so U5
		// can observe it being cleared back to undefined, and a2 omits its folder so U9's SKIP_UPDATE path
		// leaves that row's path null. pvColor is pre-filled with a distinct value per row and is the target
		// of the preview test, which must leave it unchanged.
		File temp = File.createTempFile("GridUpdateSpecialistIntegrationTest", ".csv");
		CsvTableDescriptor csvDescriptor = new CsvTableDescriptor().setIsFirstLineHeader(true);
		try (CSVWriter writer = new CSVWriterProviderImpl().createWriter(new FileWriter(temp), csvDescriptor)) {
			writer.writeNext(new String[] { "a", "u1Age", "u2Height", "u2Type", "u2Footing", "u3Name", "u4Status",
					"u5Material", "u5Color", "u6Age", "u6Status", "u6Category", "u6Discount", "firstName", "lastName",
					"u7FullName", "email", "u8Domain", "bucket", "folder", "filename", "u9Path", "phone", "pvColor" });
			writer.writeNext(new String[] { "a1", null, "20", null, null, null, null, "wood", "c1", "70", null, null,
					null, "John", "Smith", null, "alice@example.com", null, "b1", "f1", "file1", null,
					"(555) 123-4567", "pOrig1" });
			writer.writeNext(new String[] { "a2", null, "10", null, null, null, null, null, "c2", "20", null, null,
					null, "Jane", "Doe", null, "bob@test.org", null, "b2", null, "file2", null, "(555) 987-6543",
				"pOrig2" });
			writer.writeNext(new String[] { "a3", "30", "15", null, null, null, null, "metal", "c3", "10", null, null,
					null, "Amy", "Ray", null, "carol@data.net", null, "b3", "f3", "file3", null, "(555) 111-2222",
				"pOrig3" });
			writer.writeNext(new String[] { "a4", "40", "8", null, null, null, null, null, "c4", "66", null, null, null,
					"Bob", "Jones", null, "dave@mail.io", null, "b4", "f4", "file4", null, "(555) 333-4444",
				"pOrig4" });
		}

		S3FileHandle fh = fileHandleManager.uploadLocalFile(new LocalFileUploadRequest().withFileToUpload(temp)
				.withContentType("text/csv").withFileName(temp.getName()).withUserId(admin.getId().toString()));
		temp.delete();

		Project project = entityService.createEntity(admin.getId(),
				new Project().setName("GridUpdateSpecialistTest"), null);

		RecordSet recordSet = entityService.createEntity(admin.getId(), new RecordSet().setName("update-specialist-set")
				.setParentId(project.getId()).setDataFileHandleId(fh.getId()).setUpsertKey(List.of("a")), null);

		String schema$id = createJsonSchema();
		entityService.bindSchemaToEntity(admin.getId(),
				new BindSchemaToEntityRequest().setEntityId(recordSet.getId()).setSchema$id(schema$id));

		session = asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateGridRequest().setRecordSetId(recordSet.getId()), (CreateGridResponse response) -> {
					assertNotNull(response.getGridSession());
				}, MAX_WAIT_MS).getResponse().getGridSession();
		assertNotNull(session);

		// Wait for all four rows to load before any test mutates the grid.
		gridTestUtils.waitForRowCount(session.getSessionId(), INTERNAL_REPLICA_ID, 4);

		GridReplica usersReplica = gridManager
				.createReplica(admin, new CreateReplicaRequest().setGridSessionId(session.getSessionId())).getReplica();
		// The update tool resolves the agent's write connection from agentsReplicaId; in production
		// GridContextValidatorHandler sets it. Create the agent replica directly here.
		GridReplica agentReplica = gridManager.createAgentReplica(admin, session);

		gridContext = new GridAgentSessionContext().setGridSessionId(session.getSessionId())
				.setUsersReplicaId(usersReplica.getReplicaId()).setAgentsReplicaId(agentReplica.getReplicaId());

		// Select rows a2 and a3. U3 (set u3Name for selected rows) and U6 (set discount for selected rows)
		// both read this selection.
		setUserSelection(usersReplica.getReplicaId(), Set.of("a2", "a3"));
	}

	@Test
	public void testLiteralSetWhereNull() throws Exception {
		GridUpdateSpecialist specialist = specialistFactory.create();

		// call under test
		applyUpdate(specialist, "For every row where the u1Age column is null, set u1Age to 25. Leave rows that "
				+ "already have an age unchanged.");

		gridTestUtils.waitForRows(session.getSessionId(), INTERNAL_REPLICA_ID, rows -> "25".equals(cell(rows, "a1",
				"u1Age")) && "25".equals(cell(rows, "a2", "u1Age")) && "30".equals(cell(rows, "a3", "u1Age"))
				&& "40".equals(cell(rows, "a4", "u1Age")));
	}

	@Test
	public void testTwoSetsWithExplicitNullAndLimit() throws Exception {
		GridUpdateSpecialist specialist = specialistFactory.create();

		// call under test
		applyUpdate(specialist, "For rows where u2Height is greater than 12, set u2Type to 'tall' and set u2Footing to "
				+ "an explicit null value. Update at most 10 rows.");

		gridTestUtils.waitForRows(session.getSessionId(), INTERNAL_REPLICA_ID, rows -> {
			JSONObject a1 = doc(rows, "a1");
			JSONObject a3 = doc(rows, "a3");
			JSONObject a2 = doc(rows, "a2");
			JSONObject a4 = doc(rows, "a4");
			// a1 and a3 (height > 12) get type 'tall' and an explicit JSON null footing.
			boolean matched = a1 != null && "tall".equals(a1.opt("u2Type")) && a1.has("u2Footing")
					&& a1.isNull("u2Footing") && a3 != null && "tall".equals(a3.opt("u2Type"))
					&& a3.has("u2Footing") && a3.isNull("u2Footing");
			// a2 and a4 (height <= 12) are untouched — u2Type stays undefined.
			boolean untouched = a2 != null && !a2.has("u2Type") && a4 != null && !a4.has("u2Type");
			return matched && untouched;
		});
	}

	@Test
	public void testLiteralSetForSelection() throws Exception {
		GridUpdateSpecialist specialist = specialistFactory.create();

		// call under test
		applyUpdate(specialist, "Set u3Name to 'Dave' for all of the rows I currently have selected.");

		gridTestUtils.waitForRows(session.getSessionId(), INTERNAL_REPLICA_ID, rows -> {
			JSONObject a1 = doc(rows, "a1");
			JSONObject a4 = doc(rows, "a4");
			// Only the selected rows a2 and a3 are named; the unselected rows stay undefined.
			return "Dave".equals(cell(rows, "a2", "u3Name")) && "Dave".equals(cell(rows, "a3", "u3Name"))
					&& a1 != null && !a1.has("u3Name") && a4 != null && !a4.has("u3Name");
		});
	}

	@Test
	public void testSetByRowId() throws Exception {
		GridUpdateSpecialist specialist = specialistFactory.create();

		// The update specialist has no query tool to discover row ids, so read the composite
		// replicaId.sequenceNumber ids for the target rows (a1 and a4) and hand them to the model verbatim.
		String a1RowId = rowId("a1");
		String a4RowId = rowId("a4");

		// call under test
		applyUpdate(specialist, "Set u4Status to true only for the rows with these exact IDs: " + a1RowId + " and "
				+ a4RowId + ".");

		gridTestUtils.waitForRows(session.getSessionId(), INTERNAL_REPLICA_ID, rows -> {
			JSONObject a2 = doc(rows, "a2");
			JSONObject a3 = doc(rows, "a3");
			return "true".equals(cell(rows, "a1", "u4Status")) && "true".equals(cell(rows, "a4", "u4Status"))
					&& a2 != null && !a2.has("u4Status") && a3 != null && !a3.has("u4Status");
		});
	}

	@Test
	public void testSetToUndefined() throws Exception {
		GridUpdateSpecialist specialist = specialistFactory.create();

		// call under test
		applyUpdate(specialist, "For rows where u5Material is null, clear the u5Color cell so it becomes undefined "
				+ "(unset it, do not set it to an empty string or null).");

		gridTestUtils.waitForRows(session.getSessionId(), INTERNAL_REPLICA_ID, rows -> {
			JSONObject a2 = doc(rows, "a2");
			JSONObject a4 = doc(rows, "a4");
			// a2 and a4 (material null) have their color cleared to undefined; the others keep their color.
			return a2 != null && !a2.has("u5Color") && a4 != null && !a4.has("u5Color")
					&& "c1".equals(cell(rows, "a1", "u5Color")) && "c3".equals(cell(rows, "a3", "u5Color"));
		});
	}

	@Test
	public void testBatchOfThree() throws Exception {
		GridUpdateSpecialist specialist = specialistFactory.create();

		// call under test
		applyUpdate(specialist, "Make these updates together: set u6Status to 'active' for rows where u6Age is greater "
				+ "than 18; set u6Category to 'senior' for rows where u6Age is at least 65; and set u6Discount to 0.15 "
				+ "for the rows I currently have selected.");

		gridTestUtils.waitForRows(session.getSessionId(), INTERNAL_REPLICA_ID, rows -> {
			JSONObject a1 = doc(rows, "a1");
			JSONObject a2 = doc(rows, "a2");
			JSONObject a3 = doc(rows, "a3");
			JSONObject a4 = doc(rows, "a4");
			if (a1 == null || a2 == null || a3 == null || a4 == null) {
				return false;
			}
			// status 'active' where age > 18: a1 (70), a2 (20), a4 (66); a3 (10) untouched.
			boolean status = "active".equals(a1.opt("u6Status")) && "active".equals(a2.opt("u6Status"))
					&& "active".equals(a4.opt("u6Status")) && !a3.has("u6Status");
			// category 'senior' where age >= 65: a1 (70), a4 (66); a2 (20), a3 (10) untouched.
			boolean category = "senior".equals(a1.opt("u6Category")) && "senior".equals(a4.opt("u6Category"))
					&& !a2.has("u6Category") && !a3.has("u6Category");
			// discount 0.15 for the selected rows a2, a3; the unselected rows untouched.
			boolean discount = "0.15".equals(String.valueOf(a2.opt("u6Discount")))
					&& "0.15".equals(String.valueOf(a3.opt("u6Discount"))) && !a1.has("u6Discount")
					&& !a4.has("u6Discount");
			return status && category && discount;
		});
	}

	@Test
	public void testTemplateConcat() throws Exception {
		GridUpdateSpecialist specialist = specialistFactory.create();

		// call under test
		applyUpdate(specialist, "Combine the firstName and lastName columns into the u7FullName column, separated by a "
				+ "single space, for rows where firstName is not null.");

		gridTestUtils.waitForRows(session.getSessionId(), INTERNAL_REPLICA_ID,
				rows -> "John Smith".equals(cell(rows, "a1", "u7FullName"))
						&& "Jane Doe".equals(cell(rows, "a2", "u7FullName"))
						&& "Amy Ray".equals(cell(rows, "a3", "u7FullName"))
						&& "Bob Jones".equals(cell(rows, "a4", "u7FullName")));
	}

	@Test
	public void testTemplateRegexExtract() throws Exception {
		GridUpdateSpecialist specialist = specialistFactory.create();

		// call under test
		applyUpdate(specialist, "Extract the domain (the part after the '@') from the email column into the u8Domain "
				+ "column using a regex, treating a missing email as an empty string. Only update rows where email is "
				+ "not null.");

		gridTestUtils.waitForRows(session.getSessionId(), INTERNAL_REPLICA_ID,
				rows -> "example.com".equals(cell(rows, "a1", "u8Domain"))
						&& "test.org".equals(cell(rows, "a2", "u8Domain"))
						&& "data.net".equals(cell(rows, "a3", "u8Domain"))
						&& "mail.io".equals(cell(rows, "a4", "u8Domain")));
	}

	@Test
	public void testTemplateMultiColumnSkipMissing() throws Exception {
		GridUpdateSpecialist specialist = specialistFactory.create();

		// call under test
		applyUpdate(specialist, "Build a path of the form 'bucket/folder/filename' into the u9Path column from the "
				+ "bucket, folder, and filename columns, but skip any row where one of those source columns is missing. "
				+ "Only fill rows where u9Path is currently null.");

		gridTestUtils.waitForRows(session.getSessionId(), INTERNAL_REPLICA_ID, rows -> {
			JSONObject a2 = doc(rows, "a2");
			// a2 is missing its folder, so it is skipped and its (required) path stays an explicit null.
			return "b1/f1/file1".equals(cell(rows, "a1", "u9Path"))
					&& "b3/f3/file3".equals(cell(rows, "a3", "u9Path"))
					&& "b4/f4/file4".equals(cell(rows, "a4", "u9Path"))
					&& a2 != null && a2.has("u9Path") && a2.isNull("u9Path");
		});
	}

	@Test
	public void testTemplateRegexReplace() throws Exception {
		GridUpdateSpecialist specialist = specialistFactory.create();

		// call under test
		applyUpdate(specialist, "Reformat the phone column from the '(555) 123-4567' style to '555-123-4567' by "
				+ "removing the parentheses and the space, for rows whose phone starts with '('.");

		gridTestUtils.waitForRows(session.getSessionId(), INTERNAL_REPLICA_ID,
				rows -> "555-123-4567".equals(cell(rows, "a1", "phone"))
						&& "555-987-6543".equals(cell(rows, "a2", "phone"))
						&& "555-111-2222".equals(cell(rows, "a3", "phone"))
						&& "555-333-4444".equals(cell(rows, "a4", "phone")));
	}

	@Test
	public void testPreviewDoesNotApplyChange() throws Exception {
		GridUpdateSpecialist specialist = specialistFactory.create();

		// call under test
		String response = specialist.chat("Preview — do not apply — what setting pvColor to 'PREVIEWED' would do "
				+ "for the row where pvColor is currently 'pOrig1'. Show me the resulting value, but make no change "
				+ "to the grid.", admin, null, gridContext);
		assertNotNull(response);
		if (response.contains("?")) {
			// A preview writes nothing, so it needs no authorization to proceed; if the model asks anyway,
			// steer it back to previewing only rather than authorizing an actual change.
			response = specialist.chat("Just run the preview and show me the result. Do not apply any change.", admin,
					null, gridContext);
			assertNotNull(response);
		}

		// The specialist should surface the value the previewed change would produce for the matched row.
		assertTrue(response.toLowerCase().contains("previewed"),
				"Expected the specialist to report the previewed resulting value, but was: " + response);

		// The preview must apply NOTHING. previewGridUpdate publishes no patch, so there is no write in flight
		// to race against — read the grid directly and confirm every pvColor still holds its original fixture
		// value. A changed value would mean the specialist applied the update instead of previewing it.
		GridHeader header = gridReplicaViewManager.readHeader(session.getSessionId(), INTERNAL_REPLICA_ID).get();
		List<RowView> rows = gridReplicaViewManager.querySinglePage(header, 100L, 0L);
		assertEquals("pOrig1", cell(rows, "a1", "pvColor"), "Preview must not modify the grid");
		assertEquals("pOrig2", cell(rows, "a2", "pvColor"), "Preview must not modify the grid");
		assertEquals("pOrig3", cell(rows, "a3", "pvColor"), "Preview must not modify the grid");
		assertEquals("pOrig4", cell(rows, "a4", "pvColor"), "Preview must not modify the grid");
	}

	/**
	 * Send an instruction to the specialist and, if it responds with a question instead of acting,
	 * authorize it to proceed once. The prompt frames the caller as a supervisor, so a confirmation
	 * request should be rare — this keeps the test robust when it happens.
	 */
	private void applyUpdate(GridUpdateSpecialist specialist, String instruction) {
		String response = specialist.chat(instruction, admin, null, gridContext);
		assertNotNull(response);
		if (response.contains("?")) {
			response = specialist.chat("You may proceed with making the change.", admin, null, gridContext);
			assertNotNull(response);
		}
	}

	/** The current value of a cell (by row "a" key and column name) as a String, or null if undefined. */
	private static String cell(List<RowView> rows, String key, String column) {
		JSONObject doc = doc(rows, key);
		return doc != null && doc.has(column) ? String.valueOf(doc.opt(column)) : null;
	}

	/** The row document for the given "a" key value within a page of rows, or null if not present. */
	private static JSONObject doc(List<RowView> rows, String key) {
		for (RowView row : rows) {
			JSONObject doc = row.getRowObject().getData().getRowJsonDocument();
			if (doc.has("a") && key.equals(String.valueOf(doc.get("a")))) {
				return doc;
			}
		}
		return null;
	}

	/** The composite replicaId.sequenceNumber id of the row with the given "a" key, from the internal view. */
	private String rowId(String key) {
		GridHeader header = gridReplicaViewManager.readHeader(session.getSessionId(), INTERNAL_REPLICA_ID).get();
		for (RowView row : gridReplicaViewManager.querySinglePage(header, 100L, 0L)) {
			JSONObject doc = row.getRowObject().getData().getRowJsonDocument();
			if (doc.has("a") && key.equals(String.valueOf(doc.get("a")))) {
				return row.getRowId();
			}
		}
		throw new IllegalStateException("No row with key: " + key);
	}

	/**
	 * Set the user replica's row selection to the given row keys (by "a" value) over the grid websocket
	 * protocol, then wait until the selection is visible on the internal replica.
	 */
	private void setUserSelection(Long usersReplicaId, Set<String> rowKeys) throws Exception {
		String url = gridService.createPresignedUrl(admin.getId(), new CreateGridPresignedUrlRequest()
				.setGridSessionId(session.getSessionId()).setReplicaId(usersReplicaId)).getPresignedUrl();
		BlockingQueue<String> incomingMessages = new LinkedBlockingQueue<>();
		WebSocket websocket = asynchronousJobWorkerHelper.createConnection(url, incomingMessages);

		GridHeader header = gridReplicaViewManager.readHeader(session.getSessionId(), INTERNAL_REPLICA_ID).get();
		List<RowView> rows = gridReplicaViewManager.querySinglePage(header, 100L, 0L);

		ReplicaSelectionModel selection = new ReplicaSelectionModel().setRowSelection(rows.stream().filter(r -> {
			JSONObject doc = r.getRowObject().getData().getRowJsonDocument();
			return doc.has("a") && rowKeys.contains(String.valueOf(doc.get("a")));
		}).map(r -> RowView.createCrdtIdFromLogical(r.getArrNodeId())).collect(Collectors.toList()))
				.setColumnSelection(List.of());

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
	 * grid document's "selection" object for the sending replica.
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
				"GridUpdateSpecialistIntegrationTest");
		JsonSchema jsonSchema = JsonEntityUtils.fromJsonString(ClasspathUtil.loadFromClasspath(SCHEMA_PATH),
				JsonSchema.class);
		jsonSchema.set$id(org.getName() + "-gridupdatespecialist");
		return asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateSchemaRequest().setDryRun(false).setSchema(jsonSchema), (CreateSchemaResponse response) -> {
					assertNotNull(response.getNewVersionInfo());
					assertNotNull(response.getNewVersionInfo().get$id());
				}, MAX_WAIT_MS).getResponse().getNewVersionInfo().get$id();
	}
}
