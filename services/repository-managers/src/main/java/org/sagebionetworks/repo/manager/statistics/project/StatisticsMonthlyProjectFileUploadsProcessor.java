package org.sagebionetworks.repo.manager.statistics.project;

import java.time.YearMonth;

import org.sagebionetworks.repo.manager.statistics.monthly.StatisticsMonthlyProcessor;
import org.sagebionetworks.repo.model.statistics.FileEvent;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

/**
 * 
 * Processor to compute the monthly project file uploads statistics
 * 
 * @author Marco
 */
@Service
@Order(2)
public class StatisticsMonthlyProjectFileUploadsProcessor implements StatisticsMonthlyProcessor {

	private StatisticsMonthlyProjectManager statisticsManager;

	@Autowired
	public StatisticsMonthlyProjectFileUploadsProcessor(StatisticsMonthlyProjectManager statisticsManager) {
		this.statisticsManager = statisticsManager;
	}

	@Override
	public StatisticsObjectType getSupportedType() {
		return StatisticsObjectType.PROJECT;
	}

	@Override
	public void processMonth(YearMonth month) {
		statisticsManager.computeFileEventsStatistics(FileEvent.FILE_UPLOAD, month);
	}

}
