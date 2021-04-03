package org.sagebionetworks.repo.manager.download;

import java.util.List;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.entity.decider.UsersEntityAccessInfo;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.download.v2.DownloadListDAO;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListResponse;
import org.sagebionetworks.repo.model.download.AvailableFilesRequest;
import org.sagebionetworks.repo.model.download.AvailableFilesResponse;
import org.sagebionetworks.repo.model.download.DownloadListItem;
import org.sagebionetworks.repo.model.download.DownloadListItemResult;
import org.sagebionetworks.repo.model.download.DownloadListQueryRequest;
import org.sagebionetworks.repo.model.download.DownloadListQueryResponse;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListRequest;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListResponse;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DownloadListManagerImpl implements DownloadListManager {

	public static final String BATCH_SIZE_EXCEEDS_LIMIT_TEMPLATE = "Batch size of '%s' exceeds the maximum of '%s'";
	public static final String ADDING_S_FILES_EXCEEDS_LIMIT_TEMPLATE = "Adding '%s' files to your download list would exceed the maximum number of '%s' files.  You currently have '%s' files on you download list.";

	/**
	 * The maximum number of file that can be added/removed in a single batch.
	 */
	public static int MAX_FILES_PER_BATCH = 1000;
	/**
	 * The maximum number of files that a user can have on their download list.
	 */
	public static long MAX_FILES_PER_USER = 100 * 1000;

	EntityAuthorizationManager entityAuthorizationManager;
	DownloadListDAO downloadListDao;

	@Autowired
	public DownloadListManagerImpl(EntityAuthorizationManager entityAuthorizationManager,
			DownloadListDAO downloadListDao) {
		super();
		this.entityAuthorizationManager = entityAuthorizationManager;
		this.downloadListDao = downloadListDao;
	}

	@WriteTransaction
	@Override
	public AddBatchOfFilesToDownloadListResponse addBatchOfFilesToDownloadList(UserInfo userInfo,
			AddBatchOfFilesToDownloadListRequest toAdd) {
		validateUser(userInfo);
		ValidateArgument.required(toAdd, "toAdd");
		validateBatch(toAdd.getBatchToAdd());
		long currentFileCount = downloadListDao.getTotalNumberOfFilesOnDownloadList(userInfo.getId());
		if (toAdd.getBatchToAdd().size() + currentFileCount > MAX_FILES_PER_USER) {
			throw new IllegalArgumentException(String.format(ADDING_S_FILES_EXCEEDS_LIMIT_TEMPLATE,
					toAdd.getBatchToAdd().size(), MAX_FILES_PER_USER, currentFileCount));
		}

		long nubmerOfFilesAdded = downloadListDao.addBatchOfFilesToDownloadList(userInfo.getId(),
				toAdd.getBatchToAdd());
		return new AddBatchOfFilesToDownloadListResponse().setNumberOfFilesAdded(nubmerOfFilesAdded);
	}

	@WriteTransaction
	@Override
	public RemoveBatchOfFilesFromDownloadListResponse removeBatchOfFilesFromDownloadList(UserInfo userInfo,
			RemoveBatchOfFilesFromDownloadListRequest toRemove) {
		validateUser(userInfo);
		ValidateArgument.required(toRemove, "toRemove");
		validateBatch(toRemove.getBatchToRemove());
		long numberOfFilesRemoved = downloadListDao.removeBatchOfFilesFromDownloadList(userInfo.getId(),
				toRemove.getBatchToRemove());
		return new RemoveBatchOfFilesFromDownloadListResponse().setNumberOfFilesRemoved(numberOfFilesRemoved);
	}

	@WriteTransaction
	@Override
	public void clearDownloadList(UserInfo userInfo) {
		validateUser(userInfo);
		downloadListDao.clearDownloadList(userInfo.getId());
	}

	@Override
	public DownloadListQueryResponse queryDownloadList(UserInfo userInfo, DownloadListQueryRequest requestBody) {
		validateUser(userInfo);
		ValidateArgument.required(requestBody, "requestBody");
		AvailableFilesResponse availableFiles = null;
		// The AvailableFilesRequest is optional.
		if (requestBody.getAvailableFilesRequest() != null) {
			availableFiles = queryAvialableFiles(userInfo, requestBody.getAvailableFilesRequest());
		}
		return new DownloadListQueryResponse().setAvailableFiles(availableFiles);
	}

	/**
	 * Query for a single page of files that are available from the user's download
	 * list.
	 * 
	 * @param userInfo
	 * @param availableRequest
	 * @return
	 */
	AvailableFilesResponse queryAvialableFiles(UserInfo userInfo, AvailableFilesRequest availableRequest) {
		NextPageToken pageToken = new NextPageToken(availableRequest.getNextPageToken());

		List<DownloadListItemResult> page = downloadListDao
				.getFilesAvailableToDownloadFromDownloadList((List<Long> enityIds) -> {
					// Determine which files of this batch the user can download.
					List<UsersEntityAccessInfo> batchInfo = entityAuthorizationManager.batchHasAccess(userInfo,
							enityIds, ACCESS_TYPE.DOWNLOAD);
					// filter out any entity that the user is not authorized to download.
					return batchInfo.stream().filter(e -> e.getAuthroizationStatus().isAuthorized())
							.map(e -> e.getEntityId()).collect(Collectors.toList());
				}, userInfo.getId(), availableRequest.getSort(), pageToken.getLimitForQuery(), pageToken.getOffset());

		return new AvailableFilesResponse().setNextPageToken(pageToken.getNextPageTokenForCurrentResults(page))
				.setPage(page);
	}

	/**
	 * Validate that the passed batch is not null and within the limit.
	 * 
	 * @param size
	 */
	static void validateBatch(List<DownloadListItem> batch) {
		ValidateArgument.required(batch, "batch");
		if (batch.size() > MAX_FILES_PER_BATCH) {
			throw new IllegalArgumentException(
					String.format(BATCH_SIZE_EXCEEDS_LIMIT_TEMPLATE, batch.size(), MAX_FILES_PER_BATCH));
		}
	}

	/**
	 * Must have a valid, non-anonymous users.
	 * 
	 * @param userInfo
	 */
	static void validateUser(UserInfo userInfo) {
		ValidateArgument.required(userInfo, "userInfo");
		if (AuthorizationUtils.isUserAnonymous(userInfo)) {
			throw new UnauthorizedException("Must login to access your download list");
		}
	}

}
