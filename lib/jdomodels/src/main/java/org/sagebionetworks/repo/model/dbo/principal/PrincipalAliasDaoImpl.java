package org.sagebionetworks.repo.model.dbo.principal;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_UNIQUE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PRINCIPAL_ALIAS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.BootstrapGroup;
import org.sagebionetworks.repo.model.principal.BootstrapPrincipal;
import org.sagebionetworks.repo.model.principal.BootstrapUser;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Basic database implementation of of PrincipalAliasDAO
 * @author John
 *
 */
public class PrincipalAliasDaoImpl implements PrincipalAliasDAO {
	
	private static final String SQL_LIST_ALIASES_BY_ID = "SELECT * FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_PRINCIPAL_ID+" = ? ORDER BY "+COL_PRINCIPAL_ALIAS_ID;
	private static final String SQL_LIST_ALIASES_BY_ID_AND_TYPE = "SELECT * FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_PRINCIPAL_ID+" = ? AND "+COL_PRINCIPAL_ALIAS_TYPE+" = ? ORDER BY "+COL_PRINCIPAL_ALIAS_ID;
	private static final String SQL_GET_ALIAS = "SELECT * FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_ID+" = ?";
	private static final String SQL_FIND_PRINCIPAL_WITH_ALIAS = "SELECT * FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_UNIQUE+" = ?";
	private static final String SQL_IS_ALIAS_AVAILABLE = "SELECT COUNT(*) FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_UNIQUE+" = ?";
	private static final String SET_BIND_VAR = "principalIdSet";
	private static final String SQL_LIST_ALIASES_FROM_SET_OF_PRINCIPAL_IDS = "SELECT * FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_PRINCIPAL_ID+" IS IN (:"+SET_BIND_VAR+") GROUP BY "+COL_PRINCIPAL_ALIAS_PRINCIPAL_ID;
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private DBOBasicDao basicDao;
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	private static RowMapper<DBOPrincipalAlias> principalAliasMapper = new DBOPrincipalAlias().getTableMapping();

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public PrincipalAlias bindAliasToPrincipal(PrincipalAlias dto) throws NotFoundException {
		if(dto == null) throw new IllegalArgumentException("PrincipalAlais cannot be null");
		if(dto.getAlias() == null) throw new IllegalArgumentException("Alias cannot be null");
		// Convert to the DBO
		DBOPrincipalAlias dbo = AliasUtils.createDBOFromDTO(dto);
		// Validate the alias
		dbo.getAliasType().validateAlias(dbo.getAliasDisplay());
		// Does this alias already exist?
		PrincipalAlias current = findPrincipalWithAlias(dto.getAlias());
		if(current != null){
			// Is this ID already assigned to another user?
			if(!current.getPrincipalId().equals(dbo.getPrincipalId())) throw new IllegalArgumentException("The alias: "+dto.getAlias()+" is already in use.");
			if(!current.getType().equals(dto.getType())) throw new IllegalArgumentException("Cannot change the type of an alias: "+dto.getAlias()+" from "+dto.getAlias()+" to "+dbo.getAliasType());
			// Use the ID of the type
			dbo.setId(current.getAliasId());
		}else{
			// this is a new alias
			dbo.setId(idGenerator.generateNewId(TYPE.PRINCIPAL_ALIAS_ID));
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
	public boolean isAliasAvailable(String alias) {
		if(alias == null) throw new IllegalArgumentException("Alias cannot be null");
		String unique = AliasUtils.getUniqueAliasName(alias);
		long count = simpleJdbcTemplate.queryForLong(SQL_IS_ALIAS_AVAILABLE, unique);
		return count < 1;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean setAliasValid(Long aliasId, boolean valid) {
		// TODO Auto-generated method stub
		return false;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean setAliasDefault(Long aliasId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removeAliasFromPrincipal(Long principalId, Long aliasId) {
		// TODO Auto-generated method stub
		return false;
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
	public List<PrincipalAlias> listPrincipalAliases(Set<Long> principalIds) {
		if(principalIds == null) throw new IllegalArgumentException("PrincipalIds cannot be null");
		Map<String,Object> parameters = new HashMap<String,Object>();
		parameters.put(SET_BIND_VAR, principalIds);
		List<DBOPrincipalAlias> results = this.simpleJdbcTemplate.query(SQL_LIST_ALIASES_FROM_SET_OF_PRINCIPAL_IDS, principalAliasMapper, parameters);
		return AliasUtils.createDTOFromDBO(results);
	}
	
	/**
	 * This is called by Spring after all properties are set.
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
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
				try {
					// email
					BootstrapUser user = (BootstrapUser) abs;
					PrincipalAlias alias = new PrincipalAlias();
					alias.setAlias(user.getEmail());
					alias.setIsValidated(true);
					alias.setPrincipalId(abs.getId());
					alias.setType(AliasType.USER_EMAIL);
					this.bindAliasToPrincipal(alias);
					// username
					alias = new PrincipalAlias();
					alias.setAlias(user.getUserName());
					alias.setIsValidated(true);
					alias.setPrincipalId(abs.getId());
					alias.setType(AliasType.USER_NAME);
					this.bindAliasToPrincipal(alias);
				} catch (NotFoundException e) {
					throw new IllegalStateException(e);
				}
			}else{
				// This is a group
				// Add the username and email
				try {
					// Group name
					BootstrapGroup group = (BootstrapGroup) abs;
					PrincipalAlias alias = new PrincipalAlias();
					alias.setAlias(group.getGroupName());
					alias.setIsValidated(true);
					alias.setPrincipalId(abs.getId());
					alias.setType(AliasType.TEAM_NAME);
					this.bindAliasToPrincipal(alias);
				} catch (NotFoundException e) {
					throw new IllegalStateException(e);
				}
			}
		}
	}

}
