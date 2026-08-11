package org.sagebionetworks.recordset.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.MaterializedView;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Autowired integration test for RecordSet index building. Unlike
 * {@code ITRecordSetTest} (which drives the deployed WAR over HTTP and keeps a
 * single simple end-to-end case), this test exercises the more complex
 * scenarios directly against the Spring context: a schema-changing update,
 * explicit-version queries against per-version snapshots, and a materialized
 * view defined over a RecordSet.
 * <p>
 * A RecordSet is only indexed when it has a bound JSON Schema; the column
 * schema bound to the index is derived from that JSON Schema (not inferred from
 * the CSV). Because the RecordSetMetadataProvider fires on create/update before
 * a schema can be bound to the freshly-created entity, the flow is:
 * create -> bind schema -> update. The update re-fires the provider with the
 * schema now bound, which binds the column schema and emits the change message
 * that drives the RecordSetIndexWorker to build the index.
 * <p>
 * RecordSets MUST be created/updated through {@link EntityService} (not
 * {@link org.sagebionetworks.repo.manager.EntityManager}) so the
 * RecordSetMetadataProvider fires.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class RecordSetIndexWorkerIntegrationTest {

	public static final long MAX_WAIT_MS = 1000L * 60 * 2;

	private static final String ORGANIZATION_NAME = "RecordSetIndexWorkerITorg";

	@Autowired
	private EntityService entityService;

	@Autowired
	private UserManager userManager;

	@Autowired
	private FileHandleManager fileHandleManager;

	@Autowired
	private JsonSchemaManager jsonSchemaManager;

	@Autowired
	private AsynchronousJobWorkerHelper asyncHelper;

	private UserInfo adminUserInfo;
	private Project project;

	@BeforeEach
	public void before() {
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		project = new Project().setName(UUID.randomUUID().toString());
		String projectId = entityService.createEntity(adminUserInfo.getId(), project, null).getId();
		project.setId(projectId);
	}

	@AfterEach
	public void after() {
		if (project != null) {
			entityService.deleteEntity(adminUserInfo.getId(), project.getId());
		}
	}

	@Test
	public void testQueryRecordSet() throws Exception {
		// Three integer columns, all declared by the bound JSON Schema.
		RecordSet recordSet = createRecordSet(uploadCsv("a,b,c\n1,2,3\n4,5,6\n"), integerProperties("a", "b", "c"));

		queryAndAssertRows("select * from " + recordSet.getId() + " order by ROW_ID",
				List.of(List.of("1", "2", "3"), List.of("4", "5", "6")));
	}

	@Test
	public void testQueryRecordSetWithSchemaChangingUpdate() throws Exception {
		// v1 — the bound schema declares all three columns as INTEGER.
		RecordSet recordSet = createRecordSet(uploadCsv("a,b,c\n1,2,3\n4,5,6\n"), integerProperties("a", "b", "c"));
		long v1 = recordSet.getVersionNumber();

		// The v1 index is INTEGER for all three columns.
		queryAndAssertColumnTypes(recordSet.getId(),
				List.of(ColumnType.INTEGER, ColumnType.INTEGER, ColumnType.INTEGER));

		// v2 — rebind a schema where "c" is now a (length-constrained) STRING, then update
		// with non-numeric data for "c" as a new version. Routed through EntityService so the
		// provider rebinds the column schema from the newly-bound JSON Schema.
		Map<String, JsonSchema> v2Properties = new LinkedHashMap<>();
		v2Properties.put("a", new JsonSchema().setType(Type.integer));
		v2Properties.put("b", new JsonSchema().setType(Type.integer));
		v2Properties.put("c", new JsonSchema().setType(Type.string).setMaxLength(50L));
		bindSchema(recordSet.getId(), v2Properties);

		recordSet.setDataFileHandleId(uploadCsv("a,b,c\n7,8,nine\n10,11,twelve\n"));
		recordSet.setVersionLabel("v2");
		recordSet = entityService.updateEntity(adminUserInfo.getId(), recordSet, true, null);
		long v2 = recordSet.getVersionNumber();

		// The unversioned alias now resolves to v2: "c" is STRING and the new data is queryable.
		queryAndAssertColumnTypes(recordSet.getId(),
				List.of(ColumnType.INTEGER, ColumnType.INTEGER, ColumnType.STRING));
		queryAndAssertRows("select * from " + recordSet.getId() + " order by ROW_ID",
				List.of(List.of("7", "8", "nine"), List.of("10", "11", "twelve")));

		// The v1 snapshot still returns its own data with its own (all-INTEGER) schema.
		queryAndAssertRows("select * from " + recordSet.getId() + "." + v1 + " order by ROW_ID",
				List.of(List.of("1", "2", "3"), List.of("4", "5", "6")));
		queryAndAssertColumnTypes(recordSet.getId() + "." + v1,
				List.of(ColumnType.INTEGER, ColumnType.INTEGER, ColumnType.INTEGER));

		// The v2 snapshot returns the v2 data.
		queryAndAssertRows("select * from " + recordSet.getId() + "." + v2 + " order by ROW_ID",
				List.of(List.of("7", "8", "nine"), List.of("10", "11", "twelve")));
	}

	@Test
	public void testQueryRecordSetByExplicitVersion() throws Exception {
		// v1
		RecordSet recordSet = createRecordSet(uploadCsv("a,b,c\n1,2,3\n4,5,6\n"), integerProperties("a", "b", "c"));
		long v1 = recordSet.getVersionNumber();

		// v2 — different data, same schema shape. The schema bound at v1 is reused.
		recordSet.setDataFileHandleId(uploadCsv("a,b,c\n7,8,9\n10,11,12\n"));
		recordSet.setVersionLabel("v2");
		recordSet = entityService.updateEntity(adminUserInfo.getId(), recordSet, true, null);
		long v2 = recordSet.getVersionNumber();

		// Each version's immutable snapshot T{id}_{v} returns its own data, and the
		// unversioned alias T{id} resolves to the latest.
		queryAndAssertRows("select * from " + recordSet.getId() + "." + v1 + " order by ROW_ID",
				List.of(List.of("1", "2", "3"), List.of("4", "5", "6")));
		queryAndAssertRows("select * from " + recordSet.getId() + "." + v2 + " order by ROW_ID",
				List.of(List.of("7", "8", "9"), List.of("10", "11", "12")));
		queryAndAssertRows("select * from " + recordSet.getId() + " order by ROW_ID",
				List.of(List.of("7", "8", "9"), List.of("10", "11", "12")));
	}

	@Test
	public void testQueryRecordSetThroughMaterializedView() throws Exception {
		RecordSet recordSet = createRecordSet(uploadCsv("a,b,c\n1,2,3\n4,5,6\n"), integerProperties("a", "b", "c"));

		// Wait for the RecordSet index so the MV build sees rows to copy.
		queryAndAssertRows("select * from " + recordSet.getId() + " order by ROW_ID",
				List.of(List.of("1", "2", "3"), List.of("4", "5", "6")));

		MaterializedView mv = new MaterializedView()
				.setName(UUID.randomUUID().toString())
				.setParentId(project.getId())
				.setDefiningSQL("select * from " + recordSet.getId());
		String mvId = entityService.createEntity(adminUserInfo.getId(), mv, null).getId();

		queryAndAssertRows("select * from " + mvId + " order by ROW_ID",
				List.of(List.of("1", "2", "3"), List.of("4", "5", "6")));
	}

	private String uploadCsv(String content) throws Exception {
		S3FileHandle fileHandle = fileHandleManager.createFileFromByteArray(adminUserInfo.getId().toString(),
				new Date(), content.getBytes(StandardCharsets.UTF_8), "recordset.csv",
				ContentType.create("text/csv"), null);
		return fileHandle.getId();
	}

	/**
	 * Builds an ordered properties map (column order is preserved by the index) where every
	 * named property is declared as an INTEGER.
	 */
	private static Map<String, JsonSchema> integerProperties(String... names) {
		Map<String, JsonSchema> properties = new LinkedHashMap<>();
		for (String name : names) {
			properties.put(name, new JsonSchema().setType(Type.integer));
		}
		return properties;
	}

	/**
	 * Registers a new JSON Schema with the given properties and binds it to the entity. A unique
	 * schema name is used per call so repeated binds (e.g. a schema-changing update) don't collide.
	 */
	private void bindSchema(String entityId, Map<String, JsonSchema> properties) {
		Organization organization = asyncHelper.getOrCreateOrganization(adminUserInfo.getId(), ORGANIZATION_NAME);
		String schemaName = "s" + UUID.randomUUID().toString().replace("-", "");
		JsonSchema schema = new JsonSchema()
				.set$id(organization.getName() + "-" + schemaName)
				.setProperties(properties);
		String schema$id = jsonSchemaManager.createJsonSchema(adminUserInfo, new CreateSchemaRequest().setSchema(schema))
				.getNewVersionInfo().get$id();
		entityService.bindSchemaToEntity(adminUserInfo.getId(),
				new BindSchemaToEntityRequest().setEntityId(entityId).setSchema$id(schema$id));
	}

	/**
	 * Creates a RecordSet and binds a JSON Schema to it so the index is built. The metadata
	 * provider fires on create before a schema can be bound (no index yet), so we bind the schema
	 * and then update to re-fire the provider with the schema present, which builds the v1 index.
	 */
	private RecordSet createRecordSet(String dataFileHandleId, Map<String, JsonSchema> properties) {
		RecordSet rs = new RecordSet()
				.setName(UUID.randomUUID().toString())
				.setUpsertKey(List.of("a"));
		rs.setParentId(project.getId());
		rs.setDataFileHandleId(dataFileHandleId);
		rs = entityService.createEntity(adminUserInfo.getId(), rs, null);

		bindSchema(rs.getId(), properties);

		// Re-fire the provider now that a schema is bound so the (v1) index is built.
		rs.setVersionLabel("v1");
		return entityService.updateEntity(adminUserInfo.getId(), rs, false, null);
	}

	/**
	 * Queries with retries until the worker has built the index, asserting the
	 * returned rows match exactly (in order).
	 */
	private void queryAndAssertRows(String sql, List<List<String>> expectedRowValues) throws Exception {
		asyncHelper.assertQueryResult(adminUserInfo, sql, bundle -> {
			List<List<String>> actual = bundle.getQueryResult().getQueryResults().getRows().stream()
					.map(Row::getValues).collect(Collectors.toList());
			assertEquals(expectedRowValues, actual);
		}, MAX_WAIT_MS);
	}

	/**
	 * Queries with retries until the worker has built the index, asserting the
	 * bound column types of the (data) columns match.
	 */
	private void queryAndAssertColumnTypes(String tableId, List<ColumnType> expectedTypes) throws Exception {
		asyncHelper.assertQueryResult(adminUserInfo, "select * from " + tableId, bundle -> {
			List<ColumnType> actual = bundle.getColumnModels().stream()
					.map(ColumnModel::getColumnType).collect(Collectors.toList());
			assertEquals(expectedTypes, actual);
		}, MAX_WAIT_MS);
	}
}
