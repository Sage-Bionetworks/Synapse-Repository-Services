package org.sagebionetworks.repo.manager.statistics.monthly.project;

import java.time.YearMonth;

import org.sagebionetworks.repo.manager.statistics.monthly.StatisticsMonthlyProcessor;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.springframework.stereotype.Service;

@Service
public class StatisticsMonthlyProjectProcessor implements StatisticsMonthlyProcessor {
	
	@Override
	public StatisticsObjectType getSupportedType() {
		return StatisticsObjectType.PROJECT;
	}

	@Override
	public void processMonth(YearMonth month) {
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		throw new IllegalStateException("Some error UAU");
	}

}
