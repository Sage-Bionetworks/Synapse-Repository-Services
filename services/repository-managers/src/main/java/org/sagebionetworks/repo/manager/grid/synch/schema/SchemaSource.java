package org.sagebionetworks.repo.manager.grid.synch.schema;

import org.sagebionetworks.repo.manager.grid.synch.core.Source;

/**
 * Represents the source of truth side of Phase 1 schema synchronization.
 * Provides access to columns in the source schema and operations for modifying
 * the source based on synchronization with the copy schema.
 *
 * <p>
 * During Phase 1 of the synchronization process (see
 * {@link GridSynchronizationManagerImpl}), this interface is used by
 * {@link SynchronizationLogic} to:
 * <ul>
 * <li>Consume source columns for comparison with copy columns</li>
 * <li>Add columns to the source that were added by users in the copy (pushing
 * user additions)</li>
 * <li>Remove columns from the source that were deleted by users in the copy
 * (pushing user deletions)</li>
 * <li>Match source columns with copy columns by column name</li>
 * </ul>
 *
 * <p>
 * This interface extends {@link Source} with {@link ColumnCopyItem} as the copy
 * item type and {@link ColumnSourceItem} as the source item type, enabling the
 * generic {@link SynchronizationLogic#synchronize} algorithm to work with
 * schema-level synchronization.
 *
 * <p>
 * The source represents the external system's current schema state (EntityView,
 * Table, RecordSet, etc.). During synchronization:
 * <ul>
 * <li>Columns that exist only in the copy and were changed by user → pushed to
 * source</li>
 * <li>Columns that exist only in the source → pulled to copy (unless deleted by
 * user)</li>
 * <li>Columns that exist in both → no action needed for schema (but may trigger
 * merge)</li>
 * </ul>
 *
 * <p>
 * After Phase 1 schema synchronization completes, Phase 2 begins row
 * synchronization using the reconciled schema from
 * {@link SchemaCopy#getFinalSchema()}.
 *
 * @see ColumnCopyItem the type of columns in the copy
 * @see ColumnSourceItem the type of columns in the source
 * @see SchemaCopy the corresponding copy side of schema synchronization
 * @see SchemaSourceImpl the implementation that bridges with
 *      {@link SourceHandler}
 */
public interface SchemaSource extends Source<ColumnCopyItem, ColumnSourceItem> {

}
