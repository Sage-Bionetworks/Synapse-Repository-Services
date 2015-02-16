package org.sagebionetworks.evaluation.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.evaluation.manager.SubmissionEligibilityManagerImpl.STATUSES_COUNTED_TOWARD_QUOTA;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.MemberSubmissionEligibility;
import org.sagebionetworks.evaluation.model.SubmissionEligibility;
import org.sagebionetworks.evaluation.model.SubmissionQuota;
import org.sagebionetworks.evaluation.model.TeamSubmissionEligibility;
import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengeDAO;
import org.sagebionetworks.repo.model.ChallengeTeamDAO;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.evaluation.EvaluationDAO;
import org.sagebionetworks.repo.model.evaluation.SubmissionDAO;
import org.springframework.test.util.ReflectionTestUtils;

public class SubmissionEligibilityManagerTest {
	
	SubmissionEligibilityManagerImpl submissionEligibilityManager;
	private EvaluationDAO mockEvaluationDAO;
	private SubmissionDAO mockSubmissionDAO;
	private ChallengeDAO mockChallengeDAO;
	private ChallengeTeamDAO mockChallengeTeamDAO;
	private GroupMembersDAO mockGroupMembersDAO;
	private Evaluation evaluation;
	private Challenge challenge;
	private List<UserGroup> challengeParticipants;
	private List<UserGroup> submittingTeamMembers;
	private static final String EVAL_ID = "100";
	private static final String CHALLENGE_ID = "200";
	private static final String SUBMITTER_PRINCIPAL_ID = "300";
	private static final String CHALLENGE_PROJECT_ID = "400";
	private static final String CHALLENGE_PARTICIPANT_TEAM_ID = "500";
	private static final String SUBMITTING_TEAM_ID = "600";
	private static final long MAX_SUBMISSIONS_PER_ROUND = 5L;

	
	@Before
	public void setUp() throws Exception {
		submissionEligibilityManager = new SubmissionEligibilityManagerImpl();
		mockEvaluationDAO = Mockito.mock(EvaluationDAO.class);
		mockSubmissionDAO = Mockito.mock(SubmissionDAO.class);
		mockChallengeDAO = Mockito.mock(ChallengeDAO.class);
		mockChallengeTeamDAO = Mockito.mock(ChallengeTeamDAO.class);
		mockGroupMembersDAO = Mockito.mock(GroupMembersDAO.class);
		
		ReflectionTestUtils.setField(submissionEligibilityManager, "evaluationDAO", mockEvaluationDAO);
		ReflectionTestUtils.setField(submissionEligibilityManager, "submissionDAO", mockSubmissionDAO);
		ReflectionTestUtils.setField(submissionEligibilityManager, "challengeDAO", mockChallengeDAO);
		ReflectionTestUtils.setField(submissionEligibilityManager, "challengeTeamDAO", mockChallengeTeamDAO);
		ReflectionTestUtils.setField(submissionEligibilityManager, "groupMembersDAO", mockGroupMembersDAO);
		
		evaluation = new Evaluation();
		evaluation.setId(EVAL_ID);
		evaluation.setContentSource(CHALLENGE_PROJECT_ID);
		SubmissionQuota quota = new SubmissionQuota();
		long now = System.currentTimeMillis();
		quota.setFirstRoundStart(new Date(now-10000L));
		quota.setNumberOfRounds(10L);
		quota.setRoundDurationMillis(10000L);
		quota.setSubmissionLimit(MAX_SUBMISSIONS_PER_ROUND);
		evaluation.setQuota(quota);
		when(mockEvaluationDAO.get(EVAL_ID)).thenReturn(evaluation);
		
		challenge = new Challenge();
		challenge.setId(CHALLENGE_ID);
		challenge.setProjectId(CHALLENGE_PROJECT_ID);
		challenge.setParticipantTeamId(CHALLENGE_PARTICIPANT_TEAM_ID);
		when(mockChallengeDAO.getForProject(CHALLENGE_PROJECT_ID)).thenReturn(challenge);
		
		challengeParticipants = new ArrayList<UserGroup>();
		when(mockGroupMembersDAO.getMembers(CHALLENGE_PARTICIPANT_TEAM_ID)).thenReturn(challengeParticipants);
		
		
		submittingTeamMembers = new ArrayList<UserGroup>();
		when(mockGroupMembersDAO.getMembers(SUBMITTING_TEAM_ID)).thenReturn(submittingTeamMembers);
	}
	
	private static UserGroup createUserGroup(String principalId) {
		UserGroup ug = new UserGroup();
		ug.setId(principalId);
		return ug;
	}

	@Test
	public void testIsIndividualEligibleNoQuota() throws Exception {
		evaluation.setQuota(null);
		assertTrue(submissionEligibilityManager.
			isIndividualEligible(EVAL_ID, SUBMITTER_PRINCIPAL_ID, new Date()).getAuthorized());
	}

	@Test
	public void testIsIndividualEligibleSubmissionNotAllowed() throws Exception {
		long longTimeAgo = System.currentTimeMillis()-1000000L;
		assertFalse(submissionEligibilityManager.
			isIndividualEligible(EVAL_ID, SUBMITTER_PRINCIPAL_ID, new Date(longTimeAgo)).getAuthorized());
	}

