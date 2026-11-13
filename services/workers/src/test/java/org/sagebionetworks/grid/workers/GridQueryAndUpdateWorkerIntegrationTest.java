package org.sagebionetworks.grid.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.AsynchronousJobWorkerHelper;
import org.sagebionetworks.grid.db.GridIndexDao;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.manager.grid.GridManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.RowView;
import org.sagebionetworks.repo.manager.grid.internal.replica.view.GridReplicaViewManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.grid.CreateGridRequest;
import org.sagebionetworks.repo.model.grid.CreateGridResponse;
import org.sagebionetworks.repo.model.grid.CreateReplicaRequest;
import org.sagebionetworks.repo.model.grid.GridQueryJobRequest;
import org.sagebionetworks.repo.model.grid.GridQueryJobResponse;
import org.sagebionetworks.repo.model.grid.GridReplica;
import org.sagebionetworks.repo.model.grid.GridSession;
import org.sagebionetworks.repo.model.grid.GridUpdateJobRequest;
import org.sagebionetworks.repo.model.grid.GridUpdateJobResponse;
import org.sagebionetworks.repo.model.grid.query.QueryRequest;
import org.sagebionetworks.repo.model.grid.query.SelectAll;
import org.sagebionetworks.repo.model.grid.update.GridUpdateRequest;
import org.sagebionetworks.repo.model.grid.update.LiteralSetValue;
import org.sagebionetworks.repo.model.grid.update.Update;
import org.sagebionetworks.repo.model.grid.update.UpdateBatch;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.service.EntityService;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.sagebionetworks.util.csv.CSVWriterProviderImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import au.com.bytecode.opencsv.CSVWriter;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class GridQueryAndUpdateWorkerIntegrationTest {

	private static final long MAX_WAIT_MS = 120_000;
	private static final long INTERNAL_REPLICA_ID = 66534L;

	private static final String COL_NAME = "name";
	private static final String COL_VALUE = "value";

	@Autowired
	private EntityService entityService;
	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private AsynchronousJobWorkerHelper asynchronousJobWorkerHelper;
	@Autowired
	private GridReplicaViewManager gridViewManager;
	@Autowired
	private GridManager gridManager;
	@Autowired
	private GridIndexDao gridIndexDao;

	private UserInfo admin;

	@BeforeEach
	public void before() {
		admin = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		entityService.truncateAll();
		gridIndexDao.truncateAll();
	}

	@AfterEach
	public void after() {
		entityService.truncateAll();
	}

	/**
	 * Creates a RecordSet-based grid session with three rows of CSV data and waits
	 * for the grid to be fully loaded. Returns the session and a newly created user
	 * replica.
	 */
	private SessionAndReplica createGridWithData() throws Exception {
		File temp = File.createTempFile("GridQueryUpdateIntegrationTest", ".csv");
		CsvTableDescriptor csvDescriptor = new CsvTableDescriptor().setIsFirstLineHeader(true);
		try (CSVWriter writer = new CSVWriterProviderImpl().createWriter(new FileWriter(temp), csvDescriptor)) {
			writer.writeNext(new String[] { COL_NAME, COL_VALUE });
			writer.writeNext(new String[] { "alpha", "one" });
			writer.writeNext(new String[] { "beta", "two" });
			writer.writeNext(new String[] { "gamma", "three" });
		}

		S3FileHandle fh = fileHandleManager.uploadLocalFile(new LocalFileUploadRequest()
				.withFileToUpload(temp).withContentType("text/csv").withFileName(temp.getName())
				.withUserId(admin.getId().toString()));
		temp.delete();

		Project project = entityService.createEntity(admin.getId(),
				new Project().setName("GridQueryUpdateTest"), null);

		RecordSet recordSet = entityService.createEntity(admin.getId(),
				new RecordSet().setName("test-recordset").setParentId(project.getId())
						.setDataFileHandleId(fh.getId()).setUpsertKey(List.of(COL_NAME)),
				null);

		GridSession session = asynchronousJobWorkerHelper
				.assertJobResponse(admin,
						new CreateGridRequest().setRecordSetId(recordSet.getId()),
						(CreateGridResponse response) -> {
							assertNotNull(response.getGridSession());
						}, MAX_WAIT_MS)
				.getResponse().getGridSession();
		assertNotNull(session);

		// Wait for the three rows to be loaded into the grid
		TimeUtils.waitFor(MAX_WAIT_MS, 1_000L, () -> {
			Optional<GridHeader> header = gridViewManager.readHeader(session.getSessionId(), INTERNAL_REPLICA_ID);
			if (header.isEmpty()) {
				return Pair.create(false, null);
			}
			List<RowView> rows = gridViewManager.querySinglePage(header.get(), 10L, 0L);
			return Pair.create(rows.size() == 3, null);
		});

		GridReplica replica = gridManager
				.createReplica(admin, new CreateReplicaRequest().setGridSessionId(session.getSessionId()))
				.getReplica();

		return new SessionAndReplica(session, replica);
	}

	private static class SessionAndReplica {
		final GridSession session;
		final GridReplica replica;
		SessionAndReplica(GridSession session, GridReplica replica) {
			this.session = session;
			this.replica = replica;
		}
	}

	@Test
	public void testGridQueryWorkerWithData() throws Exception {
		SessionAndReplica setup = createGridWithData();
		GridSession session = setup.session;
		GridReplica replica = setup.replica;

		GridQueryJobRequest request = new GridQueryJobRequest()
				.setSessionId(session.getSessionId())
				.setReplicaId(replica.getReplicaId())
				.setQueryRequest(new QueryRequest()
						.setQuery(new org.sagebionetworks.repo.model.grid.query.Query()
								.setColumnSelection(List.of(new SelectAll()))
								.setLimit(10L)));

		// call under test
		GridQueryJobResponse response = asynchronousJobWorkerHelper
				.assertJobResponse(admin, request, (GridQueryJobResponse r) -> {
					assertNotNull(r.getQueryResult());
					assertEquals(2, r.getQueryResult().getSelectColumns().size());
					assertEquals(3, r.getQueryResult().getRows().size());
				}, MAX_WAIT_MS)
				.getResponse();

		List<String> columnNames = response.getQueryResult().getSelectColumns().stream()
				.map(c -> c.getColumnName())
				.collect(Collectors.toList());
		assertTrue(columnNames.contains(COL_NAME));
		assertTrue(columnNames.contains(COL_VALUE));
		assertEquals(3, response.getQueryResult().getRows().size());
	}

	@Test
	public void testGridUpdateWorkerWithData() throws Exception {
		SessionAndReplica setup = createGridWithData();
		GridSession session = setup.session;
		GridReplica replica = setup.replica;

		// Update all rows: set COL_VALUE to "updated"
		GridUpdateJobRequest request = new GridUpdateJobRequest()
				.setSessionId(session.getSessionId())
				.setReplicaId(replica.getReplicaId())
				.setUpdateRequest(new GridUpdateRequest()
						.setUpdate(new UpdateBatch()
								.setBatch(List.of(new Update()
										.setSet(List.of(new LiteralSetValue()
												.setColumnName(COL_VALUE)
												.setValue("updated")))))));

		// call under test
		GridUpdateJobResponse updateResponse = asynchronousJobWorkerHelper
				.assertJobResponse(admin, request, (GridUpdateJobResponse r) -> {
					assertNotNull(r.getUpdateResponse());
					assertEquals(1, r.getUpdateResponse().getUpdateResults().size());
					assertEquals(3L, (long) r.getUpdateResponse().getTotalRowsUpdated());
				}, MAX_WAIT_MS)
				.getResponse();

		assertEquals(3L, (long) updateResponse.getUpdateResponse().getTotalRowsUpdated());

		// Verify the update was applied by reading directly via the view manager
		TimeUtils.waitFor(MAX_WAIT_MS, 1_000L, () -> {
			Optional<GridHeader> header = gridViewManager.readHeader(session.getSessionId(), INTERNAL_REPLICA_ID);
			if (header.isEmpty()) {
				return Pair.create(false, null);
			}
			List<RowView> rows = gridViewManager.querySinglePage(header.get(), 10L, 0L);
			if (rows.size() != 3) {
				return Pair.create(false, null);
			}
			boolean allUpdated = rows.stream().allMatch(row -> {
				Object value = row.getRowObject().getData().getRowJsonDocument().opt(COL_VALUE);
				return "updated".equals(value);
			});
			return Pair.create(allUpdated, null);
		});
	}

}
