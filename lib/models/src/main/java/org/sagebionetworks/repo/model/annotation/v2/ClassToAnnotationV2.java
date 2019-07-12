package org.sagebionetworks.repo.model.annotation.v2;

import java.util.function.Function;

import org.checkerframework.checker.units.qual.C;

enum ClassToAnnotationV2 {

	STRING,
	DOUBLE,
	LONG,
	DATE;

	final Class javaClass;
	final AnnotationV2ValueType valueType;
	final Function<?, String> toAnnotationV2Function;
	final Function<String, ?> toAnnotationV1Function;
}
