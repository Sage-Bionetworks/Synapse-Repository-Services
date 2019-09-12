package org.sagebionetworks.repo.manager.statistics.monthly;

import java.time.YearMonth;

import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;

public interface StatisticsMonthlyProcessorNotifier {

	/**
	 * Sends a message on a SQS queue so that the processing for the given object type and month can
	 * start. This method will execute after a transaction is commited
	 * 
	 * @param objectType The statistics object type
	 * @param month      The month to process the stats for
	 */
	void sendStartProcessingNotification(StatisticsObjectType objectType, YearMonth month);
}
