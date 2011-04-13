package org.sagebionetworks.web.util;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.web.shared.ColumnInfo.Type;

/**
 * 
 * @author jmhill
 *
 */
public class RandomColumnDataTest {
	
	@Test
	public void testAllTypes(){
		// Make sure all column types are supported
		Type[] types = Type.values();
		for(int i=0; i<types.length; i++){
			Object value = RandomColumnData.createRandomValue(types[i].name());
			assertNotNull(value);
		}
	}
	
	@Test
	public void testString(){
		Object value = RandomColumnData.createRandomValue(Type.String.name());
		assertTrue(value instanceof String);
	}
	
	@Test
	public void testStringArray(){
		Object value = RandomColumnData.createRandomValue(Type.StringArray.name());
		assertTrue(value instanceof String[]);
	}
	
	@Test
	public void testBoolean(){
		Object value = RandomColumnData.createRandomValue(Type.Boolean.name());
		assertTrue(value instanceof Boolean);
	}
	
	@Test
	public void testBooleanArray(){
		Object value = RandomColumnData.createRandomValue(Type.BooleanArray.name());
		assertTrue(value instanceof Boolean[]);
	}
	
	@Test
	public void testLong(){
		Object value = RandomColumnData.createRandomValue(Type.Long.name());
		assertTrue(value instanceof Long);
	}
	
	@Test
	public void testLongArray(){
		Object value = RandomColumnData.createRandomValue(Type.LongArray.name());
		assertTrue(value instanceof Long[]);
	}
	
	@Test
	public void testDouble(){
		Object value = RandomColumnData.createRandomValue(Type.Double.name());
		assertTrue(value instanceof Double);
	}
	
	@Test
	public void testDoubleArray(){
		Object value = RandomColumnData.createRandomValue(Type.DoubleArray.name());
		assertTrue(value instanceof Double[]);
	}
	
	@Test
	public void testInteger(){
		Object value = RandomColumnData.createRandomValue(Type.Integer.name());
		assertTrue(value instanceof Integer);
	}
	
	@Test
	public void testIntegerArray(){
		Object value = RandomColumnData.createRandomValue(Type.IntegerArray.name());
		assertTrue(value instanceof Integer[]);
	}


}
