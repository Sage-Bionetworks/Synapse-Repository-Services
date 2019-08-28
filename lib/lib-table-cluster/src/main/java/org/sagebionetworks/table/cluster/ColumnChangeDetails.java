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
public class ColumnChangeDetails {

	ColumnModel oldColumn;
	DatabaseColumnInfo oldColumnInfo;
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
	public ColumnChangeDetails(ColumnModel oldColumn, ColumnModel newColumn) {
		this(oldColumn, null, newColumn);
	}

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
	public ColumnChangeDetails(ColumnModel oldColumn, DatabaseColumnInfo oldColumnInfo, ColumnModel newColumn) {
		super();
		this.oldColumn = oldColumn;
		this.oldColumnInfo = oldColumnInfo;
		this.newColumn = newColumn;
	}

	public ColumnModel getOldColumn() {
		return oldColumn;
	}

	public DatabaseColumnInfo getOldColumnInfo(){
		return this.oldColumnInfo;
	}
	
	public ColumnModel getNewColumn() {
		return newColumn;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((newColumn == null) ? 0 : newColumn.hashCode());
		result = prime * result
				+ ((oldColumn == null) ? 0 : oldColumn.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ColumnChangeDetails other = (ColumnChangeDetails) obj;
		if (newColumn == null) {
			if (other.newColumn != null)
				return false;
		} else if (!newColumn.equals(other.newColumn))
			return false;
		if (oldColumn == null) {
			if (other.oldColumn != null)
				return false;
		} else if (!oldColumn.equals(other.oldColumn))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ColumnChange [oldColumn=" + (oldColumn == null? null : oldColumn.getId()) + ", newColumn="
				+ (newColumn == null? null: newColumn.getId())+ "]";
	}

}
