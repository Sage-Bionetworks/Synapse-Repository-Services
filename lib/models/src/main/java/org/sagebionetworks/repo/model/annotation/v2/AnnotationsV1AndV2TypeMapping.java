package org.sagebionetworks.repo.model.annotation.v2;

import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.util.doubles.DoubleUtils;

enum AnnotationsV1AndV2TypeMapping {

	STRING(String.class, AnnotationsValueType.STRING, Function.identity(), Function.identity(), Annotations::getStringAnnotations),

	//DoubleUtils.fromString handles other representations of Infinity and Nan, unlike Java's built in Double.valueOf
	DOUBLE(Double.class, AnnotationsValueType.DOUBLE, Object::toString, DoubleUtils::fromString, Annotations::getDoubleAnnotations),
	LONG(Long.class, AnnotationsValueType.LONG, Object::toString, Long::valueOf, Annotations::getLongAnnotations),
	DATE(Date.class, AnnotationsValueType.TIMESTAMP_MS,
			(Date date) -> Long.toString(date.getTime()),
			(String timestampMillis) -> new Date(Long.parseLong(timestampMillis)),
			Annotations::getDateAnnotations);

	private final Class<?> javaClass;
	private final AnnotationsValueType valueType;
	private final Function<?, String> toAnnotationV2Function;
	private final Function<String, ?> toAnnotationV1Function;
	private final Function<Annotations, Map> annotationV1MapGetter;


	<T> AnnotationsV1AndV2TypeMapping(Class<T> javaClass,
									  AnnotationsValueType valueType,
									  Function<T, String> toAnnotationV2Function,
									  Function<String, T> toAnnotationV1Function,
									  Function<Annotations, Map> annotationV1MapGetter) {
		this.javaClass = javaClass;
		this.valueType = valueType;
		this.toAnnotationV2Function = toAnnotationV2Function;
		this.toAnnotationV1Function = toAnnotationV1Function;
		this.annotationV1MapGetter = annotationV1MapGetter;
	}

	public <T> Function<String, T> convertToAnnotationV1Function(){
		return (Function<String, T>) toAnnotationV1Function;
	}

	public <T> Function<T, String> convertToAnnotationV2Function(){
		return (Function<T, String>) toAnnotationV2Function;
	}

	public Function<Annotations, Map> annotationV1MapGetter() {
		return annotationV1MapGetter;
	}

	public AnnotationsValueType getValueType(){
		return valueType;
	}

	public static AnnotationsV1AndV2TypeMapping forClass(Class clazz){
		for(AnnotationsV1AndV2TypeMapping annotationsV1AndV2TypeMapping : values()){
			if(annotationsV1AndV2TypeMapping.javaClass == clazz){
				return annotationsV1AndV2TypeMapping;
			}
		}
		throw new IllegalArgumentException("No mapping for " + clazz + " to a AnnotationV1AndV2TypeMapping");
	}

	public static AnnotationsV1AndV2TypeMapping forValueType(AnnotationsValueType valueType){
		for(AnnotationsV1AndV2TypeMapping annotationsV1AndV2TypeMapping : values()){
			if(annotationsV1AndV2TypeMapping.valueType == valueType){
				return annotationsV1AndV2TypeMapping;
			}
		}
		throw new IllegalArgumentException("No mapping for " + valueType + " to a AnnotationV1AndV2TypeMapping");
	}
}
