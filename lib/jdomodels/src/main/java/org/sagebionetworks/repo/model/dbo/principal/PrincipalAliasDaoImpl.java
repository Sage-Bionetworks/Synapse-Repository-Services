package org.sagebionetworks.repo.model.dbo.principal;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.principal.AliasEnum;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
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
	
	private static final String SQL_GET_ALIAS = "SELECT * FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_ID+" = ?";
	private static final String SQL_FIND_PRINCIPAL_WITH_ALIAS = "SELECT * FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_UNIQUE+" = ?";
	private static final String SQL_IS_ALIAS_AVAILABLE = "SELECT COUNT(*) FROM "+TABLE_PRINCIPAL_ALIAS+" WHERE "+COL_PRINCIPAL_ALIAS_UNIQUE+" = ?";
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private DBOBasicDao basicDao;
	
	private static RowMapper<DBOPrincipalAlias> principalAliasMapper = new DBOPrincipalAlias().getTableMapping();

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public PrincipalAlias bindAliasToPrincipal(PrincipalAlias dto) throws NotFoundException {
		if(dto == null) throw new IllegalArgumentException("PrincipalAlais cannot be null");
		if(dto.getAlias() == null) throw new IllegalArgumentException("Alias cannot be null");
		// Convert to the DBO
		DBOPrincipalAlias dbo = AliasUtils.createDBOFromDTO(dto);
		// Validate the alias
		dbo.getAliasType().validateAlias(dbo.getAliasUnique());
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
	public List<PrincipalAlias> listPrincipalAliases(Long principalId,
			Set<AliasType> typesToInclude) {
		// TODO Auto-generated method stub
		return null;
	}

}
