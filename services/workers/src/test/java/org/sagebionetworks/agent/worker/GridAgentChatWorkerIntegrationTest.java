package org.sagebionetworks.agent.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.java_websocket.WebSocket;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.QueryElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.CellValueFilterElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.CellValueOperatorElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.RowIsValidFilterElement;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter.RowSelectionFilterElement;
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
import org.sagebionetworks.repo.model.agent.TraceEvent;
import org.sagebionetworks.repo.model.agent.TraceEventsRequest;
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
import org.sagebionetworks.repo.model.grid.query.result.QueryResult;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.service.AgentService;
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

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class GridAgentChatWorkerIntegrationTest {

	public static final long MAX_WAIT_MS = 120_000;

	private static final long INTERNAL_REPLICA_ID = 66534L;

	@Autowired
	private AgentService agentService;
	@Autowired
	private GridService gridService;
	@Autowired
	private UserManager userManager;
	@Autowired
	private EntityService entityService;
	@Autowired
	private AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;
	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	private GridReplicaViewManager gridReplicaViewManager;
	@Autowired
	private GridIndexDao gridIndexDao;

	private UserInfo admin;
	private CsvTableDescriptor csvDescriptor;
	private Project project;
	private RecordSet recordSet;
	private String schema$id;
	private GridSession gridSession;

	@AfterEach
	public void after() {
		entityService.truncateAll();
	}

	@BeforeEach
	public void before() throws Exception {
		entityService.truncateAll();
		gridIndexDao.truncateAll();
		admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	}

	/**
	 * Helper to create a grid session from CSV rows and bind the provided schema.
	 * This consolidates duplicated setup code so tests can call with different
	 * datasets and schemas.
	 *
	 * @param csvHeader                the CSV header columns (first line)
	 * @param csvRows                  the CSV data rows (each row is a String[] matching header length)
	 * @param schemaPath            classpath path to the JSON schema
	 * @param shortName             short name used when creating the schema id
	 * @param expectedTotalRows     expected total row count to wait for
	 * @param expectedInvalidRows   expected invalid row count to wait for
	 */
	void createGridSessionFromCsv(String[] csvHeader, List<String[]> csvRows, String schemaPath, String shortName, int expectedTotalRows, int expectedInvalidRows) throws Exception {
		File temp = File.createTempFile("GridScaleIntegrationTest", ".csv", null);

		csvDescriptor = new CsvTableDescriptor().setIsFirstLineHeader(true);
		try (CSVWriter writer = new CSVWriterProviderImpl().createWriter(new FileWriter(temp), csvDescriptor)) {
			writer.writeNext(csvHeader);
			for (String[] r : csvRows) {
				writer.writeNext(r);
			}
		}

		S3FileHandle fh = fileHandleManager
				.uploadLocalFile(new LocalFileUploadRequest().withFileToUpload(temp).withContentType("text/csv")
						.withFileName(temp.getName()).withUserId(admin.getId().toString()));
		temp.delete();

		project = entityService.createEntity(admin.getId(), new Project().setName("GridScaleIntegrationTest"), null);

		// use the first column as the upsert key
		recordSet = entityService.createEntity(admin.getId(), new RecordSet().setName("aRecordSet")
				.setParentId(project.getId()).setDataFileHandleId(fh.getId()).setUpsertKey(List.of(csvHeader[0])), null);

		schema$id = createJsonSchema(schemaPath, shortName);

		entityService.bindSchemaToEntity(admin.getId(),
				new BindSchemaToEntityRequest().setEntityId(recordSet.getId()).setSchema$id(schema$id));

		gridSession = asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateGridRequest().setRecordSetId(recordSet.getId()), (CreateGridResponse response) -> {
					assertNotNull(response);
					assertNotNull(response.getGridSession());
				}, MAX_WAIT_MS).getResponse().getGridSession();
		assertNotNull(gridSession);
		assertEquals(recordSet.getId(), gridSession.getSourceEntityId());
		assertEquals(schema$id, gridSession.getGridJsonSchema$Id());

		TimeUtils.waitFor(MAX_WAIT_MS, 2000L, () -> {
			System.out.println("Waiting for row validation results to change...");
			Optional<GridHeader> header = gridReplicaViewManager.readHeader(gridSession.getSessionId(),
					INTERNAL_REPLICA_ID);
			if (header.isEmpty()) {
				return Pair.create(false, null);
			}
			List<RowView> rows = gridReplicaViewManager.querySinglePage(header.get(), 100L, 0L);
			System.out.println("row count: " + rows.size());
			int invalidRows = (int) rows.stream()
					.filter(r -> r.getRowValidationResults() != null && !r.getRowValidationResults().getIsValid())
					.count();
			System.out.println("invalid count: " + invalidRows);
			if (rows.size() != expectedTotalRows || invalidRows != expectedInvalidRows) {
				return Pair.create(false, null);
			}
			return Pair.create(true, header.get());
		});
	}

	@Test
	public void testViewWithSchemaAndAgentChat() throws Exception {
		createGridSessionFromCsv(
				new String[] { "a", "b" },
				List.of(
						new String[] { "1", "one" },
						new String[] { "2" },
						new String[] { null, "no id" },
						new String[] { "3", "true" },
						new String[] { "98", "ninety eight" },
						new String[] { "99" },
						new String[] { "101" },
						new String[] { "102", "one hundred two" },
						new String[] { "103", "3.41" }
				),
				"schema/ConditionalRequirement.json",
				"conditionalrequirement",
				9,
				2
		);

		// Create replica One
		GridReplica replicaOne = gridService
				.createReplica(admin.getId(), new CreateReplicaRequest().setGridSessionId(gridSession.getSessionId()))
				.getReplica();

		String urlOne = gridService
				.createPresignedUrl(admin.getId(), new CreateGridPresignedUrlRequest()
						.setGridSessionId(gridSession.getSessionId()).setReplicaId(replicaOne.getReplicaId()))
				.getPresignedUrl();
		assertNotNull(urlOne);
		BlockingQueue<String> incomingMessages = new LinkedBlockingQueue<>();
		WebSocket websoceket = asynchronousJobWorkerHelper.createConnection(urlOne, incomingMessages);

		GridAgentSessionContext context = new GridAgentSessionContext().setGridSessionId(replicaOne.getGridSessionId())
				.setUsersReplicaId(replicaOne.getReplicaId());
		AgentSession agentSession = agentService.createSession(admin.getId(), new CreateAgentSessionRequest()
				.setSessionContext(context).setAgentAccessLevel(AgentAccessLevel.WRITE_YOUR_PRIVATE_DATA));
		assertNotNull(agentSession);
		assertEquals(context, agentSession.getSessionContext());
		assertNotNull(context.getAgentsReplicaId());

		String chatRequest = "Can you help me understand the validation error: '#/a: expected type: Integer, found: Null'?";
		// the agent is expected to read the grid's schema and help the user understand
		// the error
		asynchronousJobWorkerHelper
				.assertJobResponse(admin, new AgentChatRequest().setSessionId(agentSession.getSessionId())
						.setChatText(chatRequest).setEnableTrace(true), (AgentChatResponse response) -> {
							assertNotNull(response);
							assertEquals(agentSession.getSessionId(), response.getSessionId());
							assertNotNull(response.getResponseText());
							System.out.println(response.getResponseText());
							assertTrue(response.getResponseText().toLowerCase().contains("schema"));
							assertTrue(response.getResponseText().toLowerCase().contains("null"));
							assertTrue(response.getResponseText().toLowerCase().contains("integer"));
						}, MAX_WAIT_MS)
				.getResponse();

		chatRequest = "How many rows have a validation error?";
		asynchronousJobWorkerHelper
				.assertJobResponse(admin, new AgentChatRequest().setSessionId(agentSession.getSessionId())
						.setChatText(chatRequest).setEnableTrace(true), (AgentChatResponse response) -> {
							assertNotNull(response);
							assertEquals(agentSession.getSessionId(), response.getSessionId());
							assertNotNull(response.getResponseText());
							System.out.println(response.getResponseText());
							assertTrue(response.getResponseText().toLowerCase().contains("2"));
						}, MAX_WAIT_MS)
				.getResponse();

		// setup the user's selection.
		GridHeader header = gridReplicaViewManager.readHeader(gridSession.getSessionId(), INTERNAL_REPLICA_ID).get();
		List<RowView> rows = gridReplicaViewManager.querySinglePage(header, 100L, 0L);

		JsonRxMessage message = createSetSelectionMessage(header,
				new ReplicaSelectionModel()
						.setRowSelection(List.of(RowView.createCrdtIdFromLogical(rows.get(2).getArrNodeId())))
						// Manually select all the columns
						.setColumnSelection(header.getOrderedColumns().stream().map(Column::getColumnOrderNodeId)
								.collect(Collectors.toList())),
				new LogicalTimestamp().setReplicaId(context.getUsersReplicaId()).setSequenceNumber(1L));
		websoceket.send(message.toJson());
		asynchronousJobWorkerHelper.waitForMessage((a) -> a.optInt(0) == 5 && a.optInt(1) == message.getId().get(),
				incomingMessages);
		TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
			ReplicaSelectionModel curSelection = gridReplicaViewManager
					.readHeader(gridSession.getSessionId(), INTERNAL_REPLICA_ID, context.getUsersReplicaId()).get()
					.getReplicaSelectionModel();
			return Pair.create(curSelection != null, null);
		});

		chatRequest = "I want to focus on my currently selected row.  Why is this row invalid?";
		asynchronousJobWorkerHelper
				.assertJobResponse(admin, new AgentChatRequest().setSessionId(agentSession.getSessionId())
						.setChatText(chatRequest).setEnableTrace(true), (AgentChatResponse response) -> {
							assertNotNull(response);
							assertEquals(agentSession.getSessionId(), response.getSessionId());
							assertNotNull(response.getResponseText());
							System.out.println(response.getResponseText());
							assertTrue(response.getResponseText().toLowerCase().contains("null"));
						}, MAX_WAIT_MS)
				.getResponse();

		chatRequest = "Can you fix my currently selected row by setting a=4 and b to null (using the rowId)?";
		AgentChatResponse acr = asynchronousJobWorkerHelper
				.assertJobResponse(admin, new AgentChatRequest().setSessionId(agentSession.getSessionId())
						.setChatText(chatRequest).setEnableTrace(true), (AgentChatResponse response) -> {
							assertNotNull(response);
							assertEquals(agentSession.getSessionId(), response.getSessionId());
							assertNotNull(response.getResponseText());
							System.out.println(response.getResponseText());
						}, MAX_WAIT_MS)
				.getResponse();
		// the agent is likely to ask if it should proceed....
		if (acr.getResponseText().contains("?")) {
			chatRequest = "You may proceed with making the change.";
			acr = asynchronousJobWorkerHelper
					.assertJobResponse(admin, new AgentChatRequest().setSessionId(agentSession.getSessionId())
							.setChatText(chatRequest).setEnableTrace(true), (AgentChatResponse response) -> {
								assertNotNull(response);
								assertEquals(agentSession.getSessionId(), response.getSessionId());
								assertNotNull(response.getResponseText());
								System.out.println(response.getResponseText());
							}, MAX_WAIT_MS)
					.getResponse();
		}

		TimeUtils.waitFor(MAX_WAIT_MS, 2000L, () -> {
			System.out.println("Waiting for agent's changes to appear...");
			GridHeader h = gridReplicaViewManager
					.readHeader(gridSession.getSessionId(), INTERNAL_REPLICA_ID, context.getUsersReplicaId()).get();
			List<RowView> r = gridReplicaViewManager.querySinglePage(h,
					List.of(new RowSelectionFilterElement().setFilterSelected(true)), 100L, 0L);
			System.out.println("row count: " + r.size());
			if (r.size() == 1 && r.get(0) != null) {
				JSONObject rowData = r.get(0).getRowObject().getData().getRowJsonDocument();
				System.out.println("current row cells: " + rowData.toString());
				if (rowData.opt("a").equals(4L) && rowData.isNull("b")) {
					return Pair.create(true, null);
				}
			}
			return Pair.create(false, null);
		});

		chatRequest = "Can you provide the value of column b for the currently selected row?";

		String jobId = asynchronousJobWorkerHelper
				.assertJobResponse(admin, new AgentChatRequest().setSessionId(agentSession.getSessionId())
						.setChatText(chatRequest).setEnableTrace(true), (AgentChatResponse response) -> {
							assertNotNull(response);
							assertEquals(agentSession.getSessionId(), response.getSessionId());
							assertNotNull(response.getResponseText());
							System.out.println(response.getResponseText());
							assertTrue(response.getResponseText().toLowerCase().contains("null"));
						}, MAX_WAIT_MS)
				.getJobToken();

		String jobTraceText = agentService.getChatTrace(admin.getId(), new TraceEventsRequest().setJobId(jobId))
				.getPage().stream().map(TraceEvent::getMessage).reduce(String::concat).orElseThrow();

		// Verifies that the agent used a SelectByName to get the value of column b
		assertTrue(jobTraceText.contains("org.sagebionetworks.repo.model.grid.query.SelectByName"));

		chatRequest = "Can you now provide the values for the first row for the columns that the user selected?";

		jobId = asynchronousJobWorkerHelper
				.assertJobResponse(admin, new AgentChatRequest().setSessionId(agentSession.getSessionId())
						.setChatText(chatRequest).setEnableTrace(true), (AgentChatResponse response) -> {
							assertNotNull(response);
							assertEquals(agentSession.getSessionId(), response.getSessionId());
							assertNotNull(response.getResponseText());
							System.out.println(response.getResponseText());
							assertTrue(response.getResponseText().toLowerCase().contains("one"));
						}, MAX_WAIT_MS)
				.getJobToken();

		jobTraceText = agentService.getChatTrace(admin.getId(), new TraceEventsRequest().setJobId(jobId)).getPage()
				.stream().map(TraceEvent::getMessage).reduce(String::concat).orElseThrow();

		// Verifies that the agent used a SelectSelection in its query
		assertTrue(jobTraceText.contains("org.sagebionetworks.repo.model.grid.query.SelectSelection"));

		chatRequest = "I would like to do a batch of updates, for the row where 'a'=99 set the value of 'b' to be 'a was 99' and for the row where 'a'=101 set 'b' to be 'a was 101'";
		acr = asynchronousJobWorkerHelper
				.assertJobResponse(admin, new AgentChatRequest().setSessionId(agentSession.getSessionId())
						.setChatText(chatRequest).setEnableTrace(true), (AgentChatResponse response) -> {
							assertNotNull(response);
							assertEquals(agentSession.getSessionId(), response.getSessionId());
							assertNotNull(response.getResponseText());
							System.out.println(response.getResponseText());
						}, MAX_WAIT_MS)
				.getResponse();
		// the agent is likely to ask if it should proceed....
		if (acr.getResponseText().contains("?")) {
			chatRequest = "You may proceed with making the change.";
			acr = asynchronousJobWorkerHelper
					.assertJobResponse(admin, new AgentChatRequest().setSessionId(agentSession.getSessionId())
							.setChatText(chatRequest).setEnableTrace(true), (AgentChatResponse response) -> {
								assertNotNull(response);
								assertEquals(agentSession.getSessionId(), response.getSessionId());
								assertNotNull(response.getResponseText());
								System.out.println(response.getResponseText());
							}, MAX_WAIT_MS)
					.getResponse();
		}

		TimeUtils.waitFor(MAX_WAIT_MS, 2000L, () -> {
			System.out.println("Waiting for agent's changes to appear...");
			GridHeader h = gridReplicaViewManager
					.readHeader(gridSession.getSessionId(), INTERNAL_REPLICA_ID, context.getUsersReplicaId()).get();
			QueryResult qr = gridReplicaViewManager.querySinglePageAsQueryResult(h,
					new QueryElement().setWhere(List.of(new CellValueFilterElement().setColumnName("a")
							.setOperator(CellValueOperatorElement.IN).setValue(List.of(99, 101)))));
			String json = JDOSecondaryPropertyUtils.createJSONFromObject(qr);
			System.out.println("Query result: " + json);
			if (json.contains("a was 99") && json.contains("a was 101")) {
				return Pair.create(true, null);
			} else {
				return Pair.create(false, null);
			}
		});
	}

	@Test
	@Disabled // Unstable test, see: PLFM-9487.
	public void testRegularExpression() throws AssertionError, Exception {
		createGridSessionFromCsv(
				new String[] { "firstName", "lastName", "phone", "formattedName", "cleanPhone" },
				List.of(
						new String[] { "John", "Smith", "(555) 123-4567", "Smith, John", "555-123-4567" },
						new String[] { "Alice", "Johnson", "(555) 987-6543", null, null },
						new String[] { "Bob", "Williams", "(555) 456-7890", null, null },
						new String[] { "Carol", "Davis", "(555) 321-0987", null, null },
						new String[] { "David", "Miller", "(555) 789-0123", null, null }
				),
				"schema/PathSchema.json",
				"pathschema",
				5,
				4
		);

		// Create replica One
		GridReplica replicaOne = gridService
				.createReplica(admin.getId(), new CreateReplicaRequest().setGridSessionId(gridSession.getSessionId()))
				.getReplica();

		GridAgentSessionContext context = new GridAgentSessionContext().setGridSessionId(replicaOne.getGridSessionId())
				.setUsersReplicaId(replicaOne.getReplicaId());
		AgentSession agentSession = agentService.createSession(admin.getId(), new CreateAgentSessionRequest()
				.setSessionContext(context).setAgentAccessLevel(AgentAccessLevel.WRITE_YOUR_PRIVATE_DATA));
		assertNotNull(agentSession);
		assertEquals(context, agentSession.getSessionContext());
		assertNotNull(context.getAgentsReplicaId());

		String chatRequest = "Can you help me understand the invalid rows in this grid?";
		asynchronousJobWorkerHelper
				.assertJobResponse(admin, new AgentChatRequest().setSessionId(agentSession.getSessionId())
						.setChatText(chatRequest).setEnableTrace(true), (AgentChatResponse response) -> {
							assertNotNull(response);
							assertEquals(agentSession.getSessionId(), response.getSessionId());
							assertNotNull(response.getResponseText());
							System.out.println(response.getResponseText());
							String responseLower = response.getResponseText().toLowerCase();
							assertTrue(responseLower.contains("schema") || responseLower.contains("formattedname")
									|| responseLower.contains("require"));
						}, MAX_WAIT_MS)
				.getResponse();

		chatRequest = "For all rows where 'formattedName' is null, please set 'formattedName' to the pattern 'LastName, FirstName' (combining lastName and firstName columns). Also, for rows where 'cleanPhone' is null, transform the phone number by removing parentheses and spaces to create a clean format like '555-123-4567'. See the first row as an example of the expected output.";
		AgentChatResponse acr = asynchronousJobWorkerHelper
				.assertJobResponse(admin, new AgentChatRequest().setSessionId(agentSession.getSessionId())
						.setChatText(chatRequest).setEnableTrace(true), (AgentChatResponse response) -> {
							assertNotNull(response);
							assertEquals(agentSession.getSessionId(), response.getSessionId());
							assertNotNull(response.getResponseText());
							System.out.println(response.getResponseText());
						}, MAX_WAIT_MS)
				.getResponse();

		// the agent is likely to ask if it should proceed....
		if (acr.getResponseText().contains("?")) {
			chatRequest = "You may proceed with making all necessary changes.";
			acr = asynchronousJobWorkerHelper
					.assertJobResponse(admin, new AgentChatRequest().setSessionId(agentSession.getSessionId())
							.setChatText(chatRequest).setEnableTrace(true), (AgentChatResponse response) -> {
								assertNotNull(response);
								assertEquals(agentSession.getSessionId(), response.getSessionId());
								assertNotNull(response.getResponseText());
								System.out.println(response.getResponseText());
							}, MAX_WAIT_MS)
					.getResponse();
		}

		TimeUtils.waitFor(MAX_WAIT_MS, 2000L, () -> {
			System.out.println("Waiting for agent's changes to appear...");
			GridHeader h = gridReplicaViewManager
					.readHeader(gridSession.getSessionId(), INTERNAL_REPLICA_ID, context.getUsersReplicaId()).get();
			// all rows should be valid after the change
			QueryResult qr = gridReplicaViewManager.querySinglePageAsQueryResult(h,
					new QueryElement().setWhere(List.of(new RowIsValidFilterElement().setValue(true))));
			String json = JDOSecondaryPropertyUtils.createJSONFromObject(qr);
			System.out.println("Query result: " + json);
			if (qr.getRows().size() == 5) {
				JSONObject lastRow = (JSONObject) qr.getRows().get(4).getData();
				String lastRowJson = lastRow.toString();
				System.out.println("lastRow: " + lastRowJson);
				String expected =  "{\"firstName\":\"David\",\"lastName\":\"Miller\",\"phone\":\"(555) 789-0123\",\"formattedName\":\"Miller, David\",\"cleanPhone\":\"555-789-0123\"}";
				return Pair.create(expected.equals(lastRow.toString()), null);
			} else {
				return Pair.create(false, null);
			}
		});
	}

	@Test
	public void testArrays() throws Exception {
		createGridSessionFromCsv(
				new String[] { "arrayColumn", },
				List.of(
						new String[] { "abc", },
						new String[] { "def" },
						new String[] { "ghi" },
						new String[] { "jkl" }
				),
				"schema/ArrayProperty.json",
				"arrayproperty",
				4,
				4
		);

		GridReplica replicaOne = gridService
				.createReplica(admin.getId(), new CreateReplicaRequest().setGridSessionId(gridSession.getSessionId()))
				.getReplica();

		String urlOne = gridService
				.createPresignedUrl(admin.getId(), new CreateGridPresignedUrlRequest()
						.setGridSessionId(gridSession.getSessionId()).setReplicaId(replicaOne.getReplicaId()))
				.getPresignedUrl();
		assertNotNull(urlOne);
		BlockingQueue<String> incomingMessages = new LinkedBlockingQueue<>();
		asynchronousJobWorkerHelper.createConnection(urlOne, incomingMessages);

		GridAgentSessionContext context = new GridAgentSessionContext().setGridSessionId(replicaOne.getGridSessionId())
				.setUsersReplicaId(replicaOne.getReplicaId());
		AgentSession agentSession = agentService.createSession(admin.getId(), new CreateAgentSessionRequest()
				.setSessionContext(context).setAgentAccessLevel(AgentAccessLevel.WRITE_YOUR_PRIVATE_DATA));
		assertNotNull(agentSession);
		assertEquals(context, agentSession.getSessionContext());
		assertNotNull(context.getAgentsReplicaId());

		String chatRequest = "Set the arrayColumn column to be \"abc\", \"xyz\" for all rows in the grid";
		AgentChatResponse acr = asynchronousJobWorkerHelper
				.assertJobResponse(admin, new AgentChatRequest().setSessionId(agentSession.getSessionId())
						.setChatText(chatRequest).setEnableTrace(true), (AgentChatResponse response) -> {
					assertNotNull(response);
					assertEquals(agentSession.getSessionId(), response.getSessionId());
					assertNotNull(response.getResponseText());
					System.out.println(response.getResponseText());
				}, MAX_WAIT_MS)
				.getResponse();
		// the agent is likely to ask if it should proceed....
		if (acr.getResponseText().contains("?")) {
			chatRequest = "You may proceed with making the change.";
			acr = asynchronousJobWorkerHelper
					.assertJobResponse(admin, new AgentChatRequest().setSessionId(agentSession.getSessionId())
							.setChatText(chatRequest).setEnableTrace(true), (AgentChatResponse response) -> {
						assertNotNull(response);
						assertEquals(agentSession.getSessionId(), response.getSessionId());
						assertNotNull(response.getResponseText());
						System.out.println(response.getResponseText());
					}, MAX_WAIT_MS)
					.getResponse();
		}

		TimeUtils.waitFor(MAX_WAIT_MS, 2000L, () -> {
			System.out.println("Waiting for agent's changes to appear...");
			GridHeader h = gridReplicaViewManager
					.readHeader(gridSession.getSessionId(), INTERNAL_REPLICA_ID, context.getUsersReplicaId()).get();
			QueryResult qr = gridReplicaViewManager.querySinglePageAsQueryResult(h,
					new QueryElement().setWhere(List.of(new CellValueFilterElement().setColumnName("arrayColumn")
							.setOperator(CellValueOperatorElement.EQUALS).setValue(List.of("abc", "xyz")))));
			if (qr.getRows().size() == 4L) {
				return Pair.create(true, null);
			} else {
				return Pair.create(false, null);
			}
		});
	}

	public JsonRxMessage createSetSelectionMessage(GridHeader header, ReplicaSelectionModel selection,
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
		patch.addNewOperation(
				new InsertObjectBuilder().setObjectId(rootObjId).setMap(Map.of("selection", selectionObj)));
		return new JsonRxMessage(JsonRxMessageType.RequestData).setBody(PatchCompactSerializable.serialize(patch))
				.setId(987).setMethod("patch");
	}

	/**
	 * Helper to create a schema
	 * 
	 * @return
	 * @throws Exception
	 */
	String createJsonSchema(String schemaPath, String shortName) throws Exception {
		Organization org = asynchronousJobWorkerHelper.getOrCreateOrganization(admin.getId(),
				"GridAgentChatWorkerIntegrationTest");
		JsonSchema jsonSchema = JsonEntityUtils.fromJsonString(ClasspathUtil.loadFromClasspath(schemaPath),
				JsonSchema.class);
		jsonSchema.set$id(org.getName() + "-" + shortName);

		return asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateSchemaRequest().setDryRun(false).setSchema(jsonSchema), (CreateSchemaResponse response) -> {
					assertNotNull(response);
					assertNotNull(response.getNewVersionInfo());
					assertNotNull(response.getNewVersionInfo().get$id());
				}, MAX_WAIT_MS).getResponse().getNewVersionInfo().get$id();
	}


}
