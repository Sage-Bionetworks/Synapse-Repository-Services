package org.sagebionetworks.repo.model.dbo.persistence;

import java.io.UnsupportedEncodingException;
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
public class DBOMessageToUser implements MigratableDatabaseObject<DBOMessageToUser, DBOMessageToUser> {
	
	public static final String MESSAGE_ID_FIELD_NAME = "messageId";
	
	private static FieldColumn[] FIELDS = new FieldColumn[] {
		new FieldColumn(MESSAGE_ID_FIELD_NAME, SqlConstants.COL_MESSAGE_TO_USER_MESSAGE_ID, true).withIsBackupId(true),
		new FieldColumn("rootMessageId", SqlConstants.COL_MESSAGE_TO_USER_ROOT_ID), 
		new FieldColumn("inReplyTo", SqlConstants.COL_MESSAGE_TO_USER_REPLY_TO_ID), 
		new FieldColumn("subjectBytes", SqlConstants.COL_MESSAGE_TO_USER_SUBJECT),
		new FieldColumn("sent", SqlConstants.COL_MESSAGE_TO_USER_SENT),
		new FieldColumn("notificationsEndpoint", SqlConstants.COL_MESSAGE_NOTIFICATIONS_ENDPOINT),
		new FieldColumn("to", SqlConstants.COL_MESSAGE_TO_USER_TO),
		new FieldColumn("cc", SqlConstants.COL_MESSAGE_TO_USER_CC),
		new FieldColumn("bcc", SqlConstants.COL_MESSAGE_TO_USER_BCC)
	};
	
	private Long messageId;
	private Long rootMessageId;
	private Long inReplyTo;
	// we use a byte array to allow non-latin-1 characters
	private byte[] subjectBytes;
	private Boolean sent;
	private String notificationsEndpoint;
	private String to;
	private String cc;
	private String bcc;
	
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
				java.sql.Blob blob = rs.getBlob(SqlConstants.COL_MESSAGE_TO_USER_SUBJECT);
				if(blob != null){
					result.setSubjectBytes(blob.getBytes(1, (int) blob.length()));
				}

				result.setSent(rs.getBoolean(SqlConstants.COL_MESSAGE_TO_USER_SENT));
				result.setNotificationsEndpoint(rs.getString(SqlConstants.COL_MESSAGE_NOTIFICATIONS_ENDPOINT));
				result.setTo(rs.getString(SqlConstants.COL_MESSAGE_TO_USER_TO));
				result.setCc(rs.getString(SqlConstants.COL_MESSAGE_TO_USER_CC));
				result.setBcc(rs.getString(SqlConstants.COL_MESSAGE_TO_USER_BCC));
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

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getCc() {
		return cc;
	}

	public void setCc(String cc) {
		this.cc = cc;
	}

	public String getBcc() {
		return bcc;
	}

	public void setBcc(String bcc) {
		this.bcc = bcc;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.MESSAGE_TO_USER;
	}

	// once this has run for a single release, the back up objects will no longer have a 'subject'
	// field.  Then the translator can then be reverted to the simple, default version
	@Override
	public MigratableTableTranslation<DBOMessageToUser, DBOMessageToUser> getTranslator() {
		return new MigratableTableTranslation<DBOMessageToUser, DBOMessageToUser>() {
			@Override
			public DBOMessageToUser createDatabaseObjectFromBackup(DBOMessageToUser b) {
				DBOMessageToUserBackup backup = (DBOMessageToUserBackup)b;
				DBOMessageToUser dbo = new DBOMessageToUser();
				dbo.setTo(backup.getTo());
				dbo.setBcc(backup.getBcc());
				dbo.setCc(backup.getCc());
				dbo.setInReplyTo(backup.getInReplyTo());
				dbo.setMessageId(backup.getMessageId());
				dbo.setNotificationsEndpoint(backup.getNotificationsEndpoint());
				dbo.setRootMessageId(backup.getRootMessageId());
				dbo.setSent(backup.getSent());
				if (backup.getSubject()!=null) {
					try {
						dbo.setSubjectBytes(backup.getSubject().getBytes("UTF-8"));
					} catch (UnsupportedEncodingException e) {
						throw new RuntimeException(e);
					}
				} else {
					dbo.setSubjectBytes(backup.getSubjectBytes());
				}
				return dbo;
			}
			
			@Override
			public DBOMessageToUser createBackupFromDatabaseObject(DBOMessageToUser dbo) {
				return dbo;
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
		result = prime * result + ((bcc == null) ? 0 : bcc.hashCode());
		result = prime * result + ((cc == null) ? 0 : cc.hashCode());
		result = prime * result
				+ ((inReplyTo == null) ? 0 : inReplyTo.hashCode());
		result = prime * result
				+ ((messageId == null) ? 0 : messageId.hashCode());
		result = prime
				* result
				+ ((notificationsEndpoint == null) ? 0 : notificationsEndpoint
						.hashCode());
		result = prime * result
				+ ((rootMessageId == null) ? 0 : rootMessageId.hashCode());
		result = prime * result + ((sent == null) ? 0 : sent.hashCode());
		result = prime * result + Arrays.hashCode(subjectBytes);
		result = prime * result + ((to == null) ? 0 : to.hashCode());
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
		if (bcc == null) {
			if (other.bcc != null)
				return false;
		} else if (!bcc.equals(other.bcc))
			return false;
		if (cc == null) {
			if (other.cc != null)
				return false;
		} else if (!cc.equals(other.cc))
			return false;
		if (inReplyTo == null) {
			if (other.inReplyTo != null)
				return false;
		} else if (!inReplyTo.equals(other.inReplyTo))
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
		if (to == null) {
			if (other.to != null)
				return false;
		} else if (!to.equals(other.to))
			return false;
		return true;
	}


	@Override
	public String toString() {
		return "DBOMessageToUser [messageId=" + messageId + ", rootMessageId="
				+ rootMessageId + ", inReplyTo=" + inReplyTo + ", subject="
				+ Arrays.toString(subjectBytes) + ", sent=" + sent
				+ ", notificationsEndpoint=" + notificationsEndpoint + ", to="
				+ to + ", cc=" + cc + ", bcc=" + bcc + "]";
	}

}
