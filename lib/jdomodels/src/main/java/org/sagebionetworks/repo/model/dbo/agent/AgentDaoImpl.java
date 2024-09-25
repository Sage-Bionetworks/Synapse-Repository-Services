package org.sagebionetworks.repo.model.dbo.agent;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_SESSION_ACCESS_LEVEL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_SESSION_AGENT_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_SESSION_CREATED_BY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_SESSION_CREATED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_SESSION_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_SESSION_MODIFIED_ON;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_AGENT_SESSION_SESSION_ID;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.agent.AgentAccessLevel;
import org.sagebionetworks.repo.model.agent.AgentSession;
import org.sagebionetworks.repo.model.agent.TraceEvent;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.transactions.NewWriteTransaction;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AgentDaoImpl implements AgentDao {

	private final IdGenerator idGenerator;
	private final DBOBasicDao basicDao;
	private final JdbcTemplate jdbcTemplate;

	private final RowMapper<AgentSession> SESSION_MAPPER = new RowMapper<AgentSession>() {

		@Override
		public AgentSession mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new AgentSession().setSessionId(rs.getString(COL_AGENT_SESSION_SESSION_ID))
					.setAgentAccessLevel(AgentAccessLevel.valueOf(rs.getString(COL_AGENT_SESSION_ACCESS_LEVEL)))
					.setStartedBy(rs.getLong(COL_AGENT_SESSION_CREATED_BY))
					.setStartedOn(rs.getTimestamp(COL_AGENT_SESSION_CREATED_ON))
					.setModifiedOn(rs.getTimestamp(COL_AGENT_SESSION_MODIFIED_ON))
					.setAgentId(rs.getString(COL_AGENT_SESSION_AGENT_ID)).setEtag(rs.getString(COL_AGENT_SESSION_ETAG));
		}
	};

	@Autowired
	public AgentDaoImpl(IdGenerator idGenerator, DBOBasicDao basicDao, JdbcTemplate jdbcTemplate) {
		super();
		this.idGenerator = idGenerator;
		this.basicDao = basicDao;
		this.jdbcTemplate = jdbcTemplate;
	}

	@WriteTransaction
	@Override
	public AgentSession createSession(Long userId, AgentAccessLevel accessLevel, String agentId) {
		ValidateArgument.required(userId, "userId");
		ValidateArgument.required(accessLevel, "accessLevel");
		ValidateArgument.required(agentId, "agentId");
		Timestamp now = new Timestamp(System.currentTimeMillis());
		DBOAgentSession dbo = new DBOAgentSession().setId(idGenerator.generateNewId(IdType.AGENT_SESSION_ID))
				.setEtag(UUID.randomUUID().toString()).setCreatedBy(userId).setCreatedOn(now).setModifiedOn(now)
				.setSessionId(UUID.randomUUID().toString()).setAgentId(agentId).setAccessLevel(accessLevel.name());
		basicDao.createNew(dbo);
		return getAgentSession(dbo.getSessionId()).get();
	}

	@Override
	public Optional<AgentSession> getAgentSession(String sessionId) {
		ValidateArgument.required(sessionId, sessionId);
		try {
			return Optional.of(jdbcTemplate.queryForObject("SELECT * FROM AGENT_SESSION WHERE SESSION_ID = ?",
					SESSION_MAPPER, sessionId));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@WriteTransaction
	@Override
	public AgentSession updateSession(String sessionId, AgentAccessLevel accessLevel) {
		ValidateArgument.required(sessionId, "sessionId");
		ValidateArgument.required(accessLevel, "accessLevel");
		jdbcTemplate.update(
				"UPDATE AGENT_SESSION SET ETAG = UUID(), MODIFIED_ON = NOW(), ACCESS_LEVEL = ? WHERE SESSION_ID = ?",
				accessLevel.name(), sessionId);
		return getAgentSession(sessionId).get();
	}

	@WriteTransaction
	@Override
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM AGENT_SESSION WHERE ID > -1");
	}

	@NewWriteTransaction
	@Override
	public void addTraceToJob(String jobId, long timestamp, String message) {
		ValidateArgument.required(jobId, "jobId");
		ValidateArgument.required(message, "message");
		long jobIdLong = Long.parseLong(jobId);
		jdbcTemplate.update(
				"INSERT INTO AGENT_TRACE (JOB_ID, TIME_STAMP, MESSAGE) VALUES (?,?,?) ON DUPLICATE KEY UPDATE MESSAGE = ?",
				jobIdLong, timestamp, message, message);

	}

	@Override
	public List<TraceEvent> listTraceEvents(String jobId, Long timestamp) {
		ValidateArgument.required(jobId, "jobId");
		Long jobIdLong = Long.parseLong(jobId);
		if (timestamp == null) {
			timestamp = 0L;
		}
		return jdbcTemplate.query(
				"SELECT TIME_STAMP, MESSAGE FROM AGENT_TRACE WHERE JOB_ID = ? AND TIME_STAMP > ? ORDER BY TIME_STAMP ASC",
				(ResultSet rs, int rowNum) -> {
					return new TraceEvent().setTimestamp(rs.getLong("TIME_STAMP")).setMessage(rs.getString("MESSAGE"));
				}, jobIdLong, timestamp);
	}

}
