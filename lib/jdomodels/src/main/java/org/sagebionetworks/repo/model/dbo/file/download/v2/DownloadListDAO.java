package org.sagebionetworks.repo.model.dbo.file.download.v2;

import java.util.List;

import org.sagebionetworks.repo.model.download.ColumnName;
import org.sagebionetworks.repo.model.download.DownloadListItem;
import org.sagebionetworks.repo.model.download.IdAndVersion;
import org.sagebionetworks.repo.model.download.SortDirection;

public interface DownloadListDAO {

	/**
	 * Add a batch of files to a user's download list.
	 * 
	 * @param userId     The id of the user.
	 * @param batchToAdd The batch of files to add.
	 * @return The number of files that were actually added.
	 */
	long addBatchOfFilesToDownloadList(Long userId, List<IdAndVersion> batchToAdd);

	/**
	 * 
	 * @param userId        The id of the user.
	 * @param batchToRemove The batch of files to remove.
	 * @return The number of files that were actually removed.
	 */
	long removeBatchOfFilesFromDownloadList(Long userId, List<IdAndVersion> batchToRemove);

	/**
	 * Clear all files from the user's download list.
	 * 
	 * @param userId
	 */
	void clearDownloadList(Long userId);

	/**
	 * Get a single page of files from a user's download list that are available for
	 * download.
	 * 
	 * @param userId
	 * @param sortColumn
	 * @param sortDirection
	 * @param limit
	 * @param offset
	 * @return
	 */
	List<DownloadListItem> getFilesAvailableToDownloadFromDownloadList(Long userId, ColumnName sortColumn,
			SortDirection sortDirection, Long limit, Long offset);
	
	/**
	 * Get the DBODownloadList for the given user.
	 * @param userId
	 * @return
	 */
	DBODownloadList getDBODownloadList(Long userId);
	
	/**
	 * 
	 * @param userId
	 * @return
	 */
	List<DBODownloadListItem> getDBODownloadListItems(Long userId);
	
	/**
	 * Clear all download data for all users.
	 */
	void truncateAllData();

}
