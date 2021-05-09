package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ProjectHeader;
import org.sagebionetworks.repo.model.ProjectHeaderList;
import org.sagebionetworks.repo.model.ProjectListSortColumn;
import org.sagebionetworks.repo.model.ProjectListType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.entity.query.SortDirection;
import org.sagebionetworks.repo.model.message.Settings;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Sets;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class UserProfileManagerImplTest {

	@Autowired
	private UserManager userManager;

	@Autowired
	private UserProfileDAO userProfileDAO;

	@Autowired
	private UserProfileManager userProfileManager;

	@Autowired
	private EntityManager entityManager;
	@Autowired
	private EntityAclManager entityAclManager;

	private static final String USER_NAME = "foobar";
	private static final String USER_EMAIL = "foo@bar.com";
	private Long userId;
	private Long userIdTwo;
	UserInfo admin;
	UserInfo userInfo;
	UserInfo userInfoTwo;
	UserInfo anonymous;

	List<String> projectsToDelete;
	List<Long> usersToDelete;

	@Before
	public void setUp() throws Exception {
		admin = userManager.getUserInfo(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		anonymous = userManager.getUserInfo(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		usersToDelete = new LinkedList<>();
		projectsToDelete = new LinkedList<>();
		NewUser user = new NewUser();
		user.setEmail(USER_EMAIL);
		user.setFirstName("Foo");
		user.setLastName("Bar");
		user.setUserName(USER_NAME);
		userId = userManager.createUser(user);
		userInfo = userManager.getUserInfo(userId);
		userInfo.setAcceptsTermsOfUse(true);
		usersToDelete.add(userId);

		user = new NewUser();
		user.setEmail("doubleohhseven@gmail.com");
		user.setFirstName("James");
		user.setLastName("Bond");
		user.setUserName("doubleohhseven");
		userIdTwo = userManager.createUser(user);
		userInfoTwo = userManager.getUserInfo(userIdTwo);
		usersToDelete.add(userIdTwo);
	}

	@After
	public void tearDown() throws Exception {
		Collections.reverse(projectsToDelete);
		for (String entityId : projectsToDelete) {
			entityManager.deleteEntity(admin, entityId);
		}
		for (Long userId : usersToDelete) {
			userManager.deletePrincipal(admin, userId);
		}
	}

	@Test
	public void testCRU() throws DatastoreException, UnauthorizedException, NotFoundException {
		// delete the existing user profile so we can create our own
		userProfileDAO.delete(userId.toString());

		// Create a new UserProfile
		Long principalId = Long.parseLong(this.userId.toString());
		UserProfile created;
		{
			UserProfile profile = new UserProfile();
			profile.setCompany("Spies 'R' Us");
			profile.setFirstName("James");
			profile.setLastName("Bond");
			profile.setOwnerId(this.userId.toString());
			profile.setUserName(USER_NAME);
			Settings settings = new Settings();
			settings.setSendEmailNotifications(true);
			profile.setNotificationSettings(settings);
			// Create the profile
			created = this.userProfileManager.createUserProfile(profile);
			// the changed fields are etag and emails (which are ignored)
			// set these fields in 'profile' so we can compare to 'created'
			profile.setEmails(Collections.singletonList(USER_EMAIL));
			profile.setOpenIds(new ArrayList<String>());
			profile.setUserName(USER_NAME);
			profile.setEtag(created.getEtag());
			profile.setCreatedOn(created.getCreatedOn());
			assertEquals(profile, created);
		}
		assertNotNull(created);
		assertNotNull(created.getEtag());

		UserInfo userInfo = new UserInfo(false, principalId);
		// Get it back
		UserProfile clone = userProfileManager.getUserProfile(principalId.toString());
		assertEquals(created, clone);

		// Make sure we can update it
		created.setUserName("newUsername");
		String startEtag = created.getEtag();
		// Changing emails is currently disabled See
		UserProfile updated = userProfileManager.updateUserProfile(userInfo, created);
		assertFalse("Update failed to update the etag", startEtag.equals(updated.getEtag()));
		// Get it back
		clone = userProfileManager.getUserProfile(principalId.toString());
		assertEquals(updated, clone);
		assertEquals("newUsername", clone.getUserName());

	}

	// Note: In PLFM-2486 we allow the client to change the emails passed in, we
	// just ignore them
	@Test
	public void testPLFM_2504() throws DatastoreException, UnauthorizedException, NotFoundException {
		// delete the existing user profile so we can create our own
		userProfileDAO.delete(userId.toString());

		// Create a new UserProfile
		Long principalId = Long.parseLong(this.userId.toString());
		UserProfile profile = new UserProfile();
		profile.setCompany("Spies 'R' Us");
		profile.setEmails(new LinkedList<String>());
		profile.getEmails().add("jamesBond@spies.org");
		profile.setUserName("007");
		profile.setOwnerId(this.userId.toString());
		// Create the profile
		profile = this.userProfileManager.createUserProfile(profile);
		assertNotNull(profile);
		assertNotNull(profile.getUserName());
		assertNotNull(profile.getEtag());

		UserInfo userInfo = new UserInfo(false, principalId);
		// Get it back
		UserProfile clone = userProfileManager.getUserProfile(principalId.toString());
		assertEquals(profile, clone);
		assertEquals(Collections.singletonList(USER_EMAIL), clone.getEmails());

		// try to update it
		profile.getEmails().clear();
		profile.getEmails().add("myNewEmail@spies.org");
		String startEtag = profile.getEtag();
		// update
		// OK to change emails, as any changes to email are ignored
		profile = userProfileManager.updateUserProfile(userInfo, profile);
		assertEquals(Collections.singletonList(USER_EMAIL), profile.getEmails());
	}

	@Test(expected = NotFoundException.class)
	public void testGetPicturePresignedUrlNotFound() throws Exception {
		String userIdString = "" + userId;
		// get the presigned url for this handle
		assertNotNull(userProfileManager.getUserProfileImageUrl(userInfo, userIdString));
	}

	@Test
	public void testPLFM_4554() throws Exception {
		// User one creates a project
		Project userOnesProject = createProject("userOneProject", userInfo);

		// Share the project with public
		grantReadAcess(
				AuthorizationConstants.BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId(),
				userOnesProject.getId());

		// Public projects should not appear on user two's list.
		List<ProjectHeader> headers = getProjects(userInfoTwo, userInfoTwo);
		assertNotNull(headers);
		assertTrue(headers.isEmpty());

		// Grant user two read access to the project.
		grantReadAcess(userIdTwo, userOnesProject.getId());

		// User two should now have one project.
		headers = getProjects(userInfoTwo, userInfoTwo);
		assertNotNull(headers);
		assertEquals(1, headers.size());
		
		// call under test
		headers = getProjects(anonymous, userInfoTwo);
		assertNotNull(headers);
		assertEquals(1, headers.size());
		/*
		 * Anonymous should be able to see the public project explicitly shared with
		 * user two.
		 */
		assertEquals(userOnesProject.getId(), headers.get(0).getId());
	}

	/**
	 * Get the projects for
	 * 
	 * @param caller
	 * @param lookingAt
	 * @return
	 */
	private List<ProjectHeader> getProjects(UserInfo caller, UserInfo lookingAt) {
		Long teamId = null;
		ProjectListType type = ProjectListType.ALL;
		ProjectListSortColumn sortColumn = ProjectListSortColumn.PROJECT_NAME;
		SortDirection sortDirection = SortDirection.ASC;
		ProjectHeaderList paginated = userProfileManager.getProjects(caller, lookingAt, teamId, type,
				sortColumn, sortDirection, (new NextPageToken(null)).toToken());
		if (paginated != null) {
			return paginated.getResults();
		}
		return null;
	}

	/**
	 * Helper to create a project.
	 * 
	 * @param name
	 * @param creator
	 * @return
	 */
	private Project createProject(String name, UserInfo creator) {
		String activityId = null;
		Project project = new Project();
		project.setName(name);
		String id = entityManager.createEntity(creator, project, activityId);
		projectsToDelete.add(id);
		return entityManager.getEntity(creator, id, Project.class);
	}

	/**
	 * Helper to grant read access to an entity.
	 * 
	 * @param toGrant
	 * @param entityId
	 * @throws Exception
	 */
	private void grantReadAcess(Long principalId, String entityId) throws Exception {
		AccessControlList acl = entityAclManager.getACL(entityId, admin);
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(principalId);
		ra.setAccessType(Sets.newHashSet(ACCESS_TYPE.READ));
		acl.getResourceAccess().add(ra);
		entityAclManager.updateACL(acl, admin);
	}

}
