package org.sagebionetworks.bridge.manager.community;

import java.util.*;

import org.junit.*;
import org.mockito.*;
import org.mockito.internal.matchers.CapturingMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.bridge.model.Community;
import org.sagebionetworks.repo.manager.*;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.*;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

import static org.mockito.Mockito.*;

import static org.junit.Assert.*;

public class CommunityManagerImplTest {

	private static final String USER_ID = "123";
	private static final String TEAM_ID = "456";
	private static final String COMMUNITY_ID = "789";

	@Mock
	private AuthorizationManager authorizationManager;
	@Mock
	private GroupMembersDAO groupMembersDAO;
	@Mock
	private UserGroupDAO userGroupDAO;

	@Mock
	private AccessControlListDAO aclDAO;
	@Mock
	private UserManager userManager;
	@Mock
	private TeamManager teamManager;
	@Mock
	private AccessRequirementDAO accessRequirementDAO;
	@Mock
	private EntityManager entityManager;
	@Mock
	private EntityPermissionsManager entityPermissionsManager;

	private CommunityManager communityManager;
	private UserInfo validUser;
	private Team testTeam;
	private Community testCommunity;

	@Before
	public void doBefore() {
		MockitoAnnotations.initMocks(this);

		validUser = new UserInfo(false);
		UserGroup individualGroup = new UserGroup();
		individualGroup.setId(USER_ID);
		User user = new User();
		user.setUserId(USER_ID);
		validUser.setUser(user);
		validUser.setIndividualGroup(individualGroup);
		validUser.setGroups(Arrays.asList(new UserGroup[] { individualGroup }));

		testCommunity = new Community();
		testCommunity.setId(COMMUNITY_ID);
		testCommunity.setName(USER_ID);
		testCommunity.setDescription("hi");
		testCommunity.setTeamId(TEAM_ID);
		testCommunity.setEtag("etag");

		testTeam = new Team();
		testTeam.setId(TEAM_ID);
		testTeam.setName(COMMUNITY_ID);

		communityManager = new CommunityManagerImpl(authorizationManager, groupMembersDAO, userGroupDAO, aclDAO, userManager, teamManager,
				accessRequirementDAO, entityManager, entityPermissionsManager);
	}

	@After
	public void doAfter() {
		Mockito.verifyNoMoreInteractions(authorizationManager, groupMembersDAO, userGroupDAO, aclDAO, userManager, accessRequirementDAO,
				entityManager);
	}

	@Test
	public void testCreate() throws Exception {
		when(teamManager.create(eq(validUser), any(Team.class))).thenReturn(testTeam);

		final CaptureStub<Community> newCommunity = CaptureStub.forClass(Community.class);
		when(entityManager.createEntity(eq(validUser), newCommunity.capture(), (String) isNull())).thenReturn(COMMUNITY_ID);
		when(entityManager.getEntity(validUser, COMMUNITY_ID, Community.class)).thenAnswer(newCommunity.answer());

		UserGroup allUsers = new UserGroup();
		allUsers.setId("1");
		UserGroup authenticatedUsers = new UserGroup();
		authenticatedUsers.setId("2");
		when(userManager.getDefaultUserGroup(DEFAULT_GROUPS.AUTHENTICATED_USERS)).thenReturn(authenticatedUsers);
		when(userManager.getDefaultUserGroup(DEFAULT_GROUPS.PUBLIC)).thenReturn(allUsers);

		// set ACL, adding the current user to the community, as an admin
		when(entityPermissionsManager.getACL(COMMUNITY_ID, validUser)).thenReturn(new AccessControlList());

		Community community = new Community();
		community.setName(USER_ID);
		community.setDescription("hi");
		Community created = communityManager.create(validUser, community);

		verify(teamManager).create(eq(validUser), any(Team.class));

		verify(entityManager).createEntity(eq(validUser), newCommunity.capture(), (String) isNull());
		verify(entityManager).getEntity(validUser, COMMUNITY_ID, Community.class);

		verify(userManager).getDefaultUserGroup(DEFAULT_GROUPS.AUTHENTICATED_USERS);
		verify(userManager).getDefaultUserGroup(DEFAULT_GROUPS.PUBLIC);

		verify(entityPermissionsManager).getACL(COMMUNITY_ID, validUser);
		verify(entityPermissionsManager).updateACL(any((AccessControlList.class)), eq(validUser));

		assertEquals(TEAM_ID, created.getTeamId());
	}

