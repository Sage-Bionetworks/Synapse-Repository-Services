package org.sagebionetworks.repo.manager.table;

import java.util.List;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavilableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.cluster.SqlQuery;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.csv.CSVWriterStream;

/**
 * Business logic for table queries.
 *
 */
public interface TableQueryManager {
	/**
	 * Execute a table query.
	 * 
	 * @param user
	 * @param query
	 * @param isConsistent
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws TableUnavilableException
	 * @throws TableFailedException
	 */
	public Pair<QueryResult, Long> query(ProgressCallback<Void> progressCallback, UserInfo user, String query, List<SortItem> sortList, Long offset, Long limit, boolean runQuery,
			boolean runCount, boolean isConsistent) throws DatastoreException, NotFoundException, TableUnavilableException,
			TableFailedException;

	/**
	 * Execute a table query.
	 * 
	 * @param user
	 * @param query
	 * @param isConsistent
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws TableUnavilableException
	 * @throws TableFailedException
	 */
	public Pair<QueryResult, Long> query(ProgressCallback<Void> progressCallback, UserInfo user, SqlQuery query, Long offset, Long limit, boolean runQuery, boolean runCount,
			boolean isConsistent) throws DatastoreException, NotFoundException, TableUnavilableException, TableFailedException;

	/**
	 * get the next page of a query
	 * 
	 * @param user
	 * @param queryPageToken
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws TableUnavilableException
	 * @throws TableFailedException
	 */
	public QueryResult queryNextPage(ProgressCallback<Void> progressCallback, UserInfo user, QueryNextPageToken nextPageToken) throws DatastoreException, NotFoundException,
			TableUnavilableException, TableFailedException;

	/**
	 * Get a query bundle result
	 * 
	 * @param user
	 * @param queryBundle
	 * @return
	 * @throws TableUnavilableException
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws TableFailedException
	 */
	public QueryResultBundle queryBundle(ProgressCallback<Void> progressCallback, UserInfo user, QueryBundleRequest queryBundle) throws DatastoreException, NotFoundException,
			TableUnavilableException, TableFailedException;

	/**
	 * Run the provided SQL query string and stream the results to the passed CSVWriter. This method will stream over
	 * the rows and will not keep the row data in memory. This method can be used to stream over results sets that are
	 * larger than the available system memory, as long as the caller does not hold the resulting rows in memory.
	 * 
	 * @param user
	 * 
	 * @param sql
	 * @param list
	 * @param writer
	 * @param writeHeader
	 * @return
	 * @throws TableUnavilableException
	 * @throws NotFoundException
	 * @throws TableFailedException
	 */
	DownloadFromTableResult runConsistentQueryAsStream(ProgressCallback<Void> progressCallback, UserInfo user, String sql, List<SortItem> list, CSVWriterStream writer,
			boolean includeRowIdAndVersion, boolean writeHeader) throws TableUnavilableException, NotFoundException, TableFailedException;


	/**
	 * Get the maximum number of rows allowed for a single page (get, put, or query) for the given columns.
	 * 
	 * @param models
	 * @return
	 */
	public Long getMaxRowsPerPage(List<ColumnModel> models);
	
	/**
	 * Validate the table is available.
	 * 
	 * @param tableId
	 * @return
	 * @throws NotFoundException
	 *             If the table does not exist
	 * @throws TableUnavilableException
	 *             If the table exists but is currently processing.
	 * @throws TableFailedException
	 *             If the table exists but processing failed.
	 */
	public TableStatus validateTableIsAvailable(String tableId)
			throws NotFoundException, TableUnavilableException,
			TableFailedException;

	Long getMaxRowsPerPageSelectColumns(List<SelectColumn> models);
}
