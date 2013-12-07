package org.sagebionetworks.repo.manager.team;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.MembershipInvtnSubmissionDAO;
import org.sagebionetworks.repo.model.MembershipRqstSubmissionDAO;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.TeamMembershipStatus;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserInfo;

public class TeamManagerImplTest {
	private TeamManagerImpl teamManagerImpl = null;
	private AuthorizationManager mockAuthorizationManager = null;
	private TeamDAO mockTeamDAO = null;
	private GroupMembersDAO mockGroupMembersDAO = null;
	private UserGroupDAO mockUserGroupDAO = null;
	private AccessControlListDAO mockAclDAO = null;
	private FileHandleManager mockFileHandleManager = null;
	private MembershipInvtnSubmissionDAO mockMembershipInvtnSubmissionDAO = null;
	private MembershipRqstSubmissionDAO mockMembershipRqstSubmissionDAO = null;
	private UserManager mockUserManager = null;
	private AccessRequirementDAO mockAccessRequirementDAO = null;
	
	private UserInfo userInfo = null;
	private UserInfo adminInfo = null;
	private static final String MEMBER_PRINCIPAL_ID = "999";

	private static final String TEAM_ID = "123";

	@Before
	public void setUp() throws Exception {
		mockAuthorizationManager = Mockito.mock(AuthorizationManager.class);
		mockTeamDAO = Mockito.mock(TeamDAO.class);
		mockGroupMembersDAO = Mockito.mock(GroupMembersDAO.class);
		mockUserGroupDAO = Mockito.mock(UserGroupDAO.class);
		mockFileHandleManager = Mockito.mock(FileHandleManager.class);
		mockAclDAO = Mockito.mock(AccessControlListDAO.class);
		mockMembershipInvtnSubmissionDAO = Mockito.mock(MembershipInvtnSubmissionDAO.class);
		mockMembershipRqstSubmissionDAO = Mockito.mock(MembershipRqstSubmissionDAO.class);
		mockUserManager = Mockito.mock(UserManager.class);
		mockAccessRequirementDAO = Mockito.mock(AccessRequirementDAO.class);
		teamManagerImpl = new TeamManagerImpl(
				mockAuthorizationManager,
				mockTeamDAO,
				mockGroupMembersDAO,
				mockUserGroupDAO,
				mockAclDAO,
				mockFileHandleManager,
				mockMembershipInvtnSubmissionDAO,
				mockMembershipRqstSubmissionDAO, 
				mockUserManager,
				mockAccessRequirementDAO);
		userInfo = createUserInfo(false, MEMBER_PRINCIPAL_ID);
		adminInfo = createUserInfo(true, "-1");
	}
	
	private static UserInfo createUserInfo(boolean isAdmin, String principalId) {
		UserInfo userInfo = new UserInfo(isAdmin);
		UserGroup individualGroup = new UserGroup();
		individualGroup.setId(principalId);
		User user = new User();
		user.setUserId(principalId);
		userInfo.setUser(user);
		userInfo.setIndividualGroup(individualGroup);
		userInfo.setGroups(Arrays.asList(new UserGroup[]{individualGroup}));
		return userInfo;
	}
	
	private static Team createTeam(String id, String name, String description, String etag, String icon, 
			String createdBy, Date createdOn, String modifiedBy, Date modifiedOn) {
		Team team = new Team();
		team.setId(id);
		team.setName(name);
		team.setDescription(description);
		team.setEtag(etag);
		team.setIcon(icon);	
		team.setCreatedBy(createdBy);
		team.setCreatedOn(createdOn);
		team.setModifiedBy(modifiedBy);
		team.setModifiedOn(modifiedOn);
		return team;
	}

	private void validateForCreateExpectFailure(String id, String name, String description, String etag, String icon, 
			String createdBy, Date createdOn, String modifiedBy, Date modifiedOn) {
		Team team = createTeam(id, name, description, etag, icon, createdBy, createdOn, modifiedBy, modifiedOn);
		try {
			TeamManagerImpl.validateForCreate(team);
			fail("InvalidModelException expected");
		} catch (InvalidModelException e) {
			// as expected
		}		
	}

