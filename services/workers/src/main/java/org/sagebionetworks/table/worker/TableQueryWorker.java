package org.sagebionetworks.table.worker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.repo.manager.table.TableQueryManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.table.TableExceptionTranslator;
import org.sagebionetworks.repo.model.table.QueryBundleRequest;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.model.table.TableFailedException;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.worker.AsyncJobProgressCallback;
import org.sagebionetworks.worker.AsyncJobRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.semaphore.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This worker will stream the results of a table SQL query to a local CSV file
 * and upload the file to S3 as a FileHandle.
 * 
 */
@Service
public class TableQueryWorker implements AsyncJobRunner<QueryBundleRequest, QueryResultBundle> {

	static private Logger log = LogManager.getLogger(TableQueryWorker.class);

	@Autowired
	private TableQueryManager tableQueryManager;
	@Autowired
	private TableExceptionTranslator tableExceptionTranslator;

	@Override
	public Class<QueryBundleRequest> getRequestType() {
		return QueryBundleRequest.class;
	}
	
	@Override
	public Class<QueryResultBundle> getResponseType() {
		return QueryResultBundle.class;
	}
	
	@Override
	public QueryResultBundle run(String jobId, UserInfo user, QueryBundleRequest request, AsyncJobProgressCallback jobProgressCallback) throws RecoverableMessageException, Exception {
		try {
			return tableQueryManager.queryBundle(jobProgressCallback, user, request);
		} catch (TableUnavailableException | LockUnavilableException e) {
			// This just means we cannot do this right now.  We can try again later.
			jobProgressCallback.updateProgress("Waiting for the table index to become available...", 0L, 100L);
			throw new RecoverableMessageException();
		} catch (TableFailedException e) {
			// This means we cannot use this table
			throw e;
		} catch (Throwable e) {
			log.error("Failed", e);
			// Attempt to translate the exception into a 'user-friendly' message.
			RuntimeException translatedException = tableExceptionTranslator.translateException(e);
			throw translatedException;
		}
	}
}
