package org.sagebionetworks.statistics.workers;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.statistics.monthly.StatisticsMonthlyManager;
import org.sagebionetworks.repo.model.dao.statistics.StatisticsMonthlyStatusDAO;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.sagebionetworks.repo.model.statistics.StatisticsStatus;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class StatisticsMonthlyWorkerIntegrationTest {

	private static final long WAIT_INTERVAL = 1000;
	private static final long MAX_WAIT_MS = 60 * 1000;
	private static final StatisticsObjectType OBJECT_TYPE = StatisticsObjectType.PROJECT;
	
	@Autowired
	private StatisticsMonthlyStatusDAO statusDao;
	
	@Autowired
	private StatisticsMonthlyManager manager;
	
	@Autowired
	private StatisticsMonthlyStatusWatcherWorker statusWatcher;
	
	@BeforeEach
	public void before() {
		statusDao.clear();
	}
	
	@AfterEach
	public void after() {
		statusDao.clear();
	}
	
	@Test
	public void testProcessing() throws Exception {
		
		// Manually run start the processing
		statusWatcher.run(null);
	
		long startTime = System.currentTimeMillis();
		
		while(!isProcessingDone(startTime)) {
			Thread.sleep(WAIT_INTERVAL);
		}
		
	}
	
	boolean isProcessingDone(long startTime) {
		if (System.currentTimeMillis() - startTime >= MAX_WAIT_MS) {
			throw new IllegalStateException("Timed out while waiting for processing to finish");
		}
		
		List<YearMonth> unprocessed = manager.getUnprocessedMonths(OBJECT_TYPE);
		
		if (unprocessed.isEmpty()) {
			return true;
		}
		
		for (YearMonth month : unprocessed) {
			Optional<StatisticsMonthlyStatus> status = statusDao.getStatus(OBJECT_TYPE, month);
			
			// Still processing this month
			if (!status.isPresent()) {
				return false;
			}
			
			StatisticsMonthlyStatus monthStatus = status.get();
			
			if (StatisticsStatus.PROCESSING.equals(monthStatus.getStatus())) {
				return false;
			}
		}
		
		return true;
		
	}
	
	
}
