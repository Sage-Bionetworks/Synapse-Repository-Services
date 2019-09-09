package org.sagebionetworks.repo.manager.statistics.monthly.project;

import java.time.YearMonth;

import org.sagebionetworks.repo.manager.statistics.monthly.StatisticsMonthlyProcessor;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.springframework.stereotype.Service;

/**
 * Processor for monthly project file downloads
 * 
 * @author maras
 */
@Service
public class StatisticsMonthlyProjectFileDownloadsProcessor implements StatisticsMonthlyProcessor {

	@Override
	public StatisticsObjectType getSupportedType() {
		return StatisticsObjectType.PROJECT;
	}

	@Override
	public void processMonth(YearMonth month) {
		throw new UnsupportedOperationException("Not implemented yet");
	}

}
