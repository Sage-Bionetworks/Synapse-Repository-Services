package org.sagebionetworks.evaluation.dbo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationRound;
import org.sagebionetworks.evaluation.model.EvaluationRoundLimit;
import org.sagebionetworks.evaluation.model.EvaluationRoundLimitType;
import org.sagebionetworks.evaluation.model.SubmissionQuota;

public class EvaluationRoundTranslationUtil {

	//////////////////////////////////TEMPORARY MIGRATION CODE////////////////////////////////////////////////////

	private static final Date FAR_FUTURE_DATE = new Date(2121, Calendar.JANUARY,1);
	private static final long DAY_IN_MILLIS = 86400000L;
	private static final long WEEK_IN_MILLIS = 604800000L;
	private static final long DAYS_28_IN_MILLIS = 2419200000L;
	private static final long DAYS_31_IN_MILLIS = 2678400000L;

	public static List<EvaluationRound> fromSubmissionQuota(Evaluation evaluation){
		SubmissionQuota quota = evaluation.getQuota();
		if (quota == null){
			return Collections.emptyList();
		}

		Date firstRoundStart = quota.getFirstRoundStart();
		Long submissionLimit = quota.getSubmissionLimit();
		Long roundDurationMillis = quota.getRoundDurationMillis();
		Long numberOfRounds = quota.getNumberOfRounds();

		//no fields are defined
		if (firstRoundStart == null
				&& roundDurationMillis == null
				&& numberOfRounds == null
				&& submissionLimit == null){
			return Collections.emptyList();
		}

		//no start date
		if(firstRoundStart == null){
			//malformed Quota if only NumberOfRounds or RoundDurationMillis defined
			if(submissionLimit == null){
				return Collections.emptyList();
			}
			EvaluationRound round = singleLongTermRound(evaluation, quota);
			return Collections.singletonList(round);
		}

		// no end date
		// in this case, we just assume they mean a single unending round.
		// for detailed reasoning see:
		// https://sagebionetworks.jira.com/browse/PLFM-6574?focusedCommentId=124581&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-124581
		if(roundDurationMillis == null || numberOfRounds == null){
			EvaluationRound round = singleLongTermRound(evaluation, quota);
			return Collections.singletonList(round);
		}

		//specific round durations are converted into single round Daily, Weekly, and Monthly limits
		Date roundEnd = Date.from(firstRoundStart.toInstant()
				.plusMillis(roundDurationMillis * numberOfRounds));
		if(roundDurationMillis == DAY_IN_MILLIS){
			return Collections.singletonList(roundWithLimits(evaluation.getId(), firstRoundStart, roundEnd, EvaluationRoundLimitType.DAILY, submissionLimit));
		}else if (roundDurationMillis == WEEK_IN_MILLIS){
			return Collections.singletonList(roundWithLimits(evaluation.getId(), firstRoundStart, roundEnd, EvaluationRoundLimitType.WEEKLY, submissionLimit));
		}else if (roundDurationMillis >= DAYS_28_IN_MILLIS && roundDurationMillis <= DAYS_31_IN_MILLIS){
			return Collections.singletonList(roundWithLimits(evaluation.getId(), firstRoundStart, roundEnd, EvaluationRoundLimitType.MONTHLY, submissionLimit));
		} else{
			List<EvaluationRound> evaluationRounds = new ArrayList<>(Math.toIntExact(numberOfRounds));
			for(int roundNum = 1; roundNum <= numberOfRounds; roundNum++){
				Date roundStart = Date.from(firstRoundStart.toInstant()
						.plusMillis(roundDurationMillis * (roundNum-1)));
				roundEnd = Date.from(firstRoundStart.toInstant()
						.plusMillis(roundDurationMillis * roundNum));

				EvaluationRound round = roundWithLimits(evaluation.getId(), firstRoundStart, roundEnd, EvaluationRoundLimitType.TOTAL, submissionLimit);
				evaluationRounds.add(round);
			}
			return evaluationRounds;
		}
	}

	private static EvaluationRound roundWithLimits(String evaluationId, Date startDate, Date endDate, EvaluationRoundLimitType limitType, Long submissionLimit){
		EvaluationRound round = newRoundHelper(evaluationId);
		round.setRoundStart(startDate);
		round.setRoundEnd(endDate);
		if(limitType != null && submissionLimit != null){
			EvaluationRoundLimit limit = new EvaluationRoundLimit();
			limit.setLimitType(limitType);
			limit.setMaximumSubmissions(submissionLimit);
			round.setLimits(Collections.singletonList(limit));
		}
		return round;
	}

	private static EvaluationRound singleLongTermRound(Evaluation evaluation, SubmissionQuota quota) {
		// only submission limit is defined so we create a single round with a TOTAL limit
		EvaluationRound round = newRoundHelper(evaluation.getId());
		if(quota.getFirstRoundStart() != null){
			round.setRoundStart(quota.getFirstRoundStart());
		}else{
			round.setRoundStart(evaluation.getCreatedOn());
		}
		round.setRoundEnd(FAR_FUTURE_DATE);

		if(quota.getSubmissionLimit() != null) {
			EvaluationRoundLimit limit = new EvaluationRoundLimit();
			limit.setLimitType(EvaluationRoundLimitType.TOTAL);
			limit.setMaximumSubmissions(quota.getSubmissionLimit());
			round.setLimits(Collections.singletonList(limit));
		}
		return round;
	}

	private static EvaluationRound newRoundHelper(String evaluationId){
		EvaluationRound evaluationRound = new EvaluationRound();
		evaluationRound.setEvaluationId(evaluationId);
		return evaluationRound;
	}

	//////////////////////////////////TEMPORARY MIGRATION CODE////////////////////////////////////////////////////
}
