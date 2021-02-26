package org.sagebionetworks.repo.manager.evaluation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import org.junit.Test;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationRound;
import org.sagebionetworks.evaluation.model.EvaluationRoundLimit;
import org.sagebionetworks.evaluation.model.EvaluationRoundLimitType;
import org.sagebionetworks.evaluation.model.SubmissionQuota;
import org.sagebionetworks.repo.manager.evaluation.SubmissionQuotaUtil;
import org.sagebionetworks.util.Pair;

public class SubmissionQuotaUtilTest {
	
	private static void checkPairEquals(Pair<Date,Date> p1, Pair<Date,Date> p2) {
		assertEquals(p1.getFirst(), p2.getFirst());
		assertEquals(p1.getSecond(), p2.getSecond());
	}
	
	@Test
	public void testNullQuota() throws Exception {
		Evaluation evaluation = new Evaluation();
		assertNull(SubmissionQuotaUtil.getSubmissionQuota(evaluation));
		SubmissionQuota submissionQuota = new SubmissionQuota();
		evaluation.setQuota(submissionQuota);
		assertNull(SubmissionQuotaUtil.getSubmissionQuota(evaluation));
		submissionQuota.setSubmissionLimit(10L);
		assertEquals(new Integer(10), SubmissionQuotaUtil.getSubmissionQuota(evaluation));
	}
	
