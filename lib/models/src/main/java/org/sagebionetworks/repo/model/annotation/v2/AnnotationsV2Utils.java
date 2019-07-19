package org.sagebionetworks.repo.model.annotation.v2;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.ValidateArgument;

public class 	AnnotationsV2Utils {

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

		return annotationsMap.put(key, createNewValue(type, value));
	}

	public static AnnotationsV2Value createNewValue(AnnotationsV2ValueType type, String... value) {
		return createNewValue(type, Arrays.asList(value));
	}

	public static AnnotationsV2Value createNewValue(AnnotationsV2ValueType type, List<String> value) {
		AnnotationsV2Value v2Value = new AnnotationsV2Value();
		v2Value.setType(type);
		v2Value.setValue(value);
		return v2Value;
	}

	/**
	 * Serializes ONLY the annotations, ignoring ID and Etag.
	 * @param annotationsV2
	 * @return JSON String of annotationsV2 if any annotations exist. Null otherwise
	 * @throws JSONObjectAdapterException
	 */
	public static String toJSONStringForStorage(AnnotationsV2 annotationsV2) throws JSONObjectAdapterException {
		if(annotationsV2 == null || annotationsV2.getAnnotations() == null || annotationsV2.getAnnotations().isEmpty()){
			return null;
		}

		//use a shallow copy which does not contain the id and etag info
		AnnotationsV2 annotationsV2ShallowCopy = new AnnotationsV2();
		annotationsV2ShallowCopy.setAnnotations(annotationsV2.getAnnotations());

		return EntityFactory.createJSONStringForEntity(annotationsV2ShallowCopy);
	}


}
