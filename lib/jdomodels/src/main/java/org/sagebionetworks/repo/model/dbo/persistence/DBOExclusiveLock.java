package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_EXCLUSIVE_SEMAPHORE_EXPIRES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_EXCLUSIVE_SEMAPHORE_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_EXCLUSIVE_SEMAPHORE_LOCK_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_EXCLUSIVE_SEMAPHORE_REQUEST_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_EXCLUSIVE_SEMAPHORE;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;

@Table(name = TABLE_EXCLUSIVE_SEMAPHORE, constraints = {
		"UNIQUE KEY (" + COL_EXCLUSIVE_SEMAPHORE_LOCK_TOKEN + ")",
		"UNIQUE KEY (" + COL_EXCLUSIVE_SEMAPHORE_REQUEST_TOKEN + ")" })
public class DBOExclusiveLock implements  DatabaseObject<DBOExclusiveLock> {
	
	private static TableMapping<DBOExclusiveLock> tableMapping = AutoTableMapping.create(DBOExclusiveLock.class);
	
	@Field(name = COL_EXCLUSIVE_SEMAPHORE_KEY, nullable = false, primary=true, fixedchar=100)
	private String key;
	
	@Field(name = COL_EXCLUSIVE_SEMAPHORE_REQUEST_TOKEN, nullable = true, fixedchar=200)
	private String exclusiveRequestToken;
	
	@Field(name = COL_EXCLUSIVE_SEMAPHORE_LOCK_TOKEN, nullable = true, fixedchar=200)
	private String exclusiveLockToken;
	
	@Field(name = COL_EXCLUSIVE_SEMAPHORE_EXPIRES)
	private Long expiration;

	@Override
	public TableMapping<DBOExclusiveLock> getTableMapping() {
		return tableMapping;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getExclusiveRequestToken() {
		return exclusiveRequestToken;
	}

	public void setExclusiveRequestToken(String exclusiveRequestToken) {
		this.exclusiveRequestToken = exclusiveRequestToken;
	}

	public String getExclusiveLockToken() {
		return exclusiveLockToken;
	}

	public void setExclusiveLockToken(String exclusiveLockToken) {
		this.exclusiveLockToken = exclusiveLockToken;
	}

	public Long getExpiration() {
		return expiration;
	}

	public void setExpiration(Long expiration) {
		this.expiration = expiration;
	}

	public static void setTableMapping(TableMapping<DBOExclusiveLock> tableMapping) {
		DBOExclusiveLock.tableMapping = tableMapping;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((exclusiveLockToken == null) ? 0 : exclusiveLockToken
						.hashCode());
		result = prime
				* result
				+ ((exclusiveRequestToken == null) ? 0 : exclusiveRequestToken
						.hashCode());
		result = prime * result
				+ ((expiration == null) ? 0 : expiration.hashCode());
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
		DBOExclusiveLock other = (DBOExclusiveLock) obj;
		if (exclusiveLockToken == null) {
			if (other.exclusiveLockToken != null)
				return false;
		} else if (!exclusiveLockToken.equals(other.exclusiveLockToken))
			return false;
		if (exclusiveRequestToken == null) {
			if (other.exclusiveRequestToken != null)
				return false;
		} else if (!exclusiveRequestToken.equals(other.exclusiveRequestToken))
			return false;
		if (expiration == null) {
			if (other.expiration != null)
				return false;
		} else if (!expiration.equals(other.expiration))
			return false;
		if (key == null) {
			if (other.key != null)
				return false;
		} else if (!key.equals(other.key))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOExclusiveLock [key=" + key + ", exclusiveRequestToken="
				+ exclusiveRequestToken + ", exclusiveLockToken="
				+ exclusiveLockToken + ", expiration=" + expiration + "]";
	}
	
	
}
