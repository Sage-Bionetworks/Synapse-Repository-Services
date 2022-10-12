package org.sagebionetworks.table.worker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.TableExceptionTranslator;
import org.sagebionetworks.repo.model.table.QueryNextPageToken;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.worker.AsyncJobProgressCallback;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This worker will stream the results of a table SQL query to a local CSV file and upload the file
 * to S3 as a FileHandle.
 */
@Service
public class TableQueryNextPageWorker implements AsyncJobRunner<QueryNextPageToken, QueryResult> {

	static private Logger log = LogManager.getLogger(TableQueryNextPageWorker.class);

	@Autowired
	private TableQueryManager tableQueryManger;
	@Autowired
	private TableExceptionTranslator tableExceptionTranslator;
	
	@Override
	public Class<QueryNextPageToken> getRequestType() {
		return QueryNextPageToken.class;
	}
	
	@Override
	public Class<QueryResult> getResponseType() {
		return QueryResult.class;
	}
	
	@Override
	public QueryResult run(String jobId, UserInfo user, QueryNextPageToken request, AsyncJobProgressCallback jobProgressCallback) throws RecoverableMessageException, Exception {
		try {
			return tableQueryManger.queryNextPage(jobProgressCallback, user, request);
		} catch (TableUnavailableException | LockUnavilableException e) {
			// This just means we cannot do this right now.  We can try again later.
			jobProgressCallback.updateProgress("Waiting for the table index to become available...", 0L, 100L);
			throw new RecoverableMessageException();
		} catch (TableFailedException e) {
			// This means we cannot use this table
			throw e;
		} catch(Throwable e){
			log.error("Worker failed:", e);
			// Attempt to translate the exception into a 'user-friendly' message.
			RuntimeException translatedException = tableExceptionTranslator.translateException(e);
			throw translatedException;
		}
	}

}
