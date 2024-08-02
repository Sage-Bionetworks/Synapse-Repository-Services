package org.sagebionetworks.repo.model.dbo.webhook;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_CODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_CODE_EXPIRES_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_MSG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_WEBHOOK_VERIFICATION_STATUS;
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

public class DBOWebhookVerification implements MigratableDatabaseObject<DBOWebhookVerification, DBOWebhookVerification> {

	private Long webhookId;
	private String etag;
	private Timestamp modifiedOn;
	private String code;
	private Timestamp codeExpiresOn;
	private String status;
	private String message;

	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn("webhookId", COL_WEBHOOK_VERIFICATION_ID, true).withIsBackupId(true),
		new FieldColumn("etag", COL_WEBHOOK_VERIFICATION_ETAG).withIsEtag(true),
		new FieldColumn("modifiedOn", COL_WEBHOOK_VERIFICATION_MODIFIED_ON),
		new FieldColumn("code", COL_WEBHOOK_VERIFICATION_CODE),
		new FieldColumn("codeExpiresOn", COL_WEBHOOK_VERIFICATION_CODE_EXPIRES_ON),
		new FieldColumn("status", COL_WEBHOOK_VERIFICATION_STATUS),
		new FieldColumn("message", COL_WEBHOOK_VERIFICATION_MSG)
	};

	@Override
	public TableMapping<DBOWebhookVerification> getTableMapping() {
		return new TableMapping<DBOWebhookVerification>() {
			@Override
			public DBOWebhookVerification mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOWebhookVerification dbo = new DBOWebhookVerification();
				dbo.setWebhookId(rs.getLong(COL_WEBHOOK_VERIFICATION_ID));
				dbo.setEtag(rs.getString(COL_WEBHOOK_VERIFICATION_ETAG));
				dbo.setModifiedOn(rs.getTimestamp(COL_WEBHOOK_VERIFICATION_MODIFIED_ON));
				dbo.setCode(rs.getString(COL_WEBHOOK_VERIFICATION_CODE));
				dbo.setCodeExpiresOn(rs.getTimestamp(COL_WEBHOOK_VERIFICATION_CODE_EXPIRES_ON));
				dbo.setStatus(rs.getString(COL_WEBHOOK_VERIFICATION_STATUS));
				
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

	public Long getWebhookId() {
		return webhookId;
	}

	public DBOWebhookVerification setWebhookId(Long webhookId) {
		this.webhookId = webhookId;
		return this;
	}

	public String getEtag() {
		return etag;
	}

	public DBOWebhookVerification setEtag(String etag) {
		this.etag = etag;
		return this;
	}

	public Timestamp getModifiedOn() {
		return modifiedOn;
	}

	public DBOWebhookVerification setModifiedOn(Timestamp modifiedOn) {
		this.modifiedOn = modifiedOn;
		return this;
	}

	public String getCode() {
		return code;
	}

	public DBOWebhookVerification setCode(String code) {
		this.code = code;
		return this;
	}

	public Timestamp getCodeExpiresOn() {
		return codeExpiresOn;
	}

	public DBOWebhookVerification setCodeExpiresOn(Timestamp codeExpiresOn) {
		this.codeExpiresOn = codeExpiresOn;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public DBOWebhookVerification setStatus(String status) {
		this.status = status;
		return this;
	}
	
	public String getMessage() {
		return message;
	}
	
	public DBOWebhookVerification setMessage(String message) {
		this.message = message;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(code, codeExpiresOn, etag, modifiedOn, status, webhookId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOWebhookVerification)) {
			return false;
		}
		DBOWebhookVerification other = (DBOWebhookVerification) obj;
		return Objects.equals(code, other.code) && Objects.equals(codeExpiresOn, other.codeExpiresOn)
				&& Objects.equals(etag, other.etag)
				&& Objects.equals(modifiedOn, other.modifiedOn) && Objects.equals(status, other.status)
				&& Objects.equals(webhookId, other.webhookId);
	}

	@Override
	public String toString() {
		return "DBOWebhookVerification [webhookId=" + webhookId + ", etag=" + etag + ", modifiedOn="
				+ modifiedOn + ", code=" + code + ", codeExpiresOn=" + codeExpiresOn + ", status=" + status + "]";
	}

}