	@Test
	public void testValidateForCreate() throws Exception {
		Team team = new Team();
		
		// Happy case
		team = createTeam(null, "name", "description", null, "101", null, null, null, null);
		TeamManagerImpl.validateForCreate(team);
		
		// fields you have to set
		validateForCreateExpectFailure(null, null, "description", null, "101", null, null, null, null);

		// fields you can't set
		validateForCreateExpectFailure("id", "name", "description", null, "101", null, null, null, null);
		validateForCreateExpectFailure(null, "name", "description", "etag", "101", null, null, null, null);
		validateForCreateExpectFailure(null, "name", "description", null, "101", "createdBy", null, null, null);
		validateForCreateExpectFailure(null, "name", "description", null, "101", null, new Date(), null, null);
		validateForCreateExpectFailure(null, "name", "description", null, "101", null, null, "createdOn", null);
		validateForCreateExpectFailure(null, "name", "description", null, "101", null, null, null, new Date());
	}
	
	private void validateForUpdateExpectFailure(String id, String name, String description, String etag, String icon, 
			String createdBy, Date createdOn, String modifiedBy, Date modifiedOn) {
		Team team = createTeam(id, name, description, etag, icon, createdBy, createdOn, modifiedBy, modifiedOn);
		try {
			TeamManagerImpl.validateForUpdate(team);
			fail("InvalidModelException expected");
		} catch (InvalidModelException e) {
			// as expected
		}		
	}

	@Test
	public void testValidateForUpdate() throws Exception {
		Team team = new Team();
		
		// Happy case
		team = createTeam("id", "name", "description", "etag", "101", "createdBy", new Date(), "modifiedBy", new Date());
		TeamManagerImpl.validateForUpdate(team);
		
		// fields you have to have for an update
		validateForUpdateExpectFailure("id", "name", "description", null, "101", "createdBy", new Date(), "modifiedBy", new Date());
		validateForUpdateExpectFailure(null, "name", "description", "etag", "101", "createdBy", new Date(), "modifiedBy", new Date());
		validateForUpdateExpectFailure("id", null, "description", "etag", "101", "createdBy", new Date(), "modifiedBy", new Date());
	}
	
	@Test
	public void testPopulateCreationFields() throws Exception {
		Team team = new Team();
		Date now = new Date();
		TeamManagerImpl.populateCreationFields(userInfo, team, now);
		assertEquals(userInfo.getIndividualGroup().getId(), team.getCreatedBy());
		assertEquals(now, team.getCreatedOn());
		assertEquals(userInfo.getIndividualGroup().getId(), team.getModifiedBy());
		assertEquals(now, team.getModifiedOn());
	}
	
	
	@Test
	public void testPopulateUpdateFields() throws Exception {
		Team team = new Team();
		Date now = new Date();
		TeamManagerImpl.populateUpdateFields(userInfo, team, now);
		assertEquals(null, team.getCreatedBy());
		assertEquals(null, team.getCreatedOn());
		assertEquals(userInfo.getIndividualGroup().getId(), team.getModifiedBy());
		assertEquals(now, team.getModifiedOn());
	}
	
