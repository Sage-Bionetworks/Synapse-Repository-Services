package org.sagebionetworks.repo.model.dbo.principal;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.util.List;

import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sagebionetworks.repo.transactions.WriteTransaction;

public class PrincipalPrefixDAOImpl implements PrincipalPrefixDAO {

	private static final String SQL_LIST_TEAM_MEMBERS_FOR_PREFIX = "SELECT DISTINCT P."
			+ COL_PRINCIPAL_PREFIX_PRINCIPAL_ID
			+ " FROM "
			+ TABLE_PRINCIPAL_PREFIX
			+ " P, "
			+ TABLE_GROUP_MEMBERS
			+ " M WHERE P."
			+ COL_PRINCIPAL_PREFIX_PRINCIPAL_ID
			+ " = M."
			+ COL_GROUP_MEMBERS_MEMBER_ID
			+ " AND M."
			+ COL_GROUP_MEMBERS_GROUP_ID
			+ " = ? AND P."
			+ COL_PRINCIPAL_PREFIX_TOKEN + " LIKE ? LIMIT ? OFFSET ?";
	
	private static final String SQL_COUNT_TEAM_MEMBERS_FOR_PREFIX = "SELECT COUNT( DISTINCT P."
			+ COL_PRINCIPAL_PREFIX_PRINCIPAL_ID
			+ ") FROM "
			+ TABLE_PRINCIPAL_PREFIX
			+ " P, "
			+ TABLE_GROUP_MEMBERS
			+ " M WHERE P."
			+ COL_PRINCIPAL_PREFIX_PRINCIPAL_ID
			+ " = M."
			+ COL_GROUP_MEMBERS_MEMBER_ID
			+ " AND M."
			+ COL_GROUP_MEMBERS_GROUP_ID
			+ " = ? AND P."
			+ COL_PRINCIPAL_PREFIX_TOKEN + " LIKE ?";

	private static final String SQL_LIST_PRINCIPALS_FOR_PREFIX = "SELECT DISTINCT "
			+ COL_PRINCIPAL_PREFIX_PRINCIPAL_ID
			+ " FROM "
			+ TABLE_PRINCIPAL_PREFIX
			+ " WHERE "
			+ COL_PRINCIPAL_PREFIX_TOKEN
			+ " LIKE ? LIMIT ? OFFSET ?";

	private static final String SQL_LIST_PRINCIPALS_FOR_PREFIX_BY_TYPE =
			"SELECT DISTINCT P."+COL_PRINCIPAL_PREFIX_PRINCIPAL_ID
			+ " FROM "
			+ TABLE_PRINCIPAL_PREFIX+" P JOIN "+TABLE_USER_GROUP+" U "
			+ "ON (P."+COL_PRINCIPAL_PREFIX_PRINCIPAL_ID+" = U."+COL_USER_GROUP_ID
					+" AND U."+COL_USER_GROUP_IS_INDIVIDUAL+" = ?)"
			+ " WHERE"
			+ " P.TOKEN LIKE ? LIMIT ? OFFSET ?";

	private static final String SQL_LIST_TEAMS_FOR_PREFIX =
			"SELECT DISTINCT P." + COL_PRINCIPAL_PREFIX_PRINCIPAL_ID
					+ " FROM "
					+ TABLE_PRINCIPAL_PREFIX + " P JOIN " + TABLE_USER_GROUP + " U ON P."
					+ COL_PRINCIPAL_PREFIX_PRINCIPAL_ID + " = U." + COL_USER_GROUP_ID
					+ " JOIN " + TABLE_TEAM + " T ON U." + COL_USER_GROUP_ID + " = T."
					+ COL_TEAM_ID + " WHERE P." + COL_PRINCIPAL_PREFIX_TOKEN
					+ " LIKE ? LIMIT ? OFFSET ?";


	private static final String SQL_CLEAR_PRINCIPAL = "DELETE FROM "
			+ TABLE_PRINCIPAL_PREFIX + " WHERE "
			+ COL_PRINCIPAL_PREFIX_PRINCIPAL_ID + " = ?";

	private static final String WILDCARD = "%";

	private static final String SQL_TRUNCATE_TABLE = "TRUNCATE TABLE "
			+ TABLE_PRINCIPAL_PREFIX;

	private static final String SQL_INSERT_WITH_DUPLICATE_IGNORE = "INSERT IGNORE INTO "
			+ TABLE_PRINCIPAL_PREFIX
			+ " ("
			+ COL_PRINCIPAL_PREFIX_TOKEN
			+ ","
			+ COL_PRINCIPAL_PREFIX_PRINCIPAL_ID + ") VALUES (?,?)";

	private static final String EMPTY = "";

