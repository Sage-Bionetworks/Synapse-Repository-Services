package org.sagebionetworks.evaluation.manager;

import java.util.Date;

import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.SubmissionQuota;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.util.Pair;

public class SubmissionQuotaUtil {
	
	public void validateSubmissionQuota(Evaluation evaluation) {
		if (evaluation==null) throw new IllegalArgumentException("Evaluation is required.");
		SubmissionQuota submissionQuota = evaluation.getQuota();
		if (submissionQuota==null) return;
		if (submissionQuota.getSubmissionLimit()==null)
			throw new InvalidModelException("SubmissionLimit is required.");
		if (submissionQuota.getFirstRoundStart()==null) {
			if (submissionQuota.getRoundDurationMillis()!=null)
				throw new InvalidModelException("Round duration is specified but first round start is missing.");
			if (submissionQuota.getNumberOfRounds()!=null)
				throw new InvalidModelException("Number of rounds is specified but first round start is missing.");
		} else {
			if (submissionQuota.getRoundDurationMillis()==null)
				throw new InvalidModelException("First round start is specified but round duration is missing.");
			// of for number-of-rounds to be null.  that means there's no end
		}
	}

	public static Integer getSubmissionQuota(Evaluation evaluation) {
		if (evaluation==null) throw new IllegalArgumentException("Evaluation is required.");
		SubmissionQuota submissionQuota = evaluation.getQuota();
		if (submissionQuota==null) return null;
		if (submissionQuota.getSubmissionLimit()==null) return null;
		return submissionQuota.getSubmissionLimit().intValue();
	}
	
	/*
	 * Returns false if time segment(s) is/are defined and the given time is outside
	 * of the allowed time range.
	 */
	public static boolean isSubmissionAllowed(Evaluation evaluation, Date now) {
		if (evaluation==null) throw new IllegalArgumentException("evaluation is required.");
		SubmissionQuota submissionQuota = evaluation.getQuota();
		if (submissionQuota==null || 
				(submissionQuota.getFirstRoundStart()==null || 
				submissionQuota.getRoundDurationMillis()==null))
			return true; // there is no start or end
		Date frs = submissionQuota.getFirstRoundStart();
		if (frs.compareTo(now)>0) return false; // 'now' is before the start of the rounds
		if (submissionQuota.getNumberOfRounds()==null) return true; // there's no end
		Date end = new Date(frs.getTime()+
				submissionQuota.getNumberOfRounds()*
				submissionQuota.getRoundDurationMillis());
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
		SubmissionQuota submissionQuota = evaluation.getQuota();
		if (submissionQuota==null || 
				(submissionQuota.getFirstRoundStart()==null || 
				submissionQuota.getRoundDurationMillis()==null))
			return  new Pair<Date,Date>(null,null); // there is no start or end
		if (!isSubmissionAllowed(evaluation, now)) 
			throw new IllegalArgumentException("The given date is outside the time range allowed for submissions.");
		long frs = submissionQuota.getFirstRoundStart().getTime();
		long roundLen = submissionQuota.getRoundDurationMillis();
		long start=frs+((now.getTime()-frs)/roundLen)*roundLen;
		long end=start+roundLen;
		return new Pair<Date,Date>(new Date(start),new Date(end));
	}
}
