package org.sagebionetworks.repo.model.dbo.dao.throttle;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.quartz.core.SampledStatistics;
import org.sagebionetworks.repo.model.dao.throttle.ThrottleRulesDAO;
import org.sagebionetworks.repo.model.throttle.ThrottleRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.sun.tools.javac.util.BasicDiagnosticFormatter;


public class DBOThrottleRulesDAOImpl implements ThrottleRulesDAO {
	private static final RowMapper<ThrottleRule> THROTTLE_RULE_ROW_MAPPER = new RowMapper<ThrottleRule>() {

		@Override
		public ThrottleRule mapRow(ResultSet rs, int rowNum) throws SQLException {
			ThrottleRule throttleRule = new ThrottleRule();
			return throttleRule;
		}
	};
	
	//parameters used in namedParametersJdbcTemplate
	private static final String ID_PARAM = ":id";
	private static final String URI_PARAM = ":uri";
	private static final String MAX_CALLS_PARAM = ":maxCalls";
	private static final String CALL_PERIOD_PARAM = ":callPeriod";
	
	//TODO: maybe add sql that fetches only ones updated after certain timestamp
	private static final String SQL_GET_THROTTLES = "SELECT * FROM " + TABLE_THROTTLE_RULES;
	private static final String SQL_ADD_THROTTLE = "INSERT INTO " + TABLE_THROTTLE_RULES +
		" (" +COL_THROTTLE_RULES_ID + ", " + COL_THROTTLE_RULES_NORMALIZED_URI + ", " + COL_THROTTLE_RULES_MAX_CALLS + ", " + COL_THROTTLE_RULES_CALL_PERIOD 
		+ ") VALUES (" + ID_PARAM + ", " + URI_PARAM + ", " + MAX_CALLS_PARAM + ", " + CALL_PERIOD_PARAM +")";
	
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	
	@Override
	public List<ThrottleRule> getThrottles() {
	}

	@Override
	public void addThrottle(long id, String normalizedUri, long maxCalls, long callPeriodSec) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteThrottle(long id) {
		// TODO Auto-generated method stub

	}

	@Override
	public void clearAllThrottles() {
		// TODO Auto-generated method stub

	}

}
