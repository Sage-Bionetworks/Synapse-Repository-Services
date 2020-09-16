package org.sagebionetworks.evaluation.dbo;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.evaluation.model.EvaluationRound;
import org.sagebionetworks.evaluation.model.EvaluationRoundLimit;
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
}
