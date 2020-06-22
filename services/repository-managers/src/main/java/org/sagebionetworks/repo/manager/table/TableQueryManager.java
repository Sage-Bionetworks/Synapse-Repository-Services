package org.sagebionetworks.repo.manager.table;

import java.util.List;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.DownloadFromTableRequest;
import org.sagebionetworks.repo.model.table.DownloadFromTableResult;
import org.sagebionetworks.repo.model.table.Query;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.QueryOptions;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableStatus;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.util.csv.CSVWriterStream;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;

/**
 * Business logic for table queries.
 *
 */
public interface TableQueryManager {
	/**
	 * Execute a single page of a table query.
	 * The query results of this call will never exceed the
	 * maximum number of rows allowed. 
	 * @param user
	 * @param query
	 * @param selectedFacets
	 *
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws TableUnavailableException
	 * @throws TableFailedException
	 * @throws ParseException 
	 * @throws TableLockUnavailableException 
	 */
	public QueryResultBundle querySinglePage(ProgressCallback progressCallback, UserInfo user, Query query,	QueryOptions options) throws DatastoreException, NotFoundException, TableUnavailableException,
			TableFailedException, ParseException, LockUnavilableException;

	/**
	 * get the next page of a query
	 * 
	 * @param user
	 * @param queryPageToken
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 * @throws TableUnavailableException
	 * @throws TableFailedException
	 * @throws ParseException 
	 * @throws TableLockUnavailableException 
	 */
	public QueryResult queryNextPage(ProgressCallback progressCallback, UserInfo user, QueryNextPageToken nextPageToken) throws DatastoreException, NotFoundException,
			TableUnavailableException, TableFailedException, ParseException, LockUnavilableException;

	/**
	 * Get a query bundle result
	 * 
	 * @param user
	 * @param queryBundle
	 * @return
	 * @throws TableUnavailableException
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws TableFailedException
	 * @throws ParseException 
	 * @throws TableLockUnavailableException 
	 */
	public QueryResultBundle queryBundle(ProgressCallback progressCallback, UserInfo user, QueryBundleRequest queryBundle) throws DatastoreException, NotFoundException,
			TableUnavailableException, TableFailedException, ParseException, LockUnavilableException;

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
	 * @throws TableUnavailableException
	 * @throws NotFoundException
	 * @throws TableFailedException
	 * @throws TableLockUnavailableException 
	 */
	DownloadFromTableResult runQueryDownloadAsStream(ProgressCallback progressCallback, UserInfo user, DownloadFromTableRequest request, CSVWriterStream writer) throws TableUnavailableException, NotFoundException, TableFailedException, LockUnavilableException;


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
	 * @throws TableUnavailableException
	 *             If the table exists but is currently processing.
	 * @throws TableFailedException
	 *             If the table exists but processing failed.
	 */
	public TableStatus validateTableIsAvailable(String tableId)
			throws NotFoundException, TableUnavailableException,
			TableFailedException;
}
