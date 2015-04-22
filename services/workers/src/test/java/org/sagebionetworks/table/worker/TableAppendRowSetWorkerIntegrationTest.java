package org.sagebionetworks.table.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.table.AppendableRowSetRequest;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.PartialRow;
import org.sagebionetworks.repo.model.table.PartialRowSet;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowReference;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowReferenceSetResults;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableAppendRowSetWorkerIntegrationTest {

	public static final int MAX_WAIT_MS = 1000 * 80;
	@Autowired
	AsynchJobStatusManager asynchJobStatusManager;
	@Autowired
	StackConfiguration config;
	@Autowired
	EntityManager entityManager;
	@Autowired
	TableRowManager tableRowManager;
	@Autowired
	ColumnModelManager columnManager;
	@Autowired
	UserManager userManager;
	@Autowired
	SemaphoreManager semphoreManager;
	private UserInfo adminUserInfo;
	RowReferenceSet referenceSet;
	List<ColumnModel> schema;
	List<SelectColumn> select;
	List<Long> headers;
	private String tableId;
	private List<String> toDelete = Lists.newArrayList();

	@Before
	public void before() throws NotFoundException {
		// Only run this test if the table feature is enabled.
		Assume.assumeTrue(config.getTableEnabled());
		semphoreManager.releaseAllLocksAsAdmin(new UserInfo(true));
		// Start with an empty queue.
		asynchJobStatusManager.emptyAllQueues();
		// Get the admin user
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		this.schema = new LinkedList<ColumnModel>();
		// Create a project
		Project project = new Project();
		project.setName(UUID.randomUUID().toString());
		String id = entityManager.createEntity(adminUserInfo, project, null);
		project = entityManager.getEntity(adminUserInfo, id, Project.class);
		toDelete.add(project.getId());
		
		select = new LinkedList<SelectColumn>();
		// Create a few columns
		// String
		ColumnModel cm = new ColumnModel();
		cm.setColumnType(ColumnType.STRING);
		cm.setName("somestrings");
		cm = columnManager.createColumnModel(adminUserInfo, cm);
		this.schema.add(cm);
		select.add(createSelectColumn(cm));
		
		// integer
		cm = new ColumnModel();
		cm.setColumnType(ColumnType.INTEGER);
		cm.setName("someinteger");
		cm = columnManager.createColumnModel(adminUserInfo, cm);
		schema.add(cm);
		select.add(createSelectColumn(cm));
		headers = TableModelUtils.getIds(schema);

		// Create the table
		TableEntity table = new TableEntity();
		table.setParentId(project.getId());
		table.setColumnIds(Lists.transform(headers, TableModelUtils.LONG_TO_STRING));
		table.setName(UUID.randomUUID().toString());
		tableId = entityManager.createEntity(adminUserInfo, table, null);
		// Bind the columns. This is normally done at the service layer but the workers cannot depend on that layer.
		columnManager.bindColumnToObject(adminUserInfo, Lists.transform(headers, TableModelUtils.LONG_TO_STRING), tableId, true);

	}

	public SelectColumn createSelectColumn(ColumnModel cm) {
		SelectColumn sc = new SelectColumn();
		sc.setColumnType(cm.getColumnType());
		sc.setId(cm.getId());
		sc.setName(cm.getName());
		return sc;
	}

	@After
	public void after() {
		if (config.getTableEnabled()) {
			if (adminUserInfo != null) {
				for (String id : toDelete) {
					try {
						entityManager.deleteEntity(adminUserInfo, id);
					} catch (Exception e) {
					}
				}
			}
		}
	}

	@Test
	public void testAppendPartialRowSet() throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException, IOException,
			InterruptedException {
		// We are now ready to start the job
		AppendableRowSetRequest body = new AppendableRowSetRequest();
		PartialRowSet set = new PartialRowSet();
		body.setToAppend(set);
		set.setTableId(tableId);
		PartialRow row = new PartialRow();
		row.setRowId(null);
		Map<String, String> values = new HashMap<String, String>();
		values.put(headers.get(1).toString(), "12345");
		row.setValues(values);
		set.setRows(Arrays.asList(row));
		AsynchronousJobStatus status = asynchJobStatusManager.startJob(adminUserInfo, body);
		// Wait for the job to complete.
		status = waitForStatus(adminUserInfo, status);
		assertNotNull(status);
		assertNotNull(status.getResponseBody());
		assertTrue(status.getResponseBody() instanceof RowReferenceSetResults);
		RowReferenceSetResults results = (RowReferenceSetResults) status.getResponseBody();
		assertNotNull(results.getRowReferenceSet());
		RowReferenceSet refSet = results.getRowReferenceSet();
		assertNotNull(refSet.getRows());
		assertEquals(tableId, refSet.getTableId());
		assertEquals(1, refSet.getRows().size());
		RowReference rowRef = refSet.getRows().get(0);
		assertNotNull(rowRef.getRowId());
		assertNotNull(rowRef.getVersionNumber());
	}

	@Test
	public void testAppendRowSet() throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException, IOException,
			InterruptedException {
		// We are now ready to start the job
		AppendableRowSetRequest body = new AppendableRowSetRequest();
		RowSet set = new RowSet();
		set.setHeaders(select);
		body.setToAppend(set);
		set.setTableId(tableId);
		Row row = new Row();
		row.setValues(Arrays.asList("one", "1234"));
		set.setRows(Arrays.asList(row));
		AsynchronousJobStatus status = asynchJobStatusManager.startJob(adminUserInfo, body);
		// Wait for the job to complete.
		status = waitForStatus(adminUserInfo, status);
		assertNotNull(status);
		assertNotNull(status.getResponseBody());
		assertTrue(status.getResponseBody() instanceof RowReferenceSetResults);
		RowReferenceSetResults results = (RowReferenceSetResults) status.getResponseBody();
		assertNotNull(results.getRowReferenceSet());
		RowReferenceSet refSet = results.getRowReferenceSet();
		assertNotNull(refSet.getRows());
		assertEquals(tableId, refSet.getTableId());
		assertEquals(1, refSet.getRows().size());
		RowReference rowRef = refSet.getRows().get(0);
		assertNotNull(rowRef.getRowId());
		assertNotNull(rowRef.getVersionNumber());
	}
	
	private AsynchronousJobStatus waitForStatus(UserInfo user, AsynchronousJobStatus status) throws InterruptedException, DatastoreException,
			NotFoundException {
		long start = System.currentTimeMillis();
		while (!AsynchJobState.COMPLETE.equals(status.getJobState())) {
			assertFalse("Job Failed: " + status.getErrorDetails(), AsynchJobState.FAILED.equals(status.getJobState()));
			assertTrue("Timed out waiting for table status", (System.currentTimeMillis() - start) < MAX_WAIT_MS);
			Thread.sleep(1000);
			// Get the status again
			status = this.asynchJobStatusManager.getJobStatus(user, status.getJobId());
		}
		return status;
	}
}
