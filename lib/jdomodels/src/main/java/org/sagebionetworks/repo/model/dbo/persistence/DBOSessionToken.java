package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SESSION_TOKEN_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SESSION_TOKEN_SESSION_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_SESSION_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SESSION_TOKEN;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

public class DBOSessionToken implements MigratableDatabaseObject<DBOSessionToken, DBOSessionToken> {
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("principalId", COL_SESSION_TOKEN_PRINCIPAL_ID).withIsPrimaryKey(true).withIsBackupId(true),
			new FieldColumn("sessionToken", COL_SESSION_TOKEN_SESSION_TOKEN)
	};
	
	private Long principalId;
	private String sessionToken;

	@Override
	public TableMapping<DBOSessionToken> getTableMapping() {
		return new TableMapping<DBOSessionToken>() {
			
			@Override
			public DBOSessionToken mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOSessionToken dbo = new DBOSessionToken();
				dbo.setPrincipalId(rs.getLong(COL_SESSION_TOKEN_PRINCIPAL_ID));
				dbo.setSessionToken(rs.getString(COL_SESSION_TOKEN_SESSION_TOKEN));
				return dbo;
			}
			
			@Override
			public String getTableName() {
				return TABLE_SESSION_TOKEN;
			}
			
			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}
			
			@Override
			public String getDDLFileName() {
				return DDL_SESSION_TOKEN;
			}
			
			@Override
			public Class<? extends DBOSessionToken> getDBOClass() {
				return DBOSessionToken.class;
			}
		};
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
		return Objects.hash(principalId, sessionToken);
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
		return Objects.equals(principalId, other.principalId) && Objects.equals(sessionToken, other.sessionToken);
	}

	@Override
	public String toString() {
		return "DBOSessionToken [principalId=" + principalId + ", sessionToken=" + sessionToken + "]";
	}

}
