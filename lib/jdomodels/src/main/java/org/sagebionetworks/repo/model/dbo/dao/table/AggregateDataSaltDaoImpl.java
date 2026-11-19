package org.sagebionetworks.repo.model.dbo.dao.table;

import java.util.Optional;

import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AggregateDataSaltDaoImpl implements AggregateDataSaltDao {

	private final JdbcTemplate jdbcTemplate;

	public AggregateDataSaltDaoImpl(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Optional<String> getSalt(long objectId) {
		return jdbcTemplate.queryForList("SELECT SALT FROM AGGREGATE_DATA_SALT WHERE OBJECT_ID = ?", String.class, objectId)
				.stream().findFirst();
	}

	@Override
	@WriteTransaction
	public String createSaltIfAbsent(long objectId, String candidateSalt) {
		ValidateArgument.requiredNotBlank(candidateSalt, "candidateSalt");
		// The no-op update keeps the first salt when callers race. A racing insert that has not committed yet
		// holds the key lock, so this insert waits for it, and the read below then sees the winner's salt.
		// Unlike INSERT IGNORE, it still fails when the node does not exist.
		jdbcTemplate.update("INSERT INTO AGGREGATE_DATA_SALT (OBJECT_ID, SALT) VALUES (?, ?) ON DUPLICATE KEY UPDATE OBJECT_ID = OBJECT_ID",
				objectId, candidateSalt);
		return jdbcTemplate.queryForObject("SELECT SALT FROM AGGREGATE_DATA_SALT WHERE OBJECT_ID = ?", String.class, objectId);
	}

	@Override
	@WriteTransaction
	public void truncateAll() {
		jdbcTemplate.update("DELETE FROM AGGREGATE_DATA_SALT WHERE OBJECT_ID > -1");
	}

}
