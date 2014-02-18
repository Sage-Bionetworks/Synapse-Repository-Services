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

public class DBOCredential implements MigratableDatabaseObject<DBOCredential, DBOCredential> {
	private Long principalId;
	private String passHash;
	private String secretKey;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("principalId", SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID, true).withIsBackupId(true),
		new FieldColumn("passHash", SqlConstants.COL_CREDENTIAL_PASS_HASH), 
		new FieldColumn("secretKey", SqlConstants.COL_CREDENTIAL_SECRET_KEY), 
	};

	@Override
	public TableMapping<DBOCredential> getTableMapping() {
		return new TableMapping<DBOCredential>() {
			// Map a result set to this object
			@Override
			public DBOCredential mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOCredential cred = new DBOCredential();
				cred.setPrincipalId(rs.getLong(SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID));
				cred.setPassHash(rs.getString(SqlConstants.COL_CREDENTIAL_PASS_HASH));
				cred.setSecretKey(rs.getString(SqlConstants.COL_CREDENTIAL_SECRET_KEY));
				return cred;
			}

			@Override
			public String getTableName() {
				return SqlConstants.TABLE_CREDENTIAL;
			}

			@Override
			public String getDDLFileName() {
				return SqlConstants.DDL_CREDENTIAL;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOCredential> getDBOClass() {
				return DBOCredential.class;
			}
		};
	}

	public void setPrincipalId(Long principalId) {
		this.principalId = principalId;
	}
	public Long getPrincipalId() {
		return principalId;
	}
	public void setPassHash(String passHash) {
		this.passHash = passHash;
	}
	public String getPassHash() {
		return passHash;
	}
	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}
	public String getSecretKey() {
		return secretKey;
	}
	
	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.CREDENTIAL;
	}


	@Override
	public MigratableTableTranslation<DBOCredential, DBOCredential> getTranslator() {
		// We do not currently have a backup for this object.
		return new MigratableTableTranslation<DBOCredential, DBOCredential>(){

			@Override
			public DBOCredential createDatabaseObjectFromBackup(
					DBOCredential backup) {
				return backup;
			}

			@Override
			public DBOCredential createBackupFromDatabaseObject(DBOCredential dbo) {
				return dbo;
			}};
	}


	@Override
	public Class<? extends DBOCredential> getBackupClass() {
		return DBOCredential.class;
	}


	@Override
	public Class<? extends DBOCredential> getDatabaseObjectClass() {
		return DBOCredential.class;
	}


	@SuppressWarnings("rawtypes")
	@Override
	public List<MigratableDatabaseObject> getSecondaryTypes() {
		return null;
	}



}
