package org.sagebionetworks.repo.manager.file.download;

import com.google.common.collect.Lists;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityChildrenResponse;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.download.BulkDownloadDAO;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.file.BatchFileRequest;
import org.sagebionetworks.repo.model.file.BatchFileResult;
import org.sagebionetworks.repo.model.file.DownloadList;
import org.sagebionetworks.repo.model.file.DownloadOrder;
import org.sagebionetworks.repo.model.file.DownloadOrderSummary;
import org.sagebionetworks.repo.model.file.DownloadOrderSummaryRequest;
import org.sagebionetworks.repo.model.file.DownloadOrderSummaryResponse;
import org.sagebionetworks.repo.model.file.FileConstants;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.FileResult;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BulkDownloadManagerImpl implements BulkDownloadManager {

	public static final String ONLY_THE_OWNER_MAY_GET_A_DOWNLOAD_ORDER = "Only the owner may get a DownloadOrder";
	public static final String COULD_NOT_DOWNLOAD_ANY_FILES_FROM_THE_DOWNLOAD_LIST = "Could not download any files from the download list.";
	public static final String THE_DOWNLOAD_LIST_IS_EMPTY = "The download list is empty";
	public static final String FILES_CAN_ONLY_BE_ADDED_FROM_A_FILE_VIEW_QUERY = "Files can only be added from a file view query.";
	public static final int MAX_FILES_PER_DOWNLOAD_LIST = 100;
	public static final long QUERY_ONLY_PART_MASK = 0x1;

	public static final String EXCEEDED_MAX_NUMBER_ROWS = "We’re Sorry. Our current Download List allowance is "+
			MAX_FILES_PER_DOWNLOAD_LIST+
			" files. Please use either Programmatic Options for Download, remove existing files from your Download List, or change the size of your query.";

	@Autowired
	EntityManager entityManager;

	@Autowired
	NodeDAO nodeDoa;

	@Autowired
	BulkDownloadDAO bulkDownloadDao;

	@Autowired
	TableQueryManager tableQueryManager;

	@Autowired
	FileHandleManager fileHandleManager;

	@WriteTransaction
	@Override
	public DownloadList addFilesFromFolder(UserInfo user, String folderId) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(folderId, "Folder ID");
		List<EntityType> includeTypes = Lists.newArrayList(EntityType.file);
		String nextPageToken = null;
		do {
			EntityChildrenRequest entityChildrenRequest = new EntityChildrenRequest();
			entityChildrenRequest.setIncludeTypes(includeTypes);
			entityChildrenRequest.setParentId(folderId);
			entityChildrenRequest.setNextPageToken(nextPageToken);
			// page through the children of the given container.
			EntityChildrenResponse entityChildrenResponse = entityManager.getChildren(user, entityChildrenRequest);
			// get the files handles for the resulting files
			List<String> entityIds = new LinkedList<>();
			for (EntityHeader header : entityChildrenResponse.getPage()) {
				entityIds.add(header.getId());
			}
			// get the files handle associations for each file.
			List<FileHandleAssociation> toAdd = nodeDoa.getFileHandleAssociationsForCurrentVersion(entityIds);
			attemptToAddFilesToUsersDownloadList(user, toAdd);

			// use the token to get the next page.
			nextPageToken = entityChildrenResponse.getNextPageToken();
			// continue as long as we have a next page token.
		} while (nextPageToken != null);
		// return the final state of the download list.
		return bulkDownloadDao.getUsersDownloadList(user.getId().toString());
	}

	/**
	 * Attempt to add the given files to the user's download list.
	 * 
	 * @param user
	 * @param toAdd
	 * @throws IllegalArgumentException If the resulting total number of files
	 *                                  exceeds the limit.
	 * 
	 */
	DownloadList attemptToAddFilesToUsersDownloadList(UserInfo user, List<FileHandleAssociation> toAdd) {
		DownloadList list = bulkDownloadDao.addFilesToDownloadList(user.getId().toString(), toAdd);
		if (list.getFilesToDownload().size() > MAX_FILES_PER_DOWNLOAD_LIST) {
			throw new IllegalArgumentException(EXCEEDED_MAX_NUMBER_ROWS);
		}
		return list;
	}

	@WriteTransaction
	@Override
	public DownloadList addFilesFromQuery(ProgressCallback progressCallback, UserInfo user, Query query)
			throws DatastoreException, NotFoundException, TableFailedException, RecoverableMessageException {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(query, "Query");
		try {
			QueryBundleRequest queryBundle = new QueryBundleRequest();
			queryBundle.setPartMask(QUERY_ONLY_PART_MASK);
			queryBundle.setQuery(query);
			/*
			 * Setting a limit ensures we never read too many rows. Using a limit of max
			 * plus one provides a mechanism for detecting a query result that would yield
			 * too many rows.
			 */
			query.setLimit(MAX_FILES_PER_DOWNLOAD_LIST + 1L);
			QueryResultBundle queryResult = this.tableQueryManager.queryBundle(progressCallback, user, queryBundle);
			// validate this is query against a file view.
			IdAndVersion idAndVersion = IdAndVersion.parse(queryResult.getQueryResult().getQueryResults().getTableId());
			EntityType tableType = entityManager.getEntityType(user, idAndVersion.getId().toString());
			if (!EntityType.entityview.equals(tableType)) {
				throw new IllegalArgumentException(FILES_CAN_ONLY_BE_ADDED_FROM_A_FILE_VIEW_QUERY);
			}
			List<Row> rows = queryResult.getQueryResult().getQueryResults().getRows();
			if (rows.size() > MAX_FILES_PER_DOWNLOAD_LIST) {
				throw new IllegalArgumentException(EXCEEDED_MAX_NUMBER_ROWS);
			}
			// lookup the file handle for each row.
			List<FileHandleAssociation> toAdd = new ArrayList<FileHandleAssociation>(rows.size());
			for (Row row : rows) {
				String fileHandleId = nodeDoa.getFileHandleIdForVersion(row.getRowId().toString(), row.getVersionNumber());
				FileHandleAssociation fa = new FileHandleAssociation();
				fa.setAssociateObjectId(row.getRowId().toString());
				fa.setFileHandleId(fileHandleId);
				fa.setAssociateObjectType(FileHandleAssociateType.FileEntity);
				toAdd.add(fa);
			}
			return attemptToAddFilesToUsersDownloadList(user, toAdd);

		} catch (LockUnavilableException | TableUnavailableException e) {
			// can re-try when the view becomes available.
			throw new RecoverableMessageException();
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@WriteTransaction
	@Override
	public DownloadList addFileHandleAssociations(UserInfo user, List<FileHandleAssociation> toAdd) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(toAdd, "List<FileHandleAssociation>");
		return attemptToAddFilesToUsersDownloadList(user, toAdd);
	}

	@WriteTransaction
	@Override
	public DownloadList removeFileHandleAssociations(UserInfo user, List<FileHandleAssociation> toRemove) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(toRemove, "List<FileHandleAssociation>");
		return bulkDownloadDao.removeFilesFromDownloadList(user.getId().toString(), toRemove);
	}

	@Override
	public DownloadList getDownloadList(UserInfo user) {
		ValidateArgument.required(user, "UserInfo");
		return bulkDownloadDao.getUsersDownloadList(user.getId().toString());
	}

	@WriteTransaction
	@Override
	public DownloadList clearDownloadList(UserInfo user) {
		ValidateArgument.required(user, "UserInfo");
		return bulkDownloadDao.clearDownloadList(user.getId().toString());
	}

	@Override
	public void truncateAllDownloadDataForAllUsers(UserInfo admin) {
		ValidateArgument.required(admin, "UserInfo");
		if (!admin.isAdmin()) {
			throw new UnauthorizedException("Only an administrator may call this method");
		}
		this.bulkDownloadDao.truncateAllDownloadDataForAllUsers();
	}

	@WriteTransaction
	@Override
	public DownloadOrder createDownloadOrder(UserInfo user, String zipFileName) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(zipFileName, "zipFileName");
		// get the user's current download using the blocking 'FOR UPDATE'.
		DownloadList downloadList = this.bulkDownloadDao.getUsersDownloadListForUpdate(user.getId().toString());
		if (downloadList.getFilesToDownload().isEmpty()) {
			throw new IllegalArgumentException(THE_DOWNLOAD_LIST_IS_EMPTY);
		}
		// Get the sizes of the files the user has permission to download.
		Map<String, Long> downloadableFileSizes = getSizesOfDownloadableFiles(user, downloadList.getFilesToDownload());
		// Build a download order that is under the size limit.
		DownloadOrder order = BulkDownloadManagerImpl.buildDownloadOrderUnderSizeLimit(user,
				downloadList.getFilesToDownload(), downloadableFileSizes, zipFileName);

		if (order.getFiles().isEmpty()) {
			throw new IllegalArgumentException(COULD_NOT_DOWNLOAD_ANY_FILES_FROM_THE_DOWNLOAD_LIST);
		}
		// remove the files of the order from the user's download list.
		this.bulkDownloadDao.removeFilesFromDownloadList(user.getId().toString(), order.getFiles());
		// save and return the new download order.
		return this.bulkDownloadDao.createDownloadOrder(order);
	}

	/**
	 * Helper to build a Download from the full list of files on the user's and the
	 * file sizes of the files the user has permission to download.
	 * 
	 * @param user
	 * @param fullList              All of the files on the user's download list.
	 * @param downloadableFileSizes The sizes of the files the user has permission
	 *                              to download.
	 * @param zipFileName           The name of the resulting ZIP file.
	 * @return
	 */
	static DownloadOrder buildDownloadOrderUnderSizeLimit(UserInfo user, List<FileHandleAssociation> fullList,
			Map<String, Long> downloadableFileSizes, String zipFileName) {
		// build up a list of files to download up to the size limit.
		long totalSizeBytes = 0L;
		List<FileHandleAssociation> toDownload = new LinkedList<>();
		for (FileHandleAssociation association : fullList) {
			Long fileSizeBytes = downloadableFileSizes.get(association.getFileHandleId());
			if (fileSizeBytes != null) {
				if (totalSizeBytes + fileSizeBytes <= FileConstants.BULK_FILE_DOWNLOAD_MAX_SIZE_BYTES) {
					totalSizeBytes += fileSizeBytes;
					toDownload.add(association);
				}
			}
		}

		// create the new download order
		DownloadOrder order = new DownloadOrder();
		order.setCreatedBy(user.getId().toString());
		order.setCreatedOn(new Date());
		order.setFiles(toDownload);
		order.setTotalNumberOfFiles((long) toDownload.size());
		order.setTotalSizeBytes(totalSizeBytes);
		order.setZipFileName(zipFileName);
		return order;
	}

	/**
	 * For the given list of FileHandleAssociations, get the size of each file that
	 * meets the following criteria:
	 * <ul>
	 * <li>The user has permission to download the file</li>
	 * <li>The file is an S3FileHandle</li>
	 * </ul>
	 * Any file that does not meet this criteria will be excluded from the results.
	 * 
	 * @param user
	 * @param list
	 * @return
	 */
	Map<String, Long> getSizesOfDownloadableFiles(UserInfo user, List<FileHandleAssociation> list) {
		Map<String, Long> downloadableFileSizes = new HashMap<>(list.size());
		// Get the sub-set of files that the user can actually download
		BatchFileRequest request = new BatchFileRequest();
		request.setIncludeFileHandles(true);
		request.setIncludePreSignedURLs(false);
		request.setIncludePreviewPreSignedURLs(false);
		request.setRequestedFiles(list);
		// get the sub-set of files that the user will be able to download.
		BatchFileResult result = fileHandleManager.getFileHandleAndUrlBatch(user, request);
		for (FileResult fileResult : result.getRequestedFiles()) {
			// only files without failure codes.
			if (fileResult.getFailureCode() == null) {
				// only S3 Files.
				FileHandle fileHandle = fileResult.getFileHandle();
				if (fileHandle instanceof S3FileHandle) {
					S3FileHandle s3FileHandle = (S3FileHandle) fileHandle;
					downloadableFileSizes.put(s3FileHandle.getId(), s3FileHandle.getContentSize());
				}
			}
		}
		return downloadableFileSizes;
	}

	@Override
	public DownloadOrderSummaryResponse getDownloadHistory(UserInfo user, DownloadOrderSummaryRequest request) {
		ValidateArgument.required(user, "User");
		ValidateArgument.required(request, "DownloadOrderSummaryRequest");
		NextPageToken token = new NextPageToken(request.getNextPageToken());
		List<DownloadOrderSummary> page = this.bulkDownloadDao.getUsersDownloadOrders(user.getId().toString(),
				token.getLimitForQuery(), token.getOffset());
		DownloadOrderSummaryResponse response = new DownloadOrderSummaryResponse();
		response.setPage(page);
		response.setNextPageToken(token.getNextPageTokenForCurrentResults(page));
		return response;
	}

	@Override
	public DownloadOrder getDownloadOrder(UserInfo user, String orderId) {
		ValidateArgument.required(user, "User");
		ValidateArgument.required(orderId, "OrderId");
		DownloadOrder order = this.bulkDownloadDao.getDownloadOrder(orderId);
		if(!user.getId().toString().equals(order.getCreatedBy())) {
			throw new UnauthorizedException(ONLY_THE_OWNER_MAY_GET_A_DOWNLOAD_ORDER);
		}
		return order;
	}

}
