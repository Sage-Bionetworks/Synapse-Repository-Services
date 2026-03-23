package org.sagebionetworks.repo.manager.grid.synch.row;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

/**
 * Implementation of {@link RowCopyItem} that represents a materialized row from the
 * copy (CRDT replica) during Phase 2 row synchronization.
 *
 * <p>
 * This class holds both the logical CRDT metadata (RGA and vector clock
 * timestamps) and the physical row data (cells and optional Synapse row
 * reference). The CRDT metadata enables tracking row identity and ordering
 * across synchronization cycles, while the cell data enables cell-level
 * conflict resolution during row merging.
 *
 * <p>
 * Key responsibilities:
 * <ul>
 * <li>Store CRDT metadata for row identity and ordering</li>
 * <li>Provide access to individual cells for cell-level comparison</li>
 * <li>Track whether any cells were changed by the user (implements
 * {@link CopyItem})</li>
 * <li>Optionally reference the underlying {@link SynapseRow} from the
 * replica</li>
 * </ul>
 */
public class RowCopyItemImpl implements RowCopyItem {

	private SynapseRow synapseRow;
	private LogicalTimestamp rgaNodeId;
	private LogicalTimestamp vectorNodeId;
	private LogicalTimestamp metadataNodeId;
	private List<CellCopyItem> cells;

	/**
	 * Determines whether this row was changed by the user by checking if any of its
	 * cells were changed by the user. This method implements the {@link CopyItem}
	 * contract and is used by the synchronization logic to determine whether row
	 * changes should be pushed to the source or pulled from the source.
	 *
	 * <p>
	 * Returns true if at least one cell in this row has
	 * {@link CellCopyItem#wasChangedByUser()} returning true, indicating the user made
	 * modifications to this row that should be synchronized to the source.
	 *
	 * @return true if any cell was changed by the user, false otherwise
	 */
	@Override
	public boolean wasChangedByUser() {
		return cells != null && cells.stream().anyMatch(CellCopyItem::wasChangedByUser);
	}

	/**
	 * Gets the RGA (Replicated Growable Array) node identifier for this row. The
	 * RGA node ID is a logical timestamp that identifies the row's position in the
	 * CRDT's ordered sequence of rows, enabling consistent row ordering across
	 * replicas and tracking row identity even when source IDs change.
	 *
	 * @return the RGA node identifier
	 */
	@Override
	public LogicalTimestamp getRgaNodeId() {
		return rgaNodeId;
	}

	/**
	 * Gets the underlying Synapse row representation, if available. The
	 * {@link SynapseRow} contains the row's metadata and provides access to the
	 * row's data in the CRDT replica.
	 *
	 * <p>
	 * Returns empty when the row represents a deletion or when the row hasn't been
	 * fully materialized yet from the replica.
	 *
	 * @return the Synapse row, or empty if not available
	 */
	@Override
	public Optional<SynapseRow> getSynapseRow() {
		return Optional.ofNullable(synapseRow);
	}

	/**
	 * Gets the vector clock node identifier for this row. The vector node ID is a
	 * logical timestamp that tracks the row's version in the CRDT, enabling
	 * detection of concurrent modifications and supporting causal ordering of row
	 * operations.
	 *
	 * @return the vector clock node identifier
	 */
	@Override
	public LogicalTimestamp getVectorNodeId() {
		return vectorNodeId;
	}

	/**
	 * Sets the RGA node identifier for this row.
	 *
	 * @param rgaNodeId the RGA node identifier
	 * @return this instance for method chaining
	 */
	public RowCopyItemImpl setRgaNodeId(LogicalTimestamp rgaNodeId) {
		this.rgaNodeId = rgaNodeId;
		return this;
	}

	/**
	 * Gets the individual cells that comprise this row. During cell-level
	 * synchronization (when copy and source rows don't match), the {@link RowMerge}
	 * logic compares these cells with the corresponding source cells to determine
	 * which cells need to be pushed to the source or pulled from the source.
	 *
	 * @return the list of cells in this row
	 */
	public List<CellCopyItem> getCells() {
		return cells;
	}

	/**
	 * Sets the cells for this row.
	 *
	 * @param cells the list of cells
	 * @return this instance for method chaining
	 */
	public RowCopyItemImpl setCells(List<CellCopyItem> cells) {
		this.cells = cells;
		return this;
	}

	/**
	 * Sets the underlying Synapse row representation.
	 *
	 * @param synapseRow the Synapse row
	 * @return this instance for method chaining
	 */
	public RowCopyItemImpl setSynapseRow(SynapseRow synapseRow) {
		this.synapseRow = synapseRow;
		return this;
	}

	/**
	 * Sets the vector clock node identifier for this row.
	 *
	 * @param vectorNodeId the vector clock node identifier
	 * @return this instance for method chaining
	 */
	public RowCopyItemImpl setVectorNodeId(LogicalTimestamp vectorNodeId) {
		this.vectorNodeId = vectorNodeId;
		return this;
	}
	
	/**
	 * The ID of the metadata object that defines this row's metadata.
	 */
	@Override
	public LogicalTimestamp getMetadataNodeId() {
		return metadataNodeId;
	}


	/**
	 * The ID of the metadata object that defines this row's metadata.
	 * @param metadataNodeId
	 * @return
	 */
	public RowCopyItemImpl setMetadataNodeId(LogicalTimestamp metadataNodeId) {
		this.metadataNodeId = metadataNodeId;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(cells, metadataNodeId, rgaNodeId, synapseRow, vectorNodeId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RowCopyItemImpl other = (RowCopyItemImpl) obj;
		return Objects.equals(cells, other.cells) && Objects.equals(metadataNodeId, other.metadataNodeId)
				&& Objects.equals(rgaNodeId, other.rgaNodeId) && Objects.equals(synapseRow, other.synapseRow)
				&& Objects.equals(vectorNodeId, other.vectorNodeId);
	}

	@Override
	public String toString() {
		return "RowCopyItemImpl [synapseRow=" + synapseRow + ", rgaNodeId=" + rgaNodeId + ", vectorNodeId="
				+ vectorNodeId + ", metadataNodeId=" + metadataNodeId + ", cells=" + cells + "]";
	}

}
