package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengeDAO;
import org.sagebionetworks.repo.model.ChallengeSummary;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
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
	GroupMembersDAO groupMembershipDAO;
	
	private List<Node> nodesToDelete;
	private Challenge challenge;
	private UserGroup participantGroup;
	private static final long PARTICIPANT_TEAM_ID=BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId();;
	
	@Before
	public void setUp() throws Exception {
		nodesToDelete = new ArrayList<Node>();
		participantGroup = new UserGroup();
		participantGroup.setIsIndividual(false);
		Long participantGroupId = userGroupDAO.create(participantGroup);
		participantGroup.setId(participantGroupId.toString());
	}
	
	@After
	public void tearDown() throws Exception {
		for (Node node : nodesToDelete) {
			nodeDAO.delete(node.getId());
		}
		if (challenge!=null) {
			challengeDAO.delete(Long.parseLong(challenge.getId()));
			challenge = null;
		}
		if (participantGroup!=null) {
			userGroupDAO.delete(participantGroup.getId());
			participantGroup = null;
		}
	}

	//TODO test that two challenges can't belong to the same project
	@Test
	public void roundTrip() throws Exception {
		challenge = new Challenge();
		Long adminPrincipalId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		Node node = NodeTestUtils.createNew("challengeProject", adminPrincipalId);
		node.setId(nodeDAO.createNew(node));
		nodesToDelete.add(node);
		challenge.setProjectId(node.getId());
		challenge.setParticipantTeamId(""+PARTICIPANT_TEAM_ID);
		challenge = challengeDAO.create(challenge);
		assertNotNull(challenge.getEtag());
		assertNotNull(challenge.getId());
		assertEquals(""+PARTICIPANT_TEAM_ID, challenge.getParticipantTeamId());
		assertEquals(node.getId(), challenge.getProjectId());
		
		challenge.setParticipantTeamId(participantGroup.getId());
		challenge = challengeDAO.update(challenge);
		assertEquals(participantGroup.getId(), challenge.getParticipantTeamId());
		
		Challenge retrieved = challengeDAO.getForProject(node.getId());
		assertEquals(challenge, retrieved);
		
		try {
			challengeDAO.getForProject("syn987654321");
			fail("Expected NotFoundException");
		} catch (NotFoundException e) {
			//as expected
		}
		
		assertTrue(challengeDAO.listForUser(adminPrincipalId.toString(), 10, 0).isEmpty());
		assertEquals(0, challengeDAO.listForUserCount(adminPrincipalId.toString()));
		
		groupMembershipDAO.addMembers(participantGroup.getId(), Collections.singletonList(adminPrincipalId.toString()));
		
		assertEquals(1, challengeDAO.listForUserCount(adminPrincipalId.toString()));
		ChallengeSummary challengeSummary = new ChallengeSummary();
		challengeSummary.setChallengeId(challenge.getId());
		challengeSummary.setName(node.getName());
		challengeSummary.setParticipantTeamId(participantGroup.getId());
		challengeSummary.setProjectId(node.getId());
		assertEquals(Collections.singletonList(challengeSummary),
				challengeDAO.listForUser(adminPrincipalId.toString(), 10, 0));
		
		// TODO
//		challengeDAO.listForUser(principalId, userId, limit, offset)
//		challengeDAO.listForUserCount(principalId, userId)
		
		// TODO rerun with 'affiliated'= true, false
		challengeDAO.listParticipants(challenge.getId(), null, 10L, 0L);
		challengeDAO.listParticipantsCount(challenge.getId(), null);
		
		challengeDAO.delete(Long.parseLong(challenge.getId()));
		challenge=null;
	}

}
