package org.sagebionetworks.repo.model.annotation.v2;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.util.ValidateArgument;

/**
 * Convenience functions for modifying annotations that are only used in tests
 */
public class AnnotationsV2TestUtils {
	/**
	 * Puts the (key,value,type) mapping into the annotation. Will replace existing (value,type) if the key already exists
	 * @param annotationsV2
	 * @param key
	 * @param values
	 * @param type
	 * @return previous value if it was replaced. else null
	 */
	public static AnnotationsValue putAnnotations(Annotations annotationsV2, String key, List<String> values, AnnotationsValueType type){
		ValidateArgument.required(annotationsV2, "annotationsV2");
		ValidateArgument.requiredNotEmpty(key, "key");
		ValidateArgument.required(values, "value");
		ValidateArgument.required(type, "type");

		//ensure annotations map not null
		Map<String, AnnotationsValue> annotationsMap = annotationsV2.getAnnotations();
		if(annotationsMap == null){
			annotationsMap = new LinkedHashMap<>();
			annotationsV2.setAnnotations(annotationsMap);
		}

		return annotationsMap.put(key, createNewValue(type, values));
	}

	/**
	 * Puts the (key,value,type) mapping into the annotation. Will replace existing (value,type) if the key already exists
	 * @param annotationsV2
	 * @param key
	 * @param value
	 * @param type
	 * @return previous value if it was replaced. else null
	 */
	public static AnnotationsValue putAnnotations(Annotations annotationsV2, String key, String value, AnnotationsValueType type) {
		return putAnnotations(annotationsV2, key, Collections.singletonList(value), type);
	}

	public static AnnotationsValue createNewValue(AnnotationsValueType type, String... value) {
		return createNewValue(type, Arrays.asList(value));
	}

	public static AnnotationsValue createNewValue(AnnotationsValueType type, List<String> value) {
		AnnotationsValue v2Value = new AnnotationsValue();
		v2Value.setType(type);
		v2Value.setValue(value);
		return v2Value;
	}

	public static Annotations newEmptyAnnotationsV2(){
		return newEmptyAnnotationsV2(null);
	}

	public static Annotations newEmptyAnnotationsV2(String id){
		Annotations annotationsV2 = new Annotations();
		annotationsV2.setId(id);
		annotationsV2.setAnnotations(new HashMap<>());
		return annotationsV2;
	}
}
