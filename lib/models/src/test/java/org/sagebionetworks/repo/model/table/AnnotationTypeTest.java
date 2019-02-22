package org.sagebionetworks.repo.model.table;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;

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
}
