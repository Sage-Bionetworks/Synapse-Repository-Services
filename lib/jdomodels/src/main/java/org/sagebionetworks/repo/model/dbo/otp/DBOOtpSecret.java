package org.sagebionetworks.repo.model.dbo.otp;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OTP_SECRET_ACTIVE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OTP_SECRET_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OTP_SECRET_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OTP_SECRET_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OTP_SECRET_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_OTP_SECRET_SECRET;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_OTP_SECRET;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_OTP_SECRET;

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

public class DBOOtpSecret implements MigratableDatabaseObject<DBOOtpSecret, DBOOtpSecret> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("id", COL_OTP_SECRET_ID, true).withIsBackupId(true),
		new FieldColumn("etag", COL_OTP_SECRET_ETAG).withIsEtag(true),
		new FieldColumn("userId", COL_OTP_SECRET_PRINCIPAL_ID),
		new FieldColumn("createdOn", COL_OTP_SECRET_CREATED_ON),
		new FieldColumn("secret", COL_OTP_SECRET_SECRET),
		new FieldColumn("active", COL_OTP_SECRET_ACTIVE)
	};

	private static final TableMapping<DBOOtpSecret> TABLE_MAPPING = new TableMapping<DBOOtpSecret>() {

		@Override
		public DBOOtpSecret mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOOtpSecret secret = new DBOOtpSecret();

			secret.setId(rs.getLong(COL_OTP_SECRET_ID));
			secret.setEtag(rs.getString(COL_OTP_SECRET_ETAG));
			secret.setUserId(rs.getLong(COL_OTP_SECRET_PRINCIPAL_ID));
			secret.setCreatedOn(rs.getTimestamp(COL_OTP_SECRET_CREATED_ON));
			secret.setSecret(rs.getString(COL_OTP_SECRET_SECRET));
			secret.setActive(rs.getBoolean(COL_OTP_SECRET_ACTIVE));

			return secret;
		}

		@Override
		public String getTableName() {
			return TABLE_OTP_SECRET;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public String getDDLFileName() {
			return DDL_OTP_SECRET;
		}

		@Override
		public Class<? extends DBOOtpSecret> getDBOClass() {
			return DBOOtpSecret.class;
		}
	};

	private static final MigratableTableTranslation<DBOOtpSecret, DBOOtpSecret> TRANSLATOR = new BasicMigratableTableTranslation<>();

	private Long id;
	private String etag;
	private Long userId;
	private Timestamp createdOn;
	private String secret;
	private Boolean active;

	public DBOOtpSecret() {}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	public String getEtag() {
		return etag;
	}
	
	public void setEtag(String etag) {
		this.etag = etag;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public Boolean getActive() {
		return active;
	}
	
	public void setActive(Boolean active) {
		this.active = active;
	}

	@Override
	public TableMapping<DBOOtpSecret> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.OTP_SECRET;
	}

	@Override
	public MigratableTableTranslation<DBOOtpSecret, DBOOtpSecret> getTranslator() {
		return TRANSLATOR;
	}

	@Override
	public Class<? extends DBOOtpSecret> getBackupClass() {
		return DBOOtpSecret.class;
	}

	@Override
	public Class<? extends DBOOtpSecret> getDatabaseObjectClass() {
		return DBOOtpSecret.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdOn, active, etag, id, secret, userId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOOtpSecret)) {
			return false;
		}
		DBOOtpSecret other = (DBOOtpSecret) obj;
		return Objects.equals(createdOn, other.createdOn) && Objects.equals(active, other.active) && Objects.equals(etag, other.etag)
				&& Objects.equals(id, other.id) && Objects.equals(secret, other.secret) && Objects.equals(userId, other.userId);
	}

	@Override
	public String toString() {
		return "DBOTotpSecret [id=" + id + ", etag=" + etag + ", userId=" + userId + ", createdOn=" + createdOn + ", secret=" + secret
				+ ", active=" + active + "]";
	}

}
