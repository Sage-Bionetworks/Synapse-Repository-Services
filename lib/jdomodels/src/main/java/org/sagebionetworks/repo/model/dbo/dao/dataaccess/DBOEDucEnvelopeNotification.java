package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

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
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;

/**
 * One row per eDUC envelope observed to have reached a terminal state.
 * <p>
 * The row's existence is what stops an envelope being examined again, so it is written for every terminal
 * state — including those the requester is not notified about, which carry
 * {@link #NO_MESSAGE_SENT} in place of a message ID.
 * <p>
 * This table migrates. Were it left behind on a stack swap, every envelope already finished would look
 * unexamined and its requester would be notified a second time.
 */
public class DBOEDucEnvelopeNotification
		implements MigratableDatabaseObject<DBOEDucEnvelopeNotification, DBOEDucEnvelopeNotification> {

	/**
	 * Stands in for the message ID when a terminal state was recorded without notifying anyone, either
	 * because that state is not one the requester is told about or because delivery was suppressed.
	 */
	public static final long NO_MESSAGE_SENT = -1L;

	private static final FieldColumn[] FIELDS = new FieldColumn[] {
			new FieldColumn("id", SqlConstants.COL_EDUC_ENVELOPE_NOTIFICATION_ID, true).withIsBackupId(true),
			new FieldColumn("etag", SqlConstants.COL_EDUC_ENVELOPE_NOTIFICATION_ETAG).withIsEtag(true),
			new FieldColumn("envelopeId", SqlConstants.COL_EDUC_ENVELOPE_NOTIFICATION_ENVELOPE_ID),
			new FieldColumn("requestId", SqlConstants.COL_EDUC_ENVELOPE_NOTIFICATION_REQUEST_ID),
			new FieldColumn("terminalStatus", SqlConstants.COL_EDUC_ENVELOPE_NOTIFICATION_TERMINAL_STATUS),
			new FieldColumn("observedOn", SqlConstants.COL_EDUC_ENVELOPE_NOTIFICATION_OBSERVED_ON),
			new FieldColumn("messageId", SqlConstants.COL_EDUC_ENVELOPE_NOTIFICATION_MESSAGE_ID)
	};

	private static final TableMapping<DBOEDucEnvelopeNotification> TABLE_MAPPING = new TableMapping<DBOEDucEnvelopeNotification>() {

		@Override
		public DBOEDucEnvelopeNotification mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOEDucEnvelopeNotification dbo = new DBOEDucEnvelopeNotification();
			dbo.setId(rs.getLong(SqlConstants.COL_EDUC_ENVELOPE_NOTIFICATION_ID));
			dbo.setEtag(rs.getString(SqlConstants.COL_EDUC_ENVELOPE_NOTIFICATION_ETAG));
			dbo.setEnvelopeId(rs.getString(SqlConstants.COL_EDUC_ENVELOPE_NOTIFICATION_ENVELOPE_ID));
			dbo.setRequestId(rs.getLong(SqlConstants.COL_EDUC_ENVELOPE_NOTIFICATION_REQUEST_ID));
			dbo.setTerminalStatus(rs.getString(SqlConstants.COL_EDUC_ENVELOPE_NOTIFICATION_TERMINAL_STATUS));
			dbo.setObservedOn(rs.getTimestamp(SqlConstants.COL_EDUC_ENVELOPE_NOTIFICATION_OBSERVED_ON));
			dbo.setMessageId(rs.getLong(SqlConstants.COL_EDUC_ENVELOPE_NOTIFICATION_MESSAGE_ID));
			return dbo;
		}

		@Override
		public String getTableName() {
			return SqlConstants.TABLE_EDUC_ENVELOPE_NOTIFICATION;
		}

		@Override
		public String getDDLFileName() {
			return SqlConstants.DDL_EDUC_ENVELOPE_NOTIFICATION;
		}

		@Override
		public FieldColumn[] getFieldColumns() {
			return FIELDS;
		}

		@Override
		public Class<? extends DBOEDucEnvelopeNotification> getDBOClass() {
			return DBOEDucEnvelopeNotification.class;
		}
	};

	private static final MigratableTableTranslation<DBOEDucEnvelopeNotification, DBOEDucEnvelopeNotification> MIGRATION_TRANSLATOR = new BasicMigratableTableTranslation<>();

	private Long id;
	private String etag;
	private String envelopeId;
	private Long requestId;
	private String terminalStatus;
	private Timestamp observedOn;
	private Long messageId;

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

	public String getEnvelopeId() {
		return envelopeId;
	}

	public void setEnvelopeId(String envelopeId) {
		this.envelopeId = envelopeId;
	}

	public Long getRequestId() {
		return requestId;
	}

	public void setRequestId(Long requestId) {
		this.requestId = requestId;
	}

	public String getTerminalStatus() {
		return terminalStatus;
	}

	public void setTerminalStatus(String terminalStatus) {
		this.terminalStatus = terminalStatus;
	}

	public Timestamp getObservedOn() {
		return observedOn;
	}

	public void setObservedOn(Timestamp observedOn) {
		this.observedOn = observedOn;
	}

	public Long getMessageId() {
		return messageId;
	}

	public void setMessageId(Long messageId) {
		this.messageId = messageId;
	}

	@Override
	public TableMapping<DBOEDucEnvelopeNotification> getTableMapping() {
		return TABLE_MAPPING;
	}

	@Override
	public MigrationType getMigratableTableType() {
		return MigrationType.EDUC_ENVELOPE_NOTIFICATION;
	}

	@Override
	public MigratableTableTranslation<DBOEDucEnvelopeNotification, DBOEDucEnvelopeNotification> getTranslator() {
		return MIGRATION_TRANSLATOR;
	}

	@Override
	public Class<? extends DBOEDucEnvelopeNotification> getBackupClass() {
		return DBOEDucEnvelopeNotification.class;
	}

	@Override
	public Class<? extends DBOEDucEnvelopeNotification> getDatabaseObjectClass() {
		return DBOEDucEnvelopeNotification.class;
	}

	@Override
	public List<MigratableDatabaseObject<?, ?>> getSecondaryTypes() {
		return null;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, etag, envelopeId, requestId, terminalStatus, observedOn, messageId);
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
		DBOEDucEnvelopeNotification other = (DBOEDucEnvelopeNotification) obj;
		return Objects.equals(id, other.id)
				&& Objects.equals(etag, other.etag)
				&& Objects.equals(envelopeId, other.envelopeId)
				&& Objects.equals(requestId, other.requestId)
				&& Objects.equals(terminalStatus, other.terminalStatus)
				&& Objects.equals(observedOn, other.observedOn)
				&& Objects.equals(messageId, other.messageId);
	}

	@Override
	public String toString() {
		return "DBOEDucEnvelopeNotification [id=" + id + ", etag=" + etag + ", envelopeId=" + envelopeId
				+ ", requestId=" + requestId + ", terminalStatus=" + terminalStatus
				+ ", observedOn=" + observedOn + ", messageId=" + messageId + "]";
	}
}
