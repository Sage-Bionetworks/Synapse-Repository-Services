package org.sagebionetworks.repo.model.dbo.principal;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_BOUND_ALIAS_DISPLAY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_UNIQUE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PRINCIPAL_ALIAS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
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
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * Basic database implementation of of PrincipalAliasDAO
 * @author John
 *
 */
public class PrincipalAliasDaoImpl implements PrincipalAliasDAO {
	
	private static final String SQL_LOCK_PRINCIPAL = "SELECT "+COL_USER_GROUP_ID+" FROM "+TABLE_USER_GROUP+" WHERE "+COL_USER_GROUP_ID+" = ? FOR UPDATE";
	private static final String SQL_DELETE_ALL_ALIASES_FOR_PRINCIPAL = "DELETE FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_PRINCIPAL_ID+" = ?";
	private static final String SQL_DELETE_ALIAS_BY_PRINCIPAL_AND_ALIAS_ID = "DELETE FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_PRINCIPAL_ID+" = ? AND  "+COL_PRINCIPAL_ALIAS_ID+" = ?";
	private static final String SQL_LIST_ALIASES_BY_ID = "SELECT * FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_PRINCIPAL_ID+" = ? ORDER BY "+COL_PRINCIPAL_ALIAS_ID;
	private static final String SQL_LIST_ALIASES_BY_ID_AND_TYPE = "SELECT * FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_PRINCIPAL_ID+" = ? AND "+COL_PRINCIPAL_ALIAS_TYPE+" = ? ORDER BY "+COL_PRINCIPAL_ALIAS_ID;
	
