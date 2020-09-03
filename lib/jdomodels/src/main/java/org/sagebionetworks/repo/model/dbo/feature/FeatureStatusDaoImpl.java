package org.sagebionetworks.repo.model.dbo.feature;

import java.util.Optional;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.feature.Feature;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FeatureStatusDaoImpl implements FeatureStatusDao {

	private static final String SQL_SELECT_ENABLED = "SELECT " + SqlConstants.COL_FEATURE_STATUS_ENABLED 
			+ " FROM " + SqlConstants.TABLE_FEATURE_STATUS 
			+ " WHERE " + SqlConstants.COL_FEATURE_STATUS_TYPE + " = ?";
	
	private static final String SQL_INSERT_UPDATE = "INSERT INTO " + SqlConstants.TABLE_FEATURE_STATUS
			+ " ("
			+ SqlConstants.COL_FEATURE_STATUS_ID + ","
			+ SqlConstants.COL_FEATURE_STATUS_ETAG + ","
			+ SqlConstants.COL_FEATURE_STATUS_TYPE + ","
			+ SqlConstants.COL_FEATURE_STATUS_ENABLED 
			+ ") VALUES (?, UUID(), ?, ?) ON DUPLICATE KEY UPDATE "
			+ SqlConstants.COL_FEATURE_STATUS_ETAG + " = UUID(), "
			+ SqlConstants.COL_FEATURE_STATUS_ENABLED + " = ?";

	private JdbcTemplate jdbcTemplate;
	private IdGenerator idGenerator;

	@Autowired
	public FeatureStatusDaoImpl(final JdbcTemplate jdbcTemplate, final IdGenerator idGenerator) {
		this.jdbcTemplate = jdbcTemplate;
		this.idGenerator = idGenerator;
	}
	
	@Override
	@WriteTransaction
	public void setFeatureEnabled(Feature feature, boolean enabled) {
		
		jdbcTemplate.update(SQL_INSERT_UPDATE, (ps) -> {
			int index = 0;
			// Create fields
			ps.setLong(++index, idGenerator.generateNewId(IdType.FEATURE_STATUS_ID));
			ps.setString(++index, feature.name());
			ps.setBoolean(++index, enabled);
			// On update
			ps.setBoolean(++index, enabled);
		});
		
	}

	@Override
	public Optional<Boolean> isFeatureEnabled(Feature feature) {

		try {
			Boolean result = jdbcTemplate.queryForObject(SQL_SELECT_ENABLED, Boolean.class, feature.name());

			return Optional.of(result);

		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		}
	}

	@Override
	public void clear() {
		jdbcTemplate.batchUpdate("TRUNCATE TABLE " + SqlConstants.TABLE_FEATURE_STATUS);	
	}
	
}
