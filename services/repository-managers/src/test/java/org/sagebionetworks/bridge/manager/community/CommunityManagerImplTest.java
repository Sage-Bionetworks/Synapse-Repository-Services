package org.sagebionetworks.bridge.manager.community;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.fileupload.FileItemStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.sagebionetworks.bridge.model.Community;
import org.sagebionetworks.bridge.model.CommunityTeamDAO;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.EntityPermissionsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.manager.wiki.V2WikiManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMembershipStatus;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.common.base.Function;
import com.google.common.collect.Sets;

public class CommunityManagerImplTest extends MockitoTestBase {

	private static final String USER_ID = "123";
	private static final String TEAM_ID = "456";
	private static final String COMMUNITY_ID = "789";
	private static final String USER_ID2 = "1232";
	

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
	@Mock
	private CommunityTeamDAO communityTeamDAO;

	private CommunityManager communityManager;
	private UserInfo validUser;
	private Team testTeam;
	private Community testCommunity;
	
	private UserInfo otherUser;

	@Before
	public void doBefore() {
		initMockito();

		validUser = new UserInfo(false, USER_ID);

		otherUser = new UserInfo(false, USER_ID2);

		testCommunity = new Community();
		testCommunity.setId(COMMUNITY_ID);
		testCommunity.setName(USER_ID);
		testCommunity.setDescription("hi");
		testCommunity.setTeamId(TEAM_ID);
		testCommunity.setEtag("etag");

		testTeam = new Team();
		testTeam.setId(TEAM_ID);
		testTeam.setName(COMMUNITY_ID);
		
		communityManager = new CommunityManagerImpl(authorizationManager, fileHandleManager, userManager, teamManager, entityManager,
				entityPermissionsManager, wikiManager, communityTeamDAO, aclDAO);
	}

