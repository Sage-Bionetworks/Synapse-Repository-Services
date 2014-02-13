package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;

@Table(name = TABLE_SHARED_SEMAPHORE, constraints = {
		"UNIQUE KEY (" + COL_SHARED_SEMAPHORE_LOCK_TOKEN + ")" })
public class DBOSharedLock implements DatabaseObject<DBOSharedLock> {

	private static TableMapping<DBOSharedLock> tableMapping = AutoTableMapping
			.create(DBOSharedLock.class);

	@ForeignKey(table = TABLE_EXCLUSIVE_SEMAPHORE, field = COL_EXCLUSIVE_SEMAPHORE_KEY, cascadeDelete = true)
	@Field(name = COL_SHARED_SEMAPHORE_KEY, nullable = false, primary = true, fixedchar=100)
	private String key;

	@Field(name = COL_SHARED_SEMAPHORE_LOCK_TOKEN, nullable = false, primary = true, fixedchar=100)
	private String token;

	@Field(name = COL_SHARED_SEMAPHORE_EXPIRES, nullable = false)
	private Long expiration;

	@Override
	public TableMapping<DBOSharedLock> getTableMapping() {
		return tableMapping;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Long getExpiration() {
		return expiration;
	}

	public void setExpiration(Long expiration) {
		this.expiration = expiration;
	}

	public static void setTableMapping(TableMapping<DBOSharedLock> tableMapping) {
		DBOSharedLock.tableMapping = tableMapping;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((expiration == null) ? 0 : expiration.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((token == null) ? 0 : token.hashCode());
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
		DBOSharedLock other = (DBOSharedLock) obj;
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
		if (token == null) {
			if (other.token != null)
				return false;
		} else if (!token.equals(other.token))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOSharedLock [key=" + key + ", token=" + token
				+ ", expiration=" + expiration + "]";
	}

}
