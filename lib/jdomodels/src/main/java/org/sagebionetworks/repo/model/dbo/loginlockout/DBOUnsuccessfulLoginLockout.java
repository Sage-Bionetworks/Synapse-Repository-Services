package org.sagebionetworks.repo.model.dbo.loginlockout;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UNSUCCESSFUL_LOGIN_COUNT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UNSUCCESSFUL_LOGIN_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UNSUCCESSFUL_LOGIN_LOCKOUT_EXPIRATION_TIMESTAMP_MILLIS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_UNSUCCESSFUL_LOGIN_LOCKOUT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_UNSUCCESSFUL_LOGIN_LOCKOUT;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

public class DBOUnsuccessfulLoginLockout implements DatabaseObject<DBOUnsuccessfulLoginLockout> {
	public static final FieldColumn[] FIELD_COLUMNS = {
			new FieldColumn("userId", COL_UNSUCCESSFUL_LOGIN_KEY).withIsPrimaryKey(true),
			new FieldColumn("unsuccessfulLoginCount", COL_UNSUCCESSFUL_LOGIN_COUNT),
			new FieldColumn("lockoutExpiration", COL_UNSUCCESSFUL_LOGIN_LOCKOUT_EXPIRATION_TIMESTAMP_MILLIS)
	};

	private long userId;
	private	long unsuccessfulLoginCount;
	private long lockoutExpiration;

	@Override
	public TableMapping<DBOUnsuccessfulLoginLockout> getTableMapping() {
		return new TableMapping<DBOUnsuccessfulLoginLockout>() {
			@Override
			public String getTableName() {
				return TABLE_UNSUCCESSFUL_LOGIN_LOCKOUT;
			}

			@Override
			public String getDDLFileName() {
				return DDL_UNSUCCESSFUL_LOGIN_LOCKOUT;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELD_COLUMNS;
			}

			@Override
			public Class<? extends DBOUnsuccessfulLoginLockout> getDBOClass() {
				return DBOUnsuccessfulLoginLockout.class;
			}

			@Override
			public DBOUnsuccessfulLoginLockout mapRow(ResultSet resultSet, int i) throws SQLException {
				DBOUnsuccessfulLoginLockout mapped = new DBOUnsuccessfulLoginLockout();
				mapped.setUserId(resultSet.getLong(COL_UNSUCCESSFUL_LOGIN_KEY));
				mapped.setUnsuccessfulLoginCount(resultSet.getLong(COL_UNSUCCESSFUL_LOGIN_COUNT));
				mapped.setLockoutExpiration(resultSet.getLong(COL_UNSUCCESSFUL_LOGIN_LOCKOUT_EXPIRATION_TIMESTAMP_MILLIS));
				return mapped;
			}
		};
	}

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public long getUnsuccessfulLoginCount() {
		return unsuccessfulLoginCount;
	}

	public void setUnsuccessfulLoginCount(long unsuccessfulLoginCount) {
		this.unsuccessfulLoginCount = unsuccessfulLoginCount;
	}

	public long getLockoutExpiration() {
		return lockoutExpiration;
	}

	public void setLockoutExpiration(long lockoutExpiration) {
		this.lockoutExpiration = lockoutExpiration;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		DBOUnsuccessfulLoginLockout that = (DBOUnsuccessfulLoginLockout) o;
		return userId == that.userId &&
				unsuccessfulLoginCount == that.unsuccessfulLoginCount &&
				lockoutExpiration == that.lockoutExpiration;
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, unsuccessfulLoginCount, lockoutExpiration);
	}
}
