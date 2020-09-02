package org.sagebionetworks.evaluation.dbo;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.sagebionetworks.evaluation.model.EvaluationRound;
import org.sagebionetworks.evaluation.model.EvaluationRoundLimit;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;
import org.sagebionetworks.schema.adapter.JSONArrayAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONArrayAdapterImpl;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
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

		List<EvaluationRoundLimit> limit = dto.getLimits();
		if(limit != null && !limit.isEmpty()) {
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

		dto.setRoundStart(new Date(dbo.getRoundStart()));
		dto.setRoundEnd(new Date(dbo.getRoundEnd()));

		String limitsJson = dbo.getLimitsJson();
		if(limitsJson != null) {
			try {
				List<EvaluationRoundLimit> limits = new ArrayList<EvaluationRoundLimit>();
				JSONArrayAdapter jsonArray = new JSONArrayAdapterImpl(limitsJson);
				for (int i = 0; i<jsonArray.length(); i ++) {
					if(!jsonArray.isNull(i)) {
						limits.add(new EvaluationRoundLimit(jsonArray.getJSONObject(i)));
					}
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

	public static void validate(EvaluationRound evaluationRound){
		ValidateArgument.requiredNotBlank(evaluationRound.getId(), "id");
		ValidateArgument.requiredNotBlank(evaluationRound.getEtag(), "etag");
		ValidateArgument.requiredNotBlank(evaluationRound.getEvaluationId(), "evaluationId");
		ValidateArgument.required(evaluationRound.getRoundStart(), "roundStart");
		ValidateArgument.required(evaluationRound.getRoundEnd(), "roundEnd");
	}

}
