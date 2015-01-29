package org.sagebionetworks.repo.model.dbo.dao;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengeDAO;
import org.sagebionetworks.repo.model.ChallengeTeam;
import org.sagebionetworks.repo.model.ChallengeTeamDAO;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
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
	GroupMembersDAO groupMembershipDAO;
	
	private List<Node> nodesToDelete;
	private Challenge challenge;
	private ChallengeTeam challengeTeam;
	private UserGroup participantGroup;
	private UserGroup registeredTeam;

	@Before
	public void setUp() throws Exception {
		nodesToDelete = new ArrayList<Node>();
		
		participantGroup = new UserGroup();
		participantGroup.setIsIndividual(false);
		Long participantGroupId = userGroupDAO.create(participantGroup);
		participantGroup.setId(participantGroupId.toString());
		
		challenge = new Challenge();
		Long adminPrincipalId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		Node node = NodeTestUtils.createNew("challengeProject", adminPrincipalId);
		node.setId(nodeDAO.createNew(node));
		nodesToDelete.add(node);
		challenge.setProjectId(node.getId());
		challenge.setParticipantTeamId(participantGroup.getId());
		challenge = challengeDAO.create(challenge);
		
		registeredTeam = new UserGroup();
		registeredTeam.setIsIndividual(false);
		Long registeredTeamId = userGroupDAO.create(registeredTeam);
		registeredTeam.setId(registeredTeamId.toString());
	}

	@After
	public void tearDown() throws Exception {
		for (Node node : nodesToDelete) {
			nodeDAO.delete(node.getId());
		}
		if (challengeTeam!=null) {
			challengeTeamDAO.delete(Long.parseLong(challengeTeam.getId()));
			challengeTeam = null;
		}
		if (challenge!=null) {
			challengeDAO.delete(Long.parseLong(challenge.getId()));
			challenge = null;
		}
		if (registeredTeam!=null) {
			userGroupDAO.delete(registeredTeam.getId());
			registeredTeam = null;
		}
		if (participantGroup!=null) {
			userGroupDAO.delete(participantGroup.getId());
			participantGroup = null;
		}
	}

	@Test
	public void testRoundTrip() throws Exception {
		challengeTeam = new ChallengeTeam();
		challengeTeam.setChallengeId(challenge.getId());
		challengeTeam.setMessage("Join Our Team!!");
		challengeTeam.setTeamId(registeredTeam.getId());
		
		challengeTeam = challengeTeamDAO.create(challengeTeam);
		
		challengeTeamDAO.delete(Long.parseLong(challengeTeam.getId()));
		challengeTeam=null;
	}

}
