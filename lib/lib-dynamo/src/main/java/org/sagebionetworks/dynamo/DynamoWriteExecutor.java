package org.sagebionetworks.dynamo;


/**
 * Executes a list of write operations, wrapped as one write execution, on a Dynamo table.
 */
public interface DynamoWriteExecutor {

	/**
	 * Executes a list of write operations, wrapped as one write execution, on a Dynamo table.
	 * The list of write operations should be treated as a atomic unit of related writes.
	 *
	 * @return True, if the whole list is successfully executed; false, otherwise.
	 */
	public boolean execute(DynamoWriteExecution execution); 
}
