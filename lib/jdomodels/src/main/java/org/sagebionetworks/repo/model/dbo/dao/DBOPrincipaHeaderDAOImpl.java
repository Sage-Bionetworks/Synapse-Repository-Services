package org.sagebionetworks.repo.model.dbo.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.language.Soundex;
import org.apache.commons.lang.StringUtils;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.PrincipalHeaderDAO;
import org.sagebionetworks.repo.model.PrincipalType;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOPrincipalHeader;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import org.sagebionetworks.repo.transactions.WriteTransaction;

import com.google.common.collect.Sets;

public class DBOPrincipaHeaderDAOImpl implements PrincipalHeaderDAO {

	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;

	@Autowired
	private DBOBasicDao basicDAO;
	
	private static final String PRINCIPAL_ID_PARAM_NAME = "principalId";
	private static final String PRINCIPAL_NAME_PARAM_NAME = "principalName";
	private static final String PRINCIPAL_TYPE_PARAM_NAME = "principalType";
	private static final String DOMAIN_TYPE_PARAM_NAME = "domainType";
	private static final String LIMIT_PARAM_NAME = "limit";
	private static final String OFFSET_PARAM_NAME = "offset";
	
	private static final String DELETE_BY_ID = 
			"DELETE FROM " + SqlConstants.TABLE_PRINCIPAL_HEADER + 
			" WHERE " + SqlConstants.COL_PRINCIPAL_HEADER_ID + "=:" + PRINCIPAL_ID_PARAM_NAME;
	
	private static final String SELECT_DISTINCT_IDS = 
			"SELECT DISTINCT(" + SqlConstants.COL_PRINCIPAL_HEADER_ID + ") ";
	
	private static final String SELECT_COUNT_OF_DISTINCT_IDS = 
			"SELECT COUNT(DISTINCT(" + SqlConstants.COL_PRINCIPAL_HEADER_ID + ")) ";
	
	private static final String QUERY_ON_PRINCIPAL_HEADERS_CORE = 
			"FROM " + SqlConstants.TABLE_PRINCIPAL_HEADER + 
			" WHERE " + SqlConstants.COL_PRINCIPAL_HEADER_PRINCIPAL_TYPE + " IN (:" + PRINCIPAL_TYPE_PARAM_NAME + ")" + 
			" AND " + SqlConstants.COL_PRINCIPAL_HEADER_DOMAIN_TYPE + " IN (:" + DOMAIN_TYPE_PARAM_NAME + ")";
	
	private static final String EXACT_MATCH_CONDITION = 
			" AND " + SqlConstants.COL_PRINCIPAL_HEADER_FRAGMENT + "=:" + PRINCIPAL_NAME_PARAM_NAME;
	
	private static final String PREFIX_MATCH_CONDITION = 
			" AND " + SqlConstants.COL_PRINCIPAL_HEADER_FRAGMENT + " LIKE :" + PRINCIPAL_NAME_PARAM_NAME;
	
	/**
	 * An exact match is used here because the Soundex algorithm always outputs
	 * a 4 letter code consisting of a letter followed by 3 numbers
	 */
	private static final String SOUNDEX_MATCH_CONDITION = 
			" AND " + SqlConstants.COL_PRINCIPAL_HEADER_SOUNDEX + "=:" + PRINCIPAL_NAME_PARAM_NAME;
	
	private static final String PAGINATION_PARAMS = 
			" LIMIT :" + LIMIT_PARAM_NAME + " OFFSET :" + OFFSET_PARAM_NAME;
	
	private static final String QUERY_FOR_EXACT_MATCH = 
			SELECT_DISTINCT_IDS + QUERY_ON_PRINCIPAL_HEADERS_CORE + EXACT_MATCH_CONDITION + PAGINATION_PARAMS;
	
	private static final String QUERY_FOR_PREFIX_MATCH = 
			SELECT_DISTINCT_IDS + QUERY_ON_PRINCIPAL_HEADERS_CORE + PREFIX_MATCH_CONDITION + PAGINATION_PARAMS;
	
	private static final String QUERY_FOR_SOUNDEX_MATCH = 
			SELECT_DISTINCT_IDS + QUERY_ON_PRINCIPAL_HEADERS_CORE + SOUNDEX_MATCH_CONDITION + PAGINATION_PARAMS;
	
	private static final String COUNT_QUERY_FOR_EXACT_MATCH = 
			SELECT_COUNT_OF_DISTINCT_IDS + QUERY_ON_PRINCIPAL_HEADERS_CORE + EXACT_MATCH_CONDITION;
	
	private static final String COUNT_QUERY_FOR_PREFIX_MATCH = 
			SELECT_COUNT_OF_DISTINCT_IDS + QUERY_ON_PRINCIPAL_HEADERS_CORE + PREFIX_MATCH_CONDITION;
	
