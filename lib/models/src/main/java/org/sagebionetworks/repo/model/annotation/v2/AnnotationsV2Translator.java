package org.sagebionetworks.repo.model.annotation.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.sagebionetworks.repo.model.annotation.AnnotationBase;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;

public class AnnotationsV2Translator {

	public static org.sagebionetworks.repo.model.Annotations toAnnotationsV1(Annotations annotationsV2){
		if(annotationsV2 == null){
			return null;
		}

		org.sagebionetworks.repo.model.Annotations annotationsV1 = new org.sagebionetworks.repo.model.Annotations();
		annotationsV1.setId(annotationsV2.getId());
		annotationsV1.setEtag(annotationsV2.getEtag());

		if(annotationsV2.getAnnotations() == null) {
			return annotationsV1;
		}

		for (Map.Entry<String, AnnotationsValue> valueEntry : annotationsV2.getAnnotations().entrySet()) {
			String annotationKey = valueEntry.getKey();
			AnnotationsValue annotationsV2Value = valueEntry.getValue();
			AnnotationsV1AndV2TypeMapping typeMapping = AnnotationsV1AndV2TypeMapping.forValueType(annotationsV2Value.getType());

			List<Object> convertedValues = annotationsV2Value.getValue().stream()
					.filter(Objects::nonNull)
					.map(typeMapping.convertToAnnotationV1Function())
					.collect(Collectors.toList());

			// use appropriate getter function (Annotations::getStringAnnotations, Annotations::getLongAnnotations, etc.)
			// have to use raw Map because AnnotationV1AndV2TypeMapping can't be a generic class
			Map annotationV1TypeSpecificMap = typeMapping.annotationV1MapGetter().apply(annotationsV1);
			annotationV1TypeSpecificMap.put(annotationKey, convertedValues);
		}
		return annotationsV1;
	}

	public static Annotations toAnnotationsV2(org.sagebionetworks.repo.model.Annotations annotations){
		if(annotations == null){
			return null;
		}

		Map<String, AnnotationsValue> v2AnnotationEntries = new HashMap<>();
		v2AnnotationEntries.putAll(changeToAnnotationV2Values(annotations.getStringAnnotations(), String.class));
		v2AnnotationEntries.putAll(changeToAnnotationV2Values(annotations.getDoubleAnnotations(), Double.class));
		v2AnnotationEntries.putAll(changeToAnnotationV2Values(annotations.getLongAnnotations(), Long.class));
		v2AnnotationEntries.putAll(changeToAnnotationV2Values(annotations.getDateAnnotations(), Date.class));

		Annotations annotationsV2 = new Annotations();
		annotationsV2.setEtag(annotations.getEtag());
		annotationsV2.setId(annotations.getId());
		annotationsV2.setAnnotations(v2AnnotationEntries);

		return annotationsV2;
	}

	public static Annotations toAnnotationsV2(org.sagebionetworks.repo.model.annotation.Annotations annotations) {
		if (annotations == null) {
			return null;
		}
		
		Map<String, AnnotationsValue> v2AnnotationEntries = new HashMap<>();
		
		v2AnnotationEntries.putAll(mapStringAnnotations(annotations.getStringAnnos()));
		v2AnnotationEntries.putAll(mapDoubleAnnotations(annotations.getDoubleAnnos()));
		v2AnnotationEntries.putAll(mapLongAnnotations(annotations.getLongAnnos()));
				
		Annotations annotationsV2 = new Annotations();
		annotationsV2.setAnnotations(v2AnnotationEntries);
		return annotationsV2;
	}
	
	static Map<String, AnnotationsValue> mapStringAnnotations(List<StringAnnotation> annotations) {
		Function<StringAnnotation, String> valueSupplier = (annotation) -> annotation.getValue();
		return map(annotations, AnnotationsValueType.STRING, valueSupplier);
	}
	
	static Map<String, AnnotationsValue> mapLongAnnotations(List<LongAnnotation> annotations) {
		Function<LongAnnotation, String> valueSupplier = (annotation) -> {
			return annotation.getValue() == null ? null : annotation.getValue().toString();
		};
		return map(annotations, AnnotationsValueType.LONG, valueSupplier);
	}
	
	static Map<String, AnnotationsValue> mapDoubleAnnotations(List<DoubleAnnotation> annotations) {
		Function<DoubleAnnotation, String> valueSupplier = (annotation) -> {
			return annotation.getValue() == null ? null : annotation.getValue().toString();
		};
		return map(annotations, AnnotationsValueType.DOUBLE, valueSupplier);
	}
	
	static <T extends AnnotationBase> Map<String, AnnotationsValue> map(List<T> annotations, AnnotationsValueType valueType, Function<T, String> valueSupplier) {
		if (annotations == null || annotations.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<String, AnnotationsValue> mapped = new HashMap<>(annotations.size());
		
		annotations.forEach(annotation-> {
			AnnotationsValue mappedValue = mapped.computeIfAbsent(annotation.getKey(), (key) -> {
				AnnotationsValue emptyValue = new AnnotationsValue();
				emptyValue.setType(valueType);
				emptyValue.setValue(new ArrayList<>());
				return emptyValue;
			});
			
			String value = valueSupplier.apply(annotation);
			
			if (value != null) {
				mappedValue.getValue().add(value);
			}
			
		});
		
		return mapped;
	}
	
	static <T> Map<String, List<T>> map(List<? extends AnnotationBase> annotations, Class<T> clazz, Function<AnnotationBase, T> valueSupplier) {
		if (annotations == null || annotations.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<String, List<T>> mapped = new HashMap<>(annotations.size());
		
		annotations.forEach(annotation-> {
			List<T> values = mapped.computeIfAbsent(annotation.getKey(), (key) -> new ArrayList<>());
			values.add(valueSupplier.apply(annotation));
		});
		
		return mapped;
	}
	
	static <T> Map<String, AnnotationsValue> changeToAnnotationV2Values(Map<String, List<T>> originalMap, Class<T> clazz){
		if(originalMap == null || originalMap.isEmpty()){
			return Collections.emptyMap();
		}

		Map<String, AnnotationsValue> newMap = new HashMap<>(originalMap.size());

		AnnotationsV1AndV2TypeMapping annotationsV1AndV2TypeMapping = AnnotationsV1AndV2TypeMapping.forClass(clazz);

		for(Map.Entry<String, List<T>> entry: originalMap.entrySet()){
			//convert value list from v1 typed format to v2 string format.
			List<T> originalList = entry.getValue();
			if(originalList == null){
				originalList = Collections.emptyList();
			}

			List<String> convertedValues = originalList.stream()
					.filter(Objects::nonNull)
					.map(annotationsV1AndV2TypeMapping.convertToAnnotationV2Function())
					.collect(Collectors.toList());

			//select correct value implementation class based on size of value list
			AnnotationsValue annotationsV2Value = new AnnotationsValue();
			annotationsV2Value.setValue(convertedValues);
			annotationsV2Value.setType(annotationsV1AndV2TypeMapping.getValueType());

			newMap.put(entry.getKey(), annotationsV2Value);
		}

		return newMap;
	}
}
