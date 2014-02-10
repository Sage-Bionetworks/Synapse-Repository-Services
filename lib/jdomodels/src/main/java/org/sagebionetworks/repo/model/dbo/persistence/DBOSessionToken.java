package org.sagebionetworks.repo.model.dbo.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

public class DBOSessionToken implements MigratableDatabaseObject<DBOSessionToken, DBOSessionToken> {
	private Long principalId;
	private Date validatedOn;
	private String domain;
	private String sessionToken;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("principalId", SqlConstants.COL_SESSION_TOKEN_PRINCIPAL_ID, true).withIsBackupId(true),
		new FieldColumn("validatedOn", SqlConstants.COL_SESSION_TOKEN_VALIDATED_ON),
		new FieldColumn("domain", SqlConstants.COL_SESSION_TOKEN_DOMAIN),
		new FieldColumn("sessionToken", SqlConstants.COL_SESSION_TOKEN_SESSION_TOKEN)
	};
	
	@Override
	public TableMapping<DBOSessionToken> getTableMapping() {
		return new TableMapping<DBOSessionToken>() {
			@Override
			public DBOSessionToken mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOSessionToken row = new DBOSessionToken();
				row.setPrincipalId(rs.getLong(SqlConstants.COL_SESSION_TOKEN_PRINCIPAL_ID));
				Timestamp ts = rs.getTimestamp(SqlConstants.COL_SESSION_TOKEN_VALIDATED_ON);
				row.setValidatedOn(ts==null ? null : new Date(ts.getTime()));
				row.setDomain(rs.getString(SqlConstants.COL_SESSION_TOKEN_DOMAIN));
				row.setSessionToken(rs.getString(SqlConstants.COL_SESSION_TOKEN_SESSION_TOKEN));
				return row;
			}

			@Override
			public String getTableName() {
				return SqlConstants.TABLE_SESSION_TOKEN;
			}

			@Override
			public String getDDLFileName() {
				return SqlConstants.DDL_SESSION_TOKEN;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
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
	public Date getValidatedOn() {
		return validatedOn;
	}
	public void setValidatedOn(Date validatedOn) {
		this.validatedOn = validatedOn;
	}
	public String getDomain() {
		return domain;
	}
	public void setDomain(String domain) {
		this.domain = domain;
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
		// We do not currently have a backup for this object.
		return new MigratableTableTranslation<DBOSessionToken, DBOSessionToken>(){
			@Override
			public DBOSessionToken createDatabaseObjectFromBackup(DBOSessionToken backup) {
				return backup;
			}

			@Override
			public DBOSessionToken createBackupFromDatabaseObject(DBOSessionToken dbo) {
				return dbo;
			}
		};	
	}

	@Override
	public Class<? extends DBOSessionToken> getBackupClass() {
		return DBOSessionToken.class;
	}

	@Override
	public Class<? extends DBOSessionToken> getDatabaseObjectClass() {
		return DBOSessionToken.class;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}

}
