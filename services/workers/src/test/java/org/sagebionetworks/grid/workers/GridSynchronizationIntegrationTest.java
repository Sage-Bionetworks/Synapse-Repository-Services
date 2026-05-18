package org.sagebionetworks.grid.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.grid.CreateGridPresignedUrlRequest;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.CreateGridResponse;
import org.sagebionetworks.repo.model.grid.CreateReplicaRequest;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.SynchronizeGridRequest;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessage;
import org.sagebionetworks.repo.model.grid.message.JsonRxMessageType;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Patch;
import org.sagebionetworks.repo.model.grid.patch.compact.PatchCompactSerializable;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.Operations;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.service.EntityService;
import org.sagebionetworks.repo.service.GridService;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class GridSynchronizationIntegrationTest {

	public static final long MAX_WAIT_MS = 120_000;

	@Autowired
	private EntityService entityService;
	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;
	@Autowired
	private JsonSchemaManager jsonSchemaManager;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private ColumnModelManager columnModelManager;
	@Autowired
	private GridReplicaViewManager gridViewManager;
	@Autowired
	private GridService gridService;
	@Autowired
	private GridManager gridManager;

	private UserInfo admin;

	@BeforeEach
	public void before() {
		admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		jsonSchemaManager.truncateAll();
		entityManager.truncateAll();

	}

	@Test
	public void testSynchronizeEntityView() throws Exception {

		Project project = entityService.createEntity(admin.getId(), new Project().setName("test"), null);
		Folder folder = entityService.createEntity(admin.getId(),
				new Folder().setName("folder").setParentId(project.getId()), null);

		ExternalFileHandle fh = fileHandleManager.createExternalFileHandle(admin, new ExternalFileHandle()
				.setContentType("text/plain").setFileName("foo.bar").setExternalURL("https://something.org"));

		// create the starting files
		int fileCount = 3;
		List<FileEntity> files = createFiles(fileCount, folder.getId(), fh.getId());

		// define the view schema
		String c1Name = "theString";
		String c2Name = "theId";
		String c3Name = "toRemove";
		String c4Name = "added";
		List<ColumnModel> startingSchema = List.of(new ColumnModel().setColumnType(ColumnType.STRING).setName(c1Name),
				new ColumnModel().setColumnType(ColumnType.INTEGER).setName(c2Name),
				new ColumnModel().setColumnType(ColumnType.STRING).setName(c3Name));
		startingSchema = columnModelManager.createColumnModels(admin, startingSchema);
		ColumnModel toAdd = columnModelManager
				.createColumnModel(new ColumnModel().setColumnType(ColumnType.STRING).setName("added"));

		List<ColumnModel> finalSchema = List.of(startingSchema.get(0), startingSchema.get(1), toAdd);

		// Add annotation data to the files
		setFileJSON(files.get(0).getId(), new JSONObject().put(c1Name, "one").put(c2Name, 111L).put(c3Name, "1.1"));
		setFileJSON(files.get(1).getId(),
				new JSONObject().put(c1Name, "two").put(c2Name, JSONObject.NULL).put(c3Name, "2.2"));
		setFileJSON(files.get(2).getId(),
				new JSONObject().put(c1Name, "three").put(c2Name, 333L).put(c3Name, JSONObject.NULL));
		waitForFilesToReplicat(files);

		// setup the view
		EntityView view = entityService.createEntity(admin.getId(),
				new EntityView().setScopeIds(List.of(project.getId())).setViewTypeMask(0x01L)
						.setParentId(project.getId()).setColumnIds(
								startingSchema.stream().map(ColumnModel::getId).collect(Collectors.toList())),
				null);
		String sql = String.format("select * from %s", view.getId());
		asynchronousJobWorkerHelper.assertQueryResult(admin, sql, (QueryResultBundle result) -> {
			assertEquals((long) fileCount, result.getQueryResult().getQueryResults().getRows().size());
		}, MAX_WAIT_MS);

		// create a grid using the view.
		GridSession session = asynchronousJobWorkerHelper
				.assertJobResponse(admin, new CreateGridRequest().setOwnerPrincipalId(admin.getId().toString())
						.setInitialQuery(new Query().setSql(sql)), (CreateGridResponse response) -> {
							assertNotNull(response);
							assertNotNull(response.getGridSession());
						}, MAX_WAIT_MS)
				.getResponse().getGridSession();
		assertNotNull(session);
		assertEquals(view.getId(), session.getSourceEntityId());

		GridConnectionInfo internalCon = asynchronousJobWorkerHelper.getInternalGridConnection(session.getSessionId(),
				MAX_WAIT_MS);

		GridReplica userReplica = gridManager
				.createReplica(admin, new CreateReplicaRequest().setGridSessionId(session.getSessionId())).getReplica();

		String urlOne = gridService
				.createPresignedUrl(admin.getId(), new CreateGridPresignedUrlRequest()
						.setGridSessionId(session.getSessionId()).setReplicaId(userReplica.getReplicaId()))
				.getPresignedUrl();
		assertNotNull(urlOne);
		BlockingQueue<String> incomingMessages = new LinkedBlockingQueue<>();
		WebSocket websoceket = asynchronousJobWorkerHelper.createConnection(urlOne, incomingMessages);

		List<RowView> fetchedRows = TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
			Optional<GridHeader> header = gridViewManager.readHeader(session.getSessionId(),
					internalCon.getReplicaId());
			if (header.isEmpty()) {
				return Pair.create(false, null);
			}
			List<RowView> rows = gridViewManager.querySinglePage(header.get(), 100L, 0L);
			System.out.println("row count" + rows.size());
			Set<String> results = rows.stream().map(r -> r.getRowObject().getData().getRowJsonDocument().toString())
					.collect(Collectors.toSet());
			System.out.println(results);
			Set<String> expected = Set.of(
					//
					"{\"theString\":\"one\",\"theId\":111,\"toRemove\":\"1.1\"}",
					//
					"{\"theString\":\"two\",\"toRemove\":\"2.2\"}",
					//
					"{\"theString\":\"three\",\"theId\":333}");
			return Pair.create(expected.equals(results), rows);
		});

		// update the first row in the grid
		Patch patch = new Patch()
				.setPatchId(new LogicalTimestamp().setReplicaId(userReplica.getReplicaId()).setSequenceNumber(101L));

		patch.addNewOperation(
				Operations.insertVector().setVectorId(fetchedRows.get(0).getRowObject().getData().getVectorId())
						.setMap(Map.of(
								// 0
								0, patch.addNewOperation(
								Operations.newConstant().setValue(new ConValue(ConType.STRING, "oneUpdated"))),
								// 2
								2, patch.addNewOperation(
								Operations.newConstant().setValue(new ConValue(ConType.STRING, "1.3")))
								
								)));
		JsonRxMessage message = new JsonRxMessage(JsonRxMessageType.RequestData).setId(102).setMethod("patch")
				.setBody(PatchCompactSerializable.serialize(patch));
		websoceket.send(message.toJson());
		asynchronousJobWorkerHelper.waitForMessage((a) -> a.optInt(0) == 5 && a.optInt(1) == message.getId().get(),
				incomingMessages);

		fetchedRows = TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
			Optional<GridHeader> header = gridViewManager.readHeader(session.getSessionId(),
					internalCon.getReplicaId());
			if (header.isEmpty()) {
				return Pair.create(false, null);
			}
			List<RowView> rows = gridViewManager.querySinglePage(header.get(), 100L, 0L);
			System.out.println("row count" + rows.size());
			Set<String> results = rows.stream().map(r -> r.getRowObject().getData().getRowJsonDocument().toString())
					.collect(Collectors.toSet());
			System.out.println(results);
			Set<String> expected = Set.of(
					//
					"{\"theString\":\"oneUpdated\",\"theId\":111,\"toRemove\":\"1.3\"}",
					//
					"{\"theString\":\"two\",\"toRemove\":\"2.2\"}",
					//
					"{\"theString\":\"three\",\"theId\":333}");
			return Pair.create(expected.equals(results), rows);
		});

		// delete the third file
		FileEntity toDelete = files.remove(2);
		entityManager.deleteEntity(admin, toDelete.getId());
		// add a new File
		FileEntity newfile = entityService.createEntity(admin.getId(),
				new FileEntity().setName("the new file").setParentId(folder.getId()).setDataFileHandleId(fh.getId()),
				null);
		files.add(newfile);
		setFileJSON(files.get(0).getId(), new JSONObject().put(c1Name, "shouldBeReplaced").put(c2Name, 111L)
				.put(c3Name, "1.2").put(c4Name, "oneUpdatedInSource"));
		setFileJSON(newfile.getId(),
				new JSONObject().put(c1Name, "four").put(c2Name, 444L).put(c3Name, "4.4").put(c4Name, "newlyAdded"));
		setFileJSON(files.get(1).getId(), new JSONObject().put(c1Name, "two").put(c2Name, 222L).put(c3Name, "2.2")
				.put(c4Name, "updatedInSource"));
		waitForFilesToReplicat(files);

		boolean newVersion = false;
		// add a new column and remove an old.
		view.setColumnIds(finalSchema.stream().map(ColumnModel::getId).collect(Collectors.toList()));
		view = entityService.updateEntity(admin.getId(), view, newVersion, null);

		sql = "select * from " + view.getId() + " where ROW_ID = " + KeyFactory.stringToKey(newfile.getId());
		asynchronousJobWorkerHelper.assertQueryResult(admin, sql, (QueryResultBundle result) -> {
			assertEquals((long) 1, result.getQueryResult().getQueryResults().getRows().size());
		}, MAX_WAIT_MS);

		// call under test
		asynchronousJobWorkerHelper.assertJobResponse(admin,
				new SynchronizeGridRequest().setGridSessionId(session.getSessionId()), (r) -> {
					System.out.println(r);
				}, MAX_WAIT_MS);

		TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
			Optional<GridHeader> header = gridViewManager.readHeader(session.getSessionId(),
					internalCon.getReplicaId());
			if (header.isEmpty()) {
				return Pair.create(false, null);
			}
			List<RowView> rows = gridViewManager.querySinglePage(header.get(), 100L, 0L);
			System.out.println("row count" + rows.size());
			Set<String> results = rows.stream().map(r -> r.getRowObject().getData().getRowJsonDocument().toString())
					.collect(Collectors.toSet());
			System.out.println(results);
			Set<String> expected = Set.of(
					//
					"{\"added\":\"oneUpdatedInSource\",\"theString\":\"oneUpdated\",\"theId\":111}",
					//
					"{\"added\":\"updatedInSource\",\"theString\":\"two\",\"theId\":222}",
					//
					"{\"added\":\"newlyAdded\",\"theString\":\"four\",\"theId\":444}");
			return Pair.create(expected.equals(results), null);
		});

		verifyExpectedAnnotations(files.get(0).getId(), new JSONObject().put(c1Name, "oneUpdated").put(c2Name, 111L)
				.put(c3Name, "1.2").put(c4Name, "oneUpdatedInSource"));
		verifyExpectedAnnotations(files.get(1).getId(), new JSONObject().put(c1Name, "two").put(c2Name, 222L)
				.put(c3Name, "2.2").put(c4Name, "updatedInSource"));
		verifyExpectedAnnotations(files.get(2).getId(),
				new JSONObject().put(c1Name, "four").put(c2Name, 444L).put(c3Name, "4.4").put(c4Name, "newlyAdded"));

	}
	
	@Test
	public void testSynchronizeEntityViewWithMaxStrings() throws Exception {
		Project project = entityService.createEntity(admin.getId(), new Project().setName("test2k"), null);
		Folder folder = entityService.createEntity(admin.getId(),
				new Folder().setName("folder").setParentId(project.getId()), null);
		ExternalFileHandle fh = fileHandleManager.createExternalFileHandle(admin, new ExternalFileHandle()
				.setContentType("text/plain").setFileName("foo.bar").setExternalURL("https://something.org"));

		List<FileEntity> files = createFiles(1, folder.getId(), fh.getId());

		String colName = "largeString";
		String initialValue = "a".repeat(1000);
		String updatedValue = "b".repeat(1000);

		String listName = "aLargeList";
		int listSize = 5;
		int itemStringSize = 1000; // each item at MAX_ALLOWED_STRING_SIZE; total 5,000 chars << 32KB WebSocket frame limit
		String initialListElement = "y".repeat(itemStringSize);
		String updatedListElement = "z".repeat(itemStringSize);
		JSONArray initialListValue = new JSONArray(Collections.nCopies(listSize, initialListElement));
		JSONArray updatedListValue = new JSONArray(Collections.nCopies(listSize, updatedListElement));

		setFileJSON(files.get(0).getId(), new JSONObject().put(colName, initialValue).put(listName, initialListValue));
		waitForFilesToReplicat(files);

		List<ColumnModel> schema = columnModelManager.createColumnModels(admin,
				List.of(new ColumnModel().setColumnType(ColumnType.STRING).setMaximumSize(1000L).setName(colName),
						new ColumnModel().setColumnType(ColumnType.STRING_LIST).setMaximumListLength((long) listSize)
								.setMaximumSize((long) itemStringSize).setName(listName)));

		EntityView view = entityService.createEntity(admin.getId(),
				new EntityView().setScopeIds(List.of(project.getId())).setViewTypeMask(0x01L)
						.setParentId(project.getId())
						.setColumnIds(schema.stream().map(ColumnModel::getId).collect(Collectors.toList())),
				null);
		String sql = "select * from " + view.getId();
		asynchronousJobWorkerHelper.assertQueryResult(admin, sql, (QueryResultBundle result) -> {
			assertEquals(1L, result.getQueryResult().getQueryResults().getRows().size());
		}, MAX_WAIT_MS);

		GridSession session = asynchronousJobWorkerHelper
				.assertJobResponse(admin, new CreateGridRequest().setOwnerPrincipalId(admin.getId().toString())
						.setInitialQuery(new Query().setSql(sql)), (CreateGridResponse response) -> {
							assertNotNull(response);
							assertNotNull(response.getGridSession());
						}, MAX_WAIT_MS)
				.getResponse().getGridSession();
		assertNotNull(session);

		GridConnectionInfo internalCon = asynchronousJobWorkerHelper.getInternalGridConnection(session.getSessionId(),
				MAX_WAIT_MS);

		GridReplica userReplica = gridManager
				.createReplica(admin, new CreateReplicaRequest().setGridSessionId(session.getSessionId())).getReplica();

		String url = gridService
				.createPresignedUrl(admin.getId(), new CreateGridPresignedUrlRequest()
						.setGridSessionId(session.getSessionId()).setReplicaId(userReplica.getReplicaId()))
				.getPresignedUrl();
		BlockingQueue<String> incomingMessages = new LinkedBlockingQueue<>();
		WebSocket websocket = asynchronousJobWorkerHelper.createConnection(url, incomingMessages);

		// wait for the 2000-char initial value to appear in the grid
		List<RowView> fetchedRows = TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
			Optional<GridHeader> header = gridViewManager.readHeader(session.getSessionId(),
					internalCon.getReplicaId());
			if (header.isEmpty()) {
				return Pair.create(false, null);
			}
			List<RowView> rows = gridViewManager.querySinglePage(header.get(), 100L, 0L);
			if (rows.size() != 1) {
				return Pair.create(false, null);
			}
			String rowString = rows.get(0).getRowObject().getData().getRowJsonDocument().toString();
			boolean match = rowString.contains(initialValue) && rowString.contains(initialListElement);
			return Pair.create(match, rows);
		});

		// update the grid cell with a 2000-char value
		Patch patch = new Patch()
				.setPatchId(new LogicalTimestamp().setReplicaId(userReplica.getReplicaId()).setSequenceNumber(101L));
		patch.addNewOperation(
				Operations.insertVector().setVectorId(fetchedRows.get(0).getRowObject().getData().getVectorId())
						.setMap(Map.of(0,
								patch.addNewOperation(
										Operations.newConstant().setValue(new ConValue(ConType.STRING, updatedValue))),
								1, patch.addNewOperation(Operations.newConstant()
										.setValue(new ConValue(ConType.JSON_ARRAY, updatedListValue))))));
		JsonRxMessage message = new JsonRxMessage(JsonRxMessageType.RequestData).setId(102).setMethod("patch")
				.setBody(PatchCompactSerializable.serialize(patch));
		websocket.send(message.toJson());
		asynchronousJobWorkerHelper.waitForMessage((a) -> a.optInt(0) == 5 && a.optInt(1) == message.getId().get(),
				incomingMessages);

		// verify updated value is present in grid
		TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
			Optional<GridHeader> header = gridViewManager.readHeader(session.getSessionId(),
					internalCon.getReplicaId());
			if (header.isEmpty()) {
				return Pair.create(false, null);
			}
			List<RowView> rows = gridViewManager.querySinglePage(header.get(), 100L, 0L);
			if (rows.size() != 1) {
				return Pair.create(false, null);
			}
			String rowString = rows.get(0).getRowObject().getData().getRowJsonDocument().toString();
			boolean match = rowString.contains(updatedValue) && rowString.contains(updatedListElement);
			return Pair.create(match, null);
		});

		// call under test
		asynchronousJobWorkerHelper.assertJobResponse(admin,
				new SynchronizeGridRequest().setGridSessionId(session.getSessionId()), (r) -> {
				}, MAX_WAIT_MS);

		verifyExpectedAnnotations(files.get(0).getId(),
				new JSONObject().put(colName, updatedValue).put(listName, updatedListValue));
	}

	void verifyExpectedAnnotations(String fileId, JSONObject expected) {
		boolean includeDerived = false;
		JSONObject fetched = entityService.getEntityJson(admin.getId(), fileId, includeDerived);
		System.out.println("FileId:  " + fileId);
		System.out.println("Fetched: " + fetched.toString());
		System.out.println("Passed:  " + expected.toString());
		Iterator<String> it = expected.keys();
		while (it.hasNext()) {
			String key = it.next();
			Object expectedValue = expected.get(key);
			Object fetchedValue = fetched.get(key);
			if (expectedValue instanceof JSONArray) {
				assertEquals(((JSONArray) expectedValue).toList(), ((JSONArray) fetchedValue).toList());
			} else {
				assertEquals(expectedValue, fetchedValue);
			}
		}
	}

	public void setFileJSON(String fileId, JSONObject json) {
		boolean includeDerived = false;
		JSONObject fetched = entityService.getEntityJson(admin.getId(), fileId, includeDerived);
		Iterator<String> it = json.keys();
		while (it.hasNext()) {
			String key = it.next();
			Object value = json.get(key);
			if (JSONObject.NULL.equals(value)) {
				fetched.remove(key);
			} else {
				fetched.put(key, json.get(key));
			}
		}
		entityService.updateEntityJson(admin.getId(), fileId, fetched);
	}

	List<FileEntity> createFiles(int count, String folderId, String fileHandleId) {
		List<FileEntity> files = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			FileEntity file = entityService.createEntity(admin.getId(),
					new FileEntity().setName("file" + i).setParentId(folderId).setDataFileHandleId(fileHandleId), null);
			files.add(file);
		}
		return files;
	}

	public void waitForFilesToReplicat(List<FileEntity> files) {
		files.forEach((f) -> {
			FileEntity fetched = entityService.getEntity(admin.getId(), f.getId(), FileEntity.class);
			try {
				asynchronousJobWorkerHelper.waitForObjectReplication(ReplicationType.ENTITY,
						KeyFactory.stringToKey(f.getId()), fetched.getEtag(), MAX_WAIT_MS);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}
}
