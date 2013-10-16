package org.sagebionetworks.repo.model.dbo.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.securitytools.HMACUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * A temporary DAO that interfaces with Crowd's DB Provides methods to migrate
 * data from Crowd's DB to the repo's DB Treat's Crowd's DB as read-only
 */
public class DBOCrowdMigrationDAO {

	@Autowired
	private AuthenticationDAO authDAO;

	@Autowired
	private UserGroupDAO userGroupDAO;

	@Autowired
	private UserProfileDAO userProfileDAO;

	@Autowired
	private GroupMembersDAO groupMembersDAO;
	
	@Autowired
	private DBOBasicDao basicDAO;

	@Autowired
	private SimpleJdbcTemplate simpleCrowdJdbcTemplate;
	
	// Tables to be read from, from Crowd's DB
	// Note: these names MUST be lowercase
	private static final String TABLE_CROWD_USER = "cwd_user";
	private static final String TABLE_CROWD_USER_ATTRIBUTE = "cwd_user_attribute";
	private static final String TABLE_CROWD_MEMBERSHIP = "cwd_membership";

	// Columns used from Crowd's DB
	private static final String COL_USER_NAME = "USER_NAME";
	private static final String COL_CREATED_DATE = "CREATED_DATE";
	private static final String COL_FIRST_NAME = "FIRST_NAME";
	private static final String COL_LAST_NAME = "LAST_NAME";
	private static final String COL_ATTRIBUTE_VALUE = "ATTRIBUTE_VALUE";
	private static final String COL_CREDENTIAL = "CREDENTIAL";
	private static final String COL_PARENT_NAME = "PARENT_NAME";

	// Parameters used in calls to Crowd's DB
	private static final String USERNAME_PARAM_NAME = "username";
	private static final String LIMIT_PARAM_NAME = "limit";
	private static final String OFFSET_PARAM_NAME = "offset";
	
	private static final String SELECT_USERS_PAGINATED = 
			"SELECT " + COL_USER_NAME + ", " + COL_CREATED_DATE + ", " + COL_FIRST_NAME+ ", " + COL_LAST_NAME +
			" FROM " + TABLE_CROWD_USER +
			" LIMIT :" + LIMIT_PARAM_NAME + " OFFSET :" + OFFSET_PARAM_NAME;
	
	private static final String SELECT_COUNT_OF_USERS = 
			"SELECT COUNT(" + COL_USER_NAME + ") FROM " + TABLE_CROWD_USER;
	
	private static final String SELECT_TOU_OF_USER = 
			"SELECT " + COL_ATTRIBUTE_VALUE + 
			" FROM " + TABLE_CROWD_USER + ", " + TABLE_CROWD_USER_ATTRIBUTE + 
			" WHERE " + TABLE_CROWD_USER + ".ID" + " = " + TABLE_CROWD_USER_ATTRIBUTE + ".USER_ID" + 
				" AND " + COL_USER_NAME + " = :" + USERNAME_PARAM_NAME + 
				" AND ATTRIBUTE_NAME = \"" + AuthorizationConstants.ACCEPTS_TERMS_OF_USE_ATTRIBUTE + "\"";
	
	private static final String SELECT_SECRET_KEY_OF_USER = 
			"SELECT " + COL_ATTRIBUTE_VALUE + 
			" FROM " + TABLE_CROWD_USER + ", " + TABLE_CROWD_USER_ATTRIBUTE + 
			" WHERE " + TABLE_CROWD_USER + ".ID" + " = " + TABLE_CROWD_USER_ATTRIBUTE + ".USER_ID" + 
				" AND " + COL_USER_NAME + " = :" + USERNAME_PARAM_NAME + 
				" AND ATTRIBUTE_NAME = \"" + AuthorizationConstants.CROWD_SECRET_KEY_ATTRIBUTE + "\"";
	
	private static final String SELECT_PASSWORD_OF_USER = 
			"SELECT " + COL_CREDENTIAL + " FROM " + TABLE_CROWD_USER + 
			" WHERE USER_NAME = :" + USERNAME_PARAM_NAME;
	
