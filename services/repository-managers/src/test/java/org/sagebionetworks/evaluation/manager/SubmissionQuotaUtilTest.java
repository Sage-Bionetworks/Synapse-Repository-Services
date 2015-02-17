package org.sagebionetworks.evaluation.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.SubmissionQuota;
import org.sagebionetworks.util.Pair;

public class SubmissionQuotaUtilTest {
	
	private static void checkPairEquals(Pair<Date,Date> p1, Pair<Date,Date> p2) {
		assertEquals(p1.getFirst(), p2.getFirst());
		assertEquals(p1.getSecond(), p2.getSecond());
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
		assertFalse(SubmissionQuotaUtil.isSubmissionAllowed(eval, now));
		try {
			SubmissionQuotaUtil.getRoundInterval(eval, now);
			fail("exception expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		
		// at the start of the round
		now = firstRoundStart;
		assertTrue(SubmissionQuotaUtil.isSubmissionAllowed(eval, now));
		checkPairEquals(new Pair<Date,Date>(firstRoundStart, new Date(firstRoundStart.getTime()+1000L)), 
				SubmissionQuotaUtil.getRoundInterval(eval, now));
		
		// beginning of second round
		now = new Date(firstRoundStart.getTime()+1000L);
		assertTrue(SubmissionQuotaUtil.isSubmissionAllowed(eval, now));
		checkPairEquals(new Pair<Date,Date>(
				new Date(firstRoundStart.getTime()+1000L), 
					new Date(firstRoundStart.getTime()+2000L)), 
				SubmissionQuotaUtil.getRoundInterval(eval, now));
		
		// middle of second round
		now = new Date(firstRoundStart.getTime()+1500L);
		assertTrue(SubmissionQuotaUtil.isSubmissionAllowed(eval, now));
		checkPairEquals(new Pair<Date,Date>(
				new Date(firstRoundStart.getTime()+1000L), 
					new Date(firstRoundStart.getTime()+2000L)), 
				SubmissionQuotaUtil.getRoundInterval(eval, now));
		
		
		// end of the last round is considered beyond the end of the rounds
		now = new Date(firstRoundStart.getTime()+2000L);
		assertFalse(SubmissionQuotaUtil.isSubmissionAllowed(eval, now));
		try {
			SubmissionQuotaUtil.getRoundInterval(eval, now);
			fail("exception expected");
		} catch (IllegalArgumentException e) {
			// as expected
		}
		
		// if the challenge never ends, then it just keeps going
		quota.setNumberOfRounds(null);
		now = new Date(firstRoundStart.getTime()+2000L);
		assertTrue(SubmissionQuotaUtil.isSubmissionAllowed(eval, now));
		checkPairEquals(new Pair<Date,Date>(
				new Date(firstRoundStart.getTime()+2000L), 
					new Date(firstRoundStart.getTime()+3000L)), 
				SubmissionQuotaUtil.getRoundInterval(eval, now));
	}
	
	@Test
	public void testWithoutRounds() {
		Evaluation eval = new Evaluation();
		SubmissionQuota quota = new SubmissionQuota();
		eval.setQuota(quota);
		quota.setSubmissionLimit(10L);

		assertTrue(SubmissionQuotaUtil.isSubmissionAllowed(eval, new Date()));
		checkPairEquals(new Pair<Date,Date>(null, null), 
				SubmissionQuotaUtil.getRoundInterval(eval, new Date()));
	}

}
