package org.sagebionetworks.repo.model.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Date;

import org.junit.Test;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2ValueType;

public class AnnotationTypeTest {

	
	@Test
	public void testPaserValue(){
		assertEquals("aString", AnnotationType.STRING.parseValue("aString"));
		assertEquals(new Long(123), AnnotationType.LONG.parseValue("123"));
		assertEquals(new Double(1.23), AnnotationType.DOUBLE.parseValue("1.23"));
		// dates can be strings or numbers
		assertEquals(new Date(1480950853111L), AnnotationType.DATE.parseValue("2016-12-05 15:14:13.111"));
		assertEquals(new Date(123L), AnnotationType.DATE.parseValue("123"));
	}
	
	@Test
	public void testColumnType(){
		assertEquals(ColumnType.STRING, AnnotationType.STRING.getColumnType());
		assertEquals(ColumnType.INTEGER, AnnotationType.LONG.getColumnType());
		assertEquals(ColumnType.DOUBLE, AnnotationType.DOUBLE.getColumnType());
		assertEquals(ColumnType.DATE, AnnotationType.DATE.getColumnType());
	}

	public void testAnnotationType(){
		assertEquals(AnnotationType.STRING, AnnotationType.forAnnotationV2Type(AnnotationsV2ValueType.STRING));
		assertEquals(AnnotationType.DOUBLE, AnnotationType.forAnnotationV2Type(AnnotationsV2ValueType.DOUBLE));
		assertEquals(AnnotationType.LONG, AnnotationType.forAnnotationV2Type(AnnotationsV2ValueType.LONG));
		assertEquals(AnnotationType.DATE, AnnotationType.forAnnotationV2Type(AnnotationsV2ValueType.TIMESTAMP_MS));

		//make sure mapping exists for new enums in AnnotationsV2ValueType
		for(AnnotationsV2ValueType enumValue : AnnotationsV2ValueType.values()){
			assertNotNull( AnnotationType.forAnnotationV2Type(enumValue));
		}
	}


}
