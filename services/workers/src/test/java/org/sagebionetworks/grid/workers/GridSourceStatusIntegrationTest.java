package org.sagebionetworks.grid.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import org.sagebionetworks.repo.manager.grid.synch.GridSourceStatusManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.CreateGridResponse;
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
import org.sagebionetworks.repo.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Autowired integration test for the source status of a RecordSet-sourced grid.
 * Walks a session through the changes a client would be prompted to PULL for: a
 * new revision that carries the same data, a new revision with a new CSV, a
 * synchronization, a re-bound JSON schema, and the deletion of the synchronized
 * baseline revision.
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
		long v1 = recordSet.getVersionNumber();

		GridSession session = asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateGridRequest().setOwnerPrincipalId(admin.getId().toString()).setRecordSetId(recordSetId),
				(CreateGridResponse response) -> {
					assertNotNull(response.getGridSession());
				}, MAX_WAIT_MS).getResponse().getGridSession();
		assertNotNull(session);
		assertEquals(schemaOneId, session.getGridJsonSchema$Id());

		GridConnectionInfo internalCon = asynchronousJobWorkerHelper.getInternalGridConnection(session.getSessionId(),
				MAX_WAIT_MS);
		gridTestUtils.waitForRows(session.getSessionId(), internalCon.getReplicaId(), Map.of(
				"1", Map.of("b", "one"),
				"2", Map.of("b", "two")));

		// call under test — a freshly created session is in sync with its source.
		assertEquals(expectedStatus(session, recordSetId, v1, v1, List.of()), getStatus(session));

		// v2: a new revision that re-uses the v1 file handle, so the data is unchanged.
		recordSet = createNewVersion(recordSet, recordSet.getDataFileHandleId());
		long v2 = recordSet.getVersionNumber();

		// call under test — a newer revision alone is not a change.
		assertEquals(expectedStatus(session, recordSetId, v1, v2, List.of()), getStatus(session));

		// v3: a new revision with a new CSV.
		recordSet = createNewVersion(recordSet, uploadCsv("""
				a,b
				1,one-changed
				2,two
				"""));
		long v3 = recordSet.getVersionNumber();

		// call under test — the new CSV is reported as a data change.
		assertEquals(expectedStatus(session, recordSetId, v1, v3, List.of(GridSourceChangeType.DATA)),
				getStatus(session));

		asynchronousJobWorkerHelper.assertJobResponse(admin,
				new SynchronizeGridRequest().setGridSessionId(session.getSessionId()).setSyncType(SyncType.PULL),
				(r) -> {
				}, MAX_WAIT_MS);

		// call under test — the PULL advanced the baseline to v3, clearing the change.
		assertEquals(expectedStatus(session, recordSetId, v3, v3, List.of()), getStatus(session));

		// Re-binding a schema does not create a new revision.
		String schemaTwoId = createSchema(org, "schemaVTwo", List.of("a", "b"));
		entityService.bindSchemaToEntity(admin.getId(),
				new BindSchemaToEntityRequest().setEntityId(recordSetId).setSchema$id(schemaTwoId));

		// call under test — the schema change is reported against the synced revision.
		assertEquals(expectedStatus(session, recordSetId, v3, v3, List.of(GridSourceChangeType.JSON_SCHEMA)),
				getStatus(session));

		// Deleting the baseline revision promotes v2 back to current.
		entityService.deleteEntityVersion(admin.getId(), recordSetId, v3);

		// call under test — there is no longer a baseline to compare against, while the
		// schema change from the previous step is still outstanding.
		assertEquals(expectedStatus(session, recordSetId, v3, v2,
				List.of(GridSourceChangeType.JSON_SCHEMA, GridSourceChangeType.BASELINE_MISSING)), getStatus(session));
	}

	private GridSourceStatus getStatus(GridSession session) {
		return gridSourceStatusManager.getGridSourceStatus(admin, session.getSessionId());
	}

	private GridSourceStatus expectedStatus(GridSession session, String recordSetId, Long baselineVersionNumber,
			Long currentVersionNumber, List<GridSourceChangeType> changes) {
		return new GridSourceStatus().setGridSessionId(session.getSessionId()).setSourceEntityId(recordSetId)
				.setBaselineVersionNumber(baselineVersionNumber).setCurrentVersionNumber(currentVersionNumber)
				.setHasChanges(!changes.isEmpty()).setChanges(changes);
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