	@Test
	public void testCreate() throws Exception {
		when(teamManager.create(eq(validUser), any(Team.class))).thenReturn(testTeam);

		final CaptureStub<Community> newCommunity = CaptureStub.forClass(Community.class, new Function<Community, Community>() {
			@Override
			public Community apply(Community input) {
				input.setId(COMMUNITY_ID);
				return input;
			}
		});
		when(entityManager.createEntity(eq(validUser), newCommunity.capture(), (String) isNull())).thenReturn(COMMUNITY_ID);
		when(entityManager.getEntity(validUser, COMMUNITY_ID, Community.class)).thenAnswer(newCommunity.answer()).thenAnswer(
				newCommunity.answer());

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
		verify(communityTeamDAO).create(Long.parseLong(COMMUNITY_ID), Long.parseLong(TEAM_ID));

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
		UserInfo anonymousUser = new UserInfo(false, BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
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
	public void testJoin() throws Throwable {
		when(authorizationManager.canAccess(validUser, COMMUNITY_ID, ObjectType.ENTITY, ACCESS_TYPE.PARTICIPATE)).thenReturn(true);
		when(entityManager.getEntity(validUser, COMMUNITY_ID, Community.class)).thenReturn(testCommunity);

		communityManager.join(validUser, COMMUNITY_ID);

		verify(authorizationManager).canAccess(validUser, COMMUNITY_ID, ObjectType.ENTITY, ACCESS_TYPE.PARTICIPATE);
		verify(entityManager).getEntity(validUser, COMMUNITY_ID, Community.class);
		verify(teamManager).addMember(validUser, TEAM_ID, validUser);
	}

	@Test
	public void testLeave() throws Throwable {
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(Long.parseLong(USER_ID));
		ra.setAccessType(Sets.newHashSet(CommunityManagerImpl.COMMUNITY_MEMBER_PERMISSIONS));
		ResourceAccess ra2 = new ResourceAccess();
		ra2.setPrincipalId(5L);
		ra2.setAccessType(Sets.newHashSet(CommunityManagerImpl.COMMUNITY_ADMIN_PERMISSIONS));

		AccessControlList aclBefore = new AccessControlList();
		aclBefore.setResourceAccess(Sets.newHashSet(ra, ra2));

		AccessControlList aclAfter = new AccessControlList();
		aclAfter.setResourceAccess(Sets.newHashSet(ra2));

		when(entityManager.getEntity(validUser, COMMUNITY_ID, Community.class)).thenReturn(testCommunity);
		when(aclDAO.get(COMMUNITY_ID, ObjectType.ENTITY)).thenReturn(aclBefore);

		communityManager.leave(validUser, COMMUNITY_ID);

		verify(entityManager).getEntity(validUser, COMMUNITY_ID, Community.class);
		verify(aclDAO).get(COMMUNITY_ID, ObjectType.ENTITY);
		verify(aclDAO).update(aclAfter, ObjectType.ENTITY);
		verify(teamManager).removeMember(validUser, TEAM_ID, USER_ID);
	}

	@Test
	public void testAddAdmin() throws Throwable {
		Community community = new Community();
		community.setTeamId(TEAM_ID);

		TeamMembershipStatus membershipIsMember = new TeamMembershipStatus();
		membershipIsMember.setIsMember(true);

		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(Long.parseLong(USER_ID));
		ResourceAccess ra2 = new ResourceAccess();
		ra2.setPrincipalId(5L);
		ResourceAccess radmin = new ResourceAccess();
		radmin.setPrincipalId(Long.parseLong(USER_ID2));
		radmin.setAccessType(Sets.newHashSet(ACCESS_TYPE.READ, ACCESS_TYPE.UPDATE, ACCESS_TYPE.CREATE, ACCESS_TYPE.DELETE,
				ACCESS_TYPE.SEND_MESSAGE, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE));

		AccessControlList aclBefore = new AccessControlList();
		aclBefore.setResourceAccess(Sets.newHashSet(ra, ra2));

		AccessControlList aclAfter = new AccessControlList();
		aclAfter.setResourceAccess(Sets.newHashSet(radmin, ra, ra2));

		when(entityManager.getEntity(validUser, COMMUNITY_ID, Community.class)).thenReturn(community);
		when(userManager.getUserInfo(Long.parseLong(USER_ID2))).thenReturn(otherUser);
		when(teamManager.getTeamMembershipStatus(validUser, TEAM_ID, otherUser)).thenReturn(membershipIsMember);
		when(authorizationManager.canAccess(validUser, COMMUNITY_ID, ObjectType.ENTITY, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(true);
		when(aclDAO.get(COMMUNITY_ID, ObjectType.ENTITY)).thenReturn(aclBefore);

		communityManager.addAdmin(validUser, COMMUNITY_ID, USER_ID2);

		verify(entityManager).getEntity(validUser, COMMUNITY_ID, Community.class);
		verify(userManager).getUserInfo(Long.parseLong(USER_ID2));
		verify(teamManager).getTeamMembershipStatus(validUser, TEAM_ID, otherUser);
		verify(authorizationManager).canAccess(validUser, COMMUNITY_ID, ObjectType.ENTITY, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE);
		verify(aclDAO).get(COMMUNITY_ID, ObjectType.ENTITY);
		verify(aclDAO).update(aclAfter, ObjectType.ENTITY);
		verify(teamManager).setPermissions(validUser, TEAM_ID, USER_ID2, true);
	}

	@Test(expected = UnauthorizedException.class)
	public void testAddAdminNoPermissions() throws Throwable {
		Community community = new Community();
		community.setTeamId(TEAM_ID);

		TeamMembershipStatus membershipIsMember = new TeamMembershipStatus();
		membershipIsMember.setIsMember(true);

		when(entityManager.getEntity(validUser, COMMUNITY_ID, Community.class)).thenReturn(community);
		when(userManager.getUserInfo(Long.parseLong(USER_ID2))).thenReturn(otherUser);
		when(teamManager.getTeamMembershipStatus(validUser, TEAM_ID, otherUser)).thenReturn(membershipIsMember);
		when(authorizationManager.canAccess(validUser, COMMUNITY_ID, ObjectType.ENTITY, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE))
				.thenReturn(false);
		try {
			communityManager.addAdmin(validUser, COMMUNITY_ID, USER_ID2);
		} finally {
			verify(entityManager).getEntity(validUser, COMMUNITY_ID, Community.class);
			verify(userManager).getUserInfo(Long.parseLong(USER_ID2));
			verify(teamManager).getTeamMembershipStatus(validUser, TEAM_ID, otherUser);
			verify(authorizationManager).canAccess(validUser, COMMUNITY_ID, ObjectType.ENTITY, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE);
		}
	}

	@Test(expected = NotFoundException.class)
	public void testAddAdminNotMember() throws Throwable {
		Community community = new Community();
		community.setTeamId(TEAM_ID);

		TeamMembershipStatus membershipIsMember = new TeamMembershipStatus();
		membershipIsMember.setIsMember(false);

		when(entityManager.getEntity(validUser, COMMUNITY_ID, Community.class)).thenReturn(community);
		when(userManager.getUserInfo(Long.parseLong(USER_ID2))).thenReturn(otherUser);
		when(teamManager.getTeamMembershipStatus(validUser, TEAM_ID, otherUser)).thenReturn(membershipIsMember);
		try {
			communityManager.addAdmin(validUser, COMMUNITY_ID, USER_ID2);
		} finally {
			verify(entityManager).getEntity(validUser, COMMUNITY_ID, Community.class);
			verify(userManager).getUserInfo(Long.parseLong(USER_ID2));
			verify(teamManager).getTeamMembershipStatus(validUser, TEAM_ID, otherUser);
		}
	}

	@Test
	public void testRemoveAdmin() throws Throwable {
		Community community = new Community();
		community.setTeamId(TEAM_ID);

		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(Long.parseLong(USER_ID));
		ra.setAccessType(Sets.newHashSet(CommunityManagerImpl.COMMUNITY_ADMIN_PERMISSIONS));
		ResourceAccess ra2 = new ResourceAccess();
		ra2.setPrincipalId(5L);
		ra2.setAccessType(Sets.newHashSet(CommunityManagerImpl.COMMUNITY_MEMBER_PERMISSIONS));
		ResourceAccess radmin = new ResourceAccess();
		radmin.setPrincipalId(Long.parseLong(USER_ID2));
		radmin.setAccessType(Sets.newHashSet(CommunityManagerImpl.COMMUNITY_ADMIN_PERMISSIONS));

		AccessControlList aclBefore = new AccessControlList();
		aclBefore.setResourceAccess(Sets.newHashSet(radmin, ra, ra2));

		AccessControlList aclAfter = new AccessControlList();
		aclAfter.setResourceAccess(Sets.newHashSet(ra, ra2));

		when(entityManager.getEntity(validUser, COMMUNITY_ID, Community.class)).thenReturn(community);
		when(userManager.getUserInfo(Long.parseLong(USER_ID2))).thenReturn(otherUser);
		when(authorizationManager.canAccess(validUser, COMMUNITY_ID, ObjectType.ENTITY, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(true);
		when(aclDAO.get(COMMUNITY_ID, ObjectType.ENTITY)).thenReturn(aclBefore);

		communityManager.removeAdmin(validUser, COMMUNITY_ID, USER_ID2);

		verify(entityManager).getEntity(validUser, COMMUNITY_ID, Community.class);
		verify(userManager).getUserInfo(Long.parseLong(USER_ID2));
		verify(authorizationManager).canAccess(validUser, COMMUNITY_ID, ObjectType.ENTITY, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE);
		verify(aclDAO).get(COMMUNITY_ID, ObjectType.ENTITY);
		verify(aclDAO).update(aclAfter, ObjectType.ENTITY);
		verify(teamManager).setPermissions(validUser, TEAM_ID, USER_ID2, false);
	}

	@Test
	public void testRemoveAdminSelfNoPerms() throws Throwable {
		Community community = new Community();
		community.setTeamId(TEAM_ID);

		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(Long.parseLong(USER_ID));
		ra.setAccessType(Sets.newHashSet(CommunityManagerImpl.COMMUNITY_MEMBER_PERMISSIONS));
		ResourceAccess ra2 = new ResourceAccess();
		ra2.setPrincipalId(5L);
		ra2.setAccessType(Sets.newHashSet(CommunityManagerImpl.COMMUNITY_ADMIN_PERMISSIONS));
		ResourceAccess radmin = new ResourceAccess();
		radmin.setPrincipalId(Long.parseLong(USER_ID2));
		radmin.setAccessType(Sets.newHashSet(CommunityManagerImpl.COMMUNITY_ADMIN_PERMISSIONS));

		AccessControlList aclBefore = new AccessControlList();
		aclBefore.setResourceAccess(Sets.newHashSet(radmin, ra, ra2));

		AccessControlList aclAfter = new AccessControlList();
		aclAfter.setResourceAccess(Sets.newHashSet(ra, ra2));

		when(entityManager.getEntity(otherUser, COMMUNITY_ID, Community.class)).thenReturn(community);
		when(userManager.getUserInfo(Long.parseLong(USER_ID2))).thenReturn(otherUser);
		when(aclDAO.get(COMMUNITY_ID, ObjectType.ENTITY)).thenReturn(aclBefore);

		communityManager.removeAdmin(otherUser, COMMUNITY_ID, USER_ID2);

		verify(entityManager).getEntity(otherUser, COMMUNITY_ID, Community.class);
		verify(userManager).getUserInfo(Long.parseLong(USER_ID2));
		verify(aclDAO).get(COMMUNITY_ID, ObjectType.ENTITY);
		verify(aclDAO).update(aclAfter, ObjectType.ENTITY);
		verify(teamManager).setPermissions(otherUser, TEAM_ID, USER_ID2, false);
	}

	@Test(expected = UnauthorizedException.class)
	public void testRemoveLastAdmin() throws Throwable {
		Community community = new Community();
		community.setTeamId(TEAM_ID);

		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(Long.parseLong(USER_ID));
		ra.setAccessType(Sets.newHashSet(CommunityManagerImpl.COMMUNITY_MEMBER_PERMISSIONS));
		ResourceAccess ra2 = new ResourceAccess();
		ra2.setPrincipalId(5L);
		ra2.setAccessType(Sets.newHashSet(CommunityManagerImpl.COMMUNITY_MEMBER_PERMISSIONS));
		ResourceAccess radmin = new ResourceAccess();
		radmin.setPrincipalId(Long.parseLong(USER_ID2));
		radmin.setAccessType(Sets.newHashSet(ACCESS_TYPE.READ, ACCESS_TYPE.UPDATE, ACCESS_TYPE.CREATE, ACCESS_TYPE.DELETE,
				ACCESS_TYPE.SEND_MESSAGE, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE));

		AccessControlList aclBefore = new AccessControlList();
		aclBefore.setResourceAccess(Sets.newHashSet(radmin, ra, ra2));

		when(entityManager.getEntity(otherUser, COMMUNITY_ID, Community.class)).thenReturn(community);
		when(userManager.getUserInfo(Long.parseLong(USER_ID2))).thenReturn(otherUser);
		when(aclDAO.get(COMMUNITY_ID, ObjectType.ENTITY)).thenReturn(aclBefore);

		try {
			communityManager.removeAdmin(otherUser, COMMUNITY_ID, USER_ID2);
		} finally {
			verify(entityManager).getEntity(otherUser, COMMUNITY_ID, Community.class);
			verify(userManager).getUserInfo(Long.parseLong(USER_ID2));
			verify(aclDAO).get(COMMUNITY_ID, ObjectType.ENTITY);
		}
	}

	@Test
	public void testLeaveAdmin() throws Throwable {
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(Long.parseLong(USER_ID));
		ra.setAccessType(Sets.newHashSet(CommunityManagerImpl.COMMUNITY_ADMIN_PERMISSIONS));
		ResourceAccess ra2 = new ResourceAccess();
		ra2.setPrincipalId(5L);
		ra2.setAccessType(Sets.newHashSet(CommunityManagerImpl.COMMUNITY_ADMIN_PERMISSIONS));

		AccessControlList aclBefore = new AccessControlList();
		aclBefore.setResourceAccess(Sets.newHashSet(ra, ra2));

		AccessControlList aclAfter = new AccessControlList();
		aclAfter.setResourceAccess(Sets.newHashSet(ra2));

		when(entityManager.getEntity(validUser, COMMUNITY_ID, Community.class)).thenReturn(testCommunity);
		when(aclDAO.get(COMMUNITY_ID, ObjectType.ENTITY)).thenReturn(aclBefore);

		communityManager.leave(validUser, COMMUNITY_ID);

		verify(entityManager).getEntity(validUser, COMMUNITY_ID, Community.class);
		verify(aclDAO).get(COMMUNITY_ID, ObjectType.ENTITY);
		verify(aclDAO).update(aclAfter, ObjectType.ENTITY);
		verify(teamManager).removeMember(validUser, TEAM_ID, USER_ID);
	}
}
