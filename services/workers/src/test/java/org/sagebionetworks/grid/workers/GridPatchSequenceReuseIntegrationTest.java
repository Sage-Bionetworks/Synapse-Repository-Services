package org.sagebionetworks.grid.workers;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.grid.db.GridIndexManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.GridReplicaManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.GridReplicaPatchBuilderManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.validation.GridReplicaValidationManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisher;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.dbo.grid.GridDao;
import org.sagebionetworks.repo.model.dbo.schema.EntitySchemaValidationResultDao;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.CreateGridResponse;
import org.sagebionetworks.repo.model.grid.EventContext;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.EventType;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.GridUtils;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.update.LiteralSetValue;
import org.sagebionetworks.repo.model.grid.update.Update;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.repo.service.EntityService;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Server-side grid writers (VALIDATION, AGENT) must never reuse the sequence numbers of their earlier patches
 * (PLFM-10043). A new stack starts with an empty index database that is rebuilt from the migrated patches, so each
 * test empties the index database and checks the writers before, during, and after the rebuild.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class GridPatchSequenceReuseIntegrationTest {

	private static final long INTERNAL_REPLICA_ID = 66534L;

	public static final long MAX_WAIT_MS = 120_000;

	/**
	 * How long to wait for a built patch to be stored. A patch whose ID is reused exactly is never stored, so this
	 * wait is expected to run out in that case.
	 */
	private static final long PATCH_STORED_WAIT_MS = 30_000;

	private static final int ROW_COUNT = 3;

	/**
	 * A writer replica's state before the index database is emptied.
	 *
	 * @param replicaId         the writer's replica ID
	 * @param clockEnd          the next free sequence number after the writer's latest patch, as recorded by the
	 *                          internal replica
	 * @param highWater         the last sequence number the writer has used, as persisted in the main database
	 * @param storedSequences   the sequence numbers of the writer's patches stored in GRID_PATCH
	 */
	record WriterState(Long replicaId, Long clockEnd, Long highWater, Set<Long> storedSequences) {
	}

	record GridFixture(GridSession session, GridConnectionInfo internalConnection,
			GridConnectionInfo agentConnection, WriterState validation, WriterState agent) {
	}

	@Autowired
	private UserManager userManager;

	@Autowired
	private AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private EntityService entityService;

	@Autowired
	private FileHandleManager fileHandleManager;

	@Autowired
	private JsonSchemaManager jsonSchemaManager;

	@Autowired
	private EntitySchemaValidationResultDao schemaValidationResultDao;

	@Autowired
	private GridReplicaViewManager gridViewManager;

	@Autowired
	private GridManager gridManager;

	@Autowired
	private GridDao gridDao;

	@Autowired
	private GridIndexDao gridIndexDao;

	@Autowired
	private GridIndexManager gridIndexManager;

	@Autowired
	private GridReplicaValidationManager gridReplicaValidationManager;

	@Autowired
	private GridReplicaPatchBuilderManager patchBuilderManager;

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

	/**
	 * On a new stack, before the grid session change is rebroadcast, the internal replica does not exist, so the
	 * server-side writers have no header to build a patch from.
	 */
	@Test
	public void testServerWritesWithEmptyIndexBeforeRebroadcast() throws Exception {
		GridFixture fixture = setupGridWithValidationAndAgentPatches();
		String sessionId = fixture.session().getSessionId();

		// simulate the empty index database of a new stack
		gridIndexDao.truncateAll();

		assertTrue(gridViewManager.readHeader(sessionId, INTERNAL_REPLICA_ID).isEmpty(),
				"The internal replica must not exist before the rebroadcast");

		// call under test
		gridReplicaValidationManager.validateAfterSchemaChange(sessionId);

		// validation skipped without a header, so no patch was built and nothing was stored
		assertEquals(Optional.of(fixture.validation().highWater()),
				getWriterHighWater(sessionId, fixture.validation().replicaId()));
		assertEquals(Optional.of(fixture.agent().highWater()), getWriterHighWater(sessionId, fixture.agent().replicaId()));
		assertEquals(fixture.validation().storedSequences(),
				listStoredSequences(sessionId, fixture.validation().replicaId()));
		assertEquals(fixture.agent().storedSequences(), listStoredSequences(sessionId, fixture.agent().replicaId()));
	}

	/**
	 * While the rebuild is in progress, the internal replica has applied the snapshot but not the patches written
	 * after it. Writes made from that partial header must still start after each writer's latest stored patch.
	 */
	@Test
	public void testServerWritesWithPartiallyRebuiltIndex() throws Exception {
		GridFixture fixture = setupGridWithValidationAndAgentPatches();
		String sessionId = fixture.session().getSessionId();

		// simulate the empty index database of a new stack
		gridIndexDao.truncateAll();

		// Rebuild the internal replica only up to the snapshot, the first step of the rebroadcast. The open
		// synchronize-clock chain holds off any background synchronization, which would apply the remaining patches.
		gridIndexManager.startMessageChain(sessionId, INTERNAL_REPLICA_ID, GridReplicaManager.SYNCHRONIZE_CLOCK);
		applyLatestSnapshotToInternalReplica(fixture);

		GridHeader partialHeader = gridViewManager.readHeader(sessionId, INTERNAL_REPLICA_ID).orElseThrow();
		assertTrue(partialHeader.getClockSequenceMaximum() < fixture.validation().clockEnd(),
				"The snapshot must not include the VALIDATION patches");
		assertTrue(partialHeader.getClockSequenceMaximum() < fixture.agent().clockEnd(),
				"The snapshot must not include the AGENT patches");

		// call under test
		gridReplicaValidationManager.validateAfterSchemaChange(sessionId);
		// call under test
		gridManager.executeGridUpdate(partialHeader, fixture.agentConnection(), createSetNameUpdate("agent-3"));

		assertAll(() -> assertNoSequenceReuse(sessionId, fixture.validation()),
				() -> assertNoSequenceReuse(sessionId, fixture.agent()));
	}

	/**
	 * After the rebroadcast, the internal replica has applied every migrated patch, so its header covers the end
	 * of each writer's latest patch.
	 */
	@Test
	public void testServerWritesAfterFullRebroadcast() throws Exception {
		GridFixture fixture = setupGridWithValidationAndAgentPatches();
		String sessionId = fixture.session().getSessionId();

		// simulate the empty index database of a new stack
		gridIndexDao.truncateAll();

		// Trigger the same type of event sent by ChangeSentMessageSynchWorker
		ChangeMessage change = changeDao.replaceChange(
				new ChangeMessage().setChangeType(ChangeType.UPDATE).setObjectType(ObjectType.GRID_SESSION)
						.setObjectId(GridUtils.gridSessionIdAsLong(sessionId).toString()));
		repositoryMessagePublisher.publishBatchToTopic(ObjectType.GRID_SESSION, List.of(change));

		waitForInternalReplicaToCatchUp(sessionId, fixture.validation(), fixture.agent());

		// The snapshot applied by the rebuild triggers validation, which may run while the rebuild is still partial.
		WriterState validationBeforeWrite = fixture.validation();
		if (waitForOptionalHighWaterChange(sessionId, fixture.validation())) {
			assertNoSequenceReuse(sessionId, fixture.validation());
			validationBeforeWrite = captureWriterState(sessionId, fixture.validation().replicaId());
		}

		GridHeader fullHeader = gridViewManager.readHeader(sessionId, INTERNAL_REPLICA_ID).orElseThrow();

		// call under test
		gridReplicaValidationManager.validateAfterSchemaChange(sessionId);
		// call under test
		gridManager.executeGridUpdate(fullHeader, fixture.agentConnection(), createSetNameUpdate("agent-3"));

		WriterState validationBefore = validationBeforeWrite;
		assertAll(() -> assertNoSequenceReuse(sessionId, validationBefore),
				() -> assertNoSequenceReuse(sessionId, fixture.agent()));
	}

	/**
	 * Create a RecordSet grid with a bound schema, so it has a snapshot and a VALIDATION replica, then make two
	 * agent updates, each followed by revalidation. Waits until the internal replica has applied every patch.
	 */
	GridFixture setupGridWithValidationAndAgentPatches() throws Exception {
		Project project = entityService.createEntity(admin.getId(), new Project().setName("SequenceReuse"), null);
		String csvContent = "id,name\n1,one\n2,two\n3,three\n";
		S3FileHandle fileHandle = fileHandleManager.createFileFromByteArray(admin.getId().toString(), new Date(),
				csvContent.getBytes(StandardCharsets.UTF_8), "data.csv", ContentType.create("text/csv"), null);
		RecordSet recordSet = entityService.createEntity(admin.getId(),
				new RecordSet().setParentId(project.getId()).setName("recordSet")
						.setDataFileHandleId(fileHandle.getId()).setUpsertKey(List.of("id")),
				null);
		String schemaId = createJsonSchema(
				Map.of("id", new JsonSchema().setType(Type.integer), "name", new JsonSchema().setType(Type.string)),
				List.of("id")).getNewVersionInfo().get$id();
		entityService.bindSchemaToEntity(admin.getId(),
				new BindSchemaToEntityRequest().setEntityId(recordSet.getId()).setSchema$id(schemaId));

		GridSession session = asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateGridRequest().setRecordSetId(recordSet.getId()), (CreateGridResponse response) -> {
					assertNotNull(response);
					assertNotNull(response.getGridSession());
				}, MAX_WAIT_MS).getResponse().getGridSession();
		String sessionId = session.getSessionId();

		// the VALIDATION replica writes the validation results of the snapshot rows
		waitForRows(sessionId, row -> row.getRowValidationResults() != null);

		GridReplica agentReplica = gridManager.createAgentReplica(admin, session);
		GridConnectionInfo agentConnection = TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
			Optional<GridConnectionInfo> connection = gridManager.getConnection(sessionId,
					agentReplica.getReplicaId());
			return Pair.create(connection.isPresent(), connection.orElse(null));
		});

		GridConnectionInfo validationConnection = gridDao.getSingletonConnection(sessionId, EventSource.VALIDATION)
				.orElseThrow();
		for (String name : List.of("agent-1", "agent-2")) {
			GridHeader header = gridViewManager.readHeader(sessionId, INTERNAL_REPLICA_ID).orElseThrow();
			gridManager.executeGridUpdate(header, agentConnection, createSetNameUpdate(name));
			waitForRows(sessionId, row -> name.equals(getName(row)));
			// The next update must read a header that includes this revalidation. Otherwise its constants can be
			// older than the validation results, and the rows would not be revalidated.
			waitForValidationAfterLastAgentUpdate(sessionId, validationConnection.getReplicaId(),
					agentReplica.getReplicaId());
		}

		GridConnectionInfo internalConnection = gridDao.getSingletonConnection(sessionId, EventSource.INTERNAL)
				.orElseThrow();
		return new GridFixture(session, internalConnection, agentConnection,
				captureWriterState(sessionId, validationConnection.getReplicaId()),
				captureWriterState(sessionId, agentReplica.getReplicaId()));
	}

	/**
	 * Wait until the VALIDATION replica has written a patch after the last agent update, and the internal replica
	 * has applied every stored patch.
	 */
	void waitForValidationAfterLastAgentUpdate(String sessionId, Long validationReplicaId, Long agentReplicaId)
			throws Exception {
		TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
			Optional<Long> validationEnd = getWriterHighWater(sessionId, validationReplicaId);
			Optional<Long> agentEnd = getWriterHighWater(sessionId, agentReplicaId);
			Optional<Long> internalValidationEnd = getInternalClock(sessionId, validationReplicaId);
			Optional<Long> internalAgentEnd = getInternalClock(sessionId, agentReplicaId);
			boolean allPatchesApplied = patchBuilderManager
					.getCurrentClockIfAllPatchesApplied(sessionId, INTERNAL_REPLICA_ID).isPresent();
			System.out.println(String.format(
					"Waiting for validation after the last agent update: validationEnd=%s agentEnd=%s "
							+ "internalValidationEnd=%s internalAgentEnd=%s allPatchesApplied=%s",
					validationEnd, agentEnd, internalValidationEnd, internalAgentEnd, allPatchesApplied));
			if (validationEnd.isEmpty() || agentEnd.isEmpty()) {
				return Pair.create(false, null);
			}
			// a validation patch built after the last agent update starts after the agent patch's end
			boolean validatedAfterUpdate = validationEnd.get() > agentEnd.get();
			boolean internalCaughtUp = isLastSequenceApplied(validationEnd.get(), internalValidationEnd)
					&& isLastSequenceApplied(agentEnd.get(), internalAgentEnd) && allPatchesApplied;
			return Pair.create(validatedAfterUpdate && internalCaughtUp, null);
		});
	}

	void waitForInternalReplicaToCatchUp(String sessionId, WriterState... writers) throws Exception {
		TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
			boolean caughtUp = patchBuilderManager.getCurrentClockIfAllPatchesApplied(sessionId, INTERNAL_REPLICA_ID)
					.isPresent();
			for (WriterState writer : writers) {
				caughtUp &= getInternalClock(sessionId, writer.replicaId()).map(seq -> seq >= writer.clockEnd())
						.orElse(false);
			}
			System.out.println("Waiting for the internal replica to apply all migrated patches...");
			return Pair.create(caughtUp, null);
		});
	}

	void waitForRows(String sessionId, Predicate<RowView> condition) throws Exception {
		TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
			Optional<GridHeader> header = gridViewManager.readHeader(sessionId, INTERNAL_REPLICA_ID);
			if (header.isEmpty()) {
				return Pair.create(false, null);
			}
			List<RowView> rows = gridViewManager.querySinglePage(header.get(), 100L, 0L);
			System.out.println("Waiting for grid rows: " + rows.stream()
					.map(r -> r.getRowObject().getData().getRowJsonDocument() + " validation=" + r.getRowValidationResults())
					.collect(Collectors.toList()));
			return Pair.create(rows.size() == ROW_COUNT && rows.stream().allMatch(condition), null);
		});
	}

	/**
	 * Assert that the writer built a new patch after the given state was captured, and that the patch starts at or
	 * after the end of the writer's earlier patches.
	 *
	 * @param before the writer's state before the write
	 */
	void assertNoSequenceReuse(String sessionId, WriterState before) throws Exception {
		// the patch builder persists the writer's high-water mark before it publishes the patches of a change set
		TimeUtils.waitFor(MAX_WAIT_MS, 1000L, () -> {
			System.out.println("Waiting for replica " + before.replicaId() + " to build a patch...");
			return Pair.create(hasHighWaterChanged(sessionId, before), null);
		});

		Set<Long> newSequences = waitForNewSequences(sessionId, before);
		assertFalse(newSequences.isEmpty(), String.format(
				"Replica %d built a patch, but no new patch was stored: it reused the ID of an earlier patch exactly "
						+ "and the duplicate was dropped. Earlier patches: %s, end of the earlier patches: %d",
				before.replicaId(), before.storedSequences(), before.clockEnd()));
		Long firstNewSequence = Collections.min(newSequences);
		assertTrue(firstNewSequence >= before.clockEnd(), String.format(
				"Replica %d stored a patch at sequence %d, inside the range of its earlier patches %s "
						+ "(end of the earlier patches: %d)",
				before.replicaId(), firstNewSequence, before.storedSequences(), before.clockEnd()));
	}

	Set<Long> waitForNewSequences(String sessionId, WriterState before) throws InterruptedException {
		long start = System.currentTimeMillis();
		Set<Long> newSequences = Collections.emptySet();
		while (System.currentTimeMillis() - start < PATCH_STORED_WAIT_MS) {
			newSequences = listStoredSequences(sessionId, before.replicaId()).stream()
					.filter(seq -> !before.storedSequences().contains(seq)).collect(Collectors.toSet());
			if (!newSequences.isEmpty()) {
				return newSequences;
			}
			System.out.println("Waiting for replica " + before.replicaId() + " to store a new patch...");
			Thread.sleep(1000L);
		}
		return newSequences;
	}

	/**
	 * Wait a bounded time for the writer to build a patch. Unlike the other waits, it is not a failure if it never
	 * does.
	 *
	 * @return true if the writer built a patch after the given state was captured
	 */
	boolean waitForOptionalHighWaterChange(String sessionId, WriterState before) throws InterruptedException {
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < PATCH_STORED_WAIT_MS) {
			if (hasHighWaterChanged(sessionId, before)) {
				return true;
			}
			Thread.sleep(1000L);
		}
		return false;
	}

	boolean hasHighWaterChanged(String sessionId, WriterState before) {
		return !Optional.of(before.highWater()).equals(getWriterHighWater(sessionId, before.replicaId()));
	}

	/**
	 * The writer's high-water mark is the last sequence number it used, while the internal replica's clock for the
	 * writer is the next free one.
	 */
	boolean isLastSequenceApplied(Long writerHighWater, Optional<Long> internalClock) {
		return internalClock.map(next -> next == writerHighWater + 1).orElse(false);
	}

	WriterState captureWriterState(String sessionId, Long replicaId) {
		return new WriterState(replicaId, getInternalClock(sessionId, replicaId).orElseThrow(),
				getWriterHighWater(sessionId, replicaId).orElseThrow(), listStoredSequences(sessionId, replicaId));
	}

	Set<Long> listStoredSequences(String sessionId, Long replicaId) {
		return gridDao
				.listMissingPatchInfoForClock(sessionId,
						List.of(new LogicalTimestamp().setReplicaId(replicaId).setSequenceNumber(0L)), 10_000L)
				.stream().map(p -> p.getPatchId()).filter(id -> replicaId.equals(id.getReplicaId()))
				.map(LogicalTimestamp::getSequenceNumber).collect(Collectors.toSet());
	}

	/**
	 * The writer's persisted high-water mark, which the patch builder sets on each build to the last sequence
	 * number reserved.
	 */
	Optional<Long> getWriterHighWater(String sessionId, Long replicaId) {
		return gridDao.getReplicaSequenceHighWater(sessionId, replicaId);
	}

	/**
	 * The internal replica's clock entry for the given writer: the end of the writer's patches it has applied.
	 */
	Optional<Long> getInternalClock(String sessionId, Long writerReplicaId) {
		return gridIndexDao.getClockSequenceNumber(sessionId, INTERNAL_REPLICA_ID, writerReplicaId);
	}

	void applyLatestSnapshotToInternalReplica(GridFixture fixture) throws Exception {
		URL snapshotUrl = gridManager.getLatestSnapshotPresignedUrl(new EventContext(EventType.MESSAGE,
				EventSource.INTERNAL, fixture.internalConnection().getConnectionId())).orElseThrow();
		Path snapshotFile = Files.createTempFile("grid-snapshot-", ".cbor");
		try {
			HttpResponse<Path> response = HttpClient.newHttpClient().send(
					HttpRequest.newBuilder().uri(snapshotUrl.toURI()).GET().build(),
					HttpResponse.BodyHandlers.ofFile(snapshotFile));
			assertEquals(200, response.statusCode());
			gridIndexManager.applySnapshot(fixture.session().getSessionId(), INTERNAL_REPLICA_ID, snapshotFile);
		} finally {
			Files.deleteIfExists(snapshotFile);
		}
	}

	/**
	 * A single grid update that sets the name of every row.
	 */
	JSONObject createSetNameUpdate(String name) {
		Update update = new Update().setSet(List.of(new LiteralSetValue().setColumnName("name").setValue(name)));
		return new JSONObject(JDOSecondaryPropertyUtils.createJSONFromObject(update));
	}

	String getName(RowView row) {
		return new JSONObject(row.getRowObject().getData().getRowJsonDocument().toString()).optString("name");
	}

	CreateSchemaResponse createJsonSchema(Map<String, JsonSchema> properties, List<String> requiredProperties)
			throws Exception {
		Organization org = asynchronousJobWorkerHelper.getOrCreateOrganization(admin.getId(), "gridtestorg");
		JsonSchema jsonSchema = new JsonSchema().set$id(org.getName() + "-sequenceReuseSchema")
				.setProperties(properties).setRequired(requiredProperties);
		return asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateSchemaRequest().setDryRun(false).setSchema(jsonSchema), (CreateSchemaResponse response) -> {
					assertNotNull(response);
				}, MAX_WAIT_MS).getResponse();
	}
}
