package org.sagebionetworks.repo.model.annotation.v2;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

import org.junit.jupiter.api.Test;

public class AnnotationsV1AndV2TypeMappingTest {

	@Test
	public void testForClass(){
		assertEquals(AnnotationsV1AndV2TypeMapping.STRING, AnnotationsV1AndV2TypeMapping.forClass(String.class));
		assertEquals(AnnotationsV1AndV2TypeMapping.LONG, AnnotationsV1AndV2TypeMapping.forClass(Long.class));
		assertEquals(AnnotationsV1AndV2TypeMapping.DOUBLE, AnnotationsV1AndV2TypeMapping.forClass(Double.class));
		assertEquals(AnnotationsV1AndV2TypeMapping.DATE, AnnotationsV1AndV2TypeMapping.forClass(Date.class));
		//for any other classes it should throw exception
		assertThrows(IllegalArgumentException.class, ()->{
			AnnotationsV1AndV2TypeMapping.forClass(Object.class);
		});
	}

	@Test
	public void testForValueType(){
		assertEquals(AnnotationsV1AndV2TypeMapping.STRING, AnnotationsV1AndV2TypeMapping.forValueType(AnnotationsValueType.STRING));
		assertEquals(AnnotationsV1AndV2TypeMapping.LONG, AnnotationsV1AndV2TypeMapping.forValueType(AnnotationsValueType.LONG));
		assertEquals(AnnotationsV1AndV2TypeMapping.DOUBLE, AnnotationsV1AndV2TypeMapping.forValueType(AnnotationsValueType.DOUBLE));
		assertEquals(AnnotationsV1AndV2TypeMapping.DATE, AnnotationsV1AndV2TypeMapping.forValueType(AnnotationsValueType.TIMESTAMP_MS));
	}
}