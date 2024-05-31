package org.sagebionetworks.repo.model.dbo.webhook;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_ATTEMPTS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_CODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_EXPIRES_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_MODIFIED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_STATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_FILE_WEBHOOK_VERIFICATION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_WEBHOOK_VERIFICATION;

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

public class DBOWebhookVerification
		implements MigratableDatabaseObject<DBOWebhookVerification, DBOWebhookVerification> {

	private Long verificationId;
	private Long webhookId;
	private String verificationCode;
	private Timestamp expiresOn;
	private Long attempts;
	private String state;
	private String etag;
	private Timestamp createdOn;
	private Timestamp modifiedOn;
	private Long createdBy;
	private Long modifiedBy;

	public static final String FIELD_COLUMN_ID = "verificationId";
	public static final String FIELD_COLUMN_WEBHOOK_ID = "webhookId";
	public static final String FIELD_COLUMN_VERIFICATION_CODE = "verificationCode";
	public static final String FIELD_COLUMN_EXPIRES_ON = "expiresOn";
	public static final String FIELD_COLUMN_ATTEMPTS = "attempts";
	public static final String FIELD_COLUMN_STATE = "state";
	public static final String FIELD_COLUMN_ETAG = "etag";
	public static final String FIELD_COLUMN_CREATED_ON = "createdOn";
	public static final String FIELD_COLUMN_MODIFIED_ON = "modifiedOn";
	public static final String FIELD_COLUMN_CREATED_BY = "createdBy";
	public static final String FIELD_COLUMN_MODIFIED_BY = "modifiedBy";

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn(FIELD_COLUMN_ID, COL_WEBHOOK_VERIFICATION_ID, true).withIsBackupId(true),
			new FieldColumn(FIELD_COLUMN_WEBHOOK_ID, COL_WEBHOOK_ID),
			new FieldColumn(FIELD_COLUMN_VERIFICATION_CODE, COL_WEBHOOK_VERIFICATION_CODE),
			new FieldColumn(FIELD_COLUMN_EXPIRES_ON, COL_WEBHOOK_VERIFICATION_EXPIRES_ON),
			new FieldColumn(FIELD_COLUMN_ATTEMPTS, COL_WEBHOOK_VERIFICATION_ATTEMPTS),
			new FieldColumn(FIELD_COLUMN_STATE, COL_WEBHOOK_VERIFICATION_STATE),
			new FieldColumn(FIELD_COLUMN_ETAG, COL_WEBHOOK_VERIFICATION_ETAG).withIsEtag(true),
			new FieldColumn(FIELD_COLUMN_CREATED_ON, COL_WEBHOOK_VERIFICATION_CREATED_ON),
			new FieldColumn(FIELD_COLUMN_MODIFIED_ON, COL_WEBHOOK_VERIFICATION_MODIFIED_ON),
			new FieldColumn(FIELD_COLUMN_CREATED_BY, COL_WEBHOOK_VERIFICATION_CREATED_BY),
			new FieldColumn(FIELD_COLUMN_MODIFIED_BY, COL_WEBHOOK_VERIFICATION_MODIFIED_BY) };

	@Override
	public TableMapping<DBOWebhookVerification> getTableMapping() {
		return new TableMapping<DBOWebhookVerification>() {
			@Override
			public DBOWebhookVerification mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOWebhookVerification dbo = new DBOWebhookVerification();
				dbo.setVerificationId(rs.getLong(COL_WEBHOOK_VERIFICATION_ID));
				dbo.setWebhookId(rs.getLong(COL_WEBHOOK_ID));
				dbo.setVerificationCode(rs.getString(COL_WEBHOOK_VERIFICATION_CODE));
				dbo.setExpiresOn(rs.getTimestamp(COL_WEBHOOK_VERIFICATION_EXPIRES_ON));
				dbo.setAttempts(rs.getLong(COL_WEBHOOK_VERIFICATION_ATTEMPTS));
				dbo.setState(rs.getString(COL_WEBHOOK_VERIFICATION_STATE));
				dbo.setEtag(rs.getString(COL_WEBHOOK_VERIFICATION_ETAG));
				dbo.setCreatedOn(rs.getTimestamp(COL_WEBHOOK_VERIFICATION_CREATED_ON));
				dbo.setModifiedOn(rs.getTimestamp(COL_WEBHOOK_VERIFICATION_MODIFIED_ON));
				dbo.setCreatedBy(rs.getLong(COL_WEBHOOK_VERIFICATION_CREATED_BY));
				dbo.setModifiedBy(rs.getLong(COL_WEBHOOK_VERIFICATION_MODIFIED_BY));
				return dbo;
			}

			@Override
			public String getTableName() {
				return TABLE_WEBHOOK_VERIFICATION;
			}

			@Override
			public String getDDLFileName() {
				return DDL_FILE_WEBHOOK_VERIFICATION;
			}

			@Override
			public FieldColumn[] getFieldColumns() {
				return FIELDS;
			}

			@Override
			public Class<? extends DBOWebhookVerification> getDBOClass() {
				return DBOWebhookVerification.class;
			}
		};
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.WEBHOOK_VERIFICATION;
	}

	@Override
	public MigratableTableTranslation<DBOWebhookVerification, DBOWebhookVerification> getTranslator() {
		return new BasicMigratableTableTranslation<DBOWebhookVerification>();
	}

	@Override
	public Class<? extends DBOWebhookVerification> getBackupClass() {
		return DBOWebhookVerification.class;
	}

	@Override
	public Class<? extends DBOWebhookVerification> getDatabaseObjectClass() {
		return DBOWebhookVerification.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	public Long getVerificationId() {
		return verificationId;
	}

	public void setVerificationId(Long verificationId) {
		this.verificationId = verificationId;
	}

	public Long getWebhookId() {
		return webhookId;
	}

	public void setWebhookId(Long webhookId) {
		this.webhookId = webhookId;
	}

	public String getVerificationCode() {
		return verificationCode;
	}

	public void setVerificationCode(String verificationCode) {
		this.verificationCode = verificationCode;
	}

	public Timestamp getExpiresOn() {
		return expiresOn;
	}

	public void setExpiresOn(Timestamp expiresOn) {
		this.expiresOn = expiresOn;
	}

	public Long getAttempts() {
		return attempts;
	}

	public void setAttempts(Long attempts) {
		this.attempts = attempts;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
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

	public Long getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(Long createdBy) {
		this.createdBy = createdBy;
	}

	public Long getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(Long modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	@Override
	public int hashCode() {
		return Objects.hash(attempts, createdBy, createdOn, etag, expiresOn, modifiedBy, modifiedOn, state,
				verificationCode, verificationId, webhookId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOWebhookVerification other = (DBOWebhookVerification) obj;
		return Objects.equals(attempts, other.attempts) && Objects.equals(createdBy, other.createdBy)
				&& Objects.equals(createdOn, other.createdOn) && Objects.equals(etag, other.etag)
				&& Objects.equals(expiresOn, other.expiresOn) && Objects.equals(modifiedBy, other.modifiedBy)
				&& Objects.equals(modifiedOn, other.modifiedOn) && state == other.state
				&& Objects.equals(verificationCode, other.verificationCode)
				&& Objects.equals(verificationId, other.verificationId) && Objects.equals(webhookId, other.webhookId);
	}

	@Override
	public String toString() {
		return "DBOWebhookVerification [verificationId=" + verificationId + ", webhookId=" + webhookId
				+ ", verificationCode=" + verificationCode + ", expiresOn=" + expiresOn + ", attempts=" + attempts
				+ ", state=" + state + ", etag=" + etag + ", createdOn=" + createdOn + ", modifiedOn=" + modifiedOn
				+ ", createdBy=" + createdBy + ", modifiedBy=" + modifiedBy + "]";
	}

}
