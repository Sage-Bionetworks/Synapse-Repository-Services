package org.sagebionetworks.repo.model.dbo.principal;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_DISPLAY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_UNIQUE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_FIRST_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_LAST_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PRINCIPAL_ALIAS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_PROFILE;

import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.principal.AliasEnum;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.BootstrapAlias;
import org.sagebionetworks.repo.model.principal.BootstrapGroup;
import org.sagebionetworks.repo.model.principal.BootstrapPrincipal;
import org.sagebionetworks.repo.model.principal.BootstrapUser;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.google.common.collect.Lists;

/**
 * Basic database implementation of of PrincipalAliasDAO
 * @author John
 *
 */
public class PrincipalAliasDaoImpl implements PrincipalAliasDAO {

	private static final String PARAM_PRINCIPAL_ALIAS = "principalAliasParam";
	private static final String PARAM_PRINCIPAL_ALIAS_TYPES = "aliasTypesParam";

	private static final String SQL_LOCK_PRINCIPAL = "SELECT "+COL_USER_GROUP_ID+" FROM "+TABLE_USER_GROUP+" WHERE "+COL_USER_GROUP_ID+" = ? FOR UPDATE";
	private static final String SQL_DELETE_ALL_ALIASES_FOR_PRINCIPAL = "DELETE FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_PRINCIPAL_ID+" = ?";
	private static final String SQL_DELETE_ALIAS_BY_PRINCIPAL_AND_ALIAS_ID = "DELETE FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_PRINCIPAL_ID+" = ? AND  "+COL_PRINCIPAL_ALIAS_ID+" = ?";
	private static final String SQL_LIST_ALIASES_BY_ID = "SELECT * FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_PRINCIPAL_ID+" = ? ORDER BY "+COL_PRINCIPAL_ALIAS_ID;
	private static final String SQL_LIST_ALIASES_BY_ID_AND_TYPE = "SELECT * FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_PRINCIPAL_ID+" = :" + PARAM_PRINCIPAL_ALIAS + " AND "+COL_PRINCIPAL_ALIAS_TYPE+" IN (:" + PARAM_PRINCIPAL_ALIAS_TYPES + ") ORDER BY "+COL_PRINCIPAL_ALIAS_ID;
	
	private static final String SQL_LIST_ALIASES_BY_ID_TYPE_AND_DISPLAY = "SELECT * FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_PRINCIPAL_ID+" = ? AND "+COL_PRINCIPAL_ALIAS_TYPE+" = ? AND "+ COL_PRINCIPAL_ALIAS_DISPLAY +" = ? ORDER BY "+COL_PRINCIPAL_ALIAS_ID;
	private static final String SQL_LIST_ALIASES_BY_TYPE = "SELECT * FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_TYPE+" = ? ORDER BY "+COL_PRINCIPAL_ALIAS_ID;
	private static final String SQL_GET_ALIAS = "SELECT * FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_ID+" = ?";
	private static final String SET_ALIAS_TYPES = "aliasTypeSet";
	private static final String ALIAS_UNIQUE = "aliasUnique";
	private static final String SQL_FIND_PRINCIPAL_WITH_ALIAS = "SELECT * FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_UNIQUE+" = :" + ALIAS_UNIQUE;
	private static final String SQL_FIND_PRINCIPAL_WITH_ALIAS_AND_FILTER_BY_TYPES = SQL_FIND_PRINCIPAL_WITH_ALIAS + " AND `" + COL_PRINCIPAL_ALIAS_TYPE + "` IN (:"+SET_ALIAS_TYPES+")";
	private static final String SQL_IS_ALIAS_AVAILABLE = "SELECT COUNT(*) FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_UNIQUE+" = ?";
	private static final String SET_BIND_VAR = "principalIdSet";
	private static final String SQL_LIST_ALIASES_FROM_SET_OF_PRINCIPAL_IDS = "SELECT * FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_PRINCIPAL_ID+" IN (:"+SET_BIND_VAR+") ORDER BY "+COL_PRINCIPAL_ALIAS_PRINCIPAL_ID;