	private static final String COUNT_QUERY_FOR_SOUNDEX_MATCH = 
			SELECT_COUNT_OF_DISTINCT_IDS + QUERY_ON_PRINCIPAL_HEADERS_CORE + SOUNDEX_MATCH_CONDITION;

	private RowMapper<Long> rowMapper = new RowMapper<Long>() {

		@Override
		public Long mapRow(ResultSet rs, int rowNum)
				throws SQLException {
			return rs.getLong(SqlConstants.COL_PRINCIPAL_HEADER_ID);
		}
		
	};
			
	@Override
	@WriteTransaction
	public void insertNew(long principalId, Set<String> fragments, PrincipalType pType, DomainType dType) {
		if (fragments == null || fragments.size() <= 0) {
			return;
		}
		
		List<DBOPrincipalHeader> dbos = new ArrayList<DBOPrincipalHeader>();
		for (String fragment : fragments) {
			DBOPrincipalHeader dbo = new DBOPrincipalHeader();
			dbo.setPrincipalId(principalId);
			
			//TODO Should preprocessing be done here or in the manager layer?
			dbo.setFragment(fragment);
			dbo.setSoundex(new Soundex().encode(fragment));
			
			dbo.setPrincipalType(pType);
			dbo.setDomainType(dType);
			
			dbos.add(dbo);
		}
		
		basicDAO.createBatch(dbos);
	}

	@Override
	@WriteTransaction
	public long delete(long principalId) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(PRINCIPAL_ID_PARAM_NAME, principalId);
		return simpleJdbcTemplate.update(DELETE_BY_ID, params);
	}

	@Override
	public List<Long> query(String nameFilter, MATCH_TYPE mType,
			Set<PrincipalType> principals, Set<DomainType> domains, long limit,
			long offset) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(PRINCIPAL_TYPE_PARAM_NAME, convertEnum(principals, PrincipalType.class));
		params.addValue(DOMAIN_TYPE_PARAM_NAME, convertEnum(domains, DomainType.class));
		params.addValue(LIMIT_PARAM_NAME, limit);
		params.addValue(OFFSET_PARAM_NAME, offset);
		
		String sql;
		switch (mType) {
		case EXACT:
			sql = QUERY_FOR_EXACT_MATCH;
			break;
		case PREFIX:
			sql = QUERY_FOR_PREFIX_MATCH;
			nameFilter = preprocessFragment(nameFilter) + "%";
			break;
		case SOUNDEX:
			sql = QUERY_FOR_SOUNDEX_MATCH;
			nameFilter = new Soundex().encode(nameFilter);
			break;
		default:
			throw new IllegalArgumentException("Unknown match type: " + mType); 
		}
		params.addValue(PRINCIPAL_NAME_PARAM_NAME, nameFilter);
		
		return simpleJdbcTemplate.query(sql, rowMapper, params);
	}
	
	@Override
	public long countQueryResults(String nameFilter, MATCH_TYPE mType,
			Set<PrincipalType> principals, Set<DomainType> domains) {
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue(PRINCIPAL_TYPE_PARAM_NAME, convertEnum(principals, PrincipalType.class));
		params.addValue(DOMAIN_TYPE_PARAM_NAME, convertEnum(domains, DomainType.class));

		String sql;
		switch (mType) {
		case EXACT:
			sql = COUNT_QUERY_FOR_EXACT_MATCH;
			break;
		case PREFIX:
			sql = COUNT_QUERY_FOR_PREFIX_MATCH;
			nameFilter = preprocessFragment(nameFilter) + "%";
			break;
		case SOUNDEX:
			sql = COUNT_QUERY_FOR_SOUNDEX_MATCH;
			nameFilter = new Soundex().encode(nameFilter);
			break;
		default:
			throw new IllegalArgumentException("Unknown match type: " + mType); 
		}
		params.addValue(PRINCIPAL_NAME_PARAM_NAME, nameFilter);
		
		return simpleJdbcTemplate.queryForLong(sql, params);
	}
	
	/**
	 * Takes an enum set and returns a JDBC-friendly set of strings
	 * If the enum set is null or empty, the returned set will contain all values defined by the enum
	 */
	protected static <T extends Enum<T>> Set<String> convertEnum(Set<T> types, Class<T> clazz) {
		if (types == null || types.size() == 0) {
			types = Sets.newHashSet(clazz.getEnumConstants());
		}
		
		Set<String> jdbcFriendly = new HashSet<String>();
		for (Enum<T> type : types) {
			jdbcFriendly.add(type.name());
		}
		return jdbcFriendly;
	}
	
	//TODO Replace this method with the same preprocessing that is done to aliases
	/**
	 * Escapes the two special characters ('_' and '%') of a SQL "LIKE" operation
	 * Also transforms null into empty string ("")
	 */
	@Deprecated
	protected static String preprocessFragment(String str) {
		if (str == null) {
			return "";
		}
		
		str = StringUtils.replace(str, "_", "\\_");
		return StringUtils.replace(str, "%", "\\%");
	}
}
