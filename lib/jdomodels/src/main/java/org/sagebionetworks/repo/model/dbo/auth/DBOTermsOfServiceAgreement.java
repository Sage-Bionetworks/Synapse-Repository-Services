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

public class DBOTermsOfServiceAgreement implements MigratableDatabaseObject<DBOTermsOfServiceAgreement, DBOTermsOfServiceAgreement> {
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", SqlConstants.COL_TOS_AGREEMENT_ID, true).withIsBackupId(true),
		new FieldColumn("createdOn", SqlConstants.COL_TOS_AGREEMENT_CREATED_ON),
		new FieldColumn("createdBy", SqlConstants.COL_TOS_AGREEMENT_CREATED_BY),
		new FieldColumn("version", SqlConstants.COL_TOS_AGREEMENT_VERSION)
	};
	
	private Long id;
	private Date createdOn;
	private Long createdBy;
	private String version;
	
	public Long getId() {
		return id;
	}

	public DBOTermsOfServiceAgreement setId(Long id) {
		this.id = id;
		return this;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public DBOTermsOfServiceAgreement setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
		return this;
	}

	public Long getCreatedBy() {
		return createdBy;
	}

	public DBOTermsOfServiceAgreement setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
		return this;
	}

	public String getVersion() {
		return version;
	}

	public DBOTermsOfServiceAgreement setVersion(String version) {
		this.version = version;
		return this;
	}

	@Override
	public TableMapping<DBOTermsOfServiceAgreement> getTableMapping() {
		return new TableMapping<DBOTermsOfServiceAgreement>() {
			
			@Override
			public DBOTermsOfServiceAgreement mapRow(ResultSet rs, int rowNum) throws SQLException {
				return new DBOTermsOfServiceAgreement()
					.setId(rs.getLong(SqlConstants.COL_TOS_AGREEMENT_ID))
					.setCreatedBy(rs.getLong(SqlConstants.COL_TOS_AGREEMENT_CREATED_BY))
					.setCreatedOn(rs.getTimestamp(SqlConstants.COL_TOS_AGREEMENT_CREATED_ON))
					.setVersion(rs.getString(SqlConstants.COL_TOS_AGREEMENT_VERSION));
			}
			
			@Override
			public String getTableName() {
				return SqlConstants.TABLE_TOS_AGREEMENT;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return SqlConstants.DDL_TABLE_TOS_AGREEMENT;
			}
			
			@Override
			public Class<? extends DBOTermsOfServiceAgreement> getDBOClass() {
				return DBOTermsOfServiceAgreement.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.TOS_AGREEMENT;
	}

	@Override
	public MigratableTableTranslation<DBOTermsOfServiceAgreement, DBOTermsOfServiceAgreement> getTranslator() {
		return new BasicMigratableTableTranslation<>();
	}

	@Override
	public Class<? extends DBOTermsOfServiceAgreement> getBackupClass() {
		return DBOTermsOfServiceAgreement.class;
	}

	@Override
	public Class<? extends DBOTermsOfServiceAgreement> getDatabaseObjectClass() {
		return DBOTermsOfServiceAgreement.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdBy, createdOn, id, version);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOTermsOfServiceAgreement)) {
			return false;
		}
		DBOTermsOfServiceAgreement other = (DBOTermsOfServiceAgreement) obj;
		return Objects.equals(createdBy, other.createdBy) && Objects.equals(createdOn, other.createdOn) && Objects.equals(id, other.id)
				&& Objects.equals(version, other.version);
	}

	@Override
	public String toString() {
		return String.format("DBOTermsOfServiceAgreement [id=%s, createdOn=%s, createdBy=%s, version=%s]", id, createdOn, createdBy,
				version);
	}
}