	@Test(expected = UnauthorizedException.class)
	public void testCreateFailAnonymous() throws Exception {
		UserInfo anonymousUser = new UserInfo(false);
		UserGroup ug = new UserGroup();
		ug.setName(AuthorizationConstants.ANONYMOUS_USER_ID);
		anonymousUser.setIndividualGroup(ug);

		Community community = new Community();
		communityManager.create(anonymousUser, community);
	}

	@Test(expected = InvalidModelException.class)
	public void testCreateNoName() throws Exception {
		Community community = new Community();
		community.setDescription("hi");
		communityManager.create(validUser, community);
	}

	@Test(expected = InvalidModelException.class)
	public void testCreateIdNotExpected() throws Exception {
		Community community = new Community();
		community.setName(USER_ID);
		community.setDescription("hi");
		community.setId("hi");
		communityManager.create(validUser, community);
	}

	@Test
	public void testGet() throws Exception {
		when(authorizationManager.canAccess(validUser, COMMUNITY_ID, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(true);
		when(entityManager.getEntity(validUser, COMMUNITY_ID, Community.class)).thenReturn(testCommunity);

		Community community = communityManager.get(validUser, COMMUNITY_ID);
		assertNotNull(community);

		verify(authorizationManager).canAccess(validUser, COMMUNITY_ID, ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(entityManager).getEntity(validUser, COMMUNITY_ID, Community.class);
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetNotAuthorized() throws Throwable {
		when(authorizationManager.canAccess(validUser, COMMUNITY_ID, ObjectType.ENTITY, ACCESS_TYPE.READ)).thenReturn(false);
		try {
			communityManager.get(validUser, COMMUNITY_ID);

		} catch (Throwable t) {
			verify(authorizationManager).canAccess(validUser, COMMUNITY_ID, ObjectType.ENTITY, ACCESS_TYPE.READ);
			throw t;
		}
	}

	@Test
	public void testPut() throws Exception {
		when(authorizationManager.canAccess(validUser, COMMUNITY_ID, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(true);
		when(entityManager.getEntity(validUser, COMMUNITY_ID, Community.class)).thenReturn(testCommunity);

		Community community = communityManager.update(validUser, testCommunity);
		assertNotNull(community);

		verify(authorizationManager).canAccess(validUser, COMMUNITY_ID, ObjectType.ENTITY, ACCESS_TYPE.UPDATE);
		verify(entityManager).updateEntity(validUser, testCommunity, false, null);
		verify(entityManager).getEntity(validUser, COMMUNITY_ID, Community.class);
	}

	@Test(expected = UnauthorizedException.class)
	public void testPutNotAuthorized() throws Throwable {
		when(authorizationManager.canAccess(validUser, COMMUNITY_ID, ObjectType.ENTITY, ACCESS_TYPE.UPDATE)).thenReturn(false);
		try {
			communityManager.update(validUser, testCommunity);

		} catch (Throwable t) {
			verify(authorizationManager).canAccess(validUser, COMMUNITY_ID, ObjectType.ENTITY, ACCESS_TYPE.UPDATE);
			throw t;
		}
	}

	@Test
	public void testDelete() throws Exception {
		when(authorizationManager.canAccess(validUser, COMMUNITY_ID, ObjectType.ENTITY, ACCESS_TYPE.DELETE)).thenReturn(true);
		when(entityManager.getEntity(validUser, COMMUNITY_ID, Community.class)).thenReturn(testCommunity);

		communityManager.delete(validUser, COMMUNITY_ID);

		verify(authorizationManager).canAccess(validUser, COMMUNITY_ID, ObjectType.ENTITY, ACCESS_TYPE.DELETE);
		verify(entityManager).getEntity(validUser, COMMUNITY_ID, Community.class);
		verify(teamManager).delete(validUser, TEAM_ID);
		verify(entityManager).deleteEntity(validUser, COMMUNITY_ID);
	}

	@Test(expected = UnauthorizedException.class)
	public void testDeleteNotAuthorized() throws Throwable {
		when(authorizationManager.canAccess(validUser, COMMUNITY_ID, ObjectType.ENTITY, ACCESS_TYPE.DELETE)).thenReturn(false);
		try {
			communityManager.delete(validUser, COMMUNITY_ID);

		} catch (Throwable t) {
			verify(authorizationManager).canAccess(validUser, COMMUNITY_ID, ObjectType.ENTITY, ACCESS_TYPE.DELETE);
			throw t;
		}
	}
}
