package org.sagebionetworks.grid.workers;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.sagebionetworks.repo.model.util.AccessControlListUtil.createResourceAccess;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.http.entity.ContentType;
import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.query.QueryElement;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisher;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.dbo.schema.EntitySchemaValidationResultDao;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.grid.CreateGridPresignedUrlRequest;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.CreateGridResponse;
import org.sagebionetworks.repo.model.grid.CreateReplicaRequest;
import org.sagebionetworks.repo.model.grid.DownloadFromGridRequest;
import org.sagebionetworks.repo.model.grid.DownloadFromGridResult;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridCsvImportRequest;
import org.sagebionetworks.repo.model.grid.GridCsvImportResponse;
import org.sagebionetworks.repo.model.grid.GridRecordSetExportRequest;
import org.sagebionetworks.repo.model.grid.GridRecordSetExportResponse;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.GridUtils;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.repo.model.grid.patch.Timespan;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;
import org.sagebionetworks.repo.model.grid.patch.compact.PatchCompactSerializable;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.InsertVectorBuilder;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.NewConstantBuilder;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.Operations;
import org.sagebionetworks.repo.model.helper.AccessControlListObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.model.schema.ValidationSummaryStatistics;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.service.EntityService;
import org.sagebionetworks.repo.service.GridService;
import org.sagebionetworks.table.cluster.utils.CSVUtils;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.amazonaws.services.s3.model.GetObjectRequest;

