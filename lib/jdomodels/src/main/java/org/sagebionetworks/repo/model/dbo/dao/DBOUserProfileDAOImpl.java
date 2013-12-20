package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_PROFILE;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserGroupInt;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.TableMapping;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserProfile;
import org.sagebionetworks.repo.model.query.jdo.SqlConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author brucehoff
 *
 */
public class DBOUserProfileDAOImpl implements UserProfileDAO {
	
	@Autowired
	private DBOBasicDao basicDao;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	private static final String SELECT_PAGINATED = 
			"SELECT * FROM "+SqlConstants.TABLE_USER_PROFILE+
			" LIMIT :"+LIMIT_PARAM_NAME+" OFFSET :"+OFFSET_PARAM_NAME;
	
	private static final RowMapper<DBOUserProfile> userProfileRowMapper = (new DBOUserProfile()).getTableMapping();

	/* (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.UserProfileDAO#delete(java.lang.String)
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(DBOUserProfile.OWNER_ID_FIELD_NAME, id);
		basicDao.deleteObjectByPrimaryKey(DBOUserProfile.class, param);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String create(UserProfile dto) throws DatastoreException,
			InvalidModelException {
		DBOUserProfile jdo = new DBOUserProfile();
		UserProfileUtils.copyDtoToDbo(dto, jdo);
		if (jdo.geteTag() == null) {
			jdo.seteTag(UUID.randomUUID().toString());
		}
		jdo = basicDao.createNew(jdo);
		return jdo.getOwnerId().toString();
	}

	@Override
	public UserProfile get(String id) throws DatastoreException,
			NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(DBOUserProfile.OWNER_ID_FIELD_NAME, id);
		DBOUserProfile jdo = basicDao.getObjectByPrimaryKey(DBOUserProfile.class, param);
		UserProfile dto = UserProfileUtils.convertDboToDto(jdo);
		return dto;
	}


	@Override
	public List<UserProfile> getInRange(long fromIncl, long toExcl) throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();		
		param.addValue(OFFSET_PARAM_NAME, fromIncl);
		long limit = toExcl - fromIncl;
		if (limit<=0) throw new IllegalArgumentException("'to' param must be greater than 'from' param.");
		param.addValue(LIMIT_PARAM_NAME, limit);	
		List<DBOUserProfile> dbos = simpleJdbcTemplate.query(SELECT_PAGINATED, userProfileRowMapper, param);
		List<UserProfile> dtos = new ArrayList<UserProfile>();
		for (DBOUserProfile dbo : dbos) {
			UserProfile dto = UserProfileUtils.convertDboToDto(dbo);
			dtos.add(dto);
		}
		return dtos;
	}

	@Override
	public long getCount() throws DatastoreException {
		return basicDao.getCount(DBOUserProfile.class);
	}

	/*
	 * (non-Javadoc)
	 * @see org.sagebionetworks.repo.model.UserProfileDAO#update(UserProfile, ObjectSchema)
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public UserProfile update(UserProfile dto) throws DatastoreException,
			InvalidModelException, NotFoundException, ConflictingUpdateException {
		DBOUserProfile dbo = null;
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(DBOUserProfile.OWNER_ID_FIELD_NAME, dto.getOwnerId());
		try{
			dbo = simpleJdbcTemplate.queryForObject(SELECT_FOR_UPDATE_SQL, TABLE_MAPPING, param);
		}catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("The resource you are attempting to access cannot be found");
		}

		// Check dbo's etag against dto's etag
		// if different rollback and throw a meaningful exception
		if (!dbo.geteTag().equals(dto.getEtag())) {
			throw new ConflictingUpdateException("Use profile was updated since you last fetched it, retrieve it again and reapply the update.");
		}
		UserProfileUtils.copyDtoToDbo(dto, dbo);
		// Update with a new e-tag
		dbo.seteTag(UUID.randomUUID().toString());

		boolean success = basicDao.update(dbo);
		if (!success) throw new DatastoreException("Unsuccessful updating user profile in database.");

		UserProfile resultantDto = UserProfileUtils.convertDboToDto(dbo);

		return resultantDto;
	}

	private static final String SELECT_FOR_UPDATE_SQL = "select * from "+TABLE_USER_PROFILE+" where "+COL_USER_PROFILE_ID+
			"=:"+DBOUserProfile.OWNER_ID_FIELD_NAME+" for update";

	private static final TableMapping<DBOUserProfile> TABLE_MAPPING = (new DBOUserProfile()).getTableMapping();
	
	/**
	 * This is called by Spring after all properties are set.
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void bootstrapProfiles(){
		// Boot strap all users and groups
		if (userGroupDAO.getBootstrapUsers() == null) {
			throw new IllegalArgumentException("Bootstrap users cannot be null");
		}
		
		// For each one determine if it exists, if not create it
		for (UserGroupInt ug: userGroupDAO.getBootstrapUsers()) {
			if (ug.getId() == null) throw new IllegalArgumentException("Bootstrap users must have an id");
			if (ug.getIsIndividual()) {
				Long.parseLong(ug.getId());
				UserProfile userProfile = null;
				try {
					userProfile = this.get(ug.getId());
				} catch (NotFoundException nfe) {
					userProfile = new UserProfile();
					userProfile.setOwnerId(ug.getId());
					this.create(userProfile);
				}
			}
		}
	}
}
