package org.sagebionetworks.dynamo;

/**
 * A single write operation (c, u, d) on a Dynamo table.
 */
public interface DynamoWriteOperation extends Comparable<DynamoWriteOperation> {

	/**
	 * Executes the write.
	 *
	 * @param step Execution step number when this is executed in a list of operations.
	 * @return True, if write succeeds. False, if write fails.
	 */
	boolean write(int step);

	/**
	 * Best-effort rollback.
	 */
	void restore(int step);
}
