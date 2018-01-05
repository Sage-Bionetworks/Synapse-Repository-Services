package org.sagebionetworks.repo.manager.migration;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Bundle of a DatabaseObject with its type the represents a single row to
 * backup/restore.
 *
 */
public class RowData {

	MigrationType type;
	DatabaseObject<?> databaseObject;

	/**
	 * 
	 * @param type
	 * @param datbaseObject
	 */
	public RowData(MigrationType type, DatabaseObject<?> datbaseObject) {
		this.type = type;
		this.databaseObject = datbaseObject;
	}

	/**
	 * The type of row.
	 * 
	 * @return
	 */
	public MigrationType getType() {
		return type;
	}

	/**
	 * The type of row.
	 * 
	 * @param type
	 */
	public void setType(MigrationType type) {
		this.type = type;
	}

	/**
	 * The DatabaseObject containing all of the data for a row.
	 * 
	 * @return
	 */
	public DatabaseObject<?> getDatabaseObject() {
		return databaseObject;
	}

	/**
	 * The DatabaseObject containing all of the data for a row.
	 * 
	 * @param databaseObject
	 */
	public void setDatabaseObject(DatabaseObject<?> databaseObject) {
		this.databaseObject = databaseObject;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((databaseObject == null) ? 0 : databaseObject.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		RowData other = (RowData) obj;
		if (databaseObject == null) {
			if (other.databaseObject != null)
				return false;
		} else if (!databaseObject.equals(other.databaseObject))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "RowData [type=" + type + ", databaseObject=" + databaseObject + "]";
	}

}
