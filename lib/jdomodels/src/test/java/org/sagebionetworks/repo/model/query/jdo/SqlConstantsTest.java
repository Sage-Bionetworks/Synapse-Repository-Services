package org.sagebionetworks.repo.model.query.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;

import org.junit.Test;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.repo.model.query.Compartor;

public class SqlConstantsTest {
	
	
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
	
	@Test
	public void testgetColumnNameForPrimaryFieldDatasets(){
		Field[] fields = Dataset.class.getDeclaredFields();
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
		Field[] fields = Layer.class.getDeclaredFields();
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
