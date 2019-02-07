package org.sagebionetworks.repo.model.dbo.unsuccessfulattemptlockout;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UNSUCCESSFUL_ATTEMPT_COUNT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UNSUCCESSFUL_ATTEMPT_KEY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_UNSUCCESSFUL_ATTEMPT_LOCKOUT_EXPIRATION_TIMESTAMP_MILLIS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_UNSUCCESSFUL_ATTEMPT_LOCKOUT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_UNSUCCESSFUL_ATTEMPT_LOCKOUT;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.TableMapping;

public class DBOUnsuccessfulAttemptLockout implements DatabaseObject<DBOUnsuccessfulAttemptLockout> {
	public static final FieldColumn[] FIELD_COLUMNS = {
			new FieldColumn("attemptKey", COL_UNSUCCESSFUL_ATTEMPT_KEY).withIsPrimaryKey(true),
			new FieldColumn("unsuccessfulAttemptCount", COL_UNSUCCESSFUL_ATTEMPT_COUNT),
			new FieldColumn("lockoutExpiration", COL_UNSUCCESSFUL_ATTEMPT_LOCKOUT_EXPIRATION_TIMESTAMP_MILLIS)
	};

	private String attemptKey;
	private	long unsuccessfulAttemptCount;
	private long lockoutExpiration;

	@Override
	public TableMapping<DBOUnsuccessfulAttemptLockout> getTableMapping() {
		return new TableMapping<DBOUnsuccessfulAttemptLockout>() {
			@Override
			public String getTableName() {
				return TABLE_UNSUCCESSFUL_ATTEMPT_LOCKOUT;
			}

			@Override
			public String getDDLFileName() {
				return DDL_UNSUCCESSFUL_ATTEMPT_LOCKOUT;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELD_COLUMNS;
			}

			@Override
			public Class<? extends DBOUnsuccessfulAttemptLockout> getDBOClass() {
				return DBOUnsuccessfulAttemptLockout.class;
			}

			@Override
			public DBOUnsuccessfulAttemptLockout mapRow(ResultSet resultSet, int i) throws SQLException {
				DBOUnsuccessfulAttemptLockout mapped = new DBOUnsuccessfulAttemptLockout();
				mapped.setAttemptKey(resultSet.getString(COL_UNSUCCESSFUL_ATTEMPT_KEY));
				mapped.setUnsuccessfulAttemptCount(resultSet.getLong(COL_UNSUCCESSFUL_ATTEMPT_COUNT));
				mapped.setLockoutExpiration(resultSet.getLong(COL_UNSUCCESSFUL_ATTEMPT_LOCKOUT_EXPIRATION_TIMESTAMP_MILLIS));
				return mapped;
			}
		};
	}

	public String getAttemptKey() {
		return attemptKey;
	}

	public void setAttemptKey(String attemptKey) {
		this.attemptKey = attemptKey;
	}

	public long getUnsuccessfulAttemptCount() {
		return unsuccessfulAttemptCount;
	}

	public void setUnsuccessfulAttemptCount(long unsuccessfulAttemptCount) {
		this.unsuccessfulAttemptCount = unsuccessfulAttemptCount;
	}

	public long getLockoutExpiration() {
		return lockoutExpiration;
	}

	public void setLockoutExpiration(long lockoutExpiration) {
		this.lockoutExpiration = lockoutExpiration;
	}
}
