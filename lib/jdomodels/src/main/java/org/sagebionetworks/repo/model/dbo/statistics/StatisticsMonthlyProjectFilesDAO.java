package org.sagebionetworks.repo.model.dbo.statistics;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.statistics.FileEvent;
import org.sagebionetworks.repo.model.statistics.project.StatisticsMonthlyProjectFiles;

public interface StatisticsMonthlyProjectFilesDAO {

	/**
	 * Returns the count of distinct projects that have statistics in the given range
	 * 
	 * @param  eventType The type of event performed on files in the range
	 * @param  from      The month identifying the start of the range (inclusive)
	 * @param  to        The month identifying the end of the range (inclusive)
	 * @return           The count of distinct projects that have statistics in the given range
	 */
	Long countProjectsInRange(FileEvent eventType, YearMonth from, YearMonth to);

	/**
	 * Return a list of statistics for the given project in the given range
	 * 
	 * @param  projectId The if of the project
	 * @param  action    The type of event performed on files in the range
	 * @param  from      The month identifying the start of the range (inclusive)
	 * @param  to        The month identifying the end of the range (inclusive)
	 * @return           The list of statistics for the given project in the given range
	 */
	List<StatisticsMonthlyProjectFiles> getProjectFilesStatisticsInRange(Long projectId, FileEvent eventType, YearMonth from, YearMonth to);

	/**
	 * @param  projectId The id of the project
	 * @param  action    The type of event performed on files
	 * @param  month     The {@link YearMonth month} for which to retrieve the statistics
	 * @return           An {@link Optional} containing the statistics about the given {@link FileEvent event type} on files
	 *                   for the given project and month
	 */
	Optional<StatisticsMonthlyProjectFiles> getProjectFilesStatistics(Long projectId, FileEvent eventType, YearMonth month);

	/**
	 * Saves the given batch of project file statistics
	 * 
	 * @param  batch
	 * @return
	 */
	void save(List<StatisticsMonthlyProjectFiles> batch);

	/**
	 * Clear all the project files monthly statistics
	 */
	void clear();

}
