package org.sagebionetworks.evaluation.manager;

import java.util.Date;
import java.util.List;

import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationQuotas;
import org.sagebionetworks.evaluation.model.SubmissionRound;
import org.sagebionetworks.util.Pair;

public class EvaluationQuotaUtil {

	/** 
	 * Returns the quota for the given time.
	 * If no quota is defined, then returns null.
	 * If a global quota is defined but no rounds are defined, then returns the global quota.
	 * If rounds are defined and if there is a specific quota defined for the
	 * round, then the value for that round is returned, else the global quota is returned.
	 * If rounds are defined and if the given date is outside the rounds, then
	 * returns zero.
	 * 
	 * @param evaluation
	 * @param now
	 * @return
	 */
	public static Integer getSubmissionQuota(Evaluation evaluation, Date now) {
		if (evaluation==null) throw new IllegalArgumentException("evaluation is required.");
		if (now==null) throw new IllegalArgumentException("current date is required.");
		EvaluationQuotas evaluationQuotas = evaluation.getQuotas();
		if (evaluationQuotas==null) return null;
		List<SubmissionRound> rounds = evaluationQuotas.getRounds();
		Long submissionLimit = evaluationQuotas.getSubmissionLimit();
		Date submissionStart = evaluationQuotas.getFirstRoundStart();
		if (submissionStart!=null && submissionStart.compareTo(now)>0) {
			// we are before the start date
			return 0;
		}
		if (rounds!=null) {
			// find the round whose end date is the soonest, but not prior to 'now'.
			// this is the current round
			SubmissionRound soonestEndDateAfterNow = null;
			for (SubmissionRound sr : rounds) {
				Date srEndDate = sr.getEndOfRound();
				if (submissionStart!=null && submissionStart.compareTo(srEndDate)>0)
					throw new IllegalStateException("Round end date cannot precede beginning of rounds.");
				if (srEndDate.compareTo(now)>=0 && 
						(soonestEndDateAfterNow==null || 
						srEndDate.compareTo(soonestEndDateAfterNow.getEndOfRound())<0)) {
					soonestEndDateAfterNow = sr;
				}
			}
			if (soonestEndDateAfterNow==null) return 0; // we are beyond the end of the challenge
			// if there's a round-specific limit, return it
			if (soonestEndDateAfterNow.getSubmissionLimit()!=null) {
				return soonestEndDateAfterNow.getSubmissionLimit().intValue();
			}
		}
		// return the global quota
		if (submissionLimit==null) throw new IllegalArgumentException("Submission limit is required.");
		return submissionLimit.intValue();
	}
	
	/*
	 * Given an Evaluation and a Date (time stamp), return the interval containing the date.
	 * Our convention is the the start of the interval is 'exclusive' of the returned
	 * date but the end of the interval is 'inclusive' of the returned date.  If the
	 * interval lacks a start or end date (i.e. is open ended on either or boths ends
	 * of the interval), then null is returned for the unbounded end.
	 * 
	 */
	public static Pair<Date, Date> getRoundInterval(Evaluation evaluation, Date now) {
		if (evaluation==null) throw new IllegalArgumentException("evaluation is required.");
		if (now==null) throw new IllegalArgumentException("current date is required.");
		Date start = null;
		Date end = null;
		Pair<Date,Date> result = new Pair<Date,Date>(start, end);
		EvaluationQuotas evaluationQuotas = evaluation.getQuotas();
		if (evaluationQuotas==null) return result;
		List<SubmissionRound> rounds = evaluationQuotas.getRounds();
		end = evaluationQuotas.getFirstRoundStart();
		return result; // TODO
	}
}
