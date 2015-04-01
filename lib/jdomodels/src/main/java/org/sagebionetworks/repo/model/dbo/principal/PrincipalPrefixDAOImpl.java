package org.sagebionetworks.repo.model.dbo.principal;

import java.util.LinkedList;
import java.util.List;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOPrincipalPrefix;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class PrincipalPrefixDAOImpl implements PrincipalPrefixDAO {
	
	private static final String SQL_CLEAR_PRINCIPAL = "DELETE FROM "+TABLE_PRINCIPAL_PREFIX+" WHERE "+COL_PRINCIPAL_PREFIX_PRINCIPAL_ID+" = ?";

	private static final String WILDCARD = "%";

	private static final String SQL_COUNT_DISTINCT_PREFIX = "SELECT COUNT(DISTINCT "+COL_PRINCIPAL_PREFIX_PRINCIPAL_ID+") FROM "+TABLE_PRINCIPAL_PREFIX+" WHERE "+COL_PRINCIPAL_PREFIX_TOKEN+" LIKE ?";

	private static final String SQL_TRUNCATE_TABLE = "TRUNCATE TABLE "+TABLE_PRINCIPAL_PREFIX;

	private static final String SQL_INSERT_WITH_DUPLICATE_IGNORE = "INSERT IGNORE INTO "+TABLE_PRINCIPAL_PREFIX+" ("+COL_PRINCIPAL_PREFIX_TOKEN+","+COL_PRINCIPAL_PREFIX_PRINCIPAL_ID+") VALUES (?,?)";

	private static final String EMPTY = "";

	private static final String REG_EX_NON_ALPHA_NUMERIC = "[^a-z0-9]";

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private DBOBasicDao basicDAO;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean addPrincipalAlias(String alias, Long principalId) {
		String processed = preProcessToken(alias);
		if(EMPTY.equals(processed)){
			return false;
		}
		insertIgnore(principalId, processed);
		return true;
	}

	private void insertIgnore(Long principalId, String processed) {
		jdbcTemplate.update(SQL_INSERT_WITH_DUPLICATE_IGNORE, processed, principalId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean addPrincipalName(String firstName, String lastName,
			Long principalId) {

		List<DBOPrincipalPrefix> list = new LinkedList<DBOPrincipalPrefix>();
		// frist-last
		String firstLast = preProcessToken(firstName+lastName);
		if(!EMPTY.equals(firstLast)){
			insertIgnore(principalId, firstLast);
		}

		// last-first
		String lastFrist = preProcessToken(lastName+firstName);
		if(!EMPTY.equals(lastFrist)){
			insertIgnore(principalId, lastFrist);
		}
		return true;
	}
	
	/**
	 * Pre-process an input token.
	 * @param input
	 * @return
	 */
	public static String preProcessToken(String input){
		if(input == null){
			return EMPTY;
		}
		input = input.trim().toLowerCase();
		return input.replaceAll(REG_EX_NON_ALPHA_NUMERIC, EMPTY);
	}

	@Override
	public void clearPrincipal(Long principalId) {
		jdbcTemplate.update(SQL_CLEAR_PRINCIPAL, principalId);
	}

	@Override
	public List<Long> listUsersForPrefix(String prefix, Long limit, Long offset) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long countUsersForPrefix(String prefix) {
		String prcessed = preProcessToken(prefix);
		return jdbcTemplate.queryForObject(SQL_COUNT_DISTINCT_PREFIX, Long.class, prcessed+WILDCARD);
	}

	@Override
	public List<Long> listTeamMembersForPrefix(String prefix, Long teamId,
			Long limit, Long offset) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long countTeamMembersForPrefix(String prefix, Long teamId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void truncateTable() {
		jdbcTemplate.update(SQL_TRUNCATE_TABLE);
	}

}
