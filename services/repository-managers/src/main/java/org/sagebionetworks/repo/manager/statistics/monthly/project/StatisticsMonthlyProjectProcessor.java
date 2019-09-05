package org.sagebionetworks.repo.manager.statistics.monthly.project;

import java.time.YearMonth;

import org.sagebionetworks.repo.manager.statistics.monthly.StatisticsMonthlyProcessor;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.springframework.stereotype.Service;

@Service
public class StatisticsMonthlyProjectProcessor implements StatisticsMonthlyProcessor {
	// 30 Minutes is the timeout of athena
	private static final long TIMEOUT = 30 * 60 * 1000;
	
	@Override
	public StatisticsObjectType getSupportedType() {
		return StatisticsObjectType.PROJECT;
	}

	@Override
	public long getProcessingTimeout() {
		return TIMEOUT;
	}

	@Override
	public void processMonth(YearMonth month) {
		// TODO Auto-generated method stub
		
	}

}
