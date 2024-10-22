package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TERMS_OF_USE_AGREEMENT_AGREEMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TERMS_OF_USE_AGREEMENT_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_TERMS_OF_USE_AGREEMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TERMS_OF_USE_AGREEMENT;

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

@Deprecated
public class DBOTermsOfUseAgreement
		implements MigratableDatabaseObject<DBOTermsOfUseAgreement, DBOTermsOfUseAgreement> {

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("principalId", COL_TERMS_OF_USE_AGREEMENT_PRINCIPAL_ID).withIsPrimaryKey(true)
					.withIsBackupId(true),
			new FieldColumn("agreesToTermsOfUse", COL_TERMS_OF_USE_AGREEMENT_AGREEMENT) };

	private Long principalId;
	private Boolean agreesToTermsOfUse;

	@Override
	public TableMapping<DBOTermsOfUseAgreement> getTableMapping() {
		return new TableMapping<DBOTermsOfUseAgreement>() {

			@Override
			public DBOTermsOfUseAgreement mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOTermsOfUseAgreement dbo = new DBOTermsOfUseAgreement();
				dbo.setPrincipalId(rs.getLong(COL_TERMS_OF_USE_AGREEMENT_PRINCIPAL_ID));
				dbo.setAgreesToTermsOfUse(rs.getBoolean(COL_TERMS_OF_USE_AGREEMENT_AGREEMENT));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_TERMS_OF_USE_AGREEMENT;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public String getDDLFileName() {
				return DDL_TERMS_OF_USE_AGREEMENT;
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
		return new BasicMigratableTableTranslation<DBOTermsOfUseAgreement>();
	}

	@Override
	public Class<? extends DBOTermsOfUseAgreement> getBackupClass() {
		return DBOTermsOfUseAgreement.class;
	}

	@Override
	public Class<? extends DBOTermsOfUseAgreement> getDatabaseObjectClass() {
		return DBOTermsOfUseAgreement.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(agreesToTermsOfUse, principalId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOTermsOfUseAgreement other = (DBOTermsOfUseAgreement) obj;
		return Objects.equals(agreesToTermsOfUse, other.agreesToTermsOfUse)
				&& Objects.equals(principalId, other.principalId);
	}

	@Override
	public String toString() {
		return "DBOTermsOfUseAgreement [principalId=" + principalId + ", agreesToTermsOfUse=" + agreesToTermsOfUse
				+ "]";
	}
}
