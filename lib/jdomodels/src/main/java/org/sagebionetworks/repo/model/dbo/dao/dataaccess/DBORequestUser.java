package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

public class DBORequestUser implements MigratableDatabaseObject<DBORequestUser, DBORequestUser> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("requestId", SqlConstants.COL_DATA_ACCESS_REQUEST_USER_REQUEST_ID, true).withIsBackupId(true),
			new FieldColumn("userId", SqlConstants.COL_DATA_ACCESS_REQUEST_USER_USER_ID, true)
	};

	private static final TableMapping<DBORequestUser> TABLE_MAPPING = new TableMapping<DBORequestUser>() {

		@Override
		public DBORequestUser mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBORequestUser dbo = new DBORequestUser();
			dbo.setRequestId(rs.getLong(SqlConstants.COL_DATA_ACCESS_REQUEST_USER_REQUEST_ID));
			dbo.setUserId(rs.getLong(SqlConstants.COL_DATA_ACCESS_REQUEST_USER_USER_ID));
			return dbo;
		}

		@Override
		public String getTableName() {
			return SqlConstants.TABLE_DATA_ACCESS_REQUEST_USER;
		}

		@Override
		public String getDDLFileName() {
			return SqlConstants.DDL_DATA_ACCESS_REQUEST_USER;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBORequestUser> getDBOClass() {
			return DBORequestUser.class;
		}
	};

	private static final MigratableTableTranslation<DBORequestUser, DBORequestUser> MIGRATION_TRANSLATOR = new BasicMigratableTableTranslation<>();

	private Long requestId;
	private Long userId;

	public Long getRequestId() {
		return requestId;
	}

	public void setRequestId(Long requestId) {
		this.requestId = requestId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	@Override
	public TableMapping<DBORequestUser> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.DATA_ACCESS_REQUEST_USER;
	}

	@Override
	public MigratableTableTranslation<DBORequestUser, DBORequestUser> getTranslator() {
		return MIGRATION_TRANSLATOR;
	}

	@Override
	public Class<? extends DBORequestUser> getBackupClass() {
		return DBORequestUser.class;
	}

	@Override
	public Class<? extends DBORequestUser> getDatabaseObjectClass() {
		return DBORequestUser.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(requestId, userId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		DBORequestUser other = (DBORequestUser) obj;
		return Objects.equals(requestId, other.requestId) && Objects.equals(userId, other.userId);
	}

	@Override
	public String toString() {
		return "DBORequestUser [requestId=" + requestId + ", userId=" + userId + "]";
	}
}
