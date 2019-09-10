package org.sagebionetworks.repo.model.dao.statistics;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.model.statistics.FileAction;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyProjectFiles;

public interface StatisticsMonthlyProjectDAO {
	
	/**
	 * Return a list of statistics for the given project in the given range
	 * 
	 * @param projectId The if of the project
	 * @param action The type of action performed on files in the range
	 * @param from The month identifying the start of the range (inclusive)
	 * @param to The month identifying the end of the range (inclusive)
	 * @return
	 */
	List<StatisticsMonthlyProjectFiles> getProjectFilesStatisticsInRange(Long projectId, FileAction action, YearMonth from, YearMonth to);

	/**
	 * @param projectId The id of the project
	 * @param action    The type of action performed on files
	 * @param month     The {@link YearMonth month} for which to retrieve the statistics
	 * @return An {@link Optional} containing the statistics about the given {@link FileAction action
	 *         type} on files for the given project and month
	 */
	Optional<StatisticsMonthlyProjectFiles> getProjectFilesStatistics(Long projectId, FileAction action, YearMonth month);

	/**
	 * Saves the given batch of project file statistics
	 * 
	 * @param batch
	 * @return
	 */
	void saveBatch(List<StatisticsMonthlyProjectFiles> batch);

}