	private static final String SQL_LIST_ALIASES_BY_ID_TYPE_AND_DISPLAY = "SELECT * FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_PRINCIPAL_ID+" = ? AND "+COL_PRINCIPAL_ALIAS_TYPE+" = ? AND "+COL_BOUND_ALIAS_DISPLAY+" = ? ORDER BY "+COL_PRINCIPAL_ALIAS_ID;
	private static final String SQL_LIST_ALIASES_BY_TYPE = "SELECT * FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_TYPE+" = ? ORDER BY "+COL_PRINCIPAL_ALIAS_ID;
	private static final String SQL_GET_ALIAS = "SELECT * FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_ID+" = ?";
	private static final String SQL_FIND_PRINCIPAL_WITH_ALIAS = "SELECT * FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_UNIQUE+" = ?";
	private static final String ALIAS_PARAM = "alias";
	private static final String SQL_FIND_PRINCIPALS_WITH_ALIASES = "SELECT * FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_UNIQUE+" IN (:"+ALIAS_PARAM+")";
	private static final String SQL_IS_ALIAS_AVAILABLE = "SELECT COUNT(*) FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_UNIQUE+" = ?";
	private static final String SET_BIND_VAR = "principalIdSet";
	private static final String SQL_LIST_ALIASES_FROM_SET_OF_PRINCIPAL_IDS = "SELECT * FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_PRINCIPAL_ID+" IN (:"+SET_BIND_VAR+") ORDER BY "+COL_PRINCIPAL_ALIAS_PRINCIPAL_ID;
	private static final String SQL_GET_PRINCIPAL_ID = "SELECT "+COL_PRINCIPAL_ALIAS_PRINCIPAL_ID
			+" FROM "+TABLE_PRINCIPAL_ALIAS
			+" WHERE "+COL_PRINCIPAL_ALIAS_UNIQUE+" = ? "
			+" AND "+COL_PRINCIPAL_ALIAS_TYPE+" = ?";
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private UserGroupDAO userGroupDAO;
	
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
			this.simpleJdbcTemplate.queryForLong(SQL_LOCK_PRINCIPAL, dto.getPrincipalId());
		} catch (EmptyResultDataAccessException e) {
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
					dbo.setId(idGenerator.generateNewId(TYPE.PRINCIPAL_ALIAS_ID));
				}
			}else{
				// Multiple aliases are allowed for this type so issue a new ID.
				dbo.setId(idGenerator.generateNewId(TYPE.PRINCIPAL_ALIAS_ID));
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
			DBOPrincipalAlias dbo = simpleJdbcTemplate.queryForObject(SQL_GET_ALIAS, principalAliasMapper, aliasId);
			return AliasUtils.createDTOFromDBO(dbo);
		} catch (EmptyResultDataAccessException e) {
			// no match
			throw new NotFoundException("An Alias with the ID: "+aliasId+" does not exist");
		}
	}
	
	@Override
	public PrincipalAlias findPrincipalWithAlias(String alias) {
		if(alias == null) throw new IllegalArgumentException("Alias cannot be null");
		String unique = AliasUtils.getUniqueAliasName(alias);
		try {
			DBOPrincipalAlias dbo = simpleJdbcTemplate.queryForObject(SQL_FIND_PRINCIPAL_WITH_ALIAS, principalAliasMapper, unique);
			return AliasUtils.createDTOFromDBO(dbo);
		} catch (EmptyResultDataAccessException e) {
			// no match
			return null;
		}
	}

	@Override
	public Set<PrincipalAlias> findPrincipalsWithAliases(Set<String> aliases) {
		if(aliases == null) throw new IllegalArgumentException("aliases cannot be null");
		Set<PrincipalAlias> result = new HashSet<PrincipalAlias>();
		if (aliases.isEmpty()) return result;
		List<String> unique = new ArrayList<String>();
		for (String alias : aliases) unique.add(AliasUtils.getUniqueAliasName(alias));
		SqlParameterSource param = new MapSqlParameterSource(ALIAS_PARAM, unique);
		for (DBOPrincipalAlias dbo : simpleJdbcTemplate.query(SQL_FIND_PRINCIPALS_WITH_ALIASES, principalAliasMapper, param)) {
			result.add(AliasUtils.createDTOFromDBO(dbo));
		}
		return result;
	}

	@Override
	public boolean isAliasAvailable(String alias) {
		if(alias == null) throw new IllegalArgumentException("Alias cannot be null");
		String unique = AliasUtils.getUniqueAliasName(alias);
		long count = simpleJdbcTemplate.queryForLong(SQL_IS_ALIAS_AVAILABLE, unique);
		return count < 1;
	}

	@Override
	public boolean removeAliasFromPrincipal(Long principalId, Long aliasId) {
		if(principalId == null) throw new IllegalArgumentException("PrincipalId cannot be null");
		if(aliasId == null) throw new IllegalArgumentException("AliasId cannot be null");
		int count = this.simpleJdbcTemplate.update(SQL_DELETE_ALIAS_BY_PRINCIPAL_AND_ALIAS_ID, principalId, aliasId);
		return count > 0;
	}
	
	@Override
	public boolean removeAllAliasFromPrincipal(Long principalId) {
		int count = this.simpleJdbcTemplate.update(SQL_DELETE_ALL_ALIASES_FOR_PRINCIPAL, principalId);
		return count > 0;
	}

	@Override
	public List<PrincipalAlias> listPrincipalAliases(Long principalId) {
		if(principalId == null) throw new IllegalArgumentException("PrincipalId cannot be null");
		List<DBOPrincipalAlias> results = this.simpleJdbcTemplate.query(SQL_LIST_ALIASES_BY_ID, principalAliasMapper, principalId);
		return AliasUtils.createDTOFromDBO(results);
	}

	@Override
	public List<PrincipalAlias> listPrincipalAliases(Long principalId,
			AliasType type) {
		if(principalId == null) throw new IllegalArgumentException("PrincipalId cannot be null");
		if(type == null) throw new IllegalArgumentException("AliasType cannot be null");
		List<DBOPrincipalAlias> results = this.simpleJdbcTemplate.query(SQL_LIST_ALIASES_BY_ID_AND_TYPE, principalAliasMapper, principalId, type.name());
		return AliasUtils.createDTOFromDBO(results);
	}
	
	@Override
	public List<PrincipalAlias> listPrincipalAliases(Long principalId,
			AliasType type, String displayAlias) {
		if(principalId == null) throw new IllegalArgumentException("PrincipalId cannot be null");
		if(type == null) throw new IllegalArgumentException("AliasType cannot be null");
		if(displayAlias == null) throw new IllegalArgumentException("displayAlias cannot be null");
		List<DBOPrincipalAlias> results = this.simpleJdbcTemplate.query(SQL_LIST_ALIASES_BY_ID_TYPE_AND_DISPLAY, principalAliasMapper, principalId, type.name(), displayAlias);
		return AliasUtils.createDTOFromDBO(results);
	}
	
	@Override
	public List<PrincipalAlias> listPrincipalAliases(AliasType type) {
		if(type == null) throw new IllegalArgumentException("AliasType cannot be null");
		List<DBOPrincipalAlias> results = this.simpleJdbcTemplate.query(SQL_LIST_ALIASES_BY_TYPE, principalAliasMapper, type.name());
		return AliasUtils.createDTOFromDBO(results);
	}

	@Override
	public List<PrincipalAlias> listPrincipalAliases(Set<Long> principalIds) {
		if(principalIds == null) throw new IllegalArgumentException("PrincipalIds cannot be null");
		if(principalIds.isEmpty()) return Collections.emptyList();
		Map<String,Object> parameters = new HashMap<String,Object>();
		parameters.put(SET_BIND_VAR, principalIds);
		List<DBOPrincipalAlias> results = this.simpleJdbcTemplate.query(SQL_LIST_ALIASES_FROM_SET_OF_PRINCIPAL_IDS, principalAliasMapper, parameters);
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
		idGenerator.reserveId(boot.getAliasId(), TYPE.PRINCIPAL_ALIAS_ID);
	}

	@Override
	public String getUserName(Long principalId) throws NotFoundException {
		List<PrincipalAlias> aliases = listPrincipalAliases(principalId, AliasType.USER_NAME);
		if (aliases.isEmpty()) throw new NotFoundException("No user name for "+principalId);
		if (aliases.size()>1) throw new IllegalStateException("Expected one user name but found "+aliases.size()+" for "+principalId);
		return aliases.get(0).getAlias();
	}

	@Override
	public long lookupPrincipalID(String alias, AliasType type) {
		ValidateArgument.required(alias, "alias");
		ValidateArgument.required(type, "type");
		ValidateArgument.requirement(alias != "", "alias must not be empty");
		List<Long> queryResult = simpleJdbcTemplate.query(SQL_GET_PRINCIPAL_ID, new RowMapper<Long>(){

			@Override
			public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getLong(COL_PRINCIPAL_ALIAS_PRINCIPAL_ID);
			}
		}, AliasUtils.getUniqueAliasName(alias), type.name());
		if (queryResult.size() != 1) {
			throw new NotFoundException();
		}
		return queryResult.get(0);
	}
}
