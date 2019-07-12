package org.sagebionetworks.repo.model.annotation.v2;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationV2MultiValue;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationV2SingleValue;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationV2Value;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationV2ValueType;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2;

public class AnnotationsV2Translator {
	private static final Map<Class, AnnotationV2ValueType> CLASS_TO_ANNOTATION_TYPE = ImmutableMap.of(
		String.class, AnnotationV2ValueType.STRING,
		Double.class, AnnotationV2ValueType.DOUBLE,
		Long.class, AnnotationV2ValueType.LONG,
		Date.class, AnnotationV2ValueType.DATE
	);

	public static AnnotationsV2 translateFromAnnotationsV1(Annotations annotations){
		Map<String, AnnotationV2Value> v2AnnotationEntries = new HashMap<>();
		v2AnnotationEntries.putAll(changeToAnnotationV2Values(annotations.getStringAnnotations(), String.class));
		v2AnnotationEntries.putAll(changeToAnnotationV2Values(annotations.getDoubleAnnotations(), Double.class));
		v2AnnotationEntries.putAll(changeToAnnotationV2Values(annotations.getLongAnnotations(), Long.class));
		v2AnnotationEntries.putAll(changeToAnnotationV2Values(annotations.getDateAnnotations(), Date.class));
	}

	static <T> Map<String, AnnotationV2Value> changeToAnnotationV2Values(Map<String, List<T>> originalMap, Class<T> clazz){
		if(originalMap == null || originalMap.isEmpty()){
			return Collections.emptyMap();
		}

		Map<String, AnnotationV2Value> newMap = new HashMap<>(originalMap.size());

		for(Map.Entry<String, List<T>> entry: originalMap.entrySet()){
			String key = entry.getKey();
			List<T> listValue = entry.getValue();

			AnnotationV2Value annotationV2Value;
			if(listValue.isEmpty()){
				continue;
			} else if (listValue.size() == 1){
				annotationV2Value = new AnnotationV2SingleValue();
				((AnnotationV2SingleValue) annotationV2Value).setValue(
						listValue.get(0).toString()
				);
			} else{
				annotationV2Value = new AnnotationV2MultiValue();
				((AnnotationV2MultiValue) annotationV2Value).setValue(
						listValue.stream()
						.map(Objects::toString)//TODO: date needs to be handled differently
						.collect(Collectors.toList())
				);
			}
			annotationV2Value.setType(CLASS_TO_ANNOTATION_TYPE.get(clazz));
		}

		return newMap;
	}
}
