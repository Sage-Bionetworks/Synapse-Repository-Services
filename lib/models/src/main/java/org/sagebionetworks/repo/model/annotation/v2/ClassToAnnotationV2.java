package org.sagebionetworks.repo.model.annotation.v2;

import java.util.Date;
import java.util.function.Function;

enum ClassToAnnotationV2 {

	STRING(String.class, AnnotationV2ValueType.STRING, String::toString, String::toString),
	DOUBLE(Double.class, AnnotationV2ValueType.DOUBLE, Double::toString, Double::parseDouble),
	LONG(Long.class, AnnotationV2ValueType.LONG, Long::toString, Long::parseLong),
	DATE(Date.class, AnnotationV2ValueType.DATE, (Date date) -> Long.toString(date.getTime()), (String timestampMillis) -> new Date(Long.parseLong(timestampMillis)));

	private final Class<?> javaClass;
	private final AnnotationV2ValueType valueType;
	private final Function<?, String> toAnnotationV2Function;
	private final Function<String, ?> toAnnotationV1Function;


	<T> ClassToAnnotationV2(Class<T> javaClass, AnnotationV2ValueType valueType, Function<T, String> toAnnotationV2Function, Function<String, T> toAnnotationV1Function) {
		this.javaClass = javaClass;
		this.valueType = valueType;
		this.toAnnotationV2Function = toAnnotationV2Function;
		this.toAnnotationV1Function = toAnnotationV1Function;
	}


	public <T> Function<String, T> convertToAnnotationV1Function(){
		return (Function<String, T>) toAnnotationV1Function;
	}

	public <T> Function<T, String> convertToAnnotationV2Function(){
		return (Function<T, String>) toAnnotationV2Function;
	}

	public AnnotationV2ValueType getValueType(){
		return valueType;
	}


	public static ClassToAnnotationV2 forClass(Class clazz){
		for(ClassToAnnotationV2 classToAnnotationV2 : values()){
			if(classToAnnotationV2.javaClass == clazz){
				return classToAnnotationV2;
			}
		}
		throw new IllegalArgumentException("No mapping for " + clazz + " to a ClassToAnnotationV2");
	}

	public static ClassToAnnotationV2 forValueType(AnnotationV2ValueType valueType){
		for(ClassToAnnotationV2 classToAnnotationV2 : values()){
			if(classToAnnotationV2.valueType == valueType){
				return classToAnnotationV2;
			}
		}
		throw new IllegalArgumentException("No mapping for " + valueType + " to a ClassToAnnotationV2");
	}
}
