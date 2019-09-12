package org.sagebionetworks.repo.manager.statistics.monthly.project;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.YearMonth;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.dao.statistics.StatisticsMonthlyProjectDAO;
import org.sagebionetworks.repo.model.statistics.FileEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class StatisticsMonthlyProjectManagerAutowireTest {

	@Autowired
	private StatisticsMonthlyProjectDAO statisticsDao;

	@Autowired
	private StatisticsMonthlyProjectManager manager;

	@BeforeEach
	public void before() {
		statisticsDao.clear();
	}

	@AfterEach
	public void after() {
		statisticsDao.clear();
	}

	@Test
	public void testComputeProjectFileStatistics() {
		
		// A month in the future so that we are sure there is no data
		YearMonth month = YearMonth.now(ZoneOffset.UTC).plusMonths(1);
		
		// Call under test
		manager.computeFileEventsStatistics(FileEvent.FILE_DOWNLOAD, month);
		
		Long count = statisticsDao.countProjectsInRange(FileEvent.FILE_DOWNLOAD, month, month);
	
		assertEquals(0, count);
	}
	
}
