package org.sagebionetworks.repo.manager.table;

import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dao.table.RowHandler;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableLockUnavailableException;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.Pagination;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.util.SimpleAggregateQueryException;
import org.sagebionetworks.table.query.util.SqlElementUntils;
import org.sagebionetworks.util.Closer;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.csv.CSVWriterStream;
import org.springframework.beans.factory.annotation.Autowired;

import com.thoughtworks.xstream.XStream;

public class TableQueryManagerImpl implements TableQueryManager {
	
	public static final int READ_LOCK_TIMEOUT_SEC = 60;
	
	public static final long BUNDLE_MASK_QUERY_RESULTS = 0x1;
	public static final long BUNDLE_MASK_QUERY_COUNT = 0x2;
	public static final long BUNDLE_MASK_QUERY_SELECT_COLUMNS = 0x4;
	public static final long BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE = 0x8;
	public static final long BUNDLE_MASK_QUERY_COLUMN_MODELS = 0x10;
	
	@Autowired
	TableManagerSupport tableManagerSupport;
	@Autowired
	ConnectionFactory tableConnectionFactory;
	@Autowired
	ColumnModelDAO columnModelDAO;
	
	/**
	 * Injected via spring
	 */
	int maxBytesPerRequest;
	
	public void setMaxBytesPerRequest(int maxBytesPerRequest) {
		this.maxBytesPerRequest = maxBytesPerRequest;
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.manager.table.TableQueryManager#querySinglePage(org.sagebionetworks.common.util.progress.ProgressCallback, org.sagebionetworks.repo.model.UserInfo, java.lang.String, java.util.List, java.lang.Long, java.lang.Long, boolean, boolean, boolean)
	 */
	@Override
	public QueryResultBundle querySinglePage(ProgressCallback<Void> progressCallback, UserInfo user, String query, List<SortItem> sortList, Long offset, Long limit, boolean runQuery,
			boolean runCount, boolean isConsistent) throws DatastoreException, NotFoundException, TableUnavailableException,
			TableFailedException, TableLockUnavailableException {
		// handler will capture the results of the query.
		SinglePageRowHandler rowHandler = null;
		if(runQuery){
			rowHandler = new SinglePageRowHandler();
		}
		// run the query.
		QueryResultBundle bundle = querySinglePagePartTwo(progressCallback, user, query,
				sortList, offset, limit, rowHandler, runCount, isConsistent);
		// add captured rows to the bundle
		if(runQuery){
			bundle.getQueryResult().getQueryResults().setRows(rowHandler.getRows());
		}
		// add the next page token if needed
		if (isRowCountEqualToMaxRowsPerPage(bundle)) {
			int maxRowsPerPage = bundle.getMaxRowsPerPage().intValue();
			long nextOffset = (offset == null ? 0 : offset) + maxRowsPerPage;
			QueryNextPageToken nextPageToken = createNextPageToken(query,
					nextOffset, limit, isConsistent);
			bundle.getQueryResult().setNextPageToken(nextPageToken);
		}
		return bundle;
	}
	
	/**
	 * The second part of a single page query.
	 * 
	 * Should only be called from {@link #querySinglePage(ProgressCallback, UserInfo, String, List, Long, Long, boolean, boolean, boolean)}
	 * 
	 * @param progressCallback
	 * @param user
	 * @param query
	 * @param sortList
	 * @param offset
	 * @param limit
	 * @param rowHandler
	 * @param runCount
	 * @param isConsistent
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws TableUnavailableException
	 * @throws TableFailedException
	 * @throws TableLockUnavailableException
	 */
	QueryResultBundle querySinglePagePartTwo(ProgressCallback<Void> progressCallback, UserInfo user, String query, List<SortItem> sortList, Long offset, Long limit, RowHandler rowHandler,
			boolean runCount, boolean isConsistent) throws DatastoreException, NotFoundException, TableUnavailableException,
			TableFailedException, TableLockUnavailableException {
		SqlQuery sqlQuery;
		try {
			sqlQuery = createQuery(query, sortList);
			// The max rows per page is a function of the select size.
			int maxRowsPerPage = Math.max(1, this.maxBytesPerRequest / sqlQuery.getMaxRowSizeBytes());
			// Create a new query with pagination.
			QuerySpecification paginatedModel = SqlElementUntils.overridePagination(sqlQuery.getModel(), offset, limit, maxRowsPerPage);
			// the paginated query is what will be run.
			sqlQuery =  new SqlQuery(paginatedModel, sqlQuery.getTableSchema(), sqlQuery.getTableId());
			// run the query as a stream.
			QueryResultBundle bundle = queryAsStream(progressCallback, user, sqlQuery, rowHandler, runCount, isConsistent);
			// save the max rows per page.
			bundle.setMaxRowsPerPage(new Long(maxRowsPerPage));
			return bundle;
		} catch (EmptySchemaException e) {
			// return an empty result.
			QueryResult result = new QueryResult();
			QueryResultBundle bundle = new QueryResultBundle();
			RowSet emptyRowSet = new RowSet();
			emptyRowSet.setTableId(e.getTableId());
			result.setQueryResults(emptyRowSet);
			bundle.setQueryResult(result);
			bundle.setQueryCount(0L);
			bundle.setColumnModels(new LinkedList<ColumnModel>());
			bundle.setMaxRowsPerPage(1L);
			bundle.setSelectColumns(new LinkedList<SelectColumn>());
			return bundle;
		}
	}
	
	/**
	 * Run a query as a stream.
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
	QueryResultBundle queryAsStream(final ProgressCallback<Void> progressCallback,
			final UserInfo user, final SqlQuery query,
			final RowHandler rowHandler,final  boolean runCount, final boolean isConsistent)
			throws DatastoreException, NotFoundException,
			TableUnavailableException, TableFailedException, TableLockUnavailableException {
		// Validate the user has read access on this object
		tableManagerSupport.validateTableReadAccess(user, query.getTableId());
		// consistent queries are run with a read lock on the table and include the current etag.
		if(isConsistent){
			// run with a read lock
			return tableManagerSupport.tryRunWithTableNonexclusiveLock(
					progressCallback, query.getTableId(), READ_LOCK_TIMEOUT_SEC,
					new ProgressingCallable<QueryResultBundle, Void>(){

					@Override
					public QueryResultBundle call(
							ProgressCallback<Void> callback) throws Exception {
						// We can only run this query if the table is available.
						final TableStatus status = validateTableIsAvailable(query.getTableId());
						// run the query
						QueryResultBundle bundle = queryAsStreamHoldingLock(progressCallback, query, rowHandler, runCount);
						// add the status to the result
						if(rowHandler != null){
							// the etag is only returned for consistent queries.
							bundle.getQueryResult().getQueryResults().setEtag(status.getLastTableChangeEtag());
						}
						return bundle;
					}});
		}else{
			// run without a read lock.
			return queryAsStreamHoldingLock(progressCallback, query, rowHandler, runCount);
		}
	}

	/**
	 * Run a query while holding an optional read lock on the table.
	 * This should only be called from {@link #queryAsStream(ProgressCallback, UserInfo, SqlQuery, RowHandler, boolean, boolean)}
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
	QueryResultBundle queryAsStreamHoldingLock(ProgressCallback<Void> progressCallback, SqlQuery query,
			RowHandler rowHandler, boolean runCount)
			throws DatastoreException, NotFoundException,
			TableUnavailableException, TableFailedException, TableLockUnavailableException {
		// build up the response.
		QueryResultBundle bundle = new QueryResultBundle();
		bundle.setColumnModels(query.getTableSchema());
		bundle.setSelectColumns(query.getSelectColumns());

		// run the actual query if needed.
		QueryResult queryResult = null;
		if(rowHandler != null){
			// run the query
			RowSet rowSet = runQueryAsStream(progressCallback, query, rowHandler);
			queryResult = new QueryResult();
			queryResult.setQueryResults(rowSet);
		}
		
		// run the count query if needed.
		Long count = null;
		if(runCount){
			// count requested.
			count = runCountQuery(query);
		}
		bundle.setQueryResult(queryResult);
		bundle.setQueryCount(count);
		return bundle;
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
	public QueryResult queryNextPage(ProgressCallback<Void> progressCallback, UserInfo user, QueryNextPageToken nextPageToken) throws DatastoreException, NotFoundException,
			TableUnavailableException, TableFailedException, TableLockUnavailableException {
		Query query = createQueryFromNextPageToken(nextPageToken);
		QueryResultBundle queryResult = querySinglePage(progressCallback, user, query.getSql(), null, query.getOffset(), query.getLimit(), true,
				false, query.getIsConsistent());
		return queryResult.getQueryResult();
	}

	@Override
	public QueryResultBundle queryBundle(ProgressCallback<Void> progressCallback, UserInfo user, QueryBundleRequest queryBundle) throws DatastoreException, NotFoundException,
			TableUnavailableException, TableFailedException, TableLockUnavailableException {
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
		boolean isConsistent = BooleanUtils.isNotFalse(queryBundle.getQuery()
				.getIsConsistent());
		
		// execute the query
		QueryResultBundle queryResult = querySinglePage(
				progressCallback,
				user,
				queryBundle.getQuery().getSql(),
				queryBundle.getQuery().getSort(),
				queryBundle.getQuery().getOffset(),
				queryBundle.getQuery().getLimit(),
				runQuery,
				runCount,
				isConsistent
				);
		
		if(runQuery){
			bundle.setQueryResult(queryResult.getQueryResult());
		}
		if(runCount){
			bundle.setQueryCount(queryResult.getQueryCount());
		}
		
		// select columns must be fetched for for the select columns or max
		// rows per page.
		if ((partMask & BUNDLE_MASK_QUERY_SELECT_COLUMNS) > 0) {
			bundle.setSelectColumns(queryResult.getSelectColumns());
		}
		// all schema columns
		if ((partMask & BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE) > 0) {
			bundle.setColumnModels(queryResult.getColumnModels());
		}
		// Max rows per column
		if ((partMask & BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE) > 0) {
			bundle.setMaxRowsPerPage(queryResult.getMaxRowsPerPage());
		}
		return bundle;
	}
	
	/**
	 * Create a QueryNextPageToken from a sql query.
	 * @param sql
	 * @param nextOffset
	 * @param limit
	 * @param isConsistent
	 * @return
	 */
	public static QueryNextPageToken createNextPageToken(SqlQuery sql, Long nextOffset, Long limit, boolean isConsistent) {
		return createNextPageToken(sql.getModel().toString(), nextOffset, limit, isConsistent);
	}

	/**
	 * Create a QueryNextPageToken from a sql string.
	 * @param sql
	 * @param nextOffset
	 * @param limit
	 * @param isConsistent
	 * @return
	 */
	public static QueryNextPageToken createNextPageToken(String sql, Long nextOffset, Long limit, boolean isConsistent) {
		Query query = new Query();
		query.setSql(sql);
		query.setOffset(nextOffset);
		query.setLimit(limit);
		query.setIsConsistent(isConsistent);

		StringWriter writer = new StringWriter(sql.length() + 50);
		XStream xstream = new XStream();
		xstream.alias("Query", Query.class);
		xstream.toXML(query, writer);
		Closer.closeQuietly(writer);
		QueryNextPageToken nextPageToken = new QueryNextPageToken();
		nextPageToken.setToken(writer.toString());
		return nextPageToken;
	}

	/**
	 * Extract a query from a next page token.
	 * @param nextPageToken
	 * @return
	 */
	public static Query createQueryFromNextPageToken(QueryNextPageToken nextPageToken) {
		if (nextPageToken == null || StringUtils.isEmpty(nextPageToken.getToken())) {
			throw new IllegalArgumentException("Next page token cannot be empty");
		}
		try {
			XStream xstream = new XStream();
			xstream.alias("Query", Query.class);
			Query query = (Query) xstream.fromXML(nextPageToken.getToken(), new Query());
			return query;
		} catch (Throwable t) {
			throw new IllegalArgumentException("Not a valid next page token", t);
		}
	}

	/**
	 * Create Query from SQL and a sort list.
	 * @param sql
	 * @param sortList
	 * @return
	 */
	public SqlQuery createQuery(String sql, List<SortItem> sortList) throws EmptySchemaException {
		// First parse the SQL
		QuerySpecification model = parserQuery(sql);
		if (sortList != null && !sortList.isEmpty()) {
			// change the query to use the sort list
			model = SqlElementUntils.convertToSortedQuery(model, sortList);
		}

		String tableId = model.getTableName();
		if (tableId == null) {
			throw new IllegalArgumentException("Could not parse the table name in the sql expression: " + sql);
		}
		// Lookup the column models for this table
		List<ColumnModel> columnModels = columnModelDAO.getColumnModelsForObject(tableId);
		if(columnModels.isEmpty()){
			throw new EmptySchemaException("Table schema is empty for: "+tableId, tableId);
		}	
		return new SqlQuery(model, columnModels, tableId);
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
	public DownloadFromTableResult runConsistentQueryAsStream(
			ProgressCallback<Void> progressCallback, UserInfo user, String sql,
			List<SortItem> sortList, final CSVWriterStream writer,
			boolean includeRowIdAndVersion, final boolean writeHeader)
			throws TableUnavailableException, NotFoundException,
			TableFailedException, TableLockUnavailableException {
		// Convert to a query.
		try {
			final SqlQuery query = createQuery(sql, sortList);

			// Do not include rowId and version if it is not provided (PLFM-2993)
			if (!query.includesRowIdAndVersion()) {
				includeRowIdAndVersion = false;
			}
			// This handler will capture the row data.
			CSVWriterRowHandler handler = new CSVWriterRowHandler(writer,
					query.getSelectColumns(), includeRowIdAndVersion);
			
			if (writeHeader) {
				handler.writeHeader();
			}
			
			// run the query.
			boolean runCount = false;
			boolean isConsistent = true;
			QueryResultBundle result = queryAsStream(progressCallback, user,
					query, handler, runCount, isConsistent);
			// convert the response
			DownloadFromTableResult response = new DownloadFromTableResult();
			response.setHeaders(result.getSelectColumns());
			response.setTableId(result.getQueryResult().getQueryResults().getTableId());
			// pass along the etag.
			response.setEtag(result.getQueryResult().getQueryResults().getEtag());
			return response;
		} catch (EmptySchemaException e) {
			throw new IllegalArgumentException("Table "+e.getTableId()+" has an empty schema");
		}
	}
	
	/**
	 * Run the given query as a stream.
	 * @param callback
	 * @param query
	 * @param rowHandler
	 * @return
	 */
	RowSet runQueryAsStream(ProgressCallback<Void> callback,
			SqlQuery query, RowHandler rowHandler) {
		// Get a connection
		TableIndexDAO indexDao = tableConnectionFactory.getConnection(query.getTableId());
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
	public long runCountQuery(SqlQuery query) {
		try {
			// create the count SQL from the already transformed model.
			String countSql = SqlElementUntils.createCountSql(query.getTransformedModel());
			// Get a connection
			TableIndexDAO indexDao = tableConnectionFactory.getConnection(query.getTableId());
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
}
