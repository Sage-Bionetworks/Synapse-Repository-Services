package org.sagebionetworks.repo.manager.grid.synch.row;

import java.util.Objects;

import org.sagebionetworks.repo.manager.grid.synch.core.CopyItem;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

/**
 * Represents a single cell from a copy row during Phase 2 cell-level
 * synchronization. Provides the cell's value and tracks whether it was changed
 * by the user, enabling the synchronization logic to determine if changes
 * should be pushed to the source.
 *
 * <p>
 * During row synchronization, when a copy row and source row don't match
 * (different hashes), the {@link RowMerge} logic compares cells individually to
 * determine:
 * <ul>
 * <li>Which cells changed in the copy vs. source</li>
 * <li>Which cells need to be pushed to the source (user changes)</li>
 * <li>Which cells need to be pulled to the copy (external source updates)</li>
 * </ul>
 *
 * <p>
 * The key distinction from {@link CellSourceItem} is that copy cells track user
 * modifications via {@link #wasChangedByUser()}. This enables the
 * synchronization logic to distinguish between:
 * <ul>
 * <li>User changes that should be pushed to the source</li>
 * <li>Copy values that should be updated with external source changes</li>
 * </ul>
 */
public class CellCopyItem implements CopyItem {

	private String name;
	private ConValue value;
	private boolean wasChangedByUser;

	/**
	 * Gets the column name for this cell. Used to match cells between copy and
	 * source rows during cell-level comparison.
	 *
	 * @return the column name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the column name for this cell.
	 *
	 * @param name the column name
	 * @return this instance for method chaining
	 */
	public CellCopyItem setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Gets the cell's current value from the copy. This value is compared with the
	 * source cell value during cell-level synchronization to detect changes and
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
	public CellCopyItem setValue(ConValue value) {
		this.value = value;
		return this;
	}

	/**
	 * Indicates whether this cell was modified by the user in the copy. This flag
	 * determines conflict resolution during cell-level synchronization:
	 * <ul>
	 * <li>If true and values differ → push copy value to source (user's change
	 * wins)</li>
	 * <li>If false and values differ → pull source value to copy (external change
	 * wins)</li>
	 * </ul>
	 *
	 * @return true if the user modified this cell, false otherwise
	 */
	@Override
	public boolean wasChangedByUser() {
		return wasChangedByUser;
	}

	/**
	 * Sets whether this cell was changed by the user.
	 *
	 * @param wasChangedByUser true if changed by user, false otherwise
	 * @return this instance for method chaining
	 */
	public CellCopyItem setWasChangedByUser(boolean wasChangedByUser) {
		this.wasChangedByUser = wasChangedByUser;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, value, wasChangedByUser);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CellCopyItem other = (CellCopyItem) obj;
		return Objects.equals(name, other.name) && Objects.equals(value, other.value)
				&& wasChangedByUser == other.wasChangedByUser;
	}

	@Override
	public String toString() {
		return "CopyCell [name=" + name + ", value=" + value + ", wasChangedByUser=" + wasChangedByUser + "]";
	}

}
