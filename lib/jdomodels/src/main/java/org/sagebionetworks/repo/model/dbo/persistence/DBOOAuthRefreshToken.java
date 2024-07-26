package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_CLAIMS_JSON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_CLIENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_LAST_USED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_REFRESH_TOKEN_SCOPES_JSON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_OAUTH_REFRESH_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_OAUTH_REFRESH_TOKEN;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.UnmodifiableXStream;
import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigrateFromXStreamToJSON;
import org.sagebionetworks.repo.model.dbo.migration.XStreamToJsonTranslator;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OAuthScopeList;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequest;

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
	private String scopesJson;
	private byte[] claims;
	private String claimsJson;
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
			new FieldColumn("scopesJson", COL_OAUTH_REFRESH_TOKEN_SCOPES_JSON),
			new FieldColumn("claimsJson", COL_OAUTH_REFRESH_TOKEN_CLAIMS_JSON),
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
		token.setScopesJson(rs.getString(COL_OAUTH_REFRESH_TOKEN_SCOPES_JSON));
		token.setClaimsJson(rs.getString(COL_OAUTH_REFRESH_TOKEN_CLAIMS_JSON));
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

	public String getScopesJson() {
		return scopesJson;
	}

	public void setScopesJson(String scopesJson) {
		this.scopesJson = scopesJson;
	}

	public String getClaimsJson() {
		return claimsJson;
	}

	public void setClaimsJson(String claimsJson) {
		this.claimsJson = claimsJson;
	}

	@Override
	public String toString() {
		return "DBOOAuthRefreshToken [id=" + id + ", tokenHash=" + tokenHash + ", name=" + name + ", principalId="
				+ principalId + ", clientId=" + clientId + ", scopes=" + Arrays.toString(scopes) + ", scopesJson="
				+ scopesJson + ", claims=" + Arrays.toString(claims) + ", claimsJson=" + claimsJson + ", createdOn="
				+ createdOn + ", modifiedOn=" + modifiedOn + ", lastUsed=" + lastUsed + ", etag=" + etag + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(claims);
		result = prime * result + Arrays.hashCode(scopes);
		result = prime * result + Objects.hash(claimsJson, clientId, createdOn, etag, id, lastUsed, modifiedOn, name,
				principalId, scopesJson, tokenHash);
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
		return Arrays.equals(claims, other.claims) && Objects.equals(claimsJson, other.claimsJson)
				&& Objects.equals(clientId, other.clientId) && Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(etag, other.etag) && Objects.equals(id, other.id)
				&& Objects.equals(lastUsed, other.lastUsed) && Objects.equals(modifiedOn, other.modifiedOn)
				&& Objects.equals(name, other.name) && Objects.equals(principalId, other.principalId)
				&& Arrays.equals(scopes, other.scopes) && Objects.equals(scopesJson, other.scopesJson)
				&& Objects.equals(tokenHash, other.tokenHash);
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.OAUTH_REFRESH_TOKEN;
	}
	
	public static UnmodifiableXStream XSTREAM = UnmodifiableXStream.builder()
			.allowTypes(List.class, OAuthScope.class, OIDCClaimsRequest.class).build();
	
	@Override
	public MigratableTableTranslation<DBOOAuthRefreshToken, DBOOAuthRefreshToken> getTranslator() {
		return new MigratableTableTranslation<DBOOAuthRefreshToken, DBOOAuthRefreshToken>() {

			@Override
			public DBOOAuthRefreshToken createDatabaseObjectFromBackup(DBOOAuthRefreshToken backup) {
				try {
					if (backup.getScopes() != null) {
						if (backup.getScopesJson() != null) {
							throw new IllegalArgumentException(
									String.format("Both '%s' and '%s' are not null", "scopes", "scopesJson"));
						}
						List<OAuthScope> scopes = (List<OAuthScope>) JDOSecondaryPropertyUtils.decompressObject(XSTREAM,
								backup.getScopes());
						backup.setScopesJson(
								JDOSecondaryPropertyUtils.createJSONFromObject(new OAuthScopeList().setList(scopes)));
						backup.setScopes(null);
					}
					if (backup.getClaims() != null) {
						if (backup.getClaimsJson() != null) {
							throw new IllegalArgumentException(
									String.format("Both '%s' and '%s' are not null", "claims", "claimsJson"));
						}
						OIDCClaimsRequest claim = (OIDCClaimsRequest) JDOSecondaryPropertyUtils
								.decompressObject(XSTREAM, backup.getClaims());
						backup.setClaimsJson(JDOSecondaryPropertyUtils.createJSONFromObject(claim));
						backup.setClaims(null);
					}
					return backup;
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			@Override
			public DBOOAuthRefreshToken createBackupFromDatabaseObject(DBOOAuthRefreshToken dbo) {
				return dbo;
			}
		};
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
