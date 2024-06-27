package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_ACCESS_TOKEN_CLIENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_ACCESS_TOKEN_CONTEXT;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_ACCESS_TOKEN_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_ACCESS_TOKEN_EXPIRES_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_ACCESS_TOKEN_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_ACCESS_TOKEN_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_ACCESS_TOKEN_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OAUTH_ACCESS_TOKEN_TOKEN_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_OAUTH_ACCESS_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_OAUTH_ACCESS_TOKEN;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * Database object representing an OAuth 2.0 access token
 */
public class DBOOAuthAccessToken implements MigratableDatabaseObject<DBOOAuthAccessToken, DBOOAuthAccessToken> {
	
	private Long id;
	private String etag;
	private String tokenId;
	private Long principalId;
	private Long clientId;
	private Timestamp createdOn;
	private Timestamp expiresOn;
	private String context;

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_OAUTH_ACCESS_TOKEN_ID, true).withIsBackupId(true),
		new FieldColumn("etag", COL_OAUTH_ACCESS_TOKEN_ETAG).withIsEtag(true),
		new FieldColumn("tokenId", COL_OAUTH_ACCESS_TOKEN_TOKEN_ID),
		new FieldColumn("principalId", COL_OAUTH_ACCESS_TOKEN_PRINCIPAL_ID),
		new FieldColumn("clientId", COL_OAUTH_ACCESS_TOKEN_CLIENT_ID),
		new FieldColumn("createdOn", COL_OAUTH_ACCESS_TOKEN_CREATED_ON),
		new FieldColumn("expiresOn", COL_OAUTH_ACCESS_TOKEN_EXPIRES_ON),
		new FieldColumn("context", COL_OAUTH_ACCESS_TOKEN_CONTEXT)
	};
	
	private static final TableMapping<DBOOAuthAccessToken> TABLE_MAPPER = new TableMapping<DBOOAuthAccessToken>() {
		
		@Override
		public DBOOAuthAccessToken mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOOAuthAccessToken token = new DBOOAuthAccessToken();
			token.setId(rs.getLong(COL_OAUTH_ACCESS_TOKEN_ID));
			token.setEtag(rs.getString(COL_OAUTH_ACCESS_TOKEN_ETAG));
			token.setTokenId(rs.getString(COL_OAUTH_ACCESS_TOKEN_TOKEN_ID));
			token.setPrincipalId(rs.getLong(COL_OAUTH_ACCESS_TOKEN_PRINCIPAL_ID));
			token.setClientId(rs.getLong(COL_OAUTH_ACCESS_TOKEN_CLIENT_ID));
			token.setCreatedOn(rs.getTimestamp(COL_OAUTH_ACCESS_TOKEN_CREATED_ON));
			token.setExpiresOn(rs.getTimestamp(COL_OAUTH_ACCESS_TOKEN_EXPIRES_ON));
			token.setContext(rs.getString(COL_OAUTH_ACCESS_TOKEN_CONTEXT));
			return token;
		}

		@Override
		public String getTableName() {
			return TABLE_OAUTH_ACCESS_TOKEN;
		}

		@Override
		public String getDDLFileName() {
			return DDL_OAUTH_ACCESS_TOKEN;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOOAuthAccessToken> getDBOClass() {
			return DBOOAuthAccessToken.class;
		}
	};

	@Override
	public TableMapping<DBOOAuthAccessToken> getTableMapping() {
		return TABLE_MAPPER;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEtag() {
		return etag;
	}
	
	public void setEtag(String eTag) {
		this.etag = eTag;
	}
	
	public String getTokenId() {
		return tokenId;
	}

	public void setTokenId(String tokenId) {
		this.tokenId = tokenId;
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

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}
	
	public Timestamp getExpiresOn() {
		return expiresOn;
	}
	
	public void setExpiresOn(Timestamp expiresOn) {
		this.expiresOn = expiresOn;
	}
	
	public String getContext() {
		return context;
	}
	
	public void setContext(String context) {
		this.context = context;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.OAUTH_ACCESS_TOKEN;
	}
	
	@Override
	public MigratableTableTranslation<DBOOAuthAccessToken, DBOOAuthAccessToken> getTranslator() {
			return new BasicMigratableTableTranslation<DBOOAuthAccessToken>();
	}


	@Override
	public Class<? extends DBOOAuthAccessToken> getBackupClass() {
		return DBOOAuthAccessToken.class;
	}


	@Override
	public Class<? extends DBOOAuthAccessToken> getDatabaseObjectClass() {
		return DBOOAuthAccessToken.class;
	}


	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(clientId, context, createdOn, etag, expiresOn, id, principalId, tokenId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOOAuthAccessToken)) {
			return false;
		}
		DBOOAuthAccessToken other = (DBOOAuthAccessToken) obj;
		return Objects.equals(clientId, other.clientId) && Objects.equals(context, other.context)
				&& Objects.equals(createdOn, other.createdOn) && Objects.equals(etag, other.etag)
				&& Objects.equals(expiresOn, other.expiresOn) && Objects.equals(id, other.id)
				&& Objects.equals(principalId, other.principalId) && Objects.equals(tokenId, other.tokenId);
	}

	@Override
	public String toString() {
		return "DBOOAuthAccessToken [id=" + id + ", etag=" + etag + ", tokenId=" + tokenId + ", principalId=" + principalId + ", clientId="
				+ clientId + ", createdOn=" + createdOn + ", expiresOn=" + expiresOn + ", context=" + context + "]";
	}

}
