package org.sagebionetworks.table.cluster;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.SelectColumnAndModel;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.FunctionType;
import org.sagebionetworks.table.query.model.HasQuoteValue;

public class SQLTranslatorUtilsTest {
	
	@Mock
	HasQuoteValue mockHasQuoteValue;
	
	Map<String, ColumnModel> columnMap = new HashMap<String, ColumnModel>(1);
	
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
	}

	
	@Test
	public void testIsNumericType(){
		assertFalse(SQLTranslatorUtils.isNumericType(ColumnType.BOOLEAN));
		assertTrue(SQLTranslatorUtils.isNumericType(ColumnType.DATE));
		assertTrue(SQLTranslatorUtils.isNumericType(ColumnType.DOUBLE));
		assertFalse(SQLTranslatorUtils.isNumericType(ColumnType.ENTITYID));
		assertTrue(SQLTranslatorUtils.isNumericType(ColumnType.FILEHANDLEID));
		assertTrue(SQLTranslatorUtils.isNumericType(ColumnType.INTEGER));
		assertFalse(SQLTranslatorUtils.isNumericType(ColumnType.LARGETEXT));
		assertFalse(SQLTranslatorUtils.isNumericType(ColumnType.LINK));
		assertFalse(SQLTranslatorUtils.isNumericType(ColumnType.STRING));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testIsNumericTypeNull(){
		SQLTranslatorUtils.isNumericType(null);
	}
	
	@Test
	public void testIsNumericAllTypes(){
		// Should work for all types without errors
		for(ColumnType type: ColumnType.values()){
			SQLTranslatorUtils.isNumericType(type);
		}
	}
	
	@Test
	public void testGetColumnTypeForFunctionCount(){
		FunctionType functionType = FunctionType.COUNT;
		ColumnType baseType = ColumnType.BOOLEAN;
		// call under test
		ColumnType lookup = SQLTranslatorUtils.getColumnTypeForFunction(functionType, baseType);
		// count is always integer
		assertEquals(ColumnType.INTEGER, lookup);
	}
	
	@Test
	public void testGetColumnTypeForFunctionCountNullType(){
		FunctionType functionType = FunctionType.COUNT;
		ColumnType baseType = null;
		// call under test
		ColumnType lookup = SQLTranslatorUtils.getColumnTypeForFunction(functionType, baseType);
		// count is always integer
		assertEquals(ColumnType.INTEGER, lookup);
	}
	
	@Test
	public void testGetColumnTypeForFunctionFoundRows(){
		FunctionType functionType = FunctionType.FOUND_ROWS;
		ColumnType baseType = null;
		// call under test
		ColumnType lookup = SQLTranslatorUtils.getColumnTypeForFunction(functionType, baseType);
		// found rows is always integer
		assertEquals(ColumnType.INTEGER, lookup);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetColumnTypeForFunctionAvgNullType(){
		FunctionType functionType = FunctionType.AVG;
		ColumnType baseType = null;
		// call under test
		SQLTranslatorUtils.getColumnTypeForFunction(functionType, baseType);
	}
	
	@Test 
	public void testGetColumnTypeForFunctionAvgNumericType(){
		FunctionType functionType = FunctionType.AVG;
		ColumnType baseType = ColumnType.INTEGER;
		// call under test
		ColumnType lookup = SQLTranslatorUtils.getColumnTypeForFunction(functionType, baseType);
		// avg is always double
		assertEquals(ColumnType.DOUBLE, lookup);
	}
	
	@Test  (expected=IllegalArgumentException.class)
	public void testGetColumnTypeForFunctionAvgNonNumericType(){
		FunctionType functionType = FunctionType.AVG;
		ColumnType baseType = ColumnType.STRING;
		// call under test
		SQLTranslatorUtils.getColumnTypeForFunction(functionType, baseType);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetColumnTypeForFunctionSumNullType(){
		FunctionType functionType = FunctionType.SUM;
		ColumnType baseType = null;
		// call under test
		SQLTranslatorUtils.getColumnTypeForFunction(functionType, baseType);
	}
	
	@Test 
	public void testGetColumnTypeForFunctionSumNumericTypeInteger(){
		FunctionType functionType = FunctionType.SUM;
		ColumnType baseType = ColumnType.INTEGER;
		// call under test
		ColumnType lookup = SQLTranslatorUtils.getColumnTypeForFunction(functionType, baseType);
		// sum is same as input numeric
		assertEquals(baseType, lookup);
	}
	
	@Test 
	public void testGetColumnTypeForFunctionSumNumericTypeDouble(){
		FunctionType functionType = FunctionType.SUM;
		ColumnType baseType = ColumnType.DOUBLE;
		// call under test
		ColumnType lookup = SQLTranslatorUtils.getColumnTypeForFunction(functionType, baseType);
		// sum is same as input numeric
		assertEquals(baseType, lookup);
	}
	
	@Test  (expected=IllegalArgumentException.class)
	public void testGetColumnTypeForFunctionSumNonNumericType(){
		FunctionType functionType = FunctionType.SUM;
		ColumnType baseType = ColumnType.STRING;
		// call under test
		SQLTranslatorUtils.getColumnTypeForFunction(functionType, baseType);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetColumnTypeForFunctionMinNull(){
		FunctionType functionType = FunctionType.MIN;
		ColumnType baseType = null;
		// call under test
		SQLTranslatorUtils.getColumnTypeForFunction(functionType, baseType);
	}
	
	@Test 
	public void testGetColumnTypeForFunctionMin(){
		FunctionType functionType = FunctionType.MIN;
		ColumnType baseType = ColumnType.DOUBLE;
		// call under test
		ColumnType lookup = SQLTranslatorUtils.getColumnTypeForFunction(functionType, baseType);
		// min is same as input
		assertEquals(baseType, lookup);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetColumnTypeForFunctionMaxNull(){
		FunctionType functionType = FunctionType.MAX;
		ColumnType baseType = null;
		// call under test
		SQLTranslatorUtils.getColumnTypeForFunction(functionType, baseType);
	}
	
	@Test 
	public void testGetColumnTypeForFunctionMax(){
		FunctionType functionType = FunctionType.MAX;
		ColumnType baseType = ColumnType.DOUBLE;
		// call under test
		ColumnType lookup = SQLTranslatorUtils.getColumnTypeForFunction(functionType, baseType);
		// min is same as input
		assertEquals(baseType, lookup);
	}
	
	/**
	 * should work for each type without an error
	 */
	@Test
	public void testGetColumnTypeForFunctionAllTypes(){
		ColumnType baseType = ColumnType.INTEGER;
		for(FunctionType functionType: FunctionType.values()){
			SQLTranslatorUtils.getColumnTypeForFunction(functionType, baseType);
		}
	}
	
	@Test
	public void testGetBaseColulmnTypeRowId(){
		when(mockHasQuoteValue.getValueWithoutQuotes()).thenReturn("row_id");
		when(mockHasQuoteValue.isSurrounedeWithQuotes()).thenReturn(false);
		// call under test
		ColumnType type = SQLTranslatorUtils.getBaseColulmnType(mockHasQuoteValue);
		// row_id is always integer
		ColumnType expected = ColumnType.INTEGER;
		assertEquals(expected, type);
	}
	
	@Test
	public void testGetBaseColulmnTypeRowVersion(){
		when(mockHasQuoteValue.getValueWithoutQuotes()).thenReturn("row_version");
		when(mockHasQuoteValue.isSurrounedeWithQuotes()).thenReturn(false);
		// call under test
		ColumnType type = SQLTranslatorUtils.getBaseColulmnType(mockHasQuoteValue);
		// row_version is always integer
		ColumnType expected = ColumnType.INTEGER;
		assertEquals(expected, type);
	}
	
	@Test
	public void testGetBaseColulmnTypeNoQuotes(){
		when(mockHasQuoteValue.getValueWithoutQuotes()).thenReturn("1.23");
		when(mockHasQuoteValue.isSurrounedeWithQuotes()).thenReturn(false);
		// call under test
		ColumnType type = SQLTranslatorUtils.getBaseColulmnType(mockHasQuoteValue);
		ColumnType expected = ColumnType.DOUBLE;
		assertEquals(expected, type);
	}
	
	@Test
	public void testGetBaseColulmnTypeWithQuotes(){
		when(mockHasQuoteValue.getValueWithoutQuotes()).thenReturn("foo");
		when(mockHasQuoteValue.isSurrounedeWithQuotes()).thenReturn(true);
		// call under test
		ColumnType type = SQLTranslatorUtils.getBaseColulmnType(mockHasQuoteValue);
		ColumnType expected = ColumnType.STRING;
		assertEquals(expected, type);
	}
	
	@Test
	public void testGetSelectColumns() throws ParseException{
		Map<String, ColumnModel> columnMap = new HashMap<String, ColumnModel>(1);
		
		DerivedColumn derivedColumn = new TableQueryParser("").derivedColumn();
		
		SelectColumnAndModel results = SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
		assertNotNull(results);
		assertEquals("", results.getName());
		assertEquals(ColumnType.BOOLEAN, results.getColumnType());
		
		
	}
}
