package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;

@Table(name = TABLE_COUNTING_SEMAPHORE)
public class DBOCountingSemaphore implements DatabaseObject<DBOCountingSemaphore> {

	public static final int KEY_NAME_LENGTH = 100;

	private static TableMapping<DBOCountingSemaphore> tableMapping = AutoTableMapping.create(DBOCountingSemaphore.class);

	@ForeignKey(table = TABLE_LOCK_MASTER, field = COL_LOCK_MASTER_KEY, cascadeDelete = true)
	@Field(name = COL_COUNTING_SEMAPHORE_KEY, nullable = false, primary = true, fixedchar = KEY_NAME_LENGTH)
	private String key;

	@Field(name = COL_COUNTING_SEMAPHORE_LOCK_TOKEN, nullable = false, primary = true, fixedchar = 100)
	private String token;

	@Field(name = COL_COUNTING_SEMAPHORE_EXPIRES, nullable = false)
	private Long expires;

	@Override
	public TableMapping<DBOCountingSemaphore> getTableMapping() {
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

	public Long getExpires() {
		return expires;
	}

	public void setExpires(Long expires) {
		this.expires = expires;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((expires == null) ? 0 : expires.hashCode());
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
		DBOCountingSemaphore other = (DBOCountingSemaphore) obj;
		if (expires == null) {
			if (other.expires != null)
				return false;
		} else if (!expires.equals(other.expires))
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
		return "DBOCountingSemaphore [key=" + key + ", token=" + token + ", expires=" + expires + "]";
	}
}
