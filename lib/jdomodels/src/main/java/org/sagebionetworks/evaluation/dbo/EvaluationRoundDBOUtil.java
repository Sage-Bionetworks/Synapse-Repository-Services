package org.sagebionetworks.evaluation.dbo;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.EvaluationRound;
import org.sagebionetworks.evaluation.model.EvaluationRoundLimit;
import org.sagebionetworks.evaluation.model.EvaluationRoundLimitType;
import org.sagebionetworks.evaluation.model.SubmissionQuota;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.schema.adapter.JSONArrayAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONArrayAdapterImpl;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.util.ValidateArgument;

public class EvaluationRoundDBOUtil {
	public static EvaluationRoundDBO toDBO(EvaluationRound dto){
		ValidateArgument.requiredNotBlank(dto.getId(), "id");
		ValidateArgument.requiredNotBlank(dto.getEtag(), "etag");
		ValidateArgument.requiredNotBlank(dto.getEvaluationId(), "evaluationId");

		EvaluationRoundDBO dbo = new EvaluationRoundDBO();

		dbo.setId(Long.parseLong(dto.getId()));
		dbo.setEtag(dto.getEtag());
		dbo.setEvaluationId(Long.parseLong(dto.getEvaluationId()));

		dbo.setRoundStart(new Timestamp(dto.getRoundStart().getTime()));
		dbo.setRoundEnd(new Timestamp(dto.getRoundEnd().getTime()));

		List<EvaluationRoundLimit> limit = dto.getLimits();
		if(CollectionUtils.isNotEmpty(limit)) {
			try {
				JSONArrayAdapter jsonArray = new JSONArrayAdapterImpl();
				for (int i = 0; i < limit.size(); i++) {
					EvaluationRoundLimit val = limit.get(i);
					if(val != null) {
						jsonArray.put(i, val.writeToJSONObject(new JSONObjectAdapterImpl()));
					}
				}
				dbo.setLimitsJson(jsonArray.toJSONString());
			} catch (JSONObjectAdapterException e) {
				throw new IllegalStateException("Could not serialize EvaluationRoundLimit", e);
			}
		}

		return dbo;
	}

	public static EvaluationRound toDTO(EvaluationRoundDBO dbo){
		EvaluationRound dto = new EvaluationRound();

		dto.setId(dbo.getId().toString());
		dto.setEtag(dbo.getEtag());
		dto.setEvaluationId(dbo.getEvaluationId().toString());

		dto.setRoundStart(dbo.getRoundStart());
		dto.setRoundEnd(dbo.getRoundEnd());

		String limitsJson = dbo.getLimitsJson();
		if(StringUtils.isNotEmpty(limitsJson)) {
			try {
				JSONArrayAdapter jsonArray = new JSONArrayAdapterImpl(limitsJson);
				List<EvaluationRoundLimit> limits = new ArrayList<>(jsonArray.length());
				for (int i = 0; i<jsonArray.length(); i ++) {
					if(jsonArray.isNull(i)) {
						throw new IllegalStateException("null value should not have been stored");
					}
					limits.add(new EvaluationRoundLimit(jsonArray.getJSONObject(i)));
				}
				if (!limits.isEmpty()){
					dto.setLimits(limits);
				}
			} catch (JSONObjectAdapterException e) {
				throw new IllegalStateException("Could not deserialize EvaluationRoundLimit", e);
			}
		}

		return dto;
	}


	private static final Date FAR_FUTURE_DATE = new Date(2121,1,1);
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
}
