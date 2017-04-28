package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_IS_INDIVIDUAL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserGroup;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.principal.BootstrapPrincipal;
import org.sagebionetworks.repo.model.principal.BootstrapUser;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class DBOUserGroupDAOImpl implements UserGroupDAO {

	private static final String SQL_SELECT_ALL = "SELECT * FROM "+TABLE_USER_GROUP;

	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private TransactionalMessenger transactionalMessenger;
	
	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;
	
	private List<BootstrapPrincipal> bootstrapPrincipals;
	
	private static final String ID_PARAM_NAME = "id";
	private static final String IS_INDIVIDUAL_PARAM_NAME = "isIndividual";
	private static final String ETAG_PARAM_NAME = "etag";

	private static final String SELECT_MULTI_BY_PRINCIPAL_IDS = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_ID+" IN (:"+ID_PARAM_NAME+")";
	
	private static final String SELECT_BY_IS_INDIVID_SQL = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_IS_INDIVIDUAL+"=:"+IS_INDIVIDUAL_PARAM_NAME;
	
	private static final String SELECT_BY_IS_INDIVID_SQL_PAGINATED = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_IS_INDIVIDUAL+"=:"+IS_INDIVIDUAL_PARAM_NAME+
			" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final String SELECT_ETAG_AND_LOCK_ROW_BY_ID = 
			"SELECT "+SqlConstants.COL_USER_GROUP_E_TAG+" FROM "+SqlConstants.TABLE_USER_GROUP+
			" WHERE "+SqlConstants.COL_USER_GROUP_ID+"=:"+ID_PARAM_NAME+
			" FOR UPDATE";
	
	private static final String UPDATE_ETAG_LIST = 
			"UPDATE "+SqlConstants.TABLE_USER_GROUP+
			" SET "+SqlConstants.COL_USER_GROUP_E_TAG+"=:"+ETAG_PARAM_NAME+
			" WHERE "+SqlConstants.COL_USER_GROUP_ID+"=:"+ID_PARAM_NAME;
	
	private static final String SELECT_IS_INDIVIDUAL = 
			"SELECT "+COL_USER_GROUP_IS_INDIVIDUAL+
			" FROM "+TABLE_USER_GROUP+
			" WHERE "+COL_USER_GROUP_ID+" = :"+ID_PARAM_NAME;

	private static final String SQL_COUNT_USER_GROUPS = "SELECT COUNT("+COL_USER_GROUP_ID+") FROM "+TABLE_USER_GROUP + " WHERE "+COL_USER_GROUP_ID+"=:"+ID_PARAM_NAME;

	private static final RowMapper<DBOUserGroup> userGroupRowMapper = (new DBOUserGroup()).getTableMapping();
	
	

	public List<BootstrapPrincipal> getBootstrapPrincipals() {
		return bootstrapPrincipals;
	}

	public void setBootstrapPrincipals(List<BootstrapPrincipal> bootstrapPrincipals) {
		this.bootstrapPrincipals = bootstrapPrincipals;
	}

	@Override
	public Collection<UserGroup> getAll(boolean isIndividual)
			throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(IS_INDIVIDUAL_PARAM_NAME, isIndividual);		
		List<DBOUserGroup> dbos = namedJdbcTemplate.query(SELECT_BY_IS_INDIVID_SQL, param, userGroupRowMapper);
		List<UserGroup> dtos = new ArrayList<UserGroup>();
		UserGroupUtils.copyDboToDto(dbos, dtos);
		return dtos;
	}
	
	@Override
	public List<UserGroup> getAllPrincipals() {
		List<DBOUserGroup> dbos = namedJdbcTemplate.query(SQL_SELECT_ALL, userGroupRowMapper);
		List<UserGroup> dtos = new ArrayList<UserGroup>();
		UserGroupUtils.copyDboToDto(dbos, dtos);
		return dtos;
	}
	
	@Override
	public long getCount()  throws DatastoreException {
		return basicDao.getCount(DBOUserGroup.class);
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
		List<DBOUserGroup> dbos = namedJdbcTemplate.query(SELECT_BY_IS_INDIVID_SQL_PAGINATED, param, userGroupRowMapper);
		List<UserGroup> dtos = new ArrayList<UserGroup>();
		UserGroupUtils.copyDboToDto(dbos, dtos);
		return dtos;
	}
	
	@Override
	@WriteTransaction
	public Long create(UserGroup dto) throws DatastoreException,
			InvalidModelException {
		// The public version unconditionally clears the ID so a new one will be assigned
		dto.setId(null);
		DBOUserGroup dbo = createPrivate(dto);
		// Send a CREATE message
		// Note: This message cannot be sent in the createPrivate method because
		// bootstrapping is not transactional when called by the Spring initializer 
		transactionalMessenger.sendMessageAfterCommit("" + dbo.getId(), ObjectType.PRINCIPAL, dbo.getEtag(), ChangeType.CREATE);
		return dbo.getId();
	}

	/**
	 * This will not clear the ID like the public method.
	 * This allows us to boostrap users with set IDs.
	 * @param dto
	 * @return
	 */
	private DBOUserGroup createPrivate(UserGroup dto) {
		DBOUserGroup dbo = new DBOUserGroup();
		UserGroupUtils.copyDtoToDbo(dto, dbo);
		// If the create is successful, it should have a new etag
		dbo.setEtag(UUID.randomUUID().toString());
		// Bootstraped users will have IDs already assigned.
		if(dbo.getId() == null){
			// We allow the ID generator to create all other IDs
			dbo.setId(idGenerator.generateNewId(IdType.PRINCIPAL_ID));
		}
		
		try {
			dbo = basicDao.createNew(dbo);
		} catch (Exception e) {
			throw new DatastoreException("id=" + dbo.getId(), e);
		}	
		return dbo;
	}

	public boolean doesIdExist(Long id) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(ID_PARAM_NAME, id);
		try{
			long count = namedJdbcTemplate.queryForObject(SQL_COUNT_USER_GROUPS, parameters, Long.class);
			return count > 0;
		}catch(Exception e){
			// Can occur when the schema does not exist.
			return false;
		}
	}
	
	@Override
	public UserGroup get(Long id) throws DatastoreException,
			NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, id);
		DBOUserGroup dbo;
		try {
			dbo = basicDao.getObjectByPrimaryKey(DBOUserGroup.class, param);
		} catch (NotFoundException e) {
			// Rethrow the basic DAO's generic error message
			throw new NotFoundException("Principal (" + id + ") does not exist");
		}
		UserGroup dto = new UserGroup();
		UserGroupUtils.copyDboToDto(dbo, dto);
		return dto;
	}
	
	@Override
	public List<UserGroup> get(List<String> ids) throws DatastoreException {
		List<UserGroup> dtos = new ArrayList<UserGroup>();
		if (ids.isEmpty()) {
			return dtos;
		}
		
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, ids);
		List<DBOUserGroup> dbos = namedJdbcTemplate.query(SELECT_MULTI_BY_PRINCIPAL_IDS, param, userGroupRowMapper);
		UserGroupUtils.copyDboToDto(dbos, dtos);
		return dtos;
	}

	@WriteTransaction
	@Override
	public void update(UserGroup dto) throws DatastoreException,
			InvalidModelException, NotFoundException,
			ConflictingUpdateException {
		DBOUserGroup dbo = new DBOUserGroup();
		UserGroupUtils.copyDtoToDbo(dto, dbo);
		
		// If the update is successful, it should have a new etag
		dbo.setEtag(UUID.randomUUID().toString());
		
		// Send a UPDATE message
		transactionalMessenger.sendMessageAfterCommit("" + dbo.getId(), ObjectType.PRINCIPAL, dbo.getEtag(), ChangeType.UPDATE);

		basicDao.update(dbo);
	}

	@WriteTransaction
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, id);
		basicDao.deleteObjectByPrimaryKey(DBOUserGroup.class, param);
	}
	
	/**
	 * This is called by Spring after all properties are set
	 */
	@Override
	@WriteTransaction
	public void bootstrapUsers() throws Exception {
		// Reserver an ID well above the current
		idGenerator.reserveId(3318977l, IdType.PRINCIPAL_ID);
		
		// Boot strap all users and groups
		if (this.bootstrapPrincipals == null) {
			throw new IllegalArgumentException("bootstrapPrincipals cannot be null");
		}
		
		// For each one determine if it exists, if not create it
		for (BootstrapPrincipal abs: this.bootstrapPrincipals) {
			if (abs.getId() == null) {
				throw new IllegalArgumentException("Bootstrap users must have an id");
			}

			if (!this.doesIdExist(abs.getId())) {
				UserGroup newUg = new UserGroup();
				newUg.setId(abs.getId().toString());
				if(abs instanceof BootstrapUser){
					newUg.setIsIndividual(true);
				}else{
					newUg.setIsIndividual(false);
				}
				this.createPrivate(newUg);
			}
		}
	}
	
	@WriteTransaction
	@Override
	public String getEtagForUpdate(String id) throws NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, id);
		try {
			return namedJdbcTemplate.queryForObject(SELECT_ETAG_AND_LOCK_ROW_BY_ID, param,
				new RowMapper<String>() {
					@Override
					public String mapRow(ResultSet rs, int rowNum)
							throws SQLException {
						return rs.getString(SqlConstants.COL_USER_GROUP_E_TAG);
					}
				});
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Principal ID "+id+" not found in system.");
		}
	}

	@WriteTransaction
	@Override
	public void touch(Long principalId) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, principalId);
		param.addValue(ETAG_PARAM_NAME, UUID.randomUUID().toString());
		namedJdbcTemplate.update(UPDATE_ETAG_LIST, param);
	}

	@Override
	public boolean isIndividual(Long principalId) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(ID_PARAM_NAME, principalId);
		try {
			return namedJdbcTemplate.queryForObject(SELECT_IS_INDIVIDUAL, param, Boolean.class);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Principal ID "+principalId+" not found");
		}
	}

}
