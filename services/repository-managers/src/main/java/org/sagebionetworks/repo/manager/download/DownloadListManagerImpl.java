package org.sagebionetworks.repo.manager.download;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONObject;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.entity.decider.UsersEntityAccessInfo;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.FileHandlePackageManager;
import org.sagebionetworks.repo.manager.file.LocalFileUploadRequest;
import org.sagebionetworks.repo.manager.table.TableManagerSupport;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityRef;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.download.v2.DownloadListDAO;
import org.sagebionetworks.repo.model.dbo.file.download.v2.EntityAccessCallback;
import org.sagebionetworks.repo.model.dbo.file.download.v2.EntityActionRequiredCallback;
import org.sagebionetworks.repo.model.dbo.file.download.v2.ManifestKeys;
import org.sagebionetworks.repo.model.download.ActionRequiredCount;
import org.sagebionetworks.repo.model.download.ActionRequiredRequest;
import org.sagebionetworks.repo.model.download.ActionRequiredResponse;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListResponse;
import org.sagebionetworks.repo.model.download.AddToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddToDownloadListResponse;
import org.sagebionetworks.repo.model.download.AvailableFilesRequest;
import org.sagebionetworks.repo.model.download.AvailableFilesResponse;
import org.sagebionetworks.repo.model.download.AvailableFilter;
import org.sagebionetworks.repo.model.download.DownloadListItem;
import org.sagebionetworks.repo.model.download.DownloadListItemResult;
import org.sagebionetworks.repo.model.download.DownloadListManifestRequest;
import org.sagebionetworks.repo.model.download.DownloadListManifestResponse;
import org.sagebionetworks.repo.model.download.DownloadListPackageRequest;
import org.sagebionetworks.repo.model.download.DownloadListPackageResponse;
import org.sagebionetworks.repo.model.download.DownloadListQueryRequest;
import org.sagebionetworks.repo.model.download.DownloadListQueryResponse;
import org.sagebionetworks.repo.model.download.FilesStatisticsRequest;
import org.sagebionetworks.repo.model.download.FilesStatisticsResponse;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListRequest;
import org.sagebionetworks.repo.model.download.RemoveBatchOfFilesFromDownloadListResponse;
import org.sagebionetworks.repo.model.download.Sort;
import org.sagebionetworks.repo.model.download.SortDirection;
import org.sagebionetworks.repo.model.download.SortField;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.file.FileConstants;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.ZipFileFormat;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.table.cluster.TableAndColumnMapper;
import org.sagebionetworks.table.cluster.utils.CSVUtils;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.OrderByClause;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.util.FileProvider;
import org.sagebionetworks.util.PaginationIterator;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import au.com.bytecode.opencsv.CSVWriter;

@Service
public class DownloadListManagerImpl implements DownloadListManager {


	public static final String ZERO_FILE_ID = "0";
	public static final String NO_FILES_AVAILABLE_FOR_DOWNLOAD = "No files available for download.";
	public static final String NO_FILES_ARE_ELIGIBLE_FOR_PACKAGING = "No files are eligible for packaging.";
	public static final String YOUR_DOWNLOAD_LIST_ALREADY_HAS_THE_MAXIMUM_NUMBER_OF_FILES = "Your download list already has the maximum number of '%s' files.";
	public static final String YOU_MUST_LOGIN_TO_ACCESS_YOUR_DOWNLOAD_LIST = "You must login to access your download list";
	public static final String BATCH_SIZE_EXCEEDS_LIMIT_TEMPLATE = "Batch size of '%s' exceeds the maximum of '%s'";
	public static final String ADDING_S_FILES_EXCEEDS_LIMIT_TEMPLATE = "Adding '%s' files to your download list would exceed the maximum number of '%s' files.  You currently have '%s' files on you download list.";
	public static final String ADDING_Q_FILES_EXCEEDS_LIMIT_TEMPLATE = "Adding the files from the given query to your download list would exceed the maximum number of '%s' files.  You currently have '%s' files on you download list.";

	public static boolean DEFAULT_USE_VERSION = true;

	/**
	 * The maximum number of file that can be added/removed in a single batch.
	 */
	public static int MAX_FILES_PER_BATCH = 1000;

