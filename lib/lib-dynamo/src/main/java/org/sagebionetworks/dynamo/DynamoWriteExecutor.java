package org.sagebionetworks.dynamo;

import java.util.List;

/**
 * Executes a list of write operations on a Dynamo table.
 *
 * @author Eric Wu
 */
public interface DynamoWriteExecutor {

	/**
	 * Execute one operation.
	 */
	boolean execute(DynamoWriteOperation op);

	/**
	 * Executes a list of write operations. The list should be treated as a atomic unit of related updates.
	 *
	 * @return True, if the whole list is successfully executed; false, otherwise.
	 */
	boolean execute(List<DynamoWriteOperation> opList);
}
