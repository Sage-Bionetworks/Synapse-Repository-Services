package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.AsynchJobType;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.Row;

@ExtendWith(ITTestExtension.class)
public class ITRecordSetTest {

	private static final long INDEX_TIMEOUT_MS = 1000L * 60 * 2;

	private static final long SCHEMA_TIMEOUT_MS = 1000L * 30;

	private SynapseAdminClient adminSynapse;
	private SynapseClient synapse;
	private Project project;
	private File csvFile;
	private FileHandle csvFileHandle;
	private RecordSet recordSet;
	private String organizationName;

	public ITRecordSetTest(SynapseAdminClient adminSynapse, SynapseClient synapse) {
		this.adminSynapse = adminSynapse;
		this.synapse = synapse;
	}
	
	@BeforeEach
	public void before() throws FileNotFoundException, SynapseException, IOException {
		adminSynapse.clearAllLocks();
		// Create a project, this will own the file entity
		project = new Project();
		project = synapse.createEntity(project);
		
		csvFile = new File(ITRecordSetTest.class.getClassLoader().getResource("docs/test.csv").getFile().replaceAll("%20", " "));

		csvFileHandle = synapse.multipartUpload(csvFile, null, false, true);

		organizationName = "it.recordset.s" + UUID.randomUUID().toString().replace("-", "");
	}
	
	@AfterEach
	public void after() throws Exception {

		if (recordSet != null) {
			synapse.deleteEntity(recordSet, true);
		}

		if (project != null){
			synapse.deleteEntity(project, true);
		}
	}
	
	@Test
	public void testRecordSet() throws SynapseException, IOException {
		recordSet = new RecordSet();
		
		recordSet.setParentId(project.getId());
		recordSet.setName("Record Set");
		recordSet.setUpsertKey(List.of("a", "b"));
		recordSet.setDataFileHandleId(csvFileHandle.getId());
	
		// Call under test
		recordSet = synapse.createEntity(recordSet);

		// Makes sure we can get a presigned URL for the file
		URL url = synapse.getFileURL(new FileHandleAssociation()
			.setAssociateObjectType(FileHandleAssociateType.FileEntity)
			.setAssociateObjectId(recordSet.getId())
			.setFileHandleId(recordSet.getDataFileHandleId())
		);
		
		assertEquals(FileUtils.readFileToString(csvFile, StandardCharsets.UTF_8), IOUtils.toString(url, StandardCharsets.UTF_8));

	}

	@Test
	public void testQueryRecordSet() throws Exception {
		recordSet = createRecordSet(csvFileHandle.getId());

		// call under test — assertions inside the consumer so AsyncJobHelper retries
		// while the worker is still building the index.
		queryAndAssertExpectedRows(recordSet.getId(), List.of("1", "2", "3"), List.of("4", "5", "6"));
	}

	private RecordSet createRecordSet(String dataFileHandleId) throws SynapseException {
		RecordSet rs = new RecordSet();
		rs.setParentId(project.getId());
		rs.setName(UUID.randomUUID().toString());
		rs.setUpsertKey(List.of("a"));
		rs.setDataFileHandleId(dataFileHandleId);
		rs = synapse.createEntity(rs);

		// A RecordSet is only indexed when it has a bound JSON Schema, and the column schema bound
		// to the index is derived from that JSON Schema. The metadata provider fires on create
		// before a schema can be bound to the new entity, so bind the schema and then update the
		// entity to re-fire the provider with the schema present, which builds the index.
		bindSchema(rs.getId(), integerProperties("a", "b", "c"));
		return synapse.putEntity(rs);
	}

	/**
	 * Registers a new JSON Schema with the given properties and binds it to the entity. A unique
	 * organization (created by the test user, who therefore has CREATE permission) and a unique
	 * schema name are used so repeated test runs don't collide.
	 */
	private void bindSchema(String entityId, Map<String, JsonSchema> properties) throws SynapseException {
		Organization organization;
		try {
			organization = synapse.getOrganizationByName(organizationName);
		} catch (SynapseNotFoundException e) {
			organization = synapse.createOrganization(new CreateOrganizationRequest().setOrganizationName(organizationName));
		}
		String schemaName = "it.recordset.s" + UUID.randomUUID().toString().replace("-", "");
		JsonSchema schema = new JsonSchema()
				.set$id(organization.getName() + "-" + schemaName)
				.setProperties(properties);
		CreateSchemaResponse response = AsyncJobHelper.assertAysncJobResult(synapse, AsynchJobType.CreateJsonSchema,
				new CreateSchemaRequest().setSchema(schema),
				(CreateSchemaResponse r) -> assertNotNull(r.getNewVersionInfo()), SCHEMA_TIMEOUT_MS).getResponse();
		String schema$id = response.getNewVersionInfo().get$id();
		synapse.bindJsonSchemaToEntity(new BindSchemaToEntityRequest().setEntityId(entityId).setSchema$id(schema$id));
	}

	/**
	 * Builds an ordered properties map (column order is preserved by the index) where every named
	 * property is declared as an INTEGER.
	 */
	private static Map<String, JsonSchema> integerProperties(String... names) {
		Map<String, JsonSchema> properties = new LinkedHashMap<>();
		for (String name : names) {
			properties.put(name, new JsonSchema().setType(Type.integer));
		}
		return properties;
	}

	/**
	 * Queries the given table/MV with retries, asserting it returns exactly two rows
	 * matching the expected values. Assertions live inside the consumer so AsyncJobHelper
	 * restarts the async query until the worker has built the index (or the timeout expires).
	 */
	private void queryAndAssertExpectedRows(String tableId, List<String> expectedRow1, List<String> expectedRow2)
			throws Exception {
		// Order by ROW_ID so the assertions below don't depend on undefined SQL row order.
		Query query = new Query().setSql("select * from " + tableId + " order by ROW_ID");
		QueryOptions options = new QueryOptions().withMask((long) SynapseClient.QUERY_PARTMASK);
		AsyncJobHelper.assertQueryBundleResults(
				synapse,
				tableId,
				query,
				options,
				bundle -> {
					List<Row> rows = bundle.getQueryResult().getQueryResults().getRows();
					assertEquals(2, rows.size());
					assertEquals(expectedRow1, rows.get(0).getValues());
					assertEquals(expectedRow2, rows.get(1).getValues());
				},
				INDEX_TIMEOUT_MS,
				AsyncJobHelper.INFINITE_RETRIES);
	}

}
