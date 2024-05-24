package org.sagebionetworks.repo.manager.download;

import java.io.IOException;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListResponse;
import org.sagebionetworks.repo.model.download.AddToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddToDownloadListResponse;
import org.sagebionetworks.repo.model.download.DownloadListManifestRequest;
import org.sagebionetworks.repo.model.download.DownloadListManifestResponse;
import org.sagebionetworks.repo.model.download.DownloadListPackageRequest;
import org.sagebionetworks.repo.model.download.DownloadListPackageResponse;
import org.sagebionetworks.repo.model.download.DownloadListQueryRequest;
import org.sagebionetworks.repo.model.download.DownloadListQueryResponse;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListRequest;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListResponse;
import org.sagebionetworks.util.progress.ProgressCallback;

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
	 * 
	 * @param userInfo
	 */
	void clearDownloadList(UserInfo userInfo);

	/**
	 * Add files to the user's download list from either a view query or folder.
	 * 
	 * @param userInfo
	 * @param requestBody
	 * @return
	 */
	AddToDownloadListResponse addToDownloadList(ProgressCallback progressCallback, UserInfo userInfo,
			AddToDownloadListRequest requestBody);

	/**
	 * Request to package files from a user's download list into a zip file.
	 * 
	 * @param progressCallback
	 * @param userInfo
	 * @param requestBody
	 * @return
	 * @throws IOException
	 */
	DownloadListPackageResponse packageFiles(ProgressCallback progressCallback, UserInfo userInfo,
			DownloadListPackageRequest requestBody) throws IOException;

	/**
	 * Request to create a CSV manifest for the available files from a user's
	 * download list.
	 * 
	 * @param progressCallback
	 * @param userInfo
	 * @param requestBody
	 * @return
	 * @throws IOException 
	 */
	DownloadListManifestResponse createManifest(ProgressCallback progressCallback, UserInfo userInfo,
			DownloadListManifestRequest requestBody) throws IOException;

}
