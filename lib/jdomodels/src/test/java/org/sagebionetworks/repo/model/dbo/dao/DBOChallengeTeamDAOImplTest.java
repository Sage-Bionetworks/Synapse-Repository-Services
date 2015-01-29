package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
	TeamDAO teamDAO;
	
	@Autowired
	UserGroupDAO userGroupDAO;
	
	@Autowired
	GroupMembersDAO groupMembersDAO;
	
	@Autowired
	AccessControlListDAO aclDAO;
	
	private List<Node> nodesToDelete;
	private List<AccessControlList> teamACLsToDelete;
	private Challenge challenge;
	private ChallengeTeam challengeTeam;
	private Team participantTeam;
	private Team registeredTeam;

	@Before
	public void setUp() throws Exception {
		nodesToDelete = new ArrayList<Node>();
		teamACLsToDelete = new ArrayList<AccessControlList>();
		Long adminPrincipalId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		participantTeam = createTeam(adminPrincipalId.toString(), false);
		
		challenge = new Challenge();
		Node node = NodeTestUtils.createNew("challengeProject", adminPrincipalId);
		node.setId(nodeDAO.createNew(node));
		nodesToDelete.add(node);
		challenge.setProjectId(node.getId());
		challenge.setParticipantTeamId(participantTeam.getId());
		challenge = challengeDAO.create(challenge);
		
		registeredTeam = createTeam(adminPrincipalId.toString(), true);
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
	 * check that 'listRegistratable' and 'listRegistratableCount' return the right values
	 */
	private void checkRegistratable(String challengeId, String principalId, List<Team> expected) throws Exception {
		if (expected==null) expected = Collections.EMPTY_LIST;
		assertEquals(
				expected,
				challengeTeamDAO.listRegistratable(challengeId, principalId, 1L+expected.size(), 0L));
		assertEquals(expected.size(), challengeTeamDAO.listRegistratableCount(challengeId, principalId));
		
		// check that pagination works
		assertTrue(challengeTeamDAO.listRegistratable(challengeId, principalId, 1L, expected.size()).isEmpty());
	}

	@Test
	public void testRoundTrip() throws Exception {
		// check that the team _can_ be registered (by an admin of the team)
		String principalId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();
		checkRegistratable(challenge.getId(), principalId, Collections.singletonList(registeredTeam));
		// not registratable by another user
		checkRegistratable(challenge.getId(), "000", null);
				
		challengeTeam = new ChallengeTeam();
		challengeTeam.setChallengeId(challenge.getId());
		challengeTeam.setMessage("Join Our Team!!");
		challengeTeam.setTeamId(registeredTeam.getId());
		
		challengeTeam = challengeTeamDAO.create(challengeTeam);
		
		challengeTeam.setMessage("Please, please, join our Team!!");
		
		ChallengeTeam updated = challengeTeamDAO.update(challengeTeam);
		challengeTeam.setEtag(updated.getEtag());
		assertEquals(challengeTeam, updated);

		// show it's no longer registrable
		checkRegistratable(challenge.getId(), principalId, null);
		// try a different challengeId
		checkRegistratable("000", principalId, Collections.singletonList(registeredTeam));
		
//		challengeTeamDAO.listForChallenge(userId, challengeId, limit, offset)
//		challengeTeamDAO.listForChallengeCount(challengeId)		
		
		challengeTeamDAO.delete(Long.parseLong(challengeTeam.getId()));
		challengeTeam=null;
	}

}
