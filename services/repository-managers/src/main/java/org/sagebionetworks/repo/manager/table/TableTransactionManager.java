package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.dao.asynch.AsynchProgress;
import org.sagebionetworks.repo.model.table.TableUnavailableException;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionRequest;
import org.sagebionetworks.repo.model.table.TableUpdateTransactionResponse;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

/**
 * Abstraction for a table manager to support table update transaction.
 * @author John
 *
 */
public interface TableTransactionManager {

	/**
	 * Update table within a single transaction.
	 * @param progressCallback Use to report progress back the caller.
	 * @param request Defines the transaction.
	 * @return Transaction response.
	 * @throws RecoverableMessageException This exception must be thrown to indicate a job cannot complete at this time and
	 * should be re-tried in the future.
	 */
	TableUpdateTransactionResponse updateTableWithTransaction(
			ProgressCallback<AsynchProgress> progressCallback,
			TableUpdateTransactionRequest request) throws RecoverableMessageException, TableUnavailableException;

}
