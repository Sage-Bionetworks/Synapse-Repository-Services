package org.sagebionetworks.repo.model.dbo.auth;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

public class DBOTermsOfServiceRequirements implements MigratableDatabaseObject<DBOTermsOfServiceRequirements, DBOTermsOfServiceRequirements> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", SqlConstants.COL_TOS_REQUIREMENTS_ID, true).withIsBackupId(true),
		new FieldColumn("createdOn", SqlConstants.COL_TOS_REQUIREMENTS_CREATED_ON),
		new FieldColumn("createdBy", SqlConstants.COL_TOS_REQUIREMENTS_CREATED_BY),
		new FieldColumn("minVersion", SqlConstants.COL_TOS_REQUIREMENTS_MIN_VERSION),
		new FieldColumn("enforcedOn", SqlConstants.COL_TOS_REQUIREMENTS_ENFORCED_ON)
	};
	
	private Long id;
	private Date createdOn;
	private Long createdBy;
	private String minVersion;
	private Date enforcedOn;
	
	public Long getId() {
		return id;
	}

	public DBOTermsOfServiceRequirements setId(Long id) {
		this.id = id;
		return this;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public DBOTermsOfServiceRequirements setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
		return this;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public DBOTermsOfServiceRequirements setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public String getMinVersion() {
		return minVersion;
	}

	public DBOTermsOfServiceRequirements setMinVersion(String minVersion) {
		this.minVersion = minVersion;
		return this;
	}

	public Date getEnforcedOn() {
		return enforcedOn;
	}

	public DBOTermsOfServiceRequirements setEnforcedOn(Date enforcedOn) {
		this.enforcedOn = enforcedOn;
		return this;
	}

	@Override
	public TableMapping<DBOTermsOfServiceRequirements> getTableMapping() {
		return new TableMapping<DBOTermsOfServiceRequirements>() {
			
			@Override
			public DBOTermsOfServiceRequirements mapRow(ResultSet rs, int rowNum) throws SQLException {
				return new DBOTermsOfServiceRequirements()
					.setId(rs.getLong(SqlConstants.COL_TOS_REQUIREMENTS_ID))
					.setCreatedOn(new Date(rs.getTimestamp(SqlConstants.COL_TOS_REQUIREMENTS_CREATED_ON).getTime()))
					.setCreatedBy(rs.getLong(SqlConstants.COL_TOS_REQUIREMENTS_CREATED_BY))
					.setMinVersion(rs.getString(SqlConstants.COL_TOS_REQUIREMENTS_MIN_VERSION))
					.setEnforcedOn(new Date(rs.getTimestamp(SqlConstants.COL_TOS_REQUIREMENTS_ENFORCED_ON).getTime()));
			}
			
			@Override
			public String getTableName() {
				return SqlConstants.TABLE_TOS_REQUIREMENTS;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return SqlConstants.DDL_TABLE_TOS_REQUIREMENTS;
			}
			
			@Override
			public Class<? extends DBOTermsOfServiceRequirements> getDBOClass() {
				return DBOTermsOfServiceRequirements.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.TOS_REQUIREMENTS;
	}

	@Override
	public MigratableTableTranslation<DBOTermsOfServiceRequirements, DBOTermsOfServiceRequirements> getTranslator() {
		return new BasicMigratableTableTranslation<>();
	}

	@Override
	public Class<? extends DBOTermsOfServiceRequirements> getBackupClass() {
		return DBOTermsOfServiceRequirements.class;
	}

	@Override
	public Class<? extends DBOTermsOfServiceRequirements> getDatabaseObjectClass() {
		return DBOTermsOfServiceRequirements.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdBy, createdOn, enforcedOn, id, minVersion);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOTermsOfServiceRequirements)) {
			return false;
		}
		DBOTermsOfServiceRequirements other = (DBOTermsOfServiceRequirements) obj;
		return Objects.equals(createdBy, other.createdBy) && Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(enforcedOn, other.enforcedOn) && Objects.equals(id, other.id)
				&& Objects.equals(minVersion, other.minVersion);
	}

	@Override
	public String toString() {
		return String.format("DBOTermsOfServiceRequirements [id=%s, createdOn=%s, createdBy=%s, minVersion=%s, enforcedOn=%s]", id,
				createdOn, createdBy, minVersion, enforcedOn);
	}

}
