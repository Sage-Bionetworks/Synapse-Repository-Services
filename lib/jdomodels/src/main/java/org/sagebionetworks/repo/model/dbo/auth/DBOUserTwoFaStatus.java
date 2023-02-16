package org.sagebionetworks.repo.model.dbo.auth;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TWO_FA_STATUS_ENABLED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TWO_FA_STATUS_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_TWO_FA_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TWO_FA_STATUS;

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

/**
 * This DB object keeps track of the 2FA status for users
 */
public class DBOUserTwoFaStatus implements MigratableDatabaseObject<DBOUserTwoFaStatus, DBOUserTwoFaStatus> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("principalId", COL_TWO_FA_STATUS_PRINCIPAL_ID, true).withIsBackupId(true),
		new FieldColumn("enabled", COL_TWO_FA_STATUS_ENABLED)
	};
	
	private static final TableMapping<DBOUserTwoFaStatus> TABLE_MAPPING = new TableMapping<DBOUserTwoFaStatus>() {

		@Override
		public DBOUserTwoFaStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOUserTwoFaStatus status = new DBOUserTwoFaStatus();
			status.setPrincipalId(rs.getLong(COL_TWO_FA_STATUS_PRINCIPAL_ID));
			status.setEnabled(rs.getBoolean(COL_TWO_FA_STATUS_ENABLED));
			return status;
		}

		@Override
		public String getTableName() {
			return TABLE_TWO_FA_STATUS;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public String getDDLFileName() {
			return DDL_TWO_FA_STATUS;
		}

		@Override
		public Class<? extends DBOUserTwoFaStatus> getDBOClass() {
			return DBOUserTwoFaStatus.class;
		}
	};
	
	private static final MigratableTableTranslation<DBOUserTwoFaStatus, DBOUserTwoFaStatus> TRANSLATOR = new BasicMigratableTableTranslation<>();
	
	private Long principalId;
	private Boolean enabled;
	
	public Long getPrincipalId() {
		return principalId;
	}

	public void setPrincipalId(Long principalId) {
		this.principalId = principalId;
	}

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public TableMapping<DBOUserTwoFaStatus> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.TWO_FA_STATUS;
	}

	@Override
	public MigratableTableTranslation<DBOUserTwoFaStatus, DBOUserTwoFaStatus> getTranslator() {
		return TRANSLATOR;
	}

	@Override
	public Class<? extends DBOUserTwoFaStatus> getBackupClass() {
		return DBOUserTwoFaStatus.class;
	}

	@Override
	public Class<? extends DBOUserTwoFaStatus> getDatabaseObjectClass() {
		return DBOUserTwoFaStatus.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(enabled, principalId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOUserTwoFaStatus)) {
			return false;
		}
		DBOUserTwoFaStatus other = (DBOUserTwoFaStatus) obj;
		return Objects.equals(enabled, other.enabled) && Objects.equals(principalId, other.principalId);
	}

	@Override
	public String toString() {
		return "DBOUserTwoFaStatus [principalId=" + principalId + ", enabled=" + enabled + "]";
	}	
	
}
