package org.sagebionetworks.repo.model.dbo.persistence;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.util.TemporaryCode;

/**
 * Contains information specific to a message sent to a user
 */
public class DBOMessageToUser implements MigratableDatabaseObject<DBOMessageToUser, DBOMessageToUserBackup> {

	public static final String MESSAGE_ID_FIELD_NAME = "messageId";

	private static FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn(MESSAGE_ID_FIELD_NAME, SqlConstants.COL_MESSAGE_TO_USER_MESSAGE_ID, true).withIsBackupId(true),
			new FieldColumn("rootMessageId", SqlConstants.COL_MESSAGE_TO_USER_ROOT_ID),
			new FieldColumn("inReplyTo", SqlConstants.COL_MESSAGE_TO_USER_REPLY_TO_ID),
			new FieldColumn("subjectBytes", SqlConstants.COL_MESSAGE_TO_USER_SUBJECT),
			new FieldColumn("sent", SqlConstants.COL_MESSAGE_TO_USER_SENT),
			new FieldColumn("notificationsEndpoint", SqlConstants.COL_MESSAGE_NOTIFICATIONS_ENDPOINT),
			new FieldColumn("profileSettingEndpoint", SqlConstants.COL_MESSAGE_PROFILE_SETTING_ENDPOINT),
			new FieldColumn("withUnsubscribeLink", SqlConstants.COL_MESSAGE_WITH_UNSUBSCRIBE_LINK),
			new FieldColumn("withProfileSettingLink", SqlConstants.COL_MESSAGE_WITH_PROFILE_SETTING_LINK),
			new FieldColumn("isNotificationMessage", SqlConstants.COL_MESSAGE_IS_NOTIFICATION_MESSAGE),
			new FieldColumn("overrideNotificationSettings", SqlConstants.COL_MESSAGE_OVERRIDE_NOTIFICATION_SETTINGS),
			new FieldColumn("bytesTo", SqlConstants.COL_MESSAGE_TO_USER_TO),
			new FieldColumn("bytesCc", SqlConstants.COL_MESSAGE_TO_USER_CC),
			new FieldColumn("bytesBcc", SqlConstants.COL_MESSAGE_TO_USER_BCC)
	};

	private Long messageId;
	private Long rootMessageId;
	private Long inReplyTo;
	// we use a byte array to allow non-latin-1 characters
	private byte[] subjectBytes;
	private byte[] bytesTo;
	private byte[] bytesCc;
	private byte[] bytesBcc;
	private Boolean sent;
	private String notificationsEndpoint;
	private String profileSettingEndpoint;
	private Boolean withUnsubscribeLink;
	private Boolean withProfileSettingLink;
	private Boolean isNotificationMessage;
	private Boolean overrideNotificationSettings;
	
	private static final MigratableTableTranslation<DBOMessageToUser, DBOMessageToUserBackup> MIGRATION_TRANSLATOR = new MigratableTableTranslation<DBOMessageToUser, DBOMessageToUserBackup>() {

		@TemporaryCode(author = "marco.marasca@sagebase.org", comment = "Can be removed afer 319 is deployed")
		@Override
		public DBOMessageToUser createDatabaseObjectFromBackup(DBOMessageToUserBackup backup) {
			DBOMessageToUser dbo = new DBOMessageToUser();
			
			dbo.setMessageId(backup.getMessageId());
			dbo.setRootMessageId(backup.getRootMessageId());
			dbo.setInReplyTo(backup.getInReplyTo());
			dbo.setSubjectBytes(backup.getSubjectBytes());
			dbo.setSent(backup.getSent());
			dbo.setNotificationsEndpoint(backup.getNotificationsEndpoint());
			dbo.setProfileSettingEndpoint(backup.getProfileSettingEndpoint());
			dbo.setWithUnsubscribeLink(backup.getWithUnsubscribeLink());
			dbo.setWithProfileSettingLink(backup.getWithProfileSettingLink());
			dbo.setIsNotificationMessage(backup.getIsNotificationMessage());
			dbo.setOverrideNotificationSettings(backup.getOverrideNotificationSettings() == null ? false : backup.getOverrideNotificationSettings());
			dbo.setBytesTo(backup.getBytesTo());
			dbo.setBytesCc(backup.getBytesCc());
			dbo.setBytesBcc(backup.getBytesBcc());
			
			return dbo;
		}
		
		@Override
		public DBOMessageToUserBackup createBackupFromDatabaseObject(DBOMessageToUser dbo) {
			DBOMessageToUserBackup backup = new DBOMessageToUserBackup();
			
			backup.setMessageId(dbo.getMessageId());
			backup.setRootMessageId(dbo.getRootMessageId());
			backup.setInReplyTo(dbo.getInReplyTo());
			backup.setSubjectBytes(dbo.getSubjectBytes());
			backup.setSent(dbo.getSent());
			backup.setNotificationsEndpoint(dbo.getNotificationsEndpoint());
			backup.setProfileSettingEndpoint(dbo.getProfileSettingEndpoint());
			backup.setWithUnsubscribeLink(dbo.getWithUnsubscribeLink());
			backup.setWithProfileSettingLink(dbo.getWithProfileSettingLink());
			backup.setIsNotificationMessage(dbo.getIsNotificationMessage());
			backup.setOverrideNotificationSettings(dbo.getOverrideNotificationSettings());
			backup.setBytesTo(dbo.getBytesTo());
			backup.setBytesCc(dbo.getBytesCc());
			backup.setBytesBcc(dbo.getBytesBcc());
			
			return backup;
		}
	};

	private static final TableMapping<DBOMessageToUser> TABLE_MAPPING = new TableMapping<DBOMessageToUser>() {

		@Override
		public DBOMessageToUser mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOMessageToUser result = new DBOMessageToUser();
			result.setMessageId(rs.getLong(SqlConstants.COL_MESSAGE_TO_USER_MESSAGE_ID));
			result.setRootMessageId(rs.getLong(SqlConstants.COL_MESSAGE_TO_USER_ROOT_ID));
			String replyTo = rs.getString(SqlConstants.COL_MESSAGE_TO_USER_REPLY_TO_ID);
			if (replyTo != null) {
				result.setInReplyTo(Long.parseLong(replyTo));
			};
			Blob subjectBytesBlob = rs.getBlob(SqlConstants.COL_MESSAGE_TO_USER_SUBJECT);
			if(subjectBytesBlob != null){
				result.setSubjectBytes(subjectBytesBlob.getBytes(1, (int) subjectBytesBlob.length()));
			}

			result.setSent(rs.getBoolean(SqlConstants.COL_MESSAGE_TO_USER_SENT));
			result.setNotificationsEndpoint(rs.getString(SqlConstants.COL_MESSAGE_NOTIFICATIONS_ENDPOINT));
			result.setProfileSettingEndpoint(rs.getString(SqlConstants.COL_MESSAGE_PROFILE_SETTING_ENDPOINT));
			result.setWithUnsubscribeLink(rs.getBoolean(SqlConstants.COL_MESSAGE_WITH_UNSUBSCRIBE_LINK));
			result.setWithProfileSettingLink(rs.getBoolean(SqlConstants.COL_MESSAGE_WITH_PROFILE_SETTING_LINK));
			result.setIsNotificationMessage(rs.getBoolean(SqlConstants.COL_MESSAGE_IS_NOTIFICATION_MESSAGE));
			result.setOverrideNotificationSettings(rs.getBoolean(SqlConstants.COL_MESSAGE_OVERRIDE_NOTIFICATION_SETTINGS));
			Blob toBlob = rs.getBlob(SqlConstants.COL_MESSAGE_TO_USER_TO);
			if (toBlob != null) {
				result.setBytesTo(toBlob.getBytes(1, (int) toBlob.length()));
			}
			Blob ccBlob = rs.getBlob(SqlConstants.COL_MESSAGE_TO_USER_CC);
			if (ccBlob != null) {
				result.setBytesCc(ccBlob.getBytes(1, (int) ccBlob.length()));
			}

			Blob bccBlob = rs.getBlob(SqlConstants.COL_MESSAGE_TO_USER_BCC);
			if (bccBlob != null) {
				result.setBytesBcc(bccBlob.getBytes(1, (int) bccBlob.length()));
			}
			return result;
		}

		@Override
		public String getTableName() {
			return SqlConstants.TABLE_MESSAGE_TO_USER;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public String getDDLFileName() {
			return SqlConstants.DDL_MESSAGE_TO_USER;
		}

		@Override
		public Class<? extends DBOMessageToUser> getDBOClass() {
			return DBOMessageToUser.class;
		}
	};
	@Override
	public TableMapping<DBOMessageToUser> getTableMapping() {
		return TABLE_MAPPING;
	}

	public Long getMessageId() {
		return messageId;
	}

	public void setMessageId(Long messageId) {
		this.messageId = messageId;
	}

	public Long getRootMessageId() {
		return rootMessageId;
	}

	public void setRootMessageId(Long rootMessageId) {
		this.rootMessageId = rootMessageId;
	}

	public Long getInReplyTo() {
		return inReplyTo;
	}

	public void setInReplyTo(Long inReplyTo) {
		this.inReplyTo = inReplyTo;
	}

	public byte[] getSubjectBytes() {
		return subjectBytes;
	}

	public void setSubjectBytes(byte[] subject) {
		this.subjectBytes = subject;
	}

	public Boolean getSent() {
		return sent;
	}

	public void setSent(Boolean sent) {
		this.sent = sent;
	}

	public String getNotificationsEndpoint() {
		return notificationsEndpoint;
	}

	public void setNotificationsEndpoint(String notificationsEndpoint) {
		this.notificationsEndpoint = notificationsEndpoint;
	}

	public String getProfileSettingEndpoint() {
		return profileSettingEndpoint;
	}

	public void setProfileSettingEndpoint(String profileSettingEndpoint) {
		this.profileSettingEndpoint = profileSettingEndpoint;
	}

	public Boolean getWithUnsubscribeLink() {
		return withUnsubscribeLink;
	}

	public void setWithUnsubscribeLink(Boolean withUnsubscribeLink) {
		this.withUnsubscribeLink = withUnsubscribeLink;
	}

	public Boolean getWithProfileSettingLink() {
		return withProfileSettingLink;
	}

	public void setWithProfileSettingLink(Boolean withProfileSettingLink) {
		this.withProfileSettingLink = withProfileSettingLink;
	}

	public Boolean getIsNotificationMessage() {
		return isNotificationMessage;
	}

	public void setIsNotificationMessage(Boolean isNotificationMessage) {
		this.isNotificationMessage = isNotificationMessage;
	}
	
	public Boolean getOverrideNotificationSettings() {
		return overrideNotificationSettings;
	}
	
	public void setOverrideNotificationSettings(Boolean overrideNotificationSettings) {
		this.overrideNotificationSettings = overrideNotificationSettings;
	}

	public byte[] getBytesTo() {
		return bytesTo;
	}

	public void setBytesTo(byte[] bytesTo) {
		this.bytesTo = bytesTo;
	}

	public byte[] getBytesCc() {
		return bytesCc;
	}

	public void setBytesCc(byte[] bytesCc) {
		this.bytesCc = bytesCc;
	}

	public byte[] getBytesBcc() {
		return bytesBcc;
	}

	public void setBytesBcc(byte[] bytesBcc) {
		this.bytesBcc = bytesBcc;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.MESSAGE_TO_USER;
	}

	@Override
	public MigratableTableTranslation<DBOMessageToUser, DBOMessageToUserBackup> getTranslator() {
		return MIGRATION_TRANSLATOR;
	}

	@Override
	public Class<? extends DBOMessageToUserBackup> getBackupClass() {
		return DBOMessageToUserBackup.class;
	}

	@Override
	public Class<? extends DBOMessageToUser> getDatabaseObjectClass() {
		return DBOMessageToUser.class;
	}

	@Override
	public List<MigratableDatabaseObject<?,?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(bytesBcc);
		result = prime * result + Arrays.hashCode(bytesCc);
		result = prime * result + Arrays.hashCode(bytesTo);
		result = prime * result + Arrays.hashCode(subjectBytes);
		result = prime * result + Objects.hash(inReplyTo, isNotificationMessage, messageId, notificationsEndpoint,
				overrideNotificationSettings, profileSettingEndpoint, rootMessageId, sent, withProfileSettingLink,
				withUnsubscribeLink);
		return result;
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
		DBOMessageToUser other = (DBOMessageToUser) obj;
		return Arrays.equals(bytesBcc, other.bytesBcc) && Arrays.equals(bytesCc, other.bytesCc)
				&& Arrays.equals(bytesTo, other.bytesTo) && Objects.equals(inReplyTo, other.inReplyTo)
				&& Objects.equals(isNotificationMessage, other.isNotificationMessage)
				&& Objects.equals(messageId, other.messageId)
				&& Objects.equals(notificationsEndpoint, other.notificationsEndpoint)
				&& Objects.equals(overrideNotificationSettings, other.overrideNotificationSettings)
				&& Objects.equals(profileSettingEndpoint, other.profileSettingEndpoint)
				&& Objects.equals(rootMessageId, other.rootMessageId) && Objects.equals(sent, other.sent)
				&& Arrays.equals(subjectBytes, other.subjectBytes)
				&& Objects.equals(withProfileSettingLink, other.withProfileSettingLink)
				&& Objects.equals(withUnsubscribeLink, other.withUnsubscribeLink);
	}

	@Override
	public String toString() {
		return "DBOMessageToUser [messageId=" + messageId + ", rootMessageId=" + rootMessageId + ", inReplyTo="
				+ inReplyTo + ", subjectBytes=" + Arrays.toString(subjectBytes) + ", bytesTo="
				+ Arrays.toString(bytesTo) + ", bytesCc=" + Arrays.toString(bytesCc) + ", bytesBcc="
				+ Arrays.toString(bytesBcc) + ", sent=" + sent + ", notificationsEndpoint=" + notificationsEndpoint
				+ ", profileSettingEndpoint=" + profileSettingEndpoint + ", withUnsubscribeLink=" + withUnsubscribeLink
				+ ", withProfileSettingLink=" + withProfileSettingLink + ", isNotificationMessage="
				+ isNotificationMessage + ", overrideNotificationSettings=" + overrideNotificationSettings + "]";
	}

}

