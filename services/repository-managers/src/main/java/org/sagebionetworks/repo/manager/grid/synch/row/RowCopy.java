package org.sagebionetworks.repo.manager.grid.synch.row;

import org.sagebionetworks.repo.manager.grid.synch.core.Copy;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReference;

/**
 * Represents the copy (CRDT replica) side of Phase 2 row synchronization.
 * Provides access to rows from the copy and operations for modifying the copy
 * based on synchronization with the source.
 *
 * <p>
 * During Phase 2 of the synchronization process (see
 * {@link GridSynchronizationManagerImpl}), this interface is used by
 * {@link SynchronizationLogic} to:
 * <ul>
 * <li>Stream all rows from the copy for comparison with source rows</li>
 * <li>Add rows to the copy that were added to the source (pulling source
 * additions)</li>
 * <li>Remove rows from the copy that were deleted from the source (pulling
 * source deletions)</li>
 * <li>Track which rows were deleted by the user (to push user's deletions to
 * source)</li>
 * </ul>
 *
 * <p>
 * This interface extends {@link Copy} with {@link RowCopyItem} as the copy item
 * type and {@link RowSourceItemReference} as the source item type, enabling the generic
 * {@link SynchronizationLogic#synchronize} algorithm to work with row-level
 * synchronization.
 *
 * <p>
 * The copy maintains CRDT metadata (RGA and vector clock timestamps) for each
 * row, enabling consistent row ordering and conflict-free synchronization even
 * when rows are added, deleted, or reordered in the source.
 *
 * @see RowCopyItem the type of rows in the copy
 * @see RowSourceItemReference the type of row identifiers from the source
 * @see RowSource the corresponding source side of row synchronization
 * @see RowMerge the merge strategy for resolving row-level conflicts
 */
public interface RowCopy extends Copy<RowCopyItem, RowSourceItemReference> {

}
