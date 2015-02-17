package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengeDAO;
import org.sagebionetworks.repo.model.ChallengeTeam;
import org.sagebionetworks.repo.model.ChallengeTeamDAO;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOChallengeTeamDAOImplTest {
	
	@Autowired
	ChallengeDAO challengeDAO;
	
	@Autowired
	ChallengeTeamDAO challengeTeamDAO;
	
	@Autowired
	NodeDAO nodeDAO;
	
	@Autowired
	UserGroupDAO userGroupDAO;
	
	@Autowired
	GroupMembersDAO groupMembersDAO;
	
	@Autowired
	TeamDAO teamDAO;
	
	@Autowired
	AccessControlListDAO aclDAO;
	
	Long principalId;
	private List<Node> nodesToDelete;
	private List<AccessControlList> teamACLsToDelete;
	private Challenge challenge;
	long challengeId;
	private ChallengeTeam challengeTeam;
	private Team participantTeam;
	private Team registeredTeam;

	@Before
	public void setUp() throws Exception {
		principalId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		nodesToDelete = new ArrayList<Node>();
		teamACLsToDelete = new ArrayList<AccessControlList>();
		participantTeam = createTeam(principalId.toString(), false);
		
		challenge = new Challenge();
		Node node = NodeTestUtils.createNew("challengeProject", principalId);
		node.setId(nodeDAO.createNew(node));
		nodesToDelete.add(node);
		challenge.setProjectId(node.getId());
		challenge.setParticipantTeamId(participantTeam.getId());
		challenge = challengeDAO.create(challenge);
		challengeId = Long.parseLong(challenge.getId());
		
		registeredTeam = createTeam(principalId.toString(), true);
	}
	
	private Team createTeam(String ownerId, boolean admin) throws NotFoundException {
		Team team = new Team();
		UserGroup ug = new UserGroup();
		ug.setIsIndividual(false);
		Long id = userGroupDAO.create(ug);
		
		team.setId(id.toString());
		team.setCreatedOn(new Date());
		team.setCreatedBy(ownerId);
		team.setModifiedOn(new Date());
		team.setModifiedBy(ownerId);
		Team created = teamDAO.create(team);
		try {
			groupMembersDAO.addMembers(id.toString(), Arrays.asList(new String[]{ownerId}));
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
				
		Set<ResourceAccess> raSet = new HashSet<ResourceAccess>();
		ResourceAccess ra = new ResourceAccess();
		if (admin) {
			ra.setAccessType(Collections.singleton(ACCESS_TYPE.TEAM_MEMBERSHIP_UPDATE));
		} else {
			ra.setAccessType(Collections.singleton(ACCESS_TYPE.READ));
			
		}
		ra.setPrincipalId(Long.parseLong(ownerId));
		raSet.add(ra);

		AccessControlList acl = new AccessControlList();
		acl.setId(id.toString());
		acl.setCreationDate(new Date());
		acl.setModifiedOn(new Date());
		acl.setResourceAccess(raSet);
		aclDAO.create(acl, ObjectType.TEAM);
		teamACLsToDelete.add(acl);
		return created;
	}
	
	private void deleteTeam(Team team) throws NotFoundException {
		teamDAO.delete(team.getId());
		userGroupDAO.delete(team.getId());
	}

	@After
	public void tearDown() throws Exception {
		for (Node node : nodesToDelete) {
			nodeDAO.delete(node.getId());
		}
		nodesToDelete.clear();
		for (AccessControlList acl : teamACLsToDelete) {
			aclDAO.delete(acl.getId(), ObjectType.TEAM);
		}
		teamACLsToDelete.clear();
		if (challengeTeam!=null) {
			challengeTeamDAO.delete(Long.parseLong(challengeTeam.getId()));
			challengeTeam = null;
		}
		if (challenge!=null) {
			challengeDAO.delete(Long.parseLong(challenge.getId()));
			challenge = null;
		}
		if (registeredTeam!=null) {
			deleteTeam(registeredTeam);
			registeredTeam = null;
		}
		if (participantTeam!=null) {
			deleteTeam(participantTeam);
			participantTeam = null;
		}
	}
	
	/*
	 * check 'listRegistratable' and 'listRegistratableCount'
	 */
	private void checkRegistratable(long challengeId, long principalId, List<String> expected) throws Exception {
		if (expected==null) expected = Collections.emptyList();
		assertEquals(expected,
				challengeTeamDAO.listRegistratable(challengeId, principalId, 1L+expected.size(), 0L));
		assertEquals(expected.size(), challengeTeamDAO.listRegistratableCount(challengeId, principalId));
		
		// check that pagination works
		assertTrue(challengeTeamDAO.listRegistratable(challengeId, principalId, 1L, expected.size()).isEmpty());
	}
	
	/*
	 * check listForChallenge and listForChallengeCount
	 */
	private void checkListForChallenge(long challengeId, 
			List<ChallengeTeam> expected) throws Exception {
		if (expected==null) expected = Collections.emptyList();
		assertEquals(expected,
				challengeTeamDAO.listForChallenge(challengeId, 1L+expected.size(), 0L));
		assertEquals(expected.size(), challengeTeamDAO.listForChallengeCount(challengeId));		
		
		// check that pagination works
		assertTrue(challengeTeamDAO.listForChallenge(challengeId, 1L, expected.size()).isEmpty());
	}
	
	private ChallengeTeam newChallengeTeam() {
		challengeTeam = new ChallengeTeam();
		challengeTeam.setChallengeId(challenge.getId());
		challengeTeam.setMessage("Join Our Team!!");
		challengeTeam.setTeamId(registeredTeam.getId());
		return challengeTeam;
	}
	
	@Test
	public void testCreate() throws Exception {
		challengeTeam = newChallengeTeam();
		challengeTeam = challengeTeamDAO.create(challengeTeam);
		assertNotNull(challengeTeam.getId());
		assertNotNull(challengeTeam.getEtag());
		assertEquals(challenge.getId(), challengeTeam.getChallengeId());
		assertEquals(registeredTeam.getId(), challengeTeam.getTeamId());
		assertEquals("Join Our Team!!", challengeTeam.getMessage());
	}
	
	@Test
	public void testGet() throws Exception {
		challengeTeam = newChallengeTeam();
		challengeTeam = challengeTeamDAO.create(challengeTeam);
		ChallengeTeam retrieved = challengeTeamDAO.get(Long.parseLong(challengeTeam.getId()));
		assertEquals(challengeTeam, retrieved);
	}
	
	@Test
	public void testUpdate() throws Exception {
		challengeTeam = newChallengeTeam();
		challengeTeam = challengeTeamDAO.create(challengeTeam);
		challengeTeam.setMessage("Please, please, join our Team!!");
		ChallengeTeam updated = challengeTeamDAO.update(challengeTeam);
		challengeTeam.setEtag(updated.getEtag());
		assertEquals(challengeTeam, updated);		
	}
	
	@Test
	public void testRegistratable() throws Exception {
		// check that the team _can_ be registered (by an admin of the team)
		checkRegistratable(challengeId, principalId, Collections.singletonList(registeredTeam.getId()));
		// not registratable by another user
		checkRegistratable(challengeId, 0L, null);
		
		challengeTeam = newChallengeTeam();
		challengeTeam = challengeTeamDAO.create(challengeTeam);
		
		// show it's no longer registrable
		checkRegistratable(challengeId, principalId, null);
	}

	@Test
	public void testListForChallenge() throws Exception {
		// check that no Teams are currently registered for the challenge
		checkListForChallenge(challengeId, null);
		checkListForChallenge(0L, null);
			
		challengeTeam = newChallengeTeam();
		challengeTeam = challengeTeamDAO.create(challengeTeam);
		
		// check that it's listed
		checkListForChallenge(challengeId, Collections.singletonList(challengeTeam));
		// but not for other challenges
		checkListForChallenge(0L, null);
	}
	
	@Test
	public void testIsTeamRegistered() throws Exception {
		// check that no Teams are currently registered for the challenge
		checkListForChallenge(challengeId, null);
		checkListForChallenge(0L, null);
		assertFalse(challengeTeamDAO.isTeamRegistered(challengeId, Long.parseLong(registeredTeam.getId())));
			
		challengeTeam = newChallengeTeam();
		challengeTeam = challengeTeamDAO.create(challengeTeam);
		
		assertTrue(challengeTeamDAO.isTeamRegistered(challengeId, Long.parseLong(registeredTeam.getId())));
	}
	 
	private void checkSubmissionTeams(long challengeId, 
			long principalId, Set<String> expected) throws Exception {
		if (expected==null) expected = Collections.emptySet();
		assertEquals(expected,
				new HashSet<String>(challengeTeamDAO.listSubmissionTeams(
						challengeId, principalId, 10L, 0L)));
		
		assertEquals((long)expected.size(), 
				challengeTeamDAO.listSubmissionTeamsCount(challengeId, principalId));
		
		// check pagination
		assertTrue(challengeTeamDAO.listSubmissionTeams(
						challengeId, principalId, 10L, expected.size()).isEmpty());
	}
	
	@Test
	public void testSubmissionTeams() throws Exception {
		// initially 'registeredTeam' is unregistered
		// 'principalId' is a member of registered 
		
		Long otherTeamMember = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();
		// user is NOT registered, and no team is registered
		checkSubmissionTeams(challengeId, otherTeamMember, null);
		
		// user is in team, but no team is registered
		checkSubmissionTeams(challengeId, principalId, null);
		
		// this registers 'registered team'
		challengeTeam = newChallengeTeam();
		challengeTeam = challengeTeamDAO.create(challengeTeam);
		
		// not in Team, but Team is registered
		checkSubmissionTeams(challengeId, otherTeamMember, null);

		// Team is registered and user is in Team
		checkSubmissionTeams(challengeId, principalId, Collections.singleton(registeredTeam.getId()));
		
	}


	@Test
	public void testUniquenessConstraint() throws Exception {
		challengeTeam = newChallengeTeam();
		challengeTeam = challengeTeamDAO.create(challengeTeam);
		
		// the database should not allow same challenge-team combination
		// to be registered twice
		ChallengeTeam secondChallengeTeam = new ChallengeTeam();
		secondChallengeTeam.setChallengeId(challenge.getId());
		secondChallengeTeam.setTeamId(registeredTeam.getId());
		try {
			secondChallengeTeam = challengeTeamDAO.create(secondChallengeTeam);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// as expected
		} finally {
			String id = secondChallengeTeam.getId();
			if (id!=null) challengeTeamDAO.delete(Long.parseLong(id));
		}
	}

	@Test
	public void testDelete() throws Exception {
		challengeTeam = newChallengeTeam();
		challengeTeam = challengeTeamDAO.create(challengeTeam);
		challengeTeamDAO.delete(Long.parseLong(challengeTeam.getId()));
		challengeTeam=null;
		
		assertEquals(0, challengeTeamDAO.listForChallengeCount(challengeId));
	}

}
