package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.TeamHeader;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserGroupHeader;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
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
	
	private String teamToDelete;
	private String aclToDelete;
	private String userToDelete;

	@Before
	public void setup() throws Exception {
		List<Team> teams = teamDAO.getInRange(1000, 0);
		for (Team team : teams) {
			teamDAO.delete(team.getId());
		}
	}

	@After
	public void tearDown() throws Exception {
		if (aclToDelete!=null) aclDAO.delete(aclToDelete, ObjectType.TEAM);
		if (teamToDelete!=null) teamDAO.delete(teamToDelete);
		if (teamToDelete!=null) userGroupDAO.delete(teamToDelete);
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
	public void testRoundTrip() throws Exception {
		UserGroup group = new UserGroup();
		group.setIsIndividual(false);
		group.setId(userGroupDAO.create(group).toString());
		teamToDelete = group.getId();

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
		
		List<Team> listed = teamDAO.list(Collections.singleton(""+id));
		assertEquals(1, listed.size());
		assertEquals(updated, listed.get(0));
		
		Set<String> emptyIds = Collections.emptySet();
		
		assertEquals(new HashMap<TeamHeader,List<UserGroupHeader>>(), teamDAO.getAllTeamsAndMembers());
		assertTrue(teamDAO.list(emptyIds).isEmpty());

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
		
		List<TeamMember> listedMembers = 
				teamDAO.listMembers(team.getId(), Collections.singleton(user.getId()));
		assertEquals(1, listedMembers.size());
		TeamMember member = teamDAO.getMember(team.getId(), user.getId());
		assertEquals(member, listedMembers.get(0));
		// check that nothing is returned for other team IDs and principal IDs
		assertEquals(0, teamDAO.listMembers("0", Collections.singleton(user.getId())).size());
		assertEquals(0, teamDAO.listMembers(team.getId(), Collections.singleton("0")).size());

		assertTrue(teamDAO.listMembers(team.getId(), emptyIds).isEmpty());
		
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
		// now make the member an admin
		AccessControlList acl = createAdminAcl(user.getId(), updated.getId(), new Date());
		aclToDelete = aclDAO.create(acl, ObjectType.TEAM);
		assertEquals(1L, teamDAO.getAdminMemberCount(updated.getId()));
		member = teamDAO.getMember(updated.getId(), user.getId());
		assertEquals(updated.getId(), member.getTeamId());
		assertTrue(member.getIsAdmin());
		ugh = member.getMember();
		assertTrue(ugh.getIsIndividual());
		assertEquals(user.getId(), ugh.getOwnerId());
	}
	
	public static AccessControlList createAdminAcl(
			final String pid, 
			final String teamId, 
			final Date creationDate) {
		Set<ResourceAccess> raSet = new HashSet<ResourceAccess>();
		Set<ACCESS_TYPE> accessSet = new HashSet<ACCESS_TYPE>(Arrays.asList(new ACCESS_TYPE[]{ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE}));
		ResourceAccess ra = new ResourceAccess();
		ra.setAccessType(accessSet);
		ra.setPrincipalId(Long.parseLong(pid));
		raSet.add(ra);
		AccessControlList acl = new AccessControlList();
		acl.setId(teamId);
		acl.setCreatedBy(pid);
		acl.setCreationDate(creationDate);
		acl.setModifiedBy(pid);
		acl.setModifiedOn(creationDate);
		acl.setResourceAccess(raSet);
		return acl;
	}


}
