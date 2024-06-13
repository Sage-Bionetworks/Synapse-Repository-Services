package org.sagebionetworks.repo.model.dbo.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.util.TemporaryCode;

public class DBOCredential implements MigratableDatabaseObject<DBOCredential, DBOCredential> {
	
	public static final int MIN_PASSWORD_CHANGE_SECONDS = 24 * 60 * 60;
	public static final int MAX_PASSWORD_VALIDITY_DAYS = 120;
	
	private Long principalId;
	private String etag;
	private Date modifiedOn;
	private Date expiresOn;
	private String passHash;
	private String secretKey;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("principalId", SqlConstants.COL_CREDENTIAL_PRINCIPAL_ID, true).withIsBackupId(true),
		new FieldColumn("etag", SqlConstants.COL_CREDENTIAL_ETAG).withIsEtag(true),
		new FieldColumn("modifiedOn", SqlConstants.COL_CREDENTIAL_MODIFIED_ON),
		new FieldColumn("expiresOn", SqlConstants.COL_CREDENTIAL_EXPIRES_ON),
		new FieldColumn("passHash", SqlConstants.COL_CREDENTIAL_PASS_HASH), 
		new FieldColumn("secretKey", SqlConstants.COL_CREDENTIAL_SECRET_KEY)
	};

	private static final MigratableTableTranslation<DBOCredential, DBOCredential> MIGRATION_MAPPER = new BasicMigratableTableTranslation<>() {

		@Override
		@TemporaryCode(author = "marco.marasca", comment = "Can be removed once in production")
		public DBOCredential createDatabaseObjectFromBackup(DBOCredential backup) {
			if (backup.getEtag() == null) {
				backup.setEtag(UUID.randomUUID().toString());
			}
			if (backup.getExpiresOn() == null) {
				backup.setExpiresOn(Date.from(Instant.now().plus(MAX_PASSWORD_VALIDITY_DAYS, ChronoUnit.DAYS)));
			}
			return backup;
		}
		
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
				cred.setEtag(rs.getString(SqlConstants.COL_CREDENTIAL_ETAG));
				
				Timestamp modifiedOn = rs.getTimestamp(SqlConstants.COL_CREDENTIAL_MODIFIED_ON);				
				cred.setModifiedOn(modifiedOn == null ? null : new Date(modifiedOn.getTime()));
				
				Timestamp expiresOn = rs.getTimestamp(SqlConstants.COL_CREDENTIAL_EXPIRES_ON);
				cred.setExpiresOn(expiresOn == null ? null : new Date(expiresOn.getTime()));
				
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
	
	public String getEtag() {
		return etag;
	}
	
	public void setEtag(String etag) {
		this.etag = etag;
	}
	
	public Date getModifiedOn() {
		return modifiedOn;
	}
	
	public void setModifiedOn(Date modifiedOn) {
		this.modifiedOn = modifiedOn;
	}
	
	public Date getExpiresOn() {
		return expiresOn;
	}
	
	public void setExpiresOn(Date expiresOn) {
		this.expiresOn = expiresOn;
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
	public MigratableTableTranslation<DBOCredential, DBOCredential> getTranslator() { return MIGRATION_MAPPER; }

	@Override
	public Class<? extends DBOCredential> getBackupClass() {
		return DBOCredential.class;
	}

	@Override
	public Class<? extends DBOCredential> getDatabaseObjectClass() {
		return DBOCredential.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(etag, expiresOn, modifiedOn, passHash, principalId, secretKey);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOCredential)) {
			return false;
		}
		DBOCredential other = (DBOCredential) obj;
		return Objects.equals(etag, other.etag) && Objects.equals(expiresOn, other.expiresOn)
				&& Objects.equals(modifiedOn, other.modifiedOn) && Objects.equals(passHash, other.passHash)
				&& Objects.equals(principalId, other.principalId) && Objects.equals(secretKey, other.secretKey);
	}

}