	@Test
	public void testIsIndividualEligibleOverLimit() throws Exception {
		when(mockSubmissionDAO.
				countSubmissionsByContributor(eq(Long.parseLong(EVAL_ID)), 
						eq(Long.parseLong(SUBMITTER_PRINCIPAL_ID)), 
						any(Date.class), any(Date.class), 
						eq(STATUSES_COUNTED_TOWARD_QUOTA))).thenReturn(MAX_SUBMISSIONS_PER_ROUND+1L);
		assertFalse(submissionEligibilityManager.
			isIndividualEligible(EVAL_ID, SUBMITTER_PRINCIPAL_ID, new Date()).getAuthorized());
	}

	@Test
	public void testIsIndividualSubmittedToAnotherTeam() throws Exception {
		when(mockSubmissionDAO.
				countSubmissionsByContributor(eq(Long.parseLong(EVAL_ID)), 
						eq(Long.parseLong(SUBMITTER_PRINCIPAL_ID)), 
						any(Date.class), any(Date.class), 
						eq(STATUSES_COUNTED_TOWARD_QUOTA))).thenReturn(MAX_SUBMISSIONS_PER_ROUND-1L);
		when(mockSubmissionDAO.hasContributedToTeamSubmission(eq(Long.parseLong(EVAL_ID)), 
				eq(Long.parseLong(SUBMITTER_PRINCIPAL_ID)), 
				any(Date.class), any(Date.class), 
				eq(STATUSES_COUNTED_TOWARD_QUOTA))).thenReturn(true);
		assertFalse(submissionEligibilityManager.
			isIndividualEligible(EVAL_ID, SUBMITTER_PRINCIPAL_ID, new Date()).getAuthorized());
	}

	@Test
	public void testIsIndividualEligibleHappyCase() throws Exception {
		when(mockSubmissionDAO.
				countSubmissionsByContributor(eq(Long.parseLong(EVAL_ID)), 
						eq(Long.parseLong(SUBMITTER_PRINCIPAL_ID)), 
						any(Date.class), any(Date.class), 
						eq(STATUSES_COUNTED_TOWARD_QUOTA))).thenReturn(MAX_SUBMISSIONS_PER_ROUND-1L);
		assertTrue(submissionEligibilityManager.
			isIndividualEligible(EVAL_ID, SUBMITTER_PRINCIPAL_ID, new Date()).getAuthorized());
	}

	@Test
	public void testGetTeamSubmissionEligibilityHappyCase() throws Exception {
		// team is registered
		when(mockChallengeTeamDAO.isTeamRegistered(
				Long.parseLong(CHALLENGE_ID), 
				Long.parseLong(SUBMITTING_TEAM_ID))).
			thenReturn(true);
		// add a user to submitting team
		submittingTeamMembers.add(createUserGroup(SUBMITTER_PRINCIPAL_ID));
		// user is registered for challenge
		challengeParticipants.add(createUserGroup(SUBMITTER_PRINCIPAL_ID));
		
		TeamSubmissionEligibility tse = submissionEligibilityManager.
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID);
		
		assertEquals(EVAL_ID, tse.getEvaluationId());
		assertEquals(SUBMITTING_TEAM_ID, tse.getTeamId());
		assertNotNull(tse.getEligibilityStateHash());
		
		SubmissionEligibility teamEligibility = tse.getTeamEligibility();
		assertTrue(teamEligibility.getIsRegistered());
		assertTrue(teamEligibility.getIsEligible());
		assertFalse(teamEligibility.getIsQuotaFilled());
		
		List<MemberSubmissionEligibility> membersEligibility = tse.getMembersEligibility();
		assertEquals(1, membersEligibility.size()); // same as the number added to submittingTeamMembers
		MemberSubmissionEligibility mse = membersEligibility.get(0);
		assertEquals(new Long(SUBMITTER_PRINCIPAL_ID), mse.getPrincipalId());
		assertTrue(mse.getIsRegistered());
		assertFalse(mse.getIsQuotaFilled());
		assertFalse(mse.getHasConflictingSubmission());
		assertTrue(mse.getIsEligible());
	}
	
	
	@Test
	public void testGetTeamSubmissionEligibilityNoChallenge() throws Exception {
		// team is registered
		when(mockChallengeTeamDAO.isTeamRegistered(
				Long.parseLong(CHALLENGE_ID), 
				Long.parseLong(SUBMITTING_TEAM_ID))).
			thenReturn(true);
		// add a user to submitting team
		submittingTeamMembers.add(createUserGroup(SUBMITTER_PRINCIPAL_ID));
		// user is registered for challenge
		challengeParticipants.add(createUserGroup(SUBMITTER_PRINCIPAL_ID));
		
		TeamSubmissionEligibility tse = submissionEligibilityManager.
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID);
		
		assertEquals(EVAL_ID, tse.getEvaluationId());
		assertEquals(SUBMITTING_TEAM_ID, tse.getTeamId());
		assertNotNull(tse.getEligibilityStateHash());
		
		SubmissionEligibility teamEligibility = tse.getTeamEligibility();
		assertTrue(teamEligibility.getIsRegistered());
		assertTrue(teamEligibility.getIsEligible());
		assertFalse(teamEligibility.getIsQuotaFilled());
		
		List<MemberSubmissionEligibility> membersEligibility = tse.getMembersEligibility();
		assertEquals(1, membersEligibility.size()); // same as the number added to submittingTeamMembers
		MemberSubmissionEligibility mse = membersEligibility.get(0);
		assertEquals(new Long(SUBMITTER_PRINCIPAL_ID), mse.getPrincipalId());
		assertTrue(mse.getIsRegistered());
		assertFalse(mse.getIsQuotaFilled());
		assertFalse(mse.getHasConflictingSubmission());
		assertTrue(mse.getIsEligible());
	}
	
	
	@Test
	public void testIsTeamEligible() throws Exception {
		
	}

}
