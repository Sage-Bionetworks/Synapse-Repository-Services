package org.sagebionetworks.repo.model.annotation.v2;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.util.ValidateArgument;

public class AnnotationsV2Utils {

	/**
	 * Puts the (key,value,type) mapping into the annotation. Will replace existing (value,type) if the key already exists
	 * @param annotationsV2
	 * @param key
	 * @param value
	 * @param type
	 * @return previous value if it was replaced. else null
	 */
	public static AnnotationsV2Value putAnnotations(AnnotationsV2 annotationsV2, String key, List<String> value, AnnotationsV2ValueType type){
		ValidateArgument.required(annotationsV2, "annotationsV2");
		ValidateArgument.requiredNotEmpty(key, "key");
		ValidateArgument.requiredNotEmpty(value, "value");
		ValidateArgument.required(type, "type");

		//ensure annotations map not null
		Map<String, AnnotationsV2Value> annotationsMap = annotationsV2.getAnnotations();
		if(annotationsMap == null){
			annotationsMap = new LinkedHashMap<>();
			annotationsV2.setAnnotations(annotationsMap);
		}

		AnnotationsV2Value v2Value = new AnnotationsV2Value();
		v2Value.setType(type);
		v2Value.setValue(value);
		return annotationsMap.put(key, v2Value);
	}

}
