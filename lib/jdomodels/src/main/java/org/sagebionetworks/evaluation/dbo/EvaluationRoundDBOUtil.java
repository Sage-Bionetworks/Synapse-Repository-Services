package org.sagebionetworks.evaluation.dbo;

import java.util.Date;

import org.sagebionetworks.evaluation.model.EvaluationRound;
import org.sagebionetworks.evaluation.model.EvaluationRoundLimit;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.ValidateArgument;

//TODO: test
public class EvaluationRoundDBOUtil {
	public static EvaluationRoundDBO toDBO(EvaluationRound dto){
		validate(dto);

		EvaluationRoundDBO dbo = new EvaluationRoundDBO();

		dbo.setId(Long.parseLong(dto.getId()));
		dbo.setEtag(dto.getEtag());
		dbo.setEvaluationId(Long.parseLong(dto.getEvaluationId()));

		dbo.setRoundStart(dto.getRoundStart().getTime());
		dbo.setRoundEnd(dto.getRoundEnd().getTime());

		EvaluationRoundLimit limit = dto.getSubmissionLimits();
		if(limit != null) {
			try {
				String jsonString = EntityFactory.createJSONStringForEntity(limit);
				dbo.setLimitsJson(jsonString);
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

		dto.setRoundStart(new Date(dbo.getRoundStart()));
		dto.setRoundEnd(new Date(dbo.getRoundEnd()));

		String limitsJson = dbo.getLimitsJson();
		if(limitsJson != null) {
			try {
				EvaluationRoundLimit submissionLimits = EntityFactory.createEntityFromJSONString(limitsJson, EvaluationRoundLimit.class);
				dto.setSubmissionLimits(submissionLimits);
			} catch (JSONObjectAdapterException e) {
				throw new IllegalStateException("Could not deserialize EvaluationRoundLimit", e);
			}
		}

		return dto;
	}

	public static void validate(EvaluationRound evaluationRound){
		ValidateArgument.requiredNotBlank(evaluationRound.getId(), "id");
		ValidateArgument.requiredNotBlank(evaluationRound.getEtag(), "etag");
		ValidateArgument.requiredNotBlank(evaluationRound.getEvaluationId(), "evaluationId");
		ValidateArgument.required(evaluationRound.getRoundStart(), "roundStart");
		ValidateArgument.required(evaluationRound.getRoundEnd(), "roundEnd");
	}

}
