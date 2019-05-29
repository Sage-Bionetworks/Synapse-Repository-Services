package org.sagebionetworks.repo.model.dbo.persistence;

import java.io.UnsupportedEncodingException;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.sagebionetworks.repo.model.dbo.FieldColumn;
import org.sagebionetworks.repo.model.dbo.MigratableDatabaseObject;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.migration.BasicMigratableTableTranslation;
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

/**
 * Contains information specific to a message sent to a user
 */
public class DBOMessageToUser implements MigratableDatabaseObject<DBOMessageToUser, DBOMessageToUser> {

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

	@Override
	public TableMapping<DBOMessageToUser> getTableMapping() {
		return new TableMapping<DBOMessageToUser>() {

			@Override
			public DBOMessageToUser mapRow(ResultSet rs, int rowNum) throws SQLException {
				DBOMessageToUser result = new DBOMessageToUser();
				result.setMessageId(rs.getLong(SqlConstants.COL_MESSAGE_TO_USER_MESSAGE_ID));
				result.setRootMessageId(rs.getLong(SqlConstants.COL_MESSAGE_TO_USER_ROOT_ID));

				long replyToId = rs.getLong(SqlConstants.COL_MESSAGE_TO_USER_REPLY_TO_ID);
				result.setInReplyTo(rs.wasNull() ? null : replyToId);

				result.setSubjectBytes(rs.getBytes(SqlConstants.COL_MESSAGE_TO_USER_SUBJECT));
				result.setSent(rs.getBoolean(SqlConstants.COL_MESSAGE_TO_USER_SENT));
				result.setNotificationsEndpoint(rs.getString(SqlConstants.COL_MESSAGE_NOTIFICATIONS_ENDPOINT));
				result.setProfileSettingEndpoint(rs.getString(SqlConstants.COL_MESSAGE_PROFILE_SETTING_ENDPOINT));
				result.setWithUnsubscribeLink(rs.getBoolean(SqlConstants.COL_MESSAGE_WITH_UNSUBSCRIBE_LINK));
				result.setWithProfileSettingLink(rs.getBoolean(SqlConstants.COL_MESSAGE_WITH_PROFILE_SETTING_LINK));
				result.setIsNotificationMessage(rs.getBoolean(SqlConstants.COL_MESSAGE_IS_NOTIFICATION_MESSAGE));
				result.setBytesTo(rs.getBytes(SqlConstants.COL_MESSAGE_TO_USER_TO));
				result.setBytesCc(rs.getBytes(SqlConstants.COL_MESSAGE_TO_USER_CC));
				result.setBytesBcc(rs.getBytes(SqlConstants.COL_MESSAGE_TO_USER_BCC));
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
	public MigratableTableTranslation<DBOMessageToUser, DBOMessageToUser> getTranslator() {
		return new BasicMigratableTableTranslation<>();
	}

	@Override
	public Class<? extends DBOMessageToUser> getBackupClass() {
		return DBOMessageToUser.class;
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
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		DBOMessageToUser that = (DBOMessageToUser) o;

		if (messageId != null ? !messageId.equals(that.messageId) : that.messageId != null) return false;
		if (rootMessageId != null ? !rootMessageId.equals(that.rootMessageId) : that.rootMessageId != null)
			return false;
		if (inReplyTo != null ? !inReplyTo.equals(that.inReplyTo) : that.inReplyTo != null) return false;
		if (!Arrays.equals(subjectBytes, that.subjectBytes)) return false;
		if (!Arrays.equals(bytesTo, that.bytesTo)) return false;
		if (!Arrays.equals(bytesCc, that.bytesCc)) return false;
		if (!Arrays.equals(bytesBcc, that.bytesBcc)) return false;
		if (sent != null ? !sent.equals(that.sent) : that.sent != null) return false;
		if (notificationsEndpoint != null ? !notificationsEndpoint.equals(that.notificationsEndpoint) : that.notificationsEndpoint != null)
			return false;
		if (profileSettingEndpoint != null ? !profileSettingEndpoint.equals(that.profileSettingEndpoint) : that.profileSettingEndpoint != null)
			return false;
		if (withUnsubscribeLink != null ? !withUnsubscribeLink.equals(that.withUnsubscribeLink) : that.withUnsubscribeLink != null)
			return false;
		if (withProfileSettingLink != null ? !withProfileSettingLink.equals(that.withProfileSettingLink) : that.withProfileSettingLink != null)
			return false;
		return isNotificationMessage != null ? isNotificationMessage.equals(that.isNotificationMessage) : that.isNotificationMessage == null;
	}

	@Override
	public int hashCode() {
		int result = messageId != null ? messageId.hashCode() : 0;
		result = 31 * result + (rootMessageId != null ? rootMessageId.hashCode() : 0);
		result = 31 * result + (inReplyTo != null ? inReplyTo.hashCode() : 0);
		result = 31 * result + Arrays.hashCode(subjectBytes);
		result = 31 * result + Arrays.hashCode(bytesTo);
		result = 31 * result + Arrays.hashCode(bytesCc);
		result = 31 * result + Arrays.hashCode(bytesBcc);
		result = 31 * result + (sent != null ? sent.hashCode() : 0);
		result = 31 * result + (notificationsEndpoint != null ? notificationsEndpoint.hashCode() : 0);
		result = 31 * result + (profileSettingEndpoint != null ? profileSettingEndpoint.hashCode() : 0);
		result = 31 * result + (withUnsubscribeLink != null ? withUnsubscribeLink.hashCode() : 0);
		result = 31 * result + (withProfileSettingLink != null ? withProfileSettingLink.hashCode() : 0);
		result = 31 * result + (isNotificationMessage != null ? isNotificationMessage.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "DBOMessageToUser{" +
				"messageId=" + messageId +
				", rootMessageId=" + rootMessageId +
				", inReplyTo=" + inReplyTo +
				", subjectBytes=" + Arrays.toString(subjectBytes) +
				", bytesTo=" + Arrays.toString(bytesTo) +
				", bytesCc=" + Arrays.toString(bytesCc) +
				", bytesBcc=" + Arrays.toString(bytesBcc) +
				", sent=" + sent +
				", notificationsEndpoint='" + notificationsEndpoint + '\'' +
				", profileSettingEndpoint='" + profileSettingEndpoint + '\'' +
				", withUnsubscribeLink=" + withUnsubscribeLink +
				", withProfileSettingLink=" + withProfileSettingLink +
				", isNotificationMessage=" + isNotificationMessage +
				'}';
	}
}

