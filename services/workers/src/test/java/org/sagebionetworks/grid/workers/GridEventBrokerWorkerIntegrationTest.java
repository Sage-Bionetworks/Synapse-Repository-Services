package org.sagebionetworks.grid.workers;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.http.entity.ContentType;
import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
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
import org.sagebionetworks.repo.model.grid.GridCsvImportRequest;
import org.sagebionetworks.repo.model.grid.GridCsvImportResponse;
import org.sagebionetworks.repo.model.grid.GridRecordSetExportRequest;
import org.sagebionetworks.repo.model.grid.GridRecordSetExportResponse;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.repo.model.grid.patch.Timespan;
import org.sagebionetworks.repo.model.grid.patch.compact.LogicalTimestampCompactSerializable;
import org.sagebionetworks.repo.model.grid.patch.compact.PatchCompactSerializable;
import org.sagebionetworks.repo.model.grid.patch.operation.NewConstant;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.InsertVectorBuilder;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.NewConstantBuilder;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.Operations;
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

		// Create replica two.
		GridReplica replicaTwo = gridService
				.createReplica(admin.getId(), new CreateReplicaRequest().setGridSessionId(session.getSessionId()))
				.getReplica();

		String urlTwo = gridService
				.createPresignedUrl(admin.getId(), new CreateGridPresignedUrlRequest()
						.setGridSessionId(session.getSessionId()).setReplicaId(replicaTwo.getReplicaId()))
				.getPresignedUrl();
		assertNotNull(urlOne);

		BlockingQueue<String> incomingMessagesTwo = new LinkedBlockingQueue<>();
		WebSocket wsTwo = createConnection(urlTwo, incomingMessagesTwo);
		waitForConnected(incomingMessagesTwo);

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

		// The second replica should be notified of two patches
		assertTrue(waitForMessage((a) -> a.optInt(0) == 8 && "new-patch".equals(a.optString(1)), incomingMessagesTwo));
		assertTrue(waitForMessage((a) -> a.optInt(0) == 8 && "new-patch".equals(a.optString(1)), incomingMessagesTwo));

		// Two's clock is currently empty so start a synchronize.
		wsTwo.send("[1,99,\"synchronize-clock\",[]]");

		List<LogicalTimestamp> clock = new ArrayList<>();
		assertTrue(waitForMessage((a) -> {
			if (a.optInt(0) == 4 && a.optInt(1) == 99) {
				Patch p = PatchCompactSerializable.deserialize(a.getJSONArray(2));
				clock.add(LogicalTimestamp.newIncrement(p.getPatchId(), p.getSpan()));
				return true;
			} else {
				return false;
			}
		}, incomingMessagesTwo));

		// after applying the patch update the clock and synchronize again.
		String newClock = LogicalTimestampCompactSerializable.serializeClock(clock).toString();
		wsTwo.send(String.format("[1,99,\"synchronize-clock\",%s]", newClock));

		clock.clear();
		assertTrue(waitForMessage((a) -> {
			if (a.optInt(0) == 4 && a.optInt(1) == 99) {
				Patch p = PatchCompactSerializable.deserialize(a.getJSONArray(2));
				clock.add(LogicalTimestamp.newIncrement(p.getPatchId(), p.getSpan()));
				return true;
			} else {
				return false;
			}
		}, incomingMessagesTwo));

		// after the second snych, replica two should be up-to-date.
		newClock = LogicalTimestampCompactSerializable.serializeClock(clock).toString();
		wsTwo.send(String.format("[1,99,\"synchronize-clock\",%s]", newClock));

		assertTrue(waitForMessage((a) -> a.optInt(0) == 5 && a.optInt(1) == 99, incomingMessagesTwo));

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

		// start the synchronize
		wsOne.send("[1,99,\"synchronize-clock\",[]]");
		
		assertTrue(waitForMessage((a) -> {
			if (a.optInt(0) == 4 && a.optInt(1) == 99) {
				Patch patch = PatchCompactSerializable.deserialize(a.getJSONArray(2));
				assertNotNull(patch);
				assertEquals(new LogicalTimestamp().setReplicaId(INTERNAL_REPLICA_ID).setSequenceNumber(1L), patch.getPatchId());
				assertEquals(38L, patch.getSpan());
				// find the constant that contains the table's last value
				Optional<NewConstant> op = patch.getOperations().stream().filter(o -> (o instanceof NewConstant))
						.map(c -> (NewConstant) c).filter(c -> ConType.LONG.equals(c.getValue().getType()))
						.filter(c -> Long.valueOf(9090).equals(c.getValue().getValue())).findFirst();
				assertTrue(op.isPresent());
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

		String annotationName = "anInt";
		String jsonSchem$id = createJsonSchema(Map.of(annotationName, new JsonSchema().setType(Type.integer)))
				.getNewVersionInfo().get$id();

		ExternalFileHandle fh = fileHandleManager.createExternalFileHandle(admin, new ExternalFileHandle()
				.setContentType("text/plain").setFileName("foo.bar").setExternalURL("https://something.org"));
		FileEntity file = entityService.createEntity(admin.getId(),
				new FileEntity().setName("file").setParentId(folder.getId()).setDataFileHandleId(fh.getId()), null);

		Annotations annos = entityService.getEntityAnnotations(admin.getId(), file.getId());
		annos.setAnnotations(Map.of(annotationName,
				new AnnotationsValue().setType(AnnotationsValueType.LONG).setValue(List.of("9090"))));
		entityService.updateEntityAnnotations(admin.getId(), file.getId(), annos);
		asynchronousJobWorkerHelper.waitForEntityReplication(admin, file.getId(), MAX_WAIT_MS);
		file = (FileEntity) entityService.getEntity(admin.getId(), file.getId());

		// Bind the schema to the file.
		entityService.bindSchemaToEntity(admin.getId(),
				new BindSchemaToEntityRequest().setEntityId(file.getId()).setSchema$id(jsonSchem$id));

		List<ColumnModel> schema = List.of(new ColumnModel().setName("anInt").setColumnType(ColumnType.INTEGER));
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

		// start the synchronize
		wsOne.send("[1,99,\"synchronize-clock\",[]]");
		assertTrue(waitForMessage((a) -> {
			if (a.optInt(0) == 4 && a.optInt(1) == 99) {
				Patch patch = PatchCompactSerializable.deserialize(a.getJSONArray(2));
				assertNotNull(patch);
				assertEquals(new LogicalTimestamp().setReplicaId(INTERNAL_REPLICA_ID).setSequenceNumber(1L),
						patch.getPatchId());
				assertEquals(20L, patch.getSpan());
				// find the constant that contains the table's value
				Optional<NewConstant> op = patch.getOperations().stream().filter(o -> (o instanceof NewConstant))
						.map(c -> (NewConstant) c).filter(c -> ConType.LONG.equals(c.getValue().getType()))
						.filter(c -> Long.valueOf(9090).equals(c.getValue().getValue())).findFirst();
				assertTrue(op.isPresent());
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
			List<RowView> rows = gridViewManager.querySinglePage(header.get(), 100L, 0L);
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
		assertArrayEquals(new String[] { "ROW_ID", "ROW_VERSION", "etag", "anInt" }, csvContents.get(0));
		assertArrayEquals(new String[] { file.getId().substring("syn".length()), file.getVersionNumber().toString(),
				file.getEtag(), updateValue }, csvContents.get(1));
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
		)).getNewVersionInfo().get$id();
		
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

		// start the synchronize
		wsOne.send("[1,99,\"synchronize-clock\",[]]");
		
		assertTrue(waitForMessage((a) -> {
			if (a.optInt(0) == 4 && a.optInt(1) == 99) {
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
				"[1,\"test_1\",1.1,true]",
				"[2,\"test_2\",null,true]",
				"[3,\"test_3\",3.3,false]"
			),
			rowsView.stream().map(r -> r.getRowObject().getData().getCells().toString()).collect(Collectors.toList())
		);
		
		// Now export the grid back to the record set		
		GridRecordSetExportRequest request = new GridRecordSetExportRequest()
			.setSessionId(session.getSessionId());
		
		ValidationSummaryStatistics validationStats = asynchronousJobWorkerHelper.assertJobResponse(admin, request, (GridRecordSetExportResponse response) -> {
			assertEquals(request.getSessionId(), response.getSessionId());
			assertEquals(recordSet.getId(), response.getRecordSetId());
			assertTrue(response.getRecordSetVersionNumber() > recordSet.getVersionNumber());
			assertNotNull(response.getValidationSummaryStatistics());
			assertEquals(3L, response.getValidationSummaryStatistics().getTotalNumberOfChildren());
			assertEquals(2L, response.getValidationSummaryStatistics().getNumberOfValidChildren());
			assertEquals(1L, response.getValidationSummaryStatistics().getNumberOfInvalidChildren());
			assertEquals(0L, response.getValidationSummaryStatistics().getNumberOfUnknownChildren());
		}, MAX_WAIT_MS).getResponse().getValidationSummaryStatistics();
		
		RecordSet recordSetV2 = entityService.getEntity(admin.getId(), recordSet.getId(), RecordSet.class);

		assertNotEquals(recordSet.getDataFileHandleId(), recordSetV2.getDataFileHandleId());
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
				"[1,\"test_1\",1.1,true]",
				"[2,\"test_2\",2.2,true]",
				"[3,\"test_3\",3.3,false]"
			),
			rowsView.stream().map(r -> r.getRowObject().getData().getCells().toString()).collect(Collectors.toList())
		);
		
		// Now export the grid again		
		validationStats = asynchronousJobWorkerHelper.assertJobResponse(admin, request, (GridRecordSetExportResponse response) -> {
			assertEquals(request.getSessionId(), response.getSessionId());
			assertEquals(recordSet.getId(), response.getRecordSetId());
			assertTrue(response.getRecordSetVersionNumber() > recordSetV2.getVersionNumber());
			assertNotNull(response.getValidationSummaryStatistics());
			assertEquals(3L, response.getValidationSummaryStatistics().getTotalNumberOfChildren());
			assertEquals(3L, response.getValidationSummaryStatistics().getNumberOfValidChildren());
			assertEquals(0L, response.getValidationSummaryStatistics().getNumberOfInvalidChildren());
			assertEquals(0L, response.getValidationSummaryStatistics().getNumberOfUnknownChildren());
		}, MAX_WAIT_MS).getResponse().getValidationSummaryStatistics();

		RecordSet recordSetV3 = entityService.getEntity(admin.getId(), recordSet.getId(), RecordSet.class);
		
		assertNotEquals(recordSetV2.getDataFileHandleId(), recordSetV3.getDataFileHandleId());
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
				"[1,\"test_1_updated\",1.1,false]",
				"[2,\"test_2\",2.2,true]",
				"[3,\"test_3_updated\",3.3,true]",
				"[4,\"test_4_created\",4.4,true]",
				"[5,\"test_5_created\",5.5,true]",
				"[6,\"test_6_created\",6.6,false]"
			),
			rowsView.stream().map(r -> r.getRowObject().getData().getCells().toString()).collect(Collectors.toList())
		);
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
	CreateSchemaResponse createJsonSchema(Map<String, JsonSchema> properties) throws Exception {
		Organization org = asynchronousJobWorkerHelper.getOrCreateOrganization(admin.getId(), "gridtestorg");
		String jsonSchemaName = "exampleSchema";
		JsonSchema jsonSchema = new JsonSchema().set$id(org.getName() + "-" + jsonSchemaName).setProperties(properties);

		return asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateSchemaRequest().setDryRun(false).setSchema(jsonSchema), (CreateSchemaResponse response) -> {
					assertNotNull(response);
				}, MAX_WAIT_MS).getResponse();
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
