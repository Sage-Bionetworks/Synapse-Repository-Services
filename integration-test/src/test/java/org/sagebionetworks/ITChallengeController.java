package org.sagebionetworks;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengeTeam;
import org.sagebionetworks.repo.model.PaginatedIds;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.TeamMemberTypeFilterOptions;

@ExtendWith(ITTestExtension.class)
public class ITChallengeController {

	private static final String MOCK_TEAM_ENDPOINT = "https://www.synapse.org/#Team:";
	private static final String MOCK_NOTIFICATION_UNSUB_ENDPOINT = "https://www.synapse.org#unsub:";

	private static String synapseUserId;
	private static String adminUserId;

	private List<String> entitiesToDelete;
	private List<String> activitiesToDelete;
	
	private Challenge challenge;
	private Team participantTeam;
	private Team registeredTeam;
	private ChallengeTeam challengeTeam;
	
	private SynapseAdminClient adminSynapse;
	private SynapseClient synapse;
	
	public ITChallengeController(SynapseAdminClient adminSynapse, SynapseClient synapse) {
		this.adminSynapse = adminSynapse;
		this.synapse = synapse;
	}
	
	@BeforeAll
	public static void beforeClass(SynapseAdminClient adminSynapse, SynapseClient synapse) throws Exception {
		adminUserId = adminSynapse.getMyProfile().getOwnerId();
		synapseUserId = synapse.getMyProfile().getOwnerId();
	}
	
	private Team createTeam(String name) throws SynapseException {
		// make sure team name isn't already used
		List<Team> existingTeams = adminSynapse.getTeams(name, 10L, 0L).getResults();
		for (Team t : existingTeams) {
			adminSynapse.deleteTeam(t.getId());
		}

		Team team = new Team();
		team.setName(name);
		team.setCanPublicJoin(true);
		team = adminSynapse.createTeam(team);
		return team;
	}
	
	@BeforeEach
	public void before() throws Exception {
		adminSynapse.clearAllLocks();
		entitiesToDelete = new ArrayList<String>();
		activitiesToDelete = new ArrayList<String>();
		participantTeam = createTeam("ITChallengeController_test_participants");
		registeredTeam = createTeam("ITChallengeController_test_my-team");
		challenge = null;
		challengeTeam = null;

		// Note:  since the 'admin' user is the one who created the 
		// team, he's the only one in that team initially
		PaginatedResults<TeamMember> initialMembers = 
				synapse.getTeamMembers(participantTeam.getId(), null, TeamMemberTypeFilterOptions.ALL,50L, 0L);
		assertEquals(1L, initialMembers.getTotalNumberOfResults());
		assertEquals(adminUserId, initialMembers.getResults().get(0).getMember().getOwnerId());
	}
	
	@AfterEach
	public void after() throws Exception {
		if (challengeTeam!=null) {
			adminSynapse.deleteChallengeTeam(challengeTeam.getId());
			challengeTeam=null;
		}
		if (challenge!=null) {
			adminSynapse.deleteChallenge(challenge.getId());
			challenge=null;
		}
		if (participantTeam!=null) {
			adminSynapse.deleteTeam(participantTeam.getId());
			participantTeam=null;
		}
		if (registeredTeam!=null) {
			adminSynapse.deleteTeam(registeredTeam.getId());
			registeredTeam=null;
		}
		for(String id : entitiesToDelete) {
			synapse.deleteEntityById(id);
		}
		for(String id : activitiesToDelete) {
			synapse.deleteActivity(id);
		}
	}
	
	private void checkChallengeParticipants(String challengeId, Set<String> affiliated, 
			Set<String> unaffiliated) throws SynapseException {
		PaginatedIds actual;
		actual=synapse.listChallengeParticipants(""+challengeId, /*affiliated*/true, 10L, 0L);
		assertEquals((long)affiliated.size(), actual.getTotalNumberOfResults().longValue());
		assertEquals(affiliated, new HashSet<String>(actual.getResults()));
		
		actual=synapse.listChallengeParticipants(""+challengeId, /*affiliated*/false, null, 0L);
		assertEquals((long)unaffiliated.size(), actual.getTotalNumberOfResults().longValue());
		assertEquals(unaffiliated, new HashSet<String>(actual.getResults()));
		
		actual=synapse.listChallengeParticipants(""+challengeId, /*affiliated*/null, null, null);
		
		Set<String> all = new HashSet<String>();
		all.addAll(affiliated);
		all.addAll(unaffiliated);
		assertEquals((long)all.size(), actual.getTotalNumberOfResults().longValue());
		assertEquals(all, new HashSet<String>(actual.getResults()));
	}
		
