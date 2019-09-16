package org.sagebionetworks.repo.manager.statistics.project;

import java.time.YearMonth;

import org.sagebionetworks.repo.model.statistics.FileEvent;

/**
 * Interface to compute the monthly statistics for projects
 * 
 * @author Marco
 *
 */
public interface StatisticsMonthlyProjectManager {

	/**
	 * Recompute the monthly project statistics for files for the given month and the given type of event
	 * 
	 * @param eventType The type of file event to recompute the statistics for
	 * @param month     The month to recompute the statistics for
	 */
	void computeFileEventsStatistics(FileEvent eventType, YearMonth month);

}
