package org.sagebionetworks.dynamo;

/**
 * A single write operation (c, u, d) on a Dynamo table.
 *
 * @author Eric Wu
 */
public interface DynamoWriteOperation extends Comparable<DynamoWriteOperation> {

	/**
	 * Executes the write.
	 *
	 * @return True, if write succeeds. False, if write fails.
	 */
	boolean write();

	/**
	 * Best-effort rollback.
	 */
	void restore();
}
