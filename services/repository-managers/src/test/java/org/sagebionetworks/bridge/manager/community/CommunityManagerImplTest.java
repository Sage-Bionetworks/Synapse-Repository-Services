package org.sagebionetworks.bridge.manager.community;

import java.util.*;

import org.apache.commons.fileupload.FileItemStream;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.internal.matchers.CapturingMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.bridge.model.Community;
import org.sagebionetworks.repo.manager.*;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.manager.wiki.V2WikiManager;
import org.sagebionetworks.repo.model.*;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

import static org.mockito.Mockito.*;

import static org.junit.Assert.*;

public class CommunityManagerImplTest extends MockitoTestBase {

	private static final String USER_ID = "123";
	private static final String TEAM_ID = "456";
	private static final String COMMUNITY_ID = "789";
	private static final String OTHER_USER_ID = "111";
	private static final String OTHER_TEAM_ID = "222";
	private static final String OTHER_COMMUNITY_ID = "333";
	

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
	@Mock
	private V2WikiManager wikiManager;
	@Mock
	private FileHandleManager fileHandleManager;

	private CommunityManager communityManager;
	private UserInfo validUser;
	private Team testTeam;
	private Community testCommunity;
	
	private UserInfo otherUser;
	private Community otherUserTestCommunity;
	private Team otherTestTeam;

	@Before
	public void doBefore() {
		initMockito();

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
		
		createCommunityFromOtherUser();

		communityManager = new CommunityManagerImpl(authorizationManager, fileHandleManager, userManager, teamManager,
				entityManager, entityPermissionsManager, wikiManager);
	}
	
	private void createCommunityFromOtherUser() {
		otherUser = new UserInfo(false);
		UserGroup individualGroup = new UserGroup();
		individualGroup.setId(OTHER_USER_ID);
		User user = new User();
		user.setUserId(OTHER_USER_ID);
		otherUser.setUser(user);
		otherUser.setIndividualGroup(individualGroup);
		otherUser.setGroups(Arrays.asList(new UserGroup[] { individualGroup }));

		otherUserTestCommunity = new Community();
		// otherUserTestCommunity.setId(OTHER_COMMUNITY_ID);
		otherUserTestCommunity.setName(OTHER_USER_ID);
		otherUserTestCommunity.setDescription("bye");
		
		otherTestTeam = new Team();
		otherTestTeam.setId(OTHER_TEAM_ID);
		otherTestTeam.setName(OTHER_COMMUNITY_ID);
	}

	@Test
	public void testCreate() throws Exception {
		when(teamManager.create(eq(validUser), any(Team.class))).thenReturn(testTeam);

		final CaptureStub<Community> newCommunity = CaptureStub.forClass(Community.class);
		when(entityManager.createEntity(eq(validUser), newCommunity.capture(), (String) isNull())).thenReturn(COMMUNITY_ID);
		when(entityManager.getEntity(validUser, COMMUNITY_ID, Community.class)).thenAnswer(newCommunity.answer()).thenAnswer(
				newCommunity.answer());

		UserGroup allUsers = new UserGroup();
		allUsers.setId("1");
		UserGroup authenticatedUsers = new UserGroup();
		authenticatedUsers.setId("2");
		when(userManager.getDefaultUserGroup(DEFAULT_GROUPS.AUTHENTICATED_USERS)).thenReturn(authenticatedUsers);
		when(userManager.getDefaultUserGroup(DEFAULT_GROUPS.PUBLIC)).thenReturn(allUsers);

		// set ACL, adding the current user to the community, as an admin
		when(entityPermissionsManager.getACL(COMMUNITY_ID, validUser)).thenReturn(new AccessControlList());

		V2WikiPage rootPage = new V2WikiPage();
		rootPage.setMarkdownFileHandleId("f1");
		rootPage.setTitle(USER_ID);
		V2WikiPage newRootPage = new V2WikiPage();
		newRootPage.setId("5");
		newRootPage.setMarkdownFileHandleId("f1");
		when(wikiManager.createWikiPage(validUser, COMMUNITY_ID, ObjectType.ENTITY, rootPage)).thenReturn(newRootPage);

		V2WikiPage welcomePage = new V2WikiPage();
		welcomePage.setTitle("Welcome to " + USER_ID);
		welcomePage.setParentWikiId("5");
		welcomePage.setMarkdownFileHandleId("f2");
		when(wikiManager.createWikiPage(validUser, COMMUNITY_ID, ObjectType.ENTITY, welcomePage)).thenReturn(welcomePage);

		V2WikiPage indexPage = new V2WikiPage();
		indexPage.setTitle("Index of " + USER_ID);
		indexPage.setParentWikiId("5");
		indexPage.setMarkdownFileHandleId("f3");
		when(wikiManager.createWikiPage(validUser, COMMUNITY_ID, ObjectType.ENTITY, indexPage)).thenReturn(indexPage);

		S3FileHandle fileHandle1 = new S3FileHandle();
		S3FileHandle fileHandle2 = new S3FileHandle();
		S3FileHandle fileHandle3 = new S3FileHandle();
		fileHandle1.setId("f1");
		fileHandle2.setId("f2");
		fileHandle3.setId("f3");
		when(fileHandleManager.uploadFile(eq(USER_ID), any(FileItemStream.class))).thenReturn(fileHandle1, fileHandle2, fileHandle3);
		
		Community community = new Community();
		community.setName(USER_ID);
		community.setDescription("hi");
		Community created = communityManager.create(validUser, community);

		verify(teamManager).create(eq(validUser), any(Team.class));

		verify(entityManager).createEntity(eq(validUser), newCommunity.capture(), (String) isNull());
		verify(entityManager, times(2)).getEntity(validUser, COMMUNITY_ID, Community.class);

		verify(userManager).getDefaultUserGroup(DEFAULT_GROUPS.AUTHENTICATED_USERS);
		verify(userManager).getDefaultUserGroup(DEFAULT_GROUPS.PUBLIC);

		verify(entityPermissionsManager).getACL(COMMUNITY_ID, validUser);
		verify(entityPermissionsManager).updateACL(any((AccessControlList.class)), eq(validUser));

		verify(fileHandleManager, times(3)).uploadFile(eq(USER_ID), any(FileItemStream.class));
		verify(wikiManager).createWikiPage(validUser, COMMUNITY_ID, ObjectType.ENTITY, rootPage);
		verify(wikiManager).createWikiPage(validUser, COMMUNITY_ID, ObjectType.ENTITY, welcomePage);
		verify(wikiManager).createWikiPage(validUser, COMMUNITY_ID, ObjectType.ENTITY, indexPage);
		verify(entityManager).updateEntity(eq(validUser), newCommunity.capture(), eq(false), (String) isNull());

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
	
	@Test
	public void getCommunitiesReturnsAll() throws Exception {
		// This implementation is temporary and possibly wrong, so I'm not going to write tests for it.
	}
	
	@Test 
	public void getCommunitiesByMemberReturnsSubset() {
		// This implementation is temporary and possibly wrong, so I'm not going to write tests for it.
	}
}
