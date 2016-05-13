package org.sagebionetworks.table.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.FunctionType;
import org.sagebionetworks.table.query.model.HasQuoteValue;

import com.google.common.collect.Lists;

public class SQLTranslatorUtilsTest {
	
	@Mock
	HasQuoteValue mockHasQuoteValue;
	
	Map<String, ColumnModel> columnMap;
	
	ColumnModel columnFoo;
	ColumnModel columnHasSpace;
	ColumnModel columnBar;
	ColumnModel columnId;
	ColumnModel columnSpecial;
	
	List<ColumnModel> schema;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);

		columnFoo = TableModelTestUtils.createColumn(111L, "foo", ColumnType.STRING);
		columnHasSpace = TableModelTestUtils.createColumn(222L, "has space", ColumnType.STRING);
		columnBar = TableModelTestUtils.createColumn(333L, "bar", ColumnType.STRING);
		columnId = TableModelTestUtils.createColumn(444L, "id", ColumnType.INTEGER);
		String specialChars = "Specialchars~!@#$%^^&*()_+|}{:?></.,;'[]\'";
		columnSpecial = TableModelTestUtils.createColumn(555L, specialChars, ColumnType.DOUBLE);
		
		schema = Lists.newArrayList(columnFoo, columnHasSpace, columnBar, columnId, columnSpecial);
		// setup the map
		columnMap = new HashMap<String, ColumnModel>(schema.size());
		for(ColumnModel cm: schema){
			columnMap.put(cm.getName(), cm);
		}
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
	public void testGetSelectColumnsConstantString() throws ParseException{
		DerivedColumn derivedColumn = new TableQueryParser("'constant'").derivedColumn();
		// call under test
		SelectColumn results = SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
		assertNotNull(results);
		assertEquals("constant", results.getName());
		assertEquals(ColumnType.STRING, results.getColumnType());
		assertEquals(null, results.getId());
	}
	
	@Test
	public void testGetSelectColumnsConstantDouble() throws ParseException{
		DerivedColumn derivedColumn = new TableQueryParser("1.23").derivedColumn();
		// call under test
		SelectColumn results = SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
		assertNotNull(results);
		assertEquals("1.23", results.getName());
		assertEquals(ColumnType.DOUBLE, results.getColumnType());
		assertEquals(null, results.getId());
	}
	
	@Test
	public void testGetSelectColumnsRowIdLower() throws ParseException{
		DerivedColumn derivedColumn = new TableQueryParser("row_id").derivedColumn();
		// call under test
		SelectColumn results = SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
		assertNotNull(results);
		assertEquals("row_id", results.getName());
		assertEquals(ColumnType.INTEGER, results.getColumnType());
		assertEquals(null, results.getId());

	}
	
	@Test
	public void testGetSelectColumnsRowIdUpper() throws ParseException{
		DerivedColumn derivedColumn = new TableQueryParser("ROW_ID").derivedColumn();
		// call under test
		SelectColumn results = SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
		assertNotNull(results);
		assertEquals("ROW_ID", results.getName());
		assertEquals(ColumnType.INTEGER, results.getColumnType());
		assertEquals(null, results.getId());
	}
	
	@Test
	public void testGetSelectColumnsCountRowId() throws ParseException{
		DerivedColumn derivedColumn = new TableQueryParser("count(row_id)").derivedColumn();
		// call under test
		SelectColumn results = SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
		assertNotNull(results);
		assertEquals("COUNT(row_id)", results.getName());
		assertEquals(ColumnType.INTEGER, results.getColumnType());
		assertEquals(null, results.getId());
	}
	
	@Test
	public void testGetSelectColumnsRowVersionLower() throws ParseException{
		DerivedColumn derivedColumn = new TableQueryParser("row_version").derivedColumn();
		// call under test
		SelectColumn results = SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
		assertNotNull(results);
		assertEquals("row_version", results.getName());
		assertEquals(ColumnType.INTEGER, results.getColumnType());
		assertEquals(null, results.getId());
	}
	
	@Test
	public void testGetSelectColumnsRowVersionUpper() throws ParseException{
		DerivedColumn derivedColumn = new TableQueryParser("ROW_VERSION").derivedColumn();
		// call under test
		SelectColumn results = SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
		assertNotNull(results);
		assertEquals("ROW_VERSION", results.getName());
		assertEquals(ColumnType.INTEGER, results.getColumnType());
		assertEquals(null, results.getId());
	}
	
	@Test
	public void testGetSelectColumnsCountStar() throws ParseException{
		DerivedColumn derivedColumn = new TableQueryParser("count(*)").derivedColumn();
		// call under test
		SelectColumn results = SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
		assertNotNull(results);
		assertEquals("COUNT(*)", results.getName());
		assertEquals(ColumnType.INTEGER, results.getColumnType());
		assertEquals(null, results.getId());
	}
	
	@Test
	public void testGetSelectColumnsCountStarAs() throws ParseException{
		DerivedColumn derivedColumn = new TableQueryParser("count(*) as \"has space\"").derivedColumn();
		// call under test
		SelectColumn results = SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
		assertNotNull(results);
		assertEquals("has space", results.getName());
		assertEquals(ColumnType.INTEGER, results.getColumnType());
		assertEquals(null, results.getId());
	}
	
	@Test
	public void testGetSelectColumnsCountNoMatch() throws ParseException{
		DerivedColumn derivedColumn = new TableQueryParser("count(no_match)").derivedColumn();
		// call under test
		SelectColumn results = SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
		assertNotNull(results);
		assertEquals("COUNT(no_match)", results.getName());
		assertEquals(ColumnType.INTEGER, results.getColumnType());
		assertEquals(null, results.getId());
	}
	
	@Test
	public void testGetSelectColumnsCountMatch() throws ParseException{
		DerivedColumn derivedColumn = new TableQueryParser("count('has space')").derivedColumn();
		// call under test
		SelectColumn results = SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
		assertNotNull(results);
		assertEquals("COUNT('has space')", results.getName());
		assertEquals(ColumnType.INTEGER, results.getColumnType());
		assertEquals(null, results.getId());
	}
	
	@Test
	public void testGetSelectColumnsCountMatchAs() throws ParseException{
		DerivedColumn derivedColumn = new TableQueryParser("count('has space') as bar").derivedColumn();
		// call under test
		SelectColumn results = SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
		assertNotNull(results);
		assertEquals("bar", results.getName());
		assertEquals(ColumnType.INTEGER, results.getColumnType());
		assertEquals(null, results.getId());
	}
	
	@Test
	public void testGetSelectColumnsSimpleMatch() throws ParseException{
		DerivedColumn derivedColumn = new TableQueryParser("foo").derivedColumn();
		// call under test
		SelectColumn results = SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
		assertNotNull(results);
		assertEquals("foo", results.getName());
		assertEquals(columnFoo.getColumnType(), results.getColumnType());
		assertEquals(columnFoo.getId(), results.getId());
	}
	
	@Test
	public void testGetSelectColumnsMatchAs() throws ParseException{
		DerivedColumn derivedColumn = new TableQueryParser("bar as foo").derivedColumn();
		// call under test
		SelectColumn results = SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
		assertNotNull(results);
		assertEquals("foo", results.getName());
		assertEquals(columnBar.getColumnType(), results.getColumnType());
		assertEquals(null, results.getId());
	}
	
	@Test
	public void testGetSelectColumnsSum() throws ParseException{
		DerivedColumn derivedColumn = new TableQueryParser("sum( id )").derivedColumn();
		// call under test
		SelectColumn results = SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
		assertNotNull(results);
		assertEquals("SUM(id)", results.getName());
		assertEquals(columnId.getColumnType(), results.getColumnType());
		assertEquals(null, results.getId());
	}
	
	@Test
	public void testGetSelectColumnsMax() throws ParseException{
		DerivedColumn derivedColumn = new TableQueryParser("max( bar )").derivedColumn();
		// call under test
		SelectColumn results = SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
		assertNotNull(results);
		assertEquals("MAX(bar)", results.getName());
		assertEquals(columnBar.getColumnType(), results.getColumnType());
		assertEquals(null, results.getId());
	}
	
	@Test
	public void testGetSelectColumnsAvg() throws ParseException{
		DerivedColumn derivedColumn = new TableQueryParser("avg( id )").derivedColumn();
		// call under test
		SelectColumn results = SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
		assertNotNull(results);
		assertEquals("AVG(id)", results.getName());
		assertEquals(ColumnType.DOUBLE, results.getColumnType());
		assertEquals(null, results.getId());
	}
	
	@Test
	public void testGetSelectColumnsSpecial() throws ParseException{
		DerivedColumn derivedColumn = new TableQueryParser("\""+columnSpecial.getName()+"\"").derivedColumn();
		// call under test
		SelectColumn results = SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
		assertNotNull(results);
		assertEquals(columnSpecial.getName(), results.getName());
		assertEquals(ColumnType.DOUBLE, results.getColumnType());
		assertEquals(columnSpecial.getId(), results.getId());
	}
}
