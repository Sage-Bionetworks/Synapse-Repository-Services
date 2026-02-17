package org.sagebionetworks.repo.manager.grid.synch.schema;

import java.util.Objects;

import org.sagebionetworks.repo.manager.grid.synch.core.SourceItem;

/**
 * Represents a single column from the source of truth during Phase 1 schema
 * synchronization. Provides the column's metadata for comparison with the copy
 * schema.
 *
 * <p>
 * During Phase 1 of the synchronization process (see
 * {@link GridSynchronizationManagerImpl}), this class is used by
 * {@link SynchronizationLogic} to:
 * <ul>
 * <li>Match source columns with copy columns using column names</li>
 * <li>Detect columns that were added to the source externally</li>
 * <li>Detect columns that were deleted from the source externally</li>
 * </ul>
 *
 * <p>
 * Unlike {@link ColumnCopyItem}, source columns don't track whether they were
 * changed by the user. The source represents the external system's current
 * schema state (EntityView, Table, RecordSet, etc.) that needs to be
 * synchronized with user changes in the CRDT replica.
 *
 * <p>
 * The synchronization behavior for source columns:
 * <ul>
 * <li>If column exists in both source and copy → compare and potentially
 * merge</li>
 * <li>If column exists only in source and not deleted by user → pull column to
 * copy (external addition)</li>
 * <li>If column exists only in source but was deleted by user → remove column
 * from source (push user deletion)</li>
 * </ul>
 *
 * @see ColumnCopyItem the corresponding copy column type
 * @see SchemaSource the source side of schema synchronization
 * @see SchemaCopy the copy side of schema synchronization
 */
public class ColumnSourceItem implements SourceItem {

	private String columnName;

	/**
	 * Gets the column name. This serves as the unique identifier for matching
	 * columns between copy and source during schema synchronization.
	 *
	 * @return the column name
	 */
	public String getColumnName() {
		return columnName;
	}

	/**
	 * Sets the column name.
	 *
	 * @param columnName the column name
	 * @return this instance for method chaining
	 */
	public ColumnSourceItem setColumnName(String columnName) {
		this.columnName = columnName;
		return this;
	}

	/**
	 * Gets the unique identifier for this column (the column name). Used by
	 * {@link SynchronizationLogic#synchronize} to match source columns with copy
	 * columns during Phase 1 schema comparison.
	 *
	 * @return the column name
	 */
	@Override
	public String getKey() {
		return columnName;
	}

	@Override
	public int hashCode() {
		return Objects.hash(columnName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ColumnSourceItem other = (ColumnSourceItem) obj;
		return Objects.equals(columnName, other.columnName);
	}

	@Override
	public String toString() {
		return "ColumnSourceItem [columnName=" + columnName + "]";
	}

}