	private static final String SELECT_GROUPS_OF_USER = 
			"SELECT " + COL_PARENT_NAME + 
			" FROM " + TABLE_CROWD_USER + ", " + TABLE_CROWD_MEMBERSHIP + 
			" WHERE CHILD_ID = " + TABLE_CROWD_USER + ".ID" + 
				" AND GROUP_TYPE = \"GROUP\"" + 
				" AND " + COL_USER_NAME + " = :" + USERNAME_PARAM_NAME;
	
	private static final RowMapper<User> userRowMapper = new RowMapper<User>() {

		@Override
		public User mapRow(ResultSet rs, int rowNum) throws SQLException {
			User user = new User();
			user.setDisplayName(rs.getString(COL_USER_NAME));
			user.setCreationDate(rs.getDate(COL_CREATED_DATE));
			user.setFname(rs.getString(COL_FIRST_NAME));
			user.setLname(rs.getString(COL_LAST_NAME));
			return user;
		}
		
	};
	
	private static final RowMapper<String> groupNameRowMapper = new RowMapper<String>() {

		@Override
		public String mapRow(ResultSet rs, int rowNum) throws SQLException {
			return rs.getString(COL_PARENT_NAME);
		}
		
	};

	/**
	 * Returns a list of names of users residing in Crowd
	 * Note: Only the displayName, creationDate, firstName, and lastName fields 
	 *   of the returned users will be populated. 
	 */
	public List<User> getUsersFromCrowd(long limit, long offset) {
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(LIMIT_PARAM_NAME, limit);
		param.addValue(OFFSET_PARAM_NAME, offset);
		return simpleCrowdJdbcTemplate.query(
				SELECT_USERS_PAGINATED, userRowMapper, param);
	}
	
	/**
	 * Returns the number of users residing in Crowd
	 */
	public Long getCount() {
		return simpleCrowdJdbcTemplate.queryForLong(SELECT_COUNT_OF_USERS);
	}

	/**
	 * Migrates the user's info from Crowd into RDS
	 * Note: this method will not migrate users that do not exist in RDS
	 * @param user See {@link #getUsersFromCrowd(long, long)} for the expected, populated fields
	 * @return The user's ID in RDS, only if successful (otherwise null)
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public String migrateUser(User user) {
		if (!userGroupDAO.doesPrincipalExist(user.getDisplayName())) {
			return null;
		}
		
		try {
			// Get the user's ID in RDS
			UserGroup ug = userGroupDAO.findGroup(user.getDisplayName(), true);
			user.setId(ug.getId());
	
			// Create the rows in other tables if necessary
			ensureSecondaryRowsExist(user);
	
			// Convert the boolean ToU acceptance state in User to a timestamp in the UserProfile
			// This will coincidentally re-serialize the profile's blob via the non-deprecated method
			// See: https://sagebionetworks.jira.com/browse/PLFM-1756
			migrateToU(user);
	
			// Get the user's secret key, password hash, and group memberships and stash it
			migrateSecretKey(user);
			migratePasswordHash(user);
			migrateGroups(user);
			
		} catch (Exception e) {
			// Make sure the transaction is rolled back
			throw new RuntimeException(e);
		}
		
		return user.getId();
	}
	
	/////////////////////////////////////////////
	// Private helpers of the migration method //
	/////////////////////////////////////////////
	
	/**
	 * Creates a default UserProfile for the user, if necessary
	 * Also create a row for the user in the Credential table, if necessary
	 */
	protected void ensureSecondaryRowsExist(User user) throws InvalidModelException {
		Long principalId = Long.parseLong(user.getId());
		
		// User profile
		try {
			userProfileDAO.get(user.getId());
		} catch (NotFoundException e) {
			// Must make a new profile
			UserProfile userProfile = new UserProfile();
			userProfile.setOwnerId(user.getId());
			userProfile.setFirstName(user.getFname());
			userProfile.setLastName(user.getLname());
			userProfile.setDisplayName(user.getDisplayName());
			userProfileDAO.create(userProfile);
		}
		
		// Credentials
		try {
			MapSqlParameterSource param = new MapSqlParameterSource();
			param.addValue("principalId", principalId);
			basicDAO.getObjectByPrimaryKey(DBOCredential.class, param);
		} catch (NotFoundException e) {
			// Create a row for the authentication DAO
			DBOCredential credDBO = new DBOCredential();
			credDBO.setPrincipalId(principalId);
			credDBO.setSecretKey(HMACUtils.newHMACSHA1Key());
			basicDAO.createNew(credDBO);
		}
	}
	/**
	 * Converts the boolean user.isAgreesToTermsOfUse() to a timestamp stored in the UserProfile
	 */
	protected void migrateToU(User user) throws NotFoundException {
		// Find out whether the user has accepted the terms
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(USERNAME_PARAM_NAME, user.getDisplayName());
		Boolean acceptedToU = false;
		try {
			String toU = simpleCrowdJdbcTemplate.queryForObject(SELECT_TOU_OF_USER, String.class, param);
			acceptedToU = Boolean.parseBoolean(toU);
		} catch (EmptyResultDataAccessException e) { }

		// Migrate the boolean for the Terms of Use over
		// Note: it is implied that not having the ToU accepted 
		//   is equivalent to not having seen the ToU ever
		//   since the CrowdAuthUtils does not transmit that info to Crowd
		if (acceptedToU) {
			authDAO.setTermsOfUseAcceptance(user.getId(), acceptedToU);
		}
	}

