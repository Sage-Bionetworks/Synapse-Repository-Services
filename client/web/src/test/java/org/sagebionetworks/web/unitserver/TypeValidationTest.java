package org.sagebionetworks.web.unitserver;

import static org.junit.Assert.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.web.server.servlet.TypeValidation;
import org.sagebionetworks.web.shared.ColumnInfo;
import org.sagebionetworks.web.shared.HeaderData;
import org.sagebionetworks.web.shared.ColumnInfo.Type;
import org.sagebionetworks.web.util.RandomColumnData;

public class TypeValidationTest {
	
	private List<ColumnInfo> allColumns;
	List<Map<String, Object>> rows;
	Map<String, HeaderData> columnMap;
	
	@Before
	public void setup(){
		// We want to use all types
		allColumns = RandomColumnData.createListOfAllTypes();
		// Populate with random data
		rows = RandomColumnData.createRandomRows(12, allColumns);
		// Create a map of the column types used
		columnMap = RandomColumnData.createMap(allColumns);
		
	}
	
	@Test
	public void testValidation(){
		// Validate all of the known types.
		List<Map<String, Object>> results = TypeValidation.validateTypes(rows, columnMap);
		assertNotNull(results);
		// For this case the input should match the output
		assertEquals(rows, results);
	}
	
	@Test
	public void testFilter(){
		// remove a column from the expected type. This should be filtered from the results
		ColumnInfo columnToFilter = allColumns.get(0);
		columnMap.remove(columnToFilter.getId());
		Map<String, Object> originalRow = rows.get(0);
		
		List<Map<String, Object>> results = TypeValidation.validateTypes(rows, columnMap);
		assertNotNull(results);
		// The number of rows should match but not the columns
		assertEquals(rows.size(), results.size());
		Map<String, Object> firstRow = results.get(0);
		assertEquals((originalRow.size() - 1),  firstRow.size());
		assertNull(firstRow.get(columnToFilter.getId()));
	}
	
	// Test various conversions
	@Test
	public void testBigIntegerToLong(){
		BigInteger bigInt = new BigInteger("123456");
		Object result = TypeValidation.convert(bigInt, Type.Long);
		assertNotNull(result);
		assertTrue(result instanceof Long);
	}
	
	@Test
	public void testArrayBigIntegerToLong(){
		BigInteger[] bigInt = new BigInteger[] {new  BigInteger("123456"), new  BigInteger("456")};
		Object result = TypeValidation.convert(bigInt, Type.LongArray);
		assertNotNull(result);
		assertTrue(result instanceof Long[]);
	}
	
	@Test
	public void testListBigIntegerToLong(){
		List<BigInteger> list = new ArrayList<BigInteger>();
		list.add(new  BigInteger("123456"));
		list.add(new  BigInteger("456"));
		Object result = TypeValidation.convert(list, Type.LongArray);
		assertNotNull(result);
		assertTrue(result instanceof Long[]);
	}
	
	@Test
	public void testLong(){
		Long bigInt = new Long("123456");
		Object result = TypeValidation.convert(bigInt, Type.Long);
		assertNotNull(result);
		assertTrue(result instanceof Long);
	}
	
	@Test
	public void testBigIntegerToInteger(){
		BigInteger bigInt = new BigInteger("123456");
		Object result = TypeValidation.convert(bigInt, Type.Integer);
		assertNotNull(result);
		assertTrue(result instanceof Integer);
	}
	
	@Test
	public void testArrayBigIntegerToInter(){
		BigInteger[] bigInt = new BigInteger[] {new  BigInteger("123456"), new  BigInteger("456")};
		Object result = TypeValidation.convert(bigInt, Type.IntegerArray);
		assertNotNull(result);
		assertTrue(result instanceof Integer[]);
	}
	
	@Test
	public void testInteger(){
		Integer bigInt = new Integer("123456");
		Object result = TypeValidation.convert(bigInt, Type.Integer);
		assertNotNull(result);
		assertTrue(result instanceof Integer);
	}
	
	@Test
	public void testFloatToDouble(){
		Float floatValue = new Float("3.41");
		Object result = TypeValidation.convert(floatValue, Type.Double);
		assertNotNull(result);
		assertTrue(result instanceof Double);
	}
	
	@Test
	public void testFloatArrayToDouble(){
		Float[] value = new Float[] {new Float("3.41"), new Float("1.23")};
		Object result = TypeValidation.convert(value, Type.DoubleArray);
		assertNotNull(result);
		assertTrue(result instanceof Double[]);
	}
	
	
	@Test
	public void testDoubleToDouble(){
		Double dubVal = new Double("3.41");
		Object result = TypeValidation.convert(dubVal, Type.Double);
		assertNotNull(result);
		assertTrue(result instanceof Double);
	}
	
	@Test
	public void testString(){
		String value = new String("3.41");
		Object result = TypeValidation.convert(value, Type.String);
		assertNotNull(result);
		assertTrue(result instanceof String);
	}
	
	@Test
	public void testStringArray(){
		String[] value = new String[] { "3.41", "more"};
		Object result = TypeValidation.convert(value, Type.StringArray);
		assertNotNull(result);
		assertTrue(result instanceof String[]);
	}
	
	@Test
	public void testStringList(){
		List<String> value = new ArrayList<String>();
		value.add("one");
		value.add("two");
		Object result = TypeValidation.convert(value, Type.StringArray);
		assertNotNull(result);
		assertTrue(result instanceof String[]);
	}
	
	@Test
	public void testBoolean(){
		Boolean value = new Boolean(true);
		Object result = TypeValidation.convert(value, Type.Boolean);
		assertNotNull(result);
		assertTrue(result instanceof Boolean);
	}
	
	@Test
	public void testBooleanArray(){
		Boolean[] value = new Boolean[] { Boolean.TRUE, Boolean.TRUE};
		Object result = TypeValidation.convert(value, Type.BooleanArray);
		assertNotNull(result);
		assertTrue(result instanceof Boolean[]);
	}
	
	@Test
	public void testBooleanList(){
		List<Boolean> value = new ArrayList<Boolean>();
		value.add(Boolean.FALSE);
		Object result = TypeValidation.convert(value, Type.BooleanArray);
		assertNotNull(result);
		assertTrue(result instanceof Boolean[]);
	}
	

}
