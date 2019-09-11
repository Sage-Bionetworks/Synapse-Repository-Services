package org.sagebionetworks.repo.manager.statistics.monthly.project;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.model.athena.AthenaQueryResult;
import org.sagebionetworks.repo.model.dao.project.AthenaProjectFilesDAO;
import org.sagebionetworks.repo.model.dao.statistics.StatisticsMonthlyProjectDAO;
import org.sagebionetworks.repo.model.statistics.FileEvent;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyProjectFiles;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.stereotype.Service;

@Service
public class StatisticsmonthlyProjectManagerImpl implements StatisticsMonthlyProjectManager {

	private static final Logger LOG = LogManager.getLogger(StatisticsmonthlyProjectManagerImpl.class);

	static final int BATCH_SIZE = 5000;

	private AthenaProjectFilesDAO athenaDao;
	private StatisticsMonthlyProjectDAO statisticsDao;

	public StatisticsmonthlyProjectManagerImpl(AthenaProjectFilesDAO athenaDao, StatisticsMonthlyProjectDAO statisticsDao) {
		this.athenaDao = athenaDao;
		this.statisticsDao = statisticsDao;
	}

	@Override
	public void computeMonthlyProjectFilesStatistics(FileEvent eventType, YearMonth month) {
		ValidateArgument.required(eventType, "eventType");
		ValidateArgument.required(month, "month");

		AthenaQueryResult<StatisticsMonthlyProjectFiles> queryResult = athenaDao.aggregateForMonth(eventType, month);

		Iterator<StatisticsMonthlyProjectFiles> resultsIterator = queryResult.getQueryResultsIterator();

		List<StatisticsMonthlyProjectFiles> batch = new ArrayList<>();

		while (resultsIterator.hasNext()) {
			batch.add(resultsIterator.next());
			saveBatch(batch, BATCH_SIZE);
		}
		// Makes sure to save the remaining records
		saveBatch(batch, 0);

	}

	void saveBatch(List<StatisticsMonthlyProjectFiles> batch, int threshold) {
		if (batch.isEmpty()) {
			return;
		}

		if (batch.size() < threshold) {
			return;
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("Saving batch of {} records...", batch.size());
		}

		statisticsDao.save(batch);

		batch.clear();

		if (LOG.isDebugEnabled()) {
			LOG.debug("Saving batch of {} records...DONE", batch.size());
		}
	}

}
