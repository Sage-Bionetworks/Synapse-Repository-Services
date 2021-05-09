package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SESSION_TOKEN_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SESSION_TOKEN_SESSION_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SESSION_TOKEN;
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

@Table(name = TABLE_SESSION_TOKEN, constraints = "UNIQUE KEY `UNIQUE_SESSION_TOKEN` (`"+COL_SESSION_TOKEN_SESSION_TOKEN+"`)")
public class DBOSessionToken implements MigratableDatabaseObject<DBOSessionToken, DBOSessionToken> {
	
	private static TableMapping<DBOSessionToken> tableMapping = AutoTableMapping.create(DBOSessionToken.class);
	
	@Field(name = COL_SESSION_TOKEN_PRINCIPAL_ID, primary = true, backupId = true)
	@ForeignKey(table = TABLE_USER_GROUP, field = COL_USER_GROUP_ID, cascadeDelete = true)
	private Long principalId;
	

	@Field(name = COL_SESSION_TOKEN_SESSION_TOKEN, varchar = 100)
	private String sessionToken;

	@Override
	public TableMapping<DBOSessionToken> getTableMapping() {
		return tableMapping;
	}
	
	public Long getPrincipalId() {
		return principalId;
	}
	public void setPrincipalId(Long principalId) {
		this.principalId = principalId;
	}
	public String getSessionToken() {
		return sessionToken;
	}
	public void setSessionToken(String sessionToken) {
		this.sessionToken = sessionToken;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.SESSION_TOKEN;
	}

	@Override
	public MigratableTableTranslation<DBOSessionToken, DBOSessionToken> getTranslator() {
		return new BasicMigratableTableTranslation<DBOSessionToken>();	
	}

	@Override
	public Class<? extends DBOSessionToken> getBackupClass() {
		return DBOSessionToken.class;
	}

	@Override
	public Class<? extends DBOSessionToken> getDatabaseObjectClass() {
		return DBOSessionToken.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((principalId == null) ? 0 : principalId.hashCode());
		result = prime * result + ((sessionToken == null) ? 0 : sessionToken.hashCode());
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
		DBOSessionToken other = (DBOSessionToken) obj;
		if (principalId == null) {
			if (other.principalId != null)
				return false;
		} else if (!principalId.equals(other.principalId))
			return false;
		if (sessionToken == null) {
			if (other.sessionToken != null)
				return false;
		} else if (!sessionToken.equals(other.sessionToken))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOSessionToken [principalId=" + principalId + ", sessionToken=" + sessionToken + "]";
	}



}
