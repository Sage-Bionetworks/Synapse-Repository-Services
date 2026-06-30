package org.sagebionetworks.repo.manager.grid.synch.handler;

import java.io.IOException;

import org.sagebionetworks.repo.model.grid.GridSession;

/**
 * Factory interface for creating CopyHandler instances. Abstracts the creation
 * of handlers that read from grid copies (CRDT replicas), allowing for
 * different implementations based on session context and enabling dependency
 * injection for testing.
 */
public interface CopyHandlerProvider {

	/**
	 * Creates a new {@link CopyHandler} for the given session. The returned handler
	 * captures a consistent, disk-backed snapshot of the grid rows at
	 * {@code REPEATABLE_READ} isolation before any patch is published, so the merge
	 * can never read its own enqueued writes or a concurrent replica's writes.
	 *
	 * <p>
	 * The caller is responsible for closing the returned handler (preferably via
	 * try-with-resources), which releases the snapshot temp file.
	 *
	 * @param session the grid session containing connection information and user
	 *                context for accessing the copy
	 * @return a CopyHandler instance ready for synchronization
	 * @throws IOException if the row snapshot cannot be captured
	 */
	CopyHandler createCopyHandler(GridSession session) throws IOException;
}
