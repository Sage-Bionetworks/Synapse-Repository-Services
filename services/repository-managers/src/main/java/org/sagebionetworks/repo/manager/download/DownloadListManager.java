package org.sagebionetworks.repo.manager.download;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListResponse;
import org.sagebionetworks.repo.model.download.DownloadListQueryRequest;
import org.sagebionetworks.repo.model.download.DownloadListQueryResponse;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListRequest;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListResponse;

public interface DownloadListManager {

	/**
	 * Add a batch of files to the user's download list.
	 * 
	 * @param userInfo
	 * @param toAdd
	 * @return
	 */
	AddBatchOfFilesToDownloadListResponse addBatchOfFilesToDownloadList(UserInfo userInfo,
			AddBatchOfFilesToDownloadListRequest toAdd);

	/**
	 * Query the user's download list.
	 * 
	 * @param userInfo
	 * @param requestBody
	 * @return
	 */
	DownloadListQueryResponse queryDownloadList(UserInfo userInfo, DownloadListQueryRequest requestBody);

	/**
	 * Remove a batch of files from the user's download list.
	 * 
	 * @param userInfo
	 * @param toRemove
	 * @return
	 */
	RemoveBatchOfFilesFromDownloadListResponse removeBatchOfFilesFromDownloadList(UserInfo userInfo,
			RemoveBatchOfFilesFromDownloadListRequest toRemove);

	/**
	 * Clear the user's download list.
	 * @param userInfo
	 */
	void clearDownloadList(UserInfo userInfo);
}
