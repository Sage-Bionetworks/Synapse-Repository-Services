package org.sagebionetworks.repo.model.athena.project;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.athena.AthenaQueryResult;
import org.sagebionetworks.repo.model.athena.AthenaQueryStatistics;
import org.sagebionetworks.repo.model.statistics.FileEvent;
import org.sagebionetworks.repo.model.statistics.project.StatisticsMonthlyProjectFiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class AthenaProjectFileStatisticsDAOImplAutowireTest {
	
	@Autowired
	private AthenaProjectFileStatisticsDAO dao;

	@Test
	public void testAggregateForMonth() throws Exception {
		FileEvent eventType = FileEvent.FILE_DOWNLOAD;
		// Generate for the next month so that we do not waste money on tests
		YearMonth month = YearMonth.now(ZoneOffset.UTC).plusMonths(1);
		
		// Call under test
		AthenaQueryResult<StatisticsMonthlyProjectFiles> result = dao.aggregateForMonth(eventType, month);
		
		assertNotNull(result);
		
		AthenaQueryStatistics stats = result.getQueryExecutionStatistics();
		
		assertNotNull(stats);
		assertNotNull(stats.getDataScanned());
		assertNotNull(stats.getExecutionTime());
		
		Iterator<StatisticsMonthlyProjectFiles> iterator = result.getQueryResultsIterator();
		
		assertNotNull(iterator);
		assertFalse(iterator.hasNext());
	}
	
}
