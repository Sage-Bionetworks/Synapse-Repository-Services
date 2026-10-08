package org.sagebionetworks.repo.model.dbo.dao.dataaccess;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.transactions.MandatoryWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EDucEnvelopeNotificationDaoImpl implements EDucEnvelopeNotificationDao {

	private final IdGenerator idGenerator;
	private final JdbcTemplate jdbcTemplate;
	private final DBOBasicDao basicDao;

	public EDucEnvelopeNotificationDaoImpl(IdGenerator idGenerator, JdbcTemplate jdbcTemplate,
			DBOBasicDao basicDao) {
		this.idGenerator = idGenerator;
		this.jdbcTemplate = jdbcTemplate;
		this.basicDao = basicDao;
	}

	@WriteTransaction
	@Override
	public DBOEDucEnvelopeNotification create(Long requestId, String envelopeId, String terminalStatus,
			Long messageId) {
		ValidateArgument.required(requestId, "requestId");
		ValidateArgument.required(envelopeId, "envelopeId");
		ValidateArgument.required(terminalStatus, "terminalStatus");
		ValidateArgument.required(messageId, "messageId");

		DBOEDucEnvelopeNotification dbo = new DBOEDucEnvelopeNotification();
		dbo.setId(idGenerator.generateNewId(IdType.EDUC_ENVELOPE_NOTIFICATION_ID));
		dbo.setEtag(UUID.randomUUID().toString());
		dbo.setEnvelopeId(envelopeId);
		dbo.setRequestId(requestId);
		dbo.setTerminalStatus(terminalStatus);
		dbo.setObservedOn(new Timestamp(System.currentTimeMillis()));
		dbo.setMessageId(messageId);

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

	@Override
	public List<EDucEnvelopeToExamine> listEnvelopesToExamine(long limit) {
		// Driven from the request rather than from the quota ledger: a quota row can be removed by an
		// administrative reset, whereas the request is the lasting record of which envelope it is using.
		return jdbcTemplate.query(
				"SELECT r.ID, r.EDUC_ENVELOPE_ID, r.CREATED_BY FROM DATA_ACCESS_REQUEST r"
						+ " LEFT JOIN EDUC_ENVELOPE_NOTIFICATION n ON r.EDUC_ENVELOPE_ID = n.ENVELOPE_ID"
						+ " WHERE r.EDUC_ENVELOPE_ID IS NOT NULL AND n.ENVELOPE_ID IS NULL"
						+ " ORDER BY r.ID LIMIT ?",
				(rs, rowNum) -> new EDucEnvelopeToExamine(rs.getLong("ID"), rs.getString("EDUC_ENVELOPE_ID"),
						rs.getLong("CREATED_BY")),
				limit);
	}

	@WriteTransaction
	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM EDUC_ENVELOPE_NOTIFICATION WHERE ID > -1");
	}
}
