package org.sagebionetworks.repo.model.dbo.ses;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_EMAIL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_EXPIRES_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_REASON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_SES_MESSAGE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_QUARANTINED_EMAILS_UPDATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_QUARANTINED_EMAILS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_QUARANTINED_EMAILS;

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

/**
 * DBO object used to store the email addresses that are in quarantine and should not be used as recipients
 * 
 * @author Marco
 *
 */
public class DBOQuarantinedEmail implements MigratableDatabaseObject<DBOQuarantinedEmail, DBOQuarantinedEmail> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("email", COL_QUARANTINED_EMAILS_EMAIL, true).withIsBackupId(true),
			new FieldColumn("etag", COL_QUARANTINED_EMAILS_ETAG).withIsEtag(true),
			new FieldColumn("createdOn", COL_QUARANTINED_EMAILS_CREATED_ON),
			new FieldColumn("updatedOn", COL_QUARANTINED_EMAILS_UPDATED_ON),
			new FieldColumn("expiresOn", COL_QUARANTINED_EMAILS_EXPIRES_ON),
			new FieldColumn("reason", COL_QUARANTINED_EMAILS_REASON),
			new FieldColumn("sesMessageId", COL_QUARANTINED_EMAILS_SES_MESSAGE_ID) };

	private static final TableMapping<DBOQuarantinedEmail> TABLE_MAPPING = new TableMapping<DBOQuarantinedEmail>() {

		@Override
		public DBOQuarantinedEmail mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOQuarantinedEmail dbo = new DBOQuarantinedEmail();

			dbo.setEmail(rs.getString(COL_QUARANTINED_EMAILS_EMAIL));
			dbo.setEtag(rs.getString(COL_QUARANTINED_EMAILS_ETAG));
			dbo.setCreatedOn(rs.getTimestamp(COL_QUARANTINED_EMAILS_CREATED_ON));
			dbo.setUpdatedOn(rs.getTimestamp(COL_QUARANTINED_EMAILS_UPDATED_ON));
			dbo.setExpiresOn(rs.getTimestamp(COL_QUARANTINED_EMAILS_EXPIRES_ON));
			dbo.setReason(rs.getString(COL_QUARANTINED_EMAILS_REASON));
			dbo.setSesMessageId(rs.getString(COL_QUARANTINED_EMAILS_SES_MESSAGE_ID));

			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_QUARANTINED_EMAILS;
		}

		@Override
		public String getDDLFileName() {
			return DDL_QUARANTINED_EMAILS;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOQuarantinedEmail> getDBOClass() {
			return DBOQuarantinedEmail.class;
		}

	};

	private static final MigratableTableTranslation<DBOQuarantinedEmail, DBOQuarantinedEmail> TABLE_TRANSLATOR = new BasicMigratableTableTranslation<>();

	private String email;
	private String etag;
	private Timestamp createdOn;
	private Timestamp updatedOn;
	private Timestamp expiresOn;
	private String reason;
	// This is the optional SES generated id that lead to the quarantine
	private String sesMessageId;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
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

	public Timestamp getUpdatedOn() {
		return updatedOn;
	}

	public void setUpdatedOn(Timestamp updatedOn) {
		this.updatedOn = updatedOn;
	}

	public Timestamp getExpiresOn() {
		return expiresOn;
	}

	public void setExpiresOn(Timestamp expiresOn) {
		this.expiresOn = expiresOn;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getSesMessageId() {
		return sesMessageId;
	}

	public void setSesMessageId(String sesMessageId) {
		this.sesMessageId = sesMessageId;
	}

	@Override
	public TableMapping<DBOQuarantinedEmail> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.QUARANTINED_EMAILS;
	}

	@Override
	public MigratableTableTranslation<DBOQuarantinedEmail, DBOQuarantinedEmail> getTranslator() {
		return TABLE_TRANSLATOR;
	}

	@Override
	public Class<? extends DBOQuarantinedEmail> getBackupClass() {
		return DBOQuarantinedEmail.class;
	}

	@Override
	public Class<? extends DBOQuarantinedEmail> getDatabaseObjectClass() {
		return DBOQuarantinedEmail.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(createdOn, email, etag, expiresOn, reason, sesMessageId, updatedOn);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOQuarantinedEmail other = (DBOQuarantinedEmail) obj;
		return Objects.equals(createdOn, other.createdOn) && Objects.equals(email, other.email) && Objects.equals(etag, other.etag)
				&& Objects.equals(expiresOn, other.expiresOn) && Objects.equals(reason, other.reason)
				&& Objects.equals(sesMessageId, other.sesMessageId) && Objects.equals(updatedOn, other.updatedOn);
	}

	@Override
	public String toString() {
		return "DBOQuarantinedEmail [email=" + email + ", etag=" + etag + ", createdOn=" + createdOn + ", updatedOn=" + updatedOn
				+ ", expiresOn=" + expiresOn + ", reason=" + reason + ", sesMessageId=" + sesMessageId + "]";
	}

}
