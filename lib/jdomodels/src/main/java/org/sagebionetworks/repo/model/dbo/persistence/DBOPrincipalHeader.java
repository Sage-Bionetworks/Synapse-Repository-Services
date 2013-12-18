package org.sagebionetworks.repo.model.dbo.persistence;

import java.util.List;

import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.NameType;
import org.sagebionetworks.repo.model.PrincipalType;
import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.ForeignKey;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

@Table(name = SqlConstants.TABLE_PRINCIPAL_HEADER)
public class DBOPrincipalHeader implements
		MigratableDatabaseObject<DBOPrincipalHeader, DBOPrincipalHeader> {

	private static final TableMapping<DBOPrincipalHeader> tableMapping = AutoTableMapping
			.create(DBOPrincipalHeader.class);

	@Override
	public TableMapping<DBOPrincipalHeader> getTableMapping() {
		return tableMapping;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.PRINCIPAL_HEADER;
	}

	@Field(name = SqlConstants.COL_PRINCIPAL_HEADER_ID, primary = true, backupId = true, nullable = false)
	@ForeignKey(table = SqlConstants.TABLE_USER_GROUP, field = SqlConstants.COL_USER_GROUP_ID, cascadeDelete = true)
	private Long principalId;
	
	@Field(name = SqlConstants.COL_PRINCIPAL_HEADER_NAME, primary = true, varchar = 256, nullable = false)
	private String principalName;
	
	@Field(name = SqlConstants.COL_PRINCIPAL_HEADER_PRINCIPAL_TYPE, nullable = false)
	private PrincipalType principalType;
	
	@Field(name = SqlConstants.COL_PRINCIPAL_HEADER_DOMAIN_TYPE, nullable = false)
	private DomainType domainType;
	
	@Field(name = SqlConstants.COL_PRINCIPAL_HEADER_NAME_TYPE, nullable = false)
	private NameType nameType;

	@Override
	public MigratableTableTranslation<DBOPrincipalHeader, DBOPrincipalHeader> getTranslator() {
		// We do not currently have a backup for this object.
		return new MigratableTableTranslation<DBOPrincipalHeader, DBOPrincipalHeader>() {

			@Override
			public DBOPrincipalHeader createDatabaseObjectFromBackup(
					DBOPrincipalHeader backup) {
				return backup;
			}

			@Override
			public DBOPrincipalHeader createBackupFromDatabaseObject(
					DBOPrincipalHeader dbo) {
				return dbo;
			}
		};
	}

	@Override
	public Class<? extends DBOPrincipalHeader> getBackupClass() {
		return DBOPrincipalHeader.class;
	}

	@Override
	public Class<? extends DBOPrincipalHeader> getDatabaseObjectClass() {
		return DBOPrincipalHeader.class;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}

	public String getPrincipalTypeAsString() {
		return principalType == null ? null : principalType.name();
	}

	public String getDomainTypeAsString() {
		return domainType == null ? null : domainType.name();
	}

	public String getNameTypeAsString() {
		return nameType == null ? null : nameType.name();
	}

	public Long getPrincipalId() {
		return principalId;
	}

	public void setPrincipalId(Long principalId) {
		this.principalId = principalId;
	}

	public String getPrincipalName() {
		return principalName;
	}

	public void setPrincipalName(String principalName) {
		this.principalName = principalName;
	}

	public PrincipalType getPrincipalType() {
		return principalType;
	}

	public void setPrincipalType(PrincipalType principalType) {
		this.principalType = principalType;
	}

	public DomainType getDomainType() {
		return domainType;
	}

	public void setDomainType(DomainType domainType) {
		this.domainType = domainType;
	}

	public NameType getNameType() {
		return nameType;
	}

	public void setNameType(NameType nameType) {
		this.nameType = nameType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((domainType == null) ? 0 : domainType.hashCode());
		result = prime * result
				+ ((nameType == null) ? 0 : nameType.hashCode());
		result = prime * result
				+ ((principalId == null) ? 0 : principalId.hashCode());
		result = prime * result
				+ ((principalName == null) ? 0 : principalName.hashCode());
		result = prime * result
				+ ((principalType == null) ? 0 : principalType.hashCode());
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
		DBOPrincipalHeader other = (DBOPrincipalHeader) obj;
		if (domainType != other.domainType)
			return false;
		if (nameType != other.nameType)
			return false;
		if (principalId == null) {
			if (other.principalId != null)
				return false;
		} else if (!principalId.equals(other.principalId))
			return false;
		if (principalName == null) {
			if (other.principalName != null)
				return false;
		} else if (!principalName.equals(other.principalName))
			return false;
		if (principalType != other.principalType)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOPrincipalHeader [principalId=" + principalId
				+ ", principalName=" + principalName + ", principalType="
				+ principalType + ", domainType=" + domainType + ", nameType="
				+ nameType + "]";
	}

	
}
