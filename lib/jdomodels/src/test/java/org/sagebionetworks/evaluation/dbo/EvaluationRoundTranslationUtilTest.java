package org.sagebionetworks.evaluation.dbo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationRound;
import org.sagebionetworks.evaluation.model.EvaluationRoundLimit;
import org.sagebionetworks.evaluation.model.EvaluationRoundLimitType;
import org.sagebionetworks.evaluation.model.SubmissionQuota;

class EvaluationRoundTranslationUtilTest {

	Evaluation evaluation;
	SubmissionQuota quota;

	String evaluationId;
	Date evaluationCreatedOn;

	Date firstRoundStart;
	long roundDurationMillis;
	long numberOfRounds;
	long submissionLimit;

	@BeforeEach
	void setUp() {
		evaluationId = "11223344";

		evaluation = new Evaluation();
		evaluation.setId(evaluationId);
		evaluationCreatedOn = new Date(2012, Calendar.DECEMBER, 21 );
		evaluation.setCreatedOn(evaluationCreatedOn);

		quota = new SubmissionQuota();
		firstRoundStart = new Date(2019, Calendar.JUNE, 9);
		quota.setFirstRoundStart(firstRoundStart);

		//5 days in millis
		roundDurationMillis = 432000000;
		quota.setRoundDurationMillis(roundDurationMillis);

		submissionLimit = 35;
		quota.setSubmissionLimit(submissionLimit);

		numberOfRounds = 4;
		quota.setNumberOfRounds(numberOfRounds);

		evaluation.setQuota(quota);
	}

	@Test
	public void testFromSubmissionQuota_NullQuota(){
		evaluation.setQuota(null);

		List<EvaluationRound> result = EvaluationRoundTranslationUtil.fromSubmissionQuota(evaluation);

		assertTrue(result.isEmpty());
	}

	@Test
	public void testFromSubmissionQuota_AllQuotaFieldsNull(){
		quota.setFirstRoundStart(null);
		quota.setNumberOfRounds(null);
		quota.setSubmissionLimit(null);
		quota.setRoundDurationMillis(null);

		List<EvaluationRound> result = EvaluationRoundTranslationUtil.fromSubmissionQuota(evaluation);

		assertTrue(result.isEmpty());
	}

	@Test
	public void testFromSubmissionQuota_FirstRoundStartNull_SubmissionLimitNonNull(){
		quota.setFirstRoundStart(null);

		List<EvaluationRound> result = EvaluationRoundTranslationUtil.fromSubmissionQuota(evaluation);


		EvaluationRound expected = new EvaluationRound();
		expected.setEvaluationId(evaluationId);
		expected.setRoundStart(evaluationCreatedOn);
		expected.setRoundEnd(EvaluationRoundTranslationUtil.FAR_FUTURE_DATE);
		EvaluationRoundLimit limit = new EvaluationRoundLimit();
		limit.setLimitType(EvaluationRoundLimitType.TOTAL);
		limit.setMaximumSubmissions(submissionLimit);
		expected.setLimits(Collections.singletonList(limit));

		assertEquals(Collections.singletonList(expected), result);
	}

	@Test
	public void testFromSubmissionQuota_FirstRoundStartNull_SubmissionLimitNull(){
		quota.setFirstRoundStart(null);
		quota.setSubmissionLimit(null);

		List<EvaluationRound> result = EvaluationRoundTranslationUtil.fromSubmissionQuota(evaluation);

		assertTrue(result.isEmpty());
	}

	@Test
	public void testFromSubmissionQuota_NumberOfRoundsNull_SubmissionLimitNonNull(){
		quota.setNumberOfRounds(null);

		List<EvaluationRound> result = EvaluationRoundTranslationUtil.fromSubmissionQuota(evaluation);


		EvaluationRound expected = new EvaluationRound();
		expected.setEvaluationId(evaluationId);
		expected.setRoundStart(firstRoundStart);
		expected.setRoundEnd(EvaluationRoundTranslationUtil.FAR_FUTURE_DATE);
		EvaluationRoundLimit limit = new EvaluationRoundLimit();
		limit.setLimitType(EvaluationRoundLimitType.TOTAL);
		limit.setMaximumSubmissions(submissionLimit);
		expected.setLimits(Collections.singletonList(limit));

		assertEquals(Collections.singletonList(expected), result);
	}

