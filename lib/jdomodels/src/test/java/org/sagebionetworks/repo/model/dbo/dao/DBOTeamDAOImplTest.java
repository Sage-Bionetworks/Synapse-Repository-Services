package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.ListWrapper;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.TeamSortOrder;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOTeamDAOImplTest {
	
	@Autowired
	private TeamDAO teamDAO;

	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private GroupMembersDAO groupMembersDAO;
	
	@Autowired
	private UserProfileDAO userProfileDAO;
	
	@Autowired
	private PrincipalAliasDAO principalAliasDAO;
	
	@Autowired
	private AccessControlListDAO aclDAO;
	
	@Autowired
	private FileHandleDao fileHanldeDAO;
	
	@Autowired
	private IdGenerator idGenerator;
	
	private List<String> teamsToDelete;
	private String aclToDelete;
	private List<String> usersToDelete;
	
	@BeforeEach
	public void setup() {
		List<Team> teams = teamDAO.getInRange(1000, 0);
		for (Team team : teams) {
			teamDAO.delete(team.getId());
		}
		teamsToDelete = new ArrayList<>();
		usersToDelete = new ArrayList<>();
		
		fileHanldeDAO.truncateTable();
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (aclToDelete!=null) {
			aclDAO.delete(aclToDelete, ObjectType.TEAM);
		}
		
		for (String teamId : teamsToDelete) {
			teamDAO.delete(teamId);
			userGroupDAO.delete(teamId);
		}

		for (String userId : usersToDelete) {
			userGroupDAO.delete(userId);
		}
		
		fileHanldeDAO.truncateTable();
	}

	private static UserGroupHeader createUserGroupHeaderFromUserProfile(UserProfile up, String userName) {
		UserGroupHeader ugh = new UserGroupHeader();
		ugh.setOwnerId(up.getOwnerId());
		ugh.setUserName(userName);
		ugh.setIsIndividual(true);
		ugh.setFirstName(up.getFirstName());
		ugh.setLastName(up.getLastName());
		return ugh;
	}

	@Test
	public void testListTeams() {
		List<Team> createdTeams = new ArrayList<>();
		for (int i=0; i<3; i++) {
			createdTeams.add(createTeam("Super Team_"+i));
		}
		assertEquals(Arrays.asList(createdTeams.get(1), createdTeams.get(0)),
				
		teamDAO.list(Arrays.asList(Long.parseLong(teamsToDelete.get(1)), Long.parseLong(teamsToDelete.get(0)))).getList());
		
		assertThrows(NotFoundException.class, () -> {
			// Call under test
			teamDAO.list(Arrays.asList(Long.parseLong(teamsToDelete.get(1)), 98776654L+Long.parseLong(teamsToDelete.get(0))));
		});
	}

	@Test
	public void testCreateTeam() {
		UserGroup group = new UserGroup();
		group.setIsIndividual(false);
		group.setId(userGroupDAO.create(group).toString());
		teamsToDelete.add(group.getId());

		Team team = new Team();
		Long id = Long.parseLong(group.getId());
		team.setId(""+id);
		team.setName("Test Create Team Team");
		team.setDescription("This is a Team designated for testing.");
		team.setCreatedOn(new Date());
		team.setCreatedBy("101");
		team.setModifiedOn(new Date());
		team.setModifiedBy("102");
		Team createdTeam = teamDAO.create(team);
		assertNotNull(createdTeam.getEtag());
		team.setEtag(createdTeam.getEtag()); // Fill in the missing eTag on the object we created
		assertEquals(team, createdTeam);

		// Test all of the methods that retrieve teams
		assertEquals(1, teamDAO.getInRange(1, 0).size());
		assertEquals(0, teamDAO.getInRange(2, 1).size()); // Pagination
		assertEquals(1, teamDAO.getCount());

		// Make sure the team isn't counted as a user in the team
		assertEquals(0, teamDAO.getForMemberInRange(""+id, 1, 0).size());
		assertEquals(0, teamDAO.getCountForMember(""+id));
	}
	
	@Test
	public void testCreateTeamWithIcon() {
		UserGroup creator = createIndividual();
		
		FileHandle fileHandle = fileHanldeDAO.createFile(TestUtils.createS3FileHandle(creator.getId(), idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		
		// Call under test
		Team team = createTeam("Some Team", fileHandle.getId(), creator.getId(), creator.getId());
		
		assertEquals(fileHandle.getId(), team.getIcon());
		
		// Verifies re-reading from the DB
		assertEquals(team, teamDAO.get(team.getId()));
		
	}

	@Test
	public void testUpdateTeam() {
		Team t = createTeam("Test Update Team");
		Team toUpdate = teamDAO.get(t.getId());
		toUpdate.setName("A Brand New Name");
		toUpdate.setDescription("A New Description Too");
		String oldEtag = toUpdate.getEtag();
		Team retrieved = teamDAO.update(toUpdate);
		assertNotEquals(retrieved.getEtag(), oldEtag);
		toUpdate.setEtag(retrieved.getEtag());
		assertEquals(retrieved, toUpdate);
	}
	
	@Test
	public void testUpdateTeamIcon() {
		// Creates a team without an icon
		Team team = createTeam("Some Team");
		
		UserGroup creator = createIndividual();
		
		FileHandle fileHandle = fileHanldeDAO.createFile(TestUtils.createS3FileHandle(creator.getId(), idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		
		team.setIcon(fileHandle.getId());
		
		// Call under test
		team = teamDAO.update(team);
		
		assertEquals(fileHandle.getId(), team.getIcon());
		
		// Verifies re-reading from the DB
		assertEquals(team, teamDAO.get(team.getId()));
	}
	
	@Test
	public void testRemoveTeamIcon() {
		UserGroup creator = createIndividual();
		
		FileHandle fileHandle = fileHanldeDAO.createFile(TestUtils.createS3FileHandle(creator.getId(), idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		
		// Creates a team with an icon
		Team team = createTeam("Some Team", fileHandle.getId(), creator.getId(), creator.getId());
		
		team.setIcon(null);
		
		// Call under test
		team = teamDAO.update(team);
		
		assertNull(team.getIcon());
		
		// Verifies re-reading from the DB
		assertEquals(team, teamDAO.get(team.getId()));
	}

	@Test
	public void testDeleteTeam() {
		Team t = createTeam("Test Delete Team");
		teamDAO.delete(t.getId());
		
		assertThrows(NotFoundException.class, () -> {
			// Call under test
			teamDAO.get(t.getId());
		});
	}

	@Test
	public void testList() {
		Team t = createTeam("team name");
		assertEquals(t, teamDAO.list(Collections.singletonList(Long.valueOf(t.getId()))).getList().get(0));
		assertEquals(1, teamDAO.list(Collections.singletonList(Long.valueOf(t.getId()))).getList().size());
		assertTrue(teamDAO.list(Collections.emptyList()).getList().isEmpty());
	}

	@Test
	public void testListMembers() {
		Team team = createTeam("Team for testing listMembers");
		UserGroup user = createIndividual();

		groupMembersDAO.addMembers(team.getId(), Collections.singletonList(user.getId()));

		// Call under test, getting team members data
		ListWrapper<TeamMember> listedMembers =
				teamDAO.listMembers(Collections.singletonList(Long.parseLong(team.getId())),
						Arrays.asList(Long.parseLong(user.getId()), Long.parseLong(user.getId())));
		assertEquals(2, listedMembers.getList().size());
		TeamMember member = teamDAO.getMember(team.getId(), user.getId());
		assertEquals(Arrays.asList(member, member), listedMembers.getList());

		// check that nothing is returned for other team IDs and principal IDs
		assertThrows(NotFoundException.class, () -> {
			// Call under test
			teamDAO.listMembers(Collections.singletonList(0L), Collections.singletonList(Long.parseLong(user.getId())));
		});
		
		assertThrows(NotFoundException.class, () -> {
			// Call under test
			teamDAO.listMembers(Collections.singletonList(Long.parseLong(team.getId())), Collections.singletonList(0L));
		});
		
		assertTrue(teamDAO.listMembers(Collections.singletonList(Long.parseLong(team.getId())), Collections.emptyList()).getList().isEmpty());
	}

	@Test
	public void testAddRemoveUser(){
		Team team = createTeam("Test Add Remove User Team");
		UserGroup user = createIndividual();
		bindUserName("user-name", Long.parseLong(user.getId()));
		// This adds the member
		groupMembersDAO.addMembers(team.getId(), Collections.singletonList(user.getId()));

		// Call under test, make sure we can find the user's team
		List<Team> membersTeams = teamDAO.getForMemberInRange(user.getId(), 1, 0);
		assertEquals(1, membersTeams.size());
		assertEquals(team, membersTeams.get(0));

		// Call under test, getting the number of teams
		assertEquals(1, teamDAO.getCountForMember(user.getId()));
	}

	@Test
	public void testMakeUserAdmin(){
		Team team = createTeam("UserIsAdminTestTeam");
		UserGroup user = createIndividual();
		groupMembersDAO.addMembers(team.getId(), Collections.singletonList(user.getId()));
		TeamMember member = teamDAO.getMember(team.getId(), user.getId());

		// Make requests before adding admin
		assertEquals(0L, teamDAO.getAdminMemberCount(team.getId()));
		assertEquals(team.getId(), member.getTeamId());
		assertFalse(member.getIsAdmin());
		UserGroupHeader ugh = member.getMember();
		assertTrue(ugh.getIsIndividual());
		assertEquals(user.getId(), ugh.getOwnerId());

		List<String> teamIds = teamDAO.getAllTeamsUserIsAdmin(user.getId());
		assertNotNull(teamIds);
		assertTrue(teamIds.isEmpty());

		// Add admin
		addUserAsAdmin(Long.parseLong(user.getId()), team.getId());

		// Again
		assertEquals(1L, teamDAO.getAdminMemberCount(team.getId()));
		member = teamDAO.getMember(team.getId(), user.getId());
		assertEquals(team.getId(), member.getTeamId());
		assertTrue(member.getIsAdmin());
		assertEquals(1, teamDAO.getAdminTeamMemberIds(team.getId()).size());
		assertEquals(user.getId(), teamDAO.getAdminTeamMemberIds(team.getId()).get(0));

	}

	@Test
	public void testGetMembersCount() {
		Team team = createTeam("UserIsAdminTestTeam");
		UserGroup user = createIndividual();
		assertEquals(0, teamDAO.getMembersCount(team.getId()));
		groupMembersDAO.addMembers(team.getId(), Collections.singletonList(user.getId()));
		assertEquals(1, teamDAO.getMembersCount(team.getId()));
	}

	@Test
	public void testGetMembersNoFilter() {
		Team team = createTeam("UserIsAdminTestTeam");
		UserGroup user = createIndividual();

		List<TeamMember> tms = teamDAO.getMembersInRange(team.getId(), null, null, 2, 0);
		assertNotNull(tms);
		assertTrue(tms.isEmpty());

		groupMembersDAO.addMembers(team.getId(), Collections.singletonList(user.getId()));
		TeamMember member = teamDAO.getMember(team.getId(), user.getId());

		tms = teamDAO.getMembersInRange(team.getId(), null, null, 2, 0);
		assertEquals(1, tms.size());
		assertEquals(member, tms.get(0));
	}

	@Test
	public void testGetMembersWithFilters(){
		Team team = createTeam("UserIsAdminTestTeam");
		UserGroup user1 = createIndividual();
		UserGroup user2 = createIndividual();
		groupMembersDAO.addMembers(team.getId(), Arrays.asList(user1.getId(), user2.getId()));
		TeamMember member1 = teamDAO.getMember(team.getId(), user1.getId());

		// Exclude user2
		List<TeamMember> tms = teamDAO.getMembersInRange(team.getId(), null, Collections.singleton(Long.parseLong(user2.getId())), 2, 0);
		assertEquals(1, tms.size());
		assertEquals(member1, tms.get(0));

		// Include user1 only
		tms = teamDAO.getMembersInRange(team.getId(), Collections.singleton(Long.parseLong(user1.getId())), null, 2, 0);
		assertEquals(1, tms.size());
		assertEquals(member1, tms.get(0));

		// Include + exclude user1
		tms = teamDAO.getMembersInRange(team.getId(), Collections.singleton(Long.parseLong(user1.getId())), Collections.singleton(Long.parseLong(user1.getId())), 2, 0);
		assertNotNull(tms);
		assertTrue(tms.isEmpty());

	}

	@Test
	public void testGetAllTeamsAndMembers() {
		Team team = createTeam("Get all teams and members test");

		// Call under test, if there are no members in any teams, we should get nothing back
		assertEquals(new HashMap<Team,Collection<TeamMember>>(), teamDAO.getAllTeamsAndMembers());

		UserGroup user = createIndividual();
		groupMembersDAO.addMembers(team.getId(), Collections.singletonList(user.getId()));
		UserProfile up = userProfileDAO.get(user.getId());
		bindUserName(up.getUserName(), Long.parseLong(user.getId()));
		String userName = principalAliasDAO.getUserName(Long.parseLong(user.getId()));

		Map<Team,Collection<TeamMember>> expectedAllTeamsAndMembers = new HashMap<>();
		UserGroupHeader ugh = createUserGroupHeaderFromUserProfile(up, userName);
		TeamMember tm = new TeamMember();
		tm.setIsAdmin(false);
		tm.setMember(ugh);
		tm.setTeamId(team.getId());
		List<TeamMember> tmList = new ArrayList<>();
		tmList.add(tm);
		expectedAllTeamsAndMembers.put(team,  tmList);

		// we have to check 'equals' on the pieces because a global 'assertEquals' fails
		Map<Team,Collection<TeamMember>> actualAllTeamsAndMembers = teamDAO.getAllTeamsAndMembers();
		assertEquals(expectedAllTeamsAndMembers.size(), actualAllTeamsAndMembers.size());
		for (Team t : expectedAllTeamsAndMembers.keySet()) {
			Collection<TeamMember> expectedTeamMembers = expectedAllTeamsAndMembers.get(t);
			Collection<TeamMember> actualTeamMembers = actualAllTeamsAndMembers.get(t);
			assertNotNull(actualTeamMembers, "Missing key "+t);
			assertEquals(expectedTeamMembers.size(), actualTeamMembers.size());
			for (TeamMember m : expectedTeamMembers) {
				assertTrue(actualTeamMembers.contains(m), "expected "+m+" but found "+actualTeamMembers);
			}
		}
	}

	@Test
	public void testGetAllTeamsUserIsAdmin() {
		Team team = createTeam("UserIsAdminTestTeam");
		UserGroup user = createIndividual();

		// Add the user to the team
		groupMembersDAO.addMembers(team.getId(), Collections.singletonList(user.getId()));

		// Call under test, user is not an admin
		List<String> teamIds = teamDAO.getAllTeamsUserIsAdmin(user.getId());
		assertNotNull(teamIds);
		assertEquals(0, teamIds.size());

		addUserAsAdmin(Long.parseLong(user.getId()), team.getId());

		// Call under test, user is an admin
		teamIds = teamDAO.getAllTeamsUserIsAdmin(user.getId());
		assertNotNull(teamIds);
		assertEquals(1, teamIds.size());
		assertEquals(team.getId(), teamIds.get(0));
	}

	private Team aTeam;
	private Team bTeam;
	private Team cTeam;
	private static final String aTeamName = "a team";
	private static final String bTeamName = "b team";
	private static final String cTeamName = "C team";

	private UserGroup beforeGetIdsForMember() {
		UserGroup user = createIndividual();

		bTeam = createTeam(bTeamName);
		aTeam = createTeam(aTeamName);
		cTeam = createTeam(cTeamName);

		groupMembersDAO.addMembers(bTeam.getId(), Arrays.asList(user.getId()));
		groupMembersDAO.addMembers(aTeam.getId(), Arrays.asList(user.getId()));
		groupMembersDAO.addMembers(cTeam.getId(), Arrays.asList(user.getId()));

		return user;
	}

	@Test
	public void testGetIdsForMemberNoOrder() {
		UserGroup user = beforeGetIdsForMember();

		// Method under test
		List<String> teamIds = teamDAO.getIdsForMember(user.getId(), 3, 0, null, null);

		assertEquals(3, teamIds.size());
		assertTrue(teamIds.contains(aTeam.getId()));
		assertTrue(teamIds.contains(bTeam.getId()));
		assertTrue(teamIds.contains(cTeam.getId()));
	}

	@Test
	public void testGetIdsForMemberOrderByNameAsc() {
		UserGroup user = beforeGetIdsForMember();

		// Method under test
		List<String> teamIds = teamDAO.getIdsForMember(user.getId(), 2, 0, TeamSortOrder.TEAM_NAME, true);

		assertEquals(2, teamIds.size());
		assertEquals(aTeam.getId(), teamIds.get(0));
		assertEquals(bTeam.getId(), teamIds.get(1));
	}

	@Test
	public void testGetIdsForMemberOrderByNameDesc() {
		UserGroup user = beforeGetIdsForMember();

		// Method under test
		List<String> teamIds = teamDAO.getIdsForMember(user.getId(), 2, 0, TeamSortOrder.TEAM_NAME, false);

		assertEquals(2, teamIds.size());
		assertEquals(cTeam.getId(), teamIds.get(0));
		assertEquals(bTeam.getId(), teamIds.get(1));
	}

	@Test
	public void testGetIdsForMemberNullPrincipalId() {
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			teamDAO.getIdsForMember(null, 0, 0, null, null);
		});
	}

	@Test
	public void testGetIdsForMemberIllegalOrderAndAscending() {
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			teamDAO.getIdsForMember("1", 0, 0, null, true);
		});
		
		assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			teamDAO.getIdsForMember("1", 0, 0, TeamSortOrder.TEAM_NAME, null);
		});
	}

	@Test
	public void testGetValidTeam() {
		UserGroup group = new UserGroup();
		group.setId(userGroupDAO.create(group).toString());
		teamsToDelete.add(group.getId());

		Team team = new Team();
		Long id = Long.parseLong(group.getId());
		team.setId("" +id);
		teamDAO.create(team);
		// Call under test
		teamDAO.get(team.getId());
	}

	@Test
	public void testGetNonexistentTeam() {
		String invalidTeamId = "404";
		String innerExceptionMessage = "The resource you are attempting to access cannot be found";
		String expected = "Team does not exist for teamId: " + invalidTeamId;
		NotFoundException exception = assertThrows(NotFoundException.class, () -> {
			// Call under test
			teamDAO.get(invalidTeamId);
		});
		assertEquals(expected, exception.getMessage());
		assertNotNull(exception.getCause());
		assertEquals(innerExceptionMessage ,exception.getCause().getMessage());
	}

	@Test
	public void testValidateTeamExists() {
		UserGroup group = new UserGroup();
		group.setId(userGroupDAO.create(group).toString());
		teamsToDelete.add(group.getId());

		Team team = new Team();
		Long id = Long.parseLong(group.getId());
		team.setId("" +id);
		teamDAO.create(team);
		// Call under test
		teamDAO.validateTeamExists(team.getId());
	}

	@Test
	public void testValidateTeamExistsNotFound() {
		String invalidTeamId = "404";
		String expected = "Team does not exist for teamId: " + invalidTeamId;
		NotFoundException exception = assertThrows(NotFoundException.class, () -> {
			// Call under test
			teamDAO.validateTeamExists(invalidTeamId);
		});
		assertEquals(expected, exception.getMessage());
	}

	private Team createTeam(String teamName) {
		return createTeam(teamName, null, "101", "102");
	}
	
	private Team createTeam(String teamName, String icon, String createdBy, String modifiedBy) {
		UserGroup group = new UserGroup();
		group.setIsIndividual(false);
		group.setId(userGroupDAO.create(group).toString());
		teamsToDelete.add(group.getId());

		Team team = new Team();
		Long id = Long.parseLong(group.getId());
		team.setId(""+id);
		team.setName(teamName);
		team.setIcon(icon);
		team.setDescription("This is a Team designated for testing.");
		team.setCreatedOn(new Date());
		team.setCreatedBy(createdBy);
		team.setModifiedOn(new Date());
		team.setModifiedBy(modifiedBy);
		Team createdTeam = teamDAO.create(team);
		bindTeamName(teamName, id);
		
		return createdTeam;

	}

	private UserGroup createIndividual() {
		UserGroup user = new UserGroup();
		user.setIsIndividual(true);
		user.setId(userGroupDAO.create(user).toString());
		createUserProfile(user.getId());
		usersToDelete.add(user.getId());
		return user;
	}

	private void createUserProfile(String userId) {
		UserProfile profile = new UserProfile();
		profile.setOwnerId(userId);
		profile.setUserName("user-name-" + UUID.randomUUID());
		userProfileDAO.create(profile);
	}

	private void addUserAsAdmin(Long userId, String teamId) {
		AccessControlList acl = AccessControlListUtil.createACL(teamId, new UserInfo(true, userId),
				Collections.singleton(ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE), new Date());
		aclToDelete = aclDAO.create(acl, ObjectType.TEAM);
	}

	private void bindUserName(String name, Long userId) {
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias(name);
		alias.setPrincipalId(userId);
		alias.setType(AliasType.USER_NAME);
		principalAliasDAO.bindAliasToPrincipal(alias);
	}

	private void bindTeamName(String name, Long teamId){
		// Determine if the email already exists
		PrincipalAlias alias = principalAliasDAO.findPrincipalWithAlias(name);
		if(alias != null && !alias.getPrincipalId().equals(teamId)){
			throw new NameConflictException("Name "+name+" is already used.");
		}
		// Bind the team name
		alias = new PrincipalAlias();
		alias.setAlias(name);
		alias.setPrincipalId(teamId);
		alias.setType(AliasType.TEAM_NAME);
		// bind this alias
		principalAliasDAO.bindAliasToPrincipal(alias);
	}
}