import au.com.bytecode.opencsv.CSVReader;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class GridEventBrokerWorkerIntegrationTest {

	private static final long INTERNAL_REPLICA_ID = 66534L;

	public static final long MAX_WAIT_MS = 120_000;

	/**
	 * The number of secondary websocket connections to create for measuring patch broadcast performance.
	 */
	private static final int SECONDARY_CONNECTION_COUNT = 5;

	/**
	 * Helper class to hold a secondary connection's replica, websocket, and message queue together.
	 */
	static class SecondaryConnection {
		private final GridReplica replica;
		private final WebSocket webSocket;
		private final BlockingQueue<String> incomingMessages;

		SecondaryConnection(GridReplica replica, WebSocket webSocket, BlockingQueue<String> incomingMessages) {
			this.replica = replica;
			this.webSocket = webSocket;
			this.incomingMessages = incomingMessages;
		}

		GridReplica replica() {
			return replica;
		}

		WebSocket webSocket() {
			return webSocket;
		}

		BlockingQueue<String> incomingMessages() {
			return incomingMessages;
		}
	}

	@Autowired
	private GridService gridService;

	@Autowired
	private UserManager userManager;

	@Autowired
	private AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private EntityService entityService;

	@Autowired
	private ColumnModelManager columnManager;

	@Autowired
	private FileHandleManager fileHandleManager;

	@Autowired
	private GridReplicaViewManager gridViewManager;

	@Autowired
	private SynapseS3Client s3Client;

	@Autowired
	private JsonSchemaManager jsonSchemaManager;

	@Autowired
	private EntitySchemaValidationResultDao schemaValidationResultDao;
	
	@Autowired
	private TeamManager teamManager;
	
	@Autowired
	private AccessControlListObjectHelper aclHelper;
	
	@Autowired
	private GridDao gridDao;
	
	@Autowired
	private GridIndexDao gridIndexDao;
	
	@Autowired
	private RepositoryMessagePublisher repositoryMessagePublisher;

	@Autowired
	private DBOChangeDAO changeDao;

	private UserInfo admin;

	@BeforeEach
	public void before() {
		admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		jsonSchemaManager.truncateAll();
		entityManager.truncateAll();
		schemaValidationResultDao.truncateAll();
	}

	@AfterEach
	public void after() {
		jsonSchemaManager.truncateAll();
		entityManager.truncateAll();
		schemaValidationResultDao.truncateAll();
	}

	@Test
	public void testPingGrid()
			throws InterruptedException, AssertionError, AsynchJobFailedException, URISyntaxException {

		// Create a grid session.
		GridSession session = asynchronousJobWorkerHelper
				.assertJobResponse(admin, new CreateGridRequest(), (CreateGridResponse response) -> {
					assertNotNull(response);
					assertNotNull(response.getGridSession());
				}, MAX_WAIT_MS).getResponse().getGridSession();

		// Create a replica
		GridReplica replica = gridService
				.createReplica(admin.getId(), new CreateReplicaRequest().setGridSessionId(session.getSessionId()))
				.getReplica();

		String presignedUrl = gridService
				.createPresignedUrl(admin.getId(), new CreateGridPresignedUrlRequest()
						.setGridSessionId(session.getSessionId()).setReplicaId(replica.getReplicaId()))
				.getPresignedUrl();
		assertNotNull(presignedUrl);

		BlockingQueue<String> incomingMessages = new LinkedBlockingQueue<>();
		WebSocket ws = createConnection(presignedUrl, incomingMessages);

		waitForConnected(incomingMessages);
		// send a ping
		ws.send(new JSONArray("[8,\"ping\"]").toString());
		assertTrue(waitForMessage((a) -> a.optInt(0) == 8 && "pong".equals(a.optString(1)), incomingMessages));
		ws.close();

	}

	void waitForConnected(BlockingQueue<String> incomingMessages) throws InterruptedException {
		assertTrue(waitForMessage((a) -> a.optInt(0) == 8 && "connected".equals(a.optString(1)), incomingMessages));
	}

	@Test
	public void testPatch() throws AssertionError, Exception {
		// Create a grid session.
		GridSession session = asynchronousJobWorkerHelper
				.assertJobResponse(admin, new CreateGridRequest(), (CreateGridResponse response) -> {
					assertNotNull(response);
					assertNotNull(response.getGridSession());
				}, MAX_WAIT_MS).getResponse().getGridSession();

		// Create replica One (the primary replica that sends patches)
		GridReplica replicaOne = gridService
				.createReplica(admin.getId(), new CreateReplicaRequest().setGridSessionId(session.getSessionId()))
				.getReplica();

		String urlOne = gridService
				.createPresignedUrl(admin.getId(), new CreateGridPresignedUrlRequest()
						.setGridSessionId(session.getSessionId()).setReplicaId(replicaOne.getReplicaId()))
				.getPresignedUrl();
		assertNotNull(urlOne);

		BlockingQueue<String> incomingMessagesOne = new LinkedBlockingQueue<>();
		WebSocket wsOne = createConnection(urlOne, incomingMessagesOne);
		waitForConnected(incomingMessagesOne);

		// Create n secondary connections to measure patch broadcast performance
		List<SecondaryConnection> secondaryConnections = createSecondaryConnections(session, SECONDARY_CONNECTION_COUNT);

		try {
			// Replica one sends a patch.
			String patchBody = String.format("[[[%d,1]],[0]]", replicaOne.getReplicaId());
			String patchRequest = String.format("[1,101,\"patch\", %s]", patchBody);
			wsOne.send(patchRequest);

			// Wait for response complete: [5,101]
			assertTrue(waitForMessage((a) -> a.optInt(0) == 5 && a.optInt(1) == 101, incomingMessagesOne));

			// send a second patch;
			patchBody = String.format("[[[%d,4]],[0]]", replicaOne.getReplicaId());
			patchRequest = String.format("[1,102,\"patch\", %s]", patchBody);
			wsOne.send(patchRequest);

			// Wait for response complete: [5,102]
			assertTrue(waitForMessage((a) -> a.optInt(0) == 5 && a.optInt(1) == 102, incomingMessagesOne));

			// All secondary replicas should be notified of two patches
			for (SecondaryConnection conn : secondaryConnections) {
				assertTrue(waitForMessage((a) -> a.optInt(0) == 8 && "new-patch".equals(a.optString(1)), conn.incomingMessages()),
						"Secondary connection " + conn.replica().getReplicaId() + " should receive first new-patch notification");
				assertTrue(waitForMessage((a) -> a.optInt(0) == 8 && "new-patch".equals(a.optString(1)), conn.incomingMessages()),
						"Secondary connection " + conn.replica().getReplicaId() + " should receive second new-patch notification");
			}

			// Use the first secondary connection for synchronization test
			SecondaryConnection firstSecondary = secondaryConnections.get(0);

			// First secondary's clock is currently empty so start a synchronize.
			firstSecondary.webSocket().send("[1,99,\"synchronize-clock\",[]]");

			List<LogicalTimestamp> clock = new ArrayList<>();
			assertTrue(waitForMessage((a) -> {
				if (a.optInt(0) == 4 && a.optInt(1) == 99) {
					JSONObject body = a.getJSONObject(2);
					assertEquals("patches", body.getString("type"), "Expected multiple patches to be returned");
					JSONArray patches = body.getJSONArray("body");
					Patch lastPatch = PatchCompactSerializable.deserialize(patches.getJSONArray(patches.length() - 1));
					clock.add(LogicalTimestamp.newIncrement(lastPatch.getPatchId(), lastPatch.getSpan()));
					return true;
				} else {
					return false;
				}
			}, firstSecondary.incomingMessages()));

			// after the second sync, first secondary should be up-to-date.
			String newClock = LogicalTimestampCompactSerializable.serializeClock(clock).toString();
			firstSecondary.webSocket().send(String.format("[1,99,\"synchronize-clock\",%s]", newClock));

			assertTrue(waitForMessage((a) -> a.optInt(0) == 5 && a.optInt(1) == 99, firstSecondary.incomingMessages()));
		} finally {
			// Clean up all connections
			wsOne.close();
			closeAllConnections(secondaryConnections);
		}
		
		TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
			List<GridConnectionInfo> webSocketConnections = gridDao.listConnections(session.getSessionId()).stream()
					.filter(c -> EventSource.WEBSOCKET.equals(c.getSource())).collect(Collectors.toList());
			System.out.println(
					"waiting for connections to drain. Current connection count: " + webSocketConnections.size());
			return Pair.create(webSocketConnections.size() == 0, null);
		});
		
	}

	/**
	 * This is a test for PLFM-9488. For a grid session that starts on production
	 * and is migrated to staging, the staging session needs a way to trigger the
	 * patch synchronization process to apply migrated patches. In order to address
	 * this issue gridManager.savePatch() was extends to push a grid_session change
	 * message. The ChangeSentMessageSynchWorker will re-broacast this message on
	 * staging which is then picked up by the GridSessionIndexWorker. The
	 * GridSessionIndexWorker will then trigger the patch synchronization process
	 * for the associated grid session.
	 * @throws AsynchJobFailedException 
	 * @throws Exception 
	 */
	@Test
	public void testGridWithNoActiveSession() throws Exception {

		String projectId = entityManager.createEntity(admin, new Project().setName("test"), null);
		List<ColumnModel> schema = List.of(new ColumnModel().setName("anInt").setColumnType(ColumnType.INTEGER));
		schema = columnManager.createColumnModels(admin, schema);
		List<String> colIds = schema.stream().map(c -> c.getId()).collect(Collectors.toList());

		TableEntity table = asynchronousJobWorkerHelper.createTable(admin, "testTable", projectId, colIds, false);

		List<Row> rows = List.of(new Row().setValues(List.of("7070")), new Row().setValues(List.of("8080")),
				new Row().setValues(List.of("9090")));

		asynchronousJobWorkerHelper.appendRowsToTable(admin, schema, table.getId(), rows, MAX_WAIT_MS);

		String sql = String.format("select * from %s", table.getId());

		// create a grid using the table
		GridSession session = asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateGridRequest().setInitialQuery(new Query().setSql(sql)), (CreateGridResponse response) -> {
					assertNotNull(response);
					assertNotNull(response.getGridSession());
				}, MAX_WAIT_MS).getResponse().getGridSession();

		assertNotNull(session);
		assertEquals(table.getId(), session.getSourceEntityId());

		// Wait for the internal replica to be created for this grid session.
		TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
			Optional<GridHeader> op = gridViewManager.readHeader(session.getSessionId(), INTERNAL_REPLICA_ID);
			if (op.isEmpty()) {
				return Pair.create(false, null);
			} else {
				List<RowView> viewRows = gridViewManager.querySinglePage(op.get(), new QueryElement());
				System.out.println("waiting for view header before truncate...");
				return Pair.create(viewRows.size() == 3, viewRows);
			}
		});

		// delete all replica data simulating an empty staging stack.
		gridIndexDao.truncateAll();

		// Trigger the same type of event sent by ChangeSentMessageSynchWorker
		ChangeMessage change = changeDao.replaceChange(
				new ChangeMessage().setChangeType(ChangeType.UPDATE).setObjectType(ObjectType.GRID_SESSION)
						.setObjectId(GridUtils.gridSessionIdAsLong(session.getSessionId()).toString()));
		repositoryMessagePublisher.publishBatchToTopic(ObjectType.GRID_SESSION, List.of(change));

		// the internal replica should be recreated.
		TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
			Optional<GridHeader> op = gridViewManager.readHeader(session.getSessionId(), INTERNAL_REPLICA_ID);
			if (op.isEmpty()) {
				System.out.println("Waiting for grid header to be restored...");
				return Pair.create(false, null);
			} else {
				List<RowView> viewRows = gridViewManager.querySinglePage(op.get(), new QueryElement());
				System.out.println("Waiting for three rows, current count: "+viewRows.size());
				return Pair.create(viewRows.size() == 3, viewRows);
			}
		});
	}

	@Test
	public void testGridWithTableQuery() throws Exception {
		// setup a table
		String projectId = entityManager.createEntity(admin, new Project().setName("test"), null);
		List<ColumnModel> schema = List.of(new ColumnModel().setName("anInt").setColumnType(ColumnType.INTEGER));
		schema = columnManager.createColumnModels(admin, schema);
		List<String> colIds = schema.stream().map(c -> c.getId()).collect(Collectors.toList());

		TableEntity table = asynchronousJobWorkerHelper.createTable(admin, "testTable", projectId, colIds, false);

		List<Row> rows = List.of(
			new Row().setValues(List.of("7070")),
			new Row().setValues(List.of("8080")),
			new Row().setValues(List.of("9090"))
		);

		asynchronousJobWorkerHelper.appendRowsToTable(admin, schema, table.getId(), rows, MAX_WAIT_MS);

		String sql = String.format("select * from %s", table.getId());

		// create a grid using the table
		GridSession session = asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateGridRequest().setInitialQuery(new Query().setSql(sql)), (CreateGridResponse response) -> {
					assertNotNull(response);
					assertNotNull(response.getGridSession());
				}, MAX_WAIT_MS).getResponse().getGridSession();

		assertNotNull(session);
		assertEquals(table.getId(), session.getSourceEntityId());

		// Create replica One
		GridReplica replicaOne = gridService
				.createReplica(admin.getId(), new CreateReplicaRequest().setGridSessionId(session.getSessionId()))
				.getReplica();

		String urlOne = gridService
				.createPresignedUrl(admin.getId(), new CreateGridPresignedUrlRequest()
						.setGridSessionId(session.getSessionId()).setReplicaId(replicaOne.getReplicaId()))
				.getPresignedUrl();
		assertNotNull(urlOne);

		BlockingQueue<String> incomingMessagesOne = new LinkedBlockingQueue<>();
		WebSocket wsOne = createConnection(urlOne, incomingMessagesOne);
		waitForConnected(incomingMessagesOne);

		// start the synchronize - expect a snapshot since this grid was created from a query
		wsOne.send("[1,99,\"synchronize-clock\",[]]");

		assertTrue(waitForMessage((a) -> {
			if (a.optInt(0) == 4 && a.optInt(1) == 99) {
				JSONObject body = a.getJSONObject(2);
				assertEquals("snapshot", body.getString("type"), "Expected snapshot for grid created from query");
				assertNotNull(body.getString("body"), "Snapshot URL should be present");
				return true;
			} else {
				return false;
			}
		}, incomingMessagesOne));

		GridHeader header = TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () ->
			gridViewManager.readHeader(session.getSessionId(), INTERNAL_REPLICA_ID)
				.map(h -> Pair.create(true, h))
				.orElse(Pair.create(false, null))
		);

		List<RowView> rowsView = TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
			List<RowView> page = gridViewManager.querySinglePage(header, 100L, 0L);
			if (page.size() == 3) {
				return Pair.create(true, page);
			}
			return Pair.create(false, null);
		});

		// Deletes the first two rows:
		Patch updatePatch = new Patch()
			.setPatchId(new LogicalTimestamp().setReplicaId(replicaOne.getReplicaId()).setSequenceNumber(25L));

		updatePatch.addNewOperation(Operations.delete()
			.setNodeId(header.getRowsId())
			.setTimespans(List.of(
				new Timespan(
					// Start node
					rowsView.get(0).getArrNodeId(),
					// Length of the span (Gap between the second and first node seq)
					rowsView.get(1).getArrNodeId().getSequenceNumber() - rowsView.get(0).getArrNodeId().getSequenceNumber() + 1
				))
			)
		);

		JSONArray patchBody = PatchCompactSerializable.serialize(updatePatch);

		wsOne.send(String.format("[1,102,\"patch\", %s]", patchBody.toString()));

		assertTrue(waitForMessage((a) -> a.optInt(0) == 5 && a.optInt(1) == 102, incomingMessagesOne));

		DownloadFromGridRequest csvDownloadRequest = new DownloadFromGridRequest().setSessionId(session.getSessionId())
			.setIncludeEtag(false);

		// Now create and validate the CSV exported form the grid
		List<String[]> csvContents = createAndDownloadCsvFromGrid(csvDownloadRequest);

		assertEquals(2, csvContents.size());
		assertArrayEquals(new String[] { "ROW_ID", "ROW_VERSION", "anInt" }, csvContents.get(0));
		assertArrayEquals(new String[] { "3", "1", "9090" }, csvContents.get(1));
	}

	@Test
	public void testGridWithViewQueryAndBoundSchema() throws Exception {
		// setup a view
		Project project = entityService.createEntity(admin.getId(), new Project().setName("test"), null);
		Folder folder = entityService.createEntity(admin.getId(),
				new Folder().setName("folder").setParentId(project.getId()), null);

		String requiredAnnotationName = "anInt";
		String optionalAnnotationName = "anOptionalString";
		String jsonSchem$id = createJsonSchema(
                Map.of(
                    requiredAnnotationName, new JsonSchema().setType(Type.integer),
                    optionalAnnotationName, new JsonSchema().setType(Type.string)
                ),
				List.of(requiredAnnotationName)
            )
            .getNewVersionInfo().get$id();

		ExternalFileHandle fh = fileHandleManager.createExternalFileHandle(admin, new ExternalFileHandle()
				.setContentType("text/plain").setFileName("foo.bar").setExternalURL("https://something.org"));
		FileEntity file = entityService.createEntity(admin.getId(),
				new FileEntity().setName("file").setParentId(folder.getId()).setDataFileHandleId(fh.getId()), null);

		Annotations annos = entityService.getEntityAnnotations(admin.getId(), file.getId());
		annos.setAnnotations(Map.of(requiredAnnotationName,
				new AnnotationsValue().setType(AnnotationsValueType.LONG).setValue(List.of("9090"))));
		entityService.updateEntityAnnotations(admin.getId(), file.getId(), annos);
		asynchronousJobWorkerHelper.waitForEntityReplication(admin, file.getId(), MAX_WAIT_MS);
		file = (FileEntity) entityService.getEntity(admin.getId(), file.getId());

		// Bind the schema to the file.
		entityService.bindSchemaToEntity(admin.getId(),
				new BindSchemaToEntityRequest().setEntityId(file.getId()).setSchema$id(jsonSchem$id));

		List<ColumnModel> schema = List.of(
				new ColumnModel().setName(requiredAnnotationName).setColumnType(ColumnType.INTEGER),
				new ColumnModel().setName(optionalAnnotationName).setColumnType(ColumnType.STRING)
		);
		schema = columnManager.createColumnModels(admin, schema);
		List<String> colIds = schema.stream().map(c -> c.getId()).collect(Collectors.toList());
		EntityView view = entityService
				.createEntity(
						admin.getId(), new EntityView().setParentId(project.getId()).setName("aView")
								.setColumnIds(colIds).setScopeIds(List.of(folder.getId())).setViewTypeMask(0x01L),
						null);

		String sql = String.format("select * from %s", view.getId());

		// create a grid using the table
		GridSession session = asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateGridRequest().setInitialQuery(new Query().setSql(sql)), (CreateGridResponse response) -> {
					assertNotNull(response);
					assertNotNull(response.getGridSession());
				}, MAX_WAIT_MS).getResponse().getGridSession();
		assertNotNull(session);
		assertEquals(view.getId(), session.getSourceEntityId());
		assertEquals(jsonSchem$id, session.getGridJsonSchema$Id());

		// Create replica One
		GridReplica replicaOne = gridService
				.createReplica(admin.getId(), new CreateReplicaRequest().setGridSessionId(session.getSessionId()))
				.getReplica();

		String urlOne = gridService
				.createPresignedUrl(admin.getId(), new CreateGridPresignedUrlRequest()
						.setGridSessionId(session.getSessionId()).setReplicaId(replicaOne.getReplicaId()))
				.getPresignedUrl();
		assertNotNull(urlOne);

		BlockingQueue<String> incomingMessagesOne = new LinkedBlockingQueue<>();
		WebSocket wsOne = createConnection(urlOne, incomingMessagesOne);
		waitForConnected(incomingMessagesOne);

		// start the synchronize - expect a snapshot since this grid was created from a view query
		wsOne.send("[1,99,\"synchronize-clock\",[]]");
		assertTrue(waitForMessage((a) -> {
			if (a.optInt(0) == 4 && a.optInt(1) == 99) {
				JSONObject body = a.getJSONObject(2);
				assertEquals("snapshot", body.getString("type"), "Expected snapshot for grid created from view query");
				assertNotNull(body.getString("body"), "Snapshot URL should be present");
				return true;
			} else {
				return false;
			}
		}, incomingMessagesOne));

		RowView row = TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
			System.out.println("Waiting for row validation results to change...");
			Optional<GridHeader> header = gridViewManager.readHeader(session.getSessionId(), INTERNAL_REPLICA_ID);
			if (header.isEmpty()) {
				return Pair.create(false, null);
			}
			List<RowView> rows = gridViewManager.querySinglePage(header.get(), 100L, 0L);
			if (rows.size() != 1) {
				return Pair.create(false, null);
			}
			return Pair.create(new ValidationResults().setIsValid(true).equals(rows.get(0).getRowValidationResults()),
					rows.get(0));
		});

		Patch updatePatch = new Patch()
				.setPatchId(new LogicalTimestamp().setReplicaId(replicaOne.getReplicaId()).setSequenceNumber(25L));
		String updateValue = "wrong-type";
		LogicalTimestamp conId = updatePatch
				.addNewOperation(new NewConstantBuilder().setValue(new ConValue(ConType.STRING, updateValue)));
		updatePatch.addNewOperation(new InsertVectorBuilder().setVectorId(row.getRowObject().getData().getVectorId())
				.setMap(Map.of(0, conId)));
		JSONArray patchBody = PatchCompactSerializable.serialize(updatePatch);
		wsOne.send(String.format("[1,102,\"patch\", %s]", patchBody.toString()));
		// Wait for response complete: [5,102]
		assertTrue(waitForMessage((a) -> a.optInt(0) == 5 && a.optInt(1) == 102, incomingMessagesOne));

		TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
			System.out.println("Waiting for row validation results to change...");
			Optional<GridHeader> header = gridViewManager.readHeader(session.getSessionId(), INTERNAL_REPLICA_ID);
			if (header.isEmpty()) {
				return Pair.create(false, null);
			}
			List<RowView> rows = gridViewManager.querySinglePage(header.get(),
					new QueryElement().setIncludeValidationMessages(true));
			if (rows.size() != 1) {
				return Pair.create(false, null);
			}
			return Pair.create(new ValidationResults().setIsValid(false)
					.setAllValidationMessages(List.of("#/anInt: expected type: Integer, found: String"))
					.setValidationErrorMessage("expected type: Integer, found: String")
					.equals(rows.get(0).getRowValidationResults()), rows.get(0));
		});

		DownloadFromGridRequest csvDownloadRequest = new DownloadFromGridRequest().setSessionId(session.getSessionId())
				.setIncludeEtag(true);

		// Create and validate the CSV exported form the grid
		List<String[]> csvContents = createAndDownloadCsvFromGrid(csvDownloadRequest);

		assertEquals(2, csvContents.size());
		assertArrayEquals(new String[] { "ROW_ID", "ROW_VERSION", "etag", "anInt", "anOptionalString" }, csvContents.get(0));
		assertArrayEquals(new String[] { file.getId().substring("syn".length()), file.getVersionNumber().toString(),
				file.getEtag(), updateValue, null }, csvContents.get(1));
	}

	@Test
	public void testGridViewWithTeamOwner() throws Exception {
		UserInfo userOne = createUser();
		UserInfo userTwo = createUser();
		UserInfo userThree = createUser();
		Team curatorsTeam = teamManager.create(admin, new Team().setName(UUID.randomUUID().toString()));
		Long curatorsTeamId = Long.parseLong(curatorsTeam.getId());
		teamManager.addMember(admin, curatorsTeam.getId(), userOne);
		teamManager.addMember(admin, curatorsTeam.getId(), userTwo);

		Project project = entityService.createEntity(admin.getId(), new Project().setName("test"), null);
		Folder folder = entityService.createEntity(admin.getId(),
				new Folder().setName("folder").setParentId(project.getId()), null);

		ExternalFileHandle fh = fileHandleManager.createExternalFileHandle(admin, new ExternalFileHandle()
				.setContentType("text/plain").setFileName("foo.bar").setExternalURL("https://something.org"));

		int fileCount = 6;
		List<FileEntity> files = createFiles(fileCount, folder.getId(), fh.getId());
		
		aclHelper.update(project.getId(),ObjectType.ENTITY, (a) -> {
			a.setId(project.getId());
			a.getResourceAccess().add(createResourceAccess(admin.getId(), ACCESS_TYPE.READ));
			a.getResourceAccess().add(createResourceAccess(admin.getId(), ACCESS_TYPE.CHANGE_PERMISSIONS));
			a.getResourceAccess().add(createResourceAccess(admin.getId(), ACCESS_TYPE.UPDATE));
			a.getResourceAccess().add(createResourceAccess(userOne.getId(), ACCESS_TYPE.READ));
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.READ));
			a.getResourceAccess().add(createResourceAccess(userThree.getId(), ACCESS_TYPE.READ));
			a.getResourceAccess().add(createResourceAccess(curatorsTeamId, ACCESS_TYPE.READ));
			a.getResourceAccess().add(createResourceAccess(curatorsTeamId, ACCESS_TYPE.UPDATE));
		});
		
		aclHelper.create((a) -> {
			a.setId(files.get(0).getId());
			a.getResourceAccess().add(createResourceAccess(curatorsTeamId, ACCESS_TYPE.READ));
			a.getResourceAccess().add(createResourceAccess(curatorsTeamId, ACCESS_TYPE.UPDATE));
		});
		
		aclHelper.create((a) -> {
			a.setId(files.get(1).getId());
			a.getResourceAccess().add(createResourceAccess(userOne.getId(), ACCESS_TYPE.READ));
			a.getResourceAccess().add(createResourceAccess(userOne.getId(), ACCESS_TYPE.UPDATE));
		});
		
		aclHelper.create((a) -> {
			a.setId(files.get(2).getId());
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.READ));
			a.getResourceAccess().add(createResourceAccess(userTwo.getId(), ACCESS_TYPE.UPDATE));
		});
		
		aclHelper.create((a) -> {
			a.setId(files.get(3).getId());
			a.getResourceAccess().add(createResourceAccess(curatorsTeamId, ACCESS_TYPE.READ));
		});
		
		aclHelper.create((a) -> {
			a.setId(files.get(4).getId());
			a.getResourceAccess().add(createResourceAccess(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId(), ACCESS_TYPE.READ));
			a.getResourceAccess().add(createResourceAccess(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId(), ACCESS_TYPE.READ));
			a.getResourceAccess().add(createResourceAccess(curatorsTeamId, ACCESS_TYPE.UPDATE));
		});
		
		/*
		 * By updating each file and waiting for the new etag we can ensure that the
		 * object_replication table contains correct benefactor for each file. This will
		 * ensure the view includes the correct benefactor ids.
		 * 
		 */
		for (FileEntity f : files) {
			FileEntity current = entityManager.getEntity(admin, f.getId(), FileEntity.class);
			current.setName(current.getName() + "updated");
			entityManager.updateEntity(admin, current, false, null);
			current = entityManager.getEntity(admin, f.getId(), FileEntity.class);

			asynchronousJobWorkerHelper.waitForObjectReplication(ReplicationType.ENTITY,
					KeyFactory.stringToKey(current.getId()), current.getEtag(), MAX_WAIT_MS);
		}

		
		List<ColumnModel> schema = List.of(
				new ColumnModel().setName("anInt").setColumnType(ColumnType.INTEGER)
		);
		schema = columnManager.createColumnModels(admin, schema);
		List<String> colIds = schema.stream().map(c -> c.getId()).collect(Collectors.toList());
		EntityView view = entityService
				.createEntity(
						admin.getId(), new EntityView().setParentId(project.getId()).setName("aView")
								.setColumnIds(colIds).setScopeIds(List.of(folder.getId())).setViewTypeMask(0x01L),
						null);

		String sql = String.format("select * from %s", view.getId());
		
		asynchronousJobWorkerHelper.assertQueryResult(admin, sql, (QueryResultBundle result) -> {
			assertEquals((long)fileCount, result.getQueryResult().getQueryResults().getRows().size());
		}, MAX_WAIT_MS);
		
		// create a grid using the table
		GridSession session = asynchronousJobWorkerHelper
				.assertJobResponse(userOne, new CreateGridRequest().setOwnerPrincipalId(curatorsTeam.getId())
						.setInitialQuery(new Query().setSql(sql)), (CreateGridResponse response) -> {
							assertNotNull(response);
							assertNotNull(response.getGridSession());
						}, MAX_WAIT_MS)
				.getResponse().getGridSession();
		assertNotNull(session);
		assertEquals(view.getId(), session.getSourceEntityId());

		// both users one and two can join the grid.
		GridReplica replicaOne = gridService
				.createReplica(userOne.getId(), new CreateReplicaRequest().setGridSessionId(session.getSessionId()))
				.getReplica();
		
		GridReplica replicaTwo = gridService
				.createReplica(userTwo.getId(), new CreateReplicaRequest().setGridSessionId(session.getSessionId()))
				.getReplica();
		
		assertThrows(UnauthorizedException.class, () -> {
			gridService.createReplica(userThree.getId(),
					new CreateReplicaRequest().setGridSessionId(session.getSessionId())).getReplica();
		});
		
		// the grid must only contain data visible to the curator team.
		TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
			Optional<GridHeader> header = gridViewManager.readHeader(session.getSessionId(), INTERNAL_REPLICA_ID);
			if (header.isEmpty()) {
				return Pair.create(false, null);
			}
			List<RowView> rows = gridViewManager.querySinglePage(header.get(), 100L, 0L);
			System.out.println("row count" + rows.size());
			Set<String> results = rows.stream().map(r -> r.getRowObject().getData().getRowJsonDocument().toString())
					.collect(Collectors.toSet());
			System.out.println(results);
			Set<String> expected = Set.of("{\"anInt\":0}","{\"anInt\":4}","{\"anInt\":5}");
			return Pair.create(expected.equals(results), null);
		});

	}
	
	List<FileEntity> createFiles(int count, String folderId, String fileHandleId) {
		List<FileEntity> files = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			FileEntity file = entityService.createEntity(admin.getId(),
					new FileEntity().setName("file" + i).setParentId(folderId).setDataFileHandleId(fileHandleId), null);
			Annotations annos = entityService.getEntityAnnotations(admin.getId(), file.getId());
			annos.setAnnotations(Map.of("anInt",
					new AnnotationsValue().setType(AnnotationsValueType.LONG).setValue(List.of("" + i))));
			entityService.updateEntityAnnotations(admin.getId(), file.getId(), annos);
			file = (FileEntity) entityService.getEntity(admin.getId(), file.getId());
			files.add(file);
		}
		return files;
	}

	@Test
	public void testGridWithRecordSet() throws Exception {
		Project project = entityService.createEntity(admin.getId(), new Project().setName("RecordSet Test"), null);

		String csvContent =
			"integer_column,string_column,double_column,boolean_column" + System.lineSeparator() +
			"1,test_1,1.1,true" 										+ System.lineSeparator() +
			"2,test_2,,true" 											+ System.lineSeparator() +
			"3,test_3,3.3,false";

		S3FileHandle fileHandle = fileHandleManager.createFileFromByteArray(admin.getId().toString(), new Date(), csvContent.getBytes(StandardCharsets.UTF_8), "recordset.csv", ContentType.create("text/csv"), null);

		RecordSet recordSet = entityService.createEntity(admin.getId(), new RecordSet()
			.setParentId(project.getId())
			.setName("recordSet")
			.setDataFileHandleId(fileHandle.getId())
			.setUpsertKey(List.of("integer_column")), null);

		String schemaId = createJsonSchema(Map.of(
			"integer_column", new JsonSchema().setType(Type.integer),
			"string_column", new JsonSchema().setType(Type.string),
			"double_column", new JsonSchema().setType(Type.number),
			"boolean_column", new JsonSchema().setType(Type._boolean)
		), List.of("double_column")).getNewVersionInfo().get$id();

		entityService.bindSchemaToEntity(admin.getId(),
			new BindSchemaToEntityRequest().setEntityId(recordSet.getId()).setSchema$id(schemaId));

		GridSession session = asynchronousJobWorkerHelper.assertJobResponse(admin,
			new CreateGridRequest().setRecordSetId(recordSet.getId()), (CreateGridResponse response) -> {
				assertNotNull(response);
				assertNotNull(response.getGridSession());
			}, MAX_WAIT_MS).getResponse().getGridSession();

		assertEquals(recordSet.getId(), session.getSourceEntityId());

		// Create replica One
		GridReplica replicaOne = gridService
			.createReplica(admin.getId(), new CreateReplicaRequest().setGridSessionId(session.getSessionId()))
			.getReplica();

		String urlOne = gridService
			.createPresignedUrl(admin.getId(), new CreateGridPresignedUrlRequest()
				.setGridSessionId(session.getSessionId())
				.setReplicaId(replicaOne.getReplicaId())
			).getPresignedUrl();

		BlockingQueue<String> incomingMessagesOne = new LinkedBlockingQueue<>();
		WebSocket wsOne = createConnection(urlOne, incomingMessagesOne);
		waitForConnected(incomingMessagesOne);

		// start the synchronize - expect a snapshot since this grid was created from a record set
		wsOne.send("[1,99,\"synchronize-clock\",[]]");

		assertTrue(waitForMessage((a) -> {
			if (a.optInt(0) == 4 && a.optInt(1) == 99) {
				JSONObject body = a.getJSONObject(2);
				assertEquals("snapshot", body.getString("type"), "Expected snapshot for grid created from record set");
				assertNotNull(body.getString("body"), "Snapshot URL should be present");
				return true;
			} else {
				return false;
			}
		}, incomingMessagesOne));

		GridHeader header = TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () ->
			gridViewManager.readHeader(session.getSessionId(), INTERNAL_REPLICA_ID)
				.map(h -> Pair.create(true, h))
				.orElse(Pair.create(false, null))
		);

		assertEquals(
			List.of("integer_column", "string_column", "double_column", "boolean_column"),
			header.getOrderedColumns().stream().map(Column::getName).collect(Collectors.toList())
		);

		List<RowView> rowsView = TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
			List<RowView> page = gridViewManager.querySinglePage(header, 100L, 0L);

			if (page.size() != 3) {
				return Pair.create(false, page);
			}

			// Also wait for the validation results to be set, the first row should be valid
			return Pair.create(
				new ValidationResults().setIsValid(true).equals(page.get(0).getRowValidationResults()),
				page
			);
		});

		assertEquals(
			List.of(
				"{\"integer_column\":1,\"string_column\":\"test_1\",\"double_column\":1.1,\"boolean_column\":true}",
				"{\"integer_column\":2,\"string_column\":\"test_2\",\"double_column\":null,\"boolean_column\":true}",
				"{\"integer_column\":3,\"string_column\":\"test_3\",\"double_column\":3.3,\"boolean_column\":false}"
			),
			rowsView.stream().map(r -> r.getRowObject().getData().getRowJsonDocument().toString()).collect(Collectors.toList())
		);

		// Now export the grid back to the record set
		GridRecordSetExportRequest request = new GridRecordSetExportRequest()
			.setSessionId(session.getSessionId());

		ValidationSummaryStatistics validationStats = asynchronousJobWorkerHelper.assertJobResponse(admin, request, (GridRecordSetExportResponse response) -> {
			assertEquals(request.getSessionId(), response.getSessionId());
			assertEquals(recordSet.getId(), response.getRecordSetId());
			assertTrue(response.getRecordSetVersionNumber() > recordSet.getVersionNumber());
			assertNotNull(response.getValidationFileHandleId());
			assertNotNull(response.getValidationSummaryStatistics());
			assertEquals(3L, response.getValidationSummaryStatistics().getTotalNumberOfChildren());
			assertEquals(2L, response.getValidationSummaryStatistics().getNumberOfValidChildren());
			assertEquals(1L, response.getValidationSummaryStatistics().getNumberOfInvalidChildren());
			assertEquals(0L, response.getValidationSummaryStatistics().getNumberOfUnknownChildren());
		}, MAX_WAIT_MS).getResponse().getValidationSummaryStatistics();

		RecordSet recordSetV2 = entityService.getEntity(admin.getId(), recordSet.getId(), RecordSet.class);

		assertNotEquals(recordSet.getDataFileHandleId(), recordSetV2.getDataFileHandleId());
		assertNotEquals(recordSet.getValidationFileHandleId(), recordSetV2.getValidationFileHandleId());
		assertEquals(validationStats, recordSetV2.getValidationSummary());

		// Now fix the grid by changing the double value in the second row from null to 2.2
		Patch patch = new Patch().setPatchId(
			new LogicalTimestamp().setReplicaId(replicaOne.getReplicaId()).setSequenceNumber(60L)
		);

		RowView secondRow = rowsView.get(1);

		patch.addNewOperation(new InsertVectorBuilder()
			.setVectorId(secondRow.getRowObject().getData().getVectorId())
			.setMap(Map.of(
				2, patch.addNewOperation(Operations.newConstant().setValue(new ConValue(ConType.DOUBLE, 2.2)))
			))
		);

		wsOne.send(String.format("[1,102,\"patch\", %s]", PatchCompactSerializable.serialize(patch).toString()));

		// Wait for response complete: [5,102]
		assertTrue(waitForMessage((a) -> a.optInt(0) == 5 && a.optInt(1) == 102, incomingMessagesOne));

		rowsView = TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
			List<RowView> page = gridViewManager.querySinglePage(header, 100L, 0L);

			// Wait for the updated validation results, all the rows should now be valid
			return Pair.create(
				new ValidationResults().setIsValid(true).equals(page.get(0).getRowValidationResults()) &&
				new ValidationResults().setIsValid(true).equals(page.get(1).getRowValidationResults()) &&
				new ValidationResults().setIsValid(true).equals(page.get(2).getRowValidationResults()),
				page
			);
		});

		assertEquals(
			List.of(
				"{\"integer_column\":1,\"string_column\":\"test_1\",\"double_column\":1.1,\"boolean_column\":true}",
				"{\"integer_column\":2,\"string_column\":\"test_2\",\"double_column\":2.2,\"boolean_column\":true}",
				"{\"integer_column\":3,\"string_column\":\"test_3\",\"double_column\":3.3,\"boolean_column\":false}"
			),
			rowsView.stream().map(r -> r.getRowObject().getData().getRowJsonDocument().toString()).collect(Collectors.toList())
		);

		// Now export the grid again
		validationStats = asynchronousJobWorkerHelper.assertJobResponse(admin, request, (GridRecordSetExportResponse response) -> {
			assertEquals(request.getSessionId(), response.getSessionId());
			assertEquals(recordSet.getId(), response.getRecordSetId());
			assertTrue(response.getRecordSetVersionNumber() > recordSetV2.getVersionNumber());
			assertNotNull(response.getValidationFileHandleId());
			assertNotNull(response.getValidationSummaryStatistics());
			assertEquals(3L, response.getValidationSummaryStatistics().getTotalNumberOfChildren());
			assertEquals(3L, response.getValidationSummaryStatistics().getNumberOfValidChildren());
			assertEquals(0L, response.getValidationSummaryStatistics().getNumberOfInvalidChildren());
			assertEquals(0L, response.getValidationSummaryStatistics().getNumberOfUnknownChildren());
		}, MAX_WAIT_MS).getResponse().getValidationSummaryStatistics();

		RecordSet recordSetV3 = entityService.getEntity(admin.getId(), recordSet.getId(), RecordSet.class);

		assertNotEquals(recordSetV2.getDataFileHandleId(), recordSetV3.getDataFileHandleId());
		assertNotEquals(recordSetV2.getValidationFileHandleId(), recordSetV3.getValidationFileHandleId());
		assertEquals(validationStats, recordSetV3.getValidationSummary());

		// Now update the record set from a CSV file
		String csvContents =
			"integer_column,string_column,double_column,boolean_column" + System.lineSeparator() +
			"1,test_1_updated,1.1,false" 								+ System.lineSeparator() + // update
																								   // Skip line 2
			"3,test_3_updated,3.3,true" 								+ System.lineSeparator() + // update
			"4,test_4_created,4.4,true"									+ System.lineSeparator() + // new row
			"5,test_5_created,5.5,true"									+ System.lineSeparator() + // new row
			"6,test_6_created,6.6,false";														   // new row

		S3FileHandle upsertFileHandle = fileHandleManager.createFileFromByteArray(admin.getId().toString(), new Date(), csvContents.getBytes(StandardCharsets.UTF_8), "recordset_upsert.csv", ContentType.create("text/csv"), null);

		GridCsvImportRequest csvImportRequest = new GridCsvImportRequest()
			.setSessionId(session.getSessionId())
			.setFileHandleId(upsertFileHandle.getId())
			.setCsvDescriptor(new CsvTableDescriptor().setIsFirstLineHeader(true))
			.setSchema(List.of(
				new ColumnModel().setName("integer_column").setColumnType(ColumnType.INTEGER),
				new ColumnModel().setName("string_column").setColumnType(ColumnType.STRING),
				new ColumnModel().setName("double_column").setColumnType(ColumnType.DOUBLE),
				new ColumnModel().setName("boolean_column").setColumnType(ColumnType.BOOLEAN)
			));

		asynchronousJobWorkerHelper.assertJobResponse(admin, csvImportRequest, (GridCsvImportResponse response) -> {
			assertEquals(request.getSessionId(), response.getSessionId());
			assertEquals(5, response.getTotalCount());
			assertEquals(2, response.getUpdatedCount());
			assertEquals(3, response.getCreatedCount());
		}, MAX_WAIT_MS).getResponse();

		rowsView = TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
			List<RowView> page = gridViewManager.querySinglePage(header, 100L, 0L);

			if (page.size() != 6) {
				return Pair.create(false, page);
			}

			// Also wait for the validation results to be set, the last row should be valid
			return Pair.create(
				new ValidationResults().setIsValid(true).equals(page.get(5).getRowValidationResults()),
				page
			);
		});

		assertEquals(
			List.of(
				"{\"integer_column\":1,\"string_column\":\"test_1_updated\",\"double_column\":1.1,\"boolean_column\":false}",
				"{\"integer_column\":2,\"string_column\":\"test_2\"," +    "\"double_column\":2.2,\"boolean_column\":true}",
				"{\"integer_column\":3,\"string_column\":\"test_3_updated\",\"double_column\":3.3,\"boolean_column\":true}",
				"{\"integer_column\":4,\"string_column\":\"test_4_created\",\"double_column\":4.4,\"boolean_column\":true}",
				"{\"integer_column\":5,\"string_column\":\"test_5_created\",\"double_column\":5.5,\"boolean_column\":true}",
				"{\"integer_column\":6,\"string_column\":\"test_6_created\",\"double_column\":6.6,\"boolean_column\":false}"
			),
			rowsView.stream().map(r -> r.getRowObject().getData().getRowJsonDocument().toString()).collect(Collectors.toList())
		);
	}

	@Test
	public void testGridWithRecordSetAndArrayColumns() throws Exception {
		Project project = entityService.createEntity(admin.getId(), new Project().setName("ArrayColumn Test"), null);

		// CSV with plain string values in a column that the JSON schema defines as array
		String csvContent =
			"id_column,tags_column,name_column" + System.lineSeparator() +
			"1,alpha,first"                     + System.lineSeparator() +
			"2,\"beta, gamma\",second"          + System.lineSeparator() +
			"3,\"[\"\"delta\"\"]\",third";

		S3FileHandle fileHandle = fileHandleManager.createFileFromByteArray(admin.getId().toString(), new Date(),
			csvContent.getBytes(StandardCharsets.UTF_8), "recordset_array.csv", ContentType.create("text/csv"), null);

		RecordSet recordSet = entityService.createEntity(admin.getId(), new RecordSet()
			.setParentId(project.getId())
			.setName("arrayRecordSet")
			.setDataFileHandleId(fileHandle.getId())
			.setUpsertKey(List.of("id_column")), null);

		// Schema with tags_column as array type
		String schemaId = createJsonSchema(Map.of(
			"id_column", new JsonSchema().setType(Type.integer),
			"tags_column", new JsonSchema().setType(Type.array).setItems(new JsonSchema().setType(Type.string)),
			"name_column", new JsonSchema().setType(Type.string)
		), List.of("id_column", "name_column")).getNewVersionInfo().get$id();

		entityService.bindSchemaToEntity(admin.getId(),
			new BindSchemaToEntityRequest().setEntityId(recordSet.getId()).setSchema$id(schemaId));

		// Create grid session from RecordSet — exercises RecordSetCreateGridHandler + CsvSchemaReconciler
		GridSession session = asynchronousJobWorkerHelper.assertJobResponse(admin,
			new CreateGridRequest().setRecordSetId(recordSet.getId()), (CreateGridResponse response) -> {
				assertNotNull(response);
				assertNotNull(response.getGridSession());
			}, MAX_WAIT_MS).getResponse().getGridSession();

		GridHeader header = TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () ->
			gridViewManager.readHeader(session.getSessionId(), INTERNAL_REPLICA_ID)
				.map(h -> Pair.create(true, h))
				.orElse(Pair.create(false, null))
		);

		assertEquals(
			List.of("id_column", "tags_column", "name_column"),
			header.getOrderedColumns().stream().map(Column::getName).collect(Collectors.toList())
		);

		// Wait for data and validation
		List<RowView> rowsView = TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
			List<RowView> page = gridViewManager.querySinglePage(header, 100L, 0L);
			if (page.size() != 3) {
				return Pair.create(false, page);
			}
			return Pair.create(
				page.get(0).getRowValidationResults() != null,
				page
			);
		});

		// Verify that plain strings were coerced to arrays
		assertEquals(
			List.of(
				"{\"id_column\":1,\"tags_column\":[\"alpha\"],\"name_column\":\"first\"}",
				"{\"id_column\":2,\"tags_column\":[\"beta\",\"gamma\"],\"name_column\":\"second\"}",
				"{\"id_column\":3,\"tags_column\":[\"delta\"],\"name_column\":\"third\"}"
			),
			rowsView.stream().map(r -> r.getRowObject().getData().getRowJsonDocument().toString()).collect(Collectors.toList())
		);

		// Now test CSV import path — exercises GridCsvImporterImpl + CsvSchemaReconciler
		String upsertCsvContent =
			"id_column,tags_column,name_column" + System.lineSeparator() +
			"1,updated_alpha,first_updated"     + System.lineSeparator() +
			"4,\"new_a, new_b\",fourth";

		S3FileHandle upsertFileHandle = fileHandleManager.createFileFromByteArray(admin.getId().toString(), new Date(),
			upsertCsvContent.getBytes(StandardCharsets.UTF_8), "recordset_array_upsert.csv", ContentType.create("text/csv"), null);

		// Note: schema uses STRING for tags_column — the reconciler should upgrade to STRING_LIST
		GridCsvImportRequest csvImportRequest = new GridCsvImportRequest()
			.setSessionId(session.getSessionId())
			.setFileHandleId(upsertFileHandle.getId())
			.setCsvDescriptor(new CsvTableDescriptor().setIsFirstLineHeader(true))
			.setSchema(List.of(
				new ColumnModel().setName("id_column").setColumnType(ColumnType.INTEGER),
				new ColumnModel().setName("tags_column").setColumnType(ColumnType.STRING),
				new ColumnModel().setName("name_column").setColumnType(ColumnType.STRING)
			));

		asynchronousJobWorkerHelper.assertJobResponse(admin, csvImportRequest, (GridCsvImportResponse response) -> {
			assertEquals(session.getSessionId(), response.getSessionId());
			assertEquals(2, response.getTotalCount());
			assertEquals(1, response.getUpdatedCount());
			assertEquals(1, response.getCreatedCount());
		}, MAX_WAIT_MS).getResponse();

		rowsView = TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
			List<RowView> page = gridViewManager.querySinglePage(header, 100L, 0L);
			if (page.size() != 4) {
				return Pair.create(false, page);
			}
			return Pair.create(
				page.get(3).getRowValidationResults() != null,
				page
			);
		});

		assertEquals(
			List.of(
				"{\"id_column\":1,\"tags_column\":[\"updated_alpha\"],\"name_column\":\"first_updated\"}",
				"{\"id_column\":2,\"tags_column\":[\"beta\",\"gamma\"],\"name_column\":\"second\"}",
				"{\"id_column\":3,\"tags_column\":[\"delta\"],\"name_column\":\"third\"}",
				"{\"id_column\":4,\"tags_column\":[\"new_a\",\"new_b\"],\"name_column\":\"fourth\"}"
			),
			rowsView.stream().map(r -> r.getRowObject().getData().getRowJsonDocument().toString()).collect(Collectors.toList())
		);
	}

	UserInfo createUser(){
		NewUser newUser = new NewUser();
		newUser.setEmail(UUID.randomUUID().toString() + "@test.com");
		newUser.setUserName(UUID.randomUUID().toString());
		return userManager.createOrGetTestUser(admin, newUser);
	}
	List<String[]> createAndDownloadCsvFromGrid(DownloadFromGridRequest request)
			throws AsynchJobFailedException, IOException {
		DownloadFromGridResult downloadFromGridResult = asynchronousJobWorkerHelper
				.assertJobResponse(admin, request, (DownloadFromGridResult response) -> {
					assertNotNull(response);
					assertEquals(request.getSessionId(), response.getSessionId());
					assertNotNull(response.getResultsFileHandleId());
				}, MAX_WAIT_MS).getResponse();

		S3FileHandle csvFileHandle = (S3FileHandle) fileHandleManager.getRawFileHandle(admin,
				downloadFromGridResult.getResultsFileHandleId());

		assertEquals("text/csv", csvFileHandle.getContentType());
		assertNotNull(csvFileHandle.getContentMd5());
		// Download the file
		List<String[]> csvContents;
		File temp = File.createTempFile("DownloadCSV", "." + CSVUtils.guessExtension(null));
		try {
			s3Client.getObject(new GetObjectRequest(csvFileHandle.getBucketName(), csvFileHandle.getKey()), temp);
			try (CSVReader csvReader = new CSVReader(new FileReader(temp))) {
				csvContents = csvReader.readAll();
			}
		} finally {
			temp.delete();
		}
		return csvContents;
	}

	/**
	 * Helper to create a schema
	 *
	 * @return
	 * @throws Exception
	 */
	CreateSchemaResponse createJsonSchema(Map<String, JsonSchema> properties, List<String> requiredProperties) throws Exception {
		Organization org = asynchronousJobWorkerHelper.getOrCreateOrganization(admin.getId(), "gridtestorg");
		String jsonSchemaName = "exampleSchema";
		JsonSchema jsonSchema = new JsonSchema()
				.set$id(org.getName() + "-" + jsonSchemaName)
				.setProperties(properties)
				.setRequired(requiredProperties);

		return asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateSchemaRequest().setDryRun(false).setSchema(jsonSchema), (CreateSchemaResponse response) -> {
					assertNotNull(response);
				}, MAX_WAIT_MS).getResponse();
	}

	/**
	 * Create n secondary websocket connections to the given grid session.
	 * 
	 * @param session The grid session to connect to
	 * @param count The number of secondary connections to create
	 * @return List of SecondaryConnection objects
	 * @throws URISyntaxException
	 */
	List<SecondaryConnection> createSecondaryConnections(GridSession session, int count) throws URISyntaxException, InterruptedException {
		List<SecondaryConnection> connections = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			GridReplica replica = gridService
					.createReplica(admin.getId(), new CreateReplicaRequest().setGridSessionId(session.getSessionId()))
					.getReplica();

			String url = gridService
					.createPresignedUrl(admin.getId(), new CreateGridPresignedUrlRequest()
							.setGridSessionId(session.getSessionId()).setReplicaId(replica.getReplicaId()))
					.getPresignedUrl();

			BlockingQueue<String> incomingMessages = new LinkedBlockingQueue<>();
			WebSocket ws = createConnection(url, incomingMessages);
			waitForConnected(incomingMessages);

			connections.add(new SecondaryConnection(replica, ws, incomingMessages));
		}
		return connections;
	}

	/**
	 * Close all websocket connections in the given list.
	 * 
	 * @param connections List of SecondaryConnection objects to close
	 */
	void closeAllConnections(List<SecondaryConnection> connections) {
		for (SecondaryConnection conn : connections) {
			if (conn.webSocket() != null) {
				conn.webSocket().close();
			}
		}
	}

	/**
	 * Wait for the given message to appear on the queue.
	 *
	 * @param code
	 * @param key
	 * @param incomingMessages
	 * @return
	 * @throws InterruptedException
	 */
	boolean waitForMessage(Predicate<JSONArray> handler, BlockingQueue<String> incomingMessages)
			throws InterruptedException {
		return asynchronousJobWorkerHelper.waitForMessage(handler, incomingMessages);
	}

	/**
	 * Create a websocket connection that will post all received messages to the
	 * passed queue.
	 *
	 * @param presignedUrl
	 * @param incomingMessages
	 * @return
	 * @throws URISyntaxException
	 */
	public WebSocket createConnection(String presignedUrl, BlockingQueue<String> incomingMessages)
			throws URISyntaxException {
		return asynchronousJobWorkerHelper.createConnection(presignedUrl, incomingMessages);
	}

}