	@Test
	public void testFromSubmissionQuota_NumberOfRoundsNull_SubmissionLimitNull(){
		quota.setNumberOfRounds(null);
		quota.setSubmissionLimit(null);

		List<EvaluationRound> result = EvaluationRoundTranslationUtil.fromSubmissionQuota(evaluation);


		EvaluationRound expected = new EvaluationRound();
		expected.setEvaluationId(evaluationId);
		expected.setRoundStart(firstRoundStart);
		expected.setRoundEnd(EvaluationRoundTranslationUtil.FAR_FUTURE_DATE);

		assertEquals(Collections.singletonList(expected), result);
	}


	@Test
	public void testFromSubmissionQuota_roundDurationMillisNull_SubmissionLimitNonNull(){
		quota.setRoundDurationMillis(null);

		List<EvaluationRound> result = EvaluationRoundTranslationUtil.fromSubmissionQuota(evaluation);


		EvaluationRound expected = new EvaluationRound();
		expected.setEvaluationId(evaluationId);
		expected.setRoundStart(firstRoundStart);
		expected.setRoundEnd(EvaluationRoundTranslationUtil.FAR_FUTURE_DATE);
		EvaluationRoundLimit limit = new EvaluationRoundLimit();
		limit.setLimitType(EvaluationRoundLimitType.TOTAL);
		limit.setMaximumSubmissions(submissionLimit);
		expected.setLimits(Collections.singletonList(limit));

		assertEquals(Collections.singletonList(expected), result);
	}

	@Test
	public void testFromSubmissionQuota_roundDurationMillisNull_SubmissionLimitNull(){
		quota.setRoundDurationMillis(null);
		quota.setSubmissionLimit(null);

		List<EvaluationRound> result = EvaluationRoundTranslationUtil.fromSubmissionQuota(evaluation);


		EvaluationRound expected = new EvaluationRound();
		expected.setEvaluationId(evaluationId);
		expected.setRoundStart(firstRoundStart);
		expected.setRoundEnd(EvaluationRoundTranslationUtil.FAR_FUTURE_DATE);

		assertEquals(Collections.singletonList(expected), result);
	}


	@Test
	public void testFromSubmissionQuota_roundDurationIsOneDay(){
		quota.setRoundDurationMillis(1 * 24 * 60 * 60 * 1000L);

		List<EvaluationRound> result = EvaluationRoundTranslationUtil.fromSubmissionQuota(evaluation);

		//setup has 4 rounds and the round duration is now a single day => 4 days
		Date expectedEndDate = new Date(2019, Calendar.JUNE, 13);

		EvaluationRound expected = new EvaluationRound();
		expected.setEvaluationId(evaluationId);
		expected.setRoundStart(firstRoundStart);
		expected.setRoundEnd(expectedEndDate);
		EvaluationRoundLimit limit = new EvaluationRoundLimit();
		//limit should be DAILY instead of TOTAL
		limit.setLimitType(EvaluationRoundLimitType.DAILY);
		limit.setMaximumSubmissions(submissionLimit);
		expected.setLimits(Collections.singletonList(limit));

		//We shouldn't create multiple evaluations
		assertEquals(Collections.singletonList(expected), result);
	}

	@Test
	public void testFromSubmissionQuota_roundDurationIsOneWeek(){
		quota.setRoundDurationMillis(7 * 24 * 60 * 60 * 1000L);

		List<EvaluationRound> result = EvaluationRoundTranslationUtil.fromSubmissionQuota(evaluation);

		//setup has 4 rounds and the round duration is now a week => 4 weeks
		Date expectedEndDate = new Date(2019, Calendar.JULY, 7);

		EvaluationRound expected = new EvaluationRound();
		expected.setEvaluationId(evaluationId);
		expected.setRoundStart(firstRoundStart);
		expected.setRoundEnd(expectedEndDate);
		EvaluationRoundLimit limit = new EvaluationRoundLimit();
		//limit should be WEEKLY instead of TOTAL
		limit.setLimitType(EvaluationRoundLimitType.WEEKLY);
		limit.setMaximumSubmissions(submissionLimit);
		expected.setLimits(Collections.singletonList(limit));

		//We shouldn't create multiple evaluations
		assertEquals(Collections.singletonList(expected), result);
	}

