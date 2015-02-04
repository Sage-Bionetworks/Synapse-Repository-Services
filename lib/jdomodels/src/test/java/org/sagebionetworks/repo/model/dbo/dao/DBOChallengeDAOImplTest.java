package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
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
import org.sagebionetworks.repo.model.InvalidModelException;
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
public class DBOChallengeDAOImplTest {
	
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
	
	private Long participantId;
	private Long requester;
	private List<Node> nodesToDelete;
	private Challenge challenge;
	private List<Team> teamsToDelete;
	private List<AccessControlList> nodeACLsToDelete;
	
	@Before
	public void setUp() throws Exception {
		participantId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		requester = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();

		nodesToDelete = new ArrayList<Node>();
		teamsToDelete = new ArrayList<Team>();
		nodeACLsToDelete = new ArrayList<AccessControlList>();
	}
	
	// NOTE this also registers the team for deletion
	private Team createTeam(String ownerId) throws NotFoundException {
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
		teamsToDelete.add(created);
		return created;
	}
	
	private void addACLtoNode(String nodeId, long principalId, ACCESS_TYPE accessType) throws Exception {
		Set<ResourceAccess> raSet = new HashSet<ResourceAccess>();
		ResourceAccess ra = new ResourceAccess();
		ra.setAccessType(Collections.singleton(accessType));
		ra.setPrincipalId(principalId);
		raSet.add(ra);

		AccessControlList acl = new AccessControlList();
		acl.setId(nodeId);
		acl.setCreationDate(new Date());
		acl.setModifiedOn(new Date());
		acl.setResourceAccess(raSet);
		aclDAO.create(acl, ObjectType.ENTITY);
		
	}
	
	private void deleteTeam(Team team) throws NotFoundException {
		teamDAO.delete(team.getId());
		userGroupDAO.delete(team.getId());
	}

	@After
	public void tearDown() throws Exception {
		for (AccessControlList acl : nodeACLsToDelete) {
			aclDAO.delete(acl.getId(), ObjectType.TEAM);
		}
		nodeACLsToDelete.clear();
		for (Node node : nodesToDelete) {
			nodeDAO.delete(node.getId());
		}
		if (challenge!=null && challenge.getId()!=null) {
			challengeDAO.delete(Long.parseLong(challenge.getId()));
			challenge = null;
		}
		for (Team team : teamsToDelete) {
			deleteTeam(team);
		}
		teamsToDelete.clear();
	}
	
	private Node createNodeAndChallenge(Team participantTeam) throws Exception {
		challenge = new Challenge();
		Node node = NodeTestUtils.createNew("challengeProject", participantId);
		node.setId(nodeDAO.createNew(node));
		nodesToDelete.add(node);
		challenge.setProjectId(node.getId());
		challenge.setParticipantTeamId(participantTeam.getId());
		return node;
	}

	@Test
	public void testCreate() throws Exception {
		Team participantTeam = createTeam(participantId.toString());
		Node node = createNodeAndChallenge(participantTeam);
		challenge = challengeDAO.create(challenge);
		assertNotNull(challenge.getEtag());
		assertNotNull(challenge.getId());
		assertEquals(participantTeam.getId(), challenge.getParticipantTeamId());
		assertEquals(node.getId(), challenge.getProjectId());		
	}
	
	@Test(expected=InvalidModelException.class)
	public void testCreateWithIllegalProject() throws Exception {
		Team participantTeam = createTeam(participantId.toString());
		challenge = new Challenge();
		challenge.setProjectId("syn00000");
		challenge.setParticipantTeamId(participantTeam.getId());
		challenge = challengeDAO.create(challenge);
	}
	
	@Test(expected=InvalidModelException.class)
	public void testCreateWithIllegalTeam() throws Exception {
		Team participantTeam = new Team();
		participantTeam.setId("000");
		createNodeAndChallenge(participantTeam);
		challengeDAO.create(challenge);
	}
	
	@Test
	public void testGet() throws Exception {
		Team participantTeam = createTeam(participantId.toString());
		createNodeAndChallenge(participantTeam);
		challenge = challengeDAO.create(challenge);
		Challenge retrieved = challengeDAO.get(Long.parseLong(challenge.getId()));
		assertEquals(challenge, retrieved);	
	}
		
	@Test
	public void testGetFromProjectId() throws Exception {
		Team participantTeam = createTeam(participantId.toString());
		Node node = createNodeAndChallenge(participantTeam);
		challenge = challengeDAO.create(challenge);
		Challenge retrieved = challengeDAO.getForProject(node.getId());
		assertEquals(challenge, retrieved);	
	}
		
	@Test
	public void testUpdate() throws Exception {
		Team participantTeam = createTeam(participantId.toString());
		createNodeAndChallenge(participantTeam);
		challenge = challengeDAO.create(challenge);
		
		Team participantTeam2 = createTeam(participantId.toString());
		challenge.setParticipantTeamId(participantTeam2.getId());
		challenge = challengeDAO.update(challenge);
		assertEquals(participantTeam2.getId(), challenge.getParticipantTeamId());	
	}
	
	@Test
	public void testUpdateWithIllegalTeam() throws Exception {
		Team participantTeam = createTeam(participantId.toString());
		createNodeAndChallenge(participantTeam);
		challenge = challengeDAO.create(challenge);
		
		Team participantTeam2 = new Team();
		participantTeam2.setId("000");
		challenge.setParticipantTeamId(participantTeam2.getId());
		try {
			challengeDAO.update(challenge);
			fail("expected InvalidModelException");
		} catch (InvalidModelException e) {
			// as expected
		}
	}
	