	@Test 
	public void challengeRoundTripTest() throws Exception {
		// create
		Project project = new Project();
		project = synapse.createEntity(project);
		entitiesToDelete.add(project.getId());
		
		challenge = new Challenge();
		challenge.setProjectId(project.getId());
		challenge.setParticipantTeamId(participantTeam.getId());
		
		challenge = synapse.createChallenge(challenge);
		assertNotNull(challenge.getId());
		String challengeId = challenge.getId();
				
		HashSet<String> affiliatedParticipants = new HashSet<String>();
		HashSet<String> unaffiliatedParticipants = new HashSet<String>();
		unaffiliatedParticipants.add(adminUserId);
		checkChallengeParticipants(challengeId, 
				affiliatedParticipants, unaffiliatedParticipants);
		
		Challenge retrieved = synapse.getChallengeForProject(project.getId());
		assertEquals(challenge, retrieved);
		
		retrieved = synapse.getChallenge(challenge.getId());
		assertEquals(challenge, retrieved);
		
		List<Challenge> challenges = 
				synapse.listChallengesForParticipant(""+synapseUserId, 10L, 0L).getResults();
		assertTrue(challenges.isEmpty());
		
		// Now join the challenge and see it appear in the query results
		synapse.addTeamMember(participantTeam.getId(), synapseUserId.toString(), MOCK_TEAM_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		assertEquals(Collections.singletonList(challenge),
				synapse.listChallengesForParticipant(""+synapseUserId, null, null).getResults()
				);
		
		// now there are two unaffiliated participants and no affiliated ones
		unaffiliatedParticipants.add(synapseUserId.toString());
		checkChallengeParticipants(challengeId, 
				affiliatedParticipants, unaffiliatedParticipants);

		Challenge updated = synapse.updateChallenge(challenge);
		assertFalse(updated.getEtag().equals(challenge.getEtag()));
		
		assertTrue(
			synapse.listChallengeTeams(challengeId, 10L, 0L).
				getResults().isEmpty());
		
		// make the user a Team admin so they have permission to register with the challenge
		synapse.addTeamMember(registeredTeam.getId(), synapseUserId.toString(), MOCK_TEAM_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		adminSynapse.setTeamMemberPermissions(registeredTeam.getId(), synapseUserId.toString(), true);
		// double check that the user is now a team administrator
		TeamMember tm = synapse.getTeamMember(registeredTeam.getId(), synapseUserId.toString());
		assertTrue(tm.getIsAdmin());
		
		PaginatedIds registratableTeams = synapse.listRegistratableTeams(challengeId, 10L, 0L);
		assertEquals(Collections.singletonList(registeredTeam.getId()),registratableTeams.getResults());
		assertEquals(new Long(1L), registratableTeams.getTotalNumberOfResults());

		challengeTeam = new ChallengeTeam();
		challengeTeam.setChallengeId(challenge.getId());
		challengeTeam.setMessage("please join our team!!");
		challengeTeam.setTeamId(registeredTeam.getId());
		challengeTeam = synapse.createChallengeTeam(challengeTeam);
		assertNotNull(challengeTeam.getId());
		
		assertEquals(Collections.singletonList(challengeTeam),
			synapse.listChallengeTeams(challengeId, null, null).
				getResults());
		
		// no longer registratable
		registratableTeams = synapse.listRegistratableTeams(challengeId, 10L, 0L);
		assertTrue(registratableTeams.getResults().isEmpty(), registratableTeams.getResults().toString());
		assertEquals(new Long(0L), registratableTeams.getTotalNumberOfResults());
		
		// having registered the Team, both users are now affiliated
		// now everyone is affiliated with a Team
		affiliatedParticipants.add(adminUserId);
		affiliatedParticipants.add(synapseUserId.toString());
		unaffiliatedParticipants.clear();
		checkChallengeParticipants(challengeId, 
				affiliatedParticipants, unaffiliatedParticipants);
		
		// PLFM-3244: what if they're both in the 'registeredTeam' but only one is in the challenge?
		// 'userToDelete' leaves the challenge
		synapse.removeTeamMember(participantTeam.getId(), synapseUserId.toString());
		affiliatedParticipants.clear();
		unaffiliatedParticipants.clear();
		affiliatedParticipants.add(adminUserId);
		checkChallengeParticipants(challengeId, 
				affiliatedParticipants, unaffiliatedParticipants);
		// rejoin the challenge to finish things up
		synapse.addTeamMember(participantTeam.getId(), synapseUserId.toString(), MOCK_TEAM_ENDPOINT, MOCK_NOTIFICATION_UNSUB_ENDPOINT);
		
		
		
		PaginatedIds submissionTeams = synapse.listSubmissionTeams(challengeId, ""+synapseUserId, null, null);
		assertEquals(new Long(1L), submissionTeams.getTotalNumberOfResults());
		assertEquals(Collections.singletonList(registeredTeam.getId()), submissionTeams.getResults());
		
		ChallengeTeam updatedCT = synapse.updateChallengeTeam(challengeTeam);
		assertFalse(updatedCT.getEtag().equals(challengeTeam.getEtag()));
		
		synapse.deleteChallengeTeam(challengeTeam.getId());
		assertTrue(
				synapse.listChallengeTeams(challengeId, null, null).
					getResults().isEmpty());
		challengeTeam=null;
		
		synapse.deleteChallenge(challengeId);
		try {
			synapse.getChallengeForProject(project.getId());
			fail("SynapseNotFoundException expected");
		} catch (SynapseNotFoundException e) {
			// as expected
		}
		challenge=null;
		
	}

}
