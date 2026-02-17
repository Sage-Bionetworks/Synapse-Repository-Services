package org.sagebionetworks.repo.manager.grid.synch.schema;

import java.util.Objects;

import org.sagebionetworks.repo.manager.grid.synch.core.CopyItem;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

/**
 * Represents a single column from the copy (CRDT replica) during Phase 1 schema
 * synchronization. Provides the column's metadata for comparison with the
 * source schema and tracking whether the column was changed by the user.
 *
 * <p>
 * During Phase 1 of the synchronization process (see
 * {@link GridSynchronizationManagerImpl}), this class is used by
 * {@link SynchronizationLogic} to:
 * <ul>
 * <li>Match copy columns with source columns using column names</li>
 * <li>Determine whether columns were added/removed by the user vs. external
 * source changes</li>
 * <li>Track CRDT metadata (RGA node IDs) for column ordering</li>
 * </ul>
 *
 * <p>
 * The {@link #wasChangedByUser} flag determines synchronization behavior:
 * <ul>
 * <li>If true and column exists only in copy → push column addition to
 * source</li>
 * <li>If false and column exists only in copy → remove column from copy
 * (deleted from source)</li>
 * </ul>
 *
 * <p>
 * The {@link #columnOrderNodeId} is the CRDT RGA (Replicated Growable Array)
 * node identifier that tracks the column's position in the schema ordering.
 * This enables conflict-free ordering of columns across concurrent schema
 * modifications.
 *
 * @see ColumnSourceItem the corresponding source column type
 * @see SchemaCopy the copy side of schema synchronization
 * @see SchemaSource the source side of schema synchronization
 */
public class ColumnCopyItem implements CopyItem {

	private String columnName;
	private boolean wasChangedByUser;
	private LogicalTimestamp columnOrderNodeId;

	/**
	 * Determines whether this column was changed by the user in the copy. This is
	 * used during Phase 1 of {@link SynchronizationLogic#synchronize} to decide
	 * whether to push changes to the source or pull changes from the source.
	 *
	 * <p>
	 * If true and the column doesn't exist in the source, the column will be added
	 * to the source (pushing user's addition). If false and the column doesn't
	 * exist in the source, the column will be removed from the copy (external
	 * deletion from source).
	 *
	 * @return true if the user added or modified this column, false otherwise
	 */
	@Override
	public boolean wasChangedByUser() {
		return wasChangedByUser;
	}

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
	public ColumnCopyItem setColumnName(String columnName) {
		this.columnName = columnName;
		return this;
	}

	/**
	 * Sets whether this column was changed by the user. This flag controls
	 * synchronization behavior when the column exists only in the copy.
	 *
	 * @param wasChangedByUser true if the user added or modified this column
	 * @return this instance for method chaining
	 */
	public ColumnCopyItem setWasChangedByUser(boolean wasChangedByUser) {
		this.wasChangedByUser = wasChangedByUser;
		return this;
	}

	/**
	 * Gets the CRDT RGA (Replicated Growable Array) node identifier for this
	 * column's position in the schema ordering. This logical timestamp enables
	 * conflict-free ordering of columns across concurrent schema modifications.
	 *
	 * <p>
	 * The RGA node ID is used when publishing schema changes to the CRDT replica to
	 * maintain consistent column ordering semantics across replicas.
	 *
	 * @return the RGA node ID for this column's position
	 */
	public LogicalTimestamp getColumnOrderNodeId() {
		return columnOrderNodeId;
	}

	/**
	 * Sets the RGA node ID for this column's position in the schema ordering.
	 *
	 * @param columnOrderNodeId the RGA node ID
	 * @return this instance for method chaining
	 */
	public ColumnCopyItem setColumnOrderNodeId(LogicalTimestamp columnOrderNodeId) {
		this.columnOrderNodeId = columnOrderNodeId;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(columnName, columnOrderNodeId, wasChangedByUser);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ColumnCopyItem other = (ColumnCopyItem) obj;
		return Objects.equals(columnName, other.columnName)
				&& Objects.equals(columnOrderNodeId, other.columnOrderNodeId)
				&& wasChangedByUser == other.wasChangedByUser;
	}

	@Override
	public String toString() {
		return "ColumnCopyItem [columnName=" + columnName + ", wasChangedByUser=" + wasChangedByUser
				+ ", columnOrderNodeId=" + columnOrderNodeId + "]";
	}

}
