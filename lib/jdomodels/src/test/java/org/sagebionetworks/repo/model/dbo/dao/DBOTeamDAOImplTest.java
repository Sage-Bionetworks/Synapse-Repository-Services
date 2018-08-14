package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import org.sagebionetworks.repo.model.TeamHeader;
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
	private String userToDelete;

	@Before
	public void setup() throws Exception {
		List<Team> teams = teamDAO.getInRange(1000, 0);
		for (Team team : teams) {
			teamDAO.delete(team.getId());
		}
		teamsToDelete = new ArrayList<String>();
	}

	@After
	public void tearDown() throws Exception {
		if (aclToDelete!=null) aclDAO.delete(aclToDelete, ObjectType.TEAM);
		for (String teamId : teamsToDelete) {
			teamDAO.delete(teamId);
			userGroupDAO.delete(teamId);
		}
		if (userToDelete!=null) userGroupDAO.delete(userToDelete);
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
	public void testListTeams() throws Exception {
		List<Team> createdTeams = new ArrayList<Team>();
		for (int i=0; i<3; i++) {
			UserGroup group = new UserGroup();
			group.setIsIndividual(false);
			group.setId(userGroupDAO.create(group).toString());
			teamsToDelete.add(group.getId());

			// create a team
			Team team = new Team();
			Long id = Long.parseLong(group.getId());
			team.setId(""+id);
			team.setName("Super Team_"+i);
			team.setDescription("This is a Team designated for testing.");
			team.setIcon("999");
			team.setCreatedOn(new Date());
			team.setCreatedBy("101");
			team.setModifiedOn(new Date());
			team.setModifiedBy("102");
			createdTeams.add(teamDAO.create(team));
		}
		assertEquals(Arrays.asList(new Team[] {createdTeams.get(1), createdTeams.get(0)}),
		teamDAO.list(Arrays.asList(new Long[]{Long.parseLong(teamsToDelete.get(1)),
				Long.parseLong(teamsToDelete.get(0))})).getList());
		try {
			teamDAO.list(Arrays.asList(new Long[]{Long.parseLong(teamsToDelete.get(1)),
					98776654L+Long.parseLong(teamsToDelete.get(0))}));
			fail("NotFoundException expected");
		} catch (NotFoundException e) {
			// as expected
		}
	}


	@Test
	public void testRoundTrip() throws Exception {
		UserGroup group = new UserGroup();
		group.setIsIndividual(false);
		group.setId(userGroupDAO.create(group).toString());
		teamsToDelete.add(group.getId());

		// create a team
		Team team = new Team();
		Long id = Long.parseLong(group.getId());
		team.setId(""+id);
		team.setName("Super Team");
		team.setDescription("This is a Team designated for testing.");
		team.setIcon("999");
		team.setCreatedOn(new Date());
		team.setCreatedBy("101");
		team.setModifiedOn(new Date());
		team.setModifiedBy("102");
		Team createdTeam = teamDAO.create(team);
		assertNotNull(createdTeam.getEtag());
		createdTeam.setEtag(null); // to allow comparison with 'team'
		assertEquals(team, createdTeam);
		// retrieve the team
		Team clone = teamDAO.get(""+id);
		team.setEtag(clone.getEtag()); // for comparison
		assertEquals(team, clone);
		// update the team
		clone.setName("Super Duper Team");
		clone.setDescription("this is the modified description");
		clone.setIcon("111");
		Team updated = teamDAO.update(clone);
		clone.setEtag(updated.getEtag()); // for comparison
		assertEquals(clone, updated);

		Team retrieved = teamDAO.get(updated.getId());
		assertEquals(updated, retrieved);
		assertEquals(1, teamDAO.getInRange(1, 0).size());
		assertEquals(0, teamDAO.getInRange(2, 1).size());
		assertEquals(1, teamDAO.getCount());
		assertEquals(0, teamDAO.getForMemberInRange(""+id, 1, 0).size());
		assertEquals(0, teamDAO.getForMemberInRange(""+id, 3, 1).size());
		assertEquals(0, teamDAO.getCountForMember(""+id));

		ListWrapper<Team> listed = teamDAO.list(Collections.singletonList(id));
		assertEquals(1, listed.getList().size());
		assertEquals(updated, listed.getList().get(0));

		List<Long> emptyIds = Collections.emptyList();

		assertEquals(new HashMap<TeamHeader,List<UserGroupHeader>>(), teamDAO.getAllTeamsAndMembers());
		assertTrue(teamDAO.list(emptyIds).getList().isEmpty());

		// need an arbitrary user to add to the group
		UserGroup user = new UserGroup();
		user.setIsIndividual(true);
		user.setId(userGroupDAO.create(user).toString());
		userToDelete = user.getId();

		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias("user-name");
		alias.setPrincipalId(Long.parseLong(user.getId()));
		alias.setType(AliasType.USER_NAME);
		principalAliasDAO.bindAliasToPrincipal(alias);

		UserProfile profile = new UserProfile();
		profile.setOwnerId(user.getId());
		userProfileDAO.create(profile);

		groupMembersDAO.addMembers(""+id, Arrays.asList(new String[]{user.getId()}));
		List<Team> membersTeams = teamDAO.getForMemberInRange(user.getId(), 1, 0);
		assertEquals(1, membersTeams.size());
		assertEquals(retrieved, membersTeams.get(0));
		assertEquals(0, teamDAO.getForMemberInRange(user.getId(), 3, 1).size());
		assertEquals(1, teamDAO.getCountForMember(user.getId()));

		Long teamId = Long.parseLong(team.getId());
		Long userIdLong = Long.parseLong(user.getId());
		ListWrapper<TeamMember> listedMembers =
				teamDAO.listMembers(Collections.singletonList(teamId), Arrays.asList(new Long[]{userIdLong, userIdLong}));
		assertEquals(2, listedMembers.getList().size());
		TeamMember member = teamDAO.getMember(team.getId(), user.getId());
		assertEquals(Arrays.asList(new TeamMember[]{member, member}), listedMembers.getList());
		// check that nothing is returned for other team IDs and principal IDs
		try {
			teamDAO.listMembers(Collections.singletonList(0L), Collections.singletonList(Long.parseLong(user.getId())));
			fail("NotFoundException expected");
		} catch (NotFoundException e) {
			// as expected
		}
		try {
			teamDAO.listMembers(Collections.singletonList(teamId), Collections.singletonList(0L));
			fail("NotFoundException expected");
		} catch (NotFoundException e) {
			// as expected
		}

		assertTrue(teamDAO.listMembers(Collections.singletonList(teamId), emptyIds).getList().isEmpty());

		UserProfile up = userProfileDAO.get(user.getId());
		String userName = principalAliasDAO.getUserName(Long.parseLong(user.getId()));
		Map<Team,Collection<TeamMember>> expectedAllTeamsAndMembers = new HashMap<Team,Collection<TeamMember>>();
		UserGroupHeader ugh = createUserGroupHeaderFromUserProfile(up, userName);
		TeamMember tm = new TeamMember();
		tm.setIsAdmin(false);
		tm.setMember(ugh);
		tm.setTeamId(""+id);
		List<TeamMember> tmList = new ArrayList<TeamMember>();
		tmList.add(tm);
		expectedAllTeamsAndMembers.put(updated,  tmList);

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

		// the team has one member,  'pg'
		assertEquals(1L, teamDAO.getMembersCount(updated.getId()));
		List<TeamMember> members = teamDAO.getMembersInRange(updated.getId(), 2, 0);
		assertEquals(1, members.size());
		TeamMember m = members.get(0);
		assertFalse(m.getIsAdmin());
		assertEquals(user.getId(), m.getMember().getOwnerId());
		assertEquals(updated.getId(), m.getTeamId());
		assertTrue(teamDAO.getAdminTeamMembers(updated.getId()).isEmpty());

		// check pagination
		assertEquals(0L, teamDAO.getMembersInRange(updated.getId(), 1, 2).size());
		// try some other team.  should get no results
		assertEquals(0L, teamDAO.getMembersInRange("-999", 10, 0).size());
		assertEquals(0L, teamDAO.getMembersCount("-999"));

		assertEquals(0L, teamDAO.getAdminMemberCount(updated.getId()));
		assertEquals(updated.getId(), member.getTeamId());
		assertFalse(member.getIsAdmin());
		ugh = member.getMember();
		assertTrue(ugh.getIsIndividual());
		assertEquals(user.getId(), ugh.getOwnerId());

		List<String> teamIds = teamDAO.getAllTeamsUserIsAdmin(user.getId());
		assertNotNull(teamIds);
		assertTrue(teamIds.isEmpty());

		// now make the member an admin
		AccessControlList acl = AccessControlListUtil.createACL(updated.getId(), new UserInfo(true, user.getId()), Collections.singleton(ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE), new Date());
		aclToDelete = aclDAO.create(acl, ObjectType.TEAM);

		assertEquals(1L, teamDAO.getAdminMemberCount(updated.getId()));
		member = teamDAO.getMember(updated.getId(), user.getId());
		assertEquals(updated.getId(), member.getTeamId());
		assertTrue(member.getIsAdmin());
		assertEquals(1, teamDAO.getAdminTeamMembers(updated.getId()).size());
		assertEquals(user.getId(), teamDAO.getAdminTeamMembers(updated.getId()).get(0));
		ugh = member.getMember();
		assertTrue(ugh.getIsIndividual());
		assertEquals(user.getId(), ugh.getOwnerId());

		teamIds = teamDAO.getAllTeamsUserIsAdmin(user.getId());
		assertNotNull(teamIds);
		assertEquals(1, teamIds.size());
		assertEquals(updated.getId(), teamIds.get(0));

		members = teamDAO.getMembersInRange(updated.getId(), 2, 0);
		assertEquals(1, members.size());
		assertEquals(member, members.get(0));

		listedMembers = teamDAO.listMembers(Collections.singletonList(teamId), Collections.singletonList(Long.parseLong(user.getId())));
		assertEquals(member, listedMembers.getList().get(0));
	}


	private UserGroup user;
	private Team aTeam;
	private Team bTeam;
	private Team cTeam;
	private static final String aTeamName = "a team";
	private static final String bTeamName = "b team";
	private static final String cTeamName = "C team";

	private void beforeGetIdsForMember() {
		user = new UserGroup();
		user.setIsIndividual(true);
		user.setId(userGroupDAO.create(user).toString());
		userToDelete = user.getId();

		bTeam = createTeam(bTeamName);
		aTeam = createTeam(aTeamName);
		cTeam = createTeam(cTeamName);

		groupMembersDAO.addMembers(bTeam.getId(), Arrays.asList(user.getId()));
		groupMembersDAO.addMembers(aTeam.getId(), Arrays.asList(user.getId()));
		groupMembersDAO.addMembers(cTeam.getId(), Arrays.asList(user.getId()));
	}

	@Test
	public void testGetIdsForMemberNoOrder() {
		beforeGetIdsForMember();

		// Method under test
		List<String> teamIds = teamDAO.getIdsForMember(user.getId(), 3, 0, null, null);

		assertEquals(3, teamIds.size());
		assertTrue(teamIds.contains(aTeam.getId()));
		assertTrue(teamIds.contains(bTeam.getId()));
		assertTrue(teamIds.contains(cTeam.getId()));
	}

	@Test
	public void testGetIdsForMemberOrderByNameAsc() {
		beforeGetIdsForMember();

		// Method under test
		List<String> teamIds = teamDAO.getIdsForMember(user.getId(), 2, 0, TeamSortOrder.TEAM_NAME, true);

		assertEquals(2, teamIds.size());
		assertEquals(aTeam.getId(), teamIds.get(0));
		assertEquals(bTeam.getId(), teamIds.get(1));
	}

	@Test
	public void testGetIdsForMemberOrderByNameDesc() {
		beforeGetIdsForMember();

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
		assertNotNull(createdTeam.getEtag());
		createdTeam.setEtag(null); // to allow comparison with 'team'
		assertEquals(team, createdTeam);

		bindTeamName(teamName, id);

		return team;
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
