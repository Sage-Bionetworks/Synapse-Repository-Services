package org.sagebionetworks.grid.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.CreateGridResponse;
import org.sagebionetworks.repo.model.grid.CreateReplicaRequest;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridCsvImportRequest;
import org.sagebionetworks.repo.model.grid.GridCsvImportResponse;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.SyncType;
import org.sagebionetworks.repo.model.grid.SynchronizeGridRequest;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.service.EntityService;

import au.com.bytecode.opencsv.CSVReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Autowired integration test for synchronizing a RecordSet-sourced grid (PULL
 * and PULL_PUSH). Exercises the full async pipeline: create a RecordSet + grid,
 * advance the RecordSet to a new revision that adds a column, changes a row, and
 * adds a row, then synchronize and verify the grid reconciles columns and merges
 * rows. PULL_PUSH additionally writes the grid back as a new RecordSet version.
 * The grid is read through the internal replica's view (no user replica/websocket
 * needed, since this scenario has no in-grid user edits).
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class GridRecordSetSynchronizationIntegrationTest {

	public static final long MAX_WAIT_MS = GridIntegrationTestUtils.MAX_WAIT_MS;

	@Autowired
	private EntityService entityService;
	@Autowired
	private UserManager userManager;
	@Autowired
	private AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;
	@Autowired
	private GridManager gridManager;
	@Autowired
	private JsonSchemaManager jsonSchemaManager;
	@Autowired
	private GridIntegrationTestUtils gridTestUtils;
	@Autowired
	private FileHandleTestUtils fileHandleTestUtils;

	private UserInfo admin;
	private Project project;

	@BeforeEach
	public void before() {
		admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		project = entityService.createEntity(admin.getId(), new Project().setName(UUID.randomUUID().toString()), null);
	}

	@AfterEach
	public void after() {
		if (project != null) {
			entityService.deleteEntity(admin.getId(), project.getId());
		}
	}

	@Test
	public void testSynchronizeRecordSetPull() throws Exception {
		// v1: columns a (key) and b.
		RecordSet recordSet = createRecordSet(uploadCsv("""
				a,b
				1,one
				2,two
				"""));
		long v1 = recordSet.getVersionNumber();

		// Create a grid from the RecordSet and wait for the internal replica to populate.
		GridSession session = asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateGridRequest().setOwnerPrincipalId(admin.getId().toString())
						.setRecordSetId(recordSet.getId()),
				(CreateGridResponse response) -> {
					assertNotNull(response.getGridSession());
				}, MAX_WAIT_MS).getResponse().getGridSession();
		assertNotNull(session);
		assertEquals(recordSet.getId(), session.getSourceEntityId());
		// the source version is captured at creation
		assertEquals(v1, session.getSourceEntityVersionNumber());

		GridConnectionInfo internalCon = asynchronousJobWorkerHelper.getInternalGridConnection(session.getSessionId(),
				MAX_WAIT_MS);

		// wait for the initial two rows
		gridTestUtils.waitForRows(session.getSessionId(), internalCon.getReplicaId(), Map.of(
				"1", Map.of("b", "one"),
				"2", Map.of("b", "two")));

		// v2: add column c, change row 1's b, add row 3.
		recordSet.setDataFileHandleId(uploadCsv("""
				a,b,c
				1,one-changed,x1
				2,two,x2
				3,three,x3
				"""));
		recordSet.setVersionLabel(null);
		recordSet = entityService.updateEntity(admin.getId(), recordSet, true, null);
		long v2 = recordSet.getVersionNumber();

		// call under test — PULL (explicit; does not write back to the RecordSet)
		asynchronousJobWorkerHelper.assertJobResponse(admin,
				new SynchronizeGridRequest().setGridSessionId(session.getSessionId()).setSyncType(SyncType.PULL),
				(r) -> {
				}, MAX_WAIT_MS);

		// the grid reconciles the new column "c" and merges all source rows (no user
		// edits, so every source value is pulled)
		gridTestUtils.waitForRows(session.getSessionId(), internalCon.getReplicaId(), Map.of(
				"1", Map.of("b", "one-changed", "c", "x1"),
				"2", Map.of("b", "two", "c", "x2"),
				"3", Map.of("b", "three", "c", "x3")));

		// the session's baseline version advances to the pulled revision
		GridSession synced = gridManager.getGridSession(admin, session.getSessionId());
		assertEquals(v2, synced.getSourceEntityVersionNumber());
	}

	/**
	 * A row with an incomplete upsertKey (blank "a") is imported into the grid when
	 * it appears in a newer revision, and is NOT re-imported on a subsequent PULL
	 * against the same revision
	 */
	@Test
	public void testSynchronizeRecordSetPullWithKeylessSourceRow() throws Exception {
		// v1: two complete-key rows.
		RecordSet recordSet = createRecordSet(uploadCsv("""
				a,b
				1,one
				2,two
				"""));

		GridSession session = asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateGridRequest().setOwnerPrincipalId(admin.getId().toString())
						.setRecordSetId(recordSet.getId()),
				(CreateGridResponse response) -> assertNotNull(response.getGridSession()), MAX_WAIT_MS).getResponse().getGridSession();
		assertNotNull(session);

		GridConnectionInfo internalCon = asynchronousJobWorkerHelper.getInternalGridConnection(session.getSessionId(),
				MAX_WAIT_MS);
		gridTestUtils.waitForRowCount(session.getSessionId(), internalCon.getReplicaId(), 2);

		// v2 (newer): adds a row with a blank "a" (incomplete upsertKey).
		recordSet.setDataFileHandleId(uploadCsv("""
				a,b
				1,one
				2,two
				,keyless
				"""));
		recordSet.setVersionLabel(null);
		recordSet = entityService.updateEntity(admin.getId(), recordSet, true, null);

		// call under test — PULL imports the keyless row as a new grid row.
		asynchronousJobWorkerHelper.assertJobResponse(admin,
				new SynchronizeGridRequest().setGridSessionId(session.getSessionId()).setSyncType(SyncType.PULL),
				(r) -> {
				}, MAX_WAIT_MS);

		// 2 complete-key rows + 1 imported keyless row.
		gridTestUtils.waitForRowCount(session.getSessionId(), internalCon.getReplicaId(), 3);

		// call under test — a redundant PULL against the same (now-synced) revision must
		// NOT re-import the keyless row.
		asynchronousJobWorkerHelper.assertJobResponse(admin,
				new SynchronizeGridRequest().setGridSessionId(session.getSessionId()).setSyncType(SyncType.PULL),
				(r) -> {
				}, MAX_WAIT_MS);

		// still 3 rows — no duplication.
		gridTestUtils.waitForRowCount(session.getSessionId(), internalCon.getReplicaId(), 3);
	}

	@Test
	public void testSynchronizeRecordSetPullPush() throws Exception {
		// v1: columns a (key) and b.
		RecordSet recordSet = createRecordSet(uploadCsv("""
				a,b
				1,one
				2,two
				"""));

		// Create a grid from the RecordSet and wait for the internal replica to populate.
		GridSession session = asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateGridRequest().setOwnerPrincipalId(admin.getId().toString())
						.setRecordSetId(recordSet.getId()),
				(CreateGridResponse response) -> {
					assertNotNull(response.getGridSession());
				}, MAX_WAIT_MS).getResponse().getGridSession();
		assertNotNull(session);

		GridConnectionInfo internalCon = asynchronousJobWorkerHelper.getInternalGridConnection(session.getSessionId(),
				MAX_WAIT_MS);
		gridTestUtils.waitForRows(session.getSessionId(), internalCon.getReplicaId(), Map.of(
				"1", Map.of("b", "one"),
				"2", Map.of("b", "two")));

		// v2: add column c, change row 1's b, add row 3.
		recordSet.setDataFileHandleId(uploadCsv("""
				a,b,c
				1,one-changed,x1
				2,two,x2
				3,three,x3
				"""));
		recordSet.setVersionLabel(null);
		recordSet = entityService.updateEntity(admin.getId(), recordSet, true, null);
		long v2 = recordSet.getVersionNumber();

		// call under test — PULL_PUSH pulls v2 into the grid and writes the grid back as
		// a new RecordSet version, built synchronously during the merge.
		asynchronousJobWorkerHelper.assertJobResponse(admin,
				new SynchronizeGridRequest().setGridSessionId(session.getSessionId()).setSyncType(SyncType.PULL_PUSH),
				(r) -> {
				}, MAX_WAIT_MS);

		// the grid reconciles the new column and merges all source rows
		gridTestUtils.waitForRows(session.getSessionId(), internalCon.getReplicaId(), Map.of(
				"1", Map.of("b", "one-changed", "c", "x1"),
				"2", Map.of("b", "two", "c", "x2"),
				"3", Map.of("b", "three", "c", "x3")));

		// the push created a new RecordSet version (> v2) and the session's baseline now
		// points at that pushed version
		RecordSet pushed = entityService.getEntity(admin.getId(), recordSet.getId(), RecordSet.class);
		assertTrue(pushed.getVersionNumber() > v2, "PULL_PUSH should create a new RecordSet version");
		GridSession synced = gridManager.getGridSession(admin, session.getSessionId());
		assertEquals(pushed.getVersionNumber(), synced.getSourceEntityVersionNumber());
	}

	/**
	 * Regression test for PLFM-9880: importing a CSV added internal-replica-attributed
	 * data that a subsequent PULL_PUSH synchronization would revert.
	 * The import must attribute its writes to the importing user so the imported rows are
	 * treated as user changes and survive the sync.
	 */
	@Test
	public void testSynchronizeRecordSetPullPushAfterCsvImport() throws Exception {
		// v1: columns a (key) and b.
		RecordSet recordSet = createRecordSet(uploadCsv("""
				a,b
				1,one
				2,two
				"""));
		long v1 = recordSet.getVersionNumber();

		// Create a grid from the RecordSet and wait for the internal replica to populate.
		GridSession session = asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateGridRequest().setOwnerPrincipalId(admin.getId().toString())
						.setRecordSetId(recordSet.getId()),
				(CreateGridResponse response) -> {
					assertNotNull(response.getGridSession());
				}, MAX_WAIT_MS).getResponse().getGridSession();
		assertNotNull(session);

		GridConnectionInfo internalCon = asynchronousJobWorkerHelper.getInternalGridConnection(session.getSessionId(),
				MAX_WAIT_MS);
		gridTestUtils.waitForRows(session.getSessionId(), internalCon.getReplicaId(), Map.of(
				"1", Map.of("b", "one"),
				"2", Map.of("b", "two")));

		// Import a CSV that exercises both loss paths in one job: an update to a row
		// that already exists in the RecordSet (1), and a brand-new row (3).
		String importFileHandleId = uploadCsv("""
				a,b
				1,one-imported
				3,three
				""");

		asynchronousJobWorkerHelper.assertJobResponse(admin,
				new GridCsvImportRequest().setSessionId(session.getSessionId()).setFileHandleId(importFileHandleId)
						.setCsvDescriptor(new CsvTableDescriptor().setIsFirstLineHeader(true))
						.setSchema(List.of(
								new ColumnModel().setName("a").setColumnType(ColumnType.INTEGER),
								new ColumnModel().setName("b").setColumnType(ColumnType.STRING))),
				(GridCsvImportResponse r) -> {
					assertEquals(2L, r.getTotalCount());
					assertEquals(1L, r.getUpdatedCount());
					assertEquals(1L, r.getCreatedCount());
				}, MAX_WAIT_MS);

		// the import landed in the grid
		gridTestUtils.waitForRows(session.getSessionId(), internalCon.getReplicaId(), Map.of(
				"1", Map.of("b", "one-imported"),
				"2", Map.of("b", "two"),
				"3", Map.of("b", "three")));

		// the imported cell is attributed to the importing user
		gridTestUtils.waitForCellAttribution(session.getSessionId(), internalCon.getReplicaId(), "1", "b", true);

		// call under test — PULL_PUSH must keep the import instead of reverting it
		asynchronousJobWorkerHelper.assertJobResponse(admin,
				new SynchronizeGridRequest().setGridSessionId(session.getSessionId()).setSyncType(SyncType.PULL_PUSH),
				(r) -> {
				}, MAX_WAIT_MS);

		// the imported row (3) and imported cell (1.b) both survive the sync
		gridTestUtils.waitForRows(session.getSessionId(), internalCon.getReplicaId(), Map.of(
				"1", Map.of("b", "one-imported"),
				"2", Map.of("b", "two"),
				"3", Map.of("b", "three")));

		// the push reached the RecordSet as a new version
		RecordSet pushed = entityService.getEntity(admin.getId(), recordSet.getId(), RecordSet.class);
		assertTrue(pushed.getVersionNumber() > v1, "PULL_PUSH should create a new RecordSet version");

		String csv = asynchronousJobWorkerHelper.downloadFileHandleFromS3(pushed.getDataFileHandleId());
		assertEquals(Map.of("1", "one-imported", "2", "two", "3", "three"), parseCsvColumns(csv, "a", "b"));
	}

	/**
	 * Parses a CSV's header row to locate {@code keyColumn} and {@code valueColumn},
	 * then returns a map from every data row's key value to its value column,
	 * immune to the exporter's column ordering and quoting.
	 */
	private Map<String, String> parseCsvColumns(String csv, String keyColumn, String valueColumn)
			throws IOException {
		try (CSVReader reader = new CSVReader(new StringReader(csv))) {
			List<String> header = List.of(reader.readNext());
			int keyIndex = header.indexOf(keyColumn);
			int valueIndex = header.indexOf(valueColumn);
			Map<String, String> result = new LinkedHashMap<>();
			String[] row;
			while ((row = reader.readNext()) != null) {
				result.put(row[keyIndex], row[valueIndex]);
			}
			return result;
		}
	}

	/**
	 * A PULL must not re-attribute the cells a user changed. The merge re-publishes a
	 * conflicting row under the service replica, which would flip the user's edits to
	 * "system" attribution and cause a subsequent PULL to revert them. With the
	 * preserve-user-attribution fix, a user-divergent cell keeps its user-owned CRDT
	 * node across repeated PULLs (so the edit survives), while a user edit that
	 * coincides with the source value is normalized to the service replica.
	 */
	@Test
	public void testSynchronizeRecordSetPullPreservesUserAttribution() throws Exception {
		// v1: key column a, plus b and c.
		RecordSet recordSet = createRecordSet(uploadCsv("""
				a,b,c
				1,one,red
				2,two,blue
				"""));

		GridSession session = asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateGridRequest().setOwnerPrincipalId(admin.getId().toString())
						.setRecordSetId(recordSet.getId()),
				(CreateGridResponse response) -> {
					assertNotNull(response.getGridSession());
				}, MAX_WAIT_MS).getResponse().getGridSession();
		assertNotNull(session);

		GridConnectionInfo internalCon = asynchronousJobWorkerHelper.getInternalGridConnection(session.getSessionId(),
				MAX_WAIT_MS);
		gridTestUtils.waitForRows(session.getSessionId(), internalCon.getReplicaId(), Map.of(
				"1", Map.of("b", "one", "c", "red"),
				"2", Map.of("b", "two", "c", "blue")));

		// A user edits row "1": sets c to a value that diverges from the source ("green"
		// vs "red"), and sets b to a value that coincides with the source ("one").
		GridReplica userReplica = gridManager
				.createReplica(admin, new CreateReplicaRequest().setGridSessionId(session.getSessionId())).getReplica();
		gridTestUtils.applyUserCellEdits(admin, session.getSessionId(), internalCon.getReplicaId(), userReplica.getReplicaId(), "1",
				Map.of("b", new ConValue(ConType.STRING, "one"), "c", new ConValue(ConType.STRING, "green")));

		// the user's edits are visible in the grid before syncing
		gridTestUtils.waitForRows(session.getSessionId(), internalCon.getReplicaId(), Map.of(
				"1", Map.of("b", "one", "c", "green"),
				"2", Map.of("b", "two", "c", "blue")));

		// PULL twice against the same (unchanged) source revision
		for (int i = 0; i < 2; i++) {
			asynchronousJobWorkerHelper.assertJobResponse(admin,
					new SynchronizeGridRequest().setGridSessionId(session.getSessionId()).setSyncType(SyncType.PULL),
					(r) -> {
					}, MAX_WAIT_MS);
		}

		// The user's divergent edit to c survives the double PULL; b stays "one".
		gridTestUtils.waitForRows(session.getSessionId(), internalCon.getReplicaId(), Map.of(
				"1", Map.of("b", "one", "c", "green"),
				"2", Map.of("b", "two", "c", "blue")));

		// Attribution: the divergent cell (c) keeps its user-owned node; the coincidental
		// cell (b) was normalized to the service replica. The cells' rendered VALUES
		// already matched before the double-PULL (waitForRows above can't detect an
		// attribution-only change), so poll for attribution directly rather than
		// reading the header once immediately after the value-based wait.
		gridTestUtils.waitForCellAttribution(session.getSessionId(), internalCon.getReplicaId(), "1", "c", true);
		gridTestUtils.waitForCellAttribution(session.getSessionId(), internalCon.getReplicaId(), "1", "b", false);
	}

	/**
	 * A schema change picked up during PULL synchronization (the RecordSet's
	 * bound schema is re-bound to a new $id) must re-validate EVERY row in the
	 * grid, including rows whose underlying data never changes across the
	 * resync. This is the behavior that distinguishes
	 * {@code GridReplicaValidationManager#validateSchemaChange} from the normal
	 * data-change-triggered validation path, which opts out of revalidating rows
	 * whose data hasn't changed.
	 */
	@Test
	public void testSynchronizeRecordSetSchemaChangeRevalidatesAllRows() throws Exception {
		Organization org = asynchronousJobWorkerHelper.getOrCreateOrganization(admin.getId(),
				"org" + UUID.randomUUID().toString().replace("-", ""));

		String schema1Id = createSchema(org, "schemaVOne", List.of("a", "b"));

		// v1: columns a (key) and b. Bind the v1 schema to the RecordSet *before*
		// creating the grid, so the grid is created already bound to it.
		RecordSet recordSet = createRecordSet(uploadCsv("""
				a,b
				1,one
				2,two
				"""));
		entityService.bindSchemaToEntity(admin.getId(),
				new BindSchemaToEntityRequest().setEntityId(recordSet.getId()).setSchema$id(schema1Id));

		GridSession session = asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateGridRequest().setOwnerPrincipalId(admin.getId().toString())
						.setRecordSetId(recordSet.getId()),
				(CreateGridResponse response) -> {
					assertNotNull(response.getGridSession());
				}, MAX_WAIT_MS).getResponse().getGridSession();
		assertNotNull(session);
		assertEquals(schema1Id, session.getGridJsonSchema$Id());

		GridConnectionInfo internalCon = asynchronousJobWorkerHelper.getInternalGridConnection(session.getSessionId(),
				MAX_WAIT_MS);
		gridTestUtils.waitForRows(session.getSessionId(), internalCon.getReplicaId(), Map.of(
				"1", Map.of("b", "one"),
				"2", Map.of("b", "two")));

		// wait for the initial validation pass (triggered by the grid's creation) to
		// populate a validation result/constant for every row.
		Map<String, LogicalTimestamp> baseline = gridTestUtils.waitForValidationConstantIds(session.getSessionId(),
				internalCon.getReplicaId(), Set.of("1", "2"));

		// Re-bind a new schema version to the RecordSet. Neither row's data changes at all.
		String schema2Id = createSchema(org, "schemaVTwo", List.of("a", "b"));
		entityService.bindSchemaToEntity(admin.getId(),
				new BindSchemaToEntityRequest().setEntityId(recordSet.getId()).setSchema$id(schema2Id));

		// call under test (indirectly) — PULL synchronization observes the
		// RecordSet's newly-bound schema and calls
		// GridManagerImpl#updateSessionSchemaId, which now invalidates the
		// session's existing validation results.
		asynchronousJobWorkerHelper.assertJobResponse(admin,
				new SynchronizeGridRequest().setGridSessionId(session.getSessionId()).setSyncType(SyncType.PULL),
				(r) -> {
				}, MAX_WAIT_MS);

		GridSession resynced = gridManager.getGridSession(admin, session.getSessionId());
		assertEquals(schema2Id, resynced.getGridJsonSchema$Id());

		// Every row — including row "2", whose data is identical under both schema
		// versions — must be re-validated: its validation constant must advance
		// beyond the baseline captured before the schema change.
		gridTestUtils.waitForValidationConstantIdsAdvanced(session.getSessionId(), internalCon.getReplicaId(), baseline);
	}

	private String createSchema(Organization org, String name, List<String> propertyNames) {
		Map<String, JsonSchema> properties = new HashMap<>();
		for (String propertyName : propertyNames) {
			properties.put(propertyName, new JsonSchema().setType(Type.string));
		}
		JsonSchema schema = new JsonSchema().set$id(org.getName() + "-" + name).setProperties(properties);
		return jsonSchemaManager.createJsonSchema(admin, new CreateSchemaRequest().setSchema(schema))
				.getNewVersionInfo().get$id();
	}

	private String uploadCsv(String content) throws Exception {
		return fileHandleTestUtils.uploadCsv(admin, content);
	}

	private RecordSet createRecordSet(String dataFileHandleId) {
		RecordSet rs = new RecordSet().setName(UUID.randomUUID().toString()).setUpsertKey(List.of("a"));
		rs.setParentId(project.getId());
		rs.setDataFileHandleId(dataFileHandleId);
		return entityService.createEntity(admin.getId(), rs, null);
	}
}
