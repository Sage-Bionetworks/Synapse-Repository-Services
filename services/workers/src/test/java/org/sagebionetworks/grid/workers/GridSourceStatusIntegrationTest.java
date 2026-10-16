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
		String schemaOneId = createSchema(org, "schemaOne");

		RecordSet recordSet = createRecordSet(uploadCsv("""
				a,b
				1,one
				2,two
				"""));
		String recordSetId = recordSet.getId();
		entityService.bindSchemaToEntity(admin.getId(),
				new BindSchemaToEntityRequest().setEntityId(recordSetId).setSchema$id(schemaOneId));

		GridSession session = asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateGridRequest().setOwnerPrincipalId(admin.getId().toString()).setRecordSetId(recordSetId),
				(CreateGridResponse response) -> {
					assertNotNull(response.getGridSession());
				}, MAX_WAIT_MS).getResponse().getGridSession();
		assertEquals(schemaOneId, session.getGridJsonSchema$Id());

		GridConnectionInfo internalCon = asynchronousJobWorkerHelper.getInternalGridConnection(session.getSessionId(),
				MAX_WAIT_MS);
		gridTestUtils.waitForRows(session.getSessionId(), internalCon.getReplicaId(), Map.of(
				"1", Map.of("b", "one"),
				"2", Map.of("b", "two")));

		// call under test
		assertEquals(expectedStatus(session, 1L, 1L, List.of()), getStatus(session));

		recordSet = createNewVersion(recordSet, recordSet.getDataFileHandleId());

		// call under test
		assertEquals(expectedStatus(session, 1L, 2L, List.of()), getStatus(session));

		recordSet = createNewVersion(recordSet, uploadCsv("""
				a,b
				1,one-changed
				2,two
				"""));

		// call under test
		assertEquals(expectedStatus(session, 1L, 3L, List.of(GridSourceChangeType.DATA)), getStatus(session));

		pull(session);

		// call under test
		assertEquals(expectedStatus(session, 3L, 3L, List.of()), getStatus(session));

		String schemaTwoId = createSchema(org, "schemaTwo");
		entityService.bindSchemaToEntity(admin.getId(),
				new BindSchemaToEntityRequest().setEntityId(recordSetId).setSchema$id(schemaTwoId));

		// call under test
		assertEquals(expectedStatus(session, 3L, 3L, List.of(GridSourceChangeType.JSON_SCHEMA)), getStatus(session));

		entityService.deleteEntityVersion(admin.getId(), recordSetId, 3L);

		// call under test - the schema change from the previous step is still outstanding
		assertEquals(expectedStatus(session, 3L, 2L,
				List.of(GridSourceChangeType.JSON_SCHEMA, GridSourceChangeType.BASELINE_MISSING)), getStatus(session));
	}

	private void pull(GridSession session) throws Exception {
		asynchronousJobWorkerHelper.assertJobResponse(admin,
				new SynchronizeGridRequest().setGridSessionId(session.getSessionId()).setSyncType(SyncType.PULL),
				(r) -> {
				}, MAX_WAIT_MS);
	}

	private GridSourceStatus getStatus(GridSession session) {
		return gridSourceStatusManager.getGridSourceStatus(admin, session.getSessionId());
	}

	private GridSourceStatus expectedStatus(GridSession session, Long baselineVersionNumber,
			Long currentVersionNumber, List<GridSourceChangeType> changes) {
		return new GridSourceStatus().setGridSessionId(session.getSessionId())
				.setSourceEntityId(session.getSourceEntityId()).setBaselineVersionNumber(baselineVersionNumber)
				.setCurrentVersionNumber(currentVersionNumber).setHasChanges(!changes.isEmpty()).setChanges(changes);
	}

	private RecordSet createNewVersion(RecordSet recordSet, String dataFileHandleId) {
		recordSet.setDataFileHandleId(dataFileHandleId);
		// a null label defaults to the new version number, avoiding a label collision
		recordSet.setVersionLabel(null);
		return entityService.updateEntity(admin.getId(), recordSet, true, null);
	}

	private String createSchema(Organization org, String name) {
		Map<String, JsonSchema> properties = new HashMap<>();
		properties.put("a", new JsonSchema().setType(Type.string));
		properties.put("b", new JsonSchema().setType(Type.string));
		JsonSchema schema = new JsonSchema().set$id(org.getName() + "-" + name).setProperties(properties);
		return jsonSchemaManager.createJsonSchema(admin, new CreateSchemaRequest().setSchema(schema))
				.getNewVersionInfo().get$id();
	}

	private String uploadCsv(String content) throws Exception {
		return fileHandleTestUtils.uploadCsv(admin, content);
	}

	private RecordSet createRecordSet(String dataFileHandleId) {
		RecordSet recordSet = new RecordSet().setName(UUID.randomUUID().toString()).setUpsertKey(List.of("a"));
		recordSet.setParentId(project.getId());
		recordSet.setDataFileHandleId(dataFileHandleId);
		return entityService.createEntity(admin.getId(), recordSet, null);
	}
}
