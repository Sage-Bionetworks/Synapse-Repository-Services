package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_CLAIMS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_CLIENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_LAST_USED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_SCOPES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_OAUTH_REFRESH_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_OAUTH_REFRESH_TOKEN;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Database object representing an OAuth 2.0 refresh token
 */
public class DBOOAuthRefreshToken implements MigratableDatabaseObject<DBOOAuthRefreshToken, DBOOAuthRefreshToken> {
	private Long id;
	private String tokenHash;
	private String name;
	private Long principalId;
	private Long clientId;
	private byte[] scopes;
	private byte[] claims;
	private Timestamp createdOn;
	private Timestamp modifiedOn;
	private Timestamp lastUsed;
	private String etag;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_OAUTH_REFRESH_TOKEN_ID, true).withIsBackupId(true),
			new FieldColumn("tokenHash", COL_OAUTH_REFRESH_TOKEN_HASH),
			new FieldColumn("name", COL_OAUTH_REFRESH_TOKEN_NAME),
			new FieldColumn("principalId", COL_OAUTH_REFRESH_TOKEN_PRINCIPAL_ID),
			new FieldColumn("clientId", COL_OAUTH_REFRESH_TOKEN_CLIENT_ID),
			new FieldColumn("scopes", COL_OAUTH_REFRESH_TOKEN_SCOPES),
			new FieldColumn("claims", COL_OAUTH_REFRESH_TOKEN_CLAIMS),
			new FieldColumn("createdOn", COL_OAUTH_REFRESH_TOKEN_CREATED_ON),
			new FieldColumn("modifiedOn", COL_OAUTH_REFRESH_TOKEN_MODIFIED_ON),
			new FieldColumn("lastUsed", COL_OAUTH_REFRESH_TOKEN_LAST_USED),
			new FieldColumn("etag", COL_OAUTH_REFRESH_TOKEN_ETAG).withIsEtag(true),
		};

	private static DBOOAuthRefreshToken mapRow(ResultSet rs, int rowNum) throws SQLException {
		DBOOAuthRefreshToken token = new DBOOAuthRefreshToken();
		token.setId(rs.getLong(COL_OAUTH_REFRESH_TOKEN_ID));
		token.setTokenHash(rs.getString(COL_OAUTH_REFRESH_TOKEN_HASH));
		token.setName(rs.getString(COL_OAUTH_REFRESH_TOKEN_NAME));
		token.setPrincipalId(rs.getLong(COL_OAUTH_REFRESH_TOKEN_PRINCIPAL_ID));
		token.setClientId(rs.getLong(COL_OAUTH_REFRESH_TOKEN_CLIENT_ID));
		token.setScopes(rs.getBytes(COL_OAUTH_REFRESH_TOKEN_SCOPES));
		token.setClaims(rs.getBytes(COL_OAUTH_REFRESH_TOKEN_CLAIMS));
		token.setCreatedOn(rs.getTimestamp(COL_OAUTH_REFRESH_TOKEN_CREATED_ON));
		token.setModifiedOn(rs.getTimestamp(COL_OAUTH_REFRESH_TOKEN_MODIFIED_ON));
		token.setLastUsed(rs.getTimestamp(COL_OAUTH_REFRESH_TOKEN_LAST_USED));
		token.setEtag(rs.getString(COL_OAUTH_REFRESH_TOKEN_ETAG));
		return token;
	}

	@Override
	public TableMapping<DBOOAuthRefreshToken> getTableMapping() {
		return new TableMapping<DBOOAuthRefreshToken>() {
			// Map a result set to this object
			@Override
			public DBOOAuthRefreshToken mapRow(ResultSet rs, int rowNum) throws SQLException {
				// Use static method to avoid unintentionally referencing `this`
				return DBOOAuthRefreshToken.mapRow(rs, rowNum);
			}

			@Override
			public String getTableName() {
				return TABLE_OAUTH_REFRESH_TOKEN;
			}

			@Override
			public String getDDLFileName() {
				return DDL_OAUTH_REFRESH_TOKEN;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOOAuthRefreshToken> getDBOClass() {
				return DBOOAuthRefreshToken.class;
			}
		};
	}


	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public void setTokenHash(String tokenHash) {
		this.tokenHash = tokenHash;
	}

	public Long getPrincipalId() {
		return principalId;
	}

	public void setPrincipalId(Long principalId) {
		this.principalId = principalId;
	}

	public Long getClientId() {
		return clientId;
	}

	public void setClientId(Long clientId) {
		this.clientId = clientId;
	}

	public byte[] getScopes() {
		return this.scopes;
	}

	public void setScopes(byte[] scopes) {
		this.scopes = scopes;
	}

	public byte[] getClaims() {
		return claims;
	}

	public void setClaims(byte[] claims) {
		this.claims = claims;
	}

	public Timestamp getLastUsed() {
		return lastUsed;
	}

	public void setLastUsed(Timestamp lastUsed) {
		this.lastUsed = lastUsed;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}

	public Timestamp getModifiedOn() {
		return modifiedOn;
	}

	public void setModifiedOn(Timestamp modifiedOn) {
		this.modifiedOn = modifiedOn;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String eTag) {
		this.etag = eTag;
	}

	@Override
	public String toString() {
		return "DBOOAuthClient [id=" + id + ", tokenHash=" + tokenHash +", name=" + name +
				", principalId=" + principalId + ", clientId=" + clientId + ", scopes=" + Arrays.toString(scopes) +
				", claims=" + Arrays.toString(claims) +	", createdOn=" + createdOn + ", modifiedOn=" + modifiedOn
				+ ", lastUsed=" + lastUsed + ", eTag=" + etag + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((tokenHash == null) ? 0 : tokenHash.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((principalId == null) ? 0 : principalId.hashCode());
		result = prime * result + ((clientId == null) ? 0 : clientId.hashCode());
		result = prime * result + ((scopes == null) ? 0 : Arrays.hashCode(scopes));
		result = prime * result + ((scopes == null) ? 0 : Arrays.hashCode(claims));
		result = prime * result + ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((modifiedOn == null) ? 0 : modifiedOn.hashCode());
		result = prime * result + ((lastUsed == null) ? 0 : lastUsed.hashCode());
		result = prime * result + ((etag == null) ? 0 : etag.hashCode());
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
		DBOOAuthRefreshToken other = (DBOOAuthRefreshToken) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (tokenHash == null) {
			if (other.tokenHash != null)
				return false;
		} else if (!tokenHash.equals(other.tokenHash))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (principalId == null) {
			if (other.principalId != null)
				return false;
		} else if (!principalId.equals(other.principalId))
			return false;
		if (clientId == null) {
			if (other.clientId != null)
				return false;
		} else if (!clientId.equals(other.clientId))
			return false;
		if (scopes == null) {
			if (other.scopes != null)
				return false;
		} else if (!Arrays.equals(scopes, other.scopes))
			return false;
		if (claims == null) {
			if (other.claims != null)
				return false;
		} else if (!Arrays.equals(claims, other.claims))
			return false;
		if (createdOn == null) {
			if (other.createdOn != null) {
				return false;
			}
		} else if (!createdOn.equals(other.createdOn)) {
			return false;
		}
		if (modifiedOn == null) {
			if (other.modifiedOn != null) {
				return false;
			}
		} else if (!modifiedOn.equals(other.modifiedOn)) {
			return false;
		}
		if (lastUsed == null) {
			if (other.lastUsed != null) {
				return false;
			}
		} else if (!lastUsed.equals(other.lastUsed)) {
			return false;
		}
		if (etag == null) {
			if (other.etag != null) {
				return false;
			}
		} else if (!etag.equals(other.etag)) {
			return false;
		}

		return true;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.OAUTH_REFRESH_TOKEN;
	}
	
	@Override
	public MigratableTableTranslation<DBOOAuthRefreshToken, DBOOAuthRefreshToken> getTranslator() {
			return new BasicMigratableTableTranslation<DBOOAuthRefreshToken>();
	}


	@Override
	public Class<? extends DBOOAuthRefreshToken> getBackupClass() {
		return DBOOAuthRefreshToken.class;
	}


	@Override
	public Class<? extends DBOOAuthRefreshToken> getDatabaseObjectClass() {
		return DBOOAuthRefreshToken.class;
	}


	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}



}
