package org.sagebionetworks.repo.model.dbo.dao;

import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_NOTIFICATION_EMAIL_ALIAS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_DISPLAY;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_PRINCIPAL_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_PRINCIPAL_ALIAS_TYPE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_CREATION_DATE;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_GROUP_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_EMAIL_NOTIFICATION;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_ETAG;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_FIRST_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_LAST_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_PICTURE_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_USER_PROFILE_PROPS_BLOB;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.LIMIT_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.OFFSET_PARAM_NAME;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_NOTIFICATION_EMAIL;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_PRINCIPAL_ALIAS;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_GROUP;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.TABLE_USER_PROFILE;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.broadcast.UserNotificationInfo;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserProfile;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.BootstrapPrincipal;
import org.sagebionetworks.repo.model.principal.BootstrapUser;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

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
	private TransactionalMessenger transactionalMessenger;

	@Autowired
	private NamedParameterJdbcTemplate namedJdbcTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	private static final String SQL_SELECT_USER_PROFILE = "SELECT G."+COL_USER_GROUP_CREATION_DATE+", P.* FROM "+TABLE_USER_GROUP+" G JOIN "+TABLE_USER_PROFILE+" P ON (G."+COL_USER_GROUP_ID+" = P."+COL_USER_PROFILE_ID+")";

	private static final String SQL_SELECT_PROFILE_BY_ID = SQL_SELECT_USER_PROFILE+" WHERE G."+COL_USER_GROUP_ID+" = ?";

	private static final String SELECT_PAGINATED = SQL_SELECT_USER_PROFILE + " LIMIT :" + LIMIT_PARAM_NAME + " OFFSET :"
			+ OFFSET_PARAM_NAME;

	private static final String LIST_FOR_IDS = SQL_SELECT_USER_PROFILE + " WHERE P." + COL_USER_PROFILE_ID + " in (:"
			+ COL_USER_PROFILE_ID + ")";

	private static final String SQL_SELECT_PROFILE_PIC_ID = "SELECT "
			+ COL_USER_PROFILE_PICTURE_ID + " FROM " + TABLE_USER_PROFILE
			+ " WHERE " + COL_USER_PROFILE_ID + " = ?";

	private static final RowMapper<DBOUserProfile> USER_PROFILE_ROW_MAPPER = new RowMapper<DBOUserProfile>() {

		@Override
		public DBOUserProfile mapRow(ResultSet rs, int rowNum) throws SQLException {
			DBOUserProfile up = new DBOUserProfile();
			up.setOwnerId(rs.getLong(COL_USER_PROFILE_ID));
			java.sql.Blob blob = rs.getBlob(COL_USER_PROFILE_PROPS_BLOB);
			if(blob != null){
				up.setProperties(blob.getBytes(1, (int) blob.length()));
			}
			up.seteTag(rs.getString(COL_USER_PROFILE_ETAG));
			up.setPictureId(rs.getLong(COL_USER_PROFILE_PICTURE_ID));
			if(rs.wasNull()){
				up.setPictureId(null);
			}
			up.setEmailNotification(rs.getBoolean(COL_USER_PROFILE_EMAIL_NOTIFICATION));
			blob = rs.getBlob(COL_USER_PROFILE_FIRST_NAME);
			if (blob != null){
				up.setFirstName(blob.getBytes(1, (int) blob.length()));
			}
			blob = rs.getBlob(COL_USER_PROFILE_LAST_NAME);
			if (blob != null){
				up.setLastName(blob.getBytes(1, (int) blob.length()));
			}
			Timestamp createdOnTimestamp = rs.getTimestamp(COL_USER_GROUP_CREATION_DATE);
			if(createdOnTimestamp != null) {
				up.setCreatedOn(createdOnTimestamp.getTime());
			}
			return up;
		}
	};
	
	private static final String SELECT_FOR_UPDATE_SQL = SQL_SELECT_USER_PROFILE + " where " + COL_USER_PROFILE_ID + "=:"
			+ DBOUserProfile.OWNER_ID_FIELD_NAME + " for update";

	private static final String SQL_GET_USER_INFO_FOR_NOTIFICATION = "SELECT"
			+ " U."+ COL_USER_PROFILE_ID + ","
			+ " U." + COL_USER_PROFILE_FIRST_NAME + ","
			+ " U." + COL_USER_PROFILE_LAST_NAME + ","
			+ " A1." + COL_PRINCIPAL_ALIAS_DISPLAY + " AS 'EMAIL',"
			+ " A2." + COL_PRINCIPAL_ALIAS_DISPLAY + " AS 'USERNAME'"
			+ " FROM " + TABLE_USER_PROFILE + " U, "
			+ TABLE_NOTIFICATION_EMAIL + " N, "
			+ TABLE_PRINCIPAL_ALIAS + " A1, "
			+ TABLE_PRINCIPAL_ALIAS + " A2 "
			+ "WHERE U." + COL_USER_PROFILE_ID + " IN (:ids)"
			+ " AND U." + COL_USER_PROFILE_EMAIL_NOTIFICATION + " = true"
			+ " AND A1." + COL_PRINCIPAL_ALIAS_PRINCIPAL_ID + " = U." + COL_USER_PROFILE_ID
			+ " AND N." + COL_NOTIFICATION_EMAIL_ALIAS_ID + " = A1." + COL_PRINCIPAL_ALIAS_ID
			+ " AND A2." + COL_PRINCIPAL_ALIAS_PRINCIPAL_ID + " = U." + COL_USER_PROFILE_ID 
			+ " AND A2." + COL_PRINCIPAL_ALIAS_TYPE + " = '" + AliasType.USER_NAME.name() + "'";

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.model.UserProfileDAO#delete(java.lang.String)
	 */
	@WriteTransaction
	@Override
	public void delete(String id) throws DatastoreException, NotFoundException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(DBOUserProfile.OWNER_ID_FIELD_NAME, id);
		basicDao.deleteObjectByPrimaryKey(DBOUserProfile.class, param);
	}

	@WriteTransaction
	@Override
	public String create(UserProfile dto) throws DatastoreException,
			InvalidModelException {
		DBOUserProfile jdo = doCreate(dto);
		transactionalMessenger.sendMessageAfterCommit("" + jdo.getOwnerId(),
				ObjectType.PRINCIPAL, jdo.geteTag(), ChangeType.CREATE);
		return jdo.getOwnerId().toString();
	}

	private DBOUserProfile doCreate(UserProfile dto) throws DatastoreException,
			InvalidModelException {
		DBOUserProfile jdo = new DBOUserProfile();
		UserProfileUtils.copyDtoToDbo(dto, jdo);
		if (jdo.geteTag() == null) {
			jdo.seteTag(UUID.randomUUID().toString());
		}
		return basicDao.createNew(jdo);
	}

	@Override
	public UserProfile get(String id) throws DatastoreException,
			NotFoundException {
		DBOUserProfile jdo;
		try {
			jdo = jdbcTemplate.queryForObject(SQL_SELECT_PROFILE_BY_ID, USER_PROFILE_ROW_MAPPER, id);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("UserProfile cannot be found for: "+id);
		}
		UserProfile dto = UserProfileUtils.convertDboToDto(jdo);
		return dto;
	}

	@Override
	public List<UserProfile> getInRange(long fromIncl, long toExcl)
			throws DatastoreException {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(OFFSET_PARAM_NAME, fromIncl);
		long limit = toExcl - fromIncl;
		if (limit <= 0)
			throw new IllegalArgumentException(
					"'to' param must be greater than 'from' param.");
		param.addValue(LIMIT_PARAM_NAME, limit);
		List<DBOUserProfile> dbos = namedJdbcTemplate.query(SELECT_PAGINATED, param,
				USER_PROFILE_ROW_MAPPER);
		List<UserProfile> dtos = new ArrayList<UserProfile>();
		for (DBOUserProfile dbo : dbos) {
			UserProfile dto = UserProfileUtils.convertDboToDto(dbo);
			dtos.add(dto);
		}
		return dtos;
	}
	
	public List<UserProfile> list(List<Long> ids) throws DatastoreException, NotFoundException {
		if (ids==null || ids.size()<1) return Collections.emptyList();
		MapSqlParameterSource param = new MapSqlParameterSource();		
		param.addValue(COL_USER_PROFILE_ID, ids);
		List<DBOUserProfile> dbos = namedJdbcTemplate.query(LIST_FOR_IDS, param, USER_PROFILE_ROW_MAPPER);
		Map<String,UserProfile> map = new HashMap<String,UserProfile>();
		for (DBOUserProfile dbo : dbos) {
			UserProfile dto = UserProfileUtils.convertDboToDto(dbo);
			map.put(dto.getOwnerId(), dto);
		}
		List<UserProfile> dtos = new ArrayList<UserProfile>();
		for (Long id : ids) {
			UserProfile userProfile = map.get(id.toString());
			if (userProfile == null) {
				throw new NotFoundException("User with id " + id + " not found");
			}
			dtos.add(userProfile);
		}
		return dtos;
	}

	@Override
	public long getCount() throws DatastoreException {
		return basicDao.getCount(DBOUserProfile.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.model.UserProfileDAO#update(UserProfile,
	 * ObjectSchema)
	 */
	@WriteTransaction
	@Override
	public UserProfile update(UserProfile dto) throws DatastoreException,
			InvalidModelException, NotFoundException,
			ConflictingUpdateException {
		DBOUserProfile dbo = null;
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(DBOUserProfile.OWNER_ID_FIELD_NAME, dto.getOwnerId());
		try {
			dbo = namedJdbcTemplate.queryForObject(SELECT_FOR_UPDATE_SQL, param,
					USER_PROFILE_ROW_MAPPER);
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException(
					"The resource you are attempting to access cannot be found");
		}

		// Check dbo's etag against dto's etag
		// if different rollback and throw a meaningful exception
		if (!dbo.geteTag().equals(dto.getEtag())) {
			throw new ConflictingUpdateException(
					"Use profile was updated since you last fetched it, retrieve it again and reapply the update.");
		}
		UserProfileUtils.copyDtoToDbo(dto, dbo);
		// Update with a new e-tag
		dbo.seteTag(UUID.randomUUID().toString());

		// Send a UPDATE message
		transactionalMessenger.sendMessageAfterCommit("" + dbo.getOwnerId(),
				ObjectType.PRINCIPAL, dbo.geteTag(), ChangeType.UPDATE);

		boolean success = basicDao.update(dbo);
		if (!success)
			throw new DatastoreException(
					"Unsuccessful updating user profile in database.");

		UserProfile resultantDto = UserProfileUtils.convertDboToDto(dbo);

		return resultantDto;
	}

	/**
	 * This is called by Spring after all properties are set.
	 */
	// @WriteTransaction -- write transaction will not work here because this method 
	// is called directly on the bean rather than on the transaction proxy.
	public void bootstrapProfiles() {
		// Boot strap all users and groups
		if (this.userGroupDAO.getBootstrapPrincipals() == null) {
			throw new IllegalArgumentException(
					"bootstrapPrincipals users cannot be null");
		}

		// For each one determine if it exists, if not create it
		for (BootstrapPrincipal abs : this.userGroupDAO
				.getBootstrapPrincipals()) {
			if (abs.getId() == null)
				throw new IllegalArgumentException(
						"Bootstrap users must have an id");
			if (abs instanceof BootstrapUser) {
				UserProfile userProfile = null;
				try {
					userProfile = this.get(abs.getId().toString());
				} catch (NotFoundException nfe) {
					userProfile = new UserProfile();
					userProfile.setOwnerId(abs.getId().toString());
					this.doCreate(userProfile);
				}
			}
		}
	}

	@Override
	public String getPictureFileHandleId(String userId)
			throws NotFoundException {
		Long userIdLong = Long.parseLong(userId);
		try {
			Long fileHandleId = jdbcTemplate.queryForObject(
					SQL_SELECT_PROFILE_PIC_ID, Long.class, userIdLong);
			if(fileHandleId == null){
				throw new NotFoundException("User: " + userId
						+ " does not have a profile picture");
			}
			return fileHandleId.toString();
		} catch (EmptyResultDataAccessException e) {
			throw new NotFoundException("Unknown user: " + userId);
		}
	}

	@Override
	public List<UserNotificationInfo> getUserNotificationInfo(Set<String> ids) {
		if (ids.isEmpty()) {
			return new ArrayList<UserNotificationInfo>();
		}
		MapSqlParameterSource parameters = new MapSqlParameterSource("ids", ids);
		return namedJdbcTemplate.query(SQL_GET_USER_INFO_FOR_NOTIFICATION, parameters, new RowMapper<UserNotificationInfo>(){

			@Override
			public UserNotificationInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
				UserNotificationInfo user = new UserNotificationInfo();
				user.setFirstName(rs.getString(COL_USER_PROFILE_FIRST_NAME));
				user.setLastName(rs.getString(COL_USER_PROFILE_LAST_NAME));
				user.setUsername(rs.getString("USERNAME"));
				user.setNotificationEmail(rs.getString("EMAIL"));
				user.setUserId(rs.getString(COL_USER_PROFILE_ID));
				return user;
			}
		});
	}
}
