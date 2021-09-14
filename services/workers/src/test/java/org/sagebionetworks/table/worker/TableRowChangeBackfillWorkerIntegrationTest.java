package org.sagebionetworks.table.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.dbo.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.dbo.dao.table.TableTransactionDao;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.IdRange;
import org.sagebionetworks.repo.model.table.RawRowSet;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.model.SparseChangeSet;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class TableRowChangeBackfillWorkerIntegrationTest {
	
	private static final int TIMEOUT = 60_000;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private TableRowTruthDAO tableChangeDao;
	
	@Autowired
	private TableTransactionDao tableTransactionDao;
	
	@Autowired
	private SemaphoreManager semaphoreManager;
	
	@Autowired
	private Scheduler scheduler;
	
	private UserInfo user;
	
	private Trigger workerTrigger;
	
	private String tableId;
	
	@BeforeEach
	public void before() throws SchedulerException {
		tableId = "syn123";
		user = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		tableChangeDao.truncateAllRowData();
		
		workerTrigger = scheduler.getTrigger(new TriggerKey("tableRowChangeBackfillWorkerTrigger"));
		
		semaphoreManager.releaseAllLocksAsAdmin(user);
		
	}

	@AfterEach
	public void after() {
		tableChangeDao.truncateAllRowData();
	}
	
	@Test
	public void testRun() throws Exception {
		
		List<ColumnModel> columns = TableModelTestUtils.createColumsWithNames("a", "b", "c");
		
		int rowsCount = 5;
		
		// create some test rows.
		List<Row> rows = TableModelTestUtils.createRows(columns, rowsCount );
		
		SparseChangeSet delta = TableModelUtils.createSparseChangeSet(new RawRowSet(TableModelUtils.getIds(columns), null, tableId, rows), columns);
		
		IdRange range = tableChangeDao.reserveIdsInRange(delta.getTableId(), rowsCount);
		
		TableModelUtils.assignRowIdsAndVersionNumbers(delta, range);
		
		Long transactionId = tableTransactionDao.startTransaction(tableId, user.getId());
		
		Boolean hasFileRefs = null;
		
		tableChangeDao.appendRowSetToTable(user.getId().toString(), delta.getTableId(), range.getEtag(), range.getVersionNumber(), columns, delta.writeToDto(), transactionId, hasFileRefs);
		
		assertEquals(1, tableChangeDao.getTableRowChangeWithNullFileRefsPage(1000, 0).size());
		
		// Manually trigger the job since the start time is very long
		scheduler.triggerJob(workerTrigger.getJobKey(), workerTrigger.getJobDataMap());
				
		// Now wait for all the dispatched requests to finish
		TimeUtils.waitFor(TIMEOUT, 1000L, () -> {
			return new Pair<>(tableChangeDao.getTableRowChangeWithNullFileRefsPage(1000, 0).isEmpty(), null);
		});
	}

}
