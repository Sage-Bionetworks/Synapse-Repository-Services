package org.sagebionetworks.repo.manager.table;

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
	 * Permanently binds the given table version to the last transaction of the table, the table must have at least one change
	 * @param tableId The id with version for the table
	 */
	void linkVersionToLatestTransaction(IdAndVersion tableId);
	
}
