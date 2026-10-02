package org.sagebionetworks.repo.model.dbo.file.download.v2;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.EntityRef;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.download.ActionRequiredCount;
import org.sagebionetworks.repo.model.download.AddToDownloadListStatsResponse;
import org.sagebionetworks.repo.model.download.AvailableFilter;
import org.sagebionetworks.repo.model.download.DownloadListItem;
import org.sagebionetworks.repo.model.download.DownloadListItemResult;
import org.sagebionetworks.repo.model.download.FilesStatisticsResponse;
import org.sagebionetworks.repo.model.download.Sort;

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
	 * Group the given items by the {@link EntityType} of the entity each one references. An item
	 * whose entity does not exist, or whose type is not one of the given types, is excluded from the
	 * result entirely.
	 *
	 * @param batch The items to group. Not mutated.
	 * @param types The set of types to group by. Any item referencing an entity of a type not in this
	 *              set is excluded from the result. An empty set matches nothing.
	 * @return A map from each type present in the batch (and in types) to the sublist of items of
	 *         that type, each sublist in the same relative order as the given batch. A type with no
	 *         matching items is simply absent from the map (not mapped to an empty list).
	 */
	Map<EntityType, List<DownloadListItem>> groupItemsByType(List<DownloadListItem> batch, Set<EntityType> types);
	
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
	 * @param userId
	 * @param parentId
	 * @param useVersion When true, the current version of the file will be used.
	 *                   When false, the version number will be null;
	 * @param limit      Limit the number of files that can be added.
	 * @return The total number of files added.
	 */
	Long addChildrenToDownloadList(Long userId, Long parentId, boolean useVersion, long limit);
	
	/**
	 * @param parentId
	 * @return The count and size of files that are children of the container with the given parentId
	 */
	AddToDownloadListStatsResponse getAddChildrenToDownloadListStats(Long parentId);
	
	/**
	 * Add all the files in the tree rooted in the given parentId to the user's download list.
	 * @param userId
	 * @param parentId
	 * @param useVersion When true, the current version of the file will be used.
	 *                   When false, the version number will be null;
	 * @param limit      Limit the number of files that can be added.
	 * @return
	 */
	Long addDescendantsToDownloadList(Long userId, Long parentId, boolean useVersion, long limit);

	/**
	 * @param parentId
	 * @param maxContainers The max number of containers to use in the computation, if the container size is exceeded will
	 *                      set the {@link AddToDownloadListStatsResponse#getIsFileCountAndSizeEstimate()} to true.
	 * @return The count and size of files contained in the given parent container
	 */
	AddToDownloadListStatsResponse getAddDescendantsToDownloadListStats(Long parentId, int maxContainers);
	
	/**
	 * For the given item load all of the details needed to write to a manifest
	 * @param item
	 * @return
	 */
	JSONObject getItemManifestDetails(DownloadListItem item);

	/**
	 * Adds all of the files referenced in the given list of {@link EntityRef} to the user's download list.
	 * 
	 * @param userId
	 * @param fileRefs
	 * @param limit	Limit the number of files that can be added.
	 * @return The total number of files added.
	 */
	Long addFileEntityRefToDownloadList(Long userId, List<EntityRef> fileRefs, long limit);
	
	/**
	 * @param fileRefs
	 * @return The total count and size of files referenced by the given list of {@link EntityRef}
	 */
	AddToDownloadListStatsResponse getAddFileEntityRefToDownloadListStats(List<EntityRef> fileRefs);
	
	/**
	 * Add all the files that are reference by each of the data set referenced in the given list of {@link EntityRef} to the user's download list.
	 * 
	 * @param userId
	 * @param datasetRefs
	 * @param limit Limit the number of files that can be added.
	 * @return
	 */
	Long addDatasetEntityRefFilesToDownloadList(Long userId, List<EntityRef> datasetRefs, long limit);

	/**
	 * @param datasets
	 * @return The total count and size of files referenced by each dataset in the given list of {@link EntityRef}
	 */
	AddToDownloadListStatsResponse getAddDatasetEntityRefFilesToDownloadListStats(List<EntityRef> datasetRefs);

	/**
	 * Count the files that are both one of the given file refs and a member of one of the given
	 * datasets. Only members that have a file handle are counted.
	 *
	 * @param fileRefs    The file refs to look for, each identified by an entity id and a version
	 *                    number. A ref that does not resolve to a real revision matches nothing.
	 * @param datasetRefs The datasets whose items are searched, each identified by an entity id and a
	 *                    version number. The items of that specific version are searched.
	 * @return The number of distinct entity id and version pairs present in both sets. A file that is
	 *         a member of more than one of the given datasets is counted once.
	 */
	long countFileRefsInDatasets(List<EntityRef> fileRefs, List<EntityRef> datasetRefs);

}
