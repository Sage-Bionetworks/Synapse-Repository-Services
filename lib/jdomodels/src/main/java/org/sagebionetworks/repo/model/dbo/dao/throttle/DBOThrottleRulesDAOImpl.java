package org.sagebionetworks.repo.model.dbo.dao.throttle;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.util.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.sagebionetworks.repo.model.dao.throttle.ThrottleRulesDAO;
import org.sagebionetworks.repo.model.throttle.ThrottleRule;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class DBOThrottleRulesDAOImpl implements ThrottleRulesDAO {
	private static final RowMapper<ThrottleRule> THROTTLE_RULE_ROW_MAPPER = new RowMapper<ThrottleRule>() {

		@Override
		public ThrottleRule mapRow(ResultSet rs, int rowNum) throws SQLException {
			ThrottleRule throttleRule = new ThrottleRule();
			throttleRule.setId(rs.getLong(COL_THROTTLE_RULES_ID));
			throttleRule.setNormalizedUri(rs.getString(COL_THROTTLE_RULES_NORMALIZED_URI));
			throttleRule.setMaxCalls(rs.getLong(COL_THROTTLE_RULES_MAX_CALLS));
			throttleRule.setCallPeriodSec(rs.getLong(COL_THROTTLE_RULES_CALL_PERIOD));
			throttleRule.setModifiedOn(rs.getDate(COL_THROTTLE_RULES_MODIFIED_ON));
			return throttleRule;
		}
	};
	
	//parameters used in namedParametersJdbcTemplate
	private static final String ID_PARAM = "id";
	private static final String URI_PARAM = "uri";
	private static final String MAX_CALLS_PARAM = "maxCalls";
	private static final String CALL_PERIOD_PARAM = "callPeriod";
	private static final String MODIFIED_ON_PARAM = "modifiedOn";
	
	//TODO: maybe add sql that fetches only ones updated after certain timestamp
	private static final String SQL_GET_THROTTLES = "SELECT * FROM " + TABLE_THROTTLE_RULES;
	private static final String SQL_GET_THROTTLES_AFTER  = SQL_GET_THROTTLES + " WHERE " + COL_THROTTLE_RULES_MODIFIED_ON + " >= :" + MODIFIED_ON_PARAM;
	private static final String SQL_ADD_THROTTLE = "INSERT INTO " + TABLE_THROTTLE_RULES +
		" (" +COL_THROTTLE_RULES_ID + ", " + COL_THROTTLE_RULES_NORMALIZED_URI + ", " + COL_THROTTLE_RULES_MAX_CALLS + ", " + COL_THROTTLE_RULES_CALL_PERIOD 
		+ ") VALUES (:" + ID_PARAM + ", :" + URI_PARAM + ", :" + MAX_CALLS_PARAM + ", :" + CALL_PERIOD_PARAM +")";
	private static final String SQL_TRUNCATE = "TRUNCATE TABLE " + TABLE_THROTTLE_RULES;
	
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Override
	public List<ThrottleRule> getAllThrottles() {
		return namedParameterJdbcTemplate.query(SQL_GET_THROTTLES, THROTTLE_RULE_ROW_MAPPER);
	}
	
	@Override
	public List<ThrottleRule> getAllThrottlesAfter(Date time){
		ValidateArgument.required(time, "time");
		MapSqlParameterSource paramMap = new MapSqlParameterSource(MODIFIED_ON_PARAM, new Timestamp(time.getTime()));
		return namedParameterJdbcTemplate.query(SQL_GET_THROTTLES_AFTER, paramMap, THROTTLE_RULE_ROW_MAPPER);
	}
	
	@WriteTransactionReadCommitted
	@Override
	public int addThrottle(long id, String normalizedUri, long maxCalls, long callPeriodSec) {
		ValidateArgument.requirement(id >= 0, "id must not be a negative number");
		ValidateArgument.required(normalizedUri, "normalizedUri");
		ValidateArgument.requirement(maxCalls >= 0, "maxCalls must not be a negative number");
		ValidateArgument.requirement(callPeriodSec >= 0, "callPeriodSec must not be a negative number");
		
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue(ID_PARAM, id);
		paramMap.addValue(URI_PARAM, normalizedUri);
		paramMap.addValue(MAX_CALLS_PARAM, maxCalls);
		paramMap.addValue(CALL_PERIOD_PARAM, callPeriodSec);
		
		return namedParameterJdbcTemplate.update(SQL_ADD_THROTTLE, paramMap);
	}
	
	@Override
	public void clearAllThrottles() {
		jdbcTemplate.execute(SQL_TRUNCATE);
	}

}
