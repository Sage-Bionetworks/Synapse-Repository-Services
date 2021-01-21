package org.sagebionetworks.repo.model.dbo.persistence;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SESSION_TOKEN_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SESSION_TOKEN_SESSION_TOKEN;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SESSION_TOKEN_VALIDATED_ON;
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
			new FieldColumn("principalId", COL_SESSION_TOKEN_PRINCIPAL_ID, true).withIsBackupId(true),
			new FieldColumn("validatedOn", COL_SESSION_TOKEN_VALIDATED_ON),
			new FieldColumn("sessionToken", COL_SESSION_TOKEN_SESSION_TOKEN) };

	private Long principalId;
	private Long validatedOn;
	private String sessionToken;
	
	public static final TableMapping<DBOSessionToken> MAPPING = new TableMapping<DBOSessionToken>() {
		@Override
		public DBOSessionToken mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOSessionToken row = new DBOSessionToken();
			row.setPrincipalId(rs.getLong(COL_SESSION_TOKEN_PRINCIPAL_ID));
			row.setValidatedOn(rs.getLong(COL_SESSION_TOKEN_VALIDATED_ON));
			row.setSessionToken(rs.getString(COL_SESSION_TOKEN_SESSION_TOKEN));
			return row;
		}

		@Override
		public String getTableName() {
			return TABLE_SESSION_TOKEN;
		}

		@Override
		public String getDDLFileName() {
			return DDL_SESSION_TOKEN;
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

	@Override
	public TableMapping<DBOSessionToken> getTableMapping() {
		return MAPPING;
	}

	public Long getPrincipalId() {
		return principalId;
	}

	public void setPrincipalId(Long principalId) {
		this.principalId = principalId;
	}

	public Long getValidatedOn() {
		return validatedOn;
	}

	public void setValidatedOn(Long validatedOn) {
		this.validatedOn = validatedOn;
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
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(principalId, sessionToken, validatedOn);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOSessionToken)) {
			return false;
		}
		DBOSessionToken other = (DBOSessionToken) obj;
		return Objects.equals(principalId, other.principalId) && Objects.equals(sessionToken, other.sessionToken)
				&& Objects.equals(validatedOn, other.validatedOn);
	}

	@Override
	public String toString() {
		return "DBOSessionToken [principalId=" + principalId + ", validatedOn=" + validatedOn + ", sessionToken="
				+ sessionToken + "]";
	}

}
