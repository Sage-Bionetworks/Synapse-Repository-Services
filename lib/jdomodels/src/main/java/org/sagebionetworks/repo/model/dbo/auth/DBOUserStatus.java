package org.sagebionetworks.repo.model.dbo.auth;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_STATUS_DISABLED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_STATUS_LAST_SEEN_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_STATUS_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_STATUS_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_STATUS_DISABLE_WARNING_SENT_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_USER_STATUS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_STATUS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOUserStatus implements MigratableDatabaseObject<DBOUserStatus, DBOUserStatus> {
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("principalId", COL_USER_STATUS_PRINCIPAL_ID, true).withIsBackupId(true),
		new FieldColumn("etag", COL_USER_STATUS_ETAG).withIsEtag(true),
		new FieldColumn("lastSeenOn", COL_USER_STATUS_LAST_SEEN_ON),
		new FieldColumn("disabled", COL_USER_STATUS_DISABLED),
		new FieldColumn("disableWarningSentOn", COL_USER_STATUS_DISABLE_WARNING_SENT_ON)
	};
	
	private static final TableMapping<DBOUserStatus> TABLE_MAPPING = new TableMapping<DBOUserStatus>() {

		@Override
		public DBOUserStatus mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOUserStatus status = new DBOUserStatus();
			status.setPrincipalId(rs.getLong(COL_USER_STATUS_PRINCIPAL_ID));
			status.setEtag(rs.getString(COL_USER_STATUS_ETAG));
			status.setLastSeenOn(rs.getTimestamp(COL_USER_STATUS_LAST_SEEN_ON));
			status.setDisabled(rs.getBoolean(COL_USER_STATUS_DISABLED));
			status.setDisableWarningSentOn(rs.getTimestamp(COL_USER_STATUS_DISABLE_WARNING_SENT_ON));

			return status;
		}

		@Override
		public String getTableName() {
			return TABLE_USER_STATUS;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public String getDDLFileName() {
			return DDL_USER_STATUS;
		}

		@Override
		public Class<? extends DBOUserStatus> getDBOClass() {
			return DBOUserStatus.class;
		}
	};
	
	private static final MigratableTableTranslation<DBOUserStatus, DBOUserStatus> TRANSLATOR = new BasicMigratableTableTranslation<>();
	
	private Long principalId;
	private String etag;
	private Timestamp lastSeenOn;
	private Boolean disabled;
	private Timestamp disableWarningSentOn;

	public DBOUserStatus() {
	}

	public Long getPrincipalId() {
		return principalId;
	}

	public void setPrincipalId(Long principalId) {
		this.principalId = principalId;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public Timestamp getLastSeenOn() {
		return lastSeenOn;
	}

	public void setLastSeenOn(Timestamp lastSeenOn) {
		this.lastSeenOn = lastSeenOn;
	}

	public Boolean getDisabled() {
		return disabled;
	}

	public void setDisabled(Boolean disabled) {
		this.disabled = disabled;
	}

	public Timestamp getDisableWarningSentOn() {
		return disableWarningSentOn;
	}

	public void setDisableWarningSentOn(Timestamp disableWarningSentOn) {
		this.disableWarningSentOn = disableWarningSentOn;
	}

	@Override
	public TableMapping<DBOUserStatus> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.USER_STATUS;
	}

	@Override
	public MigratableTableTranslation<DBOUserStatus, DBOUserStatus> getTranslator() {
		return TRANSLATOR;
	}

	@Override
	public Class<? extends DBOUserStatus> getBackupClass() {
		return DBOUserStatus.class;
	}

	@Override
	public Class<? extends DBOUserStatus> getDatabaseObjectClass() {
		return DBOUserStatus.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(disabled, etag, lastSeenOn, principalId, disableWarningSentOn);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOUserStatus)) {
			return false;
		}
		DBOUserStatus other = (DBOUserStatus) obj;
		return Objects.equals(disabled, other.disabled) && Objects.equals(etag, other.etag)
			&& Objects.equals(lastSeenOn, other.lastSeenOn) && Objects.equals(principalId, other.principalId)
			&& Objects.equals(disableWarningSentOn, other.disableWarningSentOn);
	}

	@Override
	public String toString() {
		return String.format("DBOUserStatus [principalId=%s, etag=%s, lastSeenOn=%s, disabled=%s, disableWarningSentOn=%s]",
			principalId, etag, lastSeenOn, disabled, disableWarningSentOn);
	}
	
}
