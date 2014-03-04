package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEMAPHORE_EXPIRES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEMAPHORE_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SEMAPHORE_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_SEMAPHORE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SEMAPHORE;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

/**
 * Mapping for the SEMAPHORE table.
 * @author John
 *
 */
public class DBOSemaphore implements DatabaseObject<DBOSemaphore> {

	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("key", COL_SEMAPHORE_KEY, true),
		new FieldColumn("token", COL_SEMAPHORE_TOKEN),
		new FieldColumn("expiration", COL_SEMAPHORE_EXPIRES),
		};
	
	@Override
	public TableMapping<DBOSemaphore> getTableMapping() {
		return new TableMapping<DBOSemaphore>(){

			@Override
			public DBOSemaphore mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOSemaphore sem = new DBOSemaphore();
				sem.setKey(rs.getString(COL_SEMAPHORE_KEY));
				sem.setToken(rs.getString(COL_SEMAPHORE_TOKEN));
				sem.setExpiration(rs.getLong(COL_SEMAPHORE_EXPIRES));
				return sem;
			}

			@Override
			public String getTableName() {
				return TABLE_SEMAPHORE;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_SEMAPHORE;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOSemaphore> getDBOClass() {
				return DBOSemaphore.class;
			}};
	}
	
	private String key;
	private String token;
	private Long expiration;

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
		DBOSemaphore other = (DBOSemaphore) obj;
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
		return "DBOSemaphore [key=" + key + ", token=" + token
				+ ", expiration=" + expiration + "]";
	}

}
