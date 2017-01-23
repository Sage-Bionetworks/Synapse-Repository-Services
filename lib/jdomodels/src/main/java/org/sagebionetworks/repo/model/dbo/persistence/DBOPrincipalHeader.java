package org.sagebionetworks.repo.model.dbo.persistence;

import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.PrincipalType;
import org.sagebionetworks.repo.model.dbo.AutoTableMapping;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.Field;
import org.sagebionetworks.repo.model.dbo.Table;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

@Table(name = SqlConstants.TABLE_PRINCIPAL_HEADER, constraints = "INDEX (" + SqlConstants.COL_PRINCIPAL_HEADER_FRAGMENT + ")")
public class DBOPrincipalHeader implements DatabaseObject<DBOPrincipalHeader> {

	private static final TableMapping<DBOPrincipalHeader> tableMapping = AutoTableMapping
			.create(DBOPrincipalHeader.class);

	@Override
	public TableMapping<DBOPrincipalHeader> getTableMapping() {
		return tableMapping;
	}

	// Note: There is no FK (nor cascade delete) on UserGroup because the FK introduces a few locks that may deadlock user actions
	//   For example, the PrincipalHeaderWorker updates a Principal, read-locking the UserGroup row
	//   If a separate (to be implemented) method tries to query on the PrincipalHeader table in the same transaction as an update on 
	//     the UserGroup (or a secondary) table, then there is the possibility that the worker will cause the user's action to fail.  
	@Field(name = SqlConstants.COL_PRINCIPAL_HEADER_ID, primary = true, nullable = false)
	private Long principalId;

	@Field(name = SqlConstants.COL_PRINCIPAL_HEADER_FRAGMENT, primary = true, varchar = 256, nullable = false)
	private String fragment;
	
	@Field(name = SqlConstants.COL_PRINCIPAL_HEADER_SOUNDEX, varchar = 4, nullable = false)
	private String soundex;

	@Field(name = SqlConstants.COL_PRINCIPAL_HEADER_PRINCIPAL_TYPE, nullable = false)
	private PrincipalType principalType;

	@Field(name = SqlConstants.COL_PRINCIPAL_HEADER_DOMAIN_TYPE, nullable = true)
	private DomainType domainType;

	public String getPrincipalTypeAsString() {
		return principalType == null ? null : principalType.name();
	}

	public String getDomainTypeAsString() {
		return domainType == null ? null : domainType.name();
	}

	public String getFragment() {
		return fragment;
	}

	public void setFragment(String fragment) {
		this.fragment = fragment;
	}

	public String getSoundex() {
		return soundex;
	}

	public void setSoundex(String soundex) {
		this.soundex = soundex;
	}

	public Long getPrincipalId() {
		return principalId;
	}

	public void setPrincipalId(Long principalId) {
		this.principalId = principalId;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((domainType == null) ? 0 : domainType.hashCode());
		result = prime * result
				+ ((fragment == null) ? 0 : fragment.hashCode());
		result = prime * result
				+ ((principalId == null) ? 0 : principalId.hashCode());
		result = prime * result
				+ ((principalType == null) ? 0 : principalType.hashCode());
		result = prime * result + ((soundex == null) ? 0 : soundex.hashCode());
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
		if (fragment == null) {
			if (other.fragment != null)
				return false;
		} else if (!fragment.equals(other.fragment))
			return false;
		if (principalId == null) {
			if (other.principalId != null)
				return false;
		} else if (!principalId.equals(other.principalId))
			return false;
		if (principalType != other.principalType)
			return false;
		if (soundex == null) {
			if (other.soundex != null)
				return false;
		} else if (!soundex.equals(other.soundex))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOPrincipalHeader [principalId=" + principalId + ", fragment="
				+ fragment + ", soundex=" + soundex + ", principalType="
				+ principalType + ", domainType=" + domainType + "]";
	}

}
