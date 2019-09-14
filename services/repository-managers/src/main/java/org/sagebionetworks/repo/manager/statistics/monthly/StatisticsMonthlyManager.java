package org.sagebionetworks.repo.manager.statistics.monthly;

import java.time.YearMonth;
import java.util.List;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.statistics.StatisticsProcessingException;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.sagebionetworks.repo.model.statistics.StatisticsStatus;

public interface StatisticsMonthlyManager {

	/**
	 * Returns the list of {@link YearMonth} that might need to be processed for the given type of object, in particular all
	 * the months that do not have any status or that are not in {@link StatisticsStatus#AVAILABLE} status.
	 * 
	 * Note that this method might return months that are in {@link StatisticsStatus#PROCESSING} status.
	 * 
	 * @param  objectType The statistics object type
	 * @return            The list of {@link YearMonth} that still needs to be processed
	 */
	List<YearMonth> getUnprocessedMonths(StatisticsObjectType objectType);

	/**
	 * Set the status for the object of the given type and the given month to PROCESSING if the status is not already
	 * available or if the given month is in processing status and the lastStartedOn exceeded the allowed timeout. If the
	 * status was updated sends a notification to the background workers to start processing.
	 * 
	 * @param  objectType
	 * @param  month
	 * @param  processingTimeout
	 * @return                   True if the status was set or refreshed to PROCESSING
	 */
	boolean startProcessingMonth(StatisticsObjectType objectType, YearMonth month, long processingTimeout);

	/**
	 * Process the statistics for the given object type and month. If any error occurs during the processing sets the status
	 * to {@link StatisticsStatus#PROCESSING_FAILED}, otherwise set it to {@link StatisticsStatus#AVAILABLE} at the end of
	 * the processing. This method is invoked by a worker upon receiving a notification to start processing a particular
	 * month for a given object type. A listener will be registered on the given {@link ProgressCallback} to update the
	 * lastUpdatedOn timestamp of the status.
	 * 
	 * @param  objectType                    The statistics object type
	 * @param  month                         The month to be processed
	 * @param  progressCallback              The callback that the thread will register on to update the lastUpdatedOn
	 *                                       timestamp for the status
	 * @throws StatisticsProcessingException If something goes wrong while processing the given month
	 */
	void processMonth(StatisticsObjectType objectType, YearMonth month, ProgressCallback progressCallback)
			throws StatisticsProcessingException;

}
