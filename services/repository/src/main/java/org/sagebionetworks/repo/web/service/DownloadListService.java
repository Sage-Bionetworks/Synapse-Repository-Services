package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListResponse;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListRequest;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListResponse;

public interface DownloadListService {

	/**
	 * Add a batch of files to the user's download list.
	 * 
	 * @param userId
	 * @param toAdd
	 * @return
	 */
	AddBatchOfFilesToDownloadListResponse addBatchOfFilesToDownloadList(Long userId,
			AddBatchOfFilesToDownloadListRequest toAdd);

	/**
	 * Remove a batch of files from the user's download list.
	 * @param userId
	 * @param toRemove
	 * @return
	 */
	RemoveBatchOfFilesFromDownloadListResponse removeBatchOfFilesFromDownloadList(Long userId,
			RemoveBatchOfFilesFromDownloadListRequest toRemove);

	/**
	 * Clear the user's download list.
	 * @param userId
	 */
	void clearDownloadList(Long userId);

}
