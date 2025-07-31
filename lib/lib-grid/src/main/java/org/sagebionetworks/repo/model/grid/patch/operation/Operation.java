package org.sagebionetworks.repo.model.grid.patch.operation;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

/**
 * Read/write abstraction shared by all patch operations. See: <a href=
 * "https://jsonjoy.com/specs/json-crdt-patch/patch-document/patch-structure">patch-structure</a>
 */
public interface Operation<T> extends OperationView<T> {

	/**
	 * Set the operation's ID.
	 * @param timestamp
	 * @return
	 */
	T setOperationId(LogicalTimestamp timestamp);

}
