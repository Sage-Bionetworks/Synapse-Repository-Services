package org.sagebionetworks.repo.manager.grid.synch.row;

import org.sagebionetworks.repo.manager.grid.synch.core.Merge;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReference;

/**
 * Defines the merge strategy for resolving conflicts between copy rows and
 * source rows during Phase 2 row synchronization. Performs cell-level
 * comparison and conflict resolution when a row exists in both copy and source
 * but they don't match.
 *
 * <p>
 * This interface is invoked by {@link SynchronizationLogic#synchronize} during
 * Phase 1 when:
 * <ul>
 * <li>A row exists in both copy and source</li>
 * <li>The rows don't match (different row hashes)</li>
 * <li>The rows need to be merged together</li>
 * </ul>
 *
 * <p>
 * The merge strategy compares individual cells between the copy row and source
 * row to determine for each cell:
 * <ul>
 * <li>If the cell values match → no action needed</li>
 * <li>If copy cell was changed by user → push copy value to source (user's
 * change wins)</li>
 * <li>If copy cell was not changed by user → pull source value to copy
 * (external change wins)</li>
 * </ul>
 *
 * <p>
 * This cell-level conflict resolution ensures that user changes take precedence
 * over external source changes, while external changes are pulled when the user
 * hasn't modified those cells. This enables collaborative editing where
 * concurrent changes to different cells in the same row can be merged together
 * without data loss.
 *
 * @see RowCopyItem the type of rows in the copy
 * @see RowSourceItemReference the type of row identifiers from the source
 * @see CellCopyItem the individual cells compared during merging
 * @see CellSourceItem the source cell values compared during merging
 * @see RowMergeImpl the implementation that performs cell-level synchronization
 */
public interface RowMerge extends Merge<RowCopyItem, RowSourceItemReference> {

}
