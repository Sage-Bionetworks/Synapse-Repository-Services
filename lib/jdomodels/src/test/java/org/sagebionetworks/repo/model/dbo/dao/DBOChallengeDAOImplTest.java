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
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengeDAO;
import org.sagebionetworks.repo.model.ChallengeSummary;
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
public class DBOChallengeDAOImplTest {
	
	@Autowired
	ChallengeDAO challengeDAO;
	
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
	
	private List<Node> nodesToDelete;
	private Challenge challenge;
	private List<Team> teamsToDelete;
	private List<AccessControlList> nodeACLsToDelete;
	
	@Before
	public void setUp() throws Exception {
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
		if (challenge!=null) {
			challengeDAO.delete(Long.parseLong(challenge.getId()));
			challenge = null;
		}
		for (Team team : teamsToDelete) {
			deleteTeam(team);
		}
		teamsToDelete.clear();
	}
	
	private void checkListForUser(List<ChallengeSummary> expected, long participantId) throws Exception {
		if (expected==null) expected = Collections.emptyList();
		assertEquals(expected,
				challengeDAO.listForUser(participantId, expected.size()+1, 0));
		assertEquals(expected.size(), challengeDAO.listForUserCount(participantId));
		// test pagination
		assertTrue(challengeDAO.listForUser(participantId, 10L, expected.size()).isEmpty());		
	}

	private void checkListForUser(List<ChallengeSummary> expected, long participantId, long requesterId) throws Exception {
		if (expected==null) expected = Collections.emptyList();
		assertEquals(expected,
				challengeDAO.listForUser(participantId, Collections.singletonList(requesterId), expected.size()+1, 0));
		assertEquals(expected.size(), challengeDAO.listForUserCount(participantId, Collections.singletonList(requesterId)));
		// test pagination
		assertTrue(challengeDAO.listForUser(participantId, Collections.singletonList(requesterId), 10L, expected.size()).isEmpty());		
	}

	@Test
	public void testRoundTrip() throws Exception {
		Long participantId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		Team participantTeam = createTeam(participantId.toString());
		
		challenge = new Challenge();
		Node node = NodeTestUtils.createNew("challengeProject", participantId);
		node.setId(nodeDAO.createNew(node));
		nodesToDelete.add(node);
		challenge.setProjectId(node.getId());
		challenge.setParticipantTeamId(participantTeam.getId());
		challenge = challengeDAO.create(challenge);
		assertNotNull(challenge.getEtag());
		assertNotNull(challenge.getId());
		assertEquals(participantTeam.getId(), challenge.getParticipantTeamId());
		assertEquals(node.getId(), challenge.getProjectId());
		
		Team participantTeam2 = createTeam(participantId.toString());
		challenge.setParticipantTeamId(participantTeam2.getId());
		challenge = challengeDAO.update(challenge);
		assertEquals(participantTeam2.getId(), challenge.getParticipantTeamId());
		
		Challenge retrieved = challengeDAO.getForProject(node.getId());
		assertEquals(challenge, retrieved);
		
		try {
			challengeDAO.getForProject("syn987654321");
			fail("Expected NotFoundException");
		} catch (NotFoundException e) {
			//as expected
		}
		
		ChallengeSummary challengeSummary = new ChallengeSummary();
		challengeSummary.setChallengeId(challenge.getId());
		challengeSummary.setName(node.getName());
		challengeSummary.setParticipantTeamId(participantTeam2.getId());
		challengeSummary.setProjectId(node.getId());
		checkListForUser(Collections.singletonList(challengeSummary),participantId);
		
		// other user is
		checkListForUser(null, 0L);
		
		Long requester = BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId();
		checkListForUser(null, participantId, requester);
		// add requester to ACL
		addACLtoNode(node.getId(), requester, ACCESS_TYPE.READ);
		// the requester is not registered for the challenge ...
		checkListForUser(null, requester, requester);
		// ... but now he can see the 'participant' is registered
		checkListForUser(Collections.singletonList(challengeSummary), participantId, requester);
		
		// TODO rerun with 'affiliated'= true, false
		challengeDAO.listParticipants(Long.parseLong(challenge.getId()), null, 10L, 0L);
		challengeDAO.listParticipantsCount(Long.parseLong(challenge.getId()), null);
		
		
		
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
		
		challengeDAO.delete(Long.parseLong(challenge.getId()));
		challenge=null;
	}

}
