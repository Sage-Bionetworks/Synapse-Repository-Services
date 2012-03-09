package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NODE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserGroup;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.UserGroupCache;
import org.sagebionetworks.repo.model.jdo.UserGroupDAOInitializingBean;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class DBOUserGroupDAOImpl implements UserGroupDAOInitializingBean {

	@Autowired
	private DBOBasicDao basicDao;

	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTempalte;
	
	@Autowired
	private UserGroupCache userGroupCache;

	private static final String ID_PARAM_NAME = "id";
	private static final String NAME_PARAM_NAME = "name";
	private static final String IS_INDIVIDUAL_PARAM_NAME = "isIndividual";
	
	private static final String SELECT_BY_NAME_AND_IS_INDIVID_SQL = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_NAME+"=:"+NAME_PARAM_NAME+
			" AND "+SqlConstants.COL_USER_GROUP_IS_INDIVIDUAL+"=:"+IS_INDIVIDUAL_PARAM_NAME;
	
	private static final String SELECT_BY_NAME_SQL = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_NAME+"=:"+NAME_PARAM_NAME;
	
	private static final String SELECT_MULTI_BY_NAME_SQL = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_NAME+" IN (:"+NAME_PARAM_NAME+")";
	
	private static final String SELECT_BY_IS_INDIVID_SQL = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_IS_INDIVIDUAL+"=:"+IS_INDIVIDUAL_PARAM_NAME;
	
	private static final String OFFSET_PARAM_NAME = "offset";
	private static final String LIMIT_PARAM_NAME = "LIMIT";

	private static final String SELECT_BY_IS_INDIVID_SQL_PAGINATED = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_IS_INDIVIDUAL+"=:"+IS_INDIVIDUAL_PARAM_NAME+
			" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_ALL = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP;
	
	private static final String SQL_COUNT_USER_GROUPS = "SELECT COUNT("+COL_USER_GROUP_ID+") FROM "+TABLE_USER_GROUP;

	private static final RowMapper<DBOUserGroup> userGroupRowMapper = (new DBOUserGroup()).getTableMapping();
	
	@Override
	public UserGroup findGroup(String name, boolean isIndividual)
			throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(NAME_PARAM_NAME, name);
		param.addValue(IS_INDIVIDUAL_PARAM_NAME, isIndividual);		
		List<DBOUserGroup> ugs = simpleJdbcTempalte.query(SELECT_BY_NAME_AND_IS_INDIVID_SQL, userGroupRowMapper, param);
		if (ugs.size()>2) throw new DatastoreException("Expected 0-1 UserGroups but found "+ugs.size());
		if (ugs.size()==0) return null;
		UserGroup dto = new UserGroup();
		UserGroupUtils.copyDboToDto(ugs.iterator().next(), dto);
		return dto;
	}

	@Override
	public Map<String, UserGroup> getGroupsByNames(Collection<String> groupName)
			throws DatastoreException {
		Map<String, UserGroup> dtos = new HashMap<String, UserGroup>();
		if (groupName.isEmpty()) return dtos;
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(NAME_PARAM_NAME, groupName);	
		try {
			List<DBOUserGroup> dbos = simpleJdbcTempalte.query(SELECT_MULTI_BY_NAME_SQL, userGroupRowMapper, param);
			for (DBOUserGroup dbo : dbos) {
				UserGroup dto = new UserGroup();
				UserGroupUtils.copyDboToDto(dbo, dto);
				dtos.put(dto.getName(), dto);
			}
			return dtos;
		} catch (Exception e) {
			throw new DatastoreException("'getGroupsByNames' failed for group list: "+groupName, e);
		}
	}

	@Override
	public Collection<UserGroup> getAll(boolean isIndividual)
			throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(IS_INDIVIDUAL_PARAM_NAME, isIndividual);		
		List<DBOUserGroup> dbos = simpleJdbcTempalte.query(SELECT_BY_IS_INDIVID_SQL, userGroupRowMapper, param);
		List<UserGroup> dtos = new ArrayList<UserGroup>();
		for (DBOUserGroup dbo : dbos) {
			UserGroup dto = new UserGroup();
			UserGroupUtils.copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}

	@Override
	public List<UserGroup> getInRange(long fromIncl, long toExcl,
			boolean isIndividual) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(IS_INDIVIDUAL_PARAM_NAME, isIndividual);		
		param.addValue(OFFSET_PARAM_NAME, fromIncl);
		long limit = toExcl - fromIncl;
		if (limit<=0) throw new IllegalArgumentException("'to' param must be greater than 'from' param.");
		param.addValue(LIMIT_PARAM_NAME, limit);	
		List<DBOUserGroup> dbos = simpleJdbcTempalte.query(SELECT_BY_IS_INDIVID_SQL_PAGINATED, userGroupRowMapper, param);
		List<UserGroup> dtos = new ArrayList<UserGroup>();
		for (DBOUserGroup dbo : dbos) {
			UserGroup dto = new UserGroup();
			UserGroupUtils.copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;
	}

	public DBOUserGroup findGroup(String name) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(NAME_PARAM_NAME, name);	
		List<DBOUserGroup> ugs = simpleJdbcTempalte.query(SELECT_BY_NAME_SQL, userGroupRowMapper, param);
		if (ugs.size()>2) throw new DatastoreException("Expected 0-1 UserGroups but found "+ugs.size());
		if (ugs.size()==0) return null;
		return ugs.iterator().next();
	}
	
	@Override
	public boolean doesPrincipalExist(String name) {
		try {
			return null!=findGroup(name);
		} catch (DatastoreException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean deletePrincipal(String name) {
		try {
			DBOUserGroup ug = findGroup(name);
			if (ug==null) return false;
			delete(KeyFactory.keyToString(ug.getId()));
			return true;
		} catch (DatastoreException e) {
			throw new RuntimeException(e);
		} catch (NotFoundException e) {
			return false;
		}
	}

	@Override
	public String create(UserGroup dto) throws DatastoreException,
			InvalidModelException {
		DBOUserGroup dbo = new DBOUserGroup();
		UserGroupUtils.copyDtoToDbo(dto, dbo);
		if(dbo.getId() == null){
			dbo.setId(idGenerator.generateNewId());
		}else{
			// If an id was provided then it must not exist
			if(doesIdExist(dbo.getId())) throw new IllegalArgumentException("The id: "+dbo.getId()+" already exists, so a node cannot be created using that id.");
			// Make sure the ID generator has reserved this ID.
			idGenerator.reserveId(dbo.getId());
		}
		if (dbo.geteTag()==null) {
			dbo.seteTag(0L);
		}
		dbo = basicDao.createNew(dbo);
		return KeyFactory.keyToString(dbo.getId());
	}

	@Transactional(readOnly = true)
	public boolean doesIdExist(Long id) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(ID_PARAM_NAME, id);
		try{
			long count = simpleJdbcTempalte.queryForLong(SQL_COUNT_USER_GROUPS, parameters);
			return count > 0;
		}catch(Exception e){
			// Can occur when the schema does not exist.
			return false;
		}
	}
	
	@Override
	public UserGroup get(String id) throws DatastoreException,
			NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, id);		
		DBOUserGroup dbo = basicDao.getObjectById(DBOUserGroup.class, param);
		UserGroup dto = new UserGroup();
		UserGroupUtils.copyDboToDto(dbo, dto);
		return dto;
	}

	@Override
	public Collection<UserGroup> getAll() throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();	
		List<DBOUserGroup> dbos = simpleJdbcTempalte.query(SELECT_ALL, userGroupRowMapper, param);
		List<UserGroup> dtos = new ArrayList<UserGroup>();
		for (DBOUserGroup dbo : dbos) {
			UserGroup dto = new UserGroup();
			UserGroupUtils.copyDboToDto(dbo, dto);
			dtos.add(dto);
		}
		return dtos;

	}

	@Override
	public void update(UserGroup dto) throws DatastoreException,
			InvalidModelException, NotFoundException,
			ConflictingUpdateException {
		DBOUserGroup dbo = new DBOUserGroup();
		UserGroupUtils.copyDtoToDbo(dto, dbo);
		userGroupCache.delete(dbo.getId());
		basicDao.update(dbo);
	}

	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		userGroupCache.delete(KeyFactory.stringToKey(id));
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, id);
		basicDao.deleteObjectById(DBOUserGroup.class, param);
	}

	// initialization of UserGroups
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void afterPropertiesSet() throws Exception {
		// ensure public group is created
		// Make sure all of the default groups exist
		DEFAULT_GROUPS[] groups = DEFAULT_GROUPS.values();
		for (DEFAULT_GROUPS group : groups) {
			UserGroup pg = findGroup(group.name(), false);
			if (pg == null) {
				pg = new UserGroup();
				pg.setName(group.name());
				pg.setIndividual(false);
				create(pg);
			}
		}

		// ensure the anonymous principal is created
		UserGroup anon = findGroup(AuthorizationConstants.ANONYMOUS_USER_ID, true);
		if (anon == null) {
			anon = new UserGroup();
			anon.setName(AuthorizationConstants.ANONYMOUS_USER_ID);
			anon.setIndividual(true);
			create(anon);
		}
	}



}
