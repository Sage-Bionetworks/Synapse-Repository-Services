package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.authutil.CrowdAuthUtil;
import org.sagebionetworks.ids.NamedIdGenerator;
import org.sagebionetworks.ids.NamedIdGenerator.NamedType;
import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserGroup;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserProfile;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.securitytools.HMACUtils;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOCrowdMigrationDAOTest {

	@Autowired
	private DBOCrowdMigrationDAO crowdMigrationDAO;
	
	@Autowired
	private GroupMembersDAO groupMembersDAO;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private UserProfileDAO userProfileDAO;
	
	@Autowired
	private AuthenticationDAO authDAO;
	
	@Autowired
	private DBOBasicDao basicDAO;
	
	@Autowired
	private NamedIdGenerator idGenerator;
	
	private Set<String> originalGroupsInRDS;
	private List<String> groupsToDeleteFromRDS;
	private List<String> groupsToDeleteFromCrowd;
	private List<String> usersToDeleteFromCrowd;
	
	private String randUsername;
	private User user;
	private final String password = "super secure password";
	
	@Before
	public void setUp() throws Exception {
		originalGroupsInRDS = new HashSet<String>();
		Collection<UserGroup> originals = userGroupDAO.getAll();
		for (UserGroup ug : originals) {
			originalGroupsInRDS.add(ug.getId());
		}
		groupsToDeleteFromRDS = new ArrayList<String>();
		groupsToDeleteFromCrowd = new ArrayList<String>();
		usersToDeleteFromCrowd = new ArrayList<String>();
		
		// Used by most of the tests
		randUsername = "SomeRandomUser" + UUID.randomUUID();
		user = new User();
		user.setDisplayName(randUsername);
	}

	@After
	public void tearDown() throws Exception {
		Collection<UserGroup> polluted = userGroupDAO.getAll();
		for (UserGroup ug : polluted) {
			if (!originalGroupsInRDS.contains(ug.getId())) {
				groupsToDeleteFromRDS.add(ug.getId());
			}
		}
		
		if(groupsToDeleteFromRDS != null) {
			for(String todelete: groupsToDeleteFromRDS){
				deleteTestGroupFromRDS(todelete);
			}
		}
		if(groupsToDeleteFromCrowd != null) {
			for(String todelete: groupsToDeleteFromCrowd){
				deleteTestGroupFromCrowd(todelete);
			}
		}
		if(usersToDeleteFromCrowd != null){
			for(String todelete: usersToDeleteFromCrowd) {
				CrowdAuthUtil.deleteUser(todelete);
			}
		}
	}
	
	private void deleteTestGroupFromRDS(String nameOrId) throws Exception {
		try {
			userGroupDAO.delete(nameOrId);
		} catch (NotFoundException e) {
			// Good, not in DB
		}
	}
	
	private void deleteTestGroupFromCrowd(String nameOrId) throws Exception {
		try {
			CrowdAuthUtil.deleteGroup(nameOrId);
		} catch (AuthenticationException e) {
			if (e.getRespStatus() == 404) {
				// Good, the group doesn't exist
			} else {
				throw e;
			}
		}
	}
	
	private void createCrowdUser(String username) throws Exception {
		// Add those users to Crowd
		NewUser crowdUser = new NewUser();
		crowdUser.setFirstName("bogus");
		crowdUser.setLastName("bogus");
		crowdUser.setDisplayName(username);
		crowdUser.setEmail(username);
		crowdUser.setPassword(password);
		try {
			CrowdAuthUtil.createUser(crowdUser);
		} catch (AuthenticationException e) {
			if (e.getRespStatus() == 400) {
				// User already present
			} else {
				throw e;
			}
		}
		usersToDeleteFromCrowd.add(crowdUser.getEmail());
	}
	
	private void createCrowdGroup(String name) throws Exception {
		try {
			CrowdAuthUtil.createGroup(name);
		} catch (AuthenticationException e) {
			if (e.getRespStatus() == 400) {
				// Good, the group already exists
			} else {
				throw e;
			}
		}
		groupsToDeleteFromCrowd.add(name);
	}
	
	private UserGroup createRDSUser(String username) throws Exception {
		// Add some users to the DB
		UserGroup ug = new UserGroup();
		ug.setName(username);
		ug.setIsIndividual(true);
		try {
			ug.setId(userGroupDAO.create(ug)); 
		} catch (DatastoreException e) {
			ug.setId(userGroupDAO.findGroup(username, true).getId());
		}
		groupsToDeleteFromRDS.add(ug.getId());
		return ug;
	}
	
	private UserGroup createRDSGroup(String name) throws Exception {
		UserGroup group = new UserGroup();
		group.setName(name);
		group.setIsIndividual(false);
		String id = null;
		try {
			id = userGroupDAO.create(group);
		} catch (DatastoreException e) {
			// Already exists
			id = userGroupDAO.findGroup(name, false).getId();
		}
		assertNotNull(id);
		groupsToDeleteFromRDS.add(id);
		return userGroupDAO.get(id);
	}

	@Test
	public void testGetAllUsers() throws Exception {
		Long numUsers = crowdMigrationDAO.getCount();
		List<User> users = new ArrayList<User>();
		
		for (int i = 0; i < numUsers; i++) {
			users.addAll(crowdMigrationDAO.getUsersFromCrowd(1, i));
		}
		
		Assert.assertEquals(numUsers, new Long(users.size()));
	}

	@Test
	public void testAbortNotInRDS() throws Exception {
		createCrowdUser(randUsername);
		String userId = crowdMigrationDAO.migrateUser(user);
		Assert.assertNull(userId);
	}
	
	@Test
	public void testMigrationIdempotent() throws Exception {
		createCrowdUser(randUsername);
		createRDSUser(randUsername);
		
		// Migrate once
		crowdMigrationDAO.migrateUser(user);
		
		// Check for correctness
		UserProfile userProfile = userProfileDAO.get(user.getId());
		String secretKey = authDAO.getSecretKey(user.getId());
		String passHash = PBKDF2Utils.hashPassword(password, authDAO.getPasswordSalt(randUsername));
		authDAO.checkEmailAndPassword(randUsername, passHash);
		
		// Migrate again
		crowdMigrationDAO.migrateUser(user);

		// The values should remain the same
		Assert.assertEquals(userProfile, userProfileDAO.get(user.getId()));
		Assert.assertEquals(secretKey, authDAO.getSecretKey(user.getId()));
		passHash = PBKDF2Utils.hashPassword(password, authDAO.getPasswordSalt(randUsername));
		authDAO.checkEmailAndPassword(randUsername, passHash);
	}

	@Test
	public void testEnsureSecondaryTablesExist() throws Exception {
		createCrowdUser(randUsername);

		// After migration, users may not have entries in the secondary tables of UserGroup
		//   Having just a row in the UserGroup table breaks a few assumptions made by the recently added tables/DAOs
		// For new users, this assumption is enforced by UserGroup.create() or by the UserManager
		DBOUserGroup ug = new DBOUserGroup();
		ug.setId(idGenerator.generateNewId(randUsername, NamedType.USER_GROUP_ID));
		ug.setName(randUsername);
		ug.setEtag(UUID.randomUUID().toString());
		ug = basicDAO.createNew(ug);
		groupsToDeleteFromRDS.add(ug.getId().toString());

		long startUserProfileCount = basicDAO.getCount(DBOUserProfile.class);
		long startCredentialCount = basicDAO.getCount(DBOCredential.class);
		
		user.setId(ug.getId().toString());
		crowdMigrationDAO.ensureSecondaryRowsExist(user);

		// There should be one more of each row
		Assert.assertEquals(startUserProfileCount + 1, basicDAO.getCount(DBOUserProfile.class));
		Assert.assertEquals(startCredentialCount + 1, basicDAO.getCount(DBOCredential.class));
	}

	@Test
	public void testMigrateToU_true() throws Exception {
		createCrowdUser(randUsername);
		UserGroup ug = createRDSUser(randUsername);
		CrowdAuthUtil.setAcceptsTermsOfUse(randUsername, true);
		
		user.setId(ug.getId());
		user.setCreationDate(new Date());
		crowdMigrationDAO.ensureSecondaryRowsExist(user);
		crowdMigrationDAO.migrateToU(user);
		
		UserProfile userProfile = userProfileDAO.get(user.getId());
		Assert.assertTrue(userProfile.getAgreesToTermsOfUse() >= AuthorizationConstants.MOST_RECENT_TERMS_OF_USE);
	}

	@Test
	public void testMigrateToU_false() throws Exception {
		// Due to the way our system is built, not accepting the terms of use is never transmitted to Crowd
		// Nevertheless, it could be a case to consider
		createCrowdUser(randUsername);
		UserGroup ug = createRDSUser(randUsername);
		CrowdAuthUtil.setAcceptsTermsOfUse(randUsername, false);
		
		user.setId(ug.getId());
		user.setCreationDate(ug.getCreationDate());
		crowdMigrationDAO.ensureSecondaryRowsExist(user);
		crowdMigrationDAO.migrateToU(user);
		
		UserProfile userProfile = userProfileDAO.get(user.getId());
		Assert.assertTrue(userProfile.getAgreesToTermsOfUse() < AuthorizationConstants.MOST_RECENT_TERMS_OF_USE);
	}

	@Test
	public void testMigrateToU_blank() throws Exception {
		createCrowdUser(randUsername);
		UserGroup ug = createRDSUser(randUsername);
		
		user.setId(ug.getId());
		user.setCreationDate(ug.getCreationDate());
		crowdMigrationDAO.ensureSecondaryRowsExist(user);
		crowdMigrationDAO.migrateToU(user);
		
		UserProfile userProfile = userProfileDAO.get(user.getId());
		Assert.assertTrue(userProfile.getAgreesToTermsOfUse() < AuthorizationConstants.MOST_RECENT_TERMS_OF_USE);
	}

	@Test
	public void testMigrateSecretKey() throws Exception {
		createCrowdUser(randUsername);
		UserGroup ug = createRDSUser(randUsername);
		
		// Create the secret key
		// Note: this is how the Authentication controller handles secret keys
		Map<String,Collection<String>> userAttributes = 
				 new HashMap<String,Collection<String>>(CrowdAuthUtil.getUserAttributes(randUsername));
		Collection<String> secretKeyCollection = userAttributes.get(AuthorizationConstants.CROWD_SECRET_KEY_ATTRIBUTE);
		String secretKey = HMACUtils.newHMACSHA1Key();
		secretKeyCollection = new HashSet<String>();
		secretKeyCollection.add(secretKey);
		userAttributes.put(AuthorizationConstants.CROWD_SECRET_KEY_ATTRIBUTE, secretKeyCollection);
		CrowdAuthUtil.setUserAttributes(randUsername, userAttributes);
		
		// Migrate the key over
		user.setId(ug.getId());
		crowdMigrationDAO.migrateSecretKey(user);
		
		Assert.assertEquals(secretKey, authDAO.getSecretKey(ug.getId()));
	}

	@Test
	public void testMigrateSecretKey_blank() throws Exception {
		createCrowdUser(randUsername);
		UserGroup ug = createRDSUser(randUsername);
		
		String secretKey = authDAO.getSecretKey(ug.getId());
		
		// Migrate the non-existent key over
		user.setId(ug.getId());
		crowdMigrationDAO.migrateSecretKey(user);
		
		// Key should be unchanged
		Assert.assertEquals(secretKey, authDAO.getSecretKey(ug.getId()));
	}
	
	@Test
	public void testMigratePasswordHash() throws Exception {
		createCrowdUser(randUsername);
		UserGroup ug = createRDSUser(randUsername);
		
		// Migrate the password hash
		user.setId(ug.getId());
		crowdMigrationDAO.migratePasswordHash(user);
		
		// Now our super secure password is in RDS
		// So this should not throw an UnauthorizedException
		String passHash = PBKDF2Utils.hashPassword(password, authDAO.getPasswordSalt(randUsername));
		authDAO.checkEmailAndPassword(randUsername, passHash);
	}
	
	@Test
	public void testMigrateGroups_NewGroup() throws Exception {
		// Make a group that must be created in RDS by the migrator
		String VIPGroup = "testGroup-FancyLanguages";
		createCrowdGroup(VIPGroup);
		createCrowdUser(randUsername);
		UserGroup ug = createRDSUser(randUsername);
		CrowdAuthUtil.addUserToGroup(VIPGroup, randUsername);

		user.setId(ug.getId());
		crowdMigrationDAO.migrateGroups(user);
		
		// Verify that everything was transferred
		assertTrue(userGroupDAO.doesPrincipalExist(VIPGroup));
		UserGroup notSoExclusiveAnymore = userGroupDAO.findGroup(VIPGroup, false);
		groupsToDeleteFromRDS.add(notSoExclusiveAnymore.getId());
		
		List<UserGroup> clubby = groupMembersDAO.getMembers(notSoExclusiveAnymore.getId());
		assertEquals("There should be one member", 1, clubby.size());
		assertEquals("The one member should be the Spanlish one", randUsername, clubby.get(0).getName());
	}
	
	@Test
	public void testMigrateGroups_Addition() throws Exception {
		String stompy = "testGroupIntrudedUpon";
		String lonely = "testGroupAbandoned";
		
		// This group will get an extra member
		UserGroup stompedUpon = createRDSGroup(stompy);
		UserGroup squishy = createRDSUser("IGetSquishedSoon");
		List<String> adder = new ArrayList<String>();
		adder.add(squishy.getId());
		groupMembersDAO.addMembers(stompedUpon.getId(), adder);
		
		// This Crowd group takes precedence over RDS groups
		createCrowdGroup(stompy);
		createCrowdUser(randUsername);
		UserGroup ug = createRDSUser(randUsername);
		CrowdAuthUtil.addUserToGroup(stompy, randUsername);
		
		// This group will lose a member
		UserGroup abandoned = createRDSGroup(lonely);
		adder.clear();
		adder.add(ug.getId());
		groupMembersDAO.addMembers(abandoned.getId(), adder);

		user.setId(ug.getId());
		crowdMigrationDAO.migrateGroups(user);
		
		List<UserGroup> stomped = groupMembersDAO.getMembers(stompedUpon.getId());
		assertEquals("Stomped group should only have two members", 2, stomped.size());
		
		List<UserGroup> noMembers = groupMembersDAO.getMembers(abandoned.getId());
		assertEquals("Abandoned group should have no members", 0, noMembers.size());
		
		assertTrue("Squished user should still exist", userGroupDAO.get(squishy.getId()) != null);
	}
}
