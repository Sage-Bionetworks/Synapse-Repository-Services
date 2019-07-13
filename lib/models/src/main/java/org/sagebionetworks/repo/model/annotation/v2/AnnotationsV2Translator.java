package org.sagebionetworks.repo.model.annotation.v2;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
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

//TODO: TEST
public class AnnotationsV2Translator {

	public static Annotations toAnnotationsV1(AnnotationsV2 annotationsV2){
		Annotations annotationsV1 = new Annotations();
		annotationsV1.setId(annotationsV2.getId());
		annotationsV1.setEtag(annotationsV2.getEtag());

		for(Map.Entry<String, AnnotationV2Value> valueEntry : annotationsV2.getAnnotations().entrySet()){
			valueEntry.getValue();
			ClassToAnnotationV2.forValueType();
		}

	}

	public static AnnotationsV2 toAnnotationsV2(Annotations annotations){
		Map<String, AnnotationV2Value> v2AnnotationEntries = new HashMap<>();
		v2AnnotationEntries.putAll(changeToAnnotationV2Values(annotations.getStringAnnotations(), String.class));
		v2AnnotationEntries.putAll(changeToAnnotationV2Values(annotations.getDoubleAnnotations(), Double.class));
		v2AnnotationEntries.putAll(changeToAnnotationV2Values(annotations.getLongAnnotations(), Long.class));
		v2AnnotationEntries.putAll(changeToAnnotationV2Values(annotations.getDateAnnotations(), Date.class));

		AnnotationsV2 annotationsV2 = new AnnotationsV2();
		annotationsV2.setEtag(annotations.getEtag());
		annotationsV2.setId(annotations.getId());
		annotationsV2.setAnnotations(v2AnnotationEntries);

		return annotationsV2;
	}

	static <T> Map<String, AnnotationV2Value> changeToAnnotationV2Values(Map<String, List<T>> originalMap, Class<T> clazz){
		if(originalMap == null || originalMap.isEmpty()){
			return Collections.emptyMap();
		}

		Map<String, AnnotationV2Value> newMap = new HashMap<>(originalMap.size());

		ClassToAnnotationV2 classToAnnotationV2 = ClassToAnnotationV2.forClass(clazz);

		for(Map.Entry<String, List<T>> entry: originalMap.entrySet()){
			//convert value list from v1 typed format to v2 string format.
			List<String> convertedValues = entry.getValue().stream()
					.map(classToAnnotationV2.convertToAnnotationV2Function())
					.collect(Collectors.toList());

			//skip this map entry if there are no values
			if(convertedValues.isEmpty()) {
				continue;
			}

			//select correct value implementation class based on size of value list
			AnnotationV2Value annotationV2Value;
			if (convertedValues.size() == 1){
				annotationV2Value = new AnnotationV2SingleValue();
				((AnnotationV2SingleValue) annotationV2Value).setValue(
						convertedValues.get(0)
				);
			} else{
				annotationV2Value = new AnnotationV2MultiValue();
				((AnnotationV2MultiValue) annotationV2Value).setValue(
					convertedValues
				);
			}
			annotationV2Value.setType(classToAnnotationV2.getValueType());

			newMap.put(entry.getKey(), annotationV2Value);
		}

		return newMap;
	}
}
