package org.sagebionetworks.grid.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.synch.GridSourceStatusManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.CreateGridResponse;
import org.sagebionetworks.repo.model.grid.EventSource;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.GridSourceChangeType;
import org.sagebionetworks.repo.model.grid.GridSourceStatus;
import org.sagebionetworks.repo.model.grid.SyncType;
import org.sagebionetworks.repo.model.grid.SynchronizeGridRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Autowired integration test for the source status of a RecordSet-sourced grid.
 * Walks a session through the changes a client would be prompted to PULL for — a
 * new revision that carries the same data, a new revision with a new CSV, an
 * upsert key and a CSV descriptor each edited without asking for a new version,
 * a re-bound JSON schema, the deletion of the synchronized baseline revision, an
 * unbound schema, and a schema bound again after being unbound — synchronizing
 * along the way to confirm that every reported change is one a PULL can actually
 * resolve.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class GridSourceStatusIntegrationTest {

	public static final long MAX_WAIT_MS = GridIntegrationTestUtils.MAX_WAIT_MS;

	@Autowired
	private EntityService entityService;
	@Autowired
	private UserManager userManager;
	@Autowired
	private AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;
	@Autowired
	private GridSourceStatusManager gridSourceStatusManager;
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
	public void testGetGridSourceStatusWithSourceChanges() throws Exception {
		Organization org = asynchronousJobWorkerHelper.getOrCreateOrganization(admin.getId(),
				"org" + UUID.randomUUID().toString().replace("-", ""));
		String schemaOneId = createSchema(org, "schemaVOne", List.of("a", "b"));

		// v1: columns a (key) and b, bound to the first schema before the grid exists.
		RecordSet recordSet = createRecordSet(uploadCsv("""
				a,b
				1,one
				2,two
				"""));
		entityService.bindSchemaToEntity(admin.getId(),
				new BindSchemaToEntityRequest().setEntityId(recordSet.getId()).setSchema$id(schemaOneId));
		String recordSetId = recordSet.getId();

		GridSession session = asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateGridRequest().setOwnerPrincipalId(admin.getId().toString()).setRecordSetId(recordSetId),
				(CreateGridResponse response) -> {
					assertNotNull(response.getGridSession());
				}, MAX_WAIT_MS).getResponse().getGridSession();
		assertNotNull(session);
		assertEquals(schemaOneId, session.getGridJsonSchema$Id());
		long syncedVersion = recordSet.getVersionNumber();

		GridConnectionInfo internalCon = asynchronousJobWorkerHelper.getInternalGridConnection(session.getSessionId(),
				MAX_WAIT_MS);
		gridTestUtils.waitForRows(session.getSessionId(), internalCon.getReplicaId(), Map.of(
				"1", Map.of("b", "one"),
				"2", Map.of("b", "two")));

		// call under test — a freshly created session is in sync with its source.
		assertEquals(status(session, syncedVersion, recordSet), getStatus(session));

		// v2: a new revision that re-uses the v1 file handle, so the data is unchanged.
		recordSet = createNewVersion(recordSet, recordSet.getDataFileHandleId());

		// call under test — a newer revision alone is not an importable change, but the
		// client can see the version moved.
		assertEquals(status(session, syncedVersion, recordSet), getStatus(session));

		// v3: a new revision with a new CSV.
		recordSet = createNewVersion(recordSet, uploadCsv("""
				a,b
				1,one-changed
				2,two
				"""));
		long v3 = recordSet.getVersionNumber();

		// call under test — the new CSV is reported as a data change.
		assertEquals(status(session, syncedVersion, recordSet, GridSourceChangeType.DATA), getStatus(session));

		syncedVersion = pull(session, recordSet);

		// call under test — the PULL advanced the baseline to v3, clearing the change.
		assertEquals(status(session, syncedVersion, recordSet), getStatus(session));

		// Changing the upsertKey without asking for a new version: the update is
		// auto-versioned so the synced revision keeps the key the grid was built with.
		recordSet.setUpsertKey(List.of("a", "b"));
		recordSet = entityService.updateEntity(admin.getId(), recordSet, false, null);
		long upsertKeyVersion = recordSet.getVersionNumber();
		assertTrue(upsertKeyVersion > v3);

		// call under test — the new key is reported against the synced revision.
		assertEquals(status(session, syncedVersion, recordSet, GridSourceChangeType.UPSERT_KEY),
				getStatus(session));

		syncedVersion = pull(session, recordSet);

		// call under test — the PULL re-baselined onto the revision carrying the new key.
		assertEquals(status(session, syncedVersion, recordSet), getStatus(session));

		// Changing the CSV descriptor without asking for a new version is auto-versioned
		// the same way. The CSV has no quoted values, so a new quote character changes
		// the descriptor without changing how its rows parse.
		recordSet.setCsvDescriptor(new CsvTableDescriptor().setIsFirstLineHeader(true).setQuoteCharacter("'"));
		recordSet = entityService.updateEntity(admin.getId(), recordSet, false, null);
		long csvDescriptorVersion = recordSet.getVersionNumber();
		assertTrue(csvDescriptorVersion > upsertKeyVersion);

		// call under test — the new descriptor is reported against the synced revision.
		assertEquals(status(session, syncedVersion, recordSet, GridSourceChangeType.CSV_DESCRIPTOR),
				getStatus(session));

		syncedVersion = pull(session, recordSet);

		// call under test — the PULL re-baselined onto the revision carrying the new
		// descriptor.
		assertEquals(status(session, syncedVersion, recordSet), getStatus(session));

		// Re-binding a schema does not create a new revision.
		String schemaTwoId = createSchema(org, "schemaVTwo", List.of("a", "b"));
		entityService.bindSchemaToEntity(admin.getId(),
				new BindSchemaToEntityRequest().setEntityId(recordSetId).setSchema$id(schemaTwoId));

		// call under test — the schema change is reported against the synced revision.
		assertEquals(status(session, syncedVersion, recordSet, GridSourceChangeType.JSON_SCHEMA),
				getStatus(session));

		// Deleting the baseline revision leaves the session pointing at a revision that
		// no longer exists, and makes the previous revision current.
		entityService.deleteEntityVersion(admin.getId(), recordSetId, csvDescriptorVersion);
		recordSet = entityService.getEntity(admin.getId(), recordSetId, RecordSet.class);
		assertEquals(upsertKeyVersion, recordSet.getVersionNumber());

		// call under test — there is no longer a baseline to compare against, while the
		// schema change from the previous step is still outstanding.
		assertEquals(status(session, syncedVersion, recordSet, GridSourceChangeType.JSON_SCHEMA,
				GridSourceChangeType.BASELINE_MISSING), getStatus(session));

		syncedVersion = pull(session, recordSet);

		// call under test — the reported changes are actionable: a PULL succeeds without
		// the deleted baseline and re-baselines the session.
		assertEquals(status(session, syncedVersion, recordSet), getStatus(session));

		GridConnectionInfo validationCon = gridManager
				.getSingletonConnection(session.getSessionId(), EventSource.VALIDATION).orElseThrow();

		// Unbinding the schema leaves the grid validating against a schema the source
		// no longer has.
		entityService.clearBoundSchema(admin.getId(), recordSetId);

		// call under test — losing the bound schema is reported as a schema change.
		assertEquals(status(session, syncedVersion, recordSet, GridSourceChangeType.JSON_SCHEMA),
				getStatus(session));

		syncedVersion = pull(session, recordSet);

		// call under test — the PULL cleared the session's recorded schema $id, so the
		// change does not persist across synchronizations.
		assertEquals(status(session, syncedVersion, recordSet), getStatus(session));

		// Binding a schema again after it was unbound.
		entityService.bindSchemaToEntity(admin.getId(),
				new BindSchemaToEntityRequest().setEntityId(recordSetId).setSchema$id(schemaOneId));

		// call under test — the newly bound schema is reported as a schema change.
		assertEquals(status(session, syncedVersion, recordSet, GridSourceChangeType.JSON_SCHEMA),
				getStatus(session));

		syncedVersion = pull(session, recordSet);

		// call under test — the PULL recorded the schema on the session again.
		assertEquals(status(session, syncedVersion, recordSet), getStatus(session));
		// The session kept the VALIDATION replica it had before the schema was unbound,
		// rather than gaining a second one.
		assertEquals(validationCon.getReplicaId(), gridManager
				.getSingletonConnection(session.getSessionId(), EventSource.VALIDATION).orElseThrow().getReplicaId());
	}

	/**
	 * @return the source version the PULL synchronized the session to
	 */
	private long pull(GridSession session, RecordSet current) throws Exception {
		asynchronousJobWorkerHelper.assertJobResponse(admin,
				new SynchronizeGridRequest().setGridSessionId(session.getSessionId()).setSyncType(SyncType.PULL),
				(r) -> {
				}, MAX_WAIT_MS);
		return current.getVersionNumber();
	}

	private GridSourceStatus getStatus(GridSession session) {
		return gridSourceStatusManager.getGridSourceStatus(admin, session.getSessionId());
	}

	private GridSourceStatus status(GridSession session, long syncedVersion, RecordSet current,
			GridSourceChangeType... changes) {
		return new GridSourceStatus().setGridSessionId(session.getSessionId()).setSourceEntityId(current.getId())
				.setSessionSourceVersionNumber(syncedVersion).setCurrentSourceVersionNumber(current.getVersionNumber())
				.setHasChanges(changes.length > 0).setChanges(List.of(changes));
	}

	private RecordSet createNewVersion(RecordSet recordSet, String dataFileHandleId) {
		recordSet.setDataFileHandleId(dataFileHandleId);
		// a null label defaults to the new revision number, avoiding a label collision.
		recordSet.setVersionLabel(null);
		return entityService.updateEntity(admin.getId(), recordSet, true, null);
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
