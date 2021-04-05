package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHENTICATED_ON_AUTHENTICATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHENTICATED_ON_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHENTICATED_ON_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_AUTHENTICATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;

import java.util.Date;
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

@Table(name = TABLE_AUTHENTICATED_ON)
public class DBOAuthenticatedOn implements MigratableDatabaseObject<DBOAuthenticatedOn, DBOAuthenticatedOn> {
	
	private static TableMapping<DBOAuthenticatedOn> tableMapping = AutoTableMapping.create(DBOAuthenticatedOn.class);
	
	@Field(name = COL_AUTHENTICATED_ON_PRINCIPAL_ID, backupId = true, primary = true, nullable = false)
	@ForeignKey(table = TABLE_USER_GROUP, field = COL_USER_GROUP_ID, cascadeDelete = true)
	private Long principalId;
	
	@Field(name = COL_AUTHENTICATED_ON_ETAG, backupId = false, primary = false, nullable = false, etag=true)
	private String etag;
	
	@Field(name = COL_AUTHENTICATED_ON_AUTHENTICATED_ON)
	private Date authenticatedOn;

	@Override
	public TableMapping<DBOAuthenticatedOn> getTableMapping() {
		return tableMapping;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public Long getPrincipalId() {
		return principalId;
	}
	public void setPrincipalId(Long principalId) {
		this.principalId = principalId;
	}


	public Date getAuthenticatedOn() {
		return authenticatedOn;
	}

	public void setAuthenticatedOn(Date authenticatedOn) {
		this.authenticatedOn = authenticatedOn;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.SESSION_TOKEN;
	}

	@Override
	public MigratableTableTranslation<DBOAuthenticatedOn, DBOAuthenticatedOn> getTranslator() {
		return new BasicMigratableTableTranslation<DBOAuthenticatedOn>();	
	}

	@Override
	public Class<? extends DBOAuthenticatedOn> getBackupClass() {
		return DBOAuthenticatedOn.class;
	}

	@Override
	public Class<? extends DBOAuthenticatedOn> getDatabaseObjectClass() {
		return DBOAuthenticatedOn.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((authenticatedOn == null) ? 0 : authenticatedOn.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
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
		DBOAuthenticatedOn other = (DBOAuthenticatedOn) obj;
		if (authenticatedOn == null) {
			if (other.authenticatedOn != null)
				return false;
		} else if (!authenticatedOn.equals(other.authenticatedOn))
			return false;
		if (etag == null) {
			if (other.etag != null)
				return false;
		} else if (!etag.equals(other.etag))
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
		return "DBOAuthenticatedOn [principalId=" + principalId + ", etag=" + etag + ", authenticatedOn="
				+ authenticatedOn + "]";
	}



}