	@Test
	public void testCreateAdminAcl() throws Exception {
		Date now = new Date();
		AccessControlList acl = TeamManagerImpl.createInitialAcl(userInfo, TEAM_ID, now);
		assertEquals(MEMBER_PRINCIPAL_ID, acl.getCreatedBy());
		assertEquals(now, acl.getCreationDate());
		assertEquals(TEAM_ID, acl.getId());
		assertEquals(MEMBER_PRINCIPAL_ID, acl.getModifiedBy());
		assertEquals(now, acl.getModifiedOn());
		assertEquals(2, acl.getResourceAccess().size());
		for (ResourceAccess ra : acl.getResourceAccess()) {
			if (ra.getPrincipalId().toString().equals(MEMBER_PRINCIPAL_ID)) {
		assertEquals(new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[]{
				ACCESS_TYPE.READ, 
				ACCESS_TYPE.UPDATE, 
				ACCESS_TYPE.DELETE, 
				ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE, 
				ACCESS_TYPE.SEND_MESSAGE})), ra.getAccessType());
			} else if (ra.getPrincipalId().toString().equals(TEAM_ID)) {
				
			} else {
				fail("Unexpected principal ID"+ra.getPrincipalId());
	}
		}
	}
	
	@Test
	public void testAddToAcl() throws Exception {
		AccessControlList acl = new AccessControlList();
		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		acl.setResourceAccess(ras);
		TeamManagerImpl.addToACL(acl, MEMBER_PRINCIPAL_ID, new ACCESS_TYPE[] {ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE});
		assertEquals(1, acl.getResourceAccess().size());
		ResourceAccess ra = acl.getResourceAccess().iterator().next();
		assertEquals(new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[]{ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE})), ra.getAccessType());
		assertEquals((Long)Long.parseLong(MEMBER_PRINCIPAL_ID), ra.getPrincipalId());
	}
	
	@Test
	public void testCreate() throws Exception {
		Team team = createTeam(null, "name", "description", null, "101", null, null, null, null);
		when(mockTeamDAO.create(team)).thenReturn(team);
		// mock userGroupDAO
		when(mockUserGroupDAO.create(any(UserGroup.class))).thenReturn(TEAM_ID);
		Team created = teamManagerImpl.create(userInfo,team);
		assertEquals(team, created);
		// verify that group, acl were created
		assertEquals(TEAM_ID, created.getId());
		verify(mockTeamDAO).create(team);
		verify(mockAclDAO).create((AccessControlList)any(), eq(ObjectType.TEAM));
		verify(mockGroupMembersDAO).addMembers(TEAM_ID, Arrays.asList(new String[]{MEMBER_PRINCIPAL_ID}));
		// verify that ID and dates are set in returned team
		assertNotNull(created.getCreatedOn());
		assertNotNull(created.getModifiedOn());
		assertEquals(MEMBER_PRINCIPAL_ID, created.getCreatedBy());
		assertEquals(MEMBER_PRINCIPAL_ID, created.getModifiedBy());
	}
	
	// verify that an invalid team creates an exception
	@Test(expected=InvalidModelException.class)
	public void testCreateInvalidTeam() throws Exception {
		// not allowed to specify ID of team being created
		Team team = createTeam(TEAM_ID, "name", "description", null, "101", null, null, null, null);
		when(mockTeamDAO.create(team)).thenReturn(team);
		teamManagerImpl.create(userInfo,team);
	}
	
	// verify that an invalid team creates an exception
	@Test(expected=NameConflictException.class)
	public void testCreateExistingTeam() throws Exception {
		// not allowed to specify ID of team being created
		Team team = createTeam(null, "name", "description", null, "101", null, null, null, null);
		when(mockUserManager.doesPrincipalExist("name")).thenReturn(true);
		when(mockTeamDAO.create(team)).thenReturn(team);
		teamManagerImpl.create(userInfo,team);
	}
	
	
	@Test
	public void testGetById() throws Exception {
		Team team = createTeam(TEAM_ID, "name", "description", "etag", "101", null, null, null, null);
		when(mockTeamDAO.get(TEAM_ID)).thenReturn(team);
		assertEquals(team, teamManagerImpl.get(TEAM_ID));
	}
	
	@Test
	public void testGetBatch() throws Exception {
		Team team = createTeam(TEAM_ID, "name", "description", "etag", "101", null, null, null, null);
		List<Team> teamList = Arrays.asList(new Team[]{team});
		when(mockTeamDAO.getInRange(10, 0)).thenReturn(teamList);
		when(mockTeamDAO.getCount()).thenReturn(1L);
		PaginatedResults<Team> result = teamManagerImpl.get(10,0);
		assertEquals(teamList, result.getResults());
		assertEquals(1L, result.getTotalNumberOfResults());
	}
	
	@Test
	public void testGetByMember() throws Exception {
		Team team = createTeam(TEAM_ID, "name", "description", "etag", "101", null, null, null, null);
		List<Team> teamList = Arrays.asList(new Team[]{team});
		when(mockTeamDAO.getForMemberInRange(MEMBER_PRINCIPAL_ID, 10, 0)).thenReturn(teamList);
		when(mockTeamDAO.getCountForMember(MEMBER_PRINCIPAL_ID)).thenReturn(1L);
		PaginatedResults<Team> result = teamManagerImpl.getByMember(MEMBER_PRINCIPAL_ID,10,0);
		assertEquals(teamList, result.getResults());
		assertEquals(1L, result.getTotalNumberOfResults());

	}
	
	@Test
	public void testPut() throws Exception {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.UPDATE)).thenReturn(true);
		Team team = createTeam(TEAM_ID, "name", "description", "etag", "101", null, null, null, null);
		when(mockTeamDAO.update(team)).thenReturn(team);
		Team updated = teamManagerImpl.put(userInfo, team);
		assertEquals(updated, team);
		assertNotNull(updated.getModifiedBy());
		assertNotNull(updated.getModifiedOn());
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testUnathorizedPut() throws Exception {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.UPDATE)).thenReturn(false);
		Team team = new Team();
		teamManagerImpl.put(userInfo, team);
	}
	
	@Test
	public void testDelete() throws Exception {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.DELETE)).thenReturn(true);
		teamManagerImpl.delete(userInfo, TEAM_ID);
		verify(mockTeamDAO).delete(TEAM_ID);
		verify(mockAclDAO).delete(TEAM_ID);
		verify(mockUserGroupDAO).delete(TEAM_ID);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testUnauthorizedDelete() throws Exception {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.DELETE)).thenReturn(false);
		teamManagerImpl.delete(userInfo, TEAM_ID);
	}
	
	private void mockUnmetAccessRequirements(boolean hasUnmet, UserInfo userInfo) {
		List<Long> unmetAccessRequirementIds = null;
		if (hasUnmet) {
			unmetAccessRequirementIds = Arrays.asList(new Long[]{123L, 456L});
		} else {
			unmetAccessRequirementIds = Arrays.asList(new Long[]{});
		}
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setType(RestrictableObjectType.TEAM);
		rod.setId(TEAM_ID);
		List<ACCESS_TYPE> accessTypes = new ArrayList<ACCESS_TYPE>();
		accessTypes.add(ACCESS_TYPE.DOWNLOAD);
		accessTypes.add(ACCESS_TYPE.PARTICIPATE);
		Set<Long> principalIds = new HashSet<Long>();
		for (UserGroup ug : userInfo.getGroups()) {
			principalIds.add(Long.parseLong(ug.getId()));
		}
		when(mockAccessRequirementDAO.unmetAccessRequirements(rod, principalIds, accessTypes)).thenReturn(unmetAccessRequirementIds);		
	}
	
	@Test
	public void testCanAddTeamMemberSELF() throws Exception {
		// let the team be a non-Open team (which it is by default)
		Team team = createTeam(TEAM_ID, "name", "description", null, "101", null, null, null, null);
		when(mockTeamDAO.get(TEAM_ID)).thenReturn(team);
		
		// I can add myself if I'm an admin on the Team
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(true);
		assertTrue(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, userInfo));

		// I canNOT add myself if I'm not an admin on the Team if I haven't been invited...
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(false);
		when(mockMembershipInvtnSubmissionDAO.getOpenByTeamAndUserCount(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong())).thenReturn(0L);
		assertFalse(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, userInfo));
		
		// ...unless the team is Open
		team.setCanPublicJoin(true);
		assertTrue(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, userInfo));
		team.setCanPublicJoin(false);
		
		// I can add myself if I'm not an admin on the team if I've been invited
		when(mockMembershipInvtnSubmissionDAO.getOpenByTeamAndUserCount(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong())).thenReturn(1L);
		assertTrue(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, userInfo));
		
		// I can't add myself if I'm invited to some other team...
		when(mockMembershipInvtnSubmissionDAO.getOpenByTeamAndUserCount(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong())).thenReturn(1L);
		assertTrue(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, userInfo));
		String someOtherTeam = "456";
		when(mockMembershipInvtnSubmissionDAO.getOpenByTeamAndUserCount(eq(Long.parseLong(someOtherTeam)), eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong())).thenReturn(1L);
		
		// I can add myself if I'm a Synapse admin
		when(mockAuthorizationManager.canAccess(adminInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(false);
		assertTrue(teamManagerImpl.canAddTeamMember(adminInfo, TEAM_ID, adminInfo));

		// Test access requirements:
		// first, the baseline
		assertTrue(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, userInfo));
		// now add unmet access requirement
		mockUnmetAccessRequirements(true, userInfo);
		// I can no longer join
		assertFalse(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, userInfo));

	}
	
	@Test
	public void testCanAddTeamMemberOTHER() throws Exception {
		// let the team be a non-Open team (which it is by default)
		Team team = createTeam(TEAM_ID, "name", "description", null, "101", null, null, null, null);
		when(mockTeamDAO.get(TEAM_ID)).thenReturn(team);

		// I can add someone else if I'm a Synapse admin
		when(mockAuthorizationManager.canAccess(adminInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(false);
		assertTrue(teamManagerImpl.canAddTeamMember(adminInfo, TEAM_ID, adminInfo));
		
		// I can't add someone else if they haven't requested it
		//	 I am an admin for the team
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(true);
		//	 there has been no membership request
		String otherPrincipalId = "987";
		UserInfo otherUserInfo = createUserInfo(false, otherPrincipalId);
		when(mockMembershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(otherPrincipalId)), anyLong())).thenReturn(0L);
		assertFalse(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, otherUserInfo));
		//	 now there IS a membership request
		when(mockMembershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(otherPrincipalId)), anyLong())).thenReturn(3L);
		assertTrue(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, otherUserInfo));
		
		// also, I can't add them even though there's a request if I'm not an admin on the team
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(false);
		assertFalse(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, otherUserInfo));
		
		// NOTHING CHANGES IF THE TEAM IS OPEN! ...
		team.setCanPublicJoin(true);
		
		// ...NOW JUST REPEAT THE ABOVE TESTS
		// I can add someone else if I'm a Synapse admin
		when(mockAuthorizationManager.canAccess(adminInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(false);
		assertTrue(teamManagerImpl.canAddTeamMember(adminInfo, TEAM_ID, userInfo));
		
		// I can't add someone else if they haven't requested it
		//	 I am an admin for the team
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(true);
		//	 there has been no membership request
		when(mockMembershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(otherPrincipalId)), anyLong())).thenReturn(0L);
		assertFalse(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, otherUserInfo));
		//	 now there IS a membership request
		when(mockMembershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(otherPrincipalId)), anyLong())).thenReturn(3L);
		assertTrue(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, otherUserInfo));
		
		// also, I can't add them even though there's a request if I'm not an admin on the team
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(false);
		assertFalse(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, otherUserInfo));

		// Test access requirements:
		// first, the baseline
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(true);
		assertTrue(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, otherUserInfo));
		// now add unmet access requirement
		// this is OK, since it's the one being added who must meet the access requirements
		mockUnmetAccessRequirements(true, userInfo);
		assertTrue(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, otherUserInfo));
		// but this is not OK...
		mockUnmetAccessRequirements(true, otherUserInfo);
		// ...I can no longer add him
		assertFalse(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, otherUserInfo));
	}
	
	@Test
	public void testAddMember() throws Exception {
		// 'userInfo' is a team admin and there is a membership request from 987
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(true);
		String principalId = "987";
		UserInfo principalUserInfo = createUserInfo(false, principalId);
		when(mockMembershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(principalId)), anyLong())).thenReturn(1L);
		when(mockAclDAO.get(TEAM_ID, ObjectType.TEAM)).
			thenReturn(TeamManagerImpl.createInitialAcl(userInfo, TEAM_ID, new Date()));
		teamManagerImpl.addMember(userInfo, TEAM_ID, principalUserInfo);
		verify(mockGroupMembersDAO).addMembers(TEAM_ID, Arrays.asList(new String[]{principalId}));
		verify(mockMembershipInvtnSubmissionDAO).deleteByTeamAndUser(Long.parseLong(TEAM_ID), Long.parseLong(principalId));
		verify(mockMembershipRqstSubmissionDAO).deleteByTeamAndRequester(Long.parseLong(TEAM_ID), Long.parseLong(principalId));
	}
	
	@Test
	public void testAddMemberAlreadyOnTeam() throws Exception {
		// 'userInfo' is a team admin and there is a membership request from 987
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(true);
		String principalId = "987";
		UserInfo principalUserInfo = createUserInfo(false, principalId);
		when(mockMembershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(principalId)), anyLong())).thenReturn(1L);
		when(mockAclDAO.get(TEAM_ID, ObjectType.TEAM)).
			thenReturn(TeamManagerImpl.createInitialAcl(userInfo, TEAM_ID, new Date()));
		UserGroup ug = new UserGroup();
		ug.setId(principalId);
		when(mockGroupMembersDAO.getMembers(TEAM_ID)).thenReturn(Arrays.asList(new UserGroup[]{ug}));
		teamManagerImpl.addMember(userInfo, TEAM_ID, principalUserInfo);
		verify(mockGroupMembersDAO, times(0)).addMembers(TEAM_ID, Arrays.asList(new String[]{principalId}));
		verify(mockMembershipInvtnSubmissionDAO).deleteByTeamAndUser(Long.parseLong(TEAM_ID), Long.parseLong(principalId));
		verify(mockMembershipRqstSubmissionDAO).deleteByTeamAndRequester(Long.parseLong(TEAM_ID), Long.parseLong(principalId));
	}
	
	@Test
	public void testCanRemoveTeamMember() throws Exception {
		// admin can do anything
		assertTrue(teamManagerImpl.canRemoveTeamMember(adminInfo, TEAM_ID, "987"));
		// anyone can remove self
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(false);
		assertTrue(teamManagerImpl.canRemoveTeamMember(userInfo, TEAM_ID, MEMBER_PRINCIPAL_ID));
		// team admin can remove anyone
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(true);
		assertTrue(teamManagerImpl.canRemoveTeamMember(userInfo, TEAM_ID, "987"));
		// not self or team admin, can't do it
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(false);
		assertFalse(teamManagerImpl.canRemoveTeamMember(userInfo, TEAM_ID, "987"));
	}
	
	@Test
	public void testRemoveMember() throws Exception {
		String memberPrincipalId = "987";
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(true);
		UserGroup ug = new UserGroup();
		ug.setId(memberPrincipalId);
		when(mockGroupMembersDAO.getMembers(TEAM_ID)).thenReturn(Arrays.asList(new UserGroup[]{ug}));
		AccessControlList acl = new AccessControlList();
		acl.setResourceAccess(new HashSet<ResourceAccess>());
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(Long.parseLong(memberPrincipalId));
		acl.getResourceAccess().add(ra);
		when(mockAclDAO.get(TEAM_ID, ObjectType.TEAM)).thenReturn(acl);
		teamManagerImpl.removeMember(userInfo, TEAM_ID, memberPrincipalId);
		verify(mockGroupMembersDAO).removeMembers(TEAM_ID, Arrays.asList(new String[]{memberPrincipalId}));
		verify(mockAclDAO).update((AccessControlList)any(), eq(ObjectType.TEAM));
		assertEquals(0, acl.getResourceAccess().size());
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testRemoveMemberUnathorized() throws Exception {
		String memberPrincipalId = "987";
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(false);
		teamManagerImpl.removeMember(userInfo, TEAM_ID, memberPrincipalId);		
	}
	
	@Test
	public void testRemoveMemberNotInTeam() throws Exception {
		String memberPrincipalId = "987";
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(true);
		when(mockGroupMembersDAO.getMembers(TEAM_ID)).thenReturn(Arrays.asList(new UserGroup[]{}));
		AccessControlList acl = new AccessControlList();
		acl.setResourceAccess(new HashSet<ResourceAccess>());
		when(mockAclDAO.get(TEAM_ID, ObjectType.TEAM)).thenReturn(acl);
		teamManagerImpl.removeMember(userInfo, TEAM_ID, memberPrincipalId);
		verify(mockGroupMembersDAO, times(0)).removeMembers(TEAM_ID, Arrays.asList(new String[]{memberPrincipalId}));
		verify(mockAclDAO, times(0)).update((AccessControlList)any(), eq(ObjectType.TEAM));		
	}
	
	@Test
	public void testGetACL() throws Exception {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.READ)).thenReturn(true);
		teamManagerImpl.getACL(userInfo, TEAM_ID);
		verify(mockAclDAO).get(TEAM_ID, ObjectType.TEAM);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testGetACLUnAuthorized() throws Exception {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.READ)).thenReturn(false);
		teamManagerImpl.getACL(userInfo, TEAM_ID);
	}
	
	@Test
	public void testUpdateACL() throws Exception {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.UPDATE)).thenReturn(true);
		AccessControlList acl = new AccessControlList();
		acl.setId(TEAM_ID);
		teamManagerImpl.updateACL(userInfo, acl);
		verify(mockAclDAO).update(acl, ObjectType.TEAM);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testUpdateACLUnAuthorized() throws Exception {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.UPDATE)).thenReturn(false);
		AccessControlList acl = new AccessControlList();
		acl.setId(TEAM_ID);
		teamManagerImpl.updateACL(userInfo, acl);
	}
	
	@Test
	public void testGetIconURL() throws Exception {
		Team team = createTeam(null, "name", "description", null, "101", null, null, null, null);
		when(mockTeamDAO.get(TEAM_ID)).thenReturn(team);
		teamManagerImpl.getIconURL(TEAM_ID);
		verify(mockFileHandleManager).getRedirectURLForFileHandle("101");
	}
	
	@Test
	public void testGetAllTeamsAndMembers() throws Exception {
		teamManagerImpl.getAllTeamsAndMembers();
		verify(mockTeamDAO).getAllTeamsAndMembers();
	}
	
	@Test
	public void testGetMembers() throws Exception {
		TeamMember tm = new TeamMember();
		tm.setTeamId(TEAM_ID);
		UserGroupHeader ugh = new UserGroupHeader();
		ugh.setOwnerId("101");
		tm.setMember(ugh);
		tm.setIsAdmin(false);
		List<TeamMember> tms = Arrays.asList(new TeamMember[]{tm});
		when(mockTeamDAO.getMembersInRange(TEAM_ID, 10, 0)).thenReturn(tms);
		when(mockTeamDAO.getMembersCount(TEAM_ID)).thenReturn(1L);
		PaginatedResults<TeamMember> pg = teamManagerImpl.getMembers(TEAM_ID, 10, 0);
		assertEquals(tms, pg.getResults());
		assertEquals(1L, pg.getTotalNumberOfResults());
	}
	
	@Test
	public void testSetPermissions() throws Exception {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.UPDATE)).thenReturn(true);
		AccessControlList acl = TeamManagerImpl.createInitialAcl(userInfo, TEAM_ID, new Date());
		when(mockAclDAO.get(TEAM_ID, ObjectType.TEAM)).thenReturn(acl);
		String principalId = MEMBER_PRINCIPAL_ID;
		teamManagerImpl.setPermissions(userInfo, TEAM_ID, principalId, true);
		verify(mockAclDAO).update((AccessControlList)any(),  eq(ObjectType.TEAM));
		// now check that user is actually an admin
		boolean foundRA=false;
		for (ResourceAccess ra: acl.getResourceAccess()) {
			if (principalId.equals(ra.getPrincipalId().toString())) {
				foundRA=true;
				for (ACCESS_TYPE at : TeamManagerImpl.ADMIN_TEAM_PERMISSIONS) {
					assertTrue(ra.getAccessType().contains(at));
				}
			}
		}
		assertTrue(foundRA);
		
		// now remove admin permissions
		teamManagerImpl.setPermissions(userInfo, TEAM_ID, principalId, false);
		foundRA=false;
		for (ResourceAccess ra: acl.getResourceAccess()) {
			if (principalId.equals(ra.getPrincipalId().toString())) {
				foundRA=true;
			}
		}
		assertFalse(foundRA);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testSetPermissionsUnauthorized() throws Exception {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.UPDATE)).thenReturn(false);
		String principalId = MEMBER_PRINCIPAL_ID;
		teamManagerImpl.setPermissions(userInfo, TEAM_ID, principalId, true);
	}
	
	@Test
	public void testIsMembershipApprovalRequired() throws Exception {
		// admin doesn't require approval
		assertFalse(teamManagerImpl.isMembershipApprovalRequired(adminInfo, TEAM_ID));

		// let the team be a non-Open team (which it is by default)
		Team team = createTeam(TEAM_ID, "name", "description", null, "101", null, null, null, null);
		when(mockTeamDAO.get(TEAM_ID)).thenReturn(team);
			
		// a team-admin doesn't require approval
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(true);
		assertFalse(teamManagerImpl.isMembershipApprovalRequired(userInfo, TEAM_ID));

		// a non-team-admin requires approval
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(false);
		assertTrue(teamManagerImpl.isMembershipApprovalRequired(userInfo, TEAM_ID));
		
		// unless it's an open team
		team.setCanPublicJoin(true);
		assertFalse(teamManagerImpl.isMembershipApprovalRequired(userInfo, TEAM_ID));
	}
	
	@Test
	public void testGetTeamMembershipStatus() throws Exception {
		// let the team be a non-Open team (which it is by default)
		Team team = createTeam(TEAM_ID, "name", "description", null, "101", null, null, null, null);
		when(mockTeamDAO.get(TEAM_ID)).thenReturn(team);
		
		String principalId = MEMBER_PRINCIPAL_ID;
		UserInfo principalUserInfo = createUserInfo(false, principalId);
		UserGroup ug = new UserGroup();
		ug.setId(principalId);
		when(mockGroupMembersDAO.getMembers(TEAM_ID)).thenReturn(Arrays.asList(new UserGroup[]{ug}));
		
		when(mockMembershipInvtnSubmissionDAO.getOpenByTeamAndUserCount(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong())).thenReturn(1L);
		when(mockMembershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong())).thenReturn(1L);
		
		TeamMembershipStatus tms = teamManagerImpl.getTeamMembershipStatus(userInfo, TEAM_ID, principalUserInfo);
		assertEquals(TEAM_ID, tms.getTeamId());
		assertEquals(principalId, tms.getUserId());
		assertTrue(tms.getIsMember());
		assertTrue(tms.getHasOpenInvitation());
		assertTrue(tms.getHasOpenRequest());
		assertTrue(tms.getCanJoin());
		assertTrue(tms.getMembershipApprovalRequired());
		assertFalse(tms.getHasUnmetAccessRequirement());
		
		when(mockGroupMembersDAO.getMembers(TEAM_ID)).thenReturn(Arrays.asList(new UserGroup[]{}));
		when(mockMembershipInvtnSubmissionDAO.getOpenByTeamAndUserCount(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong())).thenReturn(0L);
		when(mockMembershipRqstSubmissionDAO.getOpenByTeamAndRequesterCount(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong())).thenReturn(0L);
		tms = teamManagerImpl.getTeamMembershipStatus(userInfo, TEAM_ID, principalUserInfo);
		assertEquals(TEAM_ID, tms.getTeamId());
		assertEquals(principalId, tms.getUserId());
		assertFalse(tms.getIsMember());
		assertFalse(tms.getHasOpenInvitation());
		assertFalse(tms.getHasOpenRequest());
		assertFalse(tms.getCanJoin());
		assertTrue(tms.getMembershipApprovalRequired());
		assertFalse(tms.getHasUnmetAccessRequirement());
		
		// if the team is open the user 'can join' even if they have no invitation
		team.setCanPublicJoin(true);
		tms = teamManagerImpl.getTeamMembershipStatus(userInfo, TEAM_ID, principalUserInfo);
		assertEquals(TEAM_ID, tms.getTeamId());
		assertEquals(principalId, tms.getUserId());
		assertFalse(tms.getIsMember());
		assertFalse(tms.getHasOpenInvitation());
		assertFalse(tms.getHasOpenRequest());
		assertTrue(tms.getCanJoin());
		assertFalse(tms.getMembershipApprovalRequired());
		assertFalse(tms.getHasUnmetAccessRequirement());
		
		mockUnmetAccessRequirements(true, principalUserInfo);
		tms = teamManagerImpl.getTeamMembershipStatus(userInfo, TEAM_ID, principalUserInfo);
		assertEquals(TEAM_ID, tms.getTeamId());
		assertEquals(principalId, tms.getUserId());
		assertFalse(tms.getCanJoin());
		assertFalse(tms.getMembershipApprovalRequired());
		assertTrue(tms.getHasUnmetAccessRequirement());
	}
	


}
