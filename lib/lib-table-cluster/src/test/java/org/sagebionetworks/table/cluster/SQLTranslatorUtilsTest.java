package org.sagebionetworks.table.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.sql.ResultSet;
import java.sql.SQLException;
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
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.FunctionType;
import org.sagebionetworks.table.query.model.HasQuoteValue;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.TableReference;

import com.google.common.collect.Lists;

public class SQLTranslatorUtilsTest {
	
	@Mock
	HasQuoteValue mockHasQuoteValue;
	@Mock
	ResultSet mockResultSet;
	
	Map<String, ColumnModel> columnMap;
	
	ColumnModel columnFoo;
	ColumnModel columnHasSpace;
	ColumnModel columnBar;
	ColumnModel columnId;
	ColumnModel columnSpecial;
	ColumnModel columnDouble;
	
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
		columnDouble = TableModelTestUtils.createColumn(777L, "aDouble", ColumnType.DOUBLE);
		
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
	public void testGetColumnTypeForAllFunctionsBaseTypeInteger(){
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
	
	@Test
	public void testCreateSelectListFromSchema(){
		// call under test.
		SelectList results = SQLTranslatorUtils.createSelectListFromSchema(Lists.newArrayList(columnFoo, columnHasSpace));
		assertNotNull(results);
		assertEquals("foo, \"has space\"", results.toSql());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateSelectListFromSchemaNull(){
		// call under test.
		SQLTranslatorUtils.createSelectListFromSchema(null);
	}
	
	@Test(expected=IllegalStateException.class)
	public void testGetSelectColumnsSelectStar() throws ParseException{
		boolean isAggregate = false;
		SelectList element = new TableQueryParser("*").selectList();
		//  call under test.
		SQLTranslatorUtils.getSelectColumns(element, columnMap, isAggregate);
	}
	
	@Test
	public void testGetSelectColumnsSelectActualColumnsAggregate() throws ParseException{
		boolean isAggregate = true;
		SelectList element = new TableQueryParser("foo, bar").selectList();
		//  call under test.
		List<SelectColumn> results = SQLTranslatorUtils.getSelectColumns(element, columnMap, isAggregate);
		assertNotNull(results);
		assertEquals(2, results.size());
		for (SelectColumn select : results) {
			assertEquals(
					"This is an aggregate so all column ids must be null.",
					null, select.getId());
		}
	}
	
	@Test
	public void testGetSelectColumnsSelectActualColumnsNotAggregate() throws ParseException{
		boolean isAggregate = false;
		SelectList element = new TableQueryParser("foo, bar").selectList();
		//  call under test.
		List<SelectColumn> results = SQLTranslatorUtils.getSelectColumns(element, columnMap, isAggregate);
		assertNotNull(results);
		assertEquals(2, results.size());
		for (SelectColumn select : results) {
			assertNotNull(
					"This is not an aggregate, and all selects match the schema so all columns should have a column ID.",
					select.getId());
		}
	}
	
	@Test
	public void testGetSelectColumnsSelectConstantNotAggregate() throws ParseException{
		boolean isAggregate = false;
		SelectList element = new TableQueryParser("foo, 'some constant'").selectList();
		//  call under test.
		List<SelectColumn> results = SQLTranslatorUtils.getSelectColumns(element, columnMap, isAggregate);
		assertNotNull(results);
		assertEquals(2, results.size());
		for (SelectColumn select : results) {
			assertEquals(
					"This is not an aggregate but since one select does not match the schema, all column Ids should be null.",
					null, select.getId());
		}
	}
	
	@Test
	public void testGetSelectColumnsSelectConstantAggregate() throws ParseException{
		boolean isAggregate = true;
		SelectList element = new TableQueryParser("foo, 'some constant'").selectList();
		//  call under test.
		List<SelectColumn> results = SQLTranslatorUtils.getSelectColumns(element, columnMap, isAggregate);
		assertNotNull(results);
		assertEquals(2, results.size());
		for (SelectColumn select : results) {
			assertEquals(
					"This is an aggregate and one select does not match the schema so all column Ids should be null.",
					null, select.getId());
		}
	}
	
	@Test
	public void testAddRowIdAndVersionToSelect() throws ParseException{
		SelectList element = new TableQueryParser("foo, 'has space'").selectList();
		SelectList results = SQLTranslatorUtils.addRowIdAndVersionToSelect(element);
		assertNotNull(results);
		assertEquals("foo, 'has space', ROW_ID, ROW_VERSION", results.toSql());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testDoAllSelectMatchSchemaNullNull(){
		// call under test
		assertFalse(SQLTranslatorUtils.doAllSelectMatchSchema(null));
	}
	
	@Test
	public void testDoAllSelectMatchSchemaTrue(){
		SelectColumn one = new SelectColumn();
		one.setId("123");
		SelectColumn two = new SelectColumn();
		two.setId("456");
		// call under test
		assertTrue(SQLTranslatorUtils.doAllSelectMatchSchema(Lists.newArrayList(one, two)));
	}
	
	@Test
	public void testDoAllSelectMatchSchemaFalse(){
		SelectColumn one = new SelectColumn();
		one.setId("123");
		SelectColumn two = new SelectColumn();
		two.setId(null);
		// call under test
		assertFalse(SQLTranslatorUtils.doAllSelectMatchSchema(Lists.newArrayList(one, two)));
	}
	
	@Test
	public void testReadRow() throws SQLException{
		
		boolean includesRowIdAndVersion = true;
		
		SelectColumn one = new SelectColumn();
		one.setColumnType(ColumnType.STRING);
		SelectColumn two = new SelectColumn();
		two.setColumnType(ColumnType.BOOLEAN);
		
		List<SelectColumn> selectList = Lists.newArrayList(one, two);
		
		Long rowId = 123L;
		Long rowVersion = 2L;
		// Setup the result set
		when(mockResultSet.getLong(ROW_ID)).thenReturn(rowId);
		when(mockResultSet.getLong(ROW_VERSION)).thenReturn(rowVersion);
		when(mockResultSet.getString(1)).thenReturn("aString");
		when(mockResultSet.getString(2)).thenReturn("1");
		// call under test.
		Row result = SQLTranslatorUtils.readRow(mockResultSet, includesRowIdAndVersion, selectList);
		assertNotNull(result);
		assertEquals(rowId, result.getRowId());
		assertEquals(rowVersion, result.getVersionNumber());
		assertNotNull(result.getValues());
		assertEquals(2, result.getValues().size());
		assertEquals("aString", result.getValues().get(0));
		assertEquals(Boolean.TRUE.toString(), result.getValues().get(1));
		
	}
	
	@Test
	public void testReadRowNoRowId() throws SQLException{
		
		boolean includesRowIdAndVersion = false;
		
		SelectColumn one = new SelectColumn();
		one.setColumnType(ColumnType.STRING);
		SelectColumn two = new SelectColumn();
		two.setColumnType(ColumnType.BOOLEAN);
		
		List<SelectColumn> selectList = Lists.newArrayList(one, two);

		when(mockResultSet.getString(1)).thenReturn("aString");
		when(mockResultSet.getString(2)).thenReturn("0");
		// call under test.
		Row result = SQLTranslatorUtils.readRow(mockResultSet, includesRowIdAndVersion, selectList);
		assertNotNull(result);
		assertEquals(null, result.getRowId());
		assertEquals(null, result.getVersionNumber());
		assertNotNull(result.getValues());
		assertEquals(2, result.getValues().size());
		assertEquals("aString", result.getValues().get(0));
		assertEquals(Boolean.FALSE.toString(), result.getValues().get(1));
		
	}
	
	@Test
	public void testTranslateTableReference(){
		TableReference element = new TableReference("syn123");
		SQLTranslatorUtils.translate(element);
		assertEquals("T123",element.getTableName());
	}
	
	@Test
	public void testTranslateDerivedColumnSimple() throws ParseException{
		DerivedColumn column = new TableQueryParser("foo").derivedColumn();
		SQLTranslatorUtils.translate(column, columnMap);
		assertEquals("_C111_", column.toSql());
	}
	
	@Test
	public void testTranslateDerivedColumnSimpleAs() throws ParseException{
		DerivedColumn column = new TableQueryParser("foo as bar").derivedColumn();
		SQLTranslatorUtils.translate(column, columnMap);
		assertEquals("_C111_ AS bar", column.toSql());
	}
	
	@Test
	public void testTranslateDerivedColumnRowId() throws ParseException{
		DerivedColumn column = new TableQueryParser("row_id").derivedColumn();
		SQLTranslatorUtils.translate(column, columnMap);
		assertEquals("row_id", column.toSql());
	}
	
	@Test
	public void testTranslateDerivedColumnConstant() throws ParseException{
		DerivedColumn column = new TableQueryParser("'constant'").derivedColumn();
		SQLTranslatorUtils.translate(column, columnMap);
		assertEquals("'constant'", column.toSql());
	}
	
	@Test
	public void testTranslateDerivedColumnDouble() throws ParseException{
		DerivedColumn column = new TableQueryParser("'aDouble'").derivedColumn();
		SQLTranslatorUtils.translate(column, columnMap);
		assertEquals("'constant'", column.toSql());
	}
}
