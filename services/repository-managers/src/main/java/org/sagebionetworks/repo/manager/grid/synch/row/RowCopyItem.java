package org.sagebionetworks.repo.manager.grid.synch.row;

import java.util.List;
import java.util.Optional;

import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.manager.grid.synch.core.CopyItem;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

/**
 * Represents a single row from the copy (CRDT replica) during Phase 2 row
 * synchronization. Provides access to the row's data and CRDT metadata needed
 * for synchronization with the source.
 *
 * <p>
 * During row synchronization, the {@link SynchronizationLogic} compares copy
 * rows with source rows to determine:
 * <ul>
 * <li>Which rows match between copy and source (no action needed)</li>
 * <li>Which rows differ and need cell-level merging via {@link RowMerge}</li>
 * <li>Which rows exist only in copy (push additions or handle deletions)</li>
 * <li>Which rows exist only in source (pull additions or push deletions)</li>
 * </ul>
 *
 * <p>
 * The row implements {@link CopyItem} to support the
 * {@link SynchronizationLogic#synchronize} algorithm, which tracks whether the
 * row was changed by the user to determine whether changes should be pushed to
 * or pulled from the source.
 *
 * <p>
 * The CRDT metadata ({@link #getRgaNodeId()} and {@link #getVectorNodeId()})
 * enables the copy to track row identity and ordering across synchronization
 * cycles, even when rows are added, deleted, or reordered in the source.
 */
public interface RowCopyItem extends CopyItem {

	/**
	 * Gets the underlying Synapse row representation, if available. The
	 * {@link SynapseRow} contains the row's metadata and access to the row's data
	 * from the CRDT replica.
	 *
	 * <p>
	 * Returns empty when the row represents a deletion or when the row hasn't been
	 * fully materialized yet.
	 *
	 * @return the Synapse row, or empty if not available
	 */
	Optional<SynapseRow> getSynapseRow();

	/**
	 * Gets the RGA (Replicated Growable Array) node identifier for this row. The
	 * RGA node ID is a logical timestamp that identifies the row's position in the
	 * CRDT's ordered sequence of rows.
	 *
	 * <p>
	 * This identifier is used to:
	 * <ul>
	 * <li>Maintain consistent row ordering across replicas</li>
	 * <li>Track row identity even when source IDs change</li>
	 * <li>Support conflict-free insertion of new rows</li>
	 * </ul>
	 *
	 * @return the RGA node identifier
	 */
	LogicalTimestamp getRgaNodeId();

	/**
	 * Gets the vector clock node identifier for this row. The vector node ID is a
	 * logical timestamp that tracks the row's version in the CRDT, enabling
	 * detection of concurrent modifications.
	 *
	 * <p>
	 * This identifier is used to:
	 * <ul>
	 * <li>Track which replica last modified the row</li>
	 * <li>Detect concurrent updates to the same row</li>
	 * <li>Support causal ordering of row operations</li>
	 * </ul>
	 *
	 * @return the vector clock node identifier
	 */
	LogicalTimestamp getVectorNodeId();

	/**
	 * Gets the individual cells that comprise this row. During cell-level
	 * synchronization (when copy and source rows don't match), the {@link RowMerge}
	 * logic compares these cells with the corresponding source cells to determine
	 * which cells need to be pushed to the source or pulled from the source.
	 *
	 * @return the list of cells in this row
	 */
	List<CellCopyItem> getCells();

}
