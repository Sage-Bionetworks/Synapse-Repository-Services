package org.sagebionetworks.repo.model.query.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;

import org.junit.Test;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.query.Comparator;

public class SqlConstantsTest {
	
	
	@Test
	public void testGetSqlForAllComparator(){
		// Make sure we support all types
		Comparator[] all = Comparator.values();
		for(Comparator comp: all){
			String sql = SqlConstants.getSqlForComparator(comp);
			assertNotNull(sql);
		}
	}
	
	@Test
	public void testEquals(){
		assertEquals("=", SqlConstants.getSqlForComparator(Comparator.EQUALS));
	}

	@Test
	public void testGreater(){
		assertEquals(">", SqlConstants.getSqlForComparator(Comparator.GREATER_THAN));
	}
	
	@Test
	public void testLesss(){
		assertEquals("<", SqlConstants.getSqlForComparator(Comparator.LESS_THAN));
	}
	
	@Test
	public void testGreaterThanOrEquals(){
		assertEquals(">=", SqlConstants.getSqlForComparator(Comparator.GREATER_THAN_OR_EQUALS));
	}
	
	@Test
	public void testLessThanOrEquals(){
		assertEquals("<=", SqlConstants.getSqlForComparator(Comparator.LESS_THAN_OR_EQUALS));
	}
	
	@Test
	public void testgetColumnNameForPrimaryFieldDatasets(){
		Field[] fields = Study.class.getDeclaredFields();
		for(int i=0; i<fields.length; i++){
			if(!fields[i].isAccessible()){
				fields[i].setAccessible(true);
			}
			String fieldName = fields[i].getName();
			// Make sure we can get each
			String column = SqlConstants.getColumnNameForPrimaryField(fieldName);
			assertNotNull(column);
			System.out.println("Field: "+fieldName+" maps to column: "+column);
		}
	}
	
	@Test
	public void testgetColumnNameForPrimaryFieldLayers(){
		Field[] fields = Data.class.getDeclaredFields();
		for(int i=0; i<fields.length; i++){
			if(!fields[i].isAccessible()){
				fields[i].setAccessible(true);
			}
			String fieldName = fields[i].getName();
			// Make sure we can get each
			String column = SqlConstants.getColumnNameForPrimaryField(fieldName);
			assertNotNull(column);
			System.out.println("Field: "+fieldName+" maps to column: "+column);
		}
	}
	
}
