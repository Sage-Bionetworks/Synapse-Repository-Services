package org.sagebionetworks.repo.manager.team;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;

public class TeamManagerImplTest {
	private TeamManagerImpl teamManagerImpl = null;
	private AuthorizationManager mockAuthorizationManager = null;
	private TeamDAO mockTeamDAO = null;
	private GroupMembersDAO mockGroupMembersDAO = null;
	private UserGroupDAO mockUserGroupDAO = null;
	private UserManager mockUserManager = null;
	private AccessControlListDAO mockAclDAO = null;
	private FileHandleManager mockFileHandleManager = null;
	private MembershipInvitationManager mockMembershipInvitationManager = null;
	private MembershipRequestManager mockMembershipRequestManager = null;
	
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
		mockUserManager = Mockito.mock(UserManager.class);
		mockFileHandleManager = Mockito.mock(FileHandleManager.class);
		mockAclDAO = Mockito.mock(AccessControlListDAO.class);
		mockMembershipInvitationManager = Mockito.mock(MembershipInvitationManager.class);
		mockMembershipRequestManager = Mockito.mock(MembershipRequestManager.class);
		teamManagerImpl = new TeamManagerImpl(
				mockAuthorizationManager,
				mockTeamDAO,
				mockGroupMembersDAO,
				mockUserGroupDAO,
				mockUserManager,
				mockAclDAO,
				mockFileHandleManager,
				mockMembershipInvitationManager,
				mockMembershipRequestManager);
		userInfo = new UserInfo(false);
		UserGroup individualGroup = new UserGroup();
		individualGroup.setId(MEMBER_PRINCIPAL_ID);
		userInfo.setIndividualGroup(individualGroup);
		adminInfo = new UserInfo(true);
		adminInfo.setIndividualGroup(individualGroup);
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
		AccessControlList acl = TeamManagerImpl.createAdminAcl(userInfo, TEAM_ID, now);
		assertEquals(MEMBER_PRINCIPAL_ID, acl.getCreatedBy());
		assertEquals(now, acl.getCreationDate());
		assertEquals(TEAM_ID, acl.getId());
		assertEquals(MEMBER_PRINCIPAL_ID, acl.getModifiedBy());
		assertEquals(now, acl.getModifiedOn());
		assertEquals(1, acl.getResourceAccess().size());
		ResourceAccess ra = acl.getResourceAccess().iterator().next();
		assertEquals(new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[]{
				ACCESS_TYPE.READ, 
				ACCESS_TYPE.UPDATE, 
				ACCESS_TYPE.DELETE, 
				ACCESS_TYPE.MEMBERSHIP, 
				ACCESS_TYPE.SEND_MESSAGE})), ra.getAccessType());
		assertEquals((Long)Long.parseLong(MEMBER_PRINCIPAL_ID), ra.getPrincipalId());
	}
	
	@Test
	public void testAddToAcl() throws Exception {
		AccessControlList acl = new AccessControlList();
		Set<ResourceAccess> ras = new HashSet<ResourceAccess>();
		acl.setResourceAccess(ras);
		TeamManagerImpl.addToACL(acl, MEMBER_PRINCIPAL_ID, new ACCESS_TYPE[] {ACCESS_TYPE.MEMBERSHIP});
		assertEquals(1, acl.getResourceAccess().size());
		ResourceAccess ra = acl.getResourceAccess().iterator().next();
		assertEquals(new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[]{ACCESS_TYPE.MEMBERSHIP})), ra.getAccessType());
		assertEquals((Long)Long.parseLong(MEMBER_PRINCIPAL_ID), ra.getPrincipalId());
	}
	
	@Test
	public void testCreate() throws Exception {
		Team team = createTeam(null, "name", "description", null, "101", null, null, null, null);
		when(mockTeamDAO.create(team)).thenReturn(team);
		// mock userManager
		when(mockUserManager.createPrincipal("name", false)).thenReturn(TEAM_ID);
		Team created = teamManagerImpl.create(userInfo,team);
		assertEquals(team, created);
		// verify that group, acl were created
		assertEquals(TEAM_ID, created.getId());
		verify(mockAclDAO).create((AccessControlList)any());
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
		when(mockTeamDAO.getInRange(0, 10)).thenReturn(teamList);
		when(mockTeamDAO.getCount()).thenReturn(1L);
		QueryResults<Team> result = teamManagerImpl.get(0,10);
		assertEquals(teamList, result.getResults());
		assertEquals(1L, result.getTotalNumberOfResults());
	}
	
	@Test
	public void testGetByMember() throws Exception {
		// TODO
	}
	
	@Test
	public void testPut() throws Exception {
		// TODO
	}
	
	@Test
	public void testDelete() throws Exception {
		// TODO
	}
	
	@Test
	public void testCanAddTeamMember() throws Exception {
		// TODO
	}
	
	@Test
	public void testUserGroupsHasPrincipalId() throws Exception {
		// TODO
	}
	
	@Test
	public void testAddMember() throws Exception {
		// TODO
	}
	
	@Test
	public void testCanRemoveTeamMember() throws Exception {
		// TODO
	}
	
	@Test
	public void testRemoveMember() throws Exception {
		// TODO
	}
	
	@Test
	public void testGetACL() throws Exception {
		// TODO
	}
	
	@Test
	public void testUpdateACL() throws Exception {
		// TODO
	}
	
	@Test
	public void testGetIconURL() throws Exception {
		// TODO
	}
	
	@Test
	public void testGetAllTeamsAndMembers() throws Exception {
		// TODO
	}
	
			
	
	
	

}
