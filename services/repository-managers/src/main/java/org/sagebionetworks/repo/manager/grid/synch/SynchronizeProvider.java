package org.sagebionetworks.repo.manager.grid.synch;

import java.util.List;

import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangePublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.synch.core.SynchronizationLogic;
import org.sagebionetworks.repo.manager.grid.synch.handler.CopyHandler;
import org.sagebionetworks.repo.manager.grid.synch.handler.SourceHandler;
import org.sagebionetworks.repo.manager.grid.synch.io.RowReader;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopy;
import org.sagebionetworks.repo.manager.grid.synch.row.RowMerge;
import org.sagebionetworks.repo.manager.grid.synch.row.RowSource;
import org.sagebionetworks.repo.manager.grid.synch.schema.SchemaCopy;
import org.sagebionetworks.repo.manager.grid.synch.schema.SchemaSource;

/**
 * Factory interface for creating synchronization components (Copy, Source, and
 * Merge implementations) used during grid synchronization. This provider
 * abstracts the creation of concrete implementations, allowing for dependency
 * injection and testability.
 */
public interface SynchronizeProvider {

	/**
	 * Creates a Copy implementation for schema synchronization during Phase 1.
	 *
	 * @param intendedChangePublisher publisher for recording intended schema
	 *                                changes to the copy
	 * @param reader                  handler providing access to the copy's current
	 *                                schema
	 * @return a SchemaCopy instance for synchronizing schema columns
	 */
	SchemaCopy getSchemaCopy(IntendedChangePublisher intendedChangePublisher, CopyHandler reader);

	/**
	 * Creates a Source implementation for schema synchronization during Phase 1.
	 *
	 * @param handler handler providing access to the source's current schema
	 * @return a SchemaSource instance for synchronizing schema columns
	 */
	SchemaSource getSchemaSource(SourceHandler handler);

	/**
	 * Creates a Copy implementation for row synchronization during Phase 2.
	 *
	 * @param intendedChangePublisher publisher for recording intended row changes
	 *                                to the copy
	 * @param finalSchema             the synchronized schema from Phase 1 used for
	 *                                row operations
	 * @param reader                  handler providing access to the copy's current
	 *                                rows
	 * @return a RowCopy instance for synchronizing row data
	 */
	RowCopy getRowCopy(IntendedChangePublisher intendedChangePublisher, List<Column> finalSchema, CopyHandler reader);

	/**
	 * Creates a Source implementation for row synchronization during Phase 2.
	 *
	 * @param sourceReader reader providing disk-based access to source rows (O(n)
	 *                     memory usage)
	 * @param handler      handler for applying changes to the source
	 * @return a RowSource instance for synchronizing row data
	 */
	RowSource getRowSource(RowReader sourceReader, SourceHandler handler);

	/**
	 * Creates a Merge implementation for resolving conflicts during row
	 * synchronization in Phase 2. Determines how to merge changes when a row exists
	 * in both copy and source but has different values.
	 *
	 * @param logic                   the synchronization logic for recursive
	 *                                merging of cell-level changes
	 * @param intendedChangePublisher publisher for recording merge results
	 * @param finalSchema             the synchronized schema from Phase 1
	 * @param reader                  handler for reading copy state
	 * @param handler                 handler for applying changes to source
	 * @return a RowMerge instance for resolving row conflicts
	 */
	RowMerge getRowMerge(SynchronizationLogic logic, IntendedChangePublisher intendedChangePublisher,
			List<Column> finalSchema, CopyHandler reader, SourceHandler handler);

}
