package org.sagebionetworks.repo.model.dbo.dao.throttle;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_THROTTLE_RULES_CALL_PERIOD;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_THROTTLE_RULES_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_THROTTLE_RULES_MAX_CALLS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_THROTTLE_RULES_NORMALIZED_URI;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_THROTTLE_RULES;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.sagebionetworks.repo.model.dao.throttle.ThrottleRulesDAO;
import org.sagebionetworks.repo.model.throttle.ThrottleRule;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class DBOThrottleRulesDAOImpl implements ThrottleRulesDAO {
	private static final RowMapper<ThrottleRule> THROTTLE_RULE_ROW_MAPPER = new RowMapper<ThrottleRule>() {

		@Override
		public ThrottleRule mapRow(ResultSet rs, int rowNum) throws SQLException {
			ThrottleRule throttleRule = new ThrottleRule();
			throttleRule.setId(rs.getLong(COL_THROTTLE_RULES_ID));
			throttleRule.setNormalizedPath(rs.getString(COL_THROTTLE_RULES_NORMALIZED_URI));
			throttleRule.setMaxCallsPerPeriod(rs.getLong(COL_THROTTLE_RULES_MAX_CALLS));
			throttleRule.setPeriod(rs.getLong(COL_THROTTLE_RULES_CALL_PERIOD));
			return throttleRule;
		}
	};
	
	//parameters used in namedParametersJdbcTemplate
	
	//TODO: maybe add sql that fetches only ones updated after certain timestamp
	private static final String SQL_GET_THROTTLES = "SELECT * FROM " + TABLE_THROTTLE_RULES;
	private static final String SQL_ADD_THROTTLE = "INSERT INTO " + TABLE_THROTTLE_RULES +
		" (" +COL_THROTTLE_RULES_ID + ", " + COL_THROTTLE_RULES_NORMALIZED_URI + ", " + COL_THROTTLE_RULES_MAX_CALLS + ", " + COL_THROTTLE_RULES_CALL_PERIOD 
		+ ") VALUES ( ?, ?, ?, ?)";
	private static final String SQL_TRUNCATE = "TRUNCATE TABLE " + TABLE_THROTTLE_RULES;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Override
	public List<ThrottleRule> getAllThrottleRules() {
		return jdbcTemplate.query(SQL_GET_THROTTLES, THROTTLE_RULE_ROW_MAPPER);
	}
	
	@WriteTransaction
	@Override
	public int addThrottle(ThrottleRule throttleRule) {
		ValidateArgument.required(throttleRule, "throttleRule");
		
		return jdbcTemplate.update(SQL_ADD_THROTTLE, throttleRule.getId(), throttleRule.getNormalizedPath(), throttleRule.getMaxCallsPerPeriod(), throttleRule.getPeriod());
	}
	
	@Override
	public void clearAllThrottles() {
		jdbcTemplate.execute(SQL_TRUNCATE);
	}

}
