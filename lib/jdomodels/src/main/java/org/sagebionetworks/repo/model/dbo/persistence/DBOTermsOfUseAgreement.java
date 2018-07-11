package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TERMS_OF_USE_AGREEMENT_AGREEMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_TERMS_OF_USE_AGREEMENT_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_TERMS_OF_USE_AGREEMENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

@Table(name = TABLE_TERMS_OF_USE_AGREEMENT)
public class DBOTermsOfUseAgreement implements MigratableDatabaseObject<DBOTermsOfUseAgreement, DBOTermsOfUseAgreement> {
	
	private static TableMapping<DBOTermsOfUseAgreement> tableMapping = AutoTableMapping.create(DBOTermsOfUseAgreement.class);
	
	@Field(name = COL_TERMS_OF_USE_AGREEMENT_PRINCIPAL_ID, primary=true, backupId = true)
	@ForeignKey(table = TABLE_USER_GROUP, field = COL_USER_GROUP_ID, cascadeDelete = true)
	private Long principalId;
	
	@Field(name = COL_TERMS_OF_USE_AGREEMENT_AGREEMENT)
	private Boolean agreesToTermsOfUse;
	
	@Override
	public TableMapping<DBOTermsOfUseAgreement> getTableMapping() {
		return tableMapping;
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
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((agreesToTermsOfUse == null) ? 0 : agreesToTermsOfUse.hashCode());
		result = prime * result + ((principalId == null) ? 0 : principalId.hashCode());
		return result;
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
		if (agreesToTermsOfUse == null) {
			if (other.agreesToTermsOfUse != null)
				return false;
		} else if (!agreesToTermsOfUse.equals(other.agreesToTermsOfUse))
			return false;
		if (principalId == null) {
			if (other.principalId != null)
				return false;
		} else if (!principalId.equals(other.principalId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOTermsOfUseAgreement [principalId=" + principalId + ", agreesToTermsOfUse=" + agreesToTermsOfUse
				+ "]";
	}
}
