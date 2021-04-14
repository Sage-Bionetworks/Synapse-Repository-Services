package org.sagebionetworks.repo.manager.download;

import java.util.Arrays;
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
import org.sagebionetworks.repo.model.dbo.file.download.v2.EntityAccessCallback;
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
import org.sagebionetworks.repo.model.download.Sort;
import org.sagebionetworks.repo.model.download.SortDirection;
import org.sagebionetworks.repo.model.download.SortField;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DownloadListManagerImpl implements DownloadListManager {

	public static final String YOU_MUST_LOGIN_TO_ACCESS_YOUR_DOWNLOAD_LIST = "You must login to access your download list";
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

	private EntityAuthorizationManager entityAuthorizationManager;
	private DownloadListDAO downloadListDao;

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
		// filter out all non-files from the input
		List<DownloadListItem> filteredBatch = downloadListDao.filterUnsupportedTypes(toAdd.getBatchToAdd());
		long currentFileCount = downloadListDao.getTotalNumberOfFilesOnDownloadList(userInfo.getId());
		if (filteredBatch.size() + currentFileCount > MAX_FILES_PER_USER) {
			throw new IllegalArgumentException(String.format(ADDING_S_FILES_EXCEEDS_LIMIT_TEMPLATE,
					filteredBatch.size(), MAX_FILES_PER_USER, currentFileCount));
		}
		long numberOfFilesAdded = downloadListDao.addBatchOfFilesToDownloadList(userInfo.getId(), filteredBatch);
		return new AddBatchOfFilesToDownloadListResponse().setNumberOfFilesAdded(numberOfFilesAdded);
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
		ValidateArgument.required(requestBody, "requestBody");
		ValidateArgument.required(requestBody.getRequestDetails(), "requestBody.requestDetails");
		if (requestBody.getRequestDetails() instanceof AvailableFilesRequest) {
			return new DownloadListQueryResponse().setReponseDetails(
					queryAvailableFiles(userInfo, (AvailableFilesRequest) requestBody.getRequestDetails()));
		}
		throw new IllegalArgumentException("Unknown type: " + requestBody.getRequestDetails().getConcreteType());
	}

	/**
	 * Query for a single page of files that are available from the user's download
	 * list.
	 * 
	 * @param userInfo
	 * @param availableRequest
	 * @return
	 */
	AvailableFilesResponse queryAvailableFiles(UserInfo userInfo, AvailableFilesRequest availableRequest) {
		validateUser(userInfo);
		if (availableRequest == null) {
			availableRequest = new AvailableFilesRequest();
		}

		List<Sort> sort = availableRequest.getSort();
		if (sort == null || sort.isEmpty()) {
			sort = getDefaultSort();
		}

		NextPageToken pageToken = new NextPageToken(availableRequest.getNextPageToken());

		List<DownloadListItemResult> page = downloadListDao.getFilesAvailableToDownloadFromDownloadList(
				createAccessCallback(userInfo), userInfo.getId(), sort, pageToken.getLimitForQuery(),
				pageToken.getOffset());

		return new AvailableFilesResponse().setNextPageToken(pageToken.getNextPageTokenForCurrentResults(page))
				.setPage(page);
	}

	/**
	 * Create a default sort.
	 * 
	 * @return
	 */
	public static List<Sort> getDefaultSort() {
		return Arrays.asList(new Sort().setField(SortField.synId).setDirection(SortDirection.ASC),
				new Sort().setField(SortField.versionNumber).setDirection(SortDirection.ASC));
	}

	/**
	 * Create a callback to filter the entities that the given user can download.
	 * 
	 * @param userInfo
	 * @return
	 */
	EntityAccessCallback createAccessCallback(UserInfo userInfo) {
		return (List<Long> enityIds) -> {
			// Determine which files of this batch the user can download.
			List<UsersEntityAccessInfo> batchInfo = entityAuthorizationManager.batchHasAccess(userInfo, enityIds,
					ACCESS_TYPE.DOWNLOAD);
			// filter out any entity that the user is not authorized to download.
			return batchInfo.stream().filter(e -> e.getAuthroizationStatus().isAuthorized()).map(e -> e.getEntityId())
					.collect(Collectors.toList());
		};
	}

	/**
	 * Validate that the passed batch is not null and within the limit.
	 * 
	 * @param size
	 */
	static void validateBatch(List<DownloadListItem> batch) {
		ValidateArgument.required(batch, "batch");
		if (batch.size() < 1) {
			throw new IllegalArgumentException("Batch must contain at least one item");
		}
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
			throw new UnauthorizedException(YOU_MUST_LOGIN_TO_ACCESS_YOUR_DOWNLOAD_LIST);
		}
	}

}
