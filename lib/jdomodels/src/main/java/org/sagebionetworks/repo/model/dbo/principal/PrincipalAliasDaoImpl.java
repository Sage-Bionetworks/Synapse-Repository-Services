package org.sagebionetworks.repo.model.dbo.principal;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_ID;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.principal.AliasEnum;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
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
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private DBOBasicDao basicDao;
	
	private static RowMapper<DBOPrincipalAlias> principalAliasMapper = new DBOPrincipalAlias().getTableMapping();

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public PrincipalAlias bindAliasToPrincipal(PrincipalAlias binding) {
		if(binding == null) throw new IllegalArgumentException("PrincipalAlais cannot be null");		
		// Convert to the DBO
		DBOPrincipalAlias dbo = AliasUtils.createDBOFromDTO(binding);
		// Does this alias already exist?
		PrincipalAlias current = findPrincipalWithAlias(binding.getAlias());
		if(current != null){
			// Must match the type and id
			if(current.getPrincipalId().equals(obj))
			// this alias does not exist so create a new ID
			aliasId = idGenerator.generateNewId(IdGenerator.TYPE.PRINCIPAL_ALIAS_ID);
		}
		dbo.setId(aliasId);
		dbo.setEtag(UUID.randomUUID().toString());
		
		// Create or update the alias
		basicDao.createOrUpdate(dbo);
		// return the results from the DB.
		return getPrincipalAlias(aliasId);
	}

	@Override
	public PrincipalAlias getPrincipalAlias(Long aliasId) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public PrincipalAlias findPrincipalWithAlias(String alias) {
		if(alias == null) throw new IllegalArgumentException("Alias cannot be null");
		String unique = AliasUtils.getUniqueAliasName(alias);
		try {
			DBOPrincipalAlias dbo = simpleJdbcTemplate.queryForObject("", principalAliasMapper, unique);
			return AliasUtils.createDTOFromDBO(dbo);
		} catch (EmptyResultDataAccessException e) {
			// no match
			return null;
		}
	}

	@Override
	public boolean isAliasAvailable(String alias) {
		// TODO Auto-generated method stub
		return false;
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
