package org.sagebionetworks.bridge.manager.community;

import java.util.*;

import org.junit.*;
import org.mockito.*;
import org.mockito.internal.matchers.CapturingMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.bridge.model.Community;
import org.sagebionetworks.repo.manager.*;
import org.sagebionetworks.repo.model.*;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

import static org.mockito.Mockito.*;

import static org.junit.Assert.*;

public class CommunityManagerImplTest {

	private static final String USER_ID = "123";
	private static final String GROUP_ID = "456";
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
	private AccessRequirementDAO accessRequirementDAO;
	@Mock
	private EntityManager entityManager;

	private CommunityManager communityManager;
	private UserInfo validUser;
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
		testCommunity.setGroupId(GROUP_ID);
		testCommunity.setEtag("etag");

		communityManager = new CommunityManagerImpl(authorizationManager, groupMembersDAO, userGroupDAO, aclDAO, userManager,
				accessRequirementDAO, entityManager);
	}

	@After
	public void doAfter() {
		Mockito.verifyNoMoreInteractions(authorizationManager, groupMembersDAO, userGroupDAO, aclDAO, userManager, accessRequirementDAO,
				entityManager);
	}

	@Test
	public void testCreate() throws Exception {
		when(userManager.doesPrincipalExist(USER_ID)).thenReturn(false);
		when(userGroupDAO.create(any(UserGroup.class))).thenReturn(GROUP_ID);

		final CaptureStub<Community> newCommunity = CaptureStub.forClass(Community.class);
		when(entityManager.createEntity(eq(validUser), newCommunity.capture(), (String) isNull())).thenReturn(COMMUNITY_ID);
		when(entityManager.getEntity(validUser, COMMUNITY_ID, Community.class)).thenAnswer(newCommunity.answer());

		// create ACL, adding the current user to the community, as an admin

		Community community = new Community();
		community.setName(USER_ID);
		community.setDescription("hi");
		Community created = communityManager.create(validUser, community);

		verify(userManager).doesPrincipalExist(USER_ID);
		verify(userGroupDAO).create(any(UserGroup.class));

		verify(entityManager).createEntity(eq(validUser), newCommunity.capture(), (String) isNull());
		verify(entityManager).getEntity(validUser, COMMUNITY_ID, Community.class);

		verify(groupMembersDAO).addMembers(COMMUNITY_ID, Collections.singletonList(USER_ID));
		verify(aclDAO).create(any(AccessControlList.class));

		assertEquals(GROUP_ID, created.getGroupId());
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
	public void testCreateNoDescription() throws Exception {
		Community community = new Community();
		community.setName("hi");
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
		when(authorizationManager.canAccess(validUser, COMMUNITY_ID, ObjectType.COMMUNITY, ACCESS_TYPE.READ)).thenReturn(true);
		when(entityManager.getEntity(validUser, COMMUNITY_ID, Community.class)).thenReturn(testCommunity);

		Community community = communityManager.get(validUser, COMMUNITY_ID);
		assertNotNull(community);

		verify(authorizationManager).canAccess(validUser, COMMUNITY_ID, ObjectType.COMMUNITY, ACCESS_TYPE.READ);
		verify(entityManager).getEntity(validUser, COMMUNITY_ID, Community.class);
	}

	@Test(expected = UnauthorizedException.class)
	public void testGetNotAuthorized() throws Throwable {
		when(authorizationManager.canAccess(validUser, COMMUNITY_ID, ObjectType.COMMUNITY, ACCESS_TYPE.READ)).thenReturn(false);
		try {
			communityManager.get(validUser, COMMUNITY_ID);

		} catch (Throwable t) {
			verify(authorizationManager).canAccess(validUser, COMMUNITY_ID, ObjectType.COMMUNITY, ACCESS_TYPE.READ);
			throw t;
		}
	}

	@Test
	public void testPut() throws Exception {
		when(authorizationManager.canAccess(validUser, COMMUNITY_ID, ObjectType.COMMUNITY, ACCESS_TYPE.UPDATE)).thenReturn(true);
		when(entityManager.getEntity(validUser, COMMUNITY_ID, Community.class)).thenReturn(testCommunity);

		Community community = communityManager.put(validUser, testCommunity);
		assertNotNull(community);

		verify(authorizationManager).canAccess(validUser, COMMUNITY_ID, ObjectType.COMMUNITY, ACCESS_TYPE.UPDATE);
		verify(entityManager).updateEntity(validUser, testCommunity, true, null);
		verify(entityManager).getEntity(validUser, COMMUNITY_ID, Community.class);
	}

	@Test(expected = UnauthorizedException.class)
	public void testPutNotAuthorized() throws Throwable {
		when(authorizationManager.canAccess(validUser, COMMUNITY_ID, ObjectType.COMMUNITY, ACCESS_TYPE.UPDATE)).thenReturn(false);
		try {
			communityManager.put(validUser, testCommunity);

		} catch (Throwable t) {
			verify(authorizationManager).canAccess(validUser, COMMUNITY_ID, ObjectType.COMMUNITY, ACCESS_TYPE.UPDATE);
			throw t;
		}
	}

	@Test
	public void testDelete() throws Exception {
		when(authorizationManager.canAccess(validUser, COMMUNITY_ID, ObjectType.COMMUNITY, ACCESS_TYPE.DELETE)).thenReturn(true);
		when(entityManager.getEntity(validUser, COMMUNITY_ID, Community.class)).thenReturn(testCommunity);

		communityManager.delete(validUser, testCommunity);

		verify(authorizationManager).canAccess(validUser, COMMUNITY_ID, ObjectType.COMMUNITY, ACCESS_TYPE.DELETE);
		verify(userGroupDAO).delete(GROUP_ID);
		verify(entityManager).deleteEntity(validUser, COMMUNITY_ID);
	}

	@Test(expected = UnauthorizedException.class)
	public void testDeleteNotAuthorized() throws Throwable {
		when(authorizationManager.canAccess(validUser, COMMUNITY_ID, ObjectType.COMMUNITY, ACCESS_TYPE.DELETE)).thenReturn(false);
		try {
			communityManager.delete(validUser, testCommunity);

		} catch (Throwable t) {
			verify(authorizationManager).canAccess(validUser, COMMUNITY_ID, ObjectType.COMMUNITY, ACCESS_TYPE.DELETE);
			throw t;
		}
	}
}