	@Test
	public void testWithRounds() {
		Evaluation eval = new Evaluation();
		SubmissionQuota quota = new SubmissionQuota();
		eval.setQuota(quota);
		Date firstRoundStart = new Date();
		quota.setFirstRoundStart(firstRoundStart);
		quota.setNumberOfRounds(2L);
		quota.setRoundDurationMillis(1000L);
		quota.setSubmissionLimit(10L);
		
		assertEquals(10L, SubmissionQuotaUtil.getSubmissionQuota(eval).longValue());
		
				
		// before the start of the round
		Date now = new Date(0L);
		assertFalse(SubmissionQuotaUtil.isSubmissionAllowed(eval.getQuota(), now));
		try {
			SubmissionQuotaUtil.getRoundInterval(eval.getQuota(), now);
			fail("exception expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		
		// at the start of the round
		now = firstRoundStart;
		assertTrue(SubmissionQuotaUtil.isSubmissionAllowed(eval.getQuota(), now));
		checkPairEquals(new Pair<Date,Date>(firstRoundStart, new Date(firstRoundStart.getTime()+1000L)), 
				SubmissionQuotaUtil.getRoundInterval(eval.getQuota(), now));
		
		// beginning of second round
		now = new Date(firstRoundStart.getTime()+1000L);
		assertTrue(SubmissionQuotaUtil.isSubmissionAllowed(eval.getQuota(), now));
		checkPairEquals(new Pair<Date,Date>(
				new Date(firstRoundStart.getTime()+1000L), 
					new Date(firstRoundStart.getTime()+2000L)), 
				SubmissionQuotaUtil.getRoundInterval(eval.getQuota(), now));
		
		// middle of second round
		now = new Date(firstRoundStart.getTime()+1500L);
		assertTrue(SubmissionQuotaUtil.isSubmissionAllowed(eval.getQuota(), now));
		checkPairEquals(new Pair<Date,Date>(
				new Date(firstRoundStart.getTime()+1000L), 
					new Date(firstRoundStart.getTime()+2000L)), 
				SubmissionQuotaUtil.getRoundInterval(eval.getQuota(), now));
		
		
		// end of the last round is considered beyond the end of the rounds
		now = new Date(firstRoundStart.getTime()+2000L);
		assertFalse(SubmissionQuotaUtil.isSubmissionAllowed(eval.getQuota(), now));
		try {
			SubmissionQuotaUtil.getRoundInterval(eval.getQuota(), now);
			fail("exception expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		
		// if the challenge never ends, then it just keeps going
		quota.setNumberOfRounds(null);
		now = new Date(firstRoundStart.getTime()+2000L);
		assertTrue(SubmissionQuotaUtil.isSubmissionAllowed(eval.getQuota(), now));
		checkPairEquals(new Pair<Date,Date>(
				new Date(firstRoundStart.getTime()+2000L), 
					new Date(firstRoundStart.getTime()+3000L)), 
				SubmissionQuotaUtil.getRoundInterval(eval.getQuota(), now));
	}
	
	@Test
	public void testWithoutRounds() {
		Evaluation eval = new Evaluation();
		SubmissionQuota quota = new SubmissionQuota();
		eval.setQuota(quota);
		quota.setSubmissionLimit(10L);

		assertTrue(SubmissionQuotaUtil.isSubmissionAllowed(eval.getQuota(), new Date()));
		checkPairEquals(new Pair<Date,Date>(null, null), 
				SubmissionQuotaUtil.getRoundInterval(eval.getQuota(), new Date()));
	}

	@Test
	public void testConvertToCurrentEvaluationRound_null(){
		SubmissionQuota nullQuota = null;

		// method under test
		Optional<EvaluationRound> optionalEvaluationRound = SubmissionQuotaUtil.convertToCurrentEvaluationRound(nullQuota, new Date());
		assertTrue(optionalEvaluationRound.isPresent());

		//should convert into an empty EvaluationRound
		EvaluationRound expected = new EvaluationRound();

		assertEquals(expected, optionalEvaluationRound.get());
	}

	@Test
	public void testConvertToCurrentEvaluationRound_SubmissionNotAllowed(){
		SubmissionQuota quota = new SubmissionQuota();
		Instant now = Instant.now();
		quota.setFirstRoundStart(Date.from(now));
		long numRounds = 5;
		long roundDuration = 40L;
		quota.setNumberOfRounds(numRounds);
		quota.setRoundDurationMillis(roundDuration);

		Date roundAlreadyEnded = Date.from(now.plus(numRounds * roundDuration + 1, ChronoUnit.MILLIS));

		// method under test
		Optional<EvaluationRound> optionalEvaluationRound = SubmissionQuotaUtil.convertToCurrentEvaluationRound(quota, roundAlreadyEnded);
		assertFalse(optionalEvaluationRound.isPresent());
	}

	@Test
	public void testConvertToCurrentEvaluationRound_noSubmissionLimit(){
		SubmissionQuota quota = new SubmissionQuota();
		Instant now = Instant.now();
		quota.setFirstRoundStart(Date.from(now));
		long numRounds = 5;
		long roundDuration = 40L;
		quota.setNumberOfRounds(numRounds);
		quota.setRoundDurationMillis(roundDuration);

		Date thirdRound = Date.from(now.plus(2 * roundDuration + 1, ChronoUnit.MILLIS));

		// method under test
		Optional<EvaluationRound> optionalEvaluationRound = SubmissionQuotaUtil.convertToCurrentEvaluationRound(quota, thirdRound);
		assertTrue(optionalEvaluationRound.isPresent());

		EvaluationRound expected = new EvaluationRound();
		expected.setRoundStart(Date.from(now.plus(2 * roundDuration, ChronoUnit.MILLIS)));
		expected.setRoundEnd(Date.from(now.plus(3 * roundDuration, ChronoUnit.MILLIS)));
		expected.setLimits(null);

		assertEquals(expected, optionalEvaluationRound.get());
	}

	@Test
	public void testConvertToCurrentEvaluationRound_hasSubmissionLimit(){
		SubmissionQuota quota = new SubmissionQuota();
		Instant now = Instant.now();
		quota.setFirstRoundStart(Date.from(now));
		long numRounds = 5;
		long roundDuration = 40L;
		long submissionLimit = 35L;
		quota.setNumberOfRounds(numRounds);
		quota.setRoundDurationMillis(roundDuration);
		quota.setSubmissionLimit(submissionLimit);

		Date thirdRound = Date.from(now.plus(2 * roundDuration + 1, ChronoUnit.MILLIS));

		// method under test
		Optional<EvaluationRound> optionalEvaluationRound = SubmissionQuotaUtil.convertToCurrentEvaluationRound(quota, thirdRound);
		assertTrue(optionalEvaluationRound.isPresent());

		EvaluationRound expected = new EvaluationRound();
		expected.setRoundStart(Date.from(now.plus(2 * roundDuration, ChronoUnit.MILLIS)));
		expected.setRoundEnd(Date.from(now.plus(3 * roundDuration, ChronoUnit.MILLIS)));
		EvaluationRoundLimit expectedLimit = new EvaluationRoundLimit();
		expectedLimit.setMaximumSubmissions(submissionLimit);
		expectedLimit.setLimitType(EvaluationRoundLimitType.TOTAL);
		expected.setLimits(Collections.singletonList(expectedLimit));

		assertEquals(expected, optionalEvaluationRound.get());
	}
}
