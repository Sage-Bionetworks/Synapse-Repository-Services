package org.sagebionetworks.repo.manager.grid.synch.row;

import java.util.Objects;

import org.sagebionetworks.repo.manager.grid.synch.core.SourceItem;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

/**
 * Represents a single cell from a source row during Phase 2 cell-level
 * synchronization. Provides the cell's value for comparison with the
 * corresponding copy cell during row merging.
 *
 * <p>
 * During row synchronization, when a copy row and source row don't match
 * (different hashes), the {@link RowMerge} logic compares cells individually to
 * determine:
 * <ul>
 * <li>Which cells changed in the source vs. copy</li>
 * <li>Which cells need to be pushed to the source (user changes)</li>
 * <li>Which cells need to be pulled to the copy (external source updates)</li>
 * </ul>
 *
 * <p>
 * Unlike {@link CellCopyItem}, source cells don't track whether they were
 * changed by the user - the source represents the current state of truth that
 * needs to be synchronized with user changes in the copy.
 */
public class CellSourceItem implements SourceItem {

	private String columnName;
	private ConValue value;

	/**
	 * Gets the unique identifier for this cell (the column name). Used to match
	 * cells between copy and source rows during cell-level comparison.
	 *
	 * @return the column name
	 */
	@Override
	public String getKey() {
		return columnName;
	}

	/**
	 * Gets the column name for this cell. Used to match cells between copy and
	 * source rows during cell-level comparison.
	 *
	 * @return the column name
	 */
	public String getColumnName() {
		return columnName;
	}

	/**
	 * Sets the column name for this cell.
	 *
	 * @param columnName the column name
	 * @return this instance for method chaining
	 */
	public CellSourceItem setColumnName(String columnName) {
		this.columnName = columnName;
		return this;
	}

	/**
	 * Gets the cell's current value from the source. This value is compared with
	 * the copy cell value during cell-level synchronization to detect changes and
	 * resolve conflicts.
	 *
	 * @return the cell value
	 */
	public ConValue getValue() {
		return value;
	}

	/**
	 * Sets the cell's value.
	 *
	 * @param value the cell value
	 * @return this instance for method chaining
	 */
	public CellSourceItem setValue(ConValue value) {
		this.value = value;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(columnName, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CellSourceItem other = (CellSourceItem) obj;
		return Objects.equals(columnName, other.columnName) && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "SourceCell [columnName=" + columnName + ", value=" + value + "]";
	}

}
