package org.sagebionetworks.repo.model.dbo.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

public class DBOTermsOfUseAgreement implements MigratableDatabaseObject<DBOTermsOfUseAgreement, DBOTermsOfUseAgreement> {
	private Long principalId;
	private String domain;
	private Boolean agreesToTermsOfUse;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("principalId", SqlConstants.COL_TERMS_OF_USE_AGREEMENT_PRINCIPAL_ID, true).withIsBackupId(true),
		new FieldColumn("domain", SqlConstants.COL_TERMS_OF_USE_AGREEMENT_DOMAIN),
		new FieldColumn("agreesToTermsOfUse", SqlConstants.COL_TERMS_OF_USE_AGREEMENT_AGREES_TO_TERMS_OF_USE)
	};
	
	@Override
	public TableMapping<DBOTermsOfUseAgreement> getTableMapping() {
		return new TableMapping<DBOTermsOfUseAgreement>() {
			// Map a result set to this object
			@Override
			public DBOTermsOfUseAgreement mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOTermsOfUseAgreement tou = new DBOTermsOfUseAgreement();
				tou.setPrincipalId(rs.getLong(SqlConstants.COL_TERMS_OF_USE_AGREEMENT_PRINCIPAL_ID));
				tou.setDomain(rs.getString(SqlConstants.COL_TERMS_OF_USE_AGREEMENT_DOMAIN));
				tou.setAgreesToTermsOfUse(rs.getBoolean(SqlConstants.COL_TERMS_OF_USE_AGREEMENT_AGREES_TO_TERMS_OF_USE));
				return tou;
			}

			@Override
			public String getTableName() {
				return SqlConstants.TABLE_TERMS_OF_USE_AGREEMENT;
			}

			@Override
			public String getDDLFileName() {
				return SqlConstants.DDL_TERMS_OF_USE_AGREEMENT;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOTermsOfUseAgreement> getDBOClass() {
				return DBOTermsOfUseAgreement.class;
			}
		};
	}
	
	public Long getPrincipalId() {
		return principalId;
	}
	public void setPrincipalId(Long principalId) {
		this.principalId = principalId;
	}
	public String getDomain() {
		return domain;
	}
	public void setDomain(String domain) {
		this.domain = domain;
	}
	public Boolean getAgreesToTermsOfUse() {
		return agreesToTermsOfUse;
	}
	public void setAgreesToTermsOfUse(Boolean agreesToTermsOfUse) {
		this.agreesToTermsOfUse = agreesToTermsOfUse;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.TERMS_OF_USE_AGREEMENT;
	}

	@Override
	public MigratableTableTranslation<DBOTermsOfUseAgreement, DBOTermsOfUseAgreement> getTranslator() {
		return new MigratableTableTranslation<DBOTermsOfUseAgreement, DBOTermsOfUseAgreement>(){
			@Override
			public DBOTermsOfUseAgreement createDatabaseObjectFromBackup(DBOTermsOfUseAgreement backup) {
				return backup;
			}
			@Override
			public DBOTermsOfUseAgreement createBackupFromDatabaseObject(DBOTermsOfUseAgreement dbo) {
				return dbo;
			}
		};
	}

	@Override
	public Class<? extends DBOTermsOfUseAgreement> getBackupClass() {
		return DBOTermsOfUseAgreement.class;
	}

	@Override
	public Class<? extends DBOTermsOfUseAgreement> getDatabaseObjectClass() {
		return DBOTermsOfUseAgreement.class;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}
}
