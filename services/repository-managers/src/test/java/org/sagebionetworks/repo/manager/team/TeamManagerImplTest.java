package org.sagebionetworks.repo.manager.team;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.EmailUtils;
import org.sagebionetworks.repo.manager.MessageToUserAndBody;
import org.sagebionetworks.repo.manager.ProjectStatsManager;
import org.sagebionetworks.repo.manager.UserProfileManager;
import org.sagebionetworks.repo.manager.dataaccess.RestrictionInformationManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.FileHandleUrlRequest;
import org.sagebionetworks.repo.manager.principal.PrincipalManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Count;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.MembershipInvitationDAO;
import org.sagebionetworks.repo.model.MembershipRequestDAO;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedTeamIds;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.TeamMemberTypeFilterOptions;
import org.sagebionetworks.repo.model.TeamMembershipStatus;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserGroup;
import org.sagebionetworks.repo.model.dbo.principal.PrincipalPrefixDAO;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.BootstrapTeam;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.util.ModelConstants;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class TeamManagerImplTest {
	@Mock
	private AuthorizationManager mockAuthorizationManager;
	@Mock
	private DBOBasicDao mockBasicDAO;
	@Mock
	private TeamDAO mockTeamDAO;
	@Mock
	private GroupMembersDAO mockGroupMembersDAO;
	@Mock
	private UserGroupDAO mockUserGroupDAO;
	@Mock
	private AccessControlListDAO mockAclDAO;
	@Mock
	private FileHandleManager mockFileHandleManager;
	@Mock
	private MembershipInvitationDAO mockMembershipInvitationDAO;
	@Mock
	private MembershipRequestDAO mockMembershipRequestDAO;
	@Mock
	private RestrictionInformationManager mockRestrictionInformationManager;
	@Mock
	private PrincipalAliasDAO mockPrincipalAliasDAO;
	@Mock
	private PrincipalManager mockPrincipalManager;
	@Mock
	private PrincipalPrefixDAO mockPrincipalPrefixDao;
	@Mock
	private UserProfileManager mockUserProfileManager;
	@Mock
	private ProjectStatsManager mockProjectStatsManager;

	@InjectMocks
	TeamManagerImpl teamManagerImpl;
	
	private UserInfo userInfo;
	private UserInfo adminInfo;
	private UserProfile up;
	
	private static final String MEMBER_PRINCIPAL_ID = "999";
	private static final String TEAM_ID = "123";

	private RestrictionInformationRequest restrictionInfoRqst;
	private RestrictionInformationResponse hasUnmetAccessRqmtResponse;
	private RestrictionInformationResponse noUnmetAccessRqmtResponse;

	@BeforeEach
	public void setUp() {
		userInfo = createUserInfo(false, MEMBER_PRINCIPAL_ID);
		up = new UserProfile();
		up.setFirstName("foo");
		up.setLastName("bar");
		up.setUserName("userName");
		adminInfo = createUserInfo(true, "-1");
		restrictionInfoRqst = new RestrictionInformationRequest();
		restrictionInfoRqst.setRestrictableObjectType(RestrictableObjectType.TEAM);
		restrictionInfoRqst.setObjectId(TEAM_ID);
		hasUnmetAccessRqmtResponse = new RestrictionInformationResponse();
		hasUnmetAccessRqmtResponse.setHasUnmetAccessRequirement(true);
		noUnmetAccessRqmtResponse = new RestrictionInformationResponse();
		noUnmetAccessRqmtResponse.setHasUnmetAccessRequirement(false);
	}
	
	private static UserInfo createUserInfo(boolean isAdmin, String principalId) {
		UserInfo userInfo = new UserInfo(isAdmin, principalId);
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
	public void testValidateForCreate() {
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
		
		Assertions.assertThrows(InvalidModelException.class, ()-> {
			TeamManagerImpl.validateForUpdate(team);
		});	
	}

	@Test
	public void testValidateForUpdate() {
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
	public void testPopulateCreationFields() {
		Team team = new Team();
		Date now = new Date();
		TeamManagerImpl.populateCreationFields(userInfo, team, now);
		assertEquals(userInfo.getId().toString(), team.getCreatedBy());
		assertEquals(now, team.getCreatedOn());
		assertEquals(userInfo.getId().toString(), team.getModifiedBy());
		assertEquals(now, team.getModifiedOn());
	}
	
	
	@Test
	public void testPopulateUpdateFields() {
		Team team = new Team();
		Date now = new Date();
		TeamManagerImpl.populateUpdateFields(userInfo, team, now);
		assertEquals(null, team.getCreatedBy());
		assertEquals(null, team.getCreatedOn());
		assertEquals(userInfo.getId().toString(), team.getModifiedBy());
		assertEquals(now, team.getModifiedOn());
	}
	
	@Test
	public void testCreateAdminAcl() {
		Date now = new Date();
		AccessControlList acl = TeamManagerImpl.createInitialAcl(userInfo, TEAM_ID, now);
		assertEquals(now, acl.getCreationDate());
		assertEquals(TEAM_ID, acl.getId());
		assertEquals(3, acl.getResourceAccess().size());
		for (ResourceAccess ra : acl.getResourceAccess()) {
			if (ra.getPrincipalId().toString().equals(MEMBER_PRINCIPAL_ID)) {
				assertEquals(new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[]{
						ACCESS_TYPE.READ, 
						ACCESS_TYPE.UPDATE, 
						ACCESS_TYPE.DELETE, 
						ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE, 
						ACCESS_TYPE.SEND_MESSAGE})), ra.getAccessType());
			} else if (ra.getPrincipalId().toString().equals(TEAM_ID)) {
				assertEquals(new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[]{
						ACCESS_TYPE.READ, 
						ACCESS_TYPE.SEND_MESSAGE})), ra.getAccessType());
				
			} else if (ra.getPrincipalId().equals(AuthorizationConstants.BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId())) {
				assertEquals(new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[]{
						ACCESS_TYPE.READ, 
						ACCESS_TYPE.SEND_MESSAGE})), ra.getAccessType());
			} else {
				fail("Unexpected principal ID"+ra.getPrincipalId());
	}
		}
	}
	
	@Test
	public void testAddToAcl() {
		AccessControlList acl = new AccessControlList();
		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		acl.setResourceAccess(ras);
		TeamManagerImpl.addToACL(acl, MEMBER_PRINCIPAL_ID, Collections.singleton(ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE));
		assertEquals(1, acl.getResourceAccess().size());
		ResourceAccess ra = acl.getResourceAccess().iterator().next();
		assertEquals(new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[]{ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE})), ra.getAccessType());
		assertEquals((Long)Long.parseLong(MEMBER_PRINCIPAL_ID), ra.getPrincipalId());
	}
	
	@Test
	public void testCreate() {
		Team team = createTeam(null, "name", "description", null, "101", null, null, null, null);

		when(mockAuthorizationManager.canAccessRawFileHandleById(any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockUserGroupDAO.create(any(UserGroup.class))).thenReturn(Long.parseLong(TEAM_ID));
		when(mockTeamDAO.create(team)).thenReturn(team);
		
		// Call under test
		Team created = teamManagerImpl.create(userInfo,team);
		assertEquals(team, created);
		/*
		 *  PLFM-3078 - verify that UserGroup's creationDate is set
		 */
		ArgumentCaptor<UserGroup> captor = ArgumentCaptor.forClass(UserGroup.class);
		verify(mockUserGroupDAO).create(captor.capture());
		UserGroup ug = captor.getValue();
		assertNotNull(ug);
		assertNotNull(ug.getCreationDate());

		// verify that group, acl were created
		assertEquals(TEAM_ID, created.getId());
		verify(mockAuthorizationManager).canAccessRawFileHandleById(userInfo, "101");
		verify(mockTeamDAO).create(team);
		verify(mockAclDAO).create((AccessControlList)any(), eq(ObjectType.TEAM));
		verify(mockGroupMembersDAO).addMembers(TEAM_ID, Arrays.asList(new String[]{MEMBER_PRINCIPAL_ID}));
		// verify that ID and dates are set in returned team
		assertNotNull(created.getCreatedOn());
		assertNotNull(created.getModifiedOn());
		assertEquals(MEMBER_PRINCIPAL_ID, created.getCreatedBy());
		assertEquals(MEMBER_PRINCIPAL_ID, created.getModifiedBy());
	}
	
	@Test
	public void testCreateWithNotAllowedIcon() {
		Team team = createTeam(null, "name", "description", null, "101", null, null, null, null);

		when(mockAuthorizationManager.canAccessRawFileHandleById(any(), any())).thenReturn(AuthorizationStatus.accessDenied("Denied"));
		
		String errorMessage = assertThrows(UnauthorizedException.class, () -> {	
			// Call under test
			teamManagerImpl.create(userInfo,team);
		}).getMessage();
		
		assertEquals("Only the user that uploaded the file can set it as the team icon.", errorMessage);
		
		// No check on the file handle since it's not supplied
		verify(mockAuthorizationManager).canAccessRawFileHandleById(userInfo, "101");
	}
	
	@Test
	public void testCreateWithNoIcon() {
		Team team = createTeam(null, "name", "description", null, null, null, null, null, null);

		when(mockUserGroupDAO.create(any(UserGroup.class))).thenReturn(Long.parseLong(TEAM_ID));
		when(mockTeamDAO.create(team)).thenReturn(team);
		
		// Call under test
		teamManagerImpl.create(userInfo,team);
		
		// No check on the file handle since it's not supplied
		verify(mockAuthorizationManager, never()).canAccessRawFileHandleById(any(), any());
	}
	
	private BootstrapTeam createBootstrapTeam(String id, String name) {
		BootstrapTeam team = new BootstrapTeam();
		team.setId(id);
		team.setName(name);
		return team;
	}
	
	@Test
	public void testBootstrapTeamsNonboostrap() {
		List<BootstrapTeam> toBootstrap = Lists.newArrayList();
		toBootstrap.add(createBootstrapTeam("32","Bootstrap Team 1"));
		teamManagerImpl.setTeamsToBootstrap(toBootstrap);
		Assertions.assertThrows(IllegalArgumentException.class, ()-> {
			teamManagerImpl.bootstrapTeams();
		});
	}
	
	@Test
	public void testBootstrapTeamsThatDontExist() {
		when(mockTeamDAO.get(any(String.class))).thenThrow(new NotFoundException());
		when(mockPrincipalAliasDAO.findPrincipalWithAlias(any(String.class))).thenReturn(null);
		when(mockPrincipalAliasDAO.bindAliasToPrincipal(any(PrincipalAlias.class))).thenReturn(new PrincipalAlias());
		when(mockPrincipalManager.isAliasValid(any(String.class), eq(AliasType.TEAM_NAME))).thenReturn(true);
		Team mockTeam = new Team();
		mockTeam.setId("101");
		when(mockTeamDAO.create(any(Team.class))).thenReturn(mockTeam);
		
		List<BootstrapTeam> toBootstrap = Lists.newArrayList();
		toBootstrap.add(createBootstrapTeam(
				""+AuthorizationConstants.BOOTSTRAP_PRINCIPAL.ADMINISTRATORS_GROUP.getPrincipalId(),"Bootstrap Team 1"));
		toBootstrap.add(createBootstrapTeam(
				""+AuthorizationConstants.BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId(),"Bootstrap Team 2"));
		teamManagerImpl.setTeamsToBootstrap(toBootstrap);
		teamManagerImpl.bootstrapTeams();
		
		verify(mockTeamDAO, times(2)).get(any(String.class));
		verify(mockBasicDAO, times(2)).createOrUpdate(any(DBOUserGroup.class));
		verify(mockPrincipalAliasDAO, times(2)).findPrincipalWithAlias(any(String.class));
		verify(mockPrincipalAliasDAO, times(2)).bindAliasToPrincipal(any(PrincipalAlias.class));
		verify(mockPrincipalManager, times(2)).isAliasValid(any(String.class), eq(AliasType.TEAM_NAME));
		verify(mockTeamDAO, times(2)).create(any(Team.class));
		verify(mockAclDAO, times(2)).create(any(AccessControlList.class), eq(ObjectType.TEAM));
	}
	
	@Test
	public void testRerunBootstrapTeamsIdempotent() {
		BootstrapTeam team1 = createBootstrapTeam("32","Bootstrap Team 1");
		BootstrapTeam team2 = createBootstrapTeam("42","Bootstrap Team 2");
		List<BootstrapTeam> toBootstrap = Lists.newArrayList();
		toBootstrap.add(team1);
		toBootstrap.add(team2);
		
		when(mockTeamDAO.get(team1.getId())).thenReturn(new Team());
		when(mockTeamDAO.get(team2.getId())).thenReturn(new Team());
		when(mockPrincipalManager.isAliasValid(any(String.class), eq(AliasType.TEAM_NAME))).thenReturn(true);
		Mockito.verifyZeroInteractions(mockTeamDAO);
		Mockito.verifyZeroInteractions(mockBasicDAO);
		Mockito.verifyZeroInteractions(mockPrincipalAliasDAO);
		Mockito.verifyZeroInteractions(mockAclDAO);

		teamManagerImpl.setTeamsToBootstrap(toBootstrap);
		teamManagerImpl.bootstrapTeams();
		
		verify(mockTeamDAO, times(2)).get(any(String.class));
	}
	
	// verify that an invalid team creates an exception
	@Test
	public void testCreateInvalidTeam() {
		// not allowed to specify ID of team being created
		Team team = createTeam(TEAM_ID, "name", "description", null, "101", null, null, null, null);
		Assertions.assertThrows(InvalidModelException.class, ()-> {
			teamManagerImpl.create(userInfo,team);
		});
	}
	
	// verify that an invalid team creates an exception
	@Test
	public void testCreateExistingTeam() {
		// not allowed to specify ID of team being created
		Team team = createTeam(null, "name", "description", null, "101", null, null, null, null);
		when(mockPrincipalAliasDAO.bindAliasToPrincipal(any(PrincipalAlias.class))).thenThrow(new NameConflictException());
		
		Assertions.assertThrows(NameConflictException.class, ()-> {
			teamManagerImpl.create(userInfo,team);
		});
	}
	
	
	@Test
	public void testGetById() {
		Team team = createTeam(TEAM_ID, "name", "description", "etag", "101", null, null, null, null);
		when(mockTeamDAO.get(TEAM_ID)).thenReturn(team);
		assertEquals(team, teamManagerImpl.get(TEAM_ID));
		verify(mockTeamDAO).get(TEAM_ID);
	}
	
	@Test
	public void testGetBatch() {
		Team team = createTeam(TEAM_ID, "name", "description", "etag", "101", null, null, null, null);
		ListWrapper<Team> teamList = ListWrapper.wrap(Arrays.asList(new Team[]{team}), Team.class);
		when(mockTeamDAO.getInRange(10, 0)).thenReturn(teamList.getList());
		PaginatedResults<Team> result = teamManagerImpl.list(10,0);
		assertEquals(teamList.getList(), result.getResults());
		assertEquals(1L, result.getTotalNumberOfResults());
		
		when(mockTeamDAO.list(Collections.singletonList(101L))).thenReturn(teamList);
		assertEquals(teamList, teamManagerImpl.list(Collections.singletonList(101L)));
	}
	
	@Test
	public void testGetByMember() {
		Team team = createTeam(TEAM_ID, "name", "description", "etag", "101", null, null, null, null);
		List<Team> teamList = Arrays.asList(new Team[]{team});
		when(mockTeamDAO.getForMemberInRange(MEMBER_PRINCIPAL_ID, 10, 0)).thenReturn(teamList);
		PaginatedResults<Team> result = teamManagerImpl.listByMember(MEMBER_PRINCIPAL_ID,10,0);
		assertEquals(teamList, result.getResults());
		assertEquals(1L, result.getTotalNumberOfResults());

	}

	@Test
	public void testListIdsByMemberWithNullNextPageToken() {
		List<String> expected = new ArrayList<>();
		when(mockTeamDAO.getIdsForMember(MEMBER_PRINCIPAL_ID, NextPageToken.DEFAULT_LIMIT + 1,
				NextPageToken.DEFAULT_OFFSET, null, null)).thenReturn(expected);
		PaginatedTeamIds result = teamManagerImpl.listIdsByMember(MEMBER_PRINCIPAL_ID, null, null, null);
		verify(mockTeamDAO).getIdsForMember(MEMBER_PRINCIPAL_ID, NextPageToken.DEFAULT_LIMIT + 1,
				NextPageToken.DEFAULT_OFFSET, null, null);
		assertEquals(expected, result.getTeamIds());
		assertNull(result.getNextPageToken());
	}

	@Test
	public void testListIdsByMemberWithNextPageToken() {
		List<String> expected = new ArrayList<>(Arrays.asList("1" , "2"));
		when(mockTeamDAO.getIdsForMember(MEMBER_PRINCIPAL_ID, 2, 0, null, null)).thenReturn(expected);
		PaginatedTeamIds result = teamManagerImpl.listIdsByMember(MEMBER_PRINCIPAL_ID, new NextPageToken(1, 0).toToken(), null, null);
		verify(mockTeamDAO).getIdsForMember(MEMBER_PRINCIPAL_ID, 2, 0, null, null);
		assertEquals(expected, result.getTeamIds());
		assertEquals(new NextPageToken(1, 1).toToken(), result.getNextPageToken());
	}
	
	@Test
	public void testPut() {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		Team team = createTeam(TEAM_ID, "name", "description", "etag", "101", null, null, null, null);
		when(mockTeamDAO.get(any())).thenReturn(team);
		when(mockTeamDAO.update(team)).thenReturn(team);
		
		// Call under test
		Team updated = teamManagerImpl.put(userInfo, team);
		
		assertEquals(updated, team);
		assertNotNull(updated.getModifiedBy());
		assertNotNull(updated.getModifiedOn());
		
		verify(mockTeamDAO).get(TEAM_ID);
		verify(mockTeamDAO).update(team);
	}
	
	@Test
	public void testPutWithNoIcon() {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		Team existingTeam = createTeam(TEAM_ID, "name", "description", "etag", "101", null, null, null, null);
		Team team = createTeam(TEAM_ID, "name", "description", "etag", null, null, null, null, null);
		
		when(mockTeamDAO.get(any())).thenReturn(existingTeam);
		when(mockTeamDAO.update(team)).thenReturn(team);
		
		// Call under test
		Team updated = teamManagerImpl.put(userInfo, team);
		
		assertEquals(updated, team);
		
		verify(mockTeamDAO).get(TEAM_ID);
		verify(mockAuthorizationManager, never()).canAccessRawFileHandleById(any(), any());
	}
	
	@Test
	public void testPutWithNewIcon() {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		Team existingTeam = createTeam(TEAM_ID, "name", "description", "etag", "101", null, null, null, null);
		Team team = createTeam(TEAM_ID, "name", "description", "etag", "102", null, null, null, null);
		
		when(mockTeamDAO.get(any())).thenReturn(existingTeam);
		when(mockAuthorizationManager.canAccessRawFileHandleById(any(), any())).thenReturn(AuthorizationStatus.authorized());
		when(mockTeamDAO.update(team)).thenReturn(team);
		
		// Call under test
		Team updated = teamManagerImpl.put(userInfo, team);
		
		assertEquals(updated, team);
		
		verify(mockTeamDAO).get(TEAM_ID);
		verify(mockAuthorizationManager).canAccessRawFileHandleById(userInfo, "102");
	}
	
	@Test
	public void testPutWithNotAllowedIcon() {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		Team existingTeam = createTeam(TEAM_ID, "name", "description", "etag", "101", null, null, null, null);
		Team team = createTeam(TEAM_ID, "name", "description", "etag", "102", null, null, null, null);
		
		when(mockTeamDAO.get(any())).thenReturn(existingTeam);
		when(mockAuthorizationManager.canAccessRawFileHandleById(any(), any())).thenReturn(AuthorizationStatus.accessDenied("Denied"));
		
		String errorMessage = assertThrows(UnauthorizedException.class, () -> {			
			// Call under test
			teamManagerImpl.put(userInfo, team);
		}).getMessage();

		assertEquals("Only the user that uploaded the file can set it as the team icon.", errorMessage);
		verify(mockTeamDAO).get(TEAM_ID);
		verify(mockAuthorizationManager).canAccessRawFileHandleById(userInfo, "102");

	}
	
	@Test
	public void testUnathorizedPut() {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.accessDenied(""));
		Team team = new Team();
		team.setId(TEAM_ID);
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			teamManagerImpl.put(userInfo, team);
		});
	}
	
	@Test
	public void testDelete() {
		Team retrievedTeam = new Team();
		retrievedTeam.setName("Some name");
		retrievedTeam.setId(TEAM_ID);
		when(mockTeamDAO.get(TEAM_ID)).thenReturn(retrievedTeam);
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.DELETE)).thenReturn(AuthorizationStatus.authorized());
		teamManagerImpl.delete(userInfo, TEAM_ID);
		verify(mockTeamDAO).delete(TEAM_ID);
		verify(mockAclDAO).delete(TEAM_ID, ObjectType.TEAM);
		verify(mockUserGroupDAO).delete(TEAM_ID);
		verify(mockTeamDAO).get(TEAM_ID);
	}
	
	@Test
	public void testUnauthorizedDelete() {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.DELETE)).thenReturn(AuthorizationStatus.accessDenied(""));
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			teamManagerImpl.delete(userInfo, TEAM_ID);
		});
	}
	
	@Test
	public void testCanAddTeamMemberSELF() {
		when(mockRestrictionInformationManager.
				getRestrictionInformation(userInfo, restrictionInfoRqst)).
					thenReturn(noUnmetAccessRqmtResponse);
		// let the team be a non-Open team (which it is by default)
		Team team = createTeam(TEAM_ID, "name", "description", null, "101", null, null, null, null);
		when(mockTeamDAO.get(TEAM_ID)).thenReturn(team);
		
		// I can add myself if I'm an admin on the Team
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationStatus.authorized());
		assertEquals(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, userInfo, false), TeamManagerImpl.AUTHORIZED_ADD_TEAM_MEMBER);

		// I canNOT add myself if I'm not an admin on the Team if I haven't been invited...
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationStatus.accessDenied(""));
		when(mockMembershipInvitationDAO.getOpenByTeamAndUserCount(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong())).thenReturn(0L);
		assertEquals(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, userInfo, false), TeamManagerImpl.UNAUTHORIZED_ADD_TEAM_MEMBER_MUST_HAVE_INVITATION);
		// ... but it returns true if I'm already on the team...
		assertEquals(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, userInfo, true), TeamManagerImpl.AUTHORIZED_ADD_TEAM_MEMBER);
		
		// ...or if the team is Open
		team.setCanPublicJoin(true);
		assertEquals(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, userInfo, false), TeamManagerImpl.AUTHORIZED_ADD_TEAM_MEMBER);
		team.setCanPublicJoin(false);
		
		// I can add myself if I'm not an admin on the team if I've been invited
		when(mockMembershipInvitationDAO.getOpenByTeamAndUserCount(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong())).thenReturn(1L);
		assertEquals(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, userInfo, false), TeamManagerImpl.AUTHORIZED_ADD_TEAM_MEMBER);
		
		// I can't add myself if I'm invited to some other team...
		when(mockMembershipInvitationDAO.getOpenByTeamAndUserCount(
				eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong())).thenReturn(1L);
		assertEquals(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, userInfo, false),  TeamManagerImpl.AUTHORIZED_ADD_TEAM_MEMBER);
		
		when(mockMembershipInvitationDAO.getOpenByTeamAndUserCount(
				eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong())).thenReturn(0L);
		assertEquals(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, userInfo, false), TeamManagerImpl.UNAUTHORIZED_ADD_TEAM_MEMBER_MUST_HAVE_INVITATION);
		// ok if I'm already in the team
		assertEquals(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, userInfo, true), TeamManagerImpl.AUTHORIZED_ADD_TEAM_MEMBER);
		// restore the mock
		when(mockMembershipInvitationDAO.getOpenByTeamAndUserCount(
				eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong())).thenReturn(1L);

		assertEquals(teamManagerImpl.canAddTeamMember(adminInfo, TEAM_ID, adminInfo, false), TeamManagerImpl.AUTHORIZED_ADD_TEAM_MEMBER);

		// Test access requirements:
		// first, the baseline
		assertEquals(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, userInfo, false), TeamManagerImpl.AUTHORIZED_ADD_TEAM_MEMBER);
		// now add unmet access requirement
		when(mockRestrictionInformationManager.
				getRestrictionInformation(userInfo, restrictionInfoRqst)).
					thenReturn(hasUnmetAccessRqmtResponse);
		// I can no longer join
		assertEquals(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, userInfo, false), TeamManagerImpl.UNAUTHORIZED_ADD_TEAM_MEMBER_UNMET_AR_SELF);
		verify(mockTeamDAO, times(7)).get(TEAM_ID);
	}
	
	@Test
	public void testCanAddTeamMemberOTHER() {
		// I can add someone else if I'm a Synapse admin
		assertEquals(teamManagerImpl.canAddTeamMember(adminInfo, TEAM_ID, adminInfo, false), TeamManagerImpl.AUTHORIZED_ADD_TEAM_MEMBER);
		
		// I can't add someone else if they haven't requested it
		//	 I am an admin for the team
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationStatus.authorized());
		//	 there has been no membership request
		String otherPrincipalId = "987";
		UserInfo otherUserInfo = createUserInfo(false, otherPrincipalId);
		when(mockRestrictionInformationManager.
				getRestrictionInformation(otherUserInfo, restrictionInfoRqst)).
					thenReturn(noUnmetAccessRqmtResponse);

		when(mockMembershipRequestDAO.getOpenByTeamAndRequesterCount(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(otherPrincipalId)), anyLong())).thenReturn(0L);
		assertEquals(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, otherUserInfo, false), TeamManagerImpl.UNAUTHORIZED_ADD_TEAM_MEMBER_MUST_HAVE_REQUEST);
		// but the check returns true if I'm already on the Team
		assertEquals(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, otherUserInfo, true), TeamManagerImpl.AUTHORIZED_ADD_TEAM_MEMBER);
		
		//	 now there IS a membership request
		when(mockMembershipRequestDAO.getOpenByTeamAndRequesterCount(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(otherPrincipalId)), anyLong())).thenReturn(3L);
		assertEquals(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, otherUserInfo, false), TeamManagerImpl.AUTHORIZED_ADD_TEAM_MEMBER);
		
		// also, I can't add them even though there's a request if I'm not an admin on the team
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationStatus.accessDenied(""));
		assertEquals(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, otherUserInfo, false), TeamManagerImpl.UNAUTHORIZED_ADD_TEAM_MEMBER_MUST_BE_TEAM_MANAGER);

		// Test access requirements:
		// first, the baseline
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationStatus.authorized());
		assertEquals(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, otherUserInfo, false), TeamManagerImpl.AUTHORIZED_ADD_TEAM_MEMBER);
		// now add unmet access requirement
		// this is OK, since it's the one being added who must meet the access requirements
		assertEquals(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, otherUserInfo, false), TeamManagerImpl.AUTHORIZED_ADD_TEAM_MEMBER);
		// but this is not OK...
		when(mockRestrictionInformationManager.
				getRestrictionInformation(otherUserInfo, restrictionInfoRqst)).
					thenReturn(hasUnmetAccessRqmtResponse);
		// ...I can no longer add him
		assertEquals(teamManagerImpl.canAddTeamMember(userInfo, TEAM_ID, otherUserInfo, false), TeamManagerImpl.UNAUTHORIZED_ADD_TEAM_MEMBER_UNMET_AR_OTHER);
	}
	
	@Test
	public void testAddMember() {
		// 'userInfo' is a team admin and there is a membership request from 987
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationStatus.authorized());
		String principalId = "987";
		UserInfo principalUserInfo = createUserInfo(false, principalId);
		when(mockMembershipRequestDAO.getOpenByTeamAndRequesterCount(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(principalId)), anyLong())).thenReturn(1L);
		when(mockRestrictionInformationManager.
				getRestrictionInformation(principalUserInfo, restrictionInfoRqst)).
					thenReturn(noUnmetAccessRqmtResponse);
		
		boolean added = teamManagerImpl.addMember(userInfo, TEAM_ID, principalUserInfo);
		assertTrue(added);
		verify(mockGroupMembersDAO).addMembers(TEAM_ID, Arrays.asList(new String[]{principalId}));
		verify(mockMembershipInvitationDAO).deleteByTeamAndUser(Long.parseLong(TEAM_ID), Long.parseLong(principalId));
		verify(mockMembershipRequestDAO).deleteByTeamAndRequester(Long.parseLong(TEAM_ID), Long.parseLong(principalId));
		verify(mockProjectStatsManager).memberAddedToTeam(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(principalId)), any(Date.class));
	}
	
	@Test
	public void testAddMemberAlreadyOnTeam() {
		// 'userInfo' is a team admin and there is a membership request from 987
		String principalId = "987";
		UserInfo principalUserInfo = createUserInfo(false, principalId);
		when(mockGroupMembersDAO.getMemberIdsForUpdate(Long.valueOf(TEAM_ID))).thenReturn(ImmutableSet.of(Long.valueOf(principalId)));
		boolean added = teamManagerImpl.addMember(userInfo, TEAM_ID, principalUserInfo);
		assertFalse(added);
		verify(mockGroupMembersDAO, never()).addMembers(TEAM_ID, Arrays.asList(new String[]{principalId}));
		verify(mockMembershipInvitationDAO).deleteByTeamAndUser(Long.parseLong(TEAM_ID), Long.parseLong(principalId));
		verify(mockMembershipRequestDAO).deleteByTeamAndRequester(Long.parseLong(TEAM_ID), Long.parseLong(principalId));
	}
	
    @Test
    public void testCanRemoveTeamMember() {
            // admin can do anything
            assertTrue(teamManagerImpl.canRemoveTeamMember(adminInfo, TEAM_ID, "987"));
            // anyone can remove self
            when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationStatus.accessDenied(""));
            assertTrue(teamManagerImpl.canRemoveTeamMember(userInfo, TEAM_ID, MEMBER_PRINCIPAL_ID));
            // team admin can remove anyone
            when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationStatus.authorized());
            assertTrue(teamManagerImpl.canRemoveTeamMember(userInfo, TEAM_ID, "987"));
            // not self or team admin, can't do it
            when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationStatus.accessDenied(""));
            assertFalse(teamManagerImpl.canRemoveTeamMember(userInfo, TEAM_ID, "987"));
    }
	
	@Test
	public void testRemoveMember() {
		String memberPrincipalId = "987";
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationStatus.authorized());
		when(mockGroupMembersDAO.getMemberIdsForUpdate(Long.valueOf(TEAM_ID))).thenReturn(ImmutableSet.of(Long.valueOf(memberPrincipalId), 0L));
		AccessControlList acl = new AccessControlList();
		acl.setResourceAccess(new HashSet<ResourceAccess>());
		acl.getResourceAccess().add(TeamManagerImpl.createResourceAccess(Long.parseLong(memberPrincipalId), Collections.singleton(ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)));
		acl.getResourceAccess().add(TeamManagerImpl.createResourceAccess(Long.parseLong("000"), Collections.singleton(ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)));
		when(mockAclDAO.get(TEAM_ID, ObjectType.TEAM)).thenReturn(acl);
		teamManagerImpl.removeMember(userInfo, TEAM_ID, memberPrincipalId);
		verify(mockGroupMembersDAO).removeMembers(TEAM_ID, Arrays.asList(new String[]{memberPrincipalId}));
		verify(mockAclDAO).update((AccessControlList)any(), eq(ObjectType.TEAM));
		assertEquals(1, acl.getResourceAccess().size());
	}
	
	@Test
	public void testRemoveLastAdminMember() {
		String memberPrincipalId = "987";
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationStatus.authorized());
		// there are two members, but only one is an admin
		when(mockGroupMembersDAO.getMemberIdsForUpdate(Long.valueOf(TEAM_ID))).thenReturn(ImmutableSet.of(Long.valueOf(memberPrincipalId), 123L));
		AccessControlList acl = new AccessControlList();
		acl.setResourceAccess(new HashSet<ResourceAccess>());
		acl.getResourceAccess().add(TeamManagerImpl.createResourceAccess(Long.parseLong(memberPrincipalId), Collections.singleton(ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)));
		when(mockAclDAO.get(TEAM_ID, ObjectType.TEAM)).thenReturn(acl);
		
		Assertions.assertThrows(InvalidModelException.class, ()-> {
			teamManagerImpl.removeMember(userInfo, TEAM_ID, memberPrincipalId);
		});
	}
	
	/*
	 * create team T
	in the team ACL, set the team to administrator
	remove the user from the team
	--> At this point the team is empty, no one can administer the team
	 */
	@Test
	public void testPLFM_3612() {
		// this user is the only one in the team
		when(mockGroupMembersDAO.getMemberIdsForUpdate(Long.valueOf(TEAM_ID))).thenReturn(Collections.singleton(Long.valueOf(MEMBER_PRINCIPAL_ID)));
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			// if we remove the only member then there is no one who can administer the team
			teamManagerImpl.removeMember(userInfo, TEAM_ID, MEMBER_PRINCIPAL_ID);
		});
	}
	
	@Test
	public void testRemoveMemberUnauthorized() {
		String memberPrincipalId = "987";
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationStatus.accessDenied(""));
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			teamManagerImpl.removeMember(userInfo, TEAM_ID, memberPrincipalId);		
		});
	}
	
	@Test
	public void testRemoveMemberNotInTeam() {
		String memberPrincipalId = "987";
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationStatus.authorized());
		when(mockGroupMembersDAO.getMemberIdsForUpdate(Long.valueOf(TEAM_ID))).thenReturn(Collections.singleton(123L));
		teamManagerImpl.removeMember(userInfo, TEAM_ID, memberPrincipalId);
		verify(mockGroupMembersDAO, times(0)).removeMembers(TEAM_ID, Arrays.asList(new String[]{memberPrincipalId}));
		verify(mockAclDAO, times(0)).update((AccessControlList)any(), eq(ObjectType.TEAM));
	}
	
	@Test
	public void testGetACL() {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.authorized());
		teamManagerImpl.getACL(userInfo, TEAM_ID);
		verify(mockAclDAO).get(TEAM_ID, ObjectType.TEAM);
	}
	
	@Test
	public void testGetACLUnAuthorized() {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.READ)).thenReturn(AuthorizationStatus.accessDenied(""));
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			teamManagerImpl.getACL(userInfo, TEAM_ID);
		});
	}
	
	@Test
	public void testUpdateACL() {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		AccessControlList acl = new AccessControlList();
		acl.setId(TEAM_ID);
		teamManagerImpl.updateACL(userInfo, acl);
		verify(mockAclDAO).update(acl, ObjectType.TEAM);
	}
	
	@Test
	public void testUpdateACLUnAuthorized() {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.accessDenied(""));
		AccessControlList acl = new AccessControlList();
		acl.setId(TEAM_ID);
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			teamManagerImpl.updateACL(userInfo, acl);
		});
	}
	
	@Test
	public void testGetIconURL() {
		String iconFileHandleId = "101";
		
		Team team = createTeam(null, "name", "description", null, iconFileHandleId, null, null, null, null);
		when(mockTeamDAO.get(TEAM_ID)).thenReturn(team);
		
		FileHandleUrlRequest urlRequest = new FileHandleUrlRequest(userInfo, iconFileHandleId)
				.withAssociation(FileHandleAssociateType.TeamAttachment, TEAM_ID);
		
		String expectedUrl = "https://testurl.org";
		
		when(mockFileHandleManager.getRedirectURLForFileHandle(eq(urlRequest))).thenReturn(expectedUrl);
		
		String url = teamManagerImpl.getIconURL(userInfo, TEAM_ID);
		
		verify(mockFileHandleManager).getRedirectURLForFileHandle(eq(urlRequest));
		verify(mockTeamDAO).get(TEAM_ID);
		assertEquals(expectedUrl, url);
	}

	@Test
	public void testGetIconURLWithNullUserInfo() {
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			teamManagerImpl.getIconURL(null, TEAM_ID);
		});
		assertEquals("userInfo is required.", exception.getMessage());
	}

	@Test
	public void testGetIconURLWithNullTeamId() {
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			teamManagerImpl.getIconURL(userInfo, null);
		});
		assertEquals("teamId is required.", exception.getMessage());
	}

	@Test
	public void testGetIconPreviewURLPreviewFileHandleIdNotFoundException() {

		NotFoundException cause = new NotFoundException("inner exception");
		String iconFileHandleId = "";
		Team team = createTeam(TEAM_ID, "name", "description", null, iconFileHandleId, null, null, null, null);
        when(mockTeamDAO.get(TEAM_ID)).thenReturn(team);
		when(mockFileHandleManager.getPreviewFileHandleId(iconFileHandleId)).thenThrow(cause);

		NotFoundException exception = assertThrows(NotFoundException.class, () -> {
			// Call under test
			teamManagerImpl.getIconPreviewURL(userInfo, TEAM_ID);
		});
		verify(mockTeamDAO).get(TEAM_ID);
		assertEquals("No preview was found for the icon of the team with id: " + TEAM_ID, exception.getMessage());
		assertEquals(cause, exception.getCause());
		assertEquals(cause.getMessage(), exception.getCause().getMessage());
	}

	@Test
	public void testGetIconPreviewURL() {
		String iconFileHandleId = "101";

		Team team = createTeam(null, "name", "description", null, iconFileHandleId, null, null, null, null);
		when(mockTeamDAO.get(TEAM_ID)).thenReturn(team);

		String iconPreviewFileHandleId = "102";

		when(mockFileHandleManager.getPreviewFileHandleId(iconFileHandleId)).thenReturn(iconPreviewFileHandleId);

		FileHandleUrlRequest urlRequest = new FileHandleUrlRequest(userInfo, iconPreviewFileHandleId)
				.withAssociation(FileHandleAssociateType.TeamAttachment, TEAM_ID);

		String expectedUrl = "https://testurl.org";

		when(mockFileHandleManager.getRedirectURLForFileHandle(any())).thenReturn(expectedUrl);

		// Call under test
		String url = teamManagerImpl.getIconPreviewURL(userInfo, TEAM_ID);

		verify(mockFileHandleManager).getRedirectURLForFileHandle(urlRequest);
		verify(mockTeamDAO).get(TEAM_ID);
		assertEquals(expectedUrl, url);
	}

	@Test
	public void testGetIconPreviewURLWithNullUserInfo() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			teamManagerImpl.getIconPreviewURL(null, TEAM_ID);
		});
        assertEquals("userInfo is required.", exception.getMessage());
	}

	@Test
	public void testGetIconPreviewURLWithNullTeamId() {
		Exception exception = assertThrows(IllegalArgumentException.class, () -> {
			teamManagerImpl.getIconPreviewURL(userInfo, null);
		});
		assertEquals("teamId is required.", exception.getMessage());
	}

	@Test
	public void testGetFileHandleIdNotFoundException() {
		Team team = createTeam(TEAM_ID, "name", "description", null, null, null, null, null, null);
		when(mockTeamDAO.get(TEAM_ID)).thenReturn(team);
		Exception exception = assertThrows(NotFoundException.class, () -> {
			// Call under test
			teamManagerImpl.getFileHandleId(TEAM_ID);
		});
		assertEquals("Team " + TEAM_ID + " has no icon file handle.", exception.getMessage());
		verify(mockTeamDAO).get(TEAM_ID);
	}

	@Test
	public void testGetFileHandleId() {
		String iconFileHandleId = "101";
		Team team = createTeam(TEAM_ID, "name", "description", null, iconFileHandleId, null, null, null, null);
		when(mockTeamDAO.get(TEAM_ID)).thenReturn(team);
		// Call under test
		String result = teamManagerImpl.getFileHandleId(TEAM_ID);
		assertEquals(iconFileHandleId, result);
		verify(mockTeamDAO).get(TEAM_ID);
	}
	
	@Test
	public void testGetAllTeamsAndMembers() {
		teamManagerImpl.listAllTeamsAndMembers();
		verify(mockTeamDAO).getAllTeamsAndMembers();
	}

	@Test
	public void testGetMembers() {
		TeamMember tm = createTeamMember("101", false);
		List<TeamMember> tms = Collections.singletonList(tm);
		when(mockTeamDAO.getMembersInRange(TEAM_ID, null, null,10, 0)).thenReturn(tms);
		doNothing().when(mockTeamDAO).validateTeamExists(TEAM_ID);
		// Call under test
		PaginatedResults<TeamMember> pg = teamManagerImpl.listMembers(TEAM_ID, TeamMemberTypeFilterOptions.ALL, 10, 0);
		assertEquals(tms, pg.getResults());
		assertEquals(1L, pg.getTotalNumberOfResults());
		ListWrapper<TeamMember> lw = ListWrapper.wrap(tms, TeamMember.class);
		Long teamId = Long.parseLong(TEAM_ID);
		when(mockTeamDAO.listMembers(Collections.singletonList(teamId), Collections.singletonList(101L))).thenReturn(lw);
		assertEquals(tms, teamManagerImpl.listMembers(Collections.singletonList(teamId), Collections.singletonList(101L)).getList());
		verify(mockTeamDAO, times(1)).listMembers(Collections.singletonList(teamId), Collections.singletonList(101L));
		verify(mockTeamDAO).validateTeamExists(TEAM_ID);
	}

	@Test
	public void testListMembersWithFragment() {
		TeamMember tm = createTeamMember("101", false);
		List<TeamMember> tms = Collections.singletonList(tm);
		String prefix = "pfx";

		ListWrapper<TeamMember> lw = ListWrapper.wrap(tms, TeamMember.class);

		when(mockPrincipalPrefixDao.listTeamMembersForPrefix(prefix, Long.parseLong(TEAM_ID), 10L, 0L))
				.thenReturn(Arrays.asList(101L));
		when(mockTeamDAO.listMembers(Collections.singletonList(Long.parseLong(TEAM_ID)), Collections.singletonList(101L)))
				.thenReturn(lw);
		doNothing().when(mockTeamDAO).validateTeamExists(TEAM_ID);
		// Call under test
		PaginatedResults<TeamMember> pg = teamManagerImpl.listMembersForPrefix(prefix, TEAM_ID, TeamMemberTypeFilterOptions.ALL, 10, 0);
		assertEquals(tms, pg.getResults());
		assertEquals(1L, pg.getTotalNumberOfResults());
		verify(mockPrincipalPrefixDao, times(1)).listTeamMembersForPrefix(prefix, Long.parseLong(TEAM_ID), 10L, 0L);
		verify(mockTeamDAO, never()).getAdminTeamMemberIds(anyString());
		verify(mockTeamDAO).validateTeamExists(TEAM_ID);
	}

	@Test
	public void testListAdminMembersWithPrefix() {
		Long adminMemberId = 101L;
		TeamMember adminMember = createTeamMember("101", true);
		List<String> adminIds = new ArrayList<>();
		adminIds.add("101");
		Set<Long> adminIdsSet = Collections.singleton(101L);
		String prefix = "pfx";

		when(mockTeamDAO.getAdminTeamMemberIds(TEAM_ID)).thenReturn(adminIds);
		when(mockPrincipalPrefixDao.listCertainTeamMembersForPrefix(prefix, Long.parseLong(TEAM_ID), adminIdsSet, null,10L, 0L)).thenReturn(Collections.singletonList(adminMemberId));
		when(mockTeamDAO.listMembers(Collections.singletonList(Long.parseLong(TEAM_ID)), Collections.singletonList(adminMemberId))).thenReturn(ListWrapper.wrap(Collections.singletonList(adminMember), TeamMember.class));
		doNothing().when(mockTeamDAO).validateTeamExists(TEAM_ID);
		// Call under test
		List<TeamMember> actual = teamManagerImpl.listMembersForPrefix(prefix, TEAM_ID, TeamMemberTypeFilterOptions.ADMIN, 10L, 0L).getResults();
		verify(mockPrincipalPrefixDao, times(1)).listCertainTeamMembersForPrefix(prefix, Long.parseLong(TEAM_ID), adminIdsSet, null,10L, 0L);
		verify(mockTeamDAO, times(1)).listMembers(Collections.singletonList(Long.parseLong(TEAM_ID)), Collections.singletonList(adminMemberId));
		assertEquals(Collections.singletonList(adminMember), actual);
		verify(mockTeamDAO, times(1)).getAdminTeamMemberIds(TEAM_ID); // Once for each invocation
		verify(mockTeamDAO).validateTeamExists(TEAM_ID);
	}

	@Test
	public void testListNonAdminMembersWithPrefix() {
		Long nonAdminMemberId = 102L;
		TeamMember nonAdminMember = createTeamMember("102", false);
		List<String> adminIds = new ArrayList<>();
		adminIds.add("101");
		Set<Long> adminIdsSet = Collections.singleton(101L);
		String prefix = "pfx";

		when(mockTeamDAO.getAdminTeamMemberIds(TEAM_ID)).thenReturn(adminIds);
		when(mockPrincipalPrefixDao.listCertainTeamMembersForPrefix(prefix, Long.parseLong(TEAM_ID), null, adminIdsSet,10L, 0L)).thenReturn(Collections.singletonList(nonAdminMemberId));
		when(mockTeamDAO.listMembers(Collections.singletonList(Long.parseLong(TEAM_ID)), Collections.singletonList(nonAdminMemberId))).thenReturn(ListWrapper.wrap(Collections.singletonList(nonAdminMember), TeamMember.class));
		doNothing().when(mockTeamDAO).validateTeamExists(TEAM_ID);
		// Call under test
		List<TeamMember> actual = teamManagerImpl.listMembersForPrefix(prefix, TEAM_ID, TeamMemberTypeFilterOptions.MEMBER, 10L, 0L).getResults();
		verify(mockPrincipalPrefixDao, times(1)).listCertainTeamMembersForPrefix(prefix, Long.parseLong(TEAM_ID), null, adminIdsSet,10L, 0L);
		verify(mockTeamDAO, times(1)).listMembers(Collections.singletonList(Long.parseLong(TEAM_ID)), Collections.singletonList(nonAdminMemberId));
		assertEquals(Collections.singletonList(nonAdminMember), actual);
		verify(mockTeamDAO).validateTeamExists(TEAM_ID);
	}

	@Test
	public void testListAllMembersWithPrefix() {
		Long adminMemberId = 101L;
		TeamMember adminMember = createTeamMember("101", false);
		Long nonAdminMemberId = 102L;
		TeamMember nonAdminMember = createTeamMember("102", false);
		List<String> adminIds = new ArrayList<>();
		adminIds.add("101");
		String prefix = "pfx";

		when(mockPrincipalPrefixDao.listTeamMembersForPrefix(prefix, Long.parseLong(TEAM_ID),10L, 0L)).thenReturn(Arrays.asList(adminMemberId, nonAdminMemberId));		
		when(mockTeamDAO.listMembers(Collections.singletonList(Long.parseLong(TEAM_ID)), Arrays.asList(adminMemberId, nonAdminMemberId))).thenReturn(ListWrapper.wrap(Arrays.asList(adminMember, nonAdminMember), TeamMember.class));
		doNothing().when(mockTeamDAO).validateTeamExists(TEAM_ID);
		// Call under test
		List<TeamMember> actual = teamManagerImpl.listMembersForPrefix(prefix, TEAM_ID, TeamMemberTypeFilterOptions.ALL, 10L, 0L).getResults();
		verify(mockPrincipalPrefixDao, times(1)).listTeamMembersForPrefix(prefix, Long.parseLong(TEAM_ID), 10L, 0L);
		verify(mockTeamDAO, times(1)).listMembers(Collections.singletonList(Long.parseLong(TEAM_ID)), Arrays.asList(adminMemberId, nonAdminMemberId));
		assertEquals(Arrays.asList(adminMember, nonAdminMember), actual);
		verify(mockTeamDAO).validateTeamExists(TEAM_ID);
	}

	@Test
	public void testListMembersWithPrefixInvalidTeamNotFound() {
		String invalidId = "100";
		String prefix = "pfx";
		String expectedMessage = "Team does not exist for teamId: " + invalidId;
		NotFoundException ex = new NotFoundException(expectedMessage);
		doThrow(ex).when(mockTeamDAO).validateTeamExists(invalidId);
		NotFoundException exception = assertThrows(NotFoundException.class, () -> {
			// Call under test
			teamManagerImpl.listMembersForPrefix(prefix, invalidId ,any(), 10L, 0L);
		});
		verify(mockTeamDAO).validateTeamExists(invalidId);
		assertEquals(ex, exception);
		assertEquals(expectedMessage, exception.getMessage());
	}

	@Test
	public void testListMembersInvalidTeamNotFound() {
		String invalidId = "100";
		String expectedMessage = "Team does not exist for teamId: " + invalidId;
		NotFoundException ex = new NotFoundException(expectedMessage);
		doThrow(ex).when(mockTeamDAO).validateTeamExists(invalidId);
		NotFoundException exception = assertThrows(NotFoundException.class, () -> {
			// Call under test
			teamManagerImpl.listMembers(invalidId, any(), 10L, 0L);
		});
		verify(mockTeamDAO).validateTeamExists(invalidId);
		assertEquals(ex, exception);
		assertEquals(expectedMessage, exception.getMessage());
	}

	@Test
	public void testNoAdmins() {
		when(mockTeamDAO.getAdminTeamMemberIds(TEAM_ID)).thenReturn(Collections.EMPTY_LIST);
		doNothing().when(mockTeamDAO).validateTeamExists(TEAM_ID);
		// Call under test
		List<TeamMember> actual = teamManagerImpl.listMembers(TEAM_ID, TeamMemberTypeFilterOptions.ADMIN, 10L, 0L).getResults();
		verify(mockTeamDAO).validateTeamExists(TEAM_ID);
		assertTrue(actual.isEmpty());
	}

	@Test
	public void testListAdminMembers() {
		TeamMember adminMember = createTeamMember("101", true);
		List<String> adminIds = new ArrayList<>();
		adminIds.add("101");
		Set<Long> adminIdsSet = Collections.singleton(101L);

		when(mockTeamDAO.getAdminTeamMemberIds(TEAM_ID)).thenReturn(adminIds);
		when(mockTeamDAO.getMembersInRange(TEAM_ID, adminIdsSet, null,10L, 0L)).thenReturn(Collections.singletonList(adminMember));
		doNothing().when(mockTeamDAO).validateTeamExists(TEAM_ID);
		// Call under test
		List<TeamMember> actual = teamManagerImpl.listMembers(TEAM_ID, TeamMemberTypeFilterOptions.ADMIN, 10L, 0L).getResults();
		verify(mockTeamDAO, times(1)).getMembersInRange(TEAM_ID, adminIdsSet, null,10L, 0L);
		assertEquals(Collections.singletonList(adminMember), actual);
		verify(mockTeamDAO, times(1)).getAdminTeamMemberIds(TEAM_ID); // Once for each invocation
		verify(mockTeamDAO).validateTeamExists(TEAM_ID);
	}

	@Test
	public void testListNonAdminMembers() {
		TeamMember nonAdminMember = createTeamMember("102", false);
		List<String> adminIds = new ArrayList<>();
		adminIds.add("101");
		Set<Long> adminIdsSet = Collections.singleton(101L);

		when(mockTeamDAO.getAdminTeamMemberIds(TEAM_ID)).thenReturn(adminIds);
		when(mockTeamDAO.getMembersInRange(TEAM_ID, null, adminIdsSet,10L, 0L)).thenReturn(Collections.singletonList(nonAdminMember));
		doNothing().when(mockTeamDAO).validateTeamExists(TEAM_ID);
		// Call under test
		List<TeamMember> actual = teamManagerImpl.listMembers(TEAM_ID, TeamMemberTypeFilterOptions.MEMBER, 10L, 0L).getResults();
		verify(mockTeamDAO, times(1)).getMembersInRange(TEAM_ID, null, adminIdsSet,10L, 0L);
		assertEquals(Collections.singletonList(nonAdminMember), actual);
		verify(mockTeamDAO, times(1)).getAdminTeamMemberIds(TEAM_ID); // Once for each invocation
		verify(mockTeamDAO).validateTeamExists(TEAM_ID);
	}

	@Test
	public void testListAllTypesOfMembers() {
		TeamMember adminMember = createTeamMember("101", true);
		TeamMember nonAdminMember = createTeamMember("102", false);
		List<String> adminIds = new ArrayList<>();
		adminIds.add("101");

		when(mockTeamDAO.getAdminTeamMemberIds(TEAM_ID)).thenReturn(adminIds);
		when(mockTeamDAO.getMembersInRange(TEAM_ID, null, null,10L, 0L)).thenReturn(Arrays.asList(adminMember, nonAdminMember));
		doNothing().when(mockTeamDAO).validateTeamExists(TEAM_ID);
		// Call under test
		List<TeamMember> actual = teamManagerImpl.listMembers(TEAM_ID, TeamMemberTypeFilterOptions.ALL, 10L, 0L).getResults();
		verify(mockTeamDAO, times(1)).getMembersInRange(TEAM_ID, null, null,10L, 0L);
		assertEquals(Arrays.asList(adminMember, nonAdminMember), actual);
		verify(mockTeamDAO, times(1)).getAdminTeamMemberIds(TEAM_ID); // Once for each invocation
		verify(mockTeamDAO).validateTeamExists(TEAM_ID);
	}

	@Test
	public void testSetPermissions() {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		AccessControlList acl = TeamManagerImpl.createInitialAcl(userInfo, TEAM_ID, new Date());
		when(mockAclDAO.get(TEAM_ID, ObjectType.TEAM)).thenReturn(acl);
		String principalId = "321";
		teamManagerImpl.setPermissions(userInfo, TEAM_ID, principalId, true);
		verify(mockAclDAO).update((AccessControlList)any(), eq(ObjectType.TEAM));
		// now check that user is actually an admin
		boolean foundRA=false;
		for (ResourceAccess ra: acl.getResourceAccess()) {
			if (principalId.equals(ra.getPrincipalId().toString())) {
				foundRA=true;
				for (ACCESS_TYPE at : ModelConstants.TEAM_ADMIN_PERMISSIONS) {
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
	
	@Test
	public void testSetRemoveOwnPermissions() {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.authorized());
		AccessControlList acl = TeamManagerImpl.createInitialAcl(userInfo, TEAM_ID, new Date());
		when(mockAclDAO.get(TEAM_ID, ObjectType.TEAM)).thenReturn(acl);
		String principalId = MEMBER_PRINCIPAL_ID; // add SELF as admin
		
		teamManagerImpl.setPermissions(userInfo, TEAM_ID, principalId, true);
		
		Assertions.assertThrows(InvalidModelException.class, ()-> {
			// now try to remove own admin permissions
			teamManagerImpl.setPermissions(userInfo, TEAM_ID, principalId, false);
		});
	}
	
	@Test
	public void testSetPermissionsUnauthorized()  {
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.UPDATE)).thenReturn(AuthorizationStatus.accessDenied(""));
		String principalId = MEMBER_PRINCIPAL_ID;
		Assertions.assertThrows(UnauthorizedException.class, ()-> {
			teamManagerImpl.setPermissions(userInfo, TEAM_ID, principalId, true);
		});
	}
	
	@Test
	public void testIsMembershipApprovalRequired() {
		// admin doesn't require approval
		assertFalse(teamManagerImpl.isMembershipApprovalRequired(adminInfo, TEAM_ID));

		// let the team be a non-Open team (which it is by default)
		Team team = createTeam(TEAM_ID, "name", "description", null, "101", null, null, null, null);
		when(mockTeamDAO.get(TEAM_ID)).thenReturn(team);
			
		// a team-admin doesn't require approval
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationStatus.authorized());
		assertFalse(teamManagerImpl.isMembershipApprovalRequired(userInfo, TEAM_ID));

		// a non-team-admin requires approval
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationStatus.accessDenied(""));
		assertTrue(teamManagerImpl.isMembershipApprovalRequired(userInfo, TEAM_ID));
		
		// unless it's an open team
		team.setCanPublicJoin(true);
		assertFalse(teamManagerImpl.isMembershipApprovalRequired(userInfo, TEAM_ID));
		verify(mockTeamDAO, times(2)).get(TEAM_ID);
	}
	
	@Test
	public void testGetTeamMembershipStatus() {
		// let the team be a non-Open team (which it is by default)
		Team team = createTeam(TEAM_ID, "name", "description", null, "101", null, null, null, null);
		when(mockTeamDAO.get(TEAM_ID)).thenReturn(team);
		
		String principalId = MEMBER_PRINCIPAL_ID;
		UserInfo principalUserInfo = createUserInfo(false, principalId);
		
		when(mockGroupMembersDAO.areMemberOf(TEAM_ID, Collections.singleton(principalId))).thenReturn(true);
		
		when(mockMembershipInvitationDAO.getOpenByTeamAndUserCount(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong())).thenReturn(1L);
		when(mockMembershipRequestDAO.getOpenByTeamAndRequesterCount(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong())).thenReturn(1L);
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE)).thenReturn(AuthorizationStatus.accessDenied(""));
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.SEND_MESSAGE)).thenReturn(AuthorizationStatus.accessDenied(""));
		when(mockRestrictionInformationManager.
				getRestrictionInformation(principalUserInfo, restrictionInfoRqst)).
					thenReturn(noUnmetAccessRqmtResponse);
		
		TeamMembershipStatus tms = teamManagerImpl.getTeamMembershipStatus(userInfo, TEAM_ID, principalUserInfo);
		assertEquals(TEAM_ID, tms.getTeamId());
		assertEquals(principalId, tms.getUserId());
		assertTrue(tms.getIsMember());
		assertTrue(tms.getHasOpenInvitation());
		assertTrue(tms.getHasOpenRequest());
		assertTrue(tms.getCanJoin());
		assertTrue(tms.getMembershipApprovalRequired());
		assertFalse(tms.getHasUnmetAccessRequirement());
		assertFalse(tms.getCanSendEmail());
		
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.SEND_MESSAGE)).thenReturn(AuthorizationStatus.authorized());
		
		tms = teamManagerImpl.getTeamMembershipStatus(userInfo, TEAM_ID, principalUserInfo);
		assertEquals(TEAM_ID, tms.getTeamId());
		assertEquals(principalId, tms.getUserId());
		assertTrue(tms.getIsMember());
		assertTrue(tms.getHasOpenInvitation());
		assertTrue(tms.getHasOpenRequest());
		assertTrue(tms.getCanJoin());
		assertTrue(tms.getMembershipApprovalRequired());
		assertFalse(tms.getHasUnmetAccessRequirement());
		assertTrue(tms.getCanSendEmail());
		
		when(mockGroupMembersDAO.areMemberOf(TEAM_ID, Collections.singleton(principalId))).thenReturn(false);
		when(mockMembershipInvitationDAO.getOpenByTeamAndUserCount(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong())).thenReturn(0L);
		when(mockMembershipRequestDAO.getOpenByTeamAndRequesterCount(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(MEMBER_PRINCIPAL_ID)), anyLong())).thenReturn(0L);
		tms = teamManagerImpl.getTeamMembershipStatus(userInfo, TEAM_ID, principalUserInfo);
		assertEquals(TEAM_ID, tms.getTeamId());
		assertEquals(principalId, tms.getUserId());
		assertFalse(tms.getIsMember());
		assertFalse(tms.getHasOpenInvitation());
		assertFalse(tms.getHasOpenRequest());
		assertFalse(tms.getCanJoin());
		assertTrue(tms.getMembershipApprovalRequired());
		assertFalse(tms.getHasUnmetAccessRequirement());
		assertTrue(tms.getCanSendEmail());
		
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
		assertTrue(tms.getCanSendEmail());
		
		when(mockRestrictionInformationManager.
				getRestrictionInformation(principalUserInfo, restrictionInfoRqst)).
					thenReturn(hasUnmetAccessRqmtResponse);
		tms = teamManagerImpl.getTeamMembershipStatus(userInfo, TEAM_ID, principalUserInfo);
		assertEquals(TEAM_ID, tms.getTeamId());
		assertEquals(principalId, tms.getUserId());
		assertFalse(tms.getCanJoin());
		assertFalse(tms.getMembershipApprovalRequired());
		assertTrue(tms.getHasUnmetAccessRequirement());
		assertTrue(tms.getCanSendEmail());
	}
	
	@Test
	public void testCreateJoinedTeamNotificationSelf() {
		when(mockUserProfileManager.getUserProfile(userInfo.getId().toString())).thenReturn(up);

		Team team = new Team();
		team.setName("test-name");
		when(mockTeamDAO.get(TEAM_ID)).thenReturn(team);

		List<String> inviterPrincipalIds = Arrays.asList(new String[]{"987", "654"});
		List<MembershipInvitation> miss = new ArrayList<>();
		for (String inviterPrincipalId : inviterPrincipalIds) {
			MembershipInvitation mis = new MembershipInvitation();
			mis.setCreatedBy(inviterPrincipalId);
			miss.add(mis);
		}
		String teamEndpoint = "https://synapse.org/#Team:";
		String notificationUnsubscribeEndpoint = "https://synapse.org/#notificationUnsubscribeEndpoint:";
		
		when(mockMembershipInvitationDAO.getInvitersByTeamAndUser(eq(Long.parseLong(TEAM_ID)), eq(Long.parseLong(MEMBER_PRINCIPAL_ID)),
				anyLong())).
			thenReturn(inviterPrincipalIds);

		List<MessageToUserAndBody> resultList = 
				teamManagerImpl.createJoinedTeamNotifications(userInfo, userInfo, TEAM_ID, 
						teamEndpoint, notificationUnsubscribeEndpoint);
		assertEquals(inviterPrincipalIds.size(), resultList.size());
		for (int i=0; i<inviterPrincipalIds.size(); i++) {
			MessageToUserAndBody result = resultList.get(i);
			assertEquals("New Member Has Joined the Team", result.getMetadata().getSubject());
			assertEquals(Collections.singleton(inviterPrincipalIds.get(i)), result.getMetadata().getRecipients());
			UserProfile userProfile = mockUserProfileManager.getUserProfile(userInfo.getId().toString());
			String userId = MEMBER_PRINCIPAL_ID;
			String displayName = EmailUtils.getDisplayNameWithUsername(userProfile);
			String teamWebLink = teamEndpoint + TEAM_ID;
			String teamName = "test-name";
			String expected = "<html style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;font-size: 10px;-webkit-tap-highlight-color: rgba(0, 0, 0, 0);\">\r\n" + 
					"  <body style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;font-family: &quot;Helvetica Neue&quot;, Helvetica, Arial, sans-serif;font-size: 14px;line-height: 1.42857143;color: #333333;background-color: #ffffff;\">\r\n" + 
					"    <div style=\"margin: 10px;-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;\">\r\n" + 
					"      <p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;margin-bottom: 20px;font-size: 16px;font-weight: 300;line-height: 1.4;\">Hello,</p>\r\n" + 
					"      <p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\">\r\n" + 
					"        <strong style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;font-weight: bold;\"><a href=\"https://www.synapse.org/#!Profile:" + userId + "\">" + displayName + "</a></strong>\r\n" + 
					"        has joined team\r\n" + 
					"        <strong style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;font-weight: bold;\"><a href=\"" + teamWebLink + "\">" + teamName + "</a></strong>.\r\n" + 
					"        </p>\r\n" + 
					"      <p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\">For further information, visit the <a href=\"" + teamWebLink + "\" style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;background-color: transparent;color: #337ab7;text-decoration: none;\">team page</a>.</p>\r\n" + 
					"      <br style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;\">\r\n" + 
					"      <p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\">\r\n" + 
					"        Sincerely,\r\n" + 
					"      </p>\r\n" + 
					"      <p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\">\r\n" + 
					"        <img src=\"https://s3.amazonaws.com/static.synapse.org/images/SynapseLogo2.png\" style=\"display: inline;width: 40px;height: 40px;-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;border: 0;vertical-align: middle;\"> Synapse Administration\r\n" + 
					"      </p>\r\n" + 
					"    </div>\r\n" + 
					"  </body>\r\n" + 
					"</html>\r\n";
			assertEquals(expected, result.getBody());
		}
		verify(mockTeamDAO).get(TEAM_ID);
	}
	
	@Test
	public void testCreateJoinedTeamNotificationOther() {

		when(mockUserProfileManager.getUserProfile(userInfo.getId().toString())).thenReturn(up);
		
		Team team = new Team();
		team.setName("test-name");
		when(mockTeamDAO.get(TEAM_ID)).thenReturn(team);

		String otherPrincipalId = "987";
		String teamEndpoint = "https://synapse.org/#Team:";
		String notificationUnsubscribeEndpoint = "https://synapse.org/#notificationUnsubscribeEndpoint:";
		UserInfo otherUserInfo = createUserInfo(false, otherPrincipalId);
		List<MessageToUserAndBody> resultList = 
				teamManagerImpl.createJoinedTeamNotifications(userInfo, 
						otherUserInfo, TEAM_ID, teamEndpoint,
						notificationUnsubscribeEndpoint);
		assertEquals(1, resultList.size());
		MessageToUserAndBody result = resultList.get(0);
		assertEquals("New Member Has Joined the Team", result.getMetadata().getSubject());
		assertEquals(Collections.singleton(otherPrincipalId), result.getMetadata().getRecipients());
		UserProfile userProfile = mockUserProfileManager.getUserProfile(userInfo.getId().toString());
		String userId = MEMBER_PRINCIPAL_ID;
		String displayName = EmailUtils.getDisplayNameWithUsername(userProfile);
		String teamWebLink = teamEndpoint + TEAM_ID;
		String teamName = "test-name";
		String expected = "<html style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;font-size: 10px;-webkit-tap-highlight-color: rgba(0, 0, 0, 0);\">\r\n" + 
				"  <body style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;font-family: &quot;Helvetica Neue&quot;, Helvetica, Arial, sans-serif;font-size: 14px;line-height: 1.42857143;color: #333333;background-color: #ffffff;\">\r\n" + 
				"    <div style=\"margin: 10px;-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;\">\r\n" + 
				"      <p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;margin-bottom: 20px;font-size: 16px;font-weight: 300;line-height: 1.4;\">Hello,</p>\r\n" + 
				"      <p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\">\r\n" + 
				"        <strong style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;font-weight: bold;\"><a href=\"https://www.synapse.org/#!Profile:" + userId + "\">" + displayName + "</a></strong>\r\n" + 
				"        has accepted you into team\r\n" + 
				"        <strong style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;font-weight: bold;\"><a href=\"" + teamWebLink + "\">" + teamName + "</a></strong>.\r\n" + 
				"      </p>\r\n" + 
				"      <p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\">For further information, visit the <a href=\"" + teamWebLink + "\" style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;background-color: transparent;color: #337ab7;text-decoration: none;\">team page</a>.</p>\r\n" + 
				"      <br style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;\">\r\n" + 
				"      <p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\">\r\n" + 
				"        Sincerely,\r\n" + 
				"      </p>\r\n" + 
				"      <p style=\"-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;margin: 0 0 10px;\">\r\n" + 
				"        <img src=\"https://s3.amazonaws.com/static.synapse.org/images/SynapseLogo2.png\" style=\"display: inline;width: 40px;height: 40px;-webkit-box-sizing: border-box;-moz-box-sizing: border-box;box-sizing: border-box;border: 0;vertical-align: middle;\"> Synapse Administration\r\n" + 
				"      </p>\r\n" + 
				"    </div>\r\n" + 
				"  </body>\r\n" + 
				"</html>\r\n";
		assertEquals(expected, result.getBody());
		verify(mockTeamDAO).get(TEAM_ID);
	}
	
	@Test
	public void testCreateJoinedTeamNotificationWithNullOptionalParameters() {
		String teamEndpoint = "https://synapse.org/#Team:";
		String notificationUnsubscribeEndpoint = "https://synapse.org/#notificationUnsubscribeEndpoint:";
		List<MessageToUserAndBody> resultList = teamManagerImpl.createJoinedTeamNotifications(userInfo, userInfo, TEAM_ID, null, null);
		assertTrue(resultList.size() == 0);
		resultList = teamManagerImpl.createJoinedTeamNotifications(userInfo, userInfo, TEAM_ID, teamEndpoint, null);
		assertTrue(resultList.size() == 0);
		resultList = teamManagerImpl.createJoinedTeamNotifications(userInfo, userInfo, TEAM_ID, null, notificationUnsubscribeEndpoint);
		assertTrue(resultList.size() == 0);
	}
	
	@Test
	public void testCountMembers() {
		Count expected = new Count();
		expected.setCount(42L);
		when(mockTeamDAO.getMembersCount(TEAM_ID)).thenReturn(expected.getCount());
		assertEquals(expected, teamManagerImpl.countMembers(TEAM_ID));
		verify(mockTeamDAO).getMembersCount(TEAM_ID);
	}

	@Test
	public void testAttemptTeamDeleteWithUserGroupThatCannotBeDeleted() {
		Team retrievedTeam = new Team();
		retrievedTeam.setName("Some name");
		retrievedTeam.setId(TEAM_ID);
		when(mockTeamDAO.get(TEAM_ID)).thenReturn(retrievedTeam);
		doThrow(new DataIntegrityViolationException("")).when(mockUserGroupDAO).delete(TEAM_ID);
		when(mockAuthorizationManager.canAccess(userInfo, TEAM_ID, ObjectType.TEAM, ACCESS_TYPE.DELETE)).thenReturn(AuthorizationStatus.authorized());
		// call under test
		try {
			teamManagerImpl.delete(userInfo, TEAM_ID);
			fail("Expected exception");
		} catch (IllegalArgumentException e) {
			// Verify that the illegal argument exception was the cause (Occurred in the userGroupDao)
			assertTrue(e.getCause() instanceof DataIntegrityViolationException);
		}
		verify(mockTeamDAO).delete(TEAM_ID);
		verify(mockAclDAO).delete(TEAM_ID, ObjectType.TEAM);
		verify(mockUserGroupDAO).delete(TEAM_ID);
		verify(mockTeamDAO).get(TEAM_ID);

	}

	private TeamMember createTeamMember(String ownerId, boolean isAdmin) {
		TeamMember tm = new TeamMember();
		tm.setTeamId(TEAM_ID);
		UserGroupHeader ugh = new UserGroupHeader();
		ugh.setOwnerId(ownerId);
		tm.setMember(ugh);
		tm.setIsAdmin(isAdmin);
		return tm;
	}

}
