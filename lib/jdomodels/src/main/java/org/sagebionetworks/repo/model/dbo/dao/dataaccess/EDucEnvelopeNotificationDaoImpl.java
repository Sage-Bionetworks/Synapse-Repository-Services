package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.educ.EDucStatusEnum;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EDucEnvelopeNotificationDaoImpl implements EDucEnvelopeNotificationDao {

	private final IdGenerator idGenerator;
	private final JdbcTemplate jdbcTemplate;
	private final NamedParameterJdbcTemplate namedJdbcTemplate;
	private final DBOBasicDao basicDao;

	public EDucEnvelopeNotificationDaoImpl(IdGenerator idGenerator, JdbcTemplate jdbcTemplate,
			NamedParameterJdbcTemplate namedJdbcTemplate, DBOBasicDao basicDao) {
		this.idGenerator = idGenerator;
		this.jdbcTemplate = jdbcTemplate;
		this.namedJdbcTemplate = namedJdbcTemplate;
		this.basicDao = basicDao;
	}

	@WriteTransaction
	@Override
	public DBOEDucEnvelopeNotification createPending(Long requestId, String envelopeId,
			EDucStatusEnum terminalStatus) {
		ValidateArgument.required(requestId, "requestId");
		ValidateArgument.required(envelopeId, "envelopeId");
		ValidateArgument.required(terminalStatus, "terminalStatus");

		DBOEDucEnvelopeNotification dbo = new DBOEDucEnvelopeNotification();
		dbo.setId(idGenerator.generateNewId(IdType.EDUC_ENVELOPE_NOTIFICATION_ID));
		dbo.setEtag(UUID.randomUUID().toString());
		dbo.setEnvelopeId(envelopeId);
		dbo.setRequestId(requestId);
		dbo.setTerminalStatus(terminalStatus.name());
		dbo.setObservedOn(new Timestamp(System.currentTimeMillis()));
		dbo.setNotificationState(EDucNotificationState.PENDING.name());

		return basicDao.createNew(dbo);
	}

	@MandatoryWriteTransaction
	@Override
	public Optional<DBOEDucEnvelopeNotification> findForUpdate(String envelopeId) {
		ValidateArgument.required(envelopeId, "envelopeId");
		return queryForNotification("SELECT * FROM EDUC_ENVELOPE_NOTIFICATION WHERE ENVELOPE_ID = ? FOR UPDATE",
				envelopeId);
	}

	@Override
	public Optional<DBOEDucEnvelopeNotification> find(String envelopeId) {
		ValidateArgument.required(envelopeId, "envelopeId");
		return queryForNotification("SELECT * FROM EDUC_ENVELOPE_NOTIFICATION WHERE ENVELOPE_ID = ?", envelopeId);
	}

	private Optional<DBOEDucEnvelopeNotification> queryForNotification(String sql, String envelopeId) {
		return jdbcTemplate
				.query(sql, new DBOEDucEnvelopeNotification().getTableMapping(), envelopeId)
				.stream().findFirst();
	}

	@WriteTransaction
	@Override
	public void updateNotificationState(String envelopeId, EDucNotificationState state, Long messageId) {
		ValidateArgument.required(envelopeId, "envelopeId");
		ValidateArgument.required(state, "state");
		ValidateArgument.requirement(state != EDucNotificationState.PENDING,
				"A notification cannot be returned to PENDING.");
		ValidateArgument.requirement((state == EDucNotificationState.SENT) == (messageId != null),
				"A message ID is required for SENT and not allowed otherwise.");

		jdbcTemplate.update("UPDATE EDUC_ENVELOPE_NOTIFICATION SET NOTIFICATION_STATE = ?, MESSAGE_ID = ?,"
				+ " NOTIFIED_ON = NOW(3), ETAG = UUID() WHERE ENVELOPE_ID = ?", state.name(), messageId, envelopeId);
	}

	@Override
	public Map<String, Long> findRequestIdsByEnvelopeId(Collection<String> envelopeIds) {
		ValidateArgument.required(envelopeIds, "envelopeIds");
		Map<String, Long> requestIdsByEnvelopeId = new HashMap<>();
		if (envelopeIds.isEmpty()) {
			return requestIdsByEnvelopeId;
		}
		namedJdbcTemplate.query(
				"SELECT ID, EDUC_ENVELOPE_ID FROM DATA_ACCESS_REQUEST WHERE EDUC_ENVELOPE_ID IN (:envelopeIds)",
				new MapSqlParameterSource("envelopeIds", envelopeIds),
				rs -> {
					requestIdsByEnvelopeId.put(rs.getString("EDUC_ENVELOPE_ID"), rs.getLong("ID"));
				});
		return requestIdsByEnvelopeId;
	}

	@Override
	public List<EDucPendingNotification> listPendingNotifications(long limit) {
		return jdbcTemplate.query(
				"SELECT n.REQUEST_ID, n.ENVELOPE_ID, n.TERMINAL_STATUS, r.CREATED_BY FROM EDUC_ENVELOPE_NOTIFICATION n"
						+ " JOIN DATA_ACCESS_REQUEST r ON n.REQUEST_ID = r.ID"
						+ " WHERE n.NOTIFICATION_STATE = ? ORDER BY n.ID LIMIT ?",
				(rs, rowNum) -> new EDucPendingNotification(rs.getLong("REQUEST_ID"), rs.getString("ENVELOPE_ID"),
						EDucStatusEnum.valueOf(rs.getString("TERMINAL_STATUS")), rs.getLong("CREATED_BY")),
				EDucNotificationState.PENDING.name(), limit);
	}

	@Override
	public Optional<Instant> getStatusCheckpoint() {
		return jdbcTemplate
				.queryForList("SELECT LAST_QUERIED_ON FROM EDUC_ENVELOPE_STATUS_CHECKPOINT WHERE ID = ?",
						Timestamp.class, DBOEDucEnvelopeStatusCheckpoint.CHECKPOINT_ID)
				.stream().findFirst().map(Timestamp::toInstant);
	}

	@WriteTransaction
	@Override
	public void setStatusCheckpoint(Instant lastQueriedOn) {
		ValidateArgument.required(lastQueriedOn, "lastQueriedOn");
		// Truncated rather than left for the column to round, since rounding up could skip a change made in
		// the last fraction of a millisecond
		Timestamp checkpoint = Timestamp.from(lastQueriedOn.truncatedTo(ChronoUnit.MILLIS));
		jdbcTemplate.update("INSERT INTO EDUC_ENVELOPE_STATUS_CHECKPOINT (ID, LAST_QUERIED_ON) VALUES (?, ?)"
				+ " ON DUPLICATE KEY UPDATE LAST_QUERIED_ON = ?", DBOEDucEnvelopeStatusCheckpoint.CHECKPOINT_ID,
				checkpoint, checkpoint);
	}

	@WriteTransaction
	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM EDUC_ENVELOPE_NOTIFICATION WHERE ID > -1");
		jdbcTemplate.update("DELETE FROM EDUC_ENVELOPE_STATUS_CHECKPOINT WHERE ID > -1");
	}
}