	public static final long MAX_QUERY_PAGE_SIZE = 10_000L;

	/**
	 * The maximum number of files that a user can have on their download list.
	 */
	public static long MAX_FILES_PER_USER = 100 * 1000;

	private EntityAuthorizationManager entityAuthorizationManager;
	private DownloadListDAO downloadListDao;
	private TableQueryManager tableQueryManager;
	private TableManagerSupport tableManagerSupport;
	private FileHandlePackageManager fileHandlePackageManager;
	private FileHandleManager fileHandleManager;
	private FileProvider fileProvider;
	private NodeDAO nodeDao;

	@Autowired
	public DownloadListManagerImpl(EntityAuthorizationManager entityAuthorizationManager,
			DownloadListDAO downloadListDao, TableQueryManager tableQueryManager, TableManagerSupport tableManagerSupport,
			FileHandlePackageManager fileHandlePackageManager, FileHandleManager fileHandleManager, FileProvider fileProvider,
			NodeDAO nodeDao) {
		super();
		this.entityAuthorizationManager = entityAuthorizationManager;
		this.downloadListDao = downloadListDao;
		this.tableQueryManager = tableQueryManager;
		this.tableManagerSupport = tableManagerSupport;
		this.fileHandlePackageManager = fileHandlePackageManager;
		this.fileHandleManager = fileHandleManager;
		this.fileProvider = fileProvider;
		this.nodeDao = nodeDao;
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
			return new DownloadListQueryResponse().setResponseDetails(
					queryAvailableFiles(userInfo, (AvailableFilesRequest) requestBody.getRequestDetails()));
		} else if (requestBody.getRequestDetails() instanceof FilesStatisticsRequest) {
			return new DownloadListQueryResponse().setResponseDetails(
					getListStatistics(userInfo, (FilesStatisticsRequest) requestBody.getRequestDetails()));
		} else if (requestBody.getRequestDetails() instanceof ActionRequiredRequest) {
			return new DownloadListQueryResponse().setResponseDetails(
					queryActionRequired(userInfo, (ActionRequiredRequest) requestBody.getRequestDetails()));
		}
		throw new IllegalArgumentException("Unknown type: " + requestBody.getRequestDetails().getConcreteType());
	}

	/**
	 * Query the current user's download list for files that require some action to
	 * be taken in order to download the file.
	 * 
	 * @param userInfo
	 * @param requestDetails
	 * @return
	 */
	ActionRequiredResponse queryActionRequired(UserInfo userInfo, ActionRequiredRequest requestDetails) {
		validateUser(userInfo);
		NextPageToken pageToken = new NextPageToken(requestDetails.getNextPageToken());
		
		List<ActionRequiredCount> page = downloadListDao.getActionsRequiredFromDownloadList(
				createEntityActionRequiredCallback(userInfo), userInfo.getId(), pageToken.getLimitForQuery(),
				pageToken.getOffset());

		return new ActionRequiredResponse().setNextPageToken(pageToken.getNextPageTokenForCurrentResults(page))
				.setPage(page);
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
				createAccessCallback(userInfo), userInfo.getId(), availableRequest.getFilter(), sort,
				pageToken.getLimitForQuery(), pageToken.getOffset());

		return new AvailableFilesResponse().setNextPageToken(pageToken.getNextPageTokenForCurrentResults(page))
				.setPage(page);
	}

	/**
	 * Get the statistics for the files on the user's download list.
	 * 
	 * @param user
	 * @param statsResquest
	 * @return
	 */
	FilesStatisticsResponse getListStatistics(UserInfo user, FilesStatisticsRequest statsResquest) {
		validateUser(user);
		return downloadListDao.getListStatistics(createAccessCallback(user), user.getId());
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
			return batchInfo.stream().filter(e -> e.getAuthorizationStatus().isAuthorized()).map(e -> e.getEntityId())
					.collect(Collectors.toList());
		};
	}

	/**
	 * Create a callback to filter the entities that require action in order to be
	 * downloaded.
	 * 
	 * @param userInfo
	 * @return
	 */
	EntityActionRequiredCallback createEntityActionRequiredCallback(UserInfo userInfo) {
		return (List<Long> entityIds) -> entityAuthorizationManager.getActionsRequiredForDownload(userInfo, entityIds);
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

	@WriteTransaction
	@Override
	public AddToDownloadListResponse addToDownloadList(ProgressCallback progressCallback, UserInfo userInfo,
			AddToDownloadListRequest requestBody) {
		validateUser(userInfo);
		ValidateArgument.required(requestBody, "requestBody");
		if (requestBody.getParentId() != null && requestBody.getQuery() != null) {
			throw new IllegalArgumentException("Please provide request.parentId or request.query() but not both.");
		}
		boolean useVersionNumber = requestBody.getUseVersionNumber() == null ? DEFAULT_USE_VERSION
				: requestBody.getUseVersionNumber();
		long usersDownloadListCapacity = MAX_FILES_PER_USER
				- downloadListDao.getTotalNumberOfFilesOnDownloadList(userInfo.getId());
		if (usersDownloadListCapacity < 1) {
			throw new IllegalArgumentException(
					String.format(YOUR_DOWNLOAD_LIST_ALREADY_HAS_THE_MAXIMUM_NUMBER_OF_FILES, MAX_FILES_PER_USER));
		}
		if (requestBody.getQuery() != null) {
			return addQueryResultsToDownloadList(progressCallback, userInfo, requestBody.getQuery(), useVersionNumber,
					MAX_QUERY_PAGE_SIZE, usersDownloadListCapacity);
		} else if (requestBody.getParentId() != null) {
			return addToDownloadList(userInfo, requestBody.getParentId(), useVersionNumber, usersDownloadListCapacity);
		} else {
			throw new IllegalArgumentException("Must include either request.parentId or request.query().");
		}
	}

	/**
	 * Add all of the files from the given parentId to the user's download list.
	 * 
	 * @param userInfo
	 * @param parentId
	 * @param useVersion
	 * @return
	 */
	AddToDownloadListResponse addToDownloadList(UserInfo userInfo, String parentId, boolean useVersion, long limit) {
		entityAuthorizationManager.hasAccess(userInfo, parentId, ACCESS_TYPE.READ).checkAuthorizationOrElseThrow();
		Long parentIdKey = KeyFactory.stringToKey(parentId);
		if (nodeDao.getNodeTypeById(parentId).equals(EntityType.dataset)) {
			List<EntityRef> items = nodeDao.getNodeItems(parentIdKey);
			return new AddToDownloadListResponse().setNumberOfFilesAdded(this.downloadListDao
					.addDatasetItemsToDownloadList(userInfo.getId(), items, limit));
		} else {
			return new AddToDownloadListResponse().setNumberOfFilesAdded(this.downloadListDao
					.addChildrenToDownloadList(userInfo.getId(), parentIdKey, useVersion, limit));
		}
	}

	/**
	 * Add the files from the given view query to the user's download list.
	 * 
	 * @param progressCallback
	 * @param userInfo
	 * @param query
	 * @param useVersion
	 * @param maxLimit
	 * @return
	 */
	AddToDownloadListResponse addQueryResultsToDownloadList(final ProgressCallback progressCallback, UserInfo userInfo,
			final Query query, final boolean useVersion, long maxQueryPageSize, long usersDownloadListCapacity) {
		ValidateArgument.required(query, "query");
		ValidateArgument.required(query.getSql(), "query.sql");
		try {
			QuerySpecification model = TableQueryParser.parserQuery(query.getSql());
			
			TableAndColumnMapper tableAndColumnMapper = new TableAndColumnMapper(model, tableManagerSupport);
			
			Pair<SelectList, OrderByClause> selectFile;
			
			if (useVersion) {
				selectFile = tableAndColumnMapper.buildSelectAndOrderByFileAndVersionColumn(query.getSelectFileColumn(), query.getSelectFileVersionColumn());
			} else {
				selectFile = tableAndColumnMapper.buildSelectAndOrderByFileColumn(query.getSelectFileColumn());
			}
			
			model.replaceSelectList(selectFile.getFirst(), null);
			model.getTableExpression().replaceOrderBy(selectFile.getSecond());
			
			query.setSql(model.toSql());
			
			QueryOptions queryOptions = new QueryOptions()
				.withRunQuery(true);
			
			long totalFilesAdded = 0L;
			long limit = Math.min(usersDownloadListCapacity, maxQueryPageSize);
			long offset = 0L;
			List<DownloadListItem> batchToAdd = null;
			long batchSize = 0L;
			
			do {
				QueryResultBundle result = tableQueryManager.querySinglePage(progressCallback, userInfo, cloneQuery(query).setLimit(limit).setOffset(offset), queryOptions);
				
				batchToAdd = result.getQueryResult().getQueryResults().getRows().stream()
					.map((Row row) -> createDownloadsListItemFromRowValues(row.getValues(), useVersion))
					.collect(Collectors.toList());
				
				batchSize = (long) batchToAdd.size();
				
				// Filter non-file entities
				batchToAdd = downloadListDao.filterUnsupportedTypes(batchToAdd);
				
				long numberOfFilesAdded = downloadListDao.addBatchOfFilesToDownloadList(userInfo.getId(), batchToAdd);
				
				totalFilesAdded += numberOfFilesAdded;
				
				if (totalFilesAdded > usersDownloadListCapacity) {
					throw new IllegalArgumentException(String.format(ADDING_Q_FILES_EXCEEDS_LIMIT_TEMPLATE, MAX_FILES_PER_USER, (MAX_FILES_PER_USER - usersDownloadListCapacity)));
				}
				
				offset += limit;
			} while (batchSize >= limit);

			return new AddToDownloadListResponse().setNumberOfFilesAdded(totalFilesAdded);

		} catch (LockUnavilableException | TableUnavailableException e) {
			// can re-try when the view becomes available.
			throw new RecoverableMessageException();
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		} catch (DatastoreException | TableFailedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Helper to create a DownloadListItem from a query result row values list.
	 * @param rowValues
	 * @param includeVersion When true, the versionNumber for the download list item will be extracted from the given list of values and expected to be a Long.
	 * 
	 * @return
	 */
	DownloadListItem createDownloadsListItemFromRowValues(List<String> rowValues, final boolean includeVersion) {
		ValidateArgument.requiredNotEmpty(rowValues, "rowValues");
		
		DownloadListItem item = new DownloadListItem();
		
		item.setFileEntityId(rowValues.get(0));
		
		if (includeVersion) {
			ValidateArgument.requirement(rowValues.size() > 1, "Expected at least two elements in row values.");
			item.setVersionNumber(Long.valueOf(rowValues.get(1)));
		} else {
			item.setVersionNumber(null);
		}
		
		return item;
	}

	/**
	 * Create a deep copy of the given query.
	 * 
	 * @param query
	 * @return
	 */
	static Query cloneQuery(Query query) {
		try {
			return EntityFactory.createEntityFromJSONObject(EntityFactory.createJSONObjectForEntity(query),
					Query.class);
		} catch (JSONObjectAdapterException e) {
			throw new RuntimeException(e);
		}
	}

	@WriteTransaction
	@Override
	public DownloadListPackageResponse packageFiles(ProgressCallback progressCallback, UserInfo userInfo,
			DownloadListPackageRequest requestBody) throws IOException {
		validateUser(userInfo);
		AvailableFilter filter = AvailableFilter.eligibleForPackaging;
		// smallest files are added first
		List<Sort> sort = Arrays.asList(new Sort().setField(SortField.fileSize).setDirection(SortDirection.ASC));
		long limit = MAX_QUERY_PAGE_SIZE;
		long offset = 0L;

		List<DownloadListItemResult> page = downloadListDao.getFilesAvailableToDownloadFromDownloadList(
				createAccessCallback(userInfo), userInfo.getId(), filter, sort, limit, offset);

		List<DownloadListItemResult> packagedFiles = new ArrayList<>(page.size());
		List<FileHandleAssociation> associations = new ArrayList<>(page.size());
		Set<String> addedFileHandleIds = new HashSet<>(page.size());
		long size = 0L;
		for (DownloadListItemResult item : page) {
			if (size + item.getFileSizeBytes() > FileConstants.BULK_FILE_DOWNLOAD_MAX_SIZE_BYTES) {
				break;
			}
			if (addedFileHandleIds.add(item.getFileEntityId())) {
				size += item.getFileSizeBytes();
				associations.add(createAssociationForItem(item));
			}
			packagedFiles.add(item);
		}
		if (Boolean.TRUE.equals(requestBody.getIncludeManifest())) {
			String manifestFileHandleId = buildManifest(userInfo, requestBody.getCsvTableDescriptor(),
					packagedFiles.iterator());
			associations.add(new FileHandleAssociation().setFileHandleId(manifestFileHandleId)
					.setAssociateObjectType(FileHandleAssociateType.FileEntity).setAssociateObjectId(ZERO_FILE_ID));
		}

		if (associations.isEmpty()) {
			throw new IllegalArgumentException(NO_FILES_ARE_ELIGIBLE_FOR_PACKAGING);
		}

		// build the package zip file.
		// @formatter:off
		boolean skipFileSizeCheck = true;
		String zipFileHandleId = fileHandlePackageManager.buildZip(userInfo,
						new BulkFileDownloadRequest()
						.setRequestedFiles(associations)
						.setZipFileName(requestBody.getZipFileName())
						.setZipFileFormat(ZipFileFormat.Flat), skipFileSizeCheck)
				.getResultZipFileHandleId();
		// @formatter:on

		// remove these files from the download list
		downloadListDao.removeBatchOfFilesFromDownloadList(userInfo.getId(), packagedFiles);
		return new DownloadListPackageResponse().setResultFileHandleId(zipFileHandleId);
	}

	/**
	 * Create a FileHandleAssociation from the given DownloadListItemResult.
	 * 
	 * @param item
	 * @return
	 */
	public static FileHandleAssociation createAssociationForItem(DownloadListItemResult item) {
		return new FileHandleAssociation().setAssociateObjectId(item.getFileEntityId())
				.setAssociateObjectType(FileHandleAssociateType.FileEntity).setFileHandleId(item.getFileHandleId());
	}

	@Override
	public DownloadListManifestResponse createManifest(ProgressCallback progressCallback, UserInfo userInfo,
			DownloadListManifestRequest request) throws IOException {
		validateUser(userInfo);
		AvailableFilter filter = null;
		List<Sort> sort = Arrays.asList(new Sort().setField(SortField.fileName).setDirection(SortDirection.ASC));
		PaginationIterator<DownloadListItemResult> itertor = new PaginationIterator<>(
				(limit, offset) -> downloadListDao.getFilesAvailableToDownloadFromDownloadList(
						createAccessCallback(userInfo), userInfo.getId(), filter, sort, limit, offset),
				MAX_QUERY_PAGE_SIZE);
		String manifestFileHandleId = buildManifest(userInfo, request.getCsvTableDescriptor(), itertor);
		return new DownloadListManifestResponse().setResultFileHandleId(manifestFileHandleId);
	}

	/**
	 * Build a manifest CSV that includes a row for each provided DownloadListItemResult 
	 * @param user
	 * @param descriptor
	 * @param iterator
	 * @return The fileHandle ID of the the resulting CSV.
	 * @throws IOException
	 */
	String buildManifest(UserInfo user, CsvTableDescriptor descriptor, Iterator<DownloadListItemResult> iterator)
			throws IOException {
		// Write all of the data to a text file, and gather the unique annotation names to determine the columns of the CSV.
		return fileProvider.createTemporaryFile("items", ".txt", temp -> {
			Set<String> annotationKeys = new HashSet<>();
			Set<String> defaultKeys = Stream.of(ManifestKeys.values()).map(k->k.name()).collect(Collectors.toSet());
			try (BufferedWriter writer = fileProvider.createBufferedWriter(temp,StandardCharsets.UTF_8)) {
				int count = 0;
				while (iterator.hasNext()) {
					DownloadListItemResult item = iterator.next();
					JSONObject details = downloadListDao.getItemManifestDetails(item);
					// Capture any new annotation keys (excluding all default keys).
					annotationKeys.addAll(Sets.difference(details.keySet(), defaultKeys));
					writer.append(details.toString());
					writer.newLine();
					count++;
				}
				if(count < 1) {
					throw new IllegalArgumentException(NO_FILES_AVAILABLE_FOR_DOWNLOAD);
				}
			}
			LinkedHashMap<String, Integer> keyToIndexMap = mapKeysToColumnIndex(annotationKeys);
			// build the manifest from the text file.
			return buildManifestCSV(user, descriptor, temp, keyToIndexMap);
		});
	}
	
	
	/**
	 * Build a map of the column names to the final column index in the CSV.
	 * @param annotationNames
	 * @return
	 */
	LinkedHashMap<String, Integer> mapKeysToColumnIndex(Set<String> annotationNames){
		LinkedHashMap<String, Integer> map = new LinkedHashMap<>(annotationNames.size());
		// the first columns are always the default manifest keys
		int index = 0;
		for(ManifestKeys defaultKey: ManifestKeys.values()) {
			map.put(defaultKey.name(), index);
			index++;
		}
		// next add the sorted annotation keys 
		List<String> sortedAnnotationNames = annotationNames.stream().sorted().collect(Collectors.toList());
		for(String annoKey: sortedAnnotationNames) {
			map.put(annoKey, index);
			index++;
		}
		return map;
	}
	
	/**
	 * Build a CSV manifest file from the provided JSON text file and upload the results to S3
	 * @param user
	 * @param descriptor
	 * @param tempTextFile
	 * @param keyToColumnIndex
	 * @return
	 * @throws IOException
	 */
	String buildManifestCSV(UserInfo user, CsvTableDescriptor descriptor, File tempTextFile, LinkedHashMap<String, Integer> keyToColumnIndex)
			throws IOException {
		final String seporator = descriptor == null ? null : descriptor.getSeparator();
		final boolean includeHeader =  descriptor == null || descriptor.getIsFirstLineHeader() == null ? true : descriptor.getIsFirstLineHeader();
		String fileExtention = CSVUtils.guessExtension(seporator);
		return fileProvider.createTemporaryFile("manifest", "." + fileExtention, tempCSV -> {
			try(BufferedReader textReader = fileProvider.createBufferedReader(tempTextFile, StandardCharsets.UTF_8)){
				try (CSVWriter writer = createCSVWriter(descriptor, tempCSV)) {
					copyFromTextToCSV(keyToColumnIndex, textReader, writer, includeHeader);
				}
				String contentType = CSVUtils.guessContentType(seporator);
				return fileHandleManager
						.uploadLocalFile(new LocalFileUploadRequest().withContentType(contentType)
								.withFileName("manifest."+fileExtention).withFileToUpload(tempCSV).withUserId(user.getId().toString()))
						.getId();
			}

		});
	}
	
	/**
	 * Create a CSVWriter for the given descriptor and file.
	 * @param descriptor
	 * @param temp
	 * @return
	 * @throws IOException
	 */
	CSVWriter createCSVWriter(CsvTableDescriptor descriptor, File temp) throws IOException {
		return CSVUtils.createCSVWriter(
				fileProvider.createWriter(fileProvider.createFileOutputStream(temp), StandardCharsets.UTF_8),
				descriptor);
	}

	/**
	 * Copy all of the JSON data from the given reader into the given CSV writer.
	 * 
	 * @param keyToColumnIndex
	 * @param textReader
	 * @param writer
	 * @throws IOException
	 */
	void copyFromTextToCSV(LinkedHashMap<String, Integer> keyToColumnIndex, BufferedReader textReader, CSVWriter writer, boolean includeHeader)
			throws IOException {
		int index = 0;
		if(includeHeader) {
			// Write the header
			String[] header = new String[keyToColumnIndex.size()];
			for(String key: keyToColumnIndex.keySet()) {
				header[index] = key;
				index++;
			}
			writer.writeNext(header);
		}

		String line = null;
		while((line = textReader.readLine()) != null) {
			JSONObject itemJson = new JSONObject(line);
			String[] row = new String[keyToColumnIndex.size()];
			for(String key: keyToColumnIndex.keySet()) {
				index = keyToColumnIndex.get(key);
				if(itemJson.has(key)) {
					row[index] = itemJson.getString(key);
				}
			}
			writer.writeNext(row);
		}
	}
	
}
