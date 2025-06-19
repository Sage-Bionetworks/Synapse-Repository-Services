package org.sagebionetworks.repo.model.grid.patch.operation;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

/**
 * Abstraction shared by all patch operations. See: <a href=
 * "https://jsonjoy.com/specs/json-crdt-patch/patch-document/patch-strucure">patch-strucure</a>
 */
public interface Operation<T> {

	OperationType getType();

	/**
	 * Get the operation's ID
	 * 
	 * @return
	 */
	LogicalTimestamp getOperationId();
	
	/**
	 * Set the operation's ID.
	 * @param timestamp
	 * @return
	 */
	T setOperationId(LogicalTimestamp timestamp);
	

	/**
	 * The span is the number of cycles consumed by an operation.
	 * 
	 * @return
	 */
	long getSpan();

	/**
	 * Generate the next operation Id.
	 * @return
	 */
	default LogicalTimestamp nextOperationId() {
		return LogicalTimestamp.newIncrement(getOperationId(), getSpan());
	}

}
