package org.sagebionetworks.repo.manager.download;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONObject;
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
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.download.v2.DownloadListDAO;
import org.sagebionetworks.repo.model.dbo.file.download.v2.EntityAccessCallback;
import org.sagebionetworks.repo.model.dbo.file.download.v2.EntityActionRequiredCallback;
import org.sagebionetworks.repo.model.dbo.file.download.v2.FileAndSize;
import org.sagebionetworks.repo.model.dbo.file.download.v2.ManifestKeys;
import org.sagebionetworks.repo.model.download.ActionRequiredCount;
import org.sagebionetworks.repo.model.download.ActionRequiredRequest;
import org.sagebionetworks.repo.model.download.ActionRequiredResponse;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddBatchOfFilesToDownloadListResponse;
import org.sagebionetworks.repo.model.download.AddToDownloadListRequest;
import org.sagebionetworks.repo.model.download.AddToDownloadListResponse;
import org.sagebionetworks.repo.model.download.AddToDownloadListStatsRequest;
import org.sagebionetworks.repo.model.download.AddToDownloadListStatsResponse;
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
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.file.FileConstants;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.ZipFileFormat;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;
import org.sagebionetworks.repo.model.table.Query;
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
import org.sagebionetworks.util.csv.CSVWriterProvider;
import org.sagebionetworks.util.progress.ProgressCallback;
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
	
	/**
	 * The max number of container to use when computing file stats for descendants or dataset collections
	 */
	public static final int FILE_STATS_MAX_CONTAINERS_COUNT = 500;

	private EntityAuthorizationManager entityAuthorizationManager;
	private DownloadListDAO downloadListDao;
	private TableQueryManager tableQueryManager;
	private TableManagerSupport tableManagerSupport;
	private FileHandlePackageManager fileHandlePackageManager;
	private FileHandleManager fileHandleManager;
	private FileProvider fileProvider;
	private NodeDAO nodeDao;
    private CSVWriterProvider csvWriterProvider;

	@Autowired
	public DownloadListManagerImpl(EntityAuthorizationManager entityAuthorizationManager,
			DownloadListDAO downloadListDao, TableQueryManager tableQueryManager, TableManagerSupport tableManagerSupport,
			FileHandlePackageManager fileHandlePackageManager, FileHandleManager fileHandleManager, FileProvider fileProvider,
			NodeDAO nodeDao, CSVWriterProvider csvWriterProvider) {
		super();
		this.entityAuthorizationManager = entityAuthorizationManager;
		this.downloadListDao = downloadListDao;
		this.tableQueryManager = tableQueryManager;
		this.tableManagerSupport = tableManagerSupport;
		this.fileHandlePackageManager = fileHandlePackageManager;
		this.fileHandleManager = fileHandleManager;
		this.fileProvider = fileProvider;
		this.nodeDao = nodeDao;
        this.csvWriterProvider = csvWriterProvider;
	}

	@WriteTransaction
	@Override
	public AddBatchOfFilesToDownloadListResponse addBatchOfFilesToDownloadList(UserInfo userInfo,
			AddBatchOfFilesToDownloadListRequest toAdd) {
		validateUser(userInfo);
		ValidateArgument.required(toAdd, "toAdd");
		validateBatch(toAdd.getBatchToAdd());
		// filter out all non-files from the input
		List<DownloadListItem> filteredBatch = downloadListDao.filterItemsByType(toAdd.getBatchToAdd(), EntityTypeUtils.getFileTypes());
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
	 * @param batch
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
		if (userInfo.isUserAnonymous()) {
			throw new UnauthorizedException(YOU_MUST_LOGIN_TO_ACCESS_YOUR_DOWNLOAD_LIST);
		}
	}

	@WriteTransaction
	@Override
	public AddToDownloadListResponse addToDownloadList(ProgressCallback progressCallback, UserInfo userInfo, AddToDownloadListRequest requestBody) {
		validateUser(userInfo);
		validateAddToDownloadListRequest(requestBody);
		
		long usersDownloadListCapacity = MAX_FILES_PER_USER - downloadListDao.getTotalNumberOfFilesOnDownloadList(userInfo.getId());
		
		if (usersDownloadListCapacity < 1) {
			throw new IllegalArgumentException(String.format(YOUR_DOWNLOAD_LIST_ALREADY_HAS_THE_MAXIMUM_NUMBER_OF_FILES, MAX_FILES_PER_USER));
		}
		
		if (requestBody.getQuery() != null) {
			return addQueryResultsToDownloadList(progressCallback, userInfo, requestBody.getQuery(), requestBody.getUseVersionNumber(), usersDownloadListCapacity);
		} else {
			return addToDownloadList(userInfo, requestBody.getParentId(), requestBody.getUseVersionNumber(), requestBody.getRecursive(), usersDownloadListCapacity);
		}
	}
	
	@Override
	public AddToDownloadListStatsResponse getAddToDownloadListStats(ProgressCallback callback, UserInfo userInfo, AddToDownloadListStatsRequest requestBody) {
		validateUser(userInfo);
		ValidateArgument.required(requestBody, "requestBody");
		ValidateArgument.required(requestBody.getRequest(), "requestBody.request");
		
		validateAddToDownloadListRequest(requestBody.getRequest());
		
		if (requestBody.getRequest().getQuery() != null) {
			return getAddToDownLoadListStatsFromQuery(callback, userInfo, requestBody.getRequest().getQuery(),
					requestBody.getRequest().getUseVersionNumber());
		} else {
			return getAddToDownLoadListStatsFromParentId(userInfo, requestBody.getRequest().getParentId(), requestBody.getRequest().getRecursive());
		}
	}
	
	private void validateAddToDownloadListRequest(AddToDownloadListRequest requestBody) {
	
		ValidateArgument.required(requestBody, "requestBody");
		
		if (requestBody.getParentId() != null && requestBody.getQuery() != null) {
			throw new IllegalArgumentException("Please provide request.parentId or request.query() but not both.");
		}
		
		if (requestBody.getParentId() == null && requestBody.getQuery() == null) {
			throw new IllegalArgumentException("Must include either request.parentId or request.query().");
		}
		
		boolean recursive = Boolean.TRUE.equals(requestBody.getRecursive());
				
		if (recursive && requestBody.getParentId() == null) {
			throw new IllegalArgumentException("The recursive option is only supported when specifying a parentId.");
		}
		
		boolean useVersionNumber = requestBody.getUseVersionNumber() == null ? DEFAULT_USE_VERSION : requestBody.getUseVersionNumber();
		
		requestBody.setRecursive(recursive);
		requestBody.setUseVersionNumber(useVersionNumber);
	}	
	
	/**
	 * Add all of the files from the given parentId to the user's download list.
	 * 
	 * @param userInfo
	 * @param parentId
	 * @param useVersion
	 * @return
	 */
	AddToDownloadListResponse addToDownloadList(UserInfo userInfo, String parentId, boolean useVersion, boolean recursive, long limit) {
		
		Long parentIdKey = validateAddToDownloadListFromParentId(userInfo, parentId);
		
		Long numberOfFilesAdded;
		
		EntityType parentType = nodeDao.getNodeTypeById(parentId);
		
		if (EntityType.dataset.equals(parentType)) {
			ValidateArgument.requirement(!recursive, "The recursive option is not supported for a dataset.");
			List<EntityRef> files = nodeDao.getNodeItems(parentIdKey);
			numberOfFilesAdded = this.downloadListDao.addFileEntityRefToDownloadList(userInfo.getId(), files, limit);
		} else if (EntityType.datasetcollection.equals(parentType)) {
			List<EntityRef> datasets = nodeDao.getNodeItems(parentIdKey);
			numberOfFilesAdded = this.downloadListDao.addDatasetEntityRefFilesToDownloadList(userInfo.getId(), datasets, limit);
		} else if (recursive) {
			numberOfFilesAdded = this.downloadListDao.addDescendantsToDownloadList(userInfo.getId(), parentIdKey, useVersion, limit);
		} else {
			numberOfFilesAdded = this.downloadListDao.addChildrenToDownloadList(userInfo.getId(), parentIdKey, useVersion, limit);
		}
		
		return new AddToDownloadListResponse().setNumberOfFilesAdded(numberOfFilesAdded);
	}
	
	AddToDownloadListStatsResponse getAddToDownLoadListStatsFromParentId(UserInfo userInfo, String parentId, boolean recursive) {
		Long parentIdKey = validateAddToDownloadListFromParentId(userInfo, parentId);

		EntityType parentType = nodeDao.getNodeTypeById(parentId);
		
		if (EntityType.dataset.equals(parentType)) {
			List<EntityRef> files = nodeDao.getNodeItems(parentIdKey);
			return this.downloadListDao.getAddFileEntityRefToDownloadListStats(files);
		} else if (EntityType.datasetcollection.equals(parentType)) {
			List<EntityRef> datasets = nodeDao.getNodeItems(parentIdKey);
			
			boolean isEstimate = false;
			
			if (datasets.size() > FILE_STATS_MAX_CONTAINERS_COUNT) {
				datasets = datasets.subList(0, FILE_STATS_MAX_CONTAINERS_COUNT);
				isEstimate = true;
			}
			
			return this.downloadListDao.getAddDatasetEntityRefFilesToDownloadListStats(datasets)
				.setIsFileCountAndSizeEstimate(isEstimate);
			
		} else if (recursive) {
			return this.downloadListDao.getAddDescendantsToDownloadListStats(parentIdKey, FILE_STATS_MAX_CONTAINERS_COUNT);
		} else {
			return this.downloadListDao.getAddChildrenToDownloadListStats(parentIdKey);
		}
	}
	
	private Long validateAddToDownloadListFromParentId(UserInfo user, String parentId) {
		Long parentIdKey = KeyFactory.stringToKey(parentId);
		
		ValidateArgument.requirement(!NodeConstants.BOOTSTRAP_NODES.getAllBootstrapIds().contains(parentIdKey), "Invalid parentId.");
		
		entityAuthorizationManager.hasAccess(user, parentId, ACCESS_TYPE.READ).checkAuthorizationOrElseThrow();
		
		return parentIdKey;
	}

	/**
	 * Add the files from the given view query to the user's download list, including the files
	 * contained in any Dataset rows of the result. A file reached more than once at the version it
	 * refers to, for example as its own row and as a Dataset member, is added once. Throws if the files
	 * would exceed the user's remaining download list capacity, in which case nothing is added.
	 *
	 * @param progressCallback
	 * @param userInfo
	 * @param query
	 * @param useVersion
	 * @param usersDownloadListCapacity
	 * @return
	 */
	AddToDownloadListResponse addQueryResultsToDownloadList(final ProgressCallback progressCallback, UserInfo userInfo,
			final Query query, final boolean useVersion, long usersDownloadListCapacity) {
		ValidateArgument.required(query, "query");
		ValidateArgument.required(query.getSql(), "query.sql");
		try {
			rewriteQueryToSelectFileColumn(query, useVersion);

			long[] totalFilesAdded = { 0L };
			// A row without a version and a Dataset member at the file's current version are stored under
			// different keys, so the versions already added are tracked across batches to add each one once.
			Set<IdAndVersion> filesAdded = new HashSet<>();
			streamQueryResult(progressCallback, userInfo, query, useVersion, batch -> {
				// +1 so that Dataset files overfilling the capacity are detected by the check below rather than
				// silently truncated. Best effort: files already on the list are not counted as inserted.
				long maxDatasetFiles = usersDownloadListCapacity - totalFilesAdded[0] + 1;
				List<DownloadListItem> files = getFilesInBatch(batch, useVersion, maxDatasetFiles).stream()
						.filter(file -> filesAdded.add(toFileRef(file)))
						.map(FileAndSize::item).collect(Collectors.toList());
				totalFilesAdded[0] += downloadListDao.addBatchOfFilesToDownloadList(userInfo.getId(), files);
				checkQueryCapacity(totalFilesAdded[0], usersDownloadListCapacity);
			});

			return new AddToDownloadListResponse().setNumberOfFilesAdded(totalFilesAdded[0]);

		} catch (LockUnavilableException | TableUnavailableException e) {
			// can re-try when the view becomes available.
			throw new RecoverableMessageException();
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		} catch (DatastoreException | TableFailedException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Rewrite the given query's SQL in place so its select list is just the file entity id (and, when
	 * useVersion is true, its version), ordered the same way. This is the transformation shared by the
	 * add and stats query paths, so that both resolve rows through an identical rewrite and cannot
	 * drift apart.
	 *
	 * @param query
	 * @param useVersion
	 * @throws ParseException
	 */
	void rewriteQueryToSelectFileColumn(Query query, boolean useVersion) throws ParseException {
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
	}

	/**
	 * Run the entire given, already {@link #rewriteQueryToSelectFileColumn(Query, boolean) rewritten}
	 * query as a single stream, handing its rows to the given consumer in batches of at most
	 * {@link #MAX_QUERY_PAGE_SIZE}, in query order. Shared by the add and stats query paths so both read
	 * rows identically.
	 *
	 * @param progressCallback
	 * @param userInfo
	 * @param query
	 * @param useVersion
	 * @param batchConsumer    Called with one item per row, of any entity type.
	 */
	void streamQueryResult(ProgressCallback progressCallback, UserInfo userInfo, Query query, boolean useVersion,
			Consumer<List<DownloadListItem>> batchConsumer)
			throws DatastoreException, TableUnavailableException, TableFailedException, LockUnavilableException, IOException {
		// The stream covers the whole result, so any limit or offset of the request is not applied.
		Query streamQuery = cloneQuery(query).setLimit(null).setOffset(null);

		List<DownloadListItem> batch = new ArrayList<>();
		tableQueryManager.runQueryAsStream(progressCallback, userInfo, streamQuery, translations -> (Row row) -> {
			batch.add(createDownloadsListItemFromRowValues(row.getValues(), useVersion));
			if (batch.size() >= MAX_QUERY_PAGE_SIZE) {
				batchConsumer.accept(new ArrayList<>(batch));
				batch.clear();
			}
		});
		// Flushed only once the stream completes, so that a failed stream hands on no partial batch.
		if (!batch.isEmpty()) {
			batchConsumer.accept(batch);
		}
	}

	/**
	 * Resolve a batch of query result rows into the files that adding them would add: each file row as
	 * it is, and each Dataset row expanded into the files it contains. Rows of any other type are
	 * ignored. Shared by the add and stats query paths so that both see the same files.
	 *
	 * @param batch
	 * @param useVersion
	 * @param maxDatasetFiles The maximum number of files to take from the batch's Dataset rows.
	 * @return The files with their sizes. A file that is both its own row and a Dataset member, or a
	 *         member of more than one Dataset row, can be included more than once.
	 */
	List<FileAndSize> getFilesInBatch(List<DownloadListItem> batch, boolean useVersion, long maxDatasetFiles) {
		List<FileAndSize> files = new ArrayList<>(downloadListDao.getFilesWithSize(batch, useVersion));
		List<EntityRef> datasetRefs = toEntityRefs(
				downloadListDao.filterItemsByType(batch, EntityTypeUtils.getFileCollectionTypes()), useVersion);
		if (!datasetRefs.isEmpty()) {
			files.addAll(downloadListDao.getFilesInDatasets(datasetRefs, maxDatasetFiles));
		}
		return files;
	}

	/**
	 * Throw if the number of files added so far has exceeded the user's remaining download list
	 * capacity.
	 *
	 * @param totalFilesAdded
	 * @param usersDownloadListCapacity
	 */
	static void checkQueryCapacity(long totalFilesAdded, long usersDownloadListCapacity) {
		if (totalFilesAdded > usersDownloadListCapacity) {
			throw new IllegalArgumentException(String.format(ADDING_Q_FILES_EXCEEDS_LIMIT_TEMPLATE, MAX_FILES_PER_USER,
					(MAX_FILES_PER_USER - usersDownloadListCapacity)));
		}
	}

	/**
	 * Convert rows from a query result into the entity id and version pairs the DAO matches on. When
	 * the query did not select a version, the current version of each entity is resolved in bulk, and
	 * an entity whose current version cannot be resolved is omitted.
	 *
	 * @param items
	 * @param useVersion
	 * @return
	 */
	List<EntityRef> toEntityRefs(List<DownloadListItem> items, boolean useVersion) {
		if (items.isEmpty()) {
			return Collections.emptyList();
		}
		if (useVersion) {
			return items.stream()
					.map(item -> new EntityRef().setEntityId(item.getFileEntityId()).setVersionNumber(item.getVersionNumber()))
					.collect(Collectors.toList());
		}
		List<String> ids = items.stream().map(DownloadListItem::getFileEntityId).collect(Collectors.toList());
		return nodeDao.getCurrentRevisionNumbers(ids).stream()
				.filter(ref -> ref.getTargetVersionNumber() != null)
				.map((Reference ref) -> new EntityRef().setEntityId(ref.getTargetId()).setVersionNumber(ref.getTargetVersionNumber()))
				.collect(Collectors.toList());
	}

	/**
	 * Compute the number and total size of the distinct files that adding the given query's results
	 * would add, including the files contained in any Dataset rows of the result. The result is flagged
	 * as an estimate when there are more files than {@link #MAX_FILES_PER_USER}, in which case the files
	 * past that point are not counted.
	 *
	 * @param progressCallback
	 * @param userInfo
	 * @param query
	 * @param useVersion
	 * @return
	 */
	AddToDownloadListStatsResponse getAddToDownLoadListStatsFromQuery(ProgressCallback progressCallback, UserInfo userInfo,
			Query query, boolean useVersion) {
		ValidateArgument.required(query, "query");

		try {
			rewriteQueryToSelectFileColumn(query, useVersion);

			// The add stores each version of a file once, including a file that is both a row without a version
			// and a Dataset member at its current version. Keying the sizes on the same file refs collapses it too.
			Map<IdAndVersion, Long> fileSizes = new HashMap<>();
			streamQueryResult(progressCallback, userInfo, query, useVersion, batch -> {
				// No download list can hold more files than this, so one more is enough to show that the add
				// would fail, and nothing past it needs to be looked up or held in memory.
				if (fileSizes.size() > MAX_FILES_PER_USER) {
					return;
				}
				long maxDatasetFiles = MAX_FILES_PER_USER + 1 - fileSizes.size();
				for (FileAndSize file : getFilesInBatch(batch, useVersion, maxDatasetFiles)) {
					fileSizes.putIfAbsent(toFileRef(file), file.sizeBytes());
				}
			});

			return new AddToDownloadListStatsResponse()
				.setFileCount((long) fileSizes.size())
				.setFileSize(fileSizes.values().stream().mapToLong(Long::longValue).sum())
				.setIsFileCountAndSizeEstimate(fileSizes.size() > MAX_FILES_PER_USER);

		} catch (LockUnavilableException | TableUnavailableException e) {
			// can re-try when the view becomes available.
			throw new RecoverableMessageException();
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		} catch (DatastoreException | TableFailedException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * The (id, version) of the version the given file refers to, the same whether the file is stored
	 * against that specific version or against its current version.
	 *
	 * @param file
	 * @return
	 */
	static IdAndVersion toFileRef(FileAndSize file) {
		return IdAndVersion.newBuilder().setId(KeyFactory.stringToKey(file.item().getFileEntityId()))
				.setVersion(file.actualVersion()).build();
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
		return csvWriterProvider.createWriter(
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
