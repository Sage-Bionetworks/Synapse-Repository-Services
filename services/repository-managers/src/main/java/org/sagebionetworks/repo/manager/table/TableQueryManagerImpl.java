package org.sagebionetworks.repo.manager.table;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.FacetColumnResult;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.cluster.SqlQueryBuilder;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.Pagination;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.WhereClause;
import org.sagebionetworks.table.query.util.SimpleAggregateQueryException;
import org.sagebionetworks.table.query.util.SqlElementUntils;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.csv.CSVWriterStream;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;

public class TableQueryManagerImpl implements TableQueryManager {
	
	public static final int READ_LOCK_TIMEOUT_SEC = 60;
	
	public static final long BUNDLE_MASK_QUERY_RESULTS = 0x1;
	public static final long BUNDLE_MASK_QUERY_COUNT = 0x2;
	public static final long BUNDLE_MASK_QUERY_SELECT_COLUMNS = 0x4;
	public static final long BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE = 0x8;
	public static final long BUNDLE_MASK_QUERY_COLUMN_MODELS = 0x10;
	public static final long BUNDLE_MASK_QUERY_FACETS = 0x20;
	
	@Autowired
	TableManagerSupport tableManagerSupport;
	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	ColumnModelDAO columnModelDAO;
	
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
	 * @see
	 * org.sagebionetworks.repo.manager.table.TableQueryManager#querySinglePage
	 * (org.sagebionetworks.common.util.progress.ProgressCallback,
	 * org.sagebionetworks.repo.model.UserInfo, java.lang.String,
	 * java.util.List, java.lang.Long, java.lang.Long, boolean, boolean,
	 * boolean)
	 */
	@Override
	public QueryResultBundle querySinglePage(
			ProgressCallback progressCallback, UserInfo user,
			Query query, boolean runQuery, boolean runCount, boolean returnFacets)
			throws TableUnavailableException,
			TableFailedException, LockUnavilableException {
		try{
			// Set the default values
			TableQueryManagerImpl.setDefaultsValues(query);
			// handler will capture the results of the query.
			SinglePageRowHandler rowHandler = null;
			if(runQuery){
				rowHandler = new SinglePageRowHandler();
			}
			// pre-flight includes parsing and authorization
			SqlQuery sqlQuery = queryPreflight(user, query, this.maxBytesPerRequest);
			
			// run the query as a stream.
			QueryResultBundle bundle = queryAsStream(progressCallback, user, sqlQuery, rowHandler, runCount, returnFacets);
			// save the max rows per page.
			bundle.setMaxRowsPerPage(sqlQuery.getMaxRowsPerPage());
			// add captured rows to the bundle
			if(runQuery){
				bundle.getQueryResult().getQueryResults().setRows(rowHandler.getRows());
			}
			// add the next page token if needed
			if (isRowCountEqualToMaxRowsPerPage(bundle)) {
				int maxRowsPerPage = bundle.getMaxRowsPerPage().intValue();
				long nextOffset = (query.getOffset() == null ? 0 : query.getOffset()) + maxRowsPerPage;
				QueryNextPageToken nextPageToken = TableQueryUtils.createNextPageToken(query.getSql(), query.getSort(),
						nextOffset, query.getLimit(), query.getIsConsistent(), query.getSelectedFacets());
				bundle.getQueryResult().setNextPageToken(nextPageToken);
			}
			return bundle;
		} catch (EmptyResultException e) {
			// return an empty result.
			return createEmptyBundle(e.getTableId());
		}

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
	 * @param user
	 * @param query
	 * @return
	 * @throws EmptyResultException 
	 * @throws TableFailedException 
	 * @throws TableUnavailableException 
	 * @throws NotFoundException 
	 */
	SqlQuery queryPreflight(UserInfo user, Query query, Long maxBytesPerPage) throws EmptyResultException, NotFoundException, TableUnavailableException, TableFailedException{
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(query, "Query");
		ValidateArgument.required(query.getSql(), "Query");
		// 1. Parse the SQL string
		QuerySpecification model = parserQuery(query.getSql());
		// We now have the table's ID.
		String tableId = model.getTableName();
		
		// 2. Validate the user has read access on this table
		EntityType tableType = tableManagerSupport.validateTableReadAccess(user, tableId);
		
		// 3. Get the table's schema
		List<ColumnModel> columnModels = columnModelDAO.getColumnModelsForObject(tableId);
		if(columnModels.isEmpty()){
			throw new EmptyResultException("Table schema is empty for: "+tableId, tableId);
		}
		
		// 4. Add row level filter as needed.
		if(EntityType.entityview.equals(tableType)){
			// Table views must have a row level filter applied to the query
			model = addRowLevelFilter(user, model);
		}
		// Return the prepared query.
		return new SqlQueryBuilder(model)
		.tableSchema(columnModels)
		.overrideOffset(query.getOffset())
		.overrideLimit(query.getLimit())
		.maxBytesPerPage(maxBytesPerPage)
		.isConsistent(query.getIsConsistent())
		.includeEntityEtag(query.getIncludeEntityEtag())
		.selectedFacets(query.getSelectedFacets())
		.sortList(query.getSort())
		.tableType(tableType)
		.build();
	}
	
	/**
	 * The main entry point for all table queries.  Any business logic
	 * that must be applied to all table queries should applied here or lower.
	 * 
	 * @param progressCallback
	 * @param user
	 * @param query
	 * @param offset
	 * @param limit
	 * @param runQuery
	 * @param runCount
	 * @param isConsistent
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws TableUnavailableException
	 * @throws TableFailedException
	 * @throws EmptyResultException 
	 * @throws TableLockUnavailableException
	 */
	QueryResultBundle queryAsStream(final ProgressCallback progressCallback,
			final UserInfo user, final SqlQuery query,
			final RowHandler rowHandler,final  boolean runCount, final boolean returnFacets)
			throws DatastoreException, NotFoundException,
			TableUnavailableException, TableFailedException, LockUnavilableException, EmptyResultException {		
		// consistent queries are run with a read lock on the table and include the current etag.
		if(query.isConsistent()){
			// run with the read lock
			return tryRunWithTableReadLock(
					progressCallback, query.getTableId(),
					new ProgressingCallable<QueryResultBundle>(){

					@Override
					public QueryResultBundle call(
							ProgressCallback callback) throws Exception {
						// We can only run this query if the table is available.
						final TableStatus status = validateTableIsAvailable(query.getTableId());
						// run the query
						QueryResultBundle bundle = queryAsStreamAfterAuthorization(progressCallback, query, rowHandler, runCount, returnFacets);
						// add the status to the result
						if(rowHandler != null){
							// the etag is only returned for consistent queries.
							bundle.getQueryResult().getQueryResults().setEtag(status.getLastTableChangeEtag());
						}
						return bundle;
					}});
		}else{
			// run without a read lock.
			return queryAsStreamAfterAuthorization(progressCallback, query, rowHandler, runCount, returnFacets);
		}
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
	<R, T> R tryRunWithTableReadLock(ProgressCallback callback, String tableId,
			ProgressingCallable<R> runner) throws TableUnavailableException, TableFailedException, EmptyResultException{
		
		try {
			return tableManagerSupport.tryRunWithTableNonexclusiveLock(callback, tableId, READ_LOCK_TIMEOUT_SEC, runner);
		} catch (RuntimeException e) {
			// runtime exceptions are unchanged.
			throw e;
		} catch (TableUnavailableException e) {
			throw e;
		} catch (TableFailedException e) {
			throw e;
		} catch (EmptyResultException e) {
			throw e;
		} catch (Exception e){
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
	 * @param isConsistent
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws TableUnavailableException
	 * @throws TableFailedException
	 * @throws TableLockUnavailableException
	 */
	QueryResultBundle queryAsStreamAfterAuthorization(ProgressCallback progressCallback, SqlQuery query,
			RowHandler rowHandler, boolean runCount, boolean returnFacets)
			throws TableUnavailableException, TableFailedException, LockUnavilableException {
		// build up the response.
		QueryResultBundle bundle = new QueryResultBundle();
		bundle.setColumnModels(query.getTableSchema());
		bundle.setSelectColumns(query.getSelectColumns());
		
		TableIndexDAO indexDao = tableConnectionFactory.getConnection(query.getTableId());
		
		FacetModel facetModel = new FacetModel(query.getSelectedFacets(), query, returnFacets);
		
		//determine whether or not to run with facet filters
		SqlQuery queryToRun;
		if(facetModel.hasFiltersApplied()){
			queryToRun = facetModel.getFacetFilteredQuery();
		}else{
			queryToRun = query;
		}
		
		// run the actual query if needed.
		QueryResult queryResult = null;
		List<FacetColumnResult> facetResults = null;
		if(rowHandler != null){
			// run the query
			RowSet rowSet = runQueryAsStream(progressCallback, queryToRun, rowHandler, indexDao);
			queryResult = new QueryResult();
			queryResult.setQueryResults(rowSet);
		}
		
		// run the count query if needed.
		Long count = null;
		if(runCount){
			// count requested.
			count = runCountQuery(queryToRun, indexDao);
		}
		
		//run the facet counts if needed
		if(returnFacets){
			//use original query instead of queryToRun because need the where clause that was not modified by any facets
			facetResults = runFacetQueries(facetModel, indexDao);
		}
		
		//run 
		bundle.setQueryResult(queryResult);
		bundle.setQueryCount(count);
		bundle.setFacets(facetResults);
		return bundle;
	}
	
	/**
	 * Runs facet queries (enumeration count or range min/max) for all columns in queryFacetColumns. 
	 * @param originalQuery the non-transformed query that was submitted by the user.
	 * @param queryFacetColumns
	 * @param indexDao
	 * @return
	 */
	public List<FacetColumnResult> runFacetQueries(FacetModel facetModel, TableIndexDAO indexDao) {
		ValidateArgument.required(facetModel, "queryFacetColumns");
		ValidateArgument.required(indexDao, "indexDao");
		
		
		List<FacetColumnResult> facetResults = new ArrayList<>();
		for(FacetTransformer facetQueryTransformer : facetModel.getFacetInformationQueries()){
			RowSet rowSet = indexDao.query(null, facetQueryTransformer.getFacetSqlQuery());
			facetResults.add(facetQueryTransformer.translateToResult(rowSet));
		}
		return facetResults;
	}

	/**
	 * For the given bundle, is the number of rows equal to the maximum rows per
	 * page? This is used to determine if a next page token should be included
	 * with a query result.
	 * 
	 * @param bundle
	 * @return
	 */
	public static boolean isRowCountEqualToMaxRowsPerPage(QueryResultBundle bundle){
		if(bundle != null){
			if(bundle.getQueryResult() != null){
				if(bundle.getQueryResult().getQueryResults() != null){
					if(bundle.getMaxRowsPerPage() != null){
						int maxRowsPerPage = bundle.getMaxRowsPerPage().intValue();
						int resultSize = bundle.getQueryResult().getQueryResults().getRows().size();
						return maxRowsPerPage == resultSize;
					}
				}
			}
		}
		return false;
	}

	@Override
	public QueryResult queryNextPage(ProgressCallback progressCallback,
			UserInfo user, QueryNextPageToken nextPageToken)
			throws TableUnavailableException, TableFailedException,
			LockUnavilableException {
		Query query = TableQueryUtils.createQueryFromNextPageToken(nextPageToken);
		boolean runQuery = true;
		boolean runCount = false;
		boolean returnFacets = false;
		QueryResultBundle queryResult = querySinglePage(progressCallback, user, query, runQuery, runCount, returnFacets);
		return queryResult.getQueryResult();
	}

	@Override
	public QueryResultBundle queryBundle(
			ProgressCallback progressCallback, UserInfo user,
			QueryBundleRequest queryBundle) throws TableUnavailableException,
			TableFailedException, LockUnavilableException {
		ValidateArgument.required(queryBundle.getQuery(), "query");
		ValidateArgument.required(queryBundle.getQuery().getSql(), "query.sql");

		QueryResultBundle bundle = new QueryResultBundle();
		// The SQL query is need for the actual query, select columns, and max rows per page.
		long partMask = -1L; // default all
		if (queryBundle.getPartMask() != null) {
			partMask = queryBundle.getPartMask();
		}
		boolean runQuery = ((partMask & BUNDLE_MASK_QUERY_RESULTS) != 0);
		boolean runCount = ((partMask & BUNDLE_MASK_QUERY_COUNT) != 0);
		boolean returnFacets = ((partMask & BUNDLE_MASK_QUERY_FACETS) != 0);
		
		// execute the query
		QueryResultBundle queryResult = querySinglePage(
				progressCallback,
				user,
				queryBundle.getQuery(),
				runQuery,
				runCount,
				returnFacets
				);
		
		if(runQuery){
			bundle.setQueryResult(queryResult.getQueryResult());
		}
		if(runCount){
			bundle.setQueryCount(queryResult.getQueryCount());
		}
		
		if(returnFacets){
			bundle.setFacets(queryResult.getFacets());
		}
		
		// select columns must be fetched for for the select columns or max
		// rows per page.
		if ((partMask & BUNDLE_MASK_QUERY_SELECT_COLUMNS) > 0) {
			bundle.setSelectColumns(queryResult.getSelectColumns());
		}
		// all schema columns
		if ((partMask & BUNDLE_MASK_QUERY_COLUMN_MODELS) > 0) {
			bundle.setColumnModels(queryResult.getColumnModels());
		}
		// Max rows per column
		if ((partMask & BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE) > 0) {
			bundle.setMaxRowsPerPage(queryResult.getMaxRowsPerPage());
		}
		return bundle;
	}
	
	/**
	 * Set the default value for a Query
	 * @param listRequest
	 * @return
	 */
	public static void setDefaultsValues(Query query){
		ValidateArgument.required(query, "query");
		if(query.getIsConsistent() == null){
			// default to true
			query.setIsConsistent(true);
		}
		if(query.getIncludeEntityEtag() == null){
			// default to false
			query.setIncludeEntityEtag(false);
		}
	}
	
	/**
	 * Set the default value for a download request.
	 * @param request
	 * @return
	 */
	public static void setDefaultValues(DownloadFromTableRequest request){
		ValidateArgument.required(request, "request");
		// get query defaults
		TableQueryManagerImpl.setDefaultsValues((Query)request);
		if(request.getIncludeRowIdAndRowVersion() == null){
			// default to true
			request.setIncludeRowIdAndRowVersion(true);
		}
		if(request.getWriteHeader() == null){
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
	public DownloadFromTableResult runQueryDownloadAsStream(
			ProgressCallback progressCallback, UserInfo user, DownloadFromTableRequest request,
			final CSVWriterStream writer)
			throws TableUnavailableException, NotFoundException,
			TableFailedException, LockUnavilableException {
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
			CSVWriterRowHandler handler = new CSVWriterRowHandler(writer,
					query.getSelectColumns(), request.getIncludeRowIdAndRowVersion(), query.includeEntityEtag());
			
			if (request.getWriteHeader()) {
				handler.writeHeader();
			}
			
			// run the query.
			boolean runCount = false;
			boolean returnFacets = false;
			QueryResultBundle result = queryAsStream(progressCallback, user,
					query, handler, runCount, returnFacets);
			// convert the response
			DownloadFromTableResult response = new DownloadFromTableResult();
			response.setHeaders(result.getSelectColumns());
			response.setTableId(result.getQueryResult().getQueryResults().getTableId());
			// pass along the etag.
			response.setEtag(result.getQueryResult().getQueryResults().getEtag());
			return response;
		} catch (EmptyResultException e) {
			throw new IllegalArgumentException("Table "+e.getTableId()+" has an empty schema");
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
	RowSet runQueryAsStream(ProgressCallback callback,
			SqlQuery query, RowHandler rowHandler, TableIndexDAO indexDao) {
		ValidateArgument.required(query, "query");
		ValidateArgument.required(rowHandler, "rowHandler");
		indexDao.queryAsStream(callback, query, rowHandler);
		RowSet results = new RowSet();
		results.setHeaders(query.getSelectColumns());
		results.setTableId(query.getTableId());
		return results;
	}
	
	/**
	 * Run a count query.
	 * @param query
	 * @return
	 */
	long runCountQuery(SqlQuery query, TableIndexDAO indexDao) {
		try {
			// create the count SQL from the already transformed model.
			String countSql = SqlElementUntils.createCountSql(query.getTransformedModel());
			// execute the count query
			Long count = indexDao.countQuery(countSql, query.getParameters());
			
			/*
			 * Post processing for count. When a limit and/or offset is
			 * specified in a query, count(*) just ignores those, since it
			 * assumes the limit & offset apply to the one row count(*) returns.
			 * In actuality, we want to apply that limit & offset to the count
			 * itself. We do that here manually.
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
	 * Parser a query and convert ParseExceptions to IllegalArgumentExceptions
	 * 
	 * @param sql
	 * @return
	 */
	private QuerySpecification parserQuery(String sql){
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
	 * @see org.sagebionetworks.repo.manager.table.TableStatusManager#validateTableIsAvailable(java.lang.String)
	 */
	@Override
	public TableStatus validateTableIsAvailable(String tableId) throws NotFoundException, TableUnavailableException, TableFailedException {
		final TableStatus status = tableManagerSupport.getTableStatusOrCreateIfNotExists(tableId);
		switch(status.getState()){
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
	 * @param tableId
	 * @return
	 */
	public static QueryResultBundle createEmptyBundle(String tableId){
		QueryResult result = new QueryResult();
		QueryResultBundle bundle = new QueryResultBundle();
		RowSet emptyRowSet = new RowSet();
		emptyRowSet.setRows(new LinkedList<Row>());
		emptyRowSet.setTableId(tableId);
		emptyRowSet.setHeaders(new LinkedList<SelectColumn>());
		result.setQueryResults(emptyRowSet);
		bundle.setQueryResult(result);
		bundle.setQueryCount(0L);
		bundle.setColumnModels(new LinkedList<ColumnModel>());
		bundle.setMaxRowsPerPage(1L);
		bundle.setSelectColumns(new LinkedList<SelectColumn>());
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
	QuerySpecification addRowLevelFilter(UserInfo user, QuerySpecification query) throws EmptyResultException, NotFoundException, TableUnavailableException, TableFailedException {
		String tableId = query.getTableName();
		// Get a connection to the table.
		TableIndexDAO indexDao = tableConnectionFactory.getConnection(tableId);
		// Can only get the benefactors if the table is available.
		// We can only run this query if the table is available.
		validateTableIsAvailable(tableId);
		// lookup the distinct benefactor IDs applied to the table.
		Set<Long> tableBenefactors = indexDao.getDistinctLongValues(tableId, TableConstants.ROW_BENEFACTOR);
		if(tableBenefactors.isEmpty()){
			throw new EmptyResultException("Table has no benefactors", tableId);
		}
		// Get the sub-set of benefactors visible to the user.
		Set<Long> accessibleBenefactors = tableManagerSupport.getAccessibleBenefactors(user, tableBenefactors);
		return buildBenefactorFilter(query, accessibleBenefactors);
	}
	
	/**
	 * Build a new query with a benefactor filter applied to the SQL from the passed query.
	 * @param originalQuery
	 * @param accessibleBenefactors
	 * @return
	 * @throws EmptyResultException 
	 */
	public static QuerySpecification buildBenefactorFilter(QuerySpecification originalQuery, Set<Long> accessibleBenefactors) throws EmptyResultException{
		ValidateArgument.required(originalQuery, "originalQuery");
		ValidateArgument.required(accessibleBenefactors, "accessibleBenefactors");
		if(accessibleBenefactors.isEmpty()){
			throw new EmptyResultException("User does not have access to any benefactors in the table.", originalQuery.getTableName());
		}
		// copy the original model
		try {
			QuerySpecification modelCopy = new TableQueryParser(originalQuery.toSql()).querySpecification();
			WhereClause where = originalQuery.getTableExpression().getWhereClause();
			StringBuilder filterBuilder = new StringBuilder();
			filterBuilder.append("WHERE ");
			if(where != null){
				filterBuilder.append("(");
				filterBuilder.append(where.getSearchCondition().toSql());
				filterBuilder.append(") AND ");
			}
			filterBuilder.append(TableConstants.ROW_BENEFACTOR);
			filterBuilder.append(" IN (");
			boolean isFirst = true;
			for(Long id: accessibleBenefactors){
				if(!isFirst){
					filterBuilder.append(",");
				}
				filterBuilder.append(id);
				isFirst = false;
			}
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
}
