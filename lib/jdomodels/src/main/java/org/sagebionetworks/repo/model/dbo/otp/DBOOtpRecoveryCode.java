package org.sagebionetworks.repo.model.dbo.otp;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OTP_RECOVERY_CODE_CODE_HASH;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OTP_RECOVERY_CODE_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OTP_RECOVERY_CODE_SECRET_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_OTP_RECOVERY_CODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_OTP_RECOVERY_CODE;

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

public class DBOOtpRecoveryCode implements MigratableDatabaseObject<DBOOtpRecoveryCode, DBOOtpRecoveryCode> {
	
	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("secretId", COL_OTP_RECOVERY_CODE_SECRET_ID, true).withIsBackupId(true),
		new FieldColumn("codeHash", COL_OTP_RECOVERY_CODE_CODE_HASH, true),
		new FieldColumn("createdOn", COL_OTP_RECOVERY_CODE_CREATED_ON)
	};

	private static final TableMapping<DBOOtpRecoveryCode> TABLE_MAPPING = new TableMapping<DBOOtpRecoveryCode>() {

		@Override
		public DBOOtpRecoveryCode mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOOtpRecoveryCode recoveryCode = new DBOOtpRecoveryCode();
			
			recoveryCode.setSecretId(rs.getLong(COL_OTP_RECOVERY_CODE_SECRET_ID));
			recoveryCode.setCodeHash(rs.getString(COL_OTP_RECOVERY_CODE_CODE_HASH));
			recoveryCode.setCreatedOn(rs.getTimestamp(COL_OTP_RECOVERY_CODE_CREATED_ON));
			
			return recoveryCode;
		}

		@Override
		public String getTableName() {
			return TABLE_OTP_RECOVERY_CODE;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public String getDDLFileName() {
			return DDL_OTP_RECOVERY_CODE;
		}

		@Override
		public Class<? extends DBOOtpRecoveryCode> getDBOClass() {
			return DBOOtpRecoveryCode.class;
		}
	};

	private static final MigratableTableTranslation<DBOOtpRecoveryCode, DBOOtpRecoveryCode> TRANSLATOR = new BasicMigratableTableTranslation<>();

	private Long secretId;
	private String codeHash;
	private Timestamp createdOn;
	
	public Long getSecretId() {
		return secretId;
	}

	public void setSecretId(Long secretId) {
		this.secretId = secretId;
	}

	public String getCodeHash() {
		return codeHash;
	}

	public void setCodeHash(String codeHash) {
		this.codeHash = codeHash;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}

	@Override
	public TableMapping<DBOOtpRecoveryCode> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.OTP_RECOVERY_CODE;
	}

	@Override
	public MigratableTableTranslation<DBOOtpRecoveryCode, DBOOtpRecoveryCode> getTranslator() {
		return TRANSLATOR;
	}

	@Override
	public Class<? extends DBOOtpRecoveryCode> getBackupClass() {
		return DBOOtpRecoveryCode.class;
	}

	@Override
	public Class<? extends DBOOtpRecoveryCode> getDatabaseObjectClass() {
		return DBOOtpRecoveryCode.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(codeHash, createdOn, secretId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOOtpRecoveryCode)) {
			return false;
		}
		DBOOtpRecoveryCode other = (DBOOtpRecoveryCode) obj;
		return Objects.equals(codeHash, other.codeHash) && Objects.equals(createdOn, other.createdOn)
				&& Objects.equals(secretId, other.secretId);
	}

	@Override
	public String toString() {
		return "DBOOtpRecoveryCode [secretId=" + secretId + ", codeHash=" + codeHash + ", createdOn=" + createdOn + "]";
	}

}