	private static final String REG_EX_NON_ALPHA_NUMERIC = "[^a-z0-9]";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DBOBasicDao basicDAO;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.model.dbo.principal.PrincipalPrefixDAO#
	 * addPrincipalAlias(java.lang.String, java.lang.Long)
	 */
	@WriteTransaction
	@Override
	public void addPrincipalAlias(String alias, Long principalId) {
		String processed = preProcessToken(alias);
		if (EMPTY.equals(processed)) {
			return;
		}
		insertIgnoreDuplicate(principalId, processed);
	}

	/**
	 * Insert a single token with duplicates ignored.
	 * 
	 * @param principalId
	 * @param processed
	 */
	private void insertIgnoreDuplicate(Long principalId, String token) {
		jdbcTemplate.update(SQL_INSERT_WITH_DUPLICATE_IGNORE, token,
				principalId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.model.dbo.principal.PrincipalPrefixDAO#
	 * addPrincipalName(java.lang.String, java.lang.String, java.lang.Long)
	 */
	@WriteTransaction
	@Override
	public void addPrincipalName(String firstName, String lastName,
			Long principalId) {

		// Concatenate first-last
		String firstLast = preProcessToken(firstName + lastName);
		if (!EMPTY.equals(firstLast)) {
			insertIgnoreDuplicate(principalId, firstLast);
		}

		// Concatenate last-first
		String lastFirst = preProcessToken(lastName + firstName);
		if (!EMPTY.equals(lastFirst)) {
			insertIgnoreDuplicate(principalId, lastFirst);
		}
	}

	/**
	 * Pre-process an input token. This includes trim(), toLower() and removal
	 * of all non-alpha-numerics.
	 * 
	 * @param input
	 * @return The processed token. Will be an empty string for null inputs or
	 *         inputs that contain no alpha-numerics.
	 */
	public static String preProcessToken(String input) {
		if (input == null) {
			return EMPTY;
		}
		input = input.trim().toLowerCase();
		return input.replaceAll(REG_EX_NON_ALPHA_NUMERIC, EMPTY);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.model.dbo.principal.PrincipalPrefixDAO#
	 * clearPrincipal(java.lang.Long)
	 */
	@Override
	public void clearPrincipal(Long principalId) {
		jdbcTemplate.update(SQL_CLEAR_PRINCIPAL, principalId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.model.dbo.principal.PrincipalPrefixDAO#
	 * listPrincipalsForPrefix(java.lang.String, java.lang.Long, java.lang.Long)
	 */
	@Override
	public List<Long> listPrincipalsForPrefix(String prefix, Long limit,
			Long offset) {
		String processed = preProcessToken(prefix);
		return jdbcTemplate.queryForList(SQL_LIST_PRINCIPALS_FOR_PREFIX,
				Long.class, processed + WILDCARD, limit, offset);
	}

	/**
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.dbo.principal.PrincipalPrefixDAO#listTeamsForPrefix(java.lang.String, java.lang.Long, java.lang.Long)
	 */
	@Override
	public List<Long> listTeamsForPrefix(String prefix, Long limit, Long offset) {
		String processed = preProcessToken(prefix);
		return jdbcTemplate.queryForList(SQL_LIST_TEAMS_FOR_PREFIX,
				Long.class, processed + WILDCARD, limit, offset);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.dbo.principal.PrincipalPrefixDAO#listPrincipalsForPrefix(java.lang.String, boolean, java.lang.Long, java.lang.Long)
	 */
	@Override
	public List<Long> listPrincipalsForPrefix(String prefix,
			boolean isIndividual, Long limit, Long offset) {
		String processed = preProcessToken(prefix);
		return jdbcTemplate.queryForList(SQL_LIST_PRINCIPALS_FOR_PREFIX_BY_TYPE,
				Long.class, isIndividual, processed + WILDCARD, limit, offset);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.model.dbo.principal.PrincipalPrefixDAO#
	 * listTeamMembersForPrefix(java.lang.String, java.lang.Long,
	 * java.lang.Long, java.lang.Long)
	 */
	@Override
	public List<Long> listTeamMembersForPrefix(String prefix, Long teamId,
			Long limit, Long offset) {
		String processed = preProcessToken(prefix);
		return jdbcTemplate.queryForList(SQL_LIST_TEAM_MEMBERS_FOR_PREFIX,
				Long.class, teamId, processed + WILDCARD, limit, offset);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.model.dbo.principal.PrincipalPrefixDAO#
	 * countTeamMembersForPrefix(java.lang.String, java.lang.Long)
	 */
	@Override
	public Long countTeamMembersForPrefix(String prefix, Long teamId) {
		String processed = preProcessToken(prefix);
		return jdbcTemplate.queryForObject(SQL_COUNT_TEAM_MEMBERS_FOR_PREFIX, Long.class, teamId, processed + WILDCARD);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.model.dbo.principal.PrincipalPrefixDAO#truncateTable
	 * ()
	 */
	@Override
	public void truncateTable() {
		jdbcTemplate.update(SQL_TRUNCATE_TABLE);
	}

}
