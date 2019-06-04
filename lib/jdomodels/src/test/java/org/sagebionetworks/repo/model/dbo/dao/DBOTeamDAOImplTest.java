package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.model.util.AccessControlListUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
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
	
	private List<String> teamsToDelete;
	private String aclToDelete;
	private List<String> usersToDelete;

	@Before
	public void setup() {
		List<Team> teams = teamDAO.getInRange(1000, 0);
		for (Team team : teams) {
			teamDAO.delete(team.getId());
		}
		teamsToDelete = new ArrayList<>();
		usersToDelete = new ArrayList<>();
	}

	@After
	public void tearDown() throws Exception {
		if (aclToDelete!=null) aclDAO.delete(aclToDelete, ObjectType.TEAM);
		for (String teamId : teamsToDelete) {
			teamDAO.delete(teamId);
			userGroupDAO.delete(teamId);
		}

		for (String userId : usersToDelete) {
			userGroupDAO.delete(userId);
		}
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
		teamDAO.list(Arrays.asList(Long.parseLong(teamsToDelete.get(1)),
				Long.parseLong(teamsToDelete.get(0)))).getList());
		try {
			teamDAO.list(Arrays.asList(Long.parseLong(teamsToDelete.get(1)), 98776654L+Long.parseLong(teamsToDelete.get(0))));
			fail("NotFoundException expected");
		} catch (NotFoundException e) {
			// as expected
		}
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
		team.setIcon("999");
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
	public void testDeleteTeam() {
		Team t = createTeam("Test Delete Team");
		teamDAO.delete(t.getId());
		try {
			teamDAO.get(t.getId());
			fail("Expected NotFoundException");
		} catch (NotFoundException e) {
			// As expected
		}
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
		try {
			teamDAO.listMembers(Collections.singletonList(0L), Collections.singletonList(Long.parseLong(user.getId())));
			fail("NotFoundException expected");
		} catch (NotFoundException e) {
			// as expected
		}
		try {
			teamDAO.listMembers(Collections.singletonList(Long.parseLong(team.getId())), Collections.singletonList(0L));
			fail("NotFoundException expected");
		} catch (NotFoundException e) {
			// as expected
		}
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
			assertNotNull("Missing key "+t, actualTeamMembers);
			assertEquals(expectedTeamMembers.size(), actualTeamMembers.size());
			for (TeamMember m : expectedTeamMembers) {
				assertTrue("expected "+m+" but found "+actualTeamMembers, actualTeamMembers.contains(m));
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
		try {
			teamDAO.getIdsForMember(null, 0, 0, null, null);
			fail("Expected an IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// As expected
		}
	}

	@Test
	public void testGetIdsForMemberIllegalOrderAndAscending() {
		try {
			teamDAO.getIdsForMember("1", 0, 0, null, true);
			fail("Expected an IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// As expected
		}
		try {
			teamDAO.getIdsForMember("1", 0, 0, TeamSortOrder.TEAM_NAME, null);
			fail("Expected an IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			// As expected
		}
	}

	private Team createTeam(String teamName) {
		UserGroup group = new UserGroup();
		group.setIsIndividual(false);
		group.setId(userGroupDAO.create(group).toString());
		teamsToDelete.add(group.getId());

		Team team = new Team();
		Long id = Long.parseLong(group.getId());
		team.setId(""+id);
		team.setName(teamName);
		team.setDescription("This is a Team designated for testing.");
		team.setIcon("999");
		team.setCreatedOn(new Date());
		team.setCreatedBy("101");
		team.setModifiedOn(new Date());
		team.setModifiedBy("102");
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