	private static final String SQL_SELECT_USER_GROUP_HEADERS =
			"SELECT "
			+ "A."+COL_PRINCIPAL_ALIAS_PRINCIPAL_ID
			+", A."+COL_PRINCIPAL_ALIAS_TYPE
			+", A."+ COL_PRINCIPAL_ALIAS_DISPLAY
			+", P."+COL_USER_PROFILE_FIRST_NAME
			+", P."+COL_USER_PROFILE_LAST_NAME
			+ " FROM "
			+ TABLE_PRINCIPAL_ALIAS+" A LEFT OUTER JOIN "+TABLE_USER_PROFILE+" P"
			+ " ON (A."+COL_PRINCIPAL_ALIAS_PRINCIPAL_ID+" = P."+COL_USER_PROFILE_ID+")"
			+ " WHERE "
			+ "A."+COL_PRINCIPAL_ALIAS_TYPE+" IN ('"+AliasEnum.USER_NAME+"', '"+AliasEnum.TEAM_NAME+"') "
			+ "AND A.PRINCIPAL_ID IN (:principalIds)";
	
	private static final String TYPE_LIST = "typeList";
	private static final String UNIQUE_ALIAS_LIST = "uniqueAliasList";
	
	private static final String SQL_SELECT_BY_ALIASES_AND_TYPE = 
			"SELECT "+COL_PRINCIPAL_ALIAS_PRINCIPAL_ID
			+" FROM "+TABLE_PRINCIPAL_ALIAS
			+" WHERE "+COL_PRINCIPAL_ALIAS_UNIQUE+" IN (:"+UNIQUE_ALIAS_LIST+")"
					+ " AND "+COL_PRINCIPAL_ALIAS_TYPE+" IN (:"+TYPE_LIST+")";

	private static final String SQL_ALIAS_IS_BOUND_TO_PRINCIPAL =
			"SELECT COUNT(*) " +
			"FROM " + TABLE_PRINCIPAL_ALIAS + " " +
			"WHERE " + COL_PRINCIPAL_ALIAS_UNIQUE + " = :" + COL_PRINCIPAL_ALIAS_UNIQUE + " " +
			"AND " + COL_PRINCIPAL_ALIAS_PRINCIPAL_ID + " = :" + COL_PRINCIPAL_ALIAS_PRINCIPAL_ID;


	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private NamedParameterJdbcTemplate namedTemplate;
	
	private static RowMapper<DBOPrincipalAlias> principalAliasMapper = new DBOPrincipalAlias().getTableMapping();

	@WriteTransaction
	@Override
	public PrincipalAlias bindAliasToPrincipal(PrincipalAlias dto) throws NotFoundException {
		if(dto == null) throw new IllegalArgumentException("PrincipalAlais cannot be null");
		if(dto.getAlias() == null) throw new IllegalArgumentException("Alias cannot be null");
		try {
			// A 'SELECT FOR UPDATE' is used to prevent race conditions.  Some alias types
			// only allow one alias per principal.  Since this logic is enforced at the DAO level
			// and not the database level, the 'SELECT FOR UPDATE' ensures all updates for a given
			// principal are executed serially.
			this.jdbcTemplate.queryForObject(SQL_LOCK_PRINCIPAL, Long.class, dto.getPrincipalId());
		} catch (EmptyResultDataAccessException | NullPointerException e) {
			throw new NotFoundException("A principal does not exist with principalId: "+dto.getPrincipalId());
		}
		// Convert to the DBO
		DBOPrincipalAlias dbo = AliasUtils.createDBOFromDTO(dto);
		// Validate the alias
		dbo.getAliasType().validateAlias(dbo.getAliasDisplay());
		// Does this alias already exist?
		PrincipalAlias current = findPrincipalWithAlias(dto.getAlias());
		if(current != null){
			// Is this ID already assigned to another user?
			if(!current.getPrincipalId().equals(dbo.getPrincipalId())) throw new NameConflictException("The alias: "+dto.getAlias()+" is already in use.");
			if(!current.getType().equals(dto.getType())) throw new IllegalArgumentException("Cannot change the type of an alias: "+dto.getAlias()+" from "+dto.getAlias()+" to "+dbo.getAliasType());
			// Use the ID of the type
			dbo.setId(current.getAliasId());
		}else{
			// this is a new alias
			// If this is an alias type that only allows one alias per principal then first determine if the alias already exists
			// for this type for this principal.
			if(dbo.getAliasType().isOnePerPrincpal()){
				// Note: The 'SELECT FOR UPDATE' at the beginning of this method protects against race conditions of concurrent updates.
				List<PrincipalAlias> allOfTypeType = listPrincipalAliases(dbo.getPrincipalId(), dto.getType());
				if(allOfTypeType.size() > 1){
					// This should never happen
					throw new IllegalStateException("Multiple aliases were found for principal: "+dbo.getPrincipalId()+" of Type: "+dto.getType());
				}
				if(allOfTypeType.size() == 1){
					// We already have an alias of this type for this principal and only one is allowed so update the one that exists
					dbo.setId(allOfTypeType.get(0).getAliasId());
				}else{
					// We do not already have an alias of this type to issue a new a new ID.
					dbo.setId(idGenerator.generateNewId(IdType.PRINCIPAL_ALIAS_ID));
				}
			}else{
				// Multiple aliases are allowed for this type so issue a new ID.
				dbo.setId(idGenerator.generateNewId(IdType.PRINCIPAL_ALIAS_ID));
			}
		}
		dbo.setEtag(UUID.randomUUID().toString());
		
		// Create or update the alias
		basicDao.createOrUpdate(dbo);
		// return the results from the DB.
		return getPrincipalAlias(dbo.getId());
	}

