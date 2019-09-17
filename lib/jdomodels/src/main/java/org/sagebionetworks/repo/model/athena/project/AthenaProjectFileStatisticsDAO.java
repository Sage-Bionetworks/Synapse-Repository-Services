package org.sagebionetworks.repo.model.athena.project;

import java.time.YearMonth;

import org.sagebionetworks.repo.model.athena.AthenaQueryResult;
import org.sagebionetworks.repo.model.statistics.FileEvent;
import org.sagebionetworks.repo.model.statistics.project.StatisticsMonthlyProjectFiles;

/**
 * Data access layer on top of athena for project files
 * 
 * @author Marco
 *
 */
public interface AthenaProjectFileStatisticsDAO {

	/**
	 * Aggregates the count of files and users by project for the given month and {@link FileEvent type of event}
	 * 
	 * @param  eventType The type of file event
	 * @param  month     The month to aggregate the data for
	 * @return           The {@link AthenaQueryResult query result} allowing access to the results iterator
	 */
	AthenaQueryResult<StatisticsMonthlyProjectFiles> aggregateForMonth(FileEvent eventType, YearMonth month);

}
