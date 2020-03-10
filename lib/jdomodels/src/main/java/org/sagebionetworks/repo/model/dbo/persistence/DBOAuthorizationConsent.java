package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHORIZATION_CONSENT_CLIENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHORIZATION_CONSENT_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHORIZATION_CONSENT_GRANTED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHORIZATION_CONSENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHORIZATION_CONSENT_SCOPE_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AUTHORIZATION_CONSENT_USER_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_AUTHORIZATION_CONSENT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_AUTHORIZATION_CONSENT;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOAuthorizationConsent implements MigratableDatabaseObject<DBOAuthorizationConsent, DBOAuthorizationConsent> {
	private Long id;
	private String eTag;
	private Long userId;
	private Long clientId;
	private String scopeHash;
	private Long grantedOn;
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_AUTHORIZATION_CONSENT_ID, true).withIsBackupId(true),
		new FieldColumn("eTag", COL_AUTHORIZATION_CONSENT_ETAG).withIsEtag(true),
		new FieldColumn("userId", COL_AUTHORIZATION_CONSENT_USER_ID),
		new FieldColumn("clientId", COL_AUTHORIZATION_CONSENT_CLIENT_ID),
		new FieldColumn("scopeHash", COL_AUTHORIZATION_CONSENT_SCOPE_HASH),
		new FieldColumn("grantedOn", COL_AUTHORIZATION_CONSENT_GRANTED_ON)
		};
	
	public static final TableMapping<DBOAuthorizationConsent> TABLE_MAPPING = new TableMapping<DBOAuthorizationConsent>() {
		// Map a result set to this object
		@Override
		public DBOAuthorizationConsent mapRow(ResultSet rs, int rowNum)	throws SQLException {
			DBOAuthorizationConsent consent = new DBOAuthorizationConsent();
			consent.setId(rs.getLong(COL_AUTHORIZATION_CONSENT_ID));
			consent.seteTag(rs.getString(COL_AUTHORIZATION_CONSENT_ETAG));
			consent.setUserId(rs.getLong(COL_AUTHORIZATION_CONSENT_USER_ID));
			consent.setClientId(rs.getLong(COL_AUTHORIZATION_CONSENT_CLIENT_ID));
			consent.setScopeHash(rs.getString(COL_AUTHORIZATION_CONSENT_SCOPE_HASH));
			consent.setGrantedOn(rs.getLong(COL_AUTHORIZATION_CONSENT_GRANTED_ON));
			return consent;
		}

		@Override
		public String getTableName() {
			return TABLE_AUTHORIZATION_CONSENT;
		}

		@Override
		public String getDDLFileName() {
			return DDL_AUTHORIZATION_CONSENT;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOAuthorizationConsent> getDBOClass() {
			return DBOAuthorizationConsent.class;
		}
	};

	@Override
	public TableMapping<DBOAuthorizationConsent> getTableMapping() {
		return TABLE_MAPPING;
	}
	
	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.AUTHORIZATION_GRANT;
	}
	
	private static final  MigratableTableTranslation<DBOAuthorizationConsent, DBOAuthorizationConsent> TRANSLATOR = 
			new BasicMigratableTableTranslation<DBOAuthorizationConsent>();
			
	@Override
	public MigratableTableTranslation<DBOAuthorizationConsent, DBOAuthorizationConsent> getTranslator() {
			return TRANSLATOR;
	}


	@Override
	public Class<? extends DBOAuthorizationConsent> getBackupClass() {
		return DBOAuthorizationConsent.class;
	}


	@Override
	public Class<? extends DBOAuthorizationConsent> getDatabaseObjectClass() {
		return DBOAuthorizationConsent.class;
	}


	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String geteTag() {
		return eTag;
	}

	public void seteTag(String eTag) {
		this.eTag = eTag;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Long getClientId() {
		return clientId;
	}

	public void setClientId(Long clientId) {
		this.clientId = clientId;
	}

	public String getScopeHash() {
		return scopeHash;
	}

	public void setScopeHash(String scopeHash) {
		this.scopeHash = scopeHash;
	}

	public Long getGrantedOn() {
		return grantedOn;
	}

	public void setGrantedOn(Long grantedOn) {
		this.grantedOn = grantedOn;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((clientId == null) ? 0 : clientId.hashCode());
		result = prime * result + ((eTag == null) ? 0 : eTag.hashCode());
		result = prime * result + ((grantedOn == null) ? 0 : grantedOn.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((scopeHash == null) ? 0 : scopeHash.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
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
		DBOAuthorizationConsent other = (DBOAuthorizationConsent) obj;
		if (clientId == null) {
			if (other.clientId != null)
				return false;
		} else if (!clientId.equals(other.clientId))
			return false;
		if (eTag == null) {
			if (other.eTag != null)
				return false;
		} else if (!eTag.equals(other.eTag))
			return false;
		if (grantedOn == null) {
			if (other.grantedOn != null)
				return false;
		} else if (!grantedOn.equals(other.grantedOn))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (scopeHash == null) {
			if (other.scopeHash != null)
				return false;
		} else if (!scopeHash.equals(other.scopeHash))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOAuthorizationConsent [id=" + id + ", eTag=" + eTag + ", userId=" + userId + ", clientId=" + clientId
				+ ", scopeHash=" + scopeHash + ", grantedOn=" + grantedOn + "]";
	}

}
