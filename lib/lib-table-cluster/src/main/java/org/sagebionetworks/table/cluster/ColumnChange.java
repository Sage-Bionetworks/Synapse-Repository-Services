package org.sagebionetworks.table.cluster;

import org.sagebionetworks.repo.model.table.ColumnModel;

/**
 * Object that describes a column change as follows:
 * <ul>
 * <li>Delete: oldCoumn = toDelete, newColumn = null</li>
 * <li>Add: oldColumn = null, newColumn = toAdd</li>
 * <li>Update: oldColumn = old column definition, newColumn = new column
 * definition.</li>
 * <li>When oldColumn is equal to newColumn then no change will be made.</li>
 * </ul>
 *
 */
public class ColumnChange {

	ColumnModel oldColumn;
	ColumnModel newColumn;

	/**
	 * <ul>
	 * <li>Delete: oldCoumn = toDelete, newColumn = null</li>
	 * <li>Add: oldColumn = null, newColumn = toAdd</li>
	 * <li>Update: oldColumn = old column definition, newColumn = new column
	 * definition.</li>
	 * <li>When oldColumn is equal to newColumn then no change will be made.</li>
	 * </ul>
	 * 
	 * @param oldColumn
	 * @param newColumn
	 */
	public ColumnChange(ColumnModel oldColumn, ColumnModel newColumn) {
		super();
		this.oldColumn = oldColumn;
		this.newColumn = newColumn;
	}

	public ColumnModel getOldColumn() {
		return oldColumn;
	}

	public ColumnModel getNewColumn() {
		return newColumn;
	}

}
