package org.sagebionetworks.repo.model.annotation.v2;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.Annotations;

public class AnnotationsV2Translator {

	public static Annotations toAnnotationsV1(AnnotationsV2 annotationsV2){
		Annotations annotationsV1 = new Annotations();
		annotationsV1.setId(annotationsV2.getId());
		annotationsV1.setEtag(annotationsV2.getEtag());


		for(Map.Entry<String, AnnotationsV2Value> valueEntry : annotationsV2.getAnnotations().entrySet()){
			String annotationKey = valueEntry.getKey();
			AnnotationsV2Value annotationsV2Value = valueEntry.getValue();
			AnnotationV1AndV2TypeMapping typeMapping = AnnotationV1AndV2TypeMapping.forValueType(annotationsV2Value.getType());

			//skip empty
			if(annotationsV2Value.getValue().isEmpty()){
				continue;
			}

			List<Object> convertedValues = annotationsV2Value.getValue().stream()
					.map(typeMapping.convertToAnnotationV1Function())
					.collect(Collectors.toList());
			annotationsV1.addAnnotation(annotationKey, convertedValues);
		}

		return annotationsV1;
	}

	public static AnnotationsV2 toAnnotationsV2(Annotations annotations){
		Map<String, AnnotationsV2Value> v2AnnotationEntries = new HashMap<>();
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

	static <T> Map<String, AnnotationsV2Value> changeToAnnotationV2Values(Map<String, List<T>> originalMap, Class<T> clazz){
		if(originalMap == null || originalMap.isEmpty()){
			return Collections.emptyMap();
		}

		Map<String, AnnotationsV2Value> newMap = new HashMap<>(originalMap.size());

		AnnotationV1AndV2TypeMapping annotationV1AndV2TypeMapping = AnnotationV1AndV2TypeMapping.forClass(clazz);

		for(Map.Entry<String, List<T>> entry: originalMap.entrySet()){
			//skip this map entry if there are no values
			if(entry.getValue().isEmpty()) {
				continue;
			}

			//convert value list from v1 typed format to v2 string format.
			List<String> convertedValues = entry.getValue().stream()
					.map(annotationV1AndV2TypeMapping.convertToAnnotationV2Function())
					.collect(Collectors.toList());

			//select correct value implementation class based on size of value list
			AnnotationsV2Value annotationsV2Value = new AnnotationsV2Value();
			annotationsV2Value.setValue(convertedValues);
			annotationsV2Value.setType(annotationV1AndV2TypeMapping.getValueType());

			newMap.put(entry.getKey(), annotationsV2Value);
		}

		return newMap;
	}
}
