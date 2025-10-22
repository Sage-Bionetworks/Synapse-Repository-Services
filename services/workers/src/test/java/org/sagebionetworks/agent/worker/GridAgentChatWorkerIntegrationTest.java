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

import org.java_websocket.WebSocket;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
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
import org.sagebionetworks.repo.model.grid.CrdtId;
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
	private GridService gridServie;
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
		File temp = File.createTempFile("GridScaleIntegrationTest", ".csv", null);

		csvDescriptor = new CsvTableDescriptor().setIsFirstLineHeader(true);
		try (CSVWriter writer = new CSVWriterProviderImpl().createWriter(new FileWriter(temp), csvDescriptor)) {
			writer.writeNext(new String[] { "a", "b" });
			writer.writeNext(new String[] { "1", "one" });
			writer.writeNext(new String[] { "2" });
			writer.writeNext(new String[] { null, "no id" });
			writer.writeNext(new String[] { "3", "true" });
			writer.writeNext(new String[] { "98", "ninety eight" });
			writer.writeNext(new String[] { "99" });
			writer.writeNext(new String[] { "101" });
			writer.writeNext(new String[] { "102", "one hundred two" });
			writer.writeNext(new String[] { "103", "3.41" });
		}

		S3FileHandle fh = fileHandleManager.uploadLocalFile(new LocalFileUploadRequest().withFileToUpload(temp)
				.withContentType("text/csv").withFileName(temp.getName()).withUserId(admin.getId().toString()));
		temp.delete();

		project = entityService.createEntity(admin.getId(), new Project().setName("GridScaleIntegrationTest"), null);

		recordSet = entityService.createEntity(admin.getId(), new RecordSet().setName("aRecordSet")
				.setParentId(project.getId()).setDataFileHandleId(fh.getId()).setUpsertKey(List.of("a")), null);

		schema$id = createJsonSchema();

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
			if (rows.size() != 9 || invalidRows != 4) {
				return Pair.create(false, null);
			}
			return Pair.create(true, header.get());
		});

	}

	@Test
	public void testViewWithSchemaAndAgentChat() throws Exception {

		// Create replica One
		GridReplica replicaOne = gridServie
				.createReplica(admin.getId(), new CreateReplicaRequest().setGridSessionId(gridSession.getSessionId()))
				.getReplica();

		String urlOne = gridServie
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
							assertTrue(response.getResponseText().toLowerCase().contains("4"));
						}, MAX_WAIT_MS)
				.getResponse();

		// setup the user's selection.
		GridHeader header = gridReplicaViewManager.readHeader(gridSession.getSessionId(), INTERNAL_REPLICA_ID).get();
		List<RowView> rows = gridReplicaViewManager.querySinglePage(header, 100L, 0L);

		JsonRxMessage message = createSetSelectionMessage(header,
				new ReplicaSelectionModel()
						.setRowSelection(List.of(RowView.createCrdtIdFromLogical(rows.get(2).getArrNodeId()))),
				new LogicalTimestamp().setReplicaId(context.getUsersReplicaId()).setSequenceNumber(1L));
		websoceket.send(message.toJson());
		asynchronousJobWorkerHelper.waitForMessage((a) -> a.optInt(0) == 5 && a.optInt(1) == message.getId().get(),
				incomingMessages);
		TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
			ReplicaSelectionModel curSelection = gridReplicaViewManager
					.readHeader(gridSession.getSessionId(), INTERNAL_REPLICA_ID, context.getUsersReplicaId()).get().getReplicaSelectionModel();
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
							assertTrue(response.getResponseText().toLowerCase().contains("no id"));
						}, MAX_WAIT_MS)
				.getResponse();
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
	String createJsonSchema() throws Exception {
		Organization org = asynchronousJobWorkerHelper.getOrCreateOrganization(admin.getId(),
				"GridAgentChatWorkerIntegrationTest");
		JsonSchema jsonSchema = JsonEntityUtils.fromJsonString(
				ClasspathUtil.loadFromClasspath("schema/ConditionalRequirement.json"), JsonSchema.class);
		jsonSchema.set$id(org.getName() + "-conditionalrequirement");

		return asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateSchemaRequest().setDryRun(false).setSchema(jsonSchema), (CreateSchemaResponse response) -> {
					assertNotNull(response);
					assertNotNull(response.getNewVersionInfo());
					assertNotNull(response.getNewVersionInfo().get$id());
				}, MAX_WAIT_MS).getResponse().getNewVersionInfo().get$id();
	}

}
