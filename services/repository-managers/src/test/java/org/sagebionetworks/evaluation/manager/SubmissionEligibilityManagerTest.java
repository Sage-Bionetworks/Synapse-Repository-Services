package org.sagebionetworks.evaluation.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.evaluation.manager.SubmissionEligibilityManagerImpl.STATUSES_COUNTED_TOWARD_QUOTA;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.evaluation.EvaluationDAO;
import org.sagebionetworks.repo.model.evaluation.SubmissionDAO;
import org.sagebionetworks.repo.web.NotFoundException;
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
	private UserInfo userInfo;
	private static final String EVAL_ID = "100";
	private static final String CHALLENGE_ID = "200";
	private static final String SUBMITTER_PRINCIPAL_ID = "300";
	private static final String CHALLENGE_PROJECT_ID = "syn400";
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
		
		userInfo = new UserInfo(false);
		userInfo.setId(Long.parseLong(SUBMITTER_PRINCIPAL_ID));
		userInfo.setGroups(Collections.singleton(Long.parseLong(CHALLENGE_PARTICIPANT_TEAM_ID)));
	}
	
	private static UserGroup createUserGroup(String principalId) {
		UserGroup ug = new UserGroup();
		ug.setId(principalId);
		return ug;
	}

	@Test
	public void testIsIndividualEligibleNoChallenge() throws Exception {
		// no challenge for evaluation
		when(mockChallengeDAO.getForProject(CHALLENGE_PROJECT_ID)).thenThrow(new NotFoundException());
		assertTrue(submissionEligibilityManager.
			isIndividualEligible(EVAL_ID, userInfo, new Date()).isAuthorized());
	}

	@Test
	public void testIsIndividualEligibleNotRegistered() throws Exception {
		// starts out as a happy case
		assertTrue(submissionEligibilityManager.
				isIndividualEligible(EVAL_ID, userInfo, new Date()).isAuthorized());
		// but if you're not registered for the challenge you're ineligible to submit
		userInfo.setGroups(Collections.EMPTY_SET);
		assertFalse(submissionEligibilityManager.
				isIndividualEligible(EVAL_ID, userInfo, new Date()).isAuthorized());
	}

	@Test
	public void testIsIndividualEligibleNoQuota() throws Exception {
		evaluation.setQuota(null);
		assertTrue(submissionEligibilityManager.
			isIndividualEligible(EVAL_ID, userInfo, new Date()).isAuthorized());
	}

	@Test
	public void testIsIndividualEligibleRoundsButNoQuota() throws Exception {
		evaluation.getQuota().setSubmissionLimit(null);
		assertTrue(submissionEligibilityManager.
			isIndividualEligible(EVAL_ID, userInfo, new Date()).isAuthorized());
	}

	@Test
	public void testIsIndividualEligibleSubmissionNotAllowed() throws Exception {
		long longTimeAgo = System.currentTimeMillis()-1000000L;
		assertFalse(submissionEligibilityManager.
			isIndividualEligible(EVAL_ID, userInfo, new Date(longTimeAgo)).isAuthorized());
	}

	@Test
	public void testIsIndividualEligibleOverLimit() throws Exception {
		when(mockSubmissionDAO.
				countSubmissionsByContributor(eq(Long.parseLong(EVAL_ID)), 
						eq(Long.parseLong(SUBMITTER_PRINCIPAL_ID)), 
						any(Date.class), any(Date.class), 
						eq(STATUSES_COUNTED_TOWARD_QUOTA))).thenReturn(MAX_SUBMISSIONS_PER_ROUND+1L);
		assertFalse(submissionEligibilityManager.
			isIndividualEligible(EVAL_ID, userInfo, new Date()).isAuthorized());
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
			isIndividualEligible(EVAL_ID, userInfo, new Date()).isAuthorized());
		
		verify(mockSubmissionDAO).hasContributedToTeamSubmission(eq(Long.parseLong(EVAL_ID)), 
				eq(Long.parseLong(SUBMITTER_PRINCIPAL_ID)), 
				any(Date.class), any(Date.class), 
				eq(STATUSES_COUNTED_TOWARD_QUOTA));
	}

	@Test
	public void testIsIndividualEligibleHappyCase() throws Exception {
		when(mockSubmissionDAO.
				countSubmissionsByContributor(eq(Long.parseLong(EVAL_ID)), 
						eq(Long.parseLong(SUBMITTER_PRINCIPAL_ID)), 
						any(Date.class), any(Date.class), 
						eq(STATUSES_COUNTED_TOWARD_QUOTA))).thenReturn(MAX_SUBMISSIONS_PER_ROUND-1L);
		assertTrue(submissionEligibilityManager.
			isIndividualEligible(EVAL_ID, userInfo, new Date()).isAuthorized());
	}

	private void createValidTeamSubmissionState() throws Exception {
		// team is registered
		when(mockChallengeTeamDAO.isTeamRegistered(
				Long.parseLong(CHALLENGE_ID), 
				Long.parseLong(SUBMITTING_TEAM_ID))).
			thenReturn(true);
		// add a user to submitting team
		submittingTeamMembers.add(createUserGroup(SUBMITTER_PRINCIPAL_ID));
		// user is registered for challenge
		challengeParticipants.add(createUserGroup(SUBMITTER_PRINCIPAL_ID));
	}
	
	@Test
	public void testGetTeamSubmissionEligibilityHappyCase() throws Exception {
		createValidTeamSubmissionState();
		
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
		assertEquals(1, membersEligibility.size());
		MemberSubmissionEligibility mse = membersEligibility.get(0);
		assertEquals(new Long(SUBMITTER_PRINCIPAL_ID), mse.getPrincipalId());
		assertTrue(mse.getIsRegistered());
		assertFalse(mse.getIsQuotaFilled());
		assertFalse(mse.getHasConflictingSubmission());
		assertTrue(mse.getIsEligible());
	}
	
	@Test
	public void testGetTeamSubmissionEligibilityNoQuota() throws Exception {
		createValidTeamSubmissionState();
		evaluation.setQuota(null);
		
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
		assertEquals(1, membersEligibility.size());
		MemberSubmissionEligibility mse = membersEligibility.get(0);
		assertEquals(new Long(SUBMITTER_PRINCIPAL_ID), mse.getPrincipalId());
		assertTrue(mse.getIsRegistered());
		assertFalse(mse.getIsQuotaFilled());
		assertFalse(mse.getHasConflictingSubmission());
		assertTrue(mse.getIsEligible());
	}
	
	// here we test both that the team and the member are unregistered
	@Test
	public void testGetTeamSubmissionEligibilityUnregistered() throws Exception {
		// add a user to submitting team
		submittingTeamMembers.add(createUserGroup(SUBMITTER_PRINCIPAL_ID));
		
		TeamSubmissionEligibility tse = submissionEligibilityManager.
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID);
		
		assertEquals(EVAL_ID, tse.getEvaluationId());
		assertEquals(SUBMITTING_TEAM_ID, tse.getTeamId());
		assertNotNull(tse.getEligibilityStateHash());
		
		SubmissionEligibility teamEligibility = tse.getTeamEligibility();
		assertFalse(teamEligibility.getIsRegistered());
		assertFalse(teamEligibility.getIsEligible());
		assertFalse(teamEligibility.getIsQuotaFilled());
		
		List<MemberSubmissionEligibility> membersEligibility = tse.getMembersEligibility();
		assertEquals(1, membersEligibility.size());
		MemberSubmissionEligibility mse = membersEligibility.get(0);
		assertEquals(new Long(SUBMITTER_PRINCIPAL_ID), mse.getPrincipalId());
		assertFalse(mse.getIsRegistered());
		assertFalse(mse.getIsQuotaFilled());
		assertFalse(mse.getHasConflictingSubmission());
		assertFalse(mse.getIsEligible());
	}
		
	@Test
	public void testGetTeamSubmissionEligibilityNoChallenge() throws Exception {
		// no challenge for evaluation
		when(mockChallengeDAO.getForProject(CHALLENGE_PROJECT_ID)).thenThrow(new NotFoundException());
		// add a user to submitting team
		submittingTeamMembers.add(createUserGroup(SUBMITTER_PRINCIPAL_ID));
		
		TeamSubmissionEligibility tse = submissionEligibilityManager.
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID);
		
		assertEquals(EVAL_ID, tse.getEvaluationId());
		assertEquals(SUBMITTING_TEAM_ID, tse.getTeamId());
		assertNotNull(tse.getEligibilityStateHash());
		
		SubmissionEligibility teamEligibility = tse.getTeamEligibility();
		assertNull(teamEligibility.getIsRegistered());
		assertTrue(teamEligibility.getIsEligible());
		assertFalse(teamEligibility.getIsQuotaFilled());
		
		List<MemberSubmissionEligibility> membersEligibility = tse.getMembersEligibility();
		assertEquals(1, membersEligibility.size());
		MemberSubmissionEligibility mse = membersEligibility.get(0);
		assertEquals(new Long(SUBMITTER_PRINCIPAL_ID), mse.getPrincipalId());
		assertNull(mse.getIsRegistered());
		assertFalse(mse.getIsQuotaFilled());
		assertFalse(mse.getHasConflictingSubmission());
		assertTrue(mse.getIsEligible());
	}
	
	@Test
	public void testGetTeamSubmissionEligibilityQuotaFilled() throws Exception {
		// team is registered
		when(mockChallengeTeamDAO.isTeamRegistered(
				Long.parseLong(CHALLENGE_ID), 
				Long.parseLong(SUBMITTING_TEAM_ID))).
			thenReturn(true);
		// add a user to submitting team
		submittingTeamMembers.add(createUserGroup(SUBMITTER_PRINCIPAL_ID));
		// user is registered for challenge
		challengeParticipants.add(createUserGroup(SUBMITTER_PRINCIPAL_ID));
		when(mockSubmissionDAO.countSubmissionsByTeam(eq(Long.parseLong(EVAL_ID)),eq(Long.parseLong(SUBMITTING_TEAM_ID)), 
				any(Date.class), any(Date.class), 
				eq(STATUSES_COUNTED_TOWARD_QUOTA))).thenReturn(MAX_SUBMISSIONS_PER_ROUND+1L);
		
		Map<Long,Long> memberSubmissionCounts = new HashMap<Long,Long>();
		memberSubmissionCounts.put(new Long(SUBMITTER_PRINCIPAL_ID), MAX_SUBMISSIONS_PER_ROUND+1L);
		when(mockSubmissionDAO.countSubmissionsByTeamMembers(eq(Long.parseLong(EVAL_ID)),eq(Long.parseLong(SUBMITTING_TEAM_ID)), 
				any(Date.class), any(Date.class), 
				eq(STATUSES_COUNTED_TOWARD_QUOTA))).thenReturn(memberSubmissionCounts);
		
		TeamSubmissionEligibility tse = submissionEligibilityManager.
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID);
		
		assertEquals(EVAL_ID, tse.getEvaluationId());
		assertEquals(SUBMITTING_TEAM_ID, tse.getTeamId());
		assertNotNull(tse.getEligibilityStateHash());
		
		SubmissionEligibility teamEligibility = tse.getTeamEligibility();
		assertTrue(teamEligibility.getIsRegistered());
		assertFalse(teamEligibility.getIsEligible());
		assertTrue(teamEligibility.getIsQuotaFilled());
		
		List<MemberSubmissionEligibility> membersEligibility = tse.getMembersEligibility();
		assertEquals(1, membersEligibility.size());
		MemberSubmissionEligibility mse = membersEligibility.get(0);
		assertEquals(new Long(SUBMITTER_PRINCIPAL_ID), mse.getPrincipalId());
		assertTrue(mse.getIsRegistered());
		assertTrue(mse.getIsQuotaFilled());
		assertFalse(mse.getHasConflictingSubmission());
		assertFalse(mse.getIsEligible());
	}
	
	@Test
	public void testGetTeamSubmissionEligibilityConflictingSubmission() throws Exception {
		// no challenge for evaluation
		when(mockChallengeDAO.getForProject(CHALLENGE_PROJECT_ID)).thenThrow(new NotFoundException());
		// add a user to submitting team
		submittingTeamMembers.add(createUserGroup(SUBMITTER_PRINCIPAL_ID));
		
		when(mockSubmissionDAO.getTeamMembersSubmittingElsewhere(eq(Long.parseLong(EVAL_ID)),
				eq(Long.parseLong(SUBMITTING_TEAM_ID)), 
				any(Date.class), any(Date.class), 
				eq(STATUSES_COUNTED_TOWARD_QUOTA))).
				thenReturn(Collections.singletonList(Long.parseLong(SUBMITTER_PRINCIPAL_ID)));
		
		TeamSubmissionEligibility tse = submissionEligibilityManager.
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID);
				
		List<MemberSubmissionEligibility> membersEligibility = tse.getMembersEligibility();
		assertEquals(1, membersEligibility.size());
		MemberSubmissionEligibility mse = membersEligibility.get(0);
		assertEquals(new Long(SUBMITTER_PRINCIPAL_ID), mse.getPrincipalId());
		assertNull(mse.getIsRegistered());
		assertFalse(mse.getIsQuotaFilled());
		assertTrue(mse.getHasConflictingSubmission());
		assertFalse(mse.getIsEligible());
	}
	
	
	@Test
	public void testIsTeamEligibleHappyCase() throws Exception {
		createValidTeamSubmissionState();
		TeamSubmissionEligibility tse = submissionEligibilityManager.
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID);
		int tseHash = SubmissionEligibilityManagerImpl.computeTeamSubmissionEligibilityHash(tse);
		
		assertTrue(submissionEligibilityManager.
				isTeamEligible(EVAL_ID, 
						SUBMITTING_TEAM_ID, 
						Collections.singletonList(SUBMITTER_PRINCIPAL_ID), 
						""+tseHash, 
						new Date()).isAuthorized());
	}

	@Test
	public void testIsTeamEligibleOutsideSubmissionRange() throws Exception {
		createValidTeamSubmissionState();
		TeamSubmissionEligibility tse = submissionEligibilityManager.
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID);
		int tseHash = SubmissionEligibilityManagerImpl.computeTeamSubmissionEligibilityHash(tse);
		// start with happy case
		assertTrue(submissionEligibilityManager.
				isTeamEligible(EVAL_ID, 
						SUBMITTING_TEAM_ID, 
						Collections.singletonList(SUBMITTER_PRINCIPAL_ID), 
						""+tseHash, 
						new Date()).isAuthorized());
		// ... but if we're outside the allowed submission interval then submission isn't allowed
		long longTimeAgo = System.currentTimeMillis()-1000000L;
		assertFalse(submissionEligibilityManager.
						isTeamEligible(EVAL_ID, 
								SUBMITTING_TEAM_ID, 
								Collections.singletonList(SUBMITTER_PRINCIPAL_ID), 
								""+tseHash, 
								new Date(longTimeAgo)).isAuthorized());
	}

	
	@Test
	public void testIsTeamEligibleIncorrectHash() throws Exception {
		createValidTeamSubmissionState();
		TeamSubmissionEligibility tse = submissionEligibilityManager.
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID);
		int tseHash = SubmissionEligibilityManagerImpl.computeTeamSubmissionEligibilityHash(tse);
		// start with happy case
		assertTrue(submissionEligibilityManager.
				isTeamEligible(EVAL_ID, 
						SUBMITTING_TEAM_ID, 
						Collections.singletonList(SUBMITTER_PRINCIPAL_ID), 
						""+tseHash, 
						new Date()).isAuthorized());
		// but if the hash is missing then submission isn't allowed
		assertFalse(submissionEligibilityManager.
				isTeamEligible(EVAL_ID, 
						SUBMITTING_TEAM_ID, 
						Collections.singletonList(SUBMITTER_PRINCIPAL_ID), 
						null, 
						new Date()).isAuthorized());
		// ditto for an incorrect hash
		assertFalse(submissionEligibilityManager.
				isTeamEligible(EVAL_ID, 
						SUBMITTING_TEAM_ID, 
						Collections.singletonList(SUBMITTER_PRINCIPAL_ID), 
						"gobbldygook", 
						new Date()).isAuthorized());
		
		assertFalse(submissionEligibilityManager.
				isTeamEligible(EVAL_ID, 
						SUBMITTING_TEAM_ID, 
						Collections.singletonList(SUBMITTER_PRINCIPAL_ID), 
						""+(tseHash+1), 
						new Date()).isAuthorized());
	}

	@Test
	public void testIsTeamEligibleTeamIsIneligible() throws Exception {
		createValidTeamSubmissionState();
		TeamSubmissionEligibility tse = submissionEligibilityManager.
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID);
		int tseHash = SubmissionEligibilityManagerImpl.computeTeamSubmissionEligibilityHash(tse);
		// start with happy case
		assertTrue(submissionEligibilityManager.
				isTeamEligible(EVAL_ID, 
						SUBMITTING_TEAM_ID, 
						Collections.singletonList(SUBMITTER_PRINCIPAL_ID), 
						""+tseHash, 
						new Date()).isAuthorized());
		// ... but if the team is unregistered it is no longer eligible
		when(mockChallengeTeamDAO.isTeamRegistered(
				Long.parseLong(CHALLENGE_ID), 
				Long.parseLong(SUBMITTING_TEAM_ID))).
			thenReturn(false);
		tse = submissionEligibilityManager.
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID);
		tseHash = SubmissionEligibilityManagerImpl.computeTeamSubmissionEligibilityHash(tse);
		assertFalse(submissionEligibilityManager.
				isTeamEligible(EVAL_ID, 
						SUBMITTING_TEAM_ID, 
						Collections.singletonList(SUBMITTER_PRINCIPAL_ID), 
						""+tseHash, 
						new Date()).isAuthorized());
	}

	@Test
	public void testIsTeamEligibleMemberIsIneligible() throws Exception {
		createValidTeamSubmissionState();
		TeamSubmissionEligibility tse = submissionEligibilityManager.
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID);
		int tseHash = SubmissionEligibilityManagerImpl.computeTeamSubmissionEligibilityHash(tse);
		// start with happy case
		assertTrue(submissionEligibilityManager.
				isTeamEligible(EVAL_ID, 
						SUBMITTING_TEAM_ID, 
						Collections.singletonList(SUBMITTER_PRINCIPAL_ID), 
						""+tseHash, 
						new Date()).isAuthorized());
		// ... but if the contributor is unregistered she is no longer eligible
		challengeParticipants.clear();
		tse = submissionEligibilityManager.
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID);
		tseHash = SubmissionEligibilityManagerImpl.computeTeamSubmissionEligibilityHash(tse);
		assertFalse(submissionEligibilityManager.
				isTeamEligible(EVAL_ID, 
						SUBMITTING_TEAM_ID, 
						Collections.singletonList(SUBMITTER_PRINCIPAL_ID), 
						""+tseHash, 
						new Date()).isAuthorized());
	}

}
