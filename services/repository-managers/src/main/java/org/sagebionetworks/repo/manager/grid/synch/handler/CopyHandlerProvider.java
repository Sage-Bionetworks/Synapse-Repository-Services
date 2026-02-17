package org.sagebionetworks.repo.manager.grid.synch.handler;

import org.sagebionetworks.repo.model.grid.GridSession;

/**
 * Factory interface for creating CopyHandler instances. Abstracts the creation
 * of handlers that read from grid copies (CRDT replicas), allowing for
 * different implementations based on session context and enabling dependency
 * injection for testing.
 */
public interface CopyHandlerProvider {

	/**
	 * Creates a new CopyHandler for reading the state of a grid copy during
	 * synchronization. The handler provides read-only access to the copy's schema,
	 * rows, and CRDT metadata.
	 *
	 * @param session the grid session containing connection information and user
	 *                context for accessing the copy
	 * @return a CopyHandler instance for reading from the specified copy
	 */
	CopyHandler createCopyHandler(GridSession session);
}
