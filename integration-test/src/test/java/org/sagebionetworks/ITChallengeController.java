package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengeTeam;
import org.sagebionetworks.repo.model.PaginatedIds;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Team;

public class ITChallengeController {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;

	private List<String> entitiesToDelete;
	private List<String> activitiesToDelete;
	
	private Challenge challenge;
	private Team participantTeam;
	private Team registeredTeam;
	private ChallengeTeam challengeTeam;
	
	@BeforeClass 
	public static void beforeClass() throws Exception {
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
	}
	
	private Team createTeam() throws SynapseException {
		Team team = new Team();
		team.setCanPublicJoin(true);
		team = synapse.createTeam(team);
		return team;
	}
	
	@Before
	public void before() throws Exception {
		entitiesToDelete = new ArrayList<String>();
		activitiesToDelete = new ArrayList<String>();
		participantTeam = createTeam();
		registeredTeam = createTeam();
		challenge = null;
		challengeTeam = null;
	}
	
	@After
	public void after() throws Exception {
		if (challengeTeam!=null) {
			adminSynapse.removeTeamFromChallenge(challengeTeam.getId());
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
			synapse.deleteAndPurgeEntityById(id);
		}
		for(String id : activitiesToDelete) {
			synapse.deleteActivity(id);
		}
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		adminSynapse.deleteUser(userToDelete);
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
		
		Challenge retrieved = synapse.getChallengeForProject(project.getId());
		assertEquals(challenge, retrieved);
		assertTrue(
				synapse.listChallengesForParticipant(userToDelete, 10L, 0L).
					getList().isEmpty());
		
		// Now join the challenge and see it appear in the query results
		synapse.addTeamMember(participantTeam.getId(), userToDelete.toString());
		assertEquals(Collections.singletonList(challenge),
				synapse.listChallengesForParticipant(userToDelete, null, null).getList()
				);
		
		Challenge updated = synapse.updateChallenge(challenge);
		assertFalse(updated.getEtag().equals(challenge.getEtag()));
		
		assertTrue(
			synapse.listChallengeTeams(Long.parseLong(challenge.getId()), 10L, 0L).
				getResults().isEmpty());
		
		synapse.addTeamMember(registeredTeam.getId(), userToDelete.toString());
		synapse.setTeamMemberPermissions(registeredTeam.getId(), userToDelete.toString(), true);

		PaginatedIds registratableTeams = synapse.listRegistratableTeams(Long.parseLong(challenge.getId()), 10L, 0L);
		assertEquals(new Long(1L), registratableTeams.getTotalNumberOfResults());
		assertEquals(Collections.singletonList(registeredTeam.getId()),registratableTeams.getResults());

		challengeTeam = new ChallengeTeam();
		challengeTeam.setChallengeId(challenge.getId());
		challengeTeam.setMessage("please join our team!!");
		challengeTeam.setTeamId(registeredTeam.getId());
		challengeTeam = synapse.addTeamToChallenge(challengeTeam);
		assertNotNull(challengeTeam.getId());
		
		assertEquals(Collections.singletonList(challengeTeam),
			synapse.listChallengeTeams(Long.parseLong(challenge.getId()), null, null).
				getResults());
		
		// no longer registratable
		registratableTeams = synapse.listRegistratableTeams(Long.parseLong(challenge.getId()), 10L, 0L);
		assertEquals(new Long(0L), registratableTeams.getTotalNumberOfResults());
		assertTrue(registratableTeams.getResults().isEmpty());
		
		
		ChallengeTeam updatedCT = synapse.updateChallengeTeam(challengeTeam);
		assertFalse(updatedCT.getEtag().equals(challengeTeam.getEtag()));
		
		synapse.removeTeamFromChallenge(challengeTeam.getId());
		assertTrue(
				synapse.listChallengeTeams(Long.parseLong(challenge.getId()), null, null).
					getResults().isEmpty());
		challengeTeam=null;
		
		synapse.deleteChallenge(challenge.getId());
		try {
			synapse.getChallengeForProject(project.getId());
		} catch (SynapseNotFoundException e) {
			// as expected
		}
		challenge=null;
		
	}

}