	@Test
	public void testFromSubmissionQuota_roundDurationIsMonth_28Days(){
		quota.setRoundDurationMillis(28 * 24 * 60 * 60 * 1000L);

		List<EvaluationRound> result = EvaluationRoundTranslationUtil.fromSubmissionQuota(evaluation);

		//setup has 4 rounds and the round duration is now 28 days => 112 days
		Date expectedEndDate = new Date(2019, Calendar.SEPTEMBER, 29);

		EvaluationRound expected = new EvaluationRound();
		expected.setEvaluationId(evaluationId);
		expected.setRoundStart(firstRoundStart);
		expected.setRoundEnd(expectedEndDate);
		EvaluationRoundLimit limit = new EvaluationRoundLimit();
		//limit should be MONTHLY instead of TOTAL
		limit.setLimitType(EvaluationRoundLimitType.MONTHLY);
		limit.setMaximumSubmissions(submissionLimit);
		expected.setLimits(Collections.singletonList(limit));

		//We shouldn't create multiple evaluations
		assertEquals(Collections.singletonList(expected), result);
	}

	@Test
	public void testFromSubmissionQuota_roundDurationIsMonth_31Days(){
		quota.setRoundDurationMillis(31 * 24 * 60 * 60 * 1000L);

		List<EvaluationRound> result = EvaluationRoundTranslationUtil.fromSubmissionQuota(evaluation);

		//setup has 4 rounds and the round duration is now 28 days => 124 days
		Date expectedEndDate = new Date(2019, Calendar.OCTOBER, 11);

		EvaluationRound expected = new EvaluationRound();
		expected.setEvaluationId(evaluationId);
		expected.setRoundStart(firstRoundStart);
		expected.setRoundEnd(expectedEndDate);
		EvaluationRoundLimit limit = new EvaluationRoundLimit();
		//limit should be MONTHLY instead of TOTAL
		limit.setLimitType(EvaluationRoundLimitType.MONTHLY);
		limit.setMaximumSubmissions(submissionLimit);
		expected.setLimits(Collections.singletonList(limit));

		//We shouldn't create multiple evaluations
		assertEquals(Collections.singletonList(expected), result);
	}


	@Test
	public void testFromSubmissionQuota_roundDuration_regular_case(){
		List<EvaluationRound> result = EvaluationRoundTranslationUtil.fromSubmissionQuota(evaluation);


		List<EvaluationRound> expected = new ArrayList<>(4);

		EvaluationRoundLimit limit = new EvaluationRoundLimit();
		limit.setLimitType(EvaluationRoundLimitType.TOTAL);
		limit.setMaximumSubmissions(submissionLimit);

		EvaluationRound round1 = new EvaluationRound();
		round1.setEvaluationId(evaluationId);
		round1.setRoundStart(firstRoundStart);
		Date round1End = new Date(2019, Calendar.JUNE, 14);
		round1.setRoundEnd(round1End);
		round1.setLimits(Collections.singletonList(limit));
		expected.add(round1);

		EvaluationRound round2 = new EvaluationRound();
		round2.setEvaluationId(evaluationId);
		round2.setRoundStart(round1End);
		Date round2End = new Date(2019, Calendar.JUNE, 19);
		round2.setRoundEnd(round2End);
		round2.setLimits(Collections.singletonList(limit));
		expected.add(round2);

		EvaluationRound round3 = new EvaluationRound();
		round3.setEvaluationId(evaluationId);
		round3.setRoundStart(round2End);
		Date round3End = new Date(2019, Calendar.JUNE, 24);
		round3.setRoundEnd(round3End);
		round3.setLimits(Collections.singletonList(limit));
		expected.add(round3);

		EvaluationRound round4 = new EvaluationRound();
		round4.setEvaluationId(evaluationId);
		round4.setRoundStart(round3End);
		Date round4End = new Date(2019, Calendar.JUNE, 29);
		round4.setRoundEnd(round4End);
		round4.setLimits(Collections.singletonList(limit));
		expected.add(round4);


		//We shouldn't create multiple evaluations
		assertEquals(expected, result);
	}

}