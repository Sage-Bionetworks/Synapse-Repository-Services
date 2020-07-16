package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

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
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

public class DBODataAccessNotification
		implements MigratableDatabaseObject<DBODataAccessNotification, DBODataAccessNotification> {

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", SqlConstants.COL_DATA_ACCESS_NOTIFICATION_ID, true).withIsBackupId(true),
			new FieldColumn("etag", SqlConstants.COL_DATA_ACCESS_NOTIFICATION_ETAG).withIsEtag(true),
			new FieldColumn("notificationType", SqlConstants.COL_DATA_ACCESS_NOTIFICATION_TYPE),
			new FieldColumn("requirementId", SqlConstants.COL_DATA_ACCESS_NOTIFICATION_REQUIREMENT_ID),
			new FieldColumn("recipientId", SqlConstants.COL_DATA_ACCESS_NOTIFICATION_RECIPIENT_ID),
			new FieldColumn("accessApprovalId", SqlConstants.COL_DATA_ACCESS_NOTIFICATION_APPROVAL_ID),
			new FieldColumn("messageId", SqlConstants.COL_DATA_ACCESS_NOTIFICATION_MESSAGE_ID),
			new FieldColumn("sentOn", SqlConstants.COL_DATA_ACCESS_NOTIFICATION_SENT_ON) };

	private static final TableMapping<DBODataAccessNotification> TABLE_MAPPING = new TableMapping<DBODataAccessNotification>() {

		@Override
		public DBODataAccessNotification mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBODataAccessNotification dbo = new DBODataAccessNotification();

			dbo.setId(rs.getLong(SqlConstants.COL_DATA_ACCESS_NOTIFICATION_ID));
			dbo.setEtag(rs.getString(SqlConstants.COL_DATA_ACCESS_NOTIFICATION_ETAG));
			dbo.setNotificationType(rs.getString(SqlConstants.COL_DATA_ACCESS_NOTIFICATION_TYPE));
			dbo.setRequirementId(rs.getLong(SqlConstants.COL_DATA_ACCESS_NOTIFICATION_REQUIREMENT_ID));
			dbo.setRecipientId(rs.getLong(SqlConstants.COL_DATA_ACCESS_NOTIFICATION_RECIPIENT_ID));
			dbo.setAccessApprovalId(rs.getLong(SqlConstants.COL_DATA_ACCESS_NOTIFICATION_APPROVAL_ID));
			dbo.setMessageId(rs.getLong(SqlConstants.COL_DATA_ACCESS_NOTIFICATION_MESSAGE_ID));
			dbo.setSentOn(rs.getLong(SqlConstants.COL_DATA_ACCESS_NOTIFICATION_SENT_ON));

			return dbo;
		}

		@Override
		public String getTableName() {
			return SqlConstants.TABLE_DATA_ACCESS_NOTIFICATION;
		}

		@Override
		public String getDDLFileName() {
			return SqlConstants.DDL_DATA_ACCESS_NOTIFICATION;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBODataAccessNotification> getDBOClass() {
			return DBODataAccessNotification.class;
		}
	};

	private static final MigratableTableTranslation<DBODataAccessNotification, DBODataAccessNotification> MIGRATION_TRANSLATOR = new BasicMigratableTableTranslation<>();

	private Long id;
	private String etag;
	private String notificationType;
	private Long requirementId;
	private Long recipientId;
	private Long accessApprovalId;
	private Long messageId;
	private Long sentOn;

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

	public String getNotificationType() {
		return notificationType;
	}

	public void setNotificationType(String notificationType) {
		this.notificationType = notificationType;
	}

	public Long getRequirementId() {
		return requirementId;
	}

	public void setRequirementId(Long requirementId) {
		this.requirementId = requirementId;
	}

	public Long getRecipientId() {
		return recipientId;
	}

	public void setRecipientId(Long recipientId) {
		this.recipientId = recipientId;
	}

	public Long getAccessApprovalId() {
		return accessApprovalId;
	}

	public void setAccessApprovalId(Long accessApprovalId) {
		this.accessApprovalId = accessApprovalId;
	}

	public Long getMessageId() {
		return messageId;
	}

	public void setMessageId(Long messageId) {
		this.messageId = messageId;
	}

	public Long getSentOn() {
		return sentOn;
	}

	public void setSentOn(Long sentOn) {
		this.sentOn = sentOn;
	}

	@Override
	public TableMapping<DBODataAccessNotification> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.DATA_ACCESS_NOTIFICATIONS;
	}

	@Override
	public MigratableTableTranslation<DBODataAccessNotification, DBODataAccessNotification> getTranslator() {
		return MIGRATION_TRANSLATOR;
	}

	@Override
	public Class<? extends DBODataAccessNotification> getBackupClass() {
		return DBODataAccessNotification.class;
	}

	@Override
	public Class<? extends DBODataAccessNotification> getDatabaseObjectClass() {
		return DBODataAccessNotification.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(accessApprovalId, etag, id, messageId, notificationType, recipientId, requirementId,
				sentOn);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DBODataAccessNotification other = (DBODataAccessNotification) obj;
		return Objects.equals(accessApprovalId, other.accessApprovalId) && Objects.equals(etag, other.etag)
				&& Objects.equals(id, other.id) && Objects.equals(messageId, other.messageId)
				&& Objects.equals(notificationType, other.notificationType)
				&& Objects.equals(recipientId, other.recipientId) && Objects.equals(requirementId, other.requirementId)
				&& Objects.equals(sentOn, other.sentOn);
	}

	@Override
	public String toString() {
		return "DBODataAccessNotification [id=" + id + ", etag=" + etag + ", notificationType=" + notificationType
				+ ", requirementId=" + requirementId + ", recipientId=" + recipientId + ", accessApprovalId="
				+ accessApprovalId + ", messageId=" + messageId + ", sentOn=" + sentOn + "]";
	}

}