	/**
	 * Copies the user's secret key from Crowd to RDS
	 */
	@SuppressWarnings("deprecation")
	protected void migrateSecretKey(User user) {
		// Get the key from Crowd
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(USERNAME_PARAM_NAME, user.getDisplayName());
		String secretKey = null;
		try {
			secretKey = simpleCrowdJdbcTemplate.queryForObject(
				SELECT_SECRET_KEY_OF_USER, String.class, param);
		} catch (EmptyResultDataAccessException e) { }
		
		// Stash the key in the AuthDAO
		if (secretKey != null) {
			authDAO.changeSecretKey(user.getId(), secretKey);
		}
	}

	/**
	 * Copies the user's password hash from Crowd to RDS
	 */
	protected void migratePasswordHash(User user) throws NotFoundException {
		// Get the hash from Crowd (should be non-null)
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(USERNAME_PARAM_NAME, user.getDisplayName());
		String passHash = null;
		try {
			passHash = simpleCrowdJdbcTemplate.queryForObject(
					SELECT_PASSWORD_OF_USER, String.class, param);
		} catch (EmptyResultDataAccessException e) { }

		authDAO.changePassword(user.getId(), passHash);
	}

	/**
	 * Synchronizes the group's memberships from Crowd to RDS
	 */
	protected void migrateGroups(User user) throws NotFoundException {
		// Get the parent groups of the user
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(USERNAME_PARAM_NAME, user.getDisplayName());
		List<String> parentGroups = simpleCrowdJdbcTemplate.query(SELECT_GROUPS_OF_USER, groupNameRowMapper, param);
		List<String> parentIds = new ArrayList<String>();
		
		// Parent groups must be created if necessary
		for (String parent : parentGroups) {
			parentIds.add(ensureGroupExists(parent));
		}
		
		List<String> userId = new ArrayList<String>();
		userId.add(user.getId());
		List<UserGroup> existing = groupMembersDAO.getUsersGroups(user.getId());
		List<UserGroup> newbies = userGroupDAO.get(parentIds);

		// Remove any groups the user is not part of
		Set<UserGroup> toDelete = new HashSet<UserGroup>(existing);
		toDelete.removeAll(newbies);
		for (UserGroup toRemove : toDelete) {
			groupMembersDAO.removeMembers(toRemove.getId(), userId);
		}

		// Add any groups the user is part of
		Set<UserGroup> toAdd = new HashSet<UserGroup>(newbies);
		toAdd.removeAll(existing);
		for (UserGroup toJoin : toAdd) {
			groupMembersDAO.addMembers(toJoin.getId(), userId);
		}
	}

	protected String ensureGroupExists(String groupName) {
		UserGroup ug = userGroupDAO.findGroup(groupName, false);
		if (ug == null) {
			ug = new UserGroup();
			ug.setName(groupName);
			ug.setIsIndividual(false);
			userGroupDAO.create(ug);
			ug = userGroupDAO.findGroup(groupName, false);
		}
		return ug.getId();
	}
}
