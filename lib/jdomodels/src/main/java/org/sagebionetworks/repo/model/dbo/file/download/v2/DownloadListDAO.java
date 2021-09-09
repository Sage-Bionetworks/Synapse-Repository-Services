package org.sagebionetworks.repo.model.dbo.file.download.v2;

import java.util.List;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.download.ActionRequiredCount;
import org.sagebionetworks.repo.model.download.AvailableFilter;
import org.sagebionetworks.repo.model.download.DownloadListItem;
import org.sagebionetworks.repo.model.download.DownloadListItemResult;
import org.sagebionetworks.repo.model.download.FilesStatisticsResponse;
import org.sagebionetworks.repo.model.download.Sort;
import org.sagebionetworks.repo.model.table.DatasetItem;

public interface DownloadListDAO {

	/**
	 * Add a batch of files to a user's download list.
	 * 
	 * @param userId     The id of the user.
	 * @param batchToAdd The batch of files to add.
	 * @return The number of files that were actually added.
	 */
	long addBatchOfFilesToDownloadList(Long userId, List<DownloadListItem> batchToAdd);
	
	
	/**
	 * For a given list of DownloadListItem, filter out all items that are not actual files.
	 * 
	 * @param batch
	 * @return
	 */
	List<DownloadListItem> filterUnsupportedTypes(List<DownloadListItem> batch);
	
	/**
	 * 
	 * @param userId        The id of the user.
	 * @param batchToRemove The batch of files to remove.
	 * @return The number of files that were actually removed.
	 */
	long removeBatchOfFilesFromDownloadList(Long userId, List<? extends DownloadListItem> batchToRemove);

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
	 * @param accessCallback Callback used to determine which entities on the user's
	 *                       download list that the user can download.
	 * @param userId
	 * @param filter
	 * @param sort
	 * @param limit
	 * @param offset
	 * @return
	 */
	List<DownloadListItemResult> getFilesAvailableToDownloadFromDownloadList(EntityAccessCallback accessCallback,
			Long userId, AvailableFilter filter, List<Sort> sort, Long limit, Long offset);

	/**
	 * Get the DBODownloadList for the given user.
	 * 
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
	 * Get the DownloadListItemResult for the given user and each item.
	 * 
	 * @param userId
	 * @param item
	 * @return
	 */
	List<DownloadListItemResult> getDownloadListItems(Long userId, DownloadListItem... item);

	/**
	 * Clear all download data for all users.
	 */
	void truncateAllData();

	/**
	 * Read all of value from a temporary of the items from a user's download list
	 * that the user has download access to.
	 * 
	 * @param accessCallback
	 * @param userId
	 * @param batchSize
	 * @return
	 */
	List<Long> getAvailableFilesFromDownloadList(EntityAccessCallback accessCallback, Long userId, int batchSize);

	/**
	 * Get the total number of files currently on the user's download list.
	 * 
	 * @param userId
	 * @return
	 */
	long getTotalNumberOfFilesOnDownloadList(Long userId);


	/**
	 * Get the download list statistics for the given user.
	 * @param createAccessCallback
	 * @param id
	 * @return
	 */
	FilesStatisticsResponse getListStatistics(EntityAccessCallback createAccessCallback, Long id);
	
	/**
	 * Get a single page of actions required to download one or more files from the user's download list.
	 * @param callback
	 * @param userId
	 * @param limit
	 * @param offset
	 * @return
	 */
	List<ActionRequiredCount> getActionsRequiredFromDownloadList(EntityActionRequiredCallback callback,
			Long userId, Long limit, Long offset);


	/**
	 * Add all of the children for the given parentId to the user's download list.
	 * 
	 * @param id
	 * @param parentId
	 * @param useVersion When true, the current version of the file will be used.
	 *                   When false, the version number will be null;
	 * @param limit      Limit the number of files that can be added.
	 * @return The total number of files added.
	 */
	Long addChildrenToDownloadList(Long id, Long parentId, boolean useVersion, long limit);
	
	/**
	 * For the given item load all of the details needed to write to a manifest
	 * @param item
	 * @return
	 */
	JSONObject getItemManifestDetails(DownloadListItem item);


	/**
	 * Adds all of the files in the list of dataset items to the user's download list.
	 * 
	 * @param userId
	 * @param items
	 * @param useVersion When true, the current version of the file will be used.
	 * 					 When false, the version number will be null.
	 * @param limit		 Limit the number of files that can be added.
	 * @return The total number of files added.
	 */
	Long addDatasetItemsToDownloadList(Long userId, List<DatasetItem> items, boolean useVersion, long limit);

}
