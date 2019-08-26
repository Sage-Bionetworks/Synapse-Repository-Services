package org.sagebionetworks.repo.model.annotation.v2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;
import java.util.Set;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.util.doubles.DoubleUtils;

class AnnotationV1AndV2TypeMappingTest {

	public void testForClass(){
		assertEquals(AnnotationV1AndV2TypeMapping.STRING, AnnotationV1AndV2TypeMapping.forClass(String.class));
		assertEquals(AnnotationV1AndV2TypeMapping.LONG,AnnotationV1AndV2TypeMapping.forClass(Long.class));
		assertEquals(AnnotationV1AndV2TypeMapping.DOUBLE,AnnotationV1AndV2TypeMapping.forClass(Double.class));
		assertEquals(AnnotationV1AndV2TypeMapping.DATE,AnnotationV1AndV2TypeMapping.forClass(Date.class));
		//for any other classes it should throw exception
		assertThrows(IllegalArgumentException.class, ()->{
			AnnotationV1AndV2TypeMapping.forClass(Object.class);
		});
	}

	public void testForValueType(){
		assertEquals(AnnotationV1AndV2TypeMapping.STRING, AnnotationV1AndV2TypeMapping.forValueType(AnnotationsV2ValueType.STRING));
		assertEquals(AnnotationV1AndV2TypeMapping.LONG,AnnotationV1AndV2TypeMapping.forValueType(AnnotationsV2ValueType.LONG));
		assertEquals(AnnotationV1AndV2TypeMapping.DOUBLE,AnnotationV1AndV2TypeMapping.forValueType(AnnotationsV2ValueType.DOUBLE));
		assertEquals(AnnotationV1AndV2TypeMapping.DATE,AnnotationV1AndV2TypeMapping.forValueType(AnnotationsV2ValueType.TIMESTAMP_MS));
	}
}