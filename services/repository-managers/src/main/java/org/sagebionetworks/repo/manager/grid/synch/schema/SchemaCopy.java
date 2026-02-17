package org.sagebionetworks.repo.manager.grid.synch.schema;

import java.util.List;

import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.synch.core.Copy;

/**
 * Represents the copy (CRDT replica) side of Phase 1 schema synchronization.
 * Provides access to columns in the copy schema and operations for modifying
 * the copy based on synchronization with the source schema.
 *
 * <p>
 * During Phase 1 of the synchronization process (see
 * {@link GridSynchronizationManagerImpl}), this interface is used by
 * {@link SynchronizationLogic} to:
 * <ul>
 * <li>Stream copy columns for comparison with source columns</li>
 * <li>Remove columns from the copy that were deleted from the source (pulling
 * external deletions)</li>
 * <li>Add columns to the copy that were added to the source (pulling external
 * additions)</li>
 * <li>Track which columns were deleted by the user (for pushing user deletions
 * to source)</li>
 * </ul>
 *
 * <p>
 * This interface extends {@link Copy} with {@link ColumnCopyItem} as the copy
 * item type and {@link ColumnSourceItem} as the source item type, enabling the
 * generic {@link SynchronizationLogic#synchronize} algorithm to work with
 * schema\-level synchronization.
 *
 * <p>
 * After Phase 1 synchronization completes, {@link #getFinalSchema()} provides
 * the synchronized schema that will be used during Phase 2 (row
 * synchronization). This schema is used to:
 * <ul>
 * <li>Map column names to vector indices for CRDT operations</li>
 * <li>Define the structure for comparing and merging row data</li>
 * <li>Ensure copy and source agree on the schema before synchronizing rows</li>
 * </ul>
 *
 * @see ColumnCopyItem the type of columns in the copy
 * @see ColumnSourceItem the type of columns in the source
 * @see SchemaSource the corresponding source side of schema synchronization
 * @see SchemaCopyImpl the implementation that bridges with
 *      {@link IntendedChangePublisher}
 */
public interface SchemaCopy extends Copy<ColumnCopyItem, ColumnSourceItem>, AutoCloseable {

	/**
	 * Gets the final synchronized schema after Phase 1 schema synchronization
	 * completes. This schema represents the agreed\-upon column structure between
	 * copy and source, combining:
	 * <ul>
	 * <li>Columns that existed in both copy and source</li>
	 * <li>User\-added columns pushed to the source</li>
	 * <li>Externally\-added columns pulled from the source</li>
	 * </ul>
	 *
	 * <p>
	 * This schema is used during Phase 2 (row synchronization) to:
	 * <ul>
	 * <li>Map column names to vector indices in CRDT row operations</li>
	 * <li>Validate that cell data matches the synchronized schema structure</li>
	 * <li>Enable cell\-level comparison during row merging</li>
	 * </ul>
	 *
	 * <p>
	 * The schema includes CRDT metadata (vector indices, RGA node IDs) needed to
	 * apply row changes to the CRDT replica while maintaining convergence
	 * guarantees.
	 *
	 * @return ordered list of columns in the synchronized schema
	 */
	List<Column> getFinalSchema();

}
