package org.sagebionetworks.repo.manager.table;

import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingCallable;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dao.table.RowAndHeaderHandler;
import org.sagebionetworks.repo.model.table.ColumnMapper;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.SQLTranslatorUtils;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.MysqlFunction;
import org.sagebionetworks.table.query.model.Pagination;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.SqlDirective;
import org.sagebionetworks.table.query.model.visitors.GetTableNameVisitor;
import org.sagebionetworks.table.query.util.SqlElementUntils;
import org.sagebionetworks.util.Closer;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.ValidateArgument;
import org.sagebionetworks.util.csv.CSVWriterStream;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
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

	@Override
	public Pair<QueryResult, Long> query(ProgressCallback<Void> progressCallback, UserInfo user, String query, List<SortItem> sortList, Long offset, Long limit, boolean runQuery,
			boolean runCount, boolean isConsistent) throws DatastoreException, NotFoundException, TableUnavilableException,
			TableFailedException {
		return query(progressCallback, user, createQuery(query, sortList), offset, limit, runQuery, runCount, isConsistent);
	}

	@Override
	public Pair<QueryResult, Long> query(ProgressCallback<Void> progressCallback, UserInfo user, SqlQuery query, Long offset, Long limit, boolean runQuery,
			boolean runCount, boolean isConsistent) throws DatastoreException, NotFoundException, TableUnavilableException,
			TableFailedException {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(query, "SqlQuery");
		// Validate the user has read access on this object
		tableManagerSupport.validateTableReadAccess(user, query.getTableId());
		// Does this table exist?
		if(query.getTableSchema() == null || query.getTableSchema().isEmpty()){
			// there are no columns for this table so the table does not actually exist.
			// for this case the caller expects an empty result set.  See PLFM-2636
			QueryResult result = new QueryResult();
			RowSet emptyRowSet = new RowSet();
			emptyRowSet.setTableId(query.getTableId());
			result.setQueryResults(emptyRowSet);
			return Pair.create(runQuery ? result : null, runCount ? 0L : null);
		}

		SqlQuery paginatedQuery = null;
		Long maxRowsPerPage = null;
		boolean oneRowWasAdded = false;
		if (runQuery || (runCount && query.isAggregatedResult())) {
			maxRowsPerPage = getMaxRowsPerPage(query.getSelectColumnModels());
			if (maxRowsPerPage == null || maxRowsPerPage == 0) {
				maxRowsPerPage = 100L;
			}
			
			// limits are a bit complicated. We want to get the minimum of maxRowsPerPage and limit in query, but
			// if we use maxRowsPerPage, we want to add 1 row, which we later remove and use as a marker to see if there
			// are more results
			
			long limitFromRequest = (limit != null) ? limit : Long.MAX_VALUE;
			long offsetFromRequest = (offset != null) ? offset : 0L;
			
			long limitFromQuery = Long.MAX_VALUE;
			long offsetFromQuery = 0L;
			
			Pagination pagination = query.getModel().getTableExpression().getPagination();
			if (pagination != null) {
				if (pagination.getLimit() != null) {
					limitFromQuery = pagination.getLimit();
				}
				if (pagination.getOffset() != null) {
					offsetFromQuery = pagination.getOffset();
				}
			}
			
			long paginatedOffset = offsetFromQuery + offsetFromRequest;
			// adjust the limit from the query based on the additional offset (assume Long.MAX_VALUE - offset is still
			// always large enough)
			limitFromQuery = Math.max(0, limitFromQuery - offsetFromRequest);
			
			long paginatedLimit = Math.min(limitFromRequest, limitFromQuery);
			if (paginatedLimit > maxRowsPerPage) {
				paginatedLimit = maxRowsPerPage + 1;
				oneRowWasAdded = true;
			}
			
			paginatedQuery = createPaginatedQuery(query, paginatedOffset, paginatedLimit);
		}

		SqlQuery countQuery = null;
		if (runCount) {
			// there are two ways of getting counts. One is to replace the select clause with count(*) and another is to
			// use SQL_CALC_FOUND_ROWS & FOUND_ROWS()
			// the count(*) is usually more efficient, but it is almost impossible to get that right if the query to
			// count is an aggregate itself.
			// So, we use count(*) if there is no aggregate and SQL_CALC_FOUND_ROWS if there is
			if (query.isAggregatedResult()) {
				// add the SQL_CALC_FOUND_ROWS to the query
				SelectList paginatedSelectList = paginatedQuery.getModel().getSelectList();
				if (BooleanUtils.isTrue(paginatedQuery.getModel().getSelectList().getAsterisk())) {
					// bug in mysql, SQL_CALC_FOUND_ROWS does not work when the select is '*'. Expand to the known
					// columns
					paginatedSelectList = new SelectList(Lists.transform(paginatedQuery.getSelectColumnModels().getSelectColumns(),
							new Function<SelectColumn, DerivedColumn>() {
								@Override
								public DerivedColumn apply(SelectColumn input) {
									return SQLTranslatorUtils.createDerivedColumn(input.getName());
								}
							}));
				}
				QuerySpecification paginatedModel = new QuerySpecification(SqlDirective.SQL_CALC_FOUND_ROWS, paginatedQuery.getModel()
						.getSetQuantifier(), paginatedSelectList, paginatedQuery.getModel().getTableExpression());
				paginatedQuery = new SqlQuery(paginatedModel, paginatedQuery.getTableSchema(), paginatedQuery.getTableId());
				// and make the count query "SELECT FOUND_ROWS()"
				SelectList selectList = new SelectList(Lists.newArrayList(SQLTranslatorUtils.createDerivedColumn(MysqlFunction.FOUND_ROWS)));
				countQuery = new SqlQuery(new QuerySpecification(null, null, selectList, null), paginatedQuery.getTableSchema(),
						paginatedQuery.getTableId());
			} else {
				QuerySpecification model = SqlElementUntils.convertToCountQuery(query.getModel());
				// Lookup the column models for this table
				List<ColumnModel> columnModels = columnModelDAO.getColumnModelsForObject(query.getTableId());
				countQuery = new SqlQuery(model, columnModels, query.getTableId());
			}
		}

		RowSet rowSet = null;
		Long count = null;
		// If this is a consistent read then we need a read lock
		if (isConsistent) {
			// A consistent query is only run if the table index is available and up-to-date
			// with the table state. A read-lock on the index will be held while the query is run.
			Pair<RowSet, Long> result = runConsistentQuery(progressCallback, paginatedQuery, countQuery);
			rowSet = result.getFirst();
			count = result.getSecond();
		} else {
			// This path queries the table index regardless of the state of the index and without a
			// read-lock.
			if (paginatedQuery != null) {
				rowSet = query(progressCallback, paginatedQuery);
			}
			if (countQuery != null) {
				RowSet countResult = query(progressCallback, countQuery);
				List<Row> rows = countResult.getRows();
				if (!rows.isEmpty()) {
					List<String> values = rows.get(0).getValues();
					if (!values.isEmpty()) {
						count = Long.parseLong(values.get(0));
					}
				}
			}
		}

		// post processing for query result
		QueryResult queryResult = null;
		if (runQuery && rowSet != null) {
			QueryNextPageToken nextPageToken = null;
			if (oneRowWasAdded) {
				if (rowSet.getRows().size() > maxRowsPerPage) {
					// we need to limit the rowSet to maxRowsPerPage and set the next page token
					rowSet.setRows(Lists.newArrayList(rowSet.getRows().subList(0, maxRowsPerPage.intValue())));
					nextPageToken = createNextPageToken(query, (offset == null ? 0 : offset) + maxRowsPerPage, limit, isConsistent);
				}
			}

			queryResult = new QueryResult();
			queryResult.setQueryResults(rowSet);
			queryResult.setNextPageToken(nextPageToken);
		}

		// post processing for count. When a limit and/or offset is specified in a query, count(*) just ignores those,
		// since it assumes the limit & offset apply to the one row count(*) returns. In actuality, we want to apply
		// that limit & offset to the count itself. We do that here manually.
		if (runCount && count != null) {
			Pagination pagination = query.getModel().getTableExpression().getPagination();
			if (pagination != null) {
				if (pagination.getOffset() != null) {
					long offsetForCount = pagination.getOffset();
					count = Math.max(0, count - offsetForCount);
				}
				if (pagination.getLimit() != null) {
					long limitForCount = pagination.getLimit();
					count = Math.min(limitForCount, count);
				}
			}
		}

		return Pair.create(queryResult, count);
	}
	
	@Override
	public QueryResult queryNextPage(ProgressCallback<Void> progressCallback, UserInfo user, QueryNextPageToken nextPageToken) throws DatastoreException, NotFoundException,
			TableUnavilableException, TableFailedException {
		Query query = createQueryFromNextPageToken(nextPageToken);
		Pair<QueryResult, Long> queryResult = query(progressCallback, user, query.getSql(), null, query.getOffset(), query.getLimit(), true,
				false, query.getIsConsistent());
		return queryResult.getFirst();
	}

	@Override
	public QueryResultBundle queryBundle(ProgressCallback<Void> progressCallback, UserInfo user, QueryBundleRequest queryBundle) throws DatastoreException, NotFoundException,
			TableUnavilableException, TableFailedException {
		ValidateArgument.required(queryBundle.getQuery(), "query");
		ValidateArgument.required(queryBundle.getQuery().getSql(), "query.sql");

		QueryResultBundle bundle = new QueryResultBundle();
		// The SQL query is need for the actual query, select columns, and max rows per page.
		SqlQuery sqlQuery = createQuery(queryBundle.getQuery().getSql(), queryBundle.getQuery().getSort());

		// query
		long partMask = -1L; // default all
		if (queryBundle.getPartMask() != null) {
			partMask = queryBundle.getPartMask();
		}
		boolean runQuery = ((partMask & BUNDLE_MASK_QUERY_RESULTS) != 0);
		boolean runCount = ((partMask & BUNDLE_MASK_QUERY_COUNT) != 0);
		if (runQuery || runCount) {
			Pair<QueryResult, Long> queryResult = query(progressCallback, user, sqlQuery, queryBundle.getQuery().getOffset(),
					queryBundle.getQuery().getLimit(), runQuery, runCount, BooleanUtils.isNotFalse(queryBundle.getQuery().getIsConsistent()));
			bundle.setQueryResult(queryResult.getFirst());
			bundle.setQueryCount(queryResult.getSecond());
		}
		// select columns must be fetched for for the select columns or max rows per page.
		if ((partMask & BUNDLE_MASK_QUERY_SELECT_COLUMNS) > 0) {
			bundle.setSelectColumns(sqlQuery.getSelectColumnModels().getSelectColumns());
		}
		// all schema columns
		if ((partMask & BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE) > 0) {
			bundle.setColumnModels(sqlQuery.getTableSchema());
		}
		// Max rows per column
		if ((partMask & BUNDLE_MASK_QUERY_MAX_ROWS_PER_PAGE) > 0) {
			bundle.setMaxRowsPerPage(getMaxRowsPerPage(sqlQuery.getSelectColumnModels()));
		}
		return bundle;
	}

	public static final Charset UTF8 = Charset.forName("UTF-8");
	
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

	private SqlQuery createQuery(String sql, List<SortItem> sortList) {
		// First parse the SQL
		QuerySpecification model = parserQuery(sql);
		if (sortList != null && !sortList.isEmpty()) {
			// change the query to use the sort list
			model = SqlElementUntils.convertToSortedQuery(model, sortList);
		}

		String tableId = model.doVisit(new GetTableNameVisitor()).getTableName();
		if (tableId == null) {
			throw new IllegalArgumentException("Could not parse the table name in the sql expression: " + sql);
		}
		// Lookup the column models for this table
		List<ColumnModel> columnModels = columnModelDAO.getColumnModelsForObject(tableId);
		return new SqlQuery(model, columnModels, tableId);
	}

	private SqlQuery createPaginatedQuery(SqlQuery query, Long offset, Long limit) {
		QuerySpecification model;
		try {
			model = SqlElementUntils.convertToPaginatedQuery(query.getModel(), offset, limit);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
		// Lookup the column models for this table
		List<ColumnModel> columnModels = columnModelDAO.getColumnModelsForObject(query.getTableId());
		return new SqlQuery(model, columnModels, query.getTableId());
	}

	/**
	 * Run a consistent query. All resulting rows will be loading into memory at one time with this method.
	 * 
	 * @param tableId
	 * @param query
	 * @return
	 * @throws TableUnavilableException
	 * @throws NotFoundException
	 * @throws TableFailedException
	 */
	private Pair<RowSet, Long> runConsistentQuery(ProgressCallback<Void> progressCallback, final SqlQuery query, final SqlQuery countQuery) throws TableUnavilableException,
			NotFoundException, TableFailedException {
		List<QueryHandler> queryHandlers = Lists.newArrayList();
		final Object[] output = { null, null };
		if (query != null) {
			final RowSet results = new RowSet();
			output[0] = results;
			final List<Row> rows = new LinkedList<Row>();
			results.setRows(rows);
			results.setTableId(query.getTableId());
			// Stream the results but keep them in memory.
			queryHandlers.add(new QueryHandler(query, new RowAndHeaderHandler() {

				@Override
				public void writeHeader() {
					results.setHeaders(query.getSelectColumnModels().getSelectColumns());
				}

				@Override
				public void nextRow(Row row) {
					rows.add(row);
				}

				@Override
				public void setEtag(String etag) {
					results.setEtag(etag);
				}
			}));
		}

		if (countQuery != null) {
			queryHandlers.add(new QueryHandler(countQuery, new RowAndHeaderHandler() {

				@Override
				public void writeHeader() {
					// no-op
				}

				@Override
				public void nextRow(Row row) {
					if (!row.getValues().isEmpty()) {
						output[1] = Long.parseLong(row.getValues().get(0));
					}
				}

				@Override
				public void setEtag(String etag) {
				}
			}));
		}

		runConsistentQueryAsStream(progressCallback, queryHandlers);

		return Pair.create((RowSet) output[0], (Long) output[1]);
	}
	
	/**
	 * Run a consistent query and stream the results. Use this method to avoid loading all rows into memory at one time.
	 * 
	 * @param query
	 * @param handler
	 * @return
	 * @throws TableUnavilableException
	 * @throws NotFoundException
	 * @throws TableFailedException
	 */
	private void runConsistentQueryAsStream(ProgressCallback<Void> progressCallback, final List<QueryHandler> queryHandlers) throws TableUnavilableException, NotFoundException,
			TableFailedException {
		if (queryHandlers.isEmpty()) {
			return;
		}

		final String tableId = queryHandlers.get(0).getQuery().getTableId();
		try {
			// Run with a read lock.
			tableManagerSupport.tryRunWithTableNonexclusiveLock(progressCallback, tableId, READ_LOCK_TIMEOUT_SEC, new ProgressingCallable<String, Void>() {
				@Override
				public String call(final ProgressCallback<Void> callback) throws Exception {
					// We can only run this query if the table is available.
					final TableStatus status = validateTableIsAvailable(tableId);
					// We can only run this
					final TableIndexDAO indexDao = tableConnectionFactory.getConnection(tableId);
					indexDao.executeInReadTransaction(new TransactionCallback<Void>() {
						@Override
						public Void doInTransaction(TransactionStatus transactionStatus) {
							for (QueryHandler queryHandler : queryHandlers) {
								if (!queryHandler.getQuery().getTableId().equals(tableId)) {
									throw new IllegalArgumentException("All queries should be on the same table, but " + tableId + " and "
											+ queryHandler.getQuery().getTableId());
								}
								indexDao.queryAsStream(callback, queryHandler.getQuery(), queryHandler.getHandler());
								queryHandler.getHandler().setEtag(status.getLastTableChangeEtag());
							}
							return null;
						}
					});
					return null;
				}
			});
		} catch (LockUnavilableException e) {
			throw new TableUnavilableException(tableManagerSupport.getTableStatusOrCreateIfNotExists(tableId));
		} catch(TableUnavilableException e){
			throw e;
		} catch (TableFailedException e) {
			throw e;
		} catch (NotFoundException e) {
			throw e;
		} catch (Exception e) {
			// All other exception are converted to generic datastore.
			throw new DatastoreException(e);
		}
	}
	
	/**
	 * 
	 * @param sql
	 * @param writer
	 * @return The resulting RowSet will not contain any
	 * @throws TableUnavilableException
	 * @throws NotFoundException
	 * @throws TableFailedException
	 */
	@Override
	public DownloadFromTableResult runConsistentQueryAsStream(ProgressCallback<Void> progressCallback, UserInfo user, String sql, List<SortItem> sortList,
			final CSVWriterStream writer, boolean includeRowIdAndVersion, final boolean writeHeader) throws TableUnavilableException,
			NotFoundException, TableFailedException {
		// Convert to a query.
		final SqlQuery query = createQuery(sql, sortList);

		// Validate the user has read access on this object
		tableManagerSupport.validateTableReadAccess(user, query.getTableId());

		if(includeRowIdAndVersion && query.isAggregatedResult()){
			// PLFM-2993: in the case of an aggregated result, we cannot return row id and row versions. Just don't
			// return them if it is an aggregated query
			includeRowIdAndVersion = false;
		}
		final DownloadFromTableResult repsonse = new DownloadFromTableResult();
		final boolean includeRowIdAndVersionFinal = includeRowIdAndVersion;
		repsonse.setTableId(query.getTableId());
		repsonse.setHeaders(query.getSelectColumnModels().getSelectColumns());

		runConsistentQueryAsStream(progressCallback, Collections.singletonList(new QueryHandler(query, new RowAndHeaderHandler() {
			@Override
			public void writeHeader() {
				if (writeHeader) {
					String[] csvHeaders = TableModelUtils.createColumnNameHeader(query.getSelectColumnModels().getSelectColumns(),
							includeRowIdAndVersionFinal);
					writer.writeNext(csvHeaders);
				}
			}

			@Override
			public void nextRow(Row row) {
				String[] array = TableModelUtils.writeRowToStringArray(row, includeRowIdAndVersionFinal);
				writer.writeNext(array);
			}

			@Override
			public void setEtag(String etag) {
				repsonse.setEtag(etag);
			}
		})));
		return repsonse;
	}
	
	/**
	 * Run the actual query.
	 * @param query
	 * @return
	 */
	private RowSet query(ProgressCallback<Void> callback, SqlQuery query) {
		// Get a connection
		TableIndexDAO indexDao = tableConnectionFactory.getConnection(query.getTableId());
		return indexDao.query(callback, query);
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
	public Long getMaxRowsPerPage(ColumnMapper columnMapper) {
		// Calculate the size
		int maxRowSizeBytes = TableModelUtils.calculateMaxRowSizeForColumnModels(columnMapper);
		if (maxRowSizeBytes < 1)
			return null;
		return (long) (this.maxBytesPerRequest / maxRowSizeBytes);
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
	public TableStatus validateTableIsAvailable(String tableId) throws NotFoundException, TableUnavilableException, TableFailedException {
		final TableStatus status = tableManagerSupport.getTableStatusOrCreateIfNotExists(tableId);
		switch(status.getState()){
		case AVAILABLE:
			return status;
		case PROCESSING:
			// When the table is not available, we communicate the current status of the
			// table in this exception.
			throw new TableUnavilableException(status);
		default:
		case PROCESSING_FAILED:
			// When the table is in a failed state, we communicate the current status of the
			// table in this exception.
			throw new TableFailedException(status);
		}
	}
}
