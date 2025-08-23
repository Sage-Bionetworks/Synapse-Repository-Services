package org.sagebionetworks.repo.model.grid.patch.operation;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

/**
 * Immutable abstraction shared by all patch operations. See: <a href=
 * "https://jsonjoy.com/specs/json-crdt-patch/patch-document/patch-structure">patch-structure</a>
 */
public interface Operation {


	OperationType getType();

	/**
	 * Get the operation's ID
	 *
	 * @return
	 */
	LogicalTimestamp getOperationId();

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
