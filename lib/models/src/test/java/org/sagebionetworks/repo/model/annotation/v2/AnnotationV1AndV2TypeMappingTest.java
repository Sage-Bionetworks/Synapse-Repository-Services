package org.sagebionetworks.repo.model.annotation.v2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

class AnnotationV1AndV2TypeMappingTest {


	@Test
	public void testConvertStringToDouble_NaN() {

		Set<String> nanStringPermutations = Sets.newHashSet( "nan", "Nan", "NaN", "NAn", "NAN", "nAn", "nAN", "naN");
		assertEquals(8, nanStringPermutations.size()); // 2^3 = 8

		for (String s : nanStringPermutations) {
			assertTrue(Double.isNaN(AnnotationV1AndV2TypeMapping.convertStringToDouble(s)));
		}
	}
	@Test
	public void testConvertStringToDouble_normalDoubles() {
		assertEquals(-1.2, AnnotationV1AndV2TypeMapping.convertStringToDouble("-1.2"));
		assertEquals(0.0, AnnotationV1AndV2TypeMapping.convertStringToDouble("0"));
		assertEquals(3.14159265359, AnnotationV1AndV2TypeMapping.convertStringToDouble("3.14159265359"));
	}


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