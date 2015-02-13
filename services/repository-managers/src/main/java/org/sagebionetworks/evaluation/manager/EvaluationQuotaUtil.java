package org.sagebionetworks.evaluation.manager;

import java.util.Date;

import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationQuota;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.util.Pair;

public class EvaluationQuotaUtil {
	
	public void validateEvaluationQuota(Evaluation evaluation) {
		if (evaluation==null) throw new IllegalArgumentException("Evaluation is required.");
		EvaluationQuota evaluationQuota = evaluation.getQuota();
		if (evaluationQuota==null) return;
		if (evaluationQuota.getSubmissionLimit()==null)
			throw new InvalidModelException("SubmissionLimit is required.");
		if (evaluationQuota.getFirstRoundStart()==null) {
			if (evaluationQuota.getRoundDurationMillis()!=null)
				throw new InvalidModelException("Round duration is specified but first round start is missing.");
			if (evaluationQuota.getNumberOfRounds()!=null)
				throw new InvalidModelException("Number of rounds is specified but first round start is missing.");
		} else {
			if (evaluationQuota.getRoundDurationMillis()==null)
				throw new InvalidModelException("First round start is specified but round duration is missing.");
			// of for number-of-rounds to be null.  that means there's no end
		}
	}

	public static Integer getSubmissionQuota(Evaluation evaluation) {
		if (evaluation==null) throw new IllegalArgumentException("Evaluation is required.");
		EvaluationQuota evaluationQuota = evaluation.getQuota();
		if (evaluationQuota==null) return null;
		return evaluationQuota.getSubmissionLimit().intValue();
	}
	
	/*
	 * Returns false if time segment(s) is/are defined and the given time is outside
	 * of the allowed time range.
	 */
	public static boolean isSubmissionAllowed(Evaluation evaluation, Date now) {
		if (evaluation==null) throw new IllegalArgumentException("evaluation is required.");
		EvaluationQuota evaluationQuota = evaluation.getQuota();
		if (evaluationQuota==null || 
				(evaluationQuota.getFirstRoundStart()==null || 
				evaluationQuota.getRoundDurationMillis()==null))
			return true; // there is no start or end
		Date frs = evaluationQuota.getFirstRoundStart();
		if (frs.compareTo(now)>0) return false; // 'now' is before the start of the rounds
		if (evaluationQuota.getNumberOfRounds()==null) return true; // there's no end
		Date end = new Date(frs.getTime()+
				evaluationQuota.getNumberOfRounds()*
				evaluationQuota.getRoundDurationMillis());
		return (end.compareTo(now)>0); // 'now' is before -or equal to- the end of the rounds
	}
	
	/*
	 * Given an Evaluation and a Date (time stamp), return the interval containing the date.
	 * Before calling this, call isSubmissionAllowed to determine whether the date of
	 * interest is within the time range allowed for submissions.
	 * 
	 */
	public static Pair<Date, Date> getRoundInterval(Evaluation evaluation, Date now) {
		if (evaluation==null) throw new IllegalArgumentException("evaluation is required.");
		if (now==null) throw new IllegalArgumentException("current date is required.");
		EvaluationQuota evaluationQuota = evaluation.getQuota();
		if (evaluationQuota==null || 
				(evaluationQuota.getFirstRoundStart()==null || 
				evaluationQuota.getRoundDurationMillis()==null))
			return  new Pair<Date,Date>(null,null); // there is no start or end
		if (!isSubmissionAllowed(evaluation, now)) 
			throw new IllegalArgumentException("The given date is outside the time range allowed for submissions.");
		long frs = evaluationQuota.getFirstRoundStart().getTime();
		long roundLen = evaluationQuota.getRoundDurationMillis();
		long start=frs+((now.getTime()-frs)/roundLen)*roundLen;
		long end=start+roundLen;
		return new Pair<Date,Date>(new Date(start),new Date(end));
	}
}
