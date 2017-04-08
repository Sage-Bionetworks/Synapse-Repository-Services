package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.broadcast.UserNotificationInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.message.Settings;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.BootstrapPrincipal;
import org.sagebionetworks.repo.model.principal.BootstrapUser;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOUserProfileDAOImplTest {

	@Autowired 
	UserGroupDAO userGroupDAO;
	
	@Autowired
	UserProfileDAO userProfileDAO;
	
	@Autowired
	FileHandleDao fileHandleDao;

	@Autowired
	PrincipalAliasDAO principalAliasDAO;

	@Autowired
	private NotificationEmailDAO notificationEmailDAO;

	@Autowired
	private IdGenerator idGenerator;
	
	private UserGroup principal = null;
	private UserGroup principal2 = null;
	
	private List<UserGroup> principalToDelete = null;
	private List<String> fileHandlesToDelete = null;
	
	private List<String> toDelete;
	
	@Before
	public void setUp() throws Exception {
		principal = new UserGroup();
		principal.setIsIndividual(true);
		principal.setCreationDate(new Date());
		principal.setId(userGroupDAO.create(principal).toString());
		principal2 = new UserGroup();
		principal2.setIsIndividual(true);
		principal2.setCreationDate(new Date());
		principal2.setId(userGroupDAO.create(principal2).toString());
		toDelete = new LinkedList<String>();
		principalToDelete = new ArrayList<UserGroup>();
		for (int i=0; i<2; i++) {
			UserGroup individualGroup = new UserGroup();
			individualGroup.setIsIndividual(true);
			individualGroup.setCreationDate(new Date());
			individualGroup.setId(userGroupDAO.create(individualGroup).toString());
			principalToDelete.add(individualGroup);
		}
		fileHandlesToDelete = new LinkedList<String>();
	}
		
	
	@After
	public void tearDown() throws Exception{
		for (UserGroup ug : principalToDelete) {
			// this will delete the user profile too
			userGroupDAO.delete(ug.getId());
		}
		if(toDelete != null){
			for(String id: toDelete){
				try {
					userProfileDAO.delete(id);
				} catch (Exception e) {}
			}
		}
		if(fileHandlesToDelete != null){
			for(String id:fileHandlesToDelete){
				try {
					fileHandleDao.delete(id);
				} catch (Exception e) {}
			}
		}
		principalToDelete.clear();
	}
	
	/**
	 * Helper to create a new profile using the first user.
	 * @return
	 */
	private UserProfile createUserProfile(){
		UserProfile userProfile = new UserProfile();
		userProfile.setOwnerId(principal.getId());
		userProfile.setFirstName("foo");
		userProfile.setLastName("bar");
		userProfile.setRStudioUrl("http://rstudio.com");
		userProfile.setEtag(NodeConstants.ZERO_E_TAG);
		return userProfile;
	}
	
	@Test
	public void testCreateNoSettings() throws Exception{
		// Create a new user profile without settings
		UserProfile userProfile = createUserProfile();
		// Create it
		String id = userProfileDAO.create(userProfile);
		assertNotNull(id);
		
		// Fetch it
		UserProfile clone = userProfileDAO.get(id);
		assertNotNull(clone);
		assertNotNull(clone.getNotificationSettings());
		// should default to true.
		assertTrue(clone.getNotificationSettings().getSendEmailNotifications());
	}
	
	@Test
	public void testCreateWithEmailFalse() throws Exception{
		// Create a new user profile with settings
		UserProfile userProfile = createUserProfile();
		userProfile.setNotificationSettings(new Settings());
		userProfile.getNotificationSettings().setSendEmailNotifications(false);
		// Create it
		String id = userProfileDAO.create(userProfile);
		assertNotNull(id);
		
		// Fetch it
		UserProfile clone = userProfileDAO.get(id);
		assertNotNull(clone);
		assertNotNull(clone.getNotificationSettings());
		assertFalse(clone.getNotificationSettings().getSendEmailNotifications());
	}
	
	@Test
	public void testUpdateWithNullSettings() throws Exception{
		// Create a new user profile without settings
		UserProfile userProfile = createUserProfile();
		// Create it
		String id = userProfileDAO.create(userProfile);
		assertNotNull(id);
		
		// Fetch it
		UserProfile clone = userProfileDAO.get(id);
		assertNotNull(clone);
		assertNotNull(clone.getNotificationSettings());
		// should default to true.
		assertTrue(clone.getNotificationSettings().getSendEmailNotifications());
		
		// clear the settings
		clone.setNotificationSettings(null);
		userProfileDAO.update(clone);
		// should default back to true
		clone = userProfileDAO.get(id);
		assertNotNull(clone.getNotificationSettings());
		assertTrue(clone.getNotificationSettings().getSendEmailNotifications());
	}
	
	@Test
	public void testUpdateWithEmailFalse() throws Exception{
		// Create a new user profile without settings
		UserProfile userProfile = createUserProfile();
		// Create it
		String id = userProfileDAO.create(userProfile);
		assertNotNull(id);
		
		// Fetch it
		UserProfile clone = userProfileDAO.get(id);
		assertNotNull(clone);
		assertNotNull(clone.getNotificationSettings());
		// should default to true.
		assertTrue(clone.getNotificationSettings().getSendEmailNotifications());
		
		// clear the settings
		assertNotNull(clone.getNotificationSettings());
		clone.getNotificationSettings().setSendEmailNotifications(false);
		userProfileDAO.update(clone);
		// should updated the value.
		clone = userProfileDAO.get(id);
		assertNotNull(clone.getNotificationSettings());
		assertFalse(clone.getNotificationSettings().getSendEmailNotifications());
	}
	
	@Test
	public void testNullNames(){
		UserProfile userProfile = createUserProfile();
		userProfile.setFirstName(null);
		userProfile.setLastName(null);
		// Create it
		String id = userProfileDAO.create(userProfile);
		assertNotNull(id);
		
		// Fetch it
		UserProfile clone = userProfileDAO.get(id);
		assertNotNull(clone);
		assertEquals(userProfile.getFirstName(), clone.getFirstName());
		assertEquals(userProfile.getLastName(), clone.getLastName());
	}
	
	@Test
	public void testUnicodeNames(){
		UserProfile userProfile = createUserProfile();
		userProfile.setFirstName("बंदर बट");
		userProfile.setLastName("Völlerei lässt grüßen");
		// Create it
		String id = userProfileDAO.create(userProfile);
		assertNotNull(id);
		
		// Fetch it
		UserProfile clone = userProfileDAO.get(id);
		assertNotNull(clone);
		assertEquals(userProfile.getFirstName(), clone.getFirstName());
		assertEquals(userProfile.getLastName(), clone.getLastName());
	}
	
	@Test
	public void testCRUD() throws Exception{
		List<UserProfile> userProfiles = new ArrayList<UserProfile>();
		long initialCount = userProfileDAO.getCount();
		// Create it
		for (UserGroup ug : principalToDelete) {
			// Create a new user profile
			UserProfile userProfile = new UserProfile();
			userProfile.setOwnerId(ug.getId());
			userProfile.setFirstName("foo");
			userProfile.setLastName("bar");
			userProfile.setRStudioUrl("http://rstudio.com");
			userProfile.setEtag(NodeConstants.ZERO_E_TAG);
			userProfiles.add(userProfile);
			userProfile.setNotificationSettings(new Settings());
			userProfile.getNotificationSettings().setSendEmailNotifications(true);
			// Create it
			String id = userProfileDAO.create(userProfile);
			assertNotNull(id);
			userProfile.setOwnerId(id);
		}
		
		assertEquals(userProfiles.size()+initialCount, userProfileDAO.getCount());
		
		// Fetch it
		UserProfile userProfile = userProfiles.get(0);
		String id = userProfile.getOwnerId();
		UserProfile clone = userProfileDAO.get(id);
		assertNotNull(clone);
		assertEquals(userProfile, clone);
		
		Long idLong1 = Long.parseLong(userProfiles.get(1).getOwnerId());
		Long idLong0 = Long.parseLong(userProfiles.get(0).getOwnerId());
		List<UserProfile> listed = userProfileDAO.list(Arrays.asList(new Long[]{idLong1, idLong0}));
		assertEquals(2, listed.size());
		assertEquals(Arrays.asList(new UserProfile[]{userProfiles.get(1), userProfiles.get(0)}), listed);
		try {
			userProfileDAO.list(Arrays.asList(new Long[]{idLong1, 87765443L+idLong0}));
			fail("NotFoundException expected");
		} catch (NotFoundException e) {
			//as expected
		}

		// Update it
		UserProfile updatedProfile = userProfileDAO.update(clone);
		assertTrue("etags should be different after an update", !clone.getEtag().equals(updatedProfile.getEtag()));

		try {
			clone.setFirstName("This Should Fail");
			userProfileDAO.update(clone);
			fail("conflicting update exception not thrown");
		}
		catch(ConflictingUpdateException e) {
			// We expected this exception
		}

		// Delete it
		for (UserProfile up: userProfiles) {
			userProfileDAO.delete(up.getOwnerId());
		}

		assertEquals(initialCount, userProfileDAO.getCount());
	}
	
	@Test
	public void testGetPictureFileHandleIdNotFound(){
		// Create a new type
		UserProfile userProfile = createUserProfile();
		// Create it
		String id = userProfileDAO.create(userProfile);
		assertNotNull(id);
		toDelete.add(id);
		try {
			userProfileDAO.getPictureFileHandleId(id);
			fail("Should have failed");
		} catch (NotFoundException e) {
			assertTrue(e.getMessage().contains(id));
		}
	}
	
	@Test
	public void testGetPictureFileHandleId() throws NotFoundException{
		ExternalFileHandle ef = new ExternalFileHandle();
		ef.setExternalURL("http://google.com");
		ef.setCreatedBy(principal.getId());
		ef.setCreatedOn(new Date());
		ef.setFileName("Some name");
		ef.setEtag(UUID.randomUUID().toString());
		ef.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		ef = (ExternalFileHandle) fileHandleDao.createFile(ef);
		fileHandlesToDelete.add(ef.getId());
		// Create a new type
		UserProfile userProfile = createUserProfile();
		userProfile.setProfilePicureFileHandleId(ef.getId());
		// Create it
		String id = userProfileDAO.create(userProfile);
		assertNotNull(id);
		toDelete.add(id);
		String fileId = userProfileDAO.getPictureFileHandleId(id);
		assertEquals(ef.getId(), fileId);
	}
	
	@Test
	public void testBootstrapUsers() throws DatastoreException, NotFoundException{
		List<BootstrapPrincipal> boots = this.userGroupDAO.getBootstrapPrincipals();
		assertNotNull(boots);
		assertTrue(boots.size() >0);
		// Each should exist
		for(BootstrapPrincipal bootUg: boots){
			if(bootUg instanceof BootstrapUser){
				UserProfile profile = userProfileDAO.get(bootUg.getId().toString());
				userGroupDAO.get(bootUg.getId());
				assertEquals(bootUg.getId().toString(), profile.getOwnerId());
			}
		}
	}

	@Test
	public void testGetUserNotificationInfoAllReceiveNotification() {
		String user1 = createUserWithNotificationEmail(Long.parseLong(principal.getId()),
				"username", "first", "last", "first@domain.org", true);
		String user2 = createUserWithNotificationEmail(Long.parseLong(principal2.getId()),
				"username2", "first2", "last2", "second@domain.org", true);

		Set<String> ids = new HashSet<String>();
		ids.addAll(Arrays.asList(user1, user2));
		List<UserNotificationInfo> results = userProfileDAO.getUserNotificationInfo(ids);
		assertEquals(2, results.size());
		UserNotificationInfo userInfo1 = createUserNotificationInfo(user1, "first", "last", "username", "first@domain.org");
		UserNotificationInfo userInfo2 = createUserNotificationInfo(user2, "first2", "last2", "username2", "second@domain.org");
		assertTrue(results.contains(userInfo1));
		assertTrue(results.contains(userInfo2));

		principalAliasDAO.removeAllAliasFromPrincipal(Long.parseLong(principal.getId()));
		principalAliasDAO.removeAllAliasFromPrincipal(Long.parseLong(principal2.getId()));
	}

	private UserNotificationInfo createUserNotificationInfo(String user1, String firstName,
			String lastName, String username, String email) {
		UserNotificationInfo userInfo = new UserNotificationInfo();
		userInfo.setFirstName(firstName);
		userInfo.setLastName(lastName);
		userInfo.setUsername(username);
		userInfo.setUserId(user1);
		userInfo.setNotificationEmail(email);
		return userInfo;
	}

	@Test
	public void testGetUserNotificationInfoWithUserDoNotReceiveNotification() {
		String user1 = createUserWithNotificationEmail(Long.parseLong(principal.getId()),
				"username", "first", "last", "first@domain.org", true);
		String user2 = createUserWithNotificationEmail(Long.parseLong(principal2.getId()),
				"username2", "first2", "last2", "second@domain.org", false);

		Set<String> ids = new HashSet<String>();
		ids.addAll(Arrays.asList(user1, user2));
		List<UserNotificationInfo> results = userProfileDAO.getUserNotificationInfo(ids);
		assertEquals(1, results.size());
		UserNotificationInfo userInfo = createUserNotificationInfo(user1, "first", "last", "username", "first@domain.org");
		assertTrue(results.contains(userInfo));

		principalAliasDAO.removeAllAliasFromPrincipal(Long.parseLong(principal.getId()));
		principalAliasDAO.removeAllAliasFromPrincipal(Long.parseLong(principal2.getId()));
	}

	private String createUserWithNotificationEmail(Long principalId, String username,
			String firstName, String lastName, String email, Boolean receiveNotification) {
		Settings setting = new Settings();
		setting.setSendEmailNotifications(receiveNotification);
		UserProfile userProfile = new UserProfile();
		userProfile.setOwnerId(principalId.toString());
		userProfile.setFirstName(firstName);
		userProfile.setLastName(lastName);
		userProfile.setNotificationSettings(setting);
		String user = userProfileDAO.create(userProfile);
		PrincipalAlias usernameAlias = new PrincipalAlias();
		usernameAlias.setAlias(username);
		usernameAlias.setPrincipalId(principalId);
		usernameAlias.setType(AliasType.USER_NAME);
		principalAliasDAO.bindAliasToPrincipal(usernameAlias);
		PrincipalAlias emailAlias = new PrincipalAlias();
		emailAlias.setAlias(email);
		emailAlias.setPrincipalId(principalId);
		emailAlias.setType(AliasType.USER_EMAIL);
		notificationEmailDAO.create(principalAliasDAO.bindAliasToPrincipal(emailAlias));
		return user;
	}
}
