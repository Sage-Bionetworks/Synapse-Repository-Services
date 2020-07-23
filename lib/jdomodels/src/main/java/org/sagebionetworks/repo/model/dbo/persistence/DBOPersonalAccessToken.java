package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PERSONAL_ACCESS_TOKEN_CLAIMS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PERSONAL_ACCESS_TOKEN_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PERSONAL_ACCESS_TOKEN_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PERSONAL_ACCESS_TOKEN_LAST_USED;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PERSONAL_ACCESS_TOKEN_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PERSONAL_ACCESS_TOKEN_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PERSONAL_ACCESS_TOKEN_SCOPES;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_PERSONAL_ACCESS_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PERSONAL_ACCESS_TOKEN;

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
public class DBOPersonalAccessToken implements MigratableDatabaseObject<DBOPersonalAccessToken, DBOPersonalAccessToken> {
	private Long id;
	private String name;
	private Long principalId;
	private byte[] scopes;
	private byte[] claims;
	private Timestamp createdOn;
	private Timestamp lastUsed;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_PERSONAL_ACCESS_TOKEN_ID, true).withIsBackupId(true),
			new FieldColumn("name", COL_PERSONAL_ACCESS_TOKEN_NAME),
			new FieldColumn("principalId", COL_PERSONAL_ACCESS_TOKEN_PRINCIPAL_ID),
			new FieldColumn("scopes", COL_PERSONAL_ACCESS_TOKEN_SCOPES),
			new FieldColumn("claims", COL_PERSONAL_ACCESS_TOKEN_CLAIMS),
			new FieldColumn("createdOn", COL_PERSONAL_ACCESS_TOKEN_CREATED_ON),
			new FieldColumn("lastUsed", COL_PERSONAL_ACCESS_TOKEN_LAST_USED).withIsEtag(true)
		};

	private static DBOPersonalAccessToken mapRow(ResultSet rs, int rowNum) throws SQLException {
		DBOPersonalAccessToken token = new DBOPersonalAccessToken();
		token.setId(rs.getLong(COL_PERSONAL_ACCESS_TOKEN_ID));
		token.setName(rs.getString(COL_PERSONAL_ACCESS_TOKEN_NAME));
		token.setPrincipalId(rs.getLong(COL_PERSONAL_ACCESS_TOKEN_PRINCIPAL_ID));
		token.setScopes(rs.getBytes(COL_PERSONAL_ACCESS_TOKEN_SCOPES));
		token.setClaims(rs.getBytes(COL_PERSONAL_ACCESS_TOKEN_CLAIMS));
		token.setCreatedOn(rs.getTimestamp(COL_PERSONAL_ACCESS_TOKEN_CREATED_ON));
		token.setLastUsed(rs.getTimestamp(COL_PERSONAL_ACCESS_TOKEN_LAST_USED));
		return token;
	}

	@Override
	public TableMapping<DBOPersonalAccessToken> getTableMapping() {
		return new TableMapping<DBOPersonalAccessToken>() {
			// Map a result set to this object
			@Override
			public DBOPersonalAccessToken mapRow(ResultSet rs, int rowNum) throws SQLException {
				// Use static method to avoid unintentionally referencing `this`
				return DBOPersonalAccessToken.mapRow(rs, rowNum);
			}

			@Override
			public String getTableName() {
				return TABLE_PERSONAL_ACCESS_TOKEN;
			}

			@Override
			public String getDDLFileName() {
				return DDL_PERSONAL_ACCESS_TOKEN;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOPersonalAccessToken> getDBOClass() {
				return DBOPersonalAccessToken.class;
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

	public Long getPrincipalId() {
		return principalId;
	}

	public void setPrincipalId(Long principalId) {
		this.principalId = principalId;
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

	@Override
	public String toString() {
		return "DBOOAuthClient [id=" + id + ", name=" + name + ", principalId=" + principalId +
				", scopes=" + Arrays.toString(scopes) + ", claims=" + Arrays.toString(claims) +
				", createdOn=" + createdOn + ", lastUsed=" + lastUsed + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((principalId == null) ? 0 : principalId.hashCode());
		result = prime * result + ((scopes == null) ? 0 : Arrays.hashCode(scopes));
		result = prime * result + ((scopes == null) ? 0 : Arrays.hashCode(claims));
		result = prime * result + ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result + ((lastUsed == null) ? 0 : lastUsed.hashCode());
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
		DBOPersonalAccessToken other = (DBOPersonalAccessToken) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
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
		if (lastUsed == null) {
			if (other.lastUsed != null) {
				return false;
			}
		} else if (!lastUsed.equals(other.lastUsed)) {
			return false;
		}
		return true;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.PERSONAL_ACCESS_TOKEN;
	}
	
	@Override
	public MigratableTableTranslation<DBOPersonalAccessToken, DBOPersonalAccessToken> getTranslator() {
			return new BasicMigratableTableTranslation<DBOPersonalAccessToken>();
	}


	@Override
	public Class<? extends DBOPersonalAccessToken> getBackupClass() {
		return DBOPersonalAccessToken.class;
	}


	@Override
	public Class<? extends DBOPersonalAccessToken> getDatabaseObjectClass() {
		return DBOPersonalAccessToken.class;
	}


	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}



}