	@Override
	public PrincipalAlias getPrincipalAlias(Long aliasId) throws NotFoundException {
		if(aliasId == null) throw new IllegalArgumentException("Alias cannot be null");
		try {
			DBOPrincipalAlias dbo = jdbcTemplate.queryForObject(SQL_GET_ALIAS, principalAliasMapper, aliasId);
			return AliasUtils.createDTOFromDBO(dbo);
		} catch (EmptyResultDataAccessException e) {
			// no match
			throw new NotFoundException("An Alias with the ID: "+aliasId+" does not exist");
		}
	}

	@Override
	public PrincipalAlias findPrincipalWithAlias(String alias, AliasType... aliasTypes) {
		if(alias == null) throw new IllegalArgumentException("Alias cannot be null");
		String unique = AliasUtils.getUniqueAliasName(alias);
		try {
			String sql = SQL_FIND_PRINCIPAL_WITH_ALIAS;

			Map<String, Object> paramMap = new HashMap<>();
			paramMap.put(ALIAS_UNIQUE, unique);
			if(aliasTypes.length > 0) {
				paramMap.put(SET_ALIAS_TYPES, Arrays.stream(aliasTypes).map(Enum::name).collect(Collectors.toSet()));
				sql = SQL_FIND_PRINCIPAL_WITH_ALIAS_AND_FILTER_BY_TYPES;
			}
			DBOPrincipalAlias dbo = namedTemplate.queryForObject(sql, paramMap, principalAliasMapper);
			return AliasUtils.createDTOFromDBO(dbo);
		} catch (EmptyResultDataAccessException e) {
			// no match
			return null;
		}
	}

	@Override
	public boolean isAliasAvailable(String alias) {
		if(alias == null) throw new IllegalArgumentException("Alias cannot be null");
		String unique = AliasUtils.getUniqueAliasName(alias);
		long count = jdbcTemplate.queryForObject(SQL_IS_ALIAS_AVAILABLE, Long.class, unique);
		return count < 1;
	}

	@Override
	public boolean removeAliasFromPrincipal(Long principalId, Long aliasId) {
		if(principalId == null) throw new IllegalArgumentException("PrincipalId cannot be null");
		if(aliasId == null) throw new IllegalArgumentException("AliasId cannot be null");
		int count = this.jdbcTemplate.update(SQL_DELETE_ALIAS_BY_PRINCIPAL_AND_ALIAS_ID, principalId, aliasId);
		return count > 0;
	}
	
	@Override
	public boolean removeAllAliasFromPrincipal(Long principalId) {
		int count = this.jdbcTemplate.update(SQL_DELETE_ALL_ALIASES_FOR_PRINCIPAL, principalId);
		return count > 0;
	}

	@Override
	public List<PrincipalAlias> listPrincipalAliases(Long principalId) {
		if(principalId == null) throw new IllegalArgumentException("PrincipalId cannot be null");
		List<DBOPrincipalAlias> results = this.jdbcTemplate.query(SQL_LIST_ALIASES_BY_ID, principalAliasMapper, principalId);
		return AliasUtils.createDTOFromDBO(results);
	}

	@Override
	public List<PrincipalAlias> listPrincipalAliases(Long principalId,
			AliasType... types) {

		if(principalId == null) throw new IllegalArgumentException("PrincipalId cannot be null");
		if(types == null || types[0] == null) throw new IllegalArgumentException("AliasType cannot be null");
		Map<String,Object> parameters = new HashMap<String,Object>();
		parameters.put(PARAM_PRINCIPAL_ALIAS, principalId);
		List<String> typesList = new ArrayList<>();
		for (AliasType t : types) {
			typesList.add(t.name());
		}
		parameters.put(PARAM_PRINCIPAL_ALIAS_TYPES, typesList);
		List<DBOPrincipalAlias> results = this.namedTemplate.query(SQL_LIST_ALIASES_BY_ID_AND_TYPE, parameters, principalAliasMapper);
		return AliasUtils.createDTOFromDBO(results);
	}
	
