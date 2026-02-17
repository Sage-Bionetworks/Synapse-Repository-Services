package org.sagebionetworks.repo.manager.grid.synch.row;

import org.sagebionetworks.repo.manager.grid.synch.core.Source;
import org.sagebionetworks.repo.manager.grid.synch.io.RowHeader;

/**
 * Represents the source of truth side of Phase 2 row synchronization. Provides
 * access to rows from the source and operations for modifying the source based
 * on synchronization with the copy (CRDT replica).
 *
 * <p>
 * During Phase 2 of the synchronization process (see
 * {@link GridSynchronizationManagerImpl}), this interface is used by
 * {@link SynchronizationLogic} to:
 * <ul>
 * <li>Match source rows with copy rows using row keys (unique identifiers)</li>
 * <li>Compare source rows with copy rows to detect changes</li>
 * <li>Add rows to the source that were added by the user in the copy (pushing
 * user additions)</li>
 * <li>Remove rows from the source that were deleted by the user in the copy
 * (pushing user deletions)</li>
 * </ul>
 *
 * <p>
 * This interface extends {@link Source} with {@link RowCopyItem} as the copy item
 * type and {@link RowHeader} as the source item type, enabling the generic
 * {@link SynchronizationLogic#synchronize} algorithm to work with row-level
 * synchronization. The {@link RowHeader} provides lightweight row identifiers
 * that can fetch full row data on demand, enabling memory-efficient streaming
 * comparison.
 *
 * <p>
 * The source represents the external system's current state (EntityView, Table,
 * RecordSet, etc.) that needs to be synchronized with user changes in the CRDT
 * replica. During synchronization, user changes in the copy take precedence and
 * are pushed to the source, while external source changes are pulled to the
 * copy.
 *
 * @see RowCopyItem the type of rows in the copy
 * @see RowHeader the type of row identifiers from the source
 * @see RowCopy the corresponding copy side of row synchronization
 * @see RowMerge the merge strategy for resolving row-level conflicts
 * @see RowSourceImpl the implementation that bridges with {@link SourceHandler}
 */
public interface RowSource extends Source<RowCopyItem, RowHeader> {

}
