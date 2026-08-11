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

public class DBOEDucQuota implements MigratableDatabaseObject<DBOEDucQuota, DBOEDucQuota> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", SqlConstants.COL_EDUC_QUOTA_ID, true).withIsBackupId(true),
			new FieldColumn("userId", SqlConstants.COL_EDUC_QUOTA_USER_ID),
			new FieldColumn("accessRequirementId", SqlConstants.COL_EDUC_QUOTA_ACCESS_REQUIREMENT_ID),
			new FieldColumn("createdOn", SqlConstants.COL_EDUC_QUOTA_CREATED_ON),
			new FieldColumn("envelopeId", SqlConstants.COL_EDUC_QUOTA_ENVELOPE_ID)
	};

	private static final TableMapping<DBOEDucQuota> TABLE_MAPPING = new TableMapping<DBOEDucQuota>() {

		@Override
		public DBOEDucQuota mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOEDucQuota dbo = new DBOEDucQuota();
			dbo.setId(rs.getLong(SqlConstants.COL_EDUC_QUOTA_ID));
			dbo.setUserId(rs.getLong(SqlConstants.COL_EDUC_QUOTA_USER_ID));
			dbo.setAccessRequirementId(rs.getLong(SqlConstants.COL_EDUC_QUOTA_ACCESS_REQUIREMENT_ID));
			dbo.setCreatedOn(rs.getLong(SqlConstants.COL_EDUC_QUOTA_CREATED_ON));
			dbo.setEnvelopeId(rs.getString(SqlConstants.COL_EDUC_QUOTA_ENVELOPE_ID));
			return dbo;
		}

		@Override
		public String getTableName() {
			return SqlConstants.TABLE_EDUC_QUOTA;
		}

		@Override
		public String getDDLFileName() {
			return SqlConstants.DDL_EDUC_QUOTA;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOEDucQuota> getDBOClass() {
			return DBOEDucQuota.class;
		}
	};

	private static final MigratableTableTranslation<DBOEDucQuota, DBOEDucQuota> MIGRATION_TRANSLATOR = new BasicMigratableTableTranslation<>();

	private Long id;
	private Long userId;
	private Long accessRequirementId;
	private Long createdOn;
	private String envelopeId;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Long getAccessRequirementId() {
		return accessRequirementId;
	}

	public void setAccessRequirementId(Long accessRequirementId) {
		this.accessRequirementId = accessRequirementId;
	}

	public Long getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Long createdOn) {
		this.createdOn = createdOn;
	}

	public String getEnvelopeId() {
		return envelopeId;
	}

	public void setEnvelopeId(String envelopeId) {
		this.envelopeId = envelopeId;
	}

	@Override
	public TableMapping<DBOEDucQuota> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.EDUC_QUOTA;
	}

	@Override
	public MigratableTableTranslation<DBOEDucQuota, DBOEDucQuota> getTranslator() {
		return MIGRATION_TRANSLATOR;
	}

	@Override
	public Class<? extends DBOEDucQuota> getBackupClass() {
		return DBOEDucQuota.class;
	}

	@Override
	public Class<? extends DBOEDucQuota> getDatabaseObjectClass() {
		return DBOEDucQuota.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, userId, accessRequirementId, createdOn, envelopeId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DBOEDucQuota other = (DBOEDucQuota) obj;
		return Objects.equals(id, other.id)
				&& Objects.equals(userId, other.userId)
				&& Objects.equals(accessRequirementId, other.accessRequirementId)
				&& Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(envelopeId, other.envelopeId);
	}

	@Override
	public String toString() {
		return "DBOEDucQuota [id=" + id + ", userId=" + userId
				+ ", accessRequirementId=" + accessRequirementId + ", createdOn=" + createdOn
				+ ", envelopeId=" + envelopeId + "]";
	}
}
