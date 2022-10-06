package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.dao.table.TableType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.FacetColumnResult;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.SumFileSizes;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.cluster.SqlQueryBuilder;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.description.BenefactorDescription;
import org.sagebionetworks.table.cluster.description.IndexDescription;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.Pagination;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.TextMatchesPredicate;
import org.sagebionetworks.table.query.model.WhereClause;
import org.sagebionetworks.table.query.util.SimpleAggregateQueryException;
import org.sagebionetworks.table.query.util.SqlElementUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.csv.CSVWriterStream;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TableQueryManagerImpl implements TableQueryManager {

	public static final long MAX_ROWS_PER_CALL = 100;

	@Autowired
	TableManagerSupport tableManagerSupport;
	@Autowired
	ConnectionFactory tableConnectionFactory;

	/**
	 * Injected via spring
	 */
	long maxBytesPerRequest;

	public void setMaxBytesPerRequest(long maxBytesPerRequest) {
		this.maxBytesPerRequest = maxBytesPerRequest;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.table.TableQueryManager#querySinglePage
	 * (org.sagebionetworks.common.util.progress.ProgressCallback,
	 * org.sagebionetworks.repo.model.UserInfo, java.lang.String, java.util.List,
	 * java.lang.Long, java.lang.Long, boolean, boolean, boolean)
	 */
	@Override
	public QueryResultBundle querySinglePage(ProgressCallback progressCallback, UserInfo user, Query query, QueryOptions options)
			throws TableUnavailableException, TableFailedException, LockUnavilableException {
		try {
			// Set the default values
			TableQueryManagerImpl.setDefaultsValues(query);
			// handler will capture the results of the query.
			SinglePageRowHandler rowHandler = null;
			if (options.runQuery()) {
				rowHandler = new SinglePageRowHandler();
			}

			//get combined sql before pre-flight and authorization
			String combinedSql = null;
			if (options.returnCombinedSql()) {
				combinedSql = createCombinedSql(user, query);
			}
			// pre-flight includes parsing and authorization
			SqlQuery sqlQuery = queryPreflight(user, query, this.maxBytesPerRequest);
			
			// run the query as a stream.
			QueryResultBundle bundle = queryAsStream(progressCallback, user, sqlQuery, rowHandler, options);
			// add combined sql to the bundle
			bundle.setCombinedSql(combinedSql);
			// save the max rows per page.
			if(options.returnMaxRowsPerPage()) {
				bundle.setMaxRowsPerPage(sqlQuery.getMaxRowsPerPage());
			}
			// add captured rows to the bundle
			if (options.runQuery()) {
				bundle.getQueryResult().getQueryResults().setRows(rowHandler.getRows());
			}
			int maxRowsPerPage = sqlQuery.getMaxRowsPerPage().intValue();
			// add the next page token if needed
			if (isRowCountEqualToMaxRowsPerPage(bundle, maxRowsPerPage)) {
				long nextOffset = (query.getOffset() == null ? 0 : query.getOffset()) + maxRowsPerPage;
				QueryNextPageToken nextPageToken = TableQueryUtils.createNextPageToken(query.getSql(), query.getSort(),
						nextOffset, query.getLimit(), query.getSelectedFacets());
				bundle.getQueryResult().setNextPageToken(nextPageToken);
			}
			return bundle;
		} catch (EmptyResultException e) {
			// return an empty result.
			return createEmptyBundle(e.getTableId(), options);
		}

	}

	/**
	 * Combined sql query is basic input query combined with all additional filter.
	 * createCombinedSql includes the following:
	 * <ol>
	 * <li>Parse the query SQL string, and identify the tableId.</li>
	 * <li>Build SqlQuery with additional filter, offset, limit and sortList.</li>
	 * <li>Add selected facet to query</li>
	 * <li>Parse again the combined sql to replace selectList with original select list</li>
	 * </ol>
	 *
	 * @param user
	 * @param query
	 * @return
	 *
	 */
	String createCombinedSql(UserInfo user, Query query) {
		QuerySpecification model = parserQuery(query.getSql());
		// SqlQuery changes the selectList of query so keep the original selected list.
		SelectList selectListFromUser = model.getSelectList();
		// We now have the table's ID.
		String tableId = model.getSingleTableName().orElseThrow(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEXT);
		IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
		IndexDescription indexDescription = tableManagerSupport.getIndexDescription(idAndVersion);
		SqlQuery combinedSql = new SqlQueryBuilder(model, user.getId())
				.schemaProvider(tableManagerSupport)
				.overrideOffset(query.getOffset())
				.overrideLimit(query.getLimit())
				.indexDescription(indexDescription)
				.selectedFacets(query.getSelectedFacets())
				.sortList(query.getSort())
				.additionalFilters(query.getAdditionalFilters()).build();

		//SqlQuery does not include facetModel, So add selected facet to query.
		FacetModel facetModel = new FacetModel(query.getSelectedFacets(), combinedSql, query.getSelectedFacets() != null);

		// determine whether to run with facet filters
		if (facetModel.hasFiltersApplied()) {
			combinedSql = facetModel.getFacetFilteredQuery();
		}

		// parse again the query to replace selectList with original select list.
		QuerySpecification finalModel = parserQuery(combinedSql.getCombinedSQL());
		finalModel.getSelectList().replaceElement(selectListFromUser);
		return finalModel.toSql();
	}

	/**
	 * Query pre-flight includes the following:
	 * <ol>
	 * <li>Parse the query SQL string, and identify the tableId.</li>
	 * <li>Authenticate that the user has read access on the table.</li>
	 * <li>Gather table's schema information</li>
	 * <li>Add row level filtering as needed.</li>
	 * <li>Create processed {@link SqlQuery} that is ready for execution.</li>
	 * </ol>
	 * a
	 * 
	 * @param user
	 * @param query
	 * @return
	 * @throws EmptyResultException
	 * @throws TableFailedException
	 * @throws TableUnavailableException
	 * @throws NotFoundException
	 */
	SqlQuery queryPreflight(UserInfo user, Query query, Long maxBytesPerPage)
			throws EmptyResultException, NotFoundException, TableUnavailableException, TableFailedException {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(query, "Query");
		ValidateArgument.required(query.getSql(), "Query");
		// 1. Parse the SQL string

		QuerySpecification model = parserQuery(query.getSql());
		// We now have the table's ID.
		String tableId = model.getSingleTableName().orElseThrow(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEXT);
		IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
		IndexDescription indexDescription = tableManagerSupport.getIndexDescription(idAndVersion);
		// 2. Validate the user has read access on this table
		tableManagerSupport.validateTableReadAccess(user, indexDescription);

		// 3. Get the table's schema count
		long count = tableManagerSupport.getTableSchemaCount(idAndVersion);
		if (count < 1L) {
			throw new EmptyResultException("Table schema is empty for: " + tableId, tableId);
		}

		// 4. Add row level filter as needed.
		// Table views must have a row level filter applied to the query
		model = addRowLevelFilter(user, model, indexDescription);
		// Return the prepared query.
		return new SqlQueryBuilder(model, user.getId()).schemaProvider(tableManagerSupport).overrideOffset(query.getOffset())
				.overrideLimit(query.getLimit()).maxBytesPerPage(maxBytesPerPage)
				.includeEntityEtag(query.getIncludeEntityEtag())
				.indexDescription(indexDescription).selectedFacets(query.getSelectedFacets())
				.sortList(query.getSort()).additionalFilters(query.getAdditionalFilters()).build();
	}

	/**
	 * The main entry point for all table queries. Any business logic that must be
	 * applied to all table queries should applied here or lower.
	 * 
	 * @param progressCallback
	 * @param user
	 * @param query
	 * @param offset
	 * @param limit
	 * @param runQuery
	 * @param runCount
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws TableUnavailableException
	 * @throws TableFailedException
	 * @throws EmptyResultException
	 * @throws TableLockUnavailableException
	 */
	QueryResultBundle queryAsStream(final ProgressCallback progressCallback, final UserInfo user, final SqlQuery query,
			final RowHandler rowHandler, final QueryOptions options)
			throws DatastoreException, NotFoundException, TableUnavailableException, TableFailedException,
			LockUnavilableException, EmptyResultException {
		// run with a read lock on the table and include the current etag.
		IdAndVersion idAndVersion = IdAndVersion.parse(query.getSingleTableId().orElseThrow(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEXT));
		return tryRunWithTableReadLock(progressCallback, idAndVersion, (ProgressCallback callback) -> {
					// We can only run this query if the table is available.
					final TableStatus status = validateTableIsAvailable(idAndVersion.toString());
					// run the query
					QueryResultBundle bundle = queryAsStreamAfterAuthorization(progressCallback, query,
							rowHandler, options);
					// add the status to the result
					if (rowHandler != null) {
						// the etag is only returned for consistent queries.
						bundle.getQueryResult().getQueryResults().setEtag(status.getLastTableChangeEtag());
					}
					return bundle;
				});
	}

	/**
	 * Run the passed runner while holding the table's read lock.
	 * 
	 * @param callback
	 * @param tableId
	 * @param runner
	 * @return
	 * @throws TableUnavailableException
	 * @throws TableFailedException
	 * @throws EmptyResultException
	 */
	<R, T> R tryRunWithTableReadLock(ProgressCallback callback, IdAndVersion idAndversion, ProgressingCallable<R> runner)
			throws TableUnavailableException, TableFailedException, EmptyResultException {

		try {
			return tableManagerSupport.tryRunWithTableNonExclusiveLock(callback, runner,
					idAndversion);
		} catch (RuntimeException | TableUnavailableException | EmptyResultException | TableFailedException e) {
			// runtime exceptions are unchanged.
			throw e;
		} catch (Exception e) {
			// all other checked exceptions are converted to runtime
			throw new RuntimeException(e);
		}
	}

	/**
	 * Run a query as a stream after all authorization checks have been performed
	 * and any any required row-level filtering has been applied.
	 * 
	 * @param progressCallback
	 * @param user
	 * @param query
	 * @param offset
	 * @param limit
	 * @param runQuery
	 * @param runCount
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws TableUnavailableException
	 * @throws TableFailedException
	 * @throws TableLockUnavailableException
	 */
	QueryResultBundle queryAsStreamAfterAuthorization(ProgressCallback progressCallback, SqlQuery query,
			RowHandler rowHandler, final QueryOptions options)
			throws TableUnavailableException, TableFailedException, LockUnavilableException {
		// build up the response.
		QueryResultBundle bundle = new QueryResultBundle();
		if(options.returnColumnModels()) {
			bundle.setColumnModels(query.getTableSchema());
		}
		if(options.returnSelectColumns()) {
			bundle.setSelectColumns(query.getSelectColumns());
		}

		IdAndVersion idAndVersion = IdAndVersion
				.parse(query.getSingleTableId().orElseThrow(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEXT));
		TableIndexDAO indexDao = tableConnectionFactory.getConnection(idAndVersion);
		
		if (query.isIncludeSearch() && !indexDao.isSearchEnabled(idAndVersion)) {
			throw new IllegalArgumentException("Invalid use of " + TextMatchesPredicate.KEYWORD + ". Full text search is not enabled on table " + idAndVersion + ".");
		}

		FacetModel facetModel = new FacetModel(query.getSelectedFacets(), query, options.returnFacets());

		// determine whether or not to run with facet filters
		SqlQuery queryToRun;
		if (facetModel.hasFiltersApplied()) {
			queryToRun = facetModel.getFacetFilteredQuery();
		} else {
			queryToRun = query;
		}

		// run the actual query if needed.
		if (rowHandler != null) {
			// run the query
			RowSet rowSet = runQueryAsStream(progressCallback, queryToRun, rowHandler, indexDao);
			QueryResult queryResult = new QueryResult();
			queryResult.setQueryResults(rowSet);
			bundle.setQueryResult(queryResult);
		}

		// run the count query if needed.
		if (options.runCount()) {
			// count requested.
			Long count = runCountQuery(queryToRun, indexDao);
			bundle.setQueryCount(count);
		}

		// run the facet counts if needed
		if (options.returnFacets()) {
			// use original query instead of queryToRun because need the where clause that
			// was not modified by any facets
			List<FacetColumnResult> facetResults = runFacetQueries(facetModel, indexDao);
			bundle.setFacets(facetResults);
		}
		
		if(options.runSumFileSizes()) {
			SumFileSizes sumFileSizes = runSumFileSize(queryToRun, indexDao);
			bundle.setSumFileSizes(sumFileSizes);
		}
		
		if(options.returnLastUpdatedOn()) {
			Date lastUpdatedOn = tableManagerSupport.getLastChangedOn(idAndVersion);
			bundle.setLastUpdatedOn(lastUpdatedOn);
		}

		return bundle;
	}

	/**
	 * Runs facet queries (enumeration count or range min/max) for all columns in
	 * queryFacetColumns.
	 * 
	 * @param originalQuery     the non-transformed query that was submitted by the
	 *                          user.
	 * @param queryFacetColumns
	 * @param indexDao
	 * @return
	 */
	public List<FacetColumnResult> runFacetQueries(FacetModel facetModel, TableIndexDAO indexDao) {
		ValidateArgument.required(facetModel, "queryFacetColumns");
		ValidateArgument.required(indexDao, "indexDao");

		List<FacetColumnResult> facetResults = new ArrayList<>();
		for (FacetTransformer facetQueryTransformer : facetModel.getFacetInformationQueries()) {
			RowSet rowSet = indexDao.query(null, facetQueryTransformer.getFacetSqlQuery());
			facetResults.add(facetQueryTransformer.translateToResult(rowSet));
		}
		return facetResults;
	}

	/**
	 * For the given bundle, is the number of rows equal to the maximum rows per
	 * page? This is used to determine if a next page token should be included with
	 * a query result.
	 * 
	 * @param bundle
	 * @return
	 */
	public static boolean isRowCountEqualToMaxRowsPerPage(QueryResultBundle bundle, int maxRowsPerPage) {
		if (bundle != null) {
			if (bundle.getQueryResult() != null) {
				if (bundle.getQueryResult().getQueryResults() != null) {
					if(bundle.getQueryResult().getQueryResults().getRows() != null){
						int resultSize = bundle.getQueryResult().getQueryResults().getRows().size();
						return maxRowsPerPage == resultSize;
					}
				}
			}
		}
		return false;
	}

	@Override
	public QueryResult queryNextPage(ProgressCallback progressCallback, UserInfo user, QueryNextPageToken nextPageToken)
			throws TableUnavailableException, TableFailedException, LockUnavilableException {
		Query query = TableQueryUtils.createQueryFromNextPageToken(nextPageToken);
		QueryOptions options = new QueryOptions().withRunQuery(true).withRunCount(false).withReturnFacets(false).withRunSumFileSizes(false);;
		QueryResultBundle queryResult = querySinglePage(progressCallback, user, query, options);
		return queryResult.getQueryResult();
	}

	@Override
	public QueryResultBundle queryBundle(ProgressCallback progressCallback, UserInfo user,
			QueryBundleRequest queryBundle)
			throws TableUnavailableException, TableFailedException, LockUnavilableException {
		ValidateArgument.required(queryBundle.getQuery(), "query");
		ValidateArgument.required(queryBundle.getQuery().getSql(), "query.sql");
		QueryOptions options = new QueryOptions().withMask(queryBundle.getPartMask());
		// execute the query
		return querySinglePage(progressCallback, user, queryBundle.getQuery(),  options);
	}

	/**
	 * Set the default value for a Query
	 * 
	 * @param listRequest
	 * @return
	 */
	public static void setDefaultsValues(Query query) {
		ValidateArgument.required(query, "query");
		if (query.getIncludeEntityEtag() == null) {
			// default to false
			query.setIncludeEntityEtag(false);
		}
	}

	/**
	 * Set the default value for a download request.
	 * 
	 * @param request
	 * @return
	 */
	public static void setDefaultValues(DownloadFromTableRequest request) {
		ValidateArgument.required(request, "request");
		// get query defaults
		TableQueryManagerImpl.setDefaultsValues((Query) request);
		if (request.getIncludeRowIdAndRowVersion() == null) {
			// default to true
			request.setIncludeRowIdAndRowVersion(true);
		}
		if (request.getWriteHeader() == null) {
			// default to true
			request.setWriteHeader(true);
		}
	}

	/**
	 * 
	 * @param sql
	 * @param writer
	 * @return The resulting RowSet will not contain any
	 * @throws TableUnavailableException
	 * @throws NotFoundException
	 * @throws TableFailedException
	 * @throws TableLockUnavailableException
	 */
	@Override
	public DownloadFromTableResult runQueryDownloadAsStream(ProgressCallback progressCallback, UserInfo user,
			DownloadFromTableRequest request, final CSVWriterStream writer)
			throws TableUnavailableException, NotFoundException, TableFailedException, LockUnavilableException {
		// Convert to a query.
		try {
			// ensure null values in request are set to defaults.
			setDefaultValues(request);
			// there is no limit to the size
			Long maxBytes = null;
			final SqlQuery query = queryPreflight(user, request, maxBytes);

			// Do not include rowId and version if it is not provided (PLFM-2993)
			if (!query.includesRowIdAndVersion()) {
				request.setIncludeRowIdAndRowVersion(false);
				request.setIncludeEntityEtag(false);
			}
			// This handler will capture the row data.
			CSVWriterRowHandler handler = new CSVWriterRowHandler(writer, query.getSelectColumns(),
					request.getIncludeRowIdAndRowVersion(), query.includeEntityEtag());

			if (request.getWriteHeader()) {
				handler.writeHeader();
			}

			// run the query.
			QueryOptions options = new QueryOptions().withRunQuery(true).withReturnSelectColumns(true)
					.withRunCount(false).withReturnFacets(false);
			QueryResultBundle result = queryAsStream(progressCallback, user, query, handler, options);
			// convert the response
			DownloadFromTableResult response = new DownloadFromTableResult();
			response.setHeaders(result.getSelectColumns());
			response.setTableId(result.getQueryResult().getQueryResults().getTableId());
			// pass along the etag.
			response.setEtag(result.getQueryResult().getQueryResults().getEtag());
			return response;
		} catch (EmptyResultException e) { // this is thrown in queryPreflight()
			throw new IllegalArgumentException("Table " + e.getTableId() + " has an empty schema", e);
		}
	}

	/**
	 * The last step to running an actaul query against the table as a stream.
	 * 
	 * @param callback
	 * @param query
	 * @param rowHandler
	 * @return
	 */
	RowSet runQueryAsStream(ProgressCallback callback, SqlQuery query, RowHandler rowHandler, TableIndexDAO indexDao) {
		ValidateArgument.required(query, "query");
		ValidateArgument.required(rowHandler, "rowHandler");
		indexDao.queryAsStream(callback, query, rowHandler);
		RowSet results = new RowSet();
		results.setHeaders(query.getSelectColumns());
		results.setTableId(query.getSingleTableId().orElseThrow(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEXT));
		return results;
	}

	/**
	 * Run a count query.
	 * 
	 * @param query
	 * @return
	 */
	long runCountQuery(SqlQuery query, TableIndexDAO indexDao) {
		try {
			// create the count SQL from the already transformed model.
			String countSql = SqlElementUtils.createCountSql(query.getTransformedModel());
			// execute the count query
			Long count = indexDao.countQuery(countSql, query.getParameters());

			/*
			 * Post processing for count. When a limit and/or offset is specified in a
			 * query, count(*) just ignores those, since it assumes the limit & offset apply
			 * to the one row count(*) returns. In actuality, we want to apply that limit &
			 * offset to the count itself. We do that here manually.
			 */
			Pagination pagination = query.getModel().getTableExpression().getPagination();
			if (pagination != null) {
				if (pagination.getOffsetLong() != null) {
					long offsetForCount = pagination.getOffsetLong();
					count = Math.max(0, count - offsetForCount);
				}
				if (pagination.getLimitLong() != null) {
					long limitForCount = pagination.getLimitLong();
					count = Math.min(limitForCount, count);
				}
			}
			return count;
		} catch (SimpleAggregateQueryException e) {
			// simple aggregate queries always return one row.
			return 1L;
		}
	}
	
	/**
	 * Run the queries to get the sum of the file sizes (bytes) for the given query.
	 * 
	 * @param query
	 * @param indexDao
	 * @return
	 */
	SumFileSizes runSumFileSize(SqlQuery query, TableIndexDAO indexDao) {
		SumFileSizes result = new SumFileSizes();
		result.setGreaterThan(false);
		result.setSumFileSizesBytes(0L);
		if(EntityType.entityview.equals(query.getTableType()) || EntityType.dataset.equals(query.getTableType())){
			// actual values are only provided for entity views.
			try {
				// first get the rowId and rowVersions for the given query up to the limit + 1.
				String sqlSelectIdAndVersions = SqlElementUtils.buildSqlSelectRowIdAndVersions(query.getTransformedModel(), MAX_ROWS_PER_CALL + 1L);
				List<IdAndVersion> rowIdAndVersions = indexDao.getRowIdAndVersions(sqlSelectIdAndVersions, query.getParameters());
				boolean isGreaterThan = rowIdAndVersions.size() > MAX_ROWS_PER_CALL;
				result.setGreaterThan(isGreaterThan);
				// Use the rowIds to calculate the sum of the file sizes.
				long sumFileSizesBytes = indexDao.getSumOfFileSizes(ViewObjectType.ENTITY.getMainType(), rowIdAndVersions);
				result.setSumFileSizesBytes(sumFileSizesBytes);
			} catch (SimpleAggregateQueryException e) {
				// zero results will be returned for this case.
				result.setGreaterThan(false);
				result.setSumFileSizesBytes(0L);
			}
		}
		return result;
	}
	
	

	/**
	 * Parser a query and convert ParseExceptions to IllegalArgumentExceptions
	 * 
	 * @param sql
	 * @return
	 */
	private QuerySpecification parserQuery(String sql) {
		try {
			return TableQueryParser.parserQuery(sql);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public Long getMaxRowsPerPage(List<ColumnModel> models) {
		// Calculate the size
		int maxRowSizeBytes = TableModelUtils.calculateMaxRowSize(models);
		if (maxRowSizeBytes < 1)
			return null;
		return (long) (this.maxBytesPerRequest / maxRowSizeBytes);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.manager.table.TableStatusManager#
	 * validateTableIsAvailable(java.lang.String)
	 */
	@Override
	public TableStatus validateTableIsAvailable(String tableId)
			throws NotFoundException, TableUnavailableException, TableFailedException {
		IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
		final TableStatus status = tableManagerSupport.getTableStatusOrCreateIfNotExists(idAndVersion);
		switch (status.getState()) {
		case AVAILABLE:
			return status;
		case PROCESSING:
			// When the table is not available, we communicate the current status of the
			// table in this exception.
			throw new TableUnavailableException(status);
		default:
		case PROCESSING_FAILED:
			// When the table is in a failed state, we communicate the current status of the
			// table in this exception.
			throw new TableFailedException(status);
		}
	}

	/**
	 * Create a new empty query result bundle.
	 * 
	 * @param tableId
	 * @return
	 */
	public static QueryResultBundle createEmptyBundle(String tableId, QueryOptions options) {
		QueryResult result = new QueryResult();
		QueryResultBundle bundle = new QueryResultBundle();
		if(options.runQuery()) {
			RowSet emptyRowSet = new RowSet();
			emptyRowSet.setRows(new LinkedList<Row>());
			emptyRowSet.setTableId(tableId);
			emptyRowSet.setHeaders(new LinkedList<SelectColumn>());
			result.setQueryResults(emptyRowSet);
			bundle.setQueryResult(result);
		}
		if(options.runCount()) {
			bundle.setQueryCount(0L);
		}
		if(options.returnSelectColumns()) {
			bundle.setSelectColumns(new LinkedList<SelectColumn>());
		}
		if(options.returnColumnModels()) {
			bundle.setColumnModels(new LinkedList<ColumnModel>());
		}
		if(options.returnMaxRowsPerPage()) {
			bundle.setMaxRowsPerPage(1L);
		}
		if(options.runSumFileSizes()) {
			SumFileSizes sum = new SumFileSizes();
			sum.setGreaterThan(false);
			sum.setSumFileSizesBytes(0L);
			bundle.setSumFileSizes(sum);
		}
		return bundle;
	}

	/**
	 * Add a row level filter to the given query.
	 * 
	 * @param user
	 * @param query
	 * @return
	 * @throws TableFailedException
	 * @throws TableUnavailableException
	 * @throws NotFoundException
	 */
	QuerySpecification addRowLevelFilter(UserInfo user, QuerySpecification query, IndexDescription indexDescription)
			throws NotFoundException, TableUnavailableException, TableFailedException {
		String tableId = query.getSingleTableName().orElseThrow(TableConstants.JOIN_NOT_SUPPORTED_IN_THIS_CONTEXT);
		if(indexDescription.getBenefactors().isEmpty()) {
			// with no benefactors nothing is needed.
			return query;
		}
		IdAndVersion idAndVersion = IdAndVersion.parse(tableId);
		TableIndexDAO indexDao = tableConnectionFactory.getConnection(idAndVersion);
		QuerySpecification resultQuery = query;
		for(BenefactorDescription dependencyDesc: indexDescription.getBenefactors()) {
			// lookup the distinct benefactor IDs applied to the table.
			Set<Long> tableBenefactors = null;
			try {
				tableBenefactors = indexDao.getDistinctLongValues(idAndVersion, dependencyDesc.getBenefactorColumnName());
			} catch (BadSqlGrammarException e) { // table has not been created yet
				tableBenefactors = Collections.emptySet();
			}

			Set<Long> accessibleBenefactors = tableManagerSupport.getAccessibleBenefactors(user, dependencyDesc.getBenefactorType(), tableBenefactors);

			resultQuery = buildBenefactorFilter(resultQuery, accessibleBenefactors, dependencyDesc.getBenefactorColumnName());
		}
		return resultQuery;
	}

    /**
     * Build a new query with a benefactor filter applied to the SQL from the passed
     * query.
     *
     * @param originalQuery
     * @param accessibleBenefactors
     * @return
     * @throws EmptyResultException
     */
    public static QuerySpecification buildBenefactorFilter(QuerySpecification originalQuery,
                                                           Set<Long> accessibleBenefactors,
                                                           String benefactorColumnName) {
        ValidateArgument.required(originalQuery, "originalQuery");
        ValidateArgument.required(accessibleBenefactors, "accessibleBenefactors");

		// add -1 to set, as -1 is default value for benefactors column
		accessibleBenefactors.add(-1l);

        // copy the original model
        try {
            QuerySpecification modelCopy = new TableQueryParser(originalQuery.toSql()).querySpecification();
            WhereClause where = originalQuery.getTableExpression().getWhereClause();
            StringBuilder filterBuilder = new StringBuilder();
            filterBuilder.append("WHERE ");
            if (where != null) {
                filterBuilder.append("(");
                filterBuilder.append(where.getSearchCondition().toSql());
                filterBuilder.append(") AND ");
            }

			filterBuilder.append(benefactorColumnName);
			filterBuilder.append(" IN (");

			filterBuilder.append(accessibleBenefactors.stream()
					.map(String::valueOf)
					.collect(Collectors.joining(",")));
			filterBuilder.append(")");

            // create the new where
            where = new TableQueryParser(filterBuilder.toString()).whereClause();
            modelCopy.getTableExpression().replaceWhere(where);
            // return a copy
            return modelCopy;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TableType getTableEntityType(IdAndVersion idAndVersion) {
        return tableManagerSupport.getTableType(idAndVersion);
    }
}