	@Override
	public List<PrincipalAlias> listPrincipalAliases(Long principalId,
			AliasType type, String displayAlias) {
		if(principalId == null) throw new IllegalArgumentException("PrincipalId cannot be null");
		if(type == null) throw new IllegalArgumentException("AliasType cannot be null");
		if(displayAlias == null) throw new IllegalArgumentException("displayAlias cannot be null");
		List<DBOPrincipalAlias> results = this.jdbcTemplate.query(SQL_LIST_ALIASES_BY_ID_TYPE_AND_DISPLAY, principalAliasMapper, principalId, type.name(), displayAlias);
		return AliasUtils.createDTOFromDBO(results);
	}
	
	@Override
	public List<PrincipalAlias> listPrincipalAliases(AliasType type) {
		if(type == null) throw new IllegalArgumentException("AliasType cannot be null");
		List<DBOPrincipalAlias> results = this.jdbcTemplate.query(SQL_LIST_ALIASES_BY_TYPE, principalAliasMapper, type.name());
		return AliasUtils.createDTOFromDBO(results);
	}

	@Override
	public List<PrincipalAlias> listPrincipalAliases(Collection<Long> principalIds) {
		if(principalIds == null) throw new IllegalArgumentException("PrincipalIds cannot be null");
		if(principalIds.isEmpty()) return Collections.emptyList();
		Map<String,Object> parameters = new HashMap<String,Object>();
		parameters.put(SET_BIND_VAR, principalIds);
		List<DBOPrincipalAlias> results = this.namedTemplate.query(SQL_LIST_ALIASES_FROM_SET_OF_PRINCIPAL_IDS, parameters, principalAliasMapper);
		return AliasUtils.createDTOFromDBO(results);
	}
	
	/**
	 * This is called by Spring after all properties are set.
	 */
	@WriteTransaction
	public void bootstrap(){
		// Boot strap all users and groups
		if (this.userGroupDAO.getBootstrapPrincipals() == null) {
			throw new IllegalArgumentException("bootstrapPrincipals users cannot be null");
		}
		
		// For each one determine if it exists, if not create it
		for (BootstrapPrincipal abs: this.userGroupDAO.getBootstrapPrincipals()) {
			if (abs.getId() == null) throw new IllegalArgumentException("Bootstrap users must have an id");
			if (abs instanceof BootstrapUser) {
				// Add the username and email
				BootstrapUser user = (BootstrapUser) abs;
				// email
				bootstrapAlias(user.getEmail(), AliasType.USER_EMAIL, abs.getId());
				// username
				bootstrapAlias(user.getUserName(), AliasType.USER_NAME, abs.getId());
			}else{
				// This is a group
				// Group name
				BootstrapGroup group = (BootstrapGroup) abs;
				bootstrapAlias(group.getGroupAlias(), AliasType.TEAM_NAME, abs.getId());
			}
		}
	}
	
	/**
	 * Private helper to create or update a bootstrap alias.
	 * @param boot
	 * @param type
	 * @param principalId
	 */
	private void bootstrapAlias(BootstrapAlias boot, AliasType type, Long principalId){
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias(boot.getAliasName());
		alias.setAliasId(boot.getAliasId());
		alias.setPrincipalId(principalId);
		alias.setType(type);
		alias.setEtag(UUID.randomUUID().toString());
		DBOPrincipalAlias dbo = AliasUtils.createDBOFromDTO(alias);
		basicDao.createOrUpdate(dbo);
		idGenerator.reserveId(boot.getAliasId(), IdType.PRINCIPAL_ALIAS_ID);
	}

	@Override
	public String getUserName(Long principalId) throws NotFoundException {
		List<PrincipalAlias> aliases = listPrincipalAliases(principalId, AliasType.USER_NAME);
		if (aliases.isEmpty()) throw new NotFoundException("No user name for "+principalId);
		if (aliases.size()>1) throw new IllegalStateException("Expected one user name but found "+aliases.size()+" for "+principalId);
		return aliases.get(0).getAlias();
	}

