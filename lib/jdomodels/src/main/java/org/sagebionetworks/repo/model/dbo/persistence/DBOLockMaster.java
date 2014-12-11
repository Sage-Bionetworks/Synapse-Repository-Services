package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;

@Table(name = TABLE_LOCK_MASTER)
public class DBOLockMaster implements  DatabaseObject<DBOLockMaster> {
	
	private static TableMapping<DBOLockMaster> tableMapping = AutoTableMapping.create(DBOLockMaster.class);
	
	@Field(name = COL_LOCK_MASTER_KEY, nullable = false, primary = true, fixedchar = 100)
	private String key;
	
	@Override
	public TableMapping<DBOLockMaster> getTableMapping() {
		return tableMapping;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
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
		DBOLockMaster other = (DBOLockMaster) obj;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOLockMaster [key=" + key + "]";
	}
}
