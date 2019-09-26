package org.sagebionetworks.repo.model.dbo.ses;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SES_NOTIFICATIONS_BODY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SES_NOTIFICATIONS_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SES_NOTIFICATIONS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SES_NOTIFICATIONS_ISP_TIMESTAMP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SES_NOTIFICATIONS_SES_EMAIL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SES_NOTIFICATIONS_SES_FEEDBACK_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_SES_NOTIFICATIONS_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.DDL_SES_NOTIFICATIONS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_SES_NOTIFICATIONS;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;

/**
 * DBO object used to track bounce and complaints notifications sent by SES
 * 
 * @author Marco
 */
public class DBOSESNotification implements MigratableDatabaseObject<DBOSESNotification, DBOSESNotification> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", COL_SES_NOTIFICATIONS_ID, true),
			new FieldColumn("createdOn", COL_SES_NOTIFICATIONS_CREATED_ON, true),
			new FieldColumn("sesEmailId", COL_SES_NOTIFICATIONS_SES_EMAIL_ID),
			new FieldColumn("sesFeedbackId", COL_SES_NOTIFICATIONS_SES_FEEDBACK_ID),
			new FieldColumn("ispTimestamp", COL_SES_NOTIFICATIONS_ISP_TIMESTAMP),
			new FieldColumn("notificationType", COL_SES_NOTIFICATIONS_TYPE),
			new FieldColumn("notificationBody", COL_SES_NOTIFICATIONS_BODY) };

	private static final TableMapping<DBOSESNotification> TABLE_MAPPING = new TableMapping<DBOSESNotification>() {

		@Override
		public DBOSESNotification mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOSESNotification dbo = new DBOSESNotification();
			
			dbo.setId(rs.getLong(COL_SES_NOTIFICATIONS_ID));
			dbo.setCreatedOn(rs.getTimestamp(COL_SES_NOTIFICATIONS_CREATED_ON));
			dbo.setSesEmailId(rs.getString(COL_SES_NOTIFICATIONS_SES_EMAIL_ID));
			dbo.setSesFeedbackId(rs.getString(COL_SES_NOTIFICATIONS_SES_FEEDBACK_ID));
			dbo.setNotificationType(rs.getString(COL_SES_NOTIFICATIONS_TYPE));
			dbo.setNotificationBody(rs.getBytes(COL_SES_NOTIFICATIONS_BODY));

			return dbo;
		}

		@Override
		public String getTableName() {
			return TABLE_SES_NOTIFICATIONS;
		}

		@Override
		public String getDDLFileName() {
			return DDL_SES_NOTIFICATIONS;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOSESNotification> getDBOClass() {
			return DBOSESNotification.class;
		}
	};

	private Long id;
	private Timestamp createdOn;
	private String sesEmailId;
	private String sesFeedbackId;
	private Timestamp ispTimestamp;
	private String notificationType;
	private byte[] notificationBody;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Timestamp getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Timestamp createdOn) {
		this.createdOn = createdOn;
	}

	public String getSesEmailId() {
		return sesEmailId;
	}

	public void setSesEmailId(String sesEmailId) {
		this.sesEmailId = sesEmailId;
	}

	public String getSesFeedbackId() {
		return sesFeedbackId;
	}

	public void setSesFeedbackId(String sesFeedbackId) {
		this.sesFeedbackId = sesFeedbackId;
	}

	public Timestamp getIspTimestamp() {
		return ispTimestamp;
	}

	public void setIspTimestamp(Timestamp ispTimestamp) {
		this.ispTimestamp = ispTimestamp;
	}

	public String getNotificationType() {
		return notificationType;
	}

	public void setNotificationType(String notificationType) {
		this.notificationType = notificationType;
	}

	public byte[] getNotificationBody() {
		return notificationBody;
	}

	public void setNotificationBody(byte[] notificationBody) {
		this.notificationBody = notificationBody;
	}

	@Override
	public TableMapping<DBOSESNotification> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.SES_NOTIFICATIONS;
	}

	@Override
	public MigratableTableTranslation<DBOSESNotification, DBOSESNotification> getTranslator() {
		return new BasicMigratableTableTranslation<DBOSESNotification>();
	}

	@Override
	public Class<? extends DBOSESNotification> getBackupClass() {
		return DBOSESNotification.class;
	}

	@Override
	public Class<? extends DBOSESNotification> getDatabaseObjectClass() {
		return DBOSESNotification.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(notificationBody);
		result = prime * result + Objects.hash(createdOn, id, ispTimestamp, notificationType, sesEmailId, sesFeedbackId);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DBOSESNotification other = (DBOSESNotification) obj;
		return Objects.equals(createdOn, other.createdOn) && Objects.equals(id, other.id)
				&& Objects.equals(ispTimestamp, other.ispTimestamp) && Arrays.equals(notificationBody, other.notificationBody)
				&& Objects.equals(notificationType, other.notificationType) && Objects.equals(sesEmailId, other.sesEmailId)
				&& Objects.equals(sesFeedbackId, other.sesFeedbackId);
	}

	@Override
	public String toString() {
		return "DBOSESNotification [id=" + id + ", createdOn=" + createdOn + ", sesEmailId=" + sesEmailId + ", sesFeedbackId="
				+ sesFeedbackId + ", ispTimestamp=" + ispTimestamp + ", notificationType=" + notificationType + ", notificationBody="
				+ Arrays.toString(notificationBody) + "]";
	}

}