	@Test(expected=NotFoundException.class)
	public void testGetNonExistent() throws Exception {
		challengeDAO.getForProject("syn987654321");		
	}
	
	private void checkListForUser(List<Challenge> expected, long participantId) throws Exception {
		if (expected==null) expected = Collections.emptyList();
		assertEquals(expected,
				challengeDAO.listForUser(participantId, expected.size()+1, 0));
		assertEquals(expected.size(), challengeDAO.listForUserCount(participantId));
		// test pagination
		assertTrue(challengeDAO.listForUser(participantId, 10L, expected.size()).isEmpty());		
	}

	private void checkListForUser(List<Challenge> expected, long participantId, long requesterId) throws Exception {
		if (expected==null) expected = Collections.emptyList();
		assertEquals(expected,
				challengeDAO.listForUser(participantId, Collections.singleton(requesterId), expected.size()+1, 0));
		assertEquals(expected.size(), challengeDAO.listForUserCount(participantId, Collections.singleton(requesterId)));
		// test pagination
		assertTrue(challengeDAO.listForUser(participantId, Collections.singleton(requesterId), 10L, expected.size()).isEmpty());		
	}

	@Test
	public void testListForUser() throws Exception {
		Team participantTeam = createTeam(participantId.toString());
		Node node = createNodeAndChallenge(participantTeam);
		challenge = challengeDAO.create(challenge);
		
		checkListForUser(Collections.singletonList(challenge),participantId);
		checkListForUser(null, 0L);
		
		checkListForUser(null, participantId, requester);
		// add requester to ACL
		addACLtoNode(node.getId(), requester, ACCESS_TYPE.READ);
		// the requester is not registered for the challenge ...
		checkListForUser(null, requester, requester);
		// ... but now he can see the 'participant' is registered
		checkListForUser(Collections.singletonList(challenge), participantId, requester);
	}
	
	private void checkListParticipants(Set<Long> expected, long challengeId, Boolean isAffiliated) throws Exception {
		if (expected==null) expected = Collections.emptySet();
		Set<Long> actual = new HashSet<Long>(
				challengeDAO.listParticipants(challengeId, isAffiliated, expected.size()+1, 0));
		// need to compare contents, not order
		assertEquals(expected,actual);		
		assertEquals(expected.size(), challengeDAO.listParticipantsCount(challengeId, isAffiliated));
		// test pagination
		assertTrue(challengeDAO.listParticipants(challengeId, isAffiliated, 10L, expected.size()).isEmpty());
	}
	
	private void checkListParticipantsVariants(
			long challengeId, 
			Set<Long> expectedAll,
			Set<Long> expectedAffiliated,
			Set<Long> expectedUNAffiliated
			) throws Exception {
		checkListParticipants(expectedAll, challengeId, null);
		checkListParticipants(expectedAffiliated, challengeId, true);
		checkListParticipants(expectedUNAffiliated, challengeId, false);
	}
	
	@Test
	public void testListParticipants() throws Exception {
		Team participantTeam = createTeam(participantId.toString());
		createNodeAndChallenge(participantTeam);
		challenge = challengeDAO.create(challenge);
				
		// Now let's check the participants list
		// First, show that just one user is a participant
		long challengeId = Long.parseLong(challenge.getId());
				
		checkListParticipantsVariants(challengeId, 
				Collections.singleton(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()), // expected list of all participants
				null, // expected list of participants affiliated with some team
				Collections.singleton(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId())); // expected list of participants NOT affiliated with any team

		// now let's affiliate the participant with a team
		ChallengeTeam challengeTeam = new ChallengeTeam();
		challengeTeam.setChallengeId(challenge.getId());
		Team participantTeam2 = createTeam(participantId.toString());
		challengeTeam.setTeamId(participantTeam2.getId());
		challengeTeam = challengeTeamDAO.create(challengeTeam); // will get cleaned up when 'challenge' is deleted
		
		checkListParticipantsVariants(challengeId, 
				Collections.singleton(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()), // expected list of all participants
				Collections.singleton(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()), // expected list of participants affiliated with some team
				null); // expected list of participants NOT affiliated with any team

		// now sign up a second user for the challenge
		groupMembersDAO.addMembers(participantTeam.getId(), Collections.singletonList(requester.toString()));
		
		Set<Long> both = new HashSet<Long>();
		both.add(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		both.add(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		
		// since the second participant is not on a challenge team, he will show up as unaffiliated
		checkListParticipantsVariants(challengeId, 
				both, // expected list of all participants
				Collections.singleton(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()), // expected list of participants affiliated with some team
				Collections.singleton(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId())); // expected list of participants NOT affiliated with any team
	}
		
	@Test
	public void testUniquenessConstraint() throws Exception {
		Team participantTeam = createTeam(participantId.toString());
		Node node = createNodeAndChallenge(participantTeam);
		challenge = challengeDAO.create(challenge);
		
		// let's make sure a project can't have two challenges
		Challenge secondChallenge = new Challenge();
		secondChallenge.setProjectId(node.getId());
		secondChallenge.setParticipantTeamId(participantTeam.getId());
		try {
			secondChallenge = challengeDAO.create(secondChallenge);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// as expected
		} finally {
			String id = secondChallenge.getId();
			if (id!=null) challengeDAO.delete(Long.parseLong(id));
		}
	}
	
	@Test
	public void testDelete() throws Exception {
		Team participantTeam = createTeam(participantId.toString());
		createNodeAndChallenge(participantTeam);
		challenge = challengeDAO.create(challenge);
		challengeDAO.delete(Long.parseLong(challenge.getId()));
		challenge=null;
	}

}
