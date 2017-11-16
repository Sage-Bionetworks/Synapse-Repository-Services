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
import org.sagebionetworks.repo.model.dbo.migration.MigratableTableTranslation;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

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
	public MigratableTableTranslation<DBOMessageToUser, DBOMessageToUserBackup> getTranslator() {
		return new MigratableTableTranslation<DBOMessageToUser, DBOMessageToUserBackup>() {
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
				if (toCcBccAreStrings(backup)) {
					try {
						if (backup.getTo() != null) {
							dbo.setBytesTo(backup.getTo().getBytes("UTF-8"));
						}
						if (backup.getCc() != null) {
							dbo.setBytesCc(backup.getCc().getBytes("UTF-8"));
						}
						if (backup.getBcc() != null) {
							dbo.setBytesBcc(backup.getBcc().getBytes("UTF-8"));
						}
					} catch (UnsupportedEncodingException e) {
						throw new RuntimeException(e);
					}
				} else if (toCcBccAreByteArrays(backup)) {
					dbo.setBytesTo(backup.getBytesTo());
					dbo.setBytesCc(backup.getBytesCc());
					dbo.setBytesBcc(backup.getBytesBcc());
				} else {
					throw new IllegalStateException(
							"The backup object should not have mixed to, cc and bcc field types (String and byte[])");
				}
				return dbo;
			}

			private boolean toCcBccAreStrings(DBOMessageToUserBackup backup) {
				return backup.getBytesTo() == null && backup.getBytesCc() == null && backup.getBytesBcc() == null;
			}

			private boolean toCcBccAreByteArrays(DBOMessageToUserBackup backup) {
				return backup.getTo() == null && backup.getCc() == null && backup.getBcc() == null;
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
				if (dbo.getBytesTo() != null) {
					backup.setBytesTo(dbo.getBytesTo());
				}
				if (dbo.getBytesCc() != null) {
					backup.setBytesCc(dbo.getBytesCc());
				}
				if (dbo.getBytesBcc() != null) {
					backup.setBytesBcc(dbo.getBytesBcc());
				}
				return backup;
			}
		};
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
		result = prime * result + ((inReplyTo == null) ? 0 : inReplyTo.hashCode());
		result = prime * result + ((isNotificationMessage == null) ? 0 : isNotificationMessage.hashCode());
		result = prime * result + ((messageId == null) ? 0 : messageId.hashCode());
		result = prime * result + ((notificationsEndpoint == null) ? 0 : notificationsEndpoint.hashCode());
		result = prime * result + ((profileSettingEndpoint == null) ? 0 : profileSettingEndpoint.hashCode());
		result = prime * result + ((rootMessageId == null) ? 0 : rootMessageId.hashCode());
		result = prime * result + ((sent == null) ? 0 : sent.hashCode());
		result = prime * result + Arrays.hashCode(subjectBytes);
		result = prime * result + Arrays.hashCode(bytesTo);
		result = prime * result + ((withProfileSettingLink == null) ? 0 : withProfileSettingLink.hashCode());
		result = prime * result + ((withUnsubscribeLink == null) ? 0 : withUnsubscribeLink.hashCode());
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
		DBOMessageToUser other = (DBOMessageToUser) obj;
		if (!Arrays.equals(bytesTo, other.bytesTo))
			return false;
		if (!Arrays.equals(bytesCc, other.bytesCc))
			return false;
		if (inReplyTo == null) {
			if (other.inReplyTo != null)
				return false;
		} else if (!inReplyTo.equals(other.inReplyTo))
			return false;
		if (isNotificationMessage == null) {
			if (other.isNotificationMessage != null)
				return false;
		} else if (!isNotificationMessage.equals(other.isNotificationMessage))
			return false;
		if (messageId == null) {
			if (other.messageId != null)
				return false;
		} else if (!messageId.equals(other.messageId))
			return false;
		if (notificationsEndpoint == null) {
			if (other.notificationsEndpoint != null)
				return false;
		} else if (!notificationsEndpoint.equals(other.notificationsEndpoint))
			return false;
		if (profileSettingEndpoint == null) {
			if (other.profileSettingEndpoint != null)
				return false;
		} else if (!profileSettingEndpoint.equals(other.profileSettingEndpoint))
			return false;
		if (rootMessageId == null) {
			if (other.rootMessageId != null)
				return false;
		} else if (!rootMessageId.equals(other.rootMessageId))
			return false;
		if (sent == null) {
			if (other.sent != null)
				return false;
		} else if (!sent.equals(other.sent))
			return false;
		if (!Arrays.equals(subjectBytes, other.subjectBytes))
			return false;
		if (!Arrays.equals(bytesTo, other.bytesTo))
			return false;
		if (withProfileSettingLink == null) {
			if (other.withProfileSettingLink != null)
				return false;
		} else if (!withProfileSettingLink.equals(other.withProfileSettingLink))
			return false;
		if (withUnsubscribeLink == null) {
			if (other.withUnsubscribeLink != null)
				return false;
		} else if (!withUnsubscribeLink.equals(other.withUnsubscribeLink))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "DBOMessageToUser [messageId=" + messageId + ", rootMessageId=" + rootMessageId + ", inReplyTo="
				+ inReplyTo + ", subjectBytes=" + Arrays.toString(subjectBytes) + ", sent=" + sent
				+ ", notificationsEndpoint=" + notificationsEndpoint + ", profileSettingEndpoint="
				+ profileSettingEndpoint + ", withUnsubscribeLink=" + withUnsubscribeLink + ", withProfileSettingLink="
				+ withProfileSettingLink + ", isNotificationMessage=" + isNotificationMessage + ", bytesTo="
				+ Arrays.toString(bytesTo) + ", bytesCc=" + Arrays.toString(bytesCc) + ", bytesBcc=" + Arrays.toString(bytesBcc) + "]";
	}

}