	@Override
	public String getTeamName(Long principalId) throws NotFoundException {
		List<PrincipalAlias> aliases = listPrincipalAliases(principalId, AliasType.TEAM_NAME);
		if (aliases.isEmpty()) throw new NotFoundException("No team name for "+principalId);
		if (aliases.size()>1) throw new IllegalStateException("Expected one team name but found "+aliases.size()+" for "+principalId);
		return aliases.get(0).getAlias();
	}

	@Override
	public long lookupPrincipalID(String alias, AliasType type) {
		ValidateArgument.required(alias, "alias");
		ValidateArgument.required(type, "type");
		List<Long> queryResult = findPrincipalsWithAliases(Lists.newArrayList(alias), Lists.newArrayList(type));
		if (queryResult.size() != 1) {
			throw new NotFoundException();
		}
		return queryResult.get(0);
	}

	@Override
	public List<UserGroupHeader> listPrincipalHeaders(List<Long> principalIds) {
		ValidateArgument.required(principalIds, "principalIds");
		if (principalIds.isEmpty()) {
			return new LinkedList<>();
		}
		final Map<Long, UserGroupHeader> headerMap = new HashMap<>(principalIds.size());
		MapSqlParameterSource parameters = new MapSqlParameterSource("principalIds", principalIds);
		namedTemplate.query(SQL_SELECT_USER_GROUP_HEADERS, parameters, new RowCallbackHandler() {

			@Override
			public void processRow(ResultSet rs) throws SQLException {
				UserGroupHeader header = new UserGroupHeader();
				Long principalId = rs.getLong(COL_PRINCIPAL_ALIAS_PRINCIPAL_ID);
				header.setOwnerId(""+principalId);
				header.setUserName(rs.getString(COL_PRINCIPAL_ALIAS_DISPLAY));
				AliasEnum type = AliasEnum.valueOf(rs.getString(COL_PRINCIPAL_ALIAS_TYPE));
				if(AliasEnum.USER_NAME == type){
					// user
					header.setIsIndividual(true);
					header.setFirstName(getStringUTF8(rs, COL_USER_PROFILE_FIRST_NAME));
					header.setLastName(getStringUTF8(rs, COL_USER_PROFILE_LAST_NAME));
				}else{
					// team
					header.setIsIndividual(false);
				}
				headerMap.put(principalId, header);
			}
		});
		// return the results in the order called
		List<UserGroupHeader> headers = new LinkedList<>();
		for(Long principalId: principalIds){
			UserGroupHeader header = headerMap.get(principalId);
			if(header != null){
				headers.add(headerMap.get(principalId));
			}
		}
		return headers;
	}
	
	/**
	 * Read a field from the given ResultSet as bytes converted
	 * to a UTF-8 String.
	 * 
	 * @param rs
	 * @param name
	 * @return
	 */
	static String getStringUTF8(ResultSet rs, String name){
		try {
			byte[] bytes = rs.getBytes(name);
			if(bytes == null){
				return null;
			}
			return new String(bytes, "UTF-8");
		} catch (SQLException | UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Long> findPrincipalsWithAliases(Collection<String> aliases,
			List<AliasType> types) {
		ValidateArgument.required(aliases, "aliases");
		ValidateArgument.required(types, "types");
		if(aliases.isEmpty() || types.isEmpty()){
			return new LinkedList<>();
		}
		// Process any alias to remove non-alpha numerics.
		List<String> processAliases = new LinkedList<String>();
		for (String alias : aliases){
			processAliases.add(AliasUtils.getUniqueAliasName(alias));
		}
		List<String> typeStrings = new LinkedList<String>();
		for(AliasType type: types){
			typeStrings.add(type.name());
		}
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue(UNIQUE_ALIAS_LIST, processAliases);
		parameters.addValue(TYPE_LIST, typeStrings);
		return this.namedTemplate.queryForList(SQL_SELECT_BY_ALIASES_AND_TYPE, parameters, Long.class);
	}

	@Override
	public boolean aliasIsBoundToPrincipal(String alias, String principalId) {
		MapSqlParameterSource parameters = new MapSqlParameterSource();
		parameters.addValue(COL_PRINCIPAL_ALIAS_UNIQUE, AliasUtils.getUniqueAliasName(alias));
		parameters.addValue(COL_PRINCIPAL_ALIAS_PRINCIPAL_ID, principalId);
		int count = namedTemplate.queryForObject(SQL_ALIAS_IS_BOUND_TO_PRINCIPAL, parameters, Integer.class);
		if (count == 0) {
			return false;
		} else if (count == 1) {
			return true;
		} else {
			// This should never happen
			throw new DatastoreException("Expected count 0 or 1, but greater than 1 has been found");
		}
	}
}
