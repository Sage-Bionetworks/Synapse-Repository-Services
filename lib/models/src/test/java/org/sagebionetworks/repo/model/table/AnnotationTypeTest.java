package org.sagebionetworks.repo.model.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;

public class AnnotationTypeTest {
	
	@Test
	public void testColumnType(){
		assertEquals(ColumnType.STRING, AnnotationType.STRING.getColumnType());
		assertEquals(ColumnType.INTEGER, AnnotationType.LONG.getColumnType());
		assertEquals(ColumnType.DOUBLE, AnnotationType.DOUBLE.getColumnType());
		assertEquals(ColumnType.DATE, AnnotationType.DATE.getColumnType());
		assertEquals(ColumnType.BOOLEAN, AnnotationType.BOOLEAN.getColumnType());
	}

	public void testAnnotationType(){
		assertEquals(AnnotationType.STRING, AnnotationType.forAnnotationV2Type(AnnotationsValueType.STRING));
		assertEquals(AnnotationType.DOUBLE, AnnotationType.forAnnotationV2Type(AnnotationsValueType.DOUBLE));
		assertEquals(AnnotationType.LONG, AnnotationType.forAnnotationV2Type(AnnotationsValueType.LONG));
		assertEquals(AnnotationType.DATE, AnnotationType.forAnnotationV2Type(AnnotationsValueType.TIMESTAMP_MS));
		assertEquals(AnnotationType.BOOLEAN, AnnotationType.forAnnotationV2Type(AnnotationsValueType.BOOLEAN));

		//make sure mapping exists for new enums in AnnotationsV2ValueType
		for(AnnotationsValueType enumValue : AnnotationsValueType.values()){
			assertNotNull( AnnotationType.forAnnotationV2Type(enumValue));
		}
	}
}
