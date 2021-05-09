package org.sagebionetworks.repo.manager.evaluation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.evaluation.SubmissionEligibilityManagerImpl.STATUSES_COUNTED_TOWARD_QUOTA;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.checkerframework.checker.nullness.Opt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.evaluation.dao.SubmissionUtils;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationRound;
import org.sagebionetworks.evaluation.model.EvaluationRoundLimit;
import org.sagebionetworks.evaluation.model.EvaluationRoundLimitType;
import org.sagebionetworks.evaluation.model.MemberSubmissionEligibility;
import org.sagebionetworks.evaluation.model.SubmissionEligibility;
import org.sagebionetworks.evaluation.model.SubmissionQuota;
import org.sagebionetworks.evaluation.model.TeamSubmissionEligibility;
import org.sagebionetworks.repo.manager.evaluation.SubmissionEligibilityManagerImpl;
import org.sagebionetworks.repo.manager.evaluation.SubmissionQuotaUtil;
import org.sagebionetworks.repo.model.Challenge;
import org.sagebionetworks.repo.model.ChallengeDAO;
import org.sagebionetworks.repo.model.ChallengeTeamDAO;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class SubmissionEligibilityManagerTest {

	@InjectMocks
	SubmissionEligibilityManagerImpl submissionEligibilityManager;
	@Mock
	private EvaluationDAO mockEvaluationDAO;
	@Mock
	private SubmissionDAO mockSubmissionDAO;
	@Mock
	private ChallengeDAO mockChallengeDAO;
	@Mock
	private ChallengeTeamDAO mockChallengeTeamDAO;
	@Mock
	private GroupMembersDAO mockGroupMembersDAO;
	private Evaluation evaluation;
	private Challenge challenge;
	private List<UserGroup> challengeParticipants;
	private List<UserGroup> submittingTeamMembers;
	private UserInfo userInfo;

	private Date roundStart;
	private Date roundEnd;
	private EvaluationRound evaluationRound;
	private static final String EVAL_ID = "100";
	private static final String CHALLENGE_ID = "200";
	private static final String SUBMITTER_PRINCIPAL_ID = "300";
	private static final String CHALLENGE_PROJECT_ID = "syn400";
	private static final String CHALLENGE_PARTICIPANT_TEAM_ID = "500";
	private static final String SUBMITTING_TEAM_ID = "600";
	private static final long MAX_SUBMISSIONS_PER_ROUND = 5L;

	private long dailyLimitCount;
	private long weeklyLimitCount;
	private long monthlyLimitCount;
	private EvaluationRoundLimit dailyLimit;
	private EvaluationRoundLimit weeklyLimit;
	private EvaluationRoundLimit monthlyLimit;
	private Date now;
	private Date dailyStart;
	private Date weeklyStart;
	private Date monthlyStart;




	@BeforeEach
	public void setUp() throws Exception {
		now = Date.from(LocalDateTime.parse("2019-09-20T23:59:59").toInstant(ZoneOffset.UTC));


		evaluation = new Evaluation();
		evaluation.setId(EVAL_ID);
		evaluation.setContentSource(CHALLENGE_PROJECT_ID);
		SubmissionQuota quota = new SubmissionQuota();
		quota.setFirstRoundStart(new Date(now.getTime()-10000L));
		quota.setNumberOfRounds(10L);
		quota.setRoundDurationMillis(10000L);
		quota.setSubmissionLimit(MAX_SUBMISSIONS_PER_ROUND);
		evaluation.setQuota(quota);
		lenient().when(mockEvaluationDAO.get(EVAL_ID)).thenReturn(evaluation);
		
		challenge = new Challenge();
		challenge.setId(CHALLENGE_ID);
		challenge.setProjectId(CHALLENGE_PROJECT_ID);
		challenge.setParticipantTeamId(CHALLENGE_PARTICIPANT_TEAM_ID);
		lenient().when(mockChallengeDAO.getForProject(CHALLENGE_PROJECT_ID)).thenReturn(challenge);
		
		challengeParticipants = new ArrayList<UserGroup>();
		lenient().when(mockGroupMembersDAO.getMembers(CHALLENGE_PARTICIPANT_TEAM_ID)).thenReturn(challengeParticipants);
		
		submittingTeamMembers = new ArrayList<UserGroup>();
		lenient().when(mockGroupMembersDAO.getMembers(SUBMITTING_TEAM_ID)).thenReturn(submittingTeamMembers);
		
		userInfo = new UserInfo(false);
		userInfo.setId(Long.parseLong(SUBMITTER_PRINCIPAL_ID));
		userInfo.setGroups(Collections.singleton(Long.parseLong(CHALLENGE_PARTICIPANT_TEAM_ID)));

		roundStart = now;
		roundEnd = new Date(now.getTime() + 123123123);
		evaluationRound = new EvaluationRound();
		evaluationRound.setRoundStart(roundStart);
		evaluationRound.setRoundEnd(roundEnd);

		monthlyLimitCount = 5L;
		monthlyLimit = new EvaluationRoundLimit();
		monthlyLimit.setLimitType(EvaluationRoundLimitType.MONTHLY);
		monthlyLimit.setMaximumSubmissions(monthlyLimitCount);

		weeklyLimitCount = 2L;
		weeklyLimit = new EvaluationRoundLimit();
		weeklyLimit.setMaximumSubmissions(weeklyLimitCount);
		weeklyLimit.setLimitType(EvaluationRoundLimitType.WEEKLY);

		dailyLimitCount = 3L;
		dailyLimit = new EvaluationRoundLimit();
		dailyLimit.setLimitType(EvaluationRoundLimitType.DAILY);
		dailyLimit.setMaximumSubmissions(dailyLimitCount);

		dailyStart = Date.from(LocalDateTime.parse("2019-09-20T00:00:00").toInstant(ZoneOffset.UTC));
		weeklyStart = Date.from(LocalDateTime.parse("2019-09-16T00:00:00").toInstant(ZoneOffset.UTC));
		monthlyStart = Date.from(LocalDateTime.parse("2019-09-01T00:00:00").toInstant(ZoneOffset.UTC));
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
			isIndividualEligible(EVAL_ID, userInfo, now).isAuthorized());
	}

	@Test
	public void testIsIndividualEligibleNotRegistered() throws Exception {
		// starts out as a happy case
		assertTrue(submissionEligibilityManager.
				isIndividualEligible(EVAL_ID, userInfo, now).isAuthorized());
		// but if you're not registered for the challenge you're ineligible to submit
		userInfo.setGroups(Collections.EMPTY_SET);
		assertFalse(submissionEligibilityManager.
				isIndividualEligible(EVAL_ID, userInfo, now).isAuthorized());
	}

	@Test
	public void testIsIndividualEligibleNoQuota() throws Exception {
		evaluation.setQuota(null);
		when(mockEvaluationDAO.hasEvaluationRounds(EVAL_ID)).thenReturn(true);
		when(mockEvaluationDAO.getEvaluationRoundForTimestamp(eq(evaluation.getId()), any())).thenReturn(Optional.of(evaluationRound));
		assertTrue(submissionEligibilityManager.
			isIndividualEligible(EVAL_ID, userInfo, now).isAuthorized());
	}
	@Test
	public void testIsIndividualEligibleNoQuota_NoCurrentEvaluationRound() throws Exception {
		evaluation.setQuota(null);
		when(mockEvaluationDAO.hasEvaluationRounds(EVAL_ID)).thenReturn(true);
		when(mockEvaluationDAO.getEvaluationRoundForTimestamp(eq(evaluation.getId()), any())).thenReturn(Optional.empty());
		assertFalse(submissionEligibilityManager.isIndividualEligible(EVAL_ID, userInfo, now).isAuthorized());
	}

	@Test
	public void testIsIndividualEligibleRoundsButNoQuota() throws Exception {
		evaluation.getQuota().setSubmissionLimit(null);
		assertTrue(submissionEligibilityManager.
			isIndividualEligible(EVAL_ID, userInfo, now).isAuthorized());
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
			isIndividualEligible(EVAL_ID, userInfo, now).isAuthorized());
	}

	@Test
	public void testIsIndividualEligible_MultipleLimits_NotExceeded() throws Exception {

		evaluationRound.setLimits(Arrays.asList(dailyLimit, weeklyLimit, monthlyLimit));

		when(mockSubmissionDAO.
				countSubmissionsByContributor(eq(Long.parseLong(EVAL_ID)),
						eq(Long.parseLong(SUBMITTER_PRINCIPAL_ID)),
						eq(dailyStart), eq(roundEnd),
						eq(STATUSES_COUNTED_TOWARD_QUOTA))).thenReturn(dailyLimitCount-1);
		when(mockSubmissionDAO.
				countSubmissionsByContributor(eq(Long.parseLong(EVAL_ID)),
						eq(Long.parseLong(SUBMITTER_PRINCIPAL_ID)),
						eq(weeklyStart), eq(roundEnd),
						eq(STATUSES_COUNTED_TOWARD_QUOTA))).thenReturn(weeklyLimitCount-1);
		when(mockSubmissionDAO.
				countSubmissionsByContributor(eq(Long.parseLong(EVAL_ID)),
						eq(Long.parseLong(SUBMITTER_PRINCIPAL_ID)),
						eq(monthlyStart), eq(roundEnd),
						eq(STATUSES_COUNTED_TOWARD_QUOTA))).thenReturn(monthlyLimitCount-1);

		evaluation.setQuota(null);
		when(mockEvaluationDAO.hasEvaluationRounds(EVAL_ID)).thenReturn(true);
		when(mockEvaluationDAO.getEvaluationRoundForTimestamp(evaluation.getId(), now.toInstant())).thenReturn(Optional.of(evaluationRound));

		// method under test
		assertTrue(submissionEligibilityManager.
				isIndividualEligible(EVAL_ID, userInfo, now).isAuthorized());

		verify(mockSubmissionDAO).
				countSubmissionsByContributor(eq(Long.parseLong(EVAL_ID)),
						eq(Long.parseLong(SUBMITTER_PRINCIPAL_ID)),
						eq(dailyStart), eq(roundEnd),
						eq(STATUSES_COUNTED_TOWARD_QUOTA));
		verify(mockSubmissionDAO).
				countSubmissionsByContributor(eq(Long.parseLong(EVAL_ID)),
						eq(Long.parseLong(SUBMITTER_PRINCIPAL_ID)),
						eq(weeklyStart), eq(roundEnd),
						eq(STATUSES_COUNTED_TOWARD_QUOTA));
		verify(mockSubmissionDAO).
				countSubmissionsByContributor(eq(Long.parseLong(EVAL_ID)),
						eq(Long.parseLong(SUBMITTER_PRINCIPAL_ID)),
						eq(monthlyStart), eq(roundEnd),
						eq(STATUSES_COUNTED_TOWARD_QUOTA));
	}

	@Test
	public void testIsIndividualEligible_MultipleLimits_Exceeded() throws Exception {
		evaluationRound.setLimits(Arrays.asList(dailyLimit, weeklyLimit, monthlyLimit));

		when(mockSubmissionDAO.
				countSubmissionsByContributor(eq(Long.parseLong(EVAL_ID)),
						eq(Long.parseLong(SUBMITTER_PRINCIPAL_ID)),
						eq(dailyStart), eq(roundEnd),
						eq(STATUSES_COUNTED_TOWARD_QUOTA))).thenReturn(dailyLimitCount-1);

		//make weekly limit exceeded
		when(mockSubmissionDAO.
				countSubmissionsByContributor(eq(Long.parseLong(EVAL_ID)),
						eq(Long.parseLong(SUBMITTER_PRINCIPAL_ID)),
						eq(weeklyStart), eq(roundEnd),
						eq(STATUSES_COUNTED_TOWARD_QUOTA))).thenReturn(weeklyLimitCount);

		evaluation.setQuota(null);
		when(mockEvaluationDAO.hasEvaluationRounds(EVAL_ID)).thenReturn(true);
		when(mockEvaluationDAO.getEvaluationRoundForTimestamp(evaluation.getId(), now.toInstant())).thenReturn(Optional.of(evaluationRound));

		// method under test
		assertFalse(submissionEligibilityManager.
				isIndividualEligible(EVAL_ID, userInfo, now).isAuthorized());

		verify(mockSubmissionDAO).
				countSubmissionsByContributor(eq(Long.parseLong(EVAL_ID)),
						eq(Long.parseLong(SUBMITTER_PRINCIPAL_ID)),
						eq(dailyStart), eq(roundEnd),
						eq(STATUSES_COUNTED_TOWARD_QUOTA));
		verify(mockSubmissionDAO).
				countSubmissionsByContributor(eq(Long.parseLong(EVAL_ID)),
						eq(Long.parseLong(SUBMITTER_PRINCIPAL_ID)),
						eq(weeklyStart), eq(roundEnd),
						eq(STATUSES_COUNTED_TOWARD_QUOTA));

		//verify never checked the monthly limit
		verify(mockSubmissionDAO, never()).
				countSubmissionsByContributor(eq(Long.parseLong(EVAL_ID)),
						eq(Long.parseLong(SUBMITTER_PRINCIPAL_ID)),
						eq(monthlyStart), eq(roundEnd),
						eq(STATUSES_COUNTED_TOWARD_QUOTA));
	}

	@Test
	public void testIsIndividualSubmittedToAnotherTeam() throws Exception {
		when(mockSubmissionDAO.hasContributedToTeamSubmission(eq(Long.parseLong(EVAL_ID)),
				eq(Long.parseLong(SUBMITTER_PRINCIPAL_ID)),
				any(Date.class), any(Date.class),
				eq(STATUSES_COUNTED_TOWARD_QUOTA))).thenReturn(true);
		assertFalse(submissionEligibilityManager.
			isIndividualEligible(EVAL_ID, userInfo, now).isAuthorized());

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
			isIndividualEligible(EVAL_ID, userInfo, now).isAuthorized());
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
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID, now);

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
	public void testGetTeamSubmissionEligibility_NoQuota_HasEvaluationRound_CurrentEvaluationRoundDefined() throws Exception {
		createValidTeamSubmissionState();
		evaluation.setQuota(null);
		when(mockEvaluationDAO.hasEvaluationRounds(EVAL_ID)).thenReturn(true);
		when(mockEvaluationDAO.getEvaluationRoundForTimestamp(eq(evaluation.getId()), any())).thenReturn(Optional.of(evaluationRound));

		TeamSubmissionEligibility tse = submissionEligibilityManager.
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID, now);

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
	public void testGetTeamSubmissionEligibility_NoQuota_HasEvaluationRound_CurrentEvaluationRoundNotDefined() throws Exception {
		createValidTeamSubmissionState();
		evaluation.setQuota(null);
		when(mockEvaluationDAO.hasEvaluationRounds(EVAL_ID)).thenReturn(true);
		when(mockEvaluationDAO.getEvaluationRoundForTimestamp(any(), any())).thenReturn(Optional.empty());

		String message = assertThrows(IllegalArgumentException.class, () ->
				submissionEligibilityManager.getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID, now)
		).getMessage();

		assertEquals("The given date is outside the time range allowed for submissions.", message);

	}

	// here we test both that the team and the member are unregistered
	@Test
	public void testGetTeamSubmissionEligibilityUnregistered() throws Exception {
		// add a user to submitting team
		submittingTeamMembers.add(createUserGroup(SUBMITTER_PRINCIPAL_ID));

		TeamSubmissionEligibility tse = submissionEligibilityManager.
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID, now);

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
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID, now);

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
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID, now);

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
	public void testGetTeamSubmissionEligibility_MultipleSubmissionLimits_exceededOneLimit() throws Exception {
		// team is registered
		when(mockChallengeTeamDAO.isTeamRegistered(
				Long.parseLong(CHALLENGE_ID),
				Long.parseLong(SUBMITTING_TEAM_ID))).
				thenReturn(true);
		// add a user to submitting team
		submittingTeamMembers.add(createUserGroup(SUBMITTER_PRINCIPAL_ID));
		// user is registered for challenge
		challengeParticipants.add(createUserGroup(SUBMITTER_PRINCIPAL_ID));

		evaluation.setQuota(null);
		evaluationRound.setLimits(Arrays.asList(dailyLimit, weeklyLimit, monthlyLimit));
		when(mockEvaluationDAO.hasEvaluationRounds(EVAL_ID)).thenReturn(true);
		when(mockEvaluationDAO.getEvaluationRoundForTimestamp(eq(EVAL_ID), any(Instant.class))).thenReturn(Optional.of(evaluationRound));


		when(mockSubmissionDAO.countSubmissionsByTeam(eq(Long.parseLong(EVAL_ID)),eq(Long.parseLong(SUBMITTING_TEAM_ID)),
				eq(dailyStart), eq(roundEnd),
				eq(STATUSES_COUNTED_TOWARD_QUOTA))).thenReturn(dailyLimitCount-1);
		//exceeds the weekly limit
		when(mockSubmissionDAO.countSubmissionsByTeam(eq(Long.parseLong(EVAL_ID)),eq(Long.parseLong(SUBMITTING_TEAM_ID)),
				eq(weeklyStart), eq(roundEnd),
				eq(STATUSES_COUNTED_TOWARD_QUOTA))).thenReturn(weeklyLimitCount);

		Map<Long,Long> dailyMemberSubmissionCounts = Collections.singletonMap(new Long(SUBMITTER_PRINCIPAL_ID), dailyLimitCount-1);
		when(mockSubmissionDAO.countSubmissionsByTeamMembers(Long.parseLong(EVAL_ID),Long.parseLong(SUBMITTING_TEAM_ID),
				dailyStart, roundEnd,
				STATUSES_COUNTED_TOWARD_QUOTA)).thenReturn(dailyMemberSubmissionCounts);

		Map<Long,Long> weeklyMemberSubmissionCounts = Collections.singletonMap(new Long(SUBMITTER_PRINCIPAL_ID), weeklyLimitCount);
		when(mockSubmissionDAO.countSubmissionsByTeamMembers(Long.parseLong(EVAL_ID),Long.parseLong(SUBMITTING_TEAM_ID),
				weeklyStart, roundEnd,
				STATUSES_COUNTED_TOWARD_QUOTA)).thenReturn(weeklyMemberSubmissionCounts);

		Map<Long,Long> monthlyMemberSubmissionCounts = Collections.singletonMap(new Long(SUBMITTER_PRINCIPAL_ID), monthlyLimitCount-1);
		when(mockSubmissionDAO.countSubmissionsByTeamMembers(Long.parseLong(EVAL_ID),Long.parseLong(SUBMITTING_TEAM_ID),
				monthlyStart, roundEnd,
				STATUSES_COUNTED_TOWARD_QUOTA)).thenReturn(monthlyMemberSubmissionCounts);

		// method under test
		TeamSubmissionEligibility tse = submissionEligibilityManager.
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID, now);

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
	public void testGetTeamSubmissionEligibility_MultipleSubmissionLimits_noneExceeded() throws Exception {
		// team is registered
		when(mockChallengeTeamDAO.isTeamRegistered(
				Long.parseLong(CHALLENGE_ID),
				Long.parseLong(SUBMITTING_TEAM_ID))).
				thenReturn(true);
		// add a user to submitting team
		submittingTeamMembers.add(createUserGroup(SUBMITTER_PRINCIPAL_ID));
		// user is registered for challenge
		challengeParticipants.add(createUserGroup(SUBMITTER_PRINCIPAL_ID));

		evaluation.setQuota(null);
		evaluationRound.setLimits(Arrays.asList(dailyLimit, weeklyLimit, monthlyLimit));
		when(mockEvaluationDAO.hasEvaluationRounds(EVAL_ID)).thenReturn(true);
		when(mockEvaluationDAO.getEvaluationRoundForTimestamp(eq(EVAL_ID), any(Instant.class))).thenReturn(Optional.of(evaluationRound));


		when(mockSubmissionDAO.countSubmissionsByTeam(eq(Long.parseLong(EVAL_ID)),eq(Long.parseLong(SUBMITTING_TEAM_ID)),
				eq(dailyStart), eq(roundEnd),
				eq(STATUSES_COUNTED_TOWARD_QUOTA))).thenReturn(dailyLimitCount-1);
		when(mockSubmissionDAO.countSubmissionsByTeam(eq(Long.parseLong(EVAL_ID)),eq(Long.parseLong(SUBMITTING_TEAM_ID)),
				eq(weeklyStart), eq(roundEnd),
				eq(STATUSES_COUNTED_TOWARD_QUOTA))).thenReturn(weeklyLimitCount-1);
		when(mockSubmissionDAO.countSubmissionsByTeam(eq(Long.parseLong(EVAL_ID)),eq(Long.parseLong(SUBMITTING_TEAM_ID)),
				eq(monthlyStart), eq(roundEnd),
				eq(STATUSES_COUNTED_TOWARD_QUOTA))).thenReturn(monthlyLimitCount-1);

		Map<Long,Long> dailyMemberSubmissionCounts = Collections.singletonMap(new Long(SUBMITTER_PRINCIPAL_ID), dailyLimitCount-1);
		when(mockSubmissionDAO.countSubmissionsByTeamMembers(Long.parseLong(EVAL_ID),Long.parseLong(SUBMITTING_TEAM_ID),
				dailyStart, roundEnd,
				STATUSES_COUNTED_TOWARD_QUOTA)).thenReturn(dailyMemberSubmissionCounts);

		Map<Long,Long> weeklyMemberSubmissionCounts = Collections.singletonMap(new Long(SUBMITTER_PRINCIPAL_ID), weeklyLimitCount-1);
		when(mockSubmissionDAO.countSubmissionsByTeamMembers(Long.parseLong(EVAL_ID),Long.parseLong(SUBMITTING_TEAM_ID),
				weeklyStart, roundEnd,
				STATUSES_COUNTED_TOWARD_QUOTA)).thenReturn(weeklyMemberSubmissionCounts);

		Map<Long,Long> monthlyMemberSubmissionCounts = Collections.singletonMap(new Long(SUBMITTER_PRINCIPAL_ID), monthlyLimitCount-1);
		when(mockSubmissionDAO.countSubmissionsByTeamMembers(Long.parseLong(EVAL_ID),Long.parseLong(SUBMITTING_TEAM_ID),
				monthlyStart, roundEnd,
				STATUSES_COUNTED_TOWARD_QUOTA)).thenReturn(monthlyMemberSubmissionCounts);

		// method under test
		TeamSubmissionEligibility tse = submissionEligibilityManager.
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID, now);

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
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID, now);

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
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID, now);
		int tseHash = SubmissionEligibilityManagerImpl.computeTeamSubmissionEligibilityHash(tse);

		assertTrue(submissionEligibilityManager.
				isTeamEligible(EVAL_ID,
						SUBMITTING_TEAM_ID,
						Collections.singletonList(SUBMITTER_PRINCIPAL_ID),
						""+tseHash,
						now).isAuthorized());
	}

	@Test
	public void testIsTeamEligibleOutsideSubmissionRange() throws Exception {
		createValidTeamSubmissionState();
		TeamSubmissionEligibility tse = submissionEligibilityManager.
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID, now);
		int tseHash = SubmissionEligibilityManagerImpl.computeTeamSubmissionEligibilityHash(tse);
		// start with happy case
		assertTrue(submissionEligibilityManager.
				isTeamEligible(EVAL_ID,
						SUBMITTING_TEAM_ID,
						Collections.singletonList(SUBMITTER_PRINCIPAL_ID),
						""+tseHash,
						now).isAuthorized());
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
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID, now);
		int tseHash = SubmissionEligibilityManagerImpl.computeTeamSubmissionEligibilityHash(tse);
		// start with happy case
		assertTrue(submissionEligibilityManager.
				isTeamEligible(EVAL_ID,
						SUBMITTING_TEAM_ID,
						Collections.singletonList(SUBMITTER_PRINCIPAL_ID),
						""+tseHash,
						now).isAuthorized());
		// but if the hash is missing then submission isn't allowed
		assertFalse(submissionEligibilityManager.
				isTeamEligible(EVAL_ID,
						SUBMITTING_TEAM_ID,
						Collections.singletonList(SUBMITTER_PRINCIPAL_ID),
						null,
						now).isAuthorized());
		// ditto for an incorrect hash
		assertFalse(submissionEligibilityManager.
				isTeamEligible(EVAL_ID,
						SUBMITTING_TEAM_ID,
						Collections.singletonList(SUBMITTER_PRINCIPAL_ID),
						"gobbldygook",
						now).isAuthorized());

		assertFalse(submissionEligibilityManager.
				isTeamEligible(EVAL_ID,
						SUBMITTING_TEAM_ID,
						Collections.singletonList(SUBMITTER_PRINCIPAL_ID),
						""+(tseHash+1),
						now).isAuthorized());
	}

	@Test
	public void testIsTeamEligibleTeamIsIneligible() throws Exception {
		createValidTeamSubmissionState();
		TeamSubmissionEligibility tse = submissionEligibilityManager.
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID, now);
		int tseHash = SubmissionEligibilityManagerImpl.computeTeamSubmissionEligibilityHash(tse);
		// start with happy case
		assertTrue(submissionEligibilityManager.
				isTeamEligible(EVAL_ID,
						SUBMITTING_TEAM_ID,
						Collections.singletonList(SUBMITTER_PRINCIPAL_ID),
						""+tseHash,
						now).isAuthorized());
		// ... but if the team is unregistered it is no longer eligible
		when(mockChallengeTeamDAO.isTeamRegistered(
				Long.parseLong(CHALLENGE_ID),
				Long.parseLong(SUBMITTING_TEAM_ID))).
			thenReturn(false);
		tse = submissionEligibilityManager.
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID, now);
		tseHash = SubmissionEligibilityManagerImpl.computeTeamSubmissionEligibilityHash(tse);
		assertFalse(submissionEligibilityManager.
				isTeamEligible(EVAL_ID,
						SUBMITTING_TEAM_ID,
						Collections.singletonList(SUBMITTER_PRINCIPAL_ID),
						""+tseHash,
						now).isAuthorized());
	}

	@Test
	public void testIsTeamEligibleMemberIsIneligible() throws Exception {
		createValidTeamSubmissionState();
		TeamSubmissionEligibility tse = submissionEligibilityManager.
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID, now);
		int tseHash = SubmissionEligibilityManagerImpl.computeTeamSubmissionEligibilityHash(tse);
		// start with happy case
		assertTrue(submissionEligibilityManager.
				isTeamEligible(EVAL_ID,
						SUBMITTING_TEAM_ID,
						Collections.singletonList(SUBMITTER_PRINCIPAL_ID),
						""+tseHash,
						now).isAuthorized());
		// ... but if the contributor is unregistered she is no longer eligible
		challengeParticipants.clear();
		tse = submissionEligibilityManager.
				getTeamSubmissionEligibility(evaluation, SUBMITTING_TEAM_ID, now);
		tseHash = SubmissionEligibilityManagerImpl.computeTeamSubmissionEligibilityHash(tse);
		assertFalse(submissionEligibilityManager.
				isTeamEligible(EVAL_ID,
						SUBMITTING_TEAM_ID,
						Collections.singletonList(SUBMITTER_PRINCIPAL_ID),
						""+tseHash,
						now).isAuthorized());
	}

	@Test
	public void submissionCountStartDate_TOTAL(){
		Date start = submissionEligibilityManager.submissionCountStartDate(EvaluationRoundLimitType.TOTAL, roundStart, now);
		assertEquals(roundStart, start);
	}

	@Test
	public void submissionCountStartDate_DAILY(){
		Date now = Date.from(LocalDateTime.parse("2019-09-20T23:59:59").toInstant(ZoneOffset.UTC));

		Date start = submissionEligibilityManager.submissionCountStartDate(EvaluationRoundLimitType.DAILY, roundStart, now);

		Date expected = Date.from(LocalDateTime.parse("2019-09-20T00:00:00").toInstant(ZoneOffset.UTC));
		assertEquals(expected, start);
	}

	@Test
	public void submissionCountStartDate_WEEKLY_CurrentTimeOnMonday(){
		//this test ensures the previous week's monday is not returned if we are already on a monday
		Date now = Date.from(LocalDateTime.parse("2019-09-16T00:00:00").toInstant(ZoneOffset.UTC));

		Date start = submissionEligibilityManager.submissionCountStartDate(EvaluationRoundLimitType.WEEKLY, roundStart, now);

		Date expected = Date.from(LocalDateTime.parse("2019-09-16T00:00:00").toInstant(ZoneOffset.UTC));
		assertEquals(expected, start);
	}

	@Test
	public void submissionCountStartDate_WEEKLY_CurrentTimeNotOnMonday(){
		Date now = Date.from(LocalDateTime.parse("2019-09-20T23:59:59").toInstant(ZoneOffset.UTC));

		Date start = submissionEligibilityManager.submissionCountStartDate(EvaluationRoundLimitType.WEEKLY, roundStart, now);

		Date expected = Date.from(LocalDateTime.parse("2019-09-16T00:00:00").toInstant(ZoneOffset.UTC));
		assertEquals(expected, start);
	}

	@Test
	public void submissionCountStartDate_MONTHLY(){
		for( int month = 1; month <= 12; month++) {
			Date now = Date.from(LocalDateTime.parse(String.format("2019-%02d-20T23:59:59", month)).toInstant(ZoneOffset.UTC));

			Date start = submissionEligibilityManager.submissionCountStartDate(EvaluationRoundLimitType.MONTHLY, roundStart, now);

			Date expected = Date.from(LocalDateTime.parse(String.format("2019-%02d-01T00:00:00", month)).toInstant(ZoneOffset.UTC));
			assertEquals(expected, start);
		}
	}

	@Test
	public void submissionCountStartDate_ensureImplementation(){
		for(EvaluationRoundLimitType type : EvaluationRoundLimitType.values()) {
			assertDoesNotThrow( ()->
				submissionEligibilityManager.submissionCountStartDate(type,roundStart, now),
					"This test fails when a newly introduced EvaluationRoundLimitType is not handled"
			);
		}
	}

	@Test
	public void testGetCurrentEvaluationRoundOrConvertSubmissionQuota_hasEvaluationRoundsDefinedTrue(){
		when(mockEvaluationDAO.hasEvaluationRounds(evaluation.getId())).thenReturn(true);
		Optional<EvaluationRound> mockedReturn = Optional.of(new EvaluationRound());
		when(mockEvaluationDAO.getEvaluationRoundForTimestamp(evaluation.getId(), now.toInstant())).thenReturn(mockedReturn);

		Optional<EvaluationRound> result = submissionEligibilityManager.getCurrentEvaluationRoundOrConvertSubmissionQuota(evaluation, now);
		assertSame(mockedReturn, result);

		verify(mockEvaluationDAO).hasEvaluationRounds(evaluation.getId());
		verify(mockEvaluationDAO).getEvaluationRoundForTimestamp(evaluation.getId(), now.toInstant());
	}

	@Test
	public void testGetCurrentEvaluationRoundOrConvertSubmissionQuota_hasEvaluationRoundsDefinedFalse(){
		when(mockEvaluationDAO.hasEvaluationRounds(evaluation.getId())).thenReturn(false);

		Optional<EvaluationRound> result = submissionEligibilityManager.getCurrentEvaluationRoundOrConvertSubmissionQuota(evaluation, now);
		assertEquals(SubmissionQuotaUtil.convertToCurrentEvaluationRound(evaluation.getQuota(), now), result);

		verify(mockEvaluationDAO).hasEvaluationRounds(evaluation.getId());
		verify(mockEvaluationDAO,never()).getEvaluationRoundForTimestamp(any(), any());
	}
}
