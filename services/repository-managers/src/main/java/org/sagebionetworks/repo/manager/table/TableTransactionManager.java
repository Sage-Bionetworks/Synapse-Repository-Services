package org.sagebionetworks.repo.manager.table;

import java.util.Optional;
import java.util.function.Function;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.IdAndVersion;

public interface TableTransactionManager {

	/**
	 * Executes the given function against the given table within the context of a table transaction.
	 * After the function is executed the table is updated and a message is sent to trigger an update if
	 * a transaction was started.
	 * 
	 * @param <T> The return type for the function
	 * @param user
	 * @param tableId
	 * @param function
	 * @return
	 */
	<T> T executeInTransaction(UserInfo user, String tableId, Function<TableTransactionContext, T> function);
	
	/**
	 * @param tableId
	 * @return A transaction context referring to the last transaction on the given table if any
	 */
	Optional<TableTransactionContext> getLastTransactionContext(String tableId);
	
	/**
	 * Permanently binds the given table version to the transaction associated with the given context
	 * 
	 * @param txContext
	 * @param tableId
	 */
	void linkVersionToTransaction(TableTransactionContext txContext, IdAndVersion tableId);
	
}
