package org.sagebionetworks.repo.model.query.jdo;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.repo.model.jdo.persistence.JDODateAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDODoubleAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDOLongAnnotation;
import org.sagebionetworks.repo.model.jdo.persistence.JDOStringAnnotation;
import org.sagebionetworks.repo.model.query.Compartor;
import org.sagebionetworks.repo.model.query.FieldType;

public class SqlConstantsTest {
	
	@Test
	public void testgetClassForFieldType(){
		// Make sure we can get each type
		// Date
		Class clazz = SqlConstants.getJdoClassForFieldType(FieldType.DATE_ATTRIBUTE);
		assertEquals(JDODateAnnotation.class, clazz);
		// Long
		clazz = SqlConstants.getJdoClassForFieldType(FieldType.LONG_ATTRIBUTE);
		assertEquals(JDOLongAnnotation.class, clazz);
		// String
		clazz = SqlConstants.getJdoClassForFieldType(FieldType.STRING_ATTRIBUTE);
		assertEquals(JDOStringAnnotation.class, clazz);
		// Double
		clazz = SqlConstants.getJdoClassForFieldType(FieldType.DOUBLE_ATTRIBUTE);
		assertEquals(JDODoubleAnnotation.class, clazz);
	}
	
	@Test
	public void testGetForeignKeyColumnNameForType(){
		// Test each type
		String columnName = SqlConstants.getForeignKeyColumnNameForType(FieldType.DATE_ATTRIBUTE);
		assertEquals(SqlConstants.FOREIGN_KEY_DATE_ANNOTATION, columnName);
		// Long
		columnName = SqlConstants.getForeignKeyColumnNameForType(FieldType.LONG_ATTRIBUTE);
		assertEquals(SqlConstants.FOREIGN_KEY_LONG_ANNOTATION, columnName);
		// String
		columnName = SqlConstants.getForeignKeyColumnNameForType(FieldType.STRING_ATTRIBUTE);
		assertEquals(SqlConstants.FOREIGN_KEY_STRING_ANNOTATION, columnName);
		// Double
		columnName = SqlConstants.getForeignKeyColumnNameForType(FieldType.DOUBLE_ATTRIBUTE);
		assertEquals(SqlConstants.FOREIGN_KEY_DOUBLE_ANNOTATION, columnName);
	}
	
	@Test
	public void testGetSqlForAllComparator(){
		// Make sure we support all types
		Compartor[] all = Compartor.values();
		for(Compartor comp: all){
			String sql = SqlConstants.getSqlForComparator(comp);
			assertNotNull(sql);
		}
	}
	
	@Test
	public void testEquals(){
		assertEquals("=", SqlConstants.getSqlForComparator(Compartor.EQUALS));
	}

	@Test
	public void testGreater(){
		assertEquals(">", SqlConstants.getSqlForComparator(Compartor.GREATER_THAN));
	}
	
	@Test
	public void testLesss(){
		assertEquals("<", SqlConstants.getSqlForComparator(Compartor.LESS_THAN));
	}
	
	@Test
	public void testGreaterThanOrEquals(){
		assertEquals(">=", SqlConstants.getSqlForComparator(Compartor.GREATER_THAN_OR_EQUALS));
	}
	
	@Test
	public void testLessThanOrEquals(){
		assertEquals("<=", SqlConstants.getSqlForComparator(Compartor.LESS_THAN_OR_EQUALS));
	}
}
