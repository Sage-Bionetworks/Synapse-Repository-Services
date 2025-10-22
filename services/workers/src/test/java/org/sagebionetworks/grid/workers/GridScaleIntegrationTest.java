package org.sagebionetworks.grid.workers;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.CreateGridResponse;
import org.sagebionetworks.repo.model.grid.CreateReplicaRequest;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.schema.Type;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.service.EntityService;
import org.sagebionetworks.repo.service.GridService;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.sagebionetworks.util.csv.CSVWriterProviderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import au.com.bytecode.opencsv.CSVWriter;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class GridScaleIntegrationTest {

	public static final long MAX_WAIT_MS = 120_000;

	private static final long INTERNAL_REPLICA_ID = 66534L;

	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private EntityService entityService;
	@Autowired
	private JsonSchemaManager jsonSchemaManager;
	@Autowired
	private AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;
	@Autowired
	private GridService gridService;
	@Autowired
	private GridReplicaViewManager gridReplicaViewManager;
	@Autowired
	private GridIndexDao gridIndexDao;

	private UserInfo admin;
	private CsvTableDescriptor csvDescriptor;
	private int numberRows;
	private int numberColumns;
	private Project project;
	private RecordSet recordSet;
	private JsonSchema schema;
	private String schema$id;

	@BeforeEach
	public void beforeAll() throws IOException {
		entityService.truncateAll();
		gridIndexDao.truncateAll();
		admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		numberRows = 100;
		numberColumns = 100;
		File temp = File.createTempFile("GridScaleIntegrationTest", ".csv", null);
		// Create column names
		String[] headers = new String[numberColumns];
		for (int i = 0; i < numberColumns; i++) {
			headers[i] = "c" + i;
		}

		csvDescriptor = new CsvTableDescriptor().setIsFirstLineHeader(true);
		try (CSVWriter writer = new CSVWriterProviderImpl().createWriter(new FileWriter(temp), csvDescriptor)) {

			writer.writeNext(headers);
			// rows
			String[] thisRow = new String[numberColumns];
			for (int row = 0; row < numberRows; row++) {
				for (int col = 0; col < numberColumns; col++) {
					thisRow[col] = "data_" + row + "_" + col;
				}
				writer.writeNext(thisRow);
			}
		}

		S3FileHandle fh = fileHandleManager.uploadLocalFile(new LocalFileUploadRequest().withFileToUpload(temp)
				.withContentType("text/csv").withFileName(temp.getName()).withUserId(admin.getId().toString()));
		temp.delete();

		project = entityService.createEntity(admin.getId(), new Project().setName("GridScaleIntegrationTest"), null);

		recordSet = entityService.createEntity(admin.getId(), new RecordSet().setName("aRecordSet")
				.setParentId(project.getId()).setDataFileHandleId(fh.getId()).setUpsertKey(List.of(headers[0])), null);

		Organization org = asynchronousJobWorkerHelper.getOrCreateOrganization(admin.getId(),
				"GridScaleIntegrationTestorg");

		Map<String, JsonSchema> schemaProps = new HashMap<>();
		Type[] testTypes = { Type.string, Type.integer, Type._boolean };
		for (int i = 0; i < headers.length; i++) {
			Type type = testTypes[i % testTypes.length];
			schemaProps.put(headers[i], new JsonSchema().setType(type));
		}
		schema = new JsonSchema().set$id(org.getName() + "-" + "asampleschema").setProperties(schemaProps);

		schema$id = jsonSchemaManager.createJsonSchema(admin, new CreateSchemaRequest().setSchema(schema))
				.getNewVersionInfo().get$id();

		entityService.bindSchemaToEntity(admin.getId(),
				new BindSchemaToEntityRequest().setEntityId(recordSet.getId()).setSchema$id(schema$id));

	}

	@AfterEach
	public void after() {
		entityService.truncateAll();

	}

	@Test
	public void testCreateLargeGrid() throws Exception {
		GridSession session = asynchronousJobWorkerHelper.assertJobResponse(admin,
				new CreateGridRequest().setRecordSetId(recordSet.getId()), (CreateGridResponse response) -> {
					assertNotNull(response);
					assertNotNull(response.getGridSession());
				}, MAX_WAIT_MS).getResponse().getGridSession();

		// Create a replica
		GridReplica replica = gridService
				.createReplica(admin.getId(), new CreateReplicaRequest().setGridSessionId(session.getSessionId()))
				.getReplica();

		GridHeader gridHeader = TimeUtils.waitFor(MAX_WAIT_MS, 2000L, () -> {
			System.out.println("Waiting for row validation results to change...");
			Optional<GridHeader> header = gridReplicaViewManager.readHeader(session.getSessionId(),
					INTERNAL_REPLICA_ID);
			if (header.isEmpty()) {
				return Pair.create(false, null);
			}
			List<RowView> rows = gridReplicaViewManager.querySinglePage(header.get(), Long.valueOf(numberRows+1), 0L);
			System.out.println("row count: "+rows.size());
			int invalidRows = (int) rows.stream()
					.filter(r -> r.getRowValidationResults() != null && !r.getRowValidationResults().getIsValid())
					.count();
			System.out.println("invalid count: "+invalidRows);
			if (invalidRows != numberRows) {
				return Pair.create(false, null);
			}
			return Pair.create(true, header.get());
		});

	}

}
