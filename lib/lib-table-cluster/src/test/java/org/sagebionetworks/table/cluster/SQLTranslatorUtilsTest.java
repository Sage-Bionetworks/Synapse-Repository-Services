package org.sagebionetworks.table.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ETAG;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
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
import org.sagebionetworks.table.query.model.ActualIdentifier;
import org.sagebionetworks.table.query.model.BooleanPrimary;
import org.sagebionetworks.table.query.model.CharacterStringLiteral;
import org.sagebionetworks.table.query.model.ColumnNameReference;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.ExactNumericLiteral;
import org.sagebionetworks.table.query.model.FunctionReturnType;
import org.sagebionetworks.table.query.model.GeneralLiteral;
import org.sagebionetworks.table.query.model.GroupByClause;
import org.sagebionetworks.table.query.model.HasPredicate;
import org.sagebionetworks.table.query.model.Pagination;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.RegularIdentifier;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.TableReference;
import org.sagebionetworks.table.query.model.UnsignedLiteral;
import org.sagebionetworks.table.query.model.UnsignedNumericLiteral;
import org.sagebionetworks.table.query.model.ValueExpressionPrimary;

import com.google.common.collect.Lists;

public class SQLTranslatorUtilsTest {
	
	@Mock
	ColumnNameReference mockHasQuoteValue;
	@Mock
	ResultSet mockResultSet;
	
	Map<String, ColumnModel> columnMap;
	
	ColumnModel columnFoo;
	ColumnModel columnHasSpace;
	ColumnModel columnBar;
	ColumnModel columnId;
	ColumnModel columnSpecial;
	ColumnModel columnDouble;
	ColumnModel columnDate;
	
	List<ColumnModel> schema;
	
	List<SelectColumn> selectList;
	ColumnTypeInfo[] infoArray;
	Long rowId;
	Long rowVersion;
	String etag;
	
	@Before
	public void before() throws Exception {
		MockitoAnnotations.initMocks(this);

		columnFoo = TableModelTestUtils.createColumn(111L, "foo", ColumnType.STRING);
		columnHasSpace = TableModelTestUtils.createColumn(222L, "has space", ColumnType.STRING);
		columnBar = TableModelTestUtils.createColumn(333L, "bar", ColumnType.STRING);
		columnId = TableModelTestUtils.createColumn(444L, "id", ColumnType.INTEGER);
		String specialChars = "Specialchars~!@#$%^^&*()_+|}{:?></.,;'[]\'";
		columnSpecial = TableModelTestUtils.createColumn(555L, specialChars, ColumnType.DOUBLE);
		columnDouble = TableModelTestUtils.createColumn(777L, "aDouble", ColumnType.DOUBLE);
		columnDate = TableModelTestUtils.createColumn(888L, "aDate", ColumnType.DATE);
		
		schema = Lists.newArrayList(columnFoo, columnHasSpace, columnBar, columnId, columnSpecial, columnDouble);
		// setup the map
		columnMap = new HashMap<String, ColumnModel>(schema.size());
		for(ColumnModel cm: schema){
			columnMap.put(cm.getName(), cm);
		}
		
		SelectColumn one = new SelectColumn();
		one.setColumnType(ColumnType.STRING);
		SelectColumn two = new SelectColumn();
		two.setColumnType(ColumnType.BOOLEAN);
		
		selectList = Lists.newArrayList(one, two);
		infoArray = SQLTranslatorUtils.getColumnTypeInfoArray(selectList);
		
		rowId = 123L;
		rowVersion = 2L;
		etag = "anEtag";
		// Setup the result set
		when(mockResultSet.getLong(ROW_ID)).thenReturn(rowId);
		when(mockResultSet.getLong(ROW_VERSION)).thenReturn(rowVersion);
		when(mockResultSet.getString(ROW_ETAG)).thenReturn(etag);
		when(mockResultSet.getString(1)).thenReturn("aString");
		when(mockResultSet.getString(2)).thenReturn("true");
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
		assertTrue(SQLTranslatorUtils.isNumericType(ColumnType.USERID));
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
	public void testGetBaseColulmnTypeRowId(){
		when(mockHasQuoteValue.toSqlWithoutQuotes()).thenReturn("row_id");
		when(mockHasQuoteValue.hasQuotesRecursive()).thenReturn(false);
		// call under test
		ColumnType type = SQLTranslatorUtils.getBaseColulmnType(mockHasQuoteValue);
		// row_id is always integer
		ColumnType expected = ColumnType.INTEGER;
		assertEquals(expected, type);
	}
	
	@Test
	public void testGetBaseColulmnTypeRowVersion(){
		when(mockHasQuoteValue.toSqlWithoutQuotes()).thenReturn("row_version");
		when(mockHasQuoteValue.hasQuotesRecursive()).thenReturn(false);
		// call under test
		ColumnType type = SQLTranslatorUtils.getBaseColulmnType(mockHasQuoteValue);
		// row_version is always integer
		ColumnType expected = ColumnType.INTEGER;
		assertEquals(expected, type);
	}
	
	@Test
	public void testGetBaseColulmnTypeNoQuotes(){
		when(mockHasQuoteValue.toSqlWithoutQuotes()).thenReturn("1.23");
		when(mockHasQuoteValue.hasQuotesRecursive()).thenReturn(false);
		// call under test
		ColumnType type = SQLTranslatorUtils.getBaseColulmnType(mockHasQuoteValue);
		ColumnType expected = ColumnType.DOUBLE;
		assertEquals(expected, type);
	}
	
	@Test
	public void testGetBaseColulmnTypeWithQuotes(){
		when(mockHasQuoteValue.toSqlWithoutQuotes()).thenReturn("foo");
		when(mockHasQuoteValue.hasQuotesRecursive()).thenReturn(true);
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
	public void testGetSelectColumnsMySqlFunction() throws ParseException{
		DerivedColumn derivedColumn = new TableQueryParser("from_unixtime(foo)").derivedColumn();
		// call under test
		SelectColumn results = SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
		assertNotNull(results);
		assertEquals("FROM_UNIXTIME(foo)", results.getName());
		assertEquals(ColumnType.STRING, results.getColumnType());
		assertEquals(null, results.getId());
	}

	@Test
	public void testGetSelectColumnsSimpleMismatch() throws ParseException{
		DerivedColumn derivedColumn = new TableQueryParser("fo0").derivedColumn();
		// call under test
		try {
			SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
			fail("Should throw IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			String message = e.getMessage();
			assertTrue(message.contains("fo0"));
			assertTrue(message.contains("Unknown column"));
		}
	}
	
	@Test
	public void testCreateSelectListFromSchema(){
		// call under test.
		SelectList results = SQLTranslatorUtils.createSelectListFromSchema(Lists.newArrayList(columnFoo, columnHasSpace));
		assertNotNull(results);
		assertEquals("\"foo\", \"has space\"", results.toSql());
	}
	
	@Test
	public void testCreateSelectListFromSchemaPLFM_4161(){
		ColumnModel cm = new ColumnModel();
		cm.setName("5ormore");
		cm.setColumnType(ColumnType.INTEGER);
		cm.setId("111");
		// call under test.
		SelectList results = SQLTranslatorUtils.createSelectListFromSchema(Lists.newArrayList(cm));
		assertNotNull(results);
		assertEquals("\"5ormore\"", results.toSql());
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
		boolean includeEtag = false;
		SelectList element = new TableQueryParser("foo, 'has space'").selectList();
		SelectList results = SQLTranslatorUtils.addMetadataColumnsToSelect(element, includeEtag);
		assertNotNull(results);
		assertEquals("foo, 'has space', ROW_ID, ROW_VERSION", results.toSql());
	}
	
	@Test
	public void testAddRowIdAndVersionToSelectWithEtag() throws ParseException{
		boolean includeEtag = true;
		SelectList element = new TableQueryParser("foo, 'has space'").selectList();
		SelectList results = SQLTranslatorUtils.addMetadataColumnsToSelect(element, includeEtag);
		assertNotNull(results);
		assertEquals("foo, 'has space', ROW_ID, ROW_VERSION, ROW_ETAG", results.toSql());
	}
	
	@Test
	public void testReadWithHeadersWithEtagRow() throws SQLException{
		boolean withHeaders = true;
		boolean withEtag = true;
		// call under test.
		Row result = SQLTranslatorUtils.readRow(mockResultSet, withHeaders, withEtag, infoArray);
		verify(mockResultSet).getLong(ROW_ID);
		verify(mockResultSet).getLong(ROW_VERSION);
		verify(mockResultSet).getString(ROW_ETAG);
		assertNotNull(result);
		assertEquals(rowId, result.getRowId());
		assertEquals(rowVersion, result.getVersionNumber());
		assertNotNull(result.getValues());
		assertEquals(2, result.getValues().size());
		assertEquals("aString", result.getValues().get(0));
		assertEquals(Boolean.TRUE.toString(), result.getValues().get(1));
	}
	
	@Test
	public void testReadWithoutHeadersWithEtagRow() throws SQLException{
		boolean withHeaders = false;
		boolean withEtag = true;
		// call under test.
		Row result = SQLTranslatorUtils.readRow(mockResultSet, withHeaders, withEtag, infoArray);
		verify(mockResultSet, never()).getLong(ROW_ID);
		verify(mockResultSet, never()).getLong(ROW_VERSION);
		verify(mockResultSet, never()).getString(ROW_ETAG);
		assertNotNull(result);
		assertEquals(null, result.getRowId());
		assertEquals(null, result.getVersionNumber());
		assertNotNull(result.getValues());
		assertEquals(2, result.getValues().size());
		assertEquals("aString", result.getValues().get(0));
		assertEquals(Boolean.TRUE.toString(), result.getValues().get(1));
	}
	
	@Test
	public void testReadWithoutHeadersWithoutEtagRow() throws SQLException{
		boolean withHeaders = false;
		boolean withEtag = false;
		// call under test.
		Row result = SQLTranslatorUtils.readRow(mockResultSet, withHeaders, withEtag, infoArray);
		verify(mockResultSet, never()).getLong(ROW_ID);
		verify(mockResultSet, never()).getLong(ROW_VERSION);
		verify(mockResultSet, never()).getString(ROW_ETAG);
		assertNotNull(result);
		assertEquals(null, result.getRowId());
		assertEquals(null, result.getVersionNumber());
		assertNotNull(result.getValues());
		assertEquals(2, result.getValues().size());
		assertEquals("aString", result.getValues().get(0));
		assertEquals(Boolean.TRUE.toString(), result.getValues().get(1));
	}
	
	@Test
	public void testReadWithHeadersWithoutEtagRow() throws SQLException{
		boolean withHeaders = true;
		boolean withEtag = false;
		// call under test.
		Row result = SQLTranslatorUtils.readRow(mockResultSet, withHeaders, withEtag, infoArray);
		verify(mockResultSet).getLong(ROW_ID);
		verify(mockResultSet).getLong(ROW_VERSION);
		verify(mockResultSet, never()).getString(ROW_ETAG);
		assertNotNull(result);
		assertEquals(rowId, result.getRowId());
		assertEquals(rowVersion, result.getVersionNumber());
		assertNotNull(result.getValues());
		assertEquals(2, result.getValues().size());
		assertEquals("aString", result.getValues().get(0));
		assertEquals(Boolean.TRUE.toString(), result.getValues().get(1));
	}	
	
	@Test
	public void testTranslateTableReference() throws ParseException{
		TableReference element = new TableQueryParser("syn123").tableReference();
		SQLTranslatorUtils.translate(element);
		assertEquals("T123",element.getTableName());
	}
	
	@Test
	public void testTranslateTableReferenceVerion() throws ParseException{
		TableReference element = new TableQueryParser("syn123.456").tableReference();
		SQLTranslatorUtils.translate(element);
		assertEquals("T123_456",element.getTableName());
	}
	
	@Test
	public void testTranslateSelectSimple() throws ParseException{
		ValueExpressionPrimary column = new TableQueryParser("foo").valueExpressionPrimary();
		SQLTranslatorUtils.translateSelect(column, columnMap);
		assertEquals("_C111_", column.toSql());
	}
	
	@Test
	public void testTranslateSelectFunction() throws ParseException{
		ValueExpressionPrimary column = new TableQueryParser("max(foo)").valueExpressionPrimary();
		SQLTranslatorUtils.translateSelect(column, columnMap);
		assertEquals("MAX(_C111_)", column.toSql());
	}
	
	@Test
	public void testTranslateSelectRowId() throws ParseException{
		ValueExpressionPrimary column = new TableQueryParser("row_id").valueExpressionPrimary();
		SQLTranslatorUtils.translateSelect(column, columnMap);
		assertEquals("row_id", column.toSql());
	}
	
	@Test
	public void testTranslateSelectConstant() throws ParseException{
		ValueExpressionPrimary column = new TableQueryParser("'constant'").valueExpressionPrimary();
		SQLTranslatorUtils.translateSelect(column, columnMap);
		assertEquals("'constant'", column.toSql());
	}
	
	@Test
	public void testTranslateSelectDouble() throws ParseException{
		ValueExpressionPrimary column = new TableQueryParser("aDouble").valueExpressionPrimary();
		SQLTranslatorUtils.translateSelect(column, columnMap);
		assertEquals("CASE WHEN _DBL_C777_ IS NULL THEN _C777_ ELSE _DBL_C777_ END", column.toSql());
	}
	
	@Test
	public void testTranslateSelectDoubleFunction() throws ParseException{
		ValueExpressionPrimary column = new TableQueryParser("sum(aDouble)").valueExpressionPrimary();
		SQLTranslatorUtils.translateSelect(column, columnMap);
		assertEquals("SUM(_C777_)", column.toSql());
	}
	
	@Test
	public void testTranslateOrderBySimple() throws ParseException{
		ValueExpressionPrimary column = new TableQueryParser("foo").valueExpressionPrimary();
		SQLTranslatorUtils.translateOrderBy(column, columnMap);
		assertEquals("_C111_", column.toSql());
	}
	
	@Test
	public void testTranslateOrderByFunction() throws ParseException{
		ValueExpressionPrimary column = new TableQueryParser("max(foo)").valueExpressionPrimary();
		SQLTranslatorUtils.translateOrderBy(column, columnMap);
		assertEquals("MAX(_C111_)", column.toSql());
	}
	
	@Test
	public void testTranslateOrderByRowId() throws ParseException{
		ValueExpressionPrimary column = new TableQueryParser("row_id").valueExpressionPrimary();
		SQLTranslatorUtils.translateOrderBy(column, columnMap);
		assertEquals("row_id", column.toSql());
	}
	
	@Test
	public void testTranslateOrderByConstant() throws ParseException{
		ValueExpressionPrimary column = new TableQueryParser("'constant'").valueExpressionPrimary();
		SQLTranslatorUtils.translateOrderBy(column, columnMap);
		assertEquals("'constant'", column.toSql());
	}
	
	@Test
	public void testTranslateOrderByDouble() throws ParseException{
		ValueExpressionPrimary column = new TableQueryParser("aDouble").valueExpressionPrimary();
		SQLTranslatorUtils.translateOrderBy(column, columnMap);
		assertEquals("_C777_", column.toSql());
	}
	
	@Test
	public void testTranslateSelectOrderByFunction() throws ParseException{
		ValueExpressionPrimary column = new TableQueryParser("sum(aDouble)").valueExpressionPrimary();
		SQLTranslatorUtils.translateOrderBy(column, columnMap);
		assertEquals("SUM(_C777_)", column.toSql());
	}
	
	@Test
	public void testReplaceBooleanFunctionIsNaN() throws ParseException{
		BooleanPrimary element = new TableQueryParser("isNaN(aDouble)").booleanPrimary();
		SQLTranslatorUtils.replaceBooleanFunction(element, columnMap);
		assertEquals("( _DBL_C777_ IS NOT NULL AND _DBL_C777_ = 'NaN' )", element.toSql());
	}
	
	@Test
	public void testReplaceBooleanFunctionIsInfinity() throws ParseException{
		BooleanPrimary element = new TableQueryParser("isInfinity(aDouble)").booleanPrimary();
		SQLTranslatorUtils.replaceBooleanFunction(element, columnMap);
		assertEquals("( _DBL_C777_ IS NOT NULL AND _DBL_C777_ IN ( '-Infinity', 'Infinity' ) )", element.toSql());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testReplaceBooleanFunctionNonDoubleColumn() throws ParseException{
		BooleanPrimary element = new TableQueryParser("isInfinity(id)").booleanPrimary();
		SQLTranslatorUtils.replaceBooleanFunction(element, columnMap);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testReplaceBooleanFunctionUnknownColumn() throws ParseException{
		BooleanPrimary element = new TableQueryParser("isInfinity(someUnknown)").booleanPrimary();
		SQLTranslatorUtils.replaceBooleanFunction(element, columnMap);
	}
	
	@Test
	public void testReplaceBooleanFunctionNotBooleanFunction() throws ParseException{
		BooleanPrimary element = new TableQueryParser("id = 123").booleanPrimary();
		SQLTranslatorUtils.replaceBooleanFunction(element, columnMap);
		assertEquals("Non-BooleanFunctions should not be changed by this method.","id = 123", element.toSql());
	}
	
	@Test
	public void testReplaceBooleanFunctionSearchCondition() throws ParseException{
		BooleanPrimary element = new TableQueryParser("(id = 123 OR id = 456)").booleanPrimary();
		SQLTranslatorUtils.replaceBooleanFunction(element, columnMap);
		assertEquals("SearchConditions should not be changed by this method.","( id = 123 OR id = 456 )", element.toSql());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testTranslateRightHandeSideNullElement(){
		UnsignedLiteral element = null;
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateRightHandeSide(element, columnFoo, parameters);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testTranslateRightHandeSideNullParameters() throws ParseException{
		UnsignedLiteral element = new TableQueryParser("'aString'").unsignedLiteral();
		Map<String, Object> parameters = null;
		SQLTranslatorUtils.translateRightHandeSide(element, columnFoo, parameters);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testTranslateRightHandeSideNullColumn() throws ParseException{
		UnsignedLiteral element = new TableQueryParser("'aString'").unsignedLiteral();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateRightHandeSide(element, null, parameters);
	}
	
	@Test
	public void testTranslateRightHandeSideString() throws ParseException{
		UnsignedLiteral element = new TableQueryParser("'aString'").unsignedLiteral();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateRightHandeSide(element, columnFoo, parameters);
		assertEquals(":b0", element.toSqlWithoutQuotes());
		assertEquals("aString", parameters.get("b0"));
	}
	
	
	@Test
	public void testTranslateRightHandeSideInteger() throws ParseException{
		UnsignedLiteral element = new TableQueryParser("123456").unsignedLiteral();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateRightHandeSide(element, columnId, parameters);
		assertEquals(":b0", element.toSqlWithoutQuotes());
		assertEquals(new Long(123456), parameters.get("b0"));
	}
	
	@Test
	public void testTranslateRightHandeSideIntegerLikeValue() throws ParseException{
		UnsignedLiteral element = new TableQueryParser("'12345%'").unsignedLiteral();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateRightHandeSide(element, columnId, parameters);
		assertEquals(":b0", element.toSqlWithoutQuotes());
		assertEquals("12345%", parameters.get("b0"));
	}
	
	@Test
	public void testTranslateRightHandeSideDouble() throws ParseException{
		UnsignedLiteral element = new TableQueryParser("1.45").unsignedLiteral();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateRightHandeSide(element, columnDouble, parameters);
		assertEquals(":b0", element.toSqlWithoutQuotes());
		assertEquals(new Double(1.45), parameters.get("b0"));
	}
	
	@Test
	public void testTranslateRightHandeSideDateString() throws ParseException{
		UnsignedLiteral element = new TableQueryParser("'16-01-29 13:55:33.999'").unsignedLiteral();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateRightHandeSide(element, columnDate, parameters);
		assertEquals(":b0", element.toSqlWithoutQuotes());
		assertEquals(new Long(1454075733999L), parameters.get("b0"));
	}
	
	@Test
	public void testTranslateRightHandeSideDateEpoch() throws ParseException{
		UnsignedLiteral element = new TableQueryParser("1454075733999").unsignedLiteral();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateRightHandeSide(element, columnDate, parameters);
		assertEquals(":b0", element.toSqlWithoutQuotes());
		assertEquals(new Long(1454075733999L), parameters.get("b0"));
	}
	
	@Test
	public void testTranslateRightHandeSideInterval() throws ParseException{
		UnsignedLiteral element = new TableQueryParser("INTERVAL 3 MONTH").unsignedLiteral();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateRightHandeSide(element, columnDate, parameters);
		assertEquals("INTERVAL 3 MONTH", element.toSqlWithoutQuotes());
	}
	
	@Test
	public void testTranslateGroupByMultiple() throws ParseException{
		GroupByClause element = new TableQueryParser("group by foo, id").groupByClause();
		SQLTranslatorUtils.translate(element, columnMap);
		assertEquals("GROUP BY _C111_, _C444_", element.toSql());
	}
	
	@Test
	public void testTranslateGroupByDouble() throws ParseException{
		GroupByClause element = new TableQueryParser("group by aDouble").groupByClause();
		SQLTranslatorUtils.translate(element, columnMap);
		assertEquals("GROUP BY _C777_", element.toSql());
	}
	
	@Test
	public void testTranslateGroupByQuotes() throws ParseException{
		GroupByClause element = new TableQueryParser("group by \""+columnSpecial.getName()+"\"").groupByClause();
		SQLTranslatorUtils.translate(element, columnMap);
		assertEquals("GROUP BY _C555_", element.toSql());
	}
	
	@Test
	public void testTranslateGroupByUnknown() throws ParseException{
		GroupByClause element = new TableQueryParser("group by doesNotExist").groupByClause();
		SQLTranslatorUtils.translate(element, columnMap);
		assertEquals("GROUP BY doesNotExist", element.toSql());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testTranslateGroupByNull() throws ParseException{
		GroupByClause element = null;
		SQLTranslatorUtils.translate(element, columnMap);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testTranslateGroupByMapNull() throws ParseException{
		GroupByClause element = new TableQueryParser("group by doesNotExist").groupByClause();
		SQLTranslatorUtils.translate(element, null);
	}
	
	@Test
	public void testTranslateHasPredicate() throws ParseException{
		Predicate element = new TableQueryParser("id <> 3").predicate();
		HasPredicate hasPredicate = element.getFirstElementOfType(HasPredicate.class);
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
		assertEquals("_C444_ <> :b0",element.toSql());
		assertEquals(new Long(3), parameters.get("b0"));
	}
	
	@Test
	public void testTranslateHasPredicateUnknownColumn() throws ParseException{
		Predicate element = new TableQueryParser("_D999_ IS NOT NULL").predicate();
		HasPredicate hasPredicate = element.getFirstElementOfType(HasPredicate.class);
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
		assertEquals("_D999_ IS NOT NULL",element.toSql());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testTranslateHasPredicateNullElement() throws ParseException{
		HasPredicate hasPredicate = null;
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testTranslateHasPredicateNullParameters() throws ParseException{
		Predicate element = new TableQueryParser("id <> 3").predicate();
		HasPredicate hasPredicate = element.getFirstElementOfType(HasPredicate.class);
		Map<String, Object> parameters = null;
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testTranslateHasPredicateNullMap() throws ParseException{
		Predicate element = new TableQueryParser("id <> 3").predicate();
		HasPredicate hasPredicate = element.getFirstElementOfType(HasPredicate.class);
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translate(hasPredicate, parameters, null);
	}
	
	@Test
	public void testTranslatePagination() throws ParseException{
		Pagination element = new TableQueryParser("limit 1 offset 9").pagination();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translate(element, parameters);
		assertEquals("LIMIT :b0 OFFSET :b1", element.toSql());
		assertEquals(new Long(1), parameters.get("b0"));
		assertEquals(new Long(9), parameters.get("b1"));
	}
	
	@Test
	public void testTranslatePaginationNoOffset() throws ParseException{
		Pagination element = new TableQueryParser("limit 1").pagination();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translate(element, parameters);
		assertEquals("LIMIT :b0", element.toSql());
		assertEquals(new Long(1), parameters.get("b0"));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testTranslatePaginationNull() throws ParseException{
		Pagination element = null;
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translate(element, parameters);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testTranslatePaginationParametersNull() throws ParseException{
		Pagination element = new TableQueryParser("limit 1").pagination();
		Map<String, Object> parameters = null;
		SQLTranslatorUtils.translate(element, parameters);
	}
	
	@Test
	public void testTranslateModelSimple() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT _C111_ FROM T123",element.toSql());
	}
	
	@Test
	public void testTranslateModelSelectFunction() throws ParseException{
		QuerySpecification element = new TableQueryParser("select sum(foo) from syn123").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT SUM(_C111_) FROM T123",element.toSql());
	}
	
	@Test
	public void testTranslateModelSelectDouble() throws ParseException{
		QuerySpecification element = new TableQueryParser("select aDouble from syn123").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT CASE WHEN _DBL_C777_ IS NULL THEN _C777_ ELSE _DBL_C777_ END FROM T123",element.toSql());
	}
	
	@Test
	public void testTranslateModelSelectDoubleFunction() throws ParseException{
		QuerySpecification element = new TableQueryParser("select sum(aDouble) from syn123").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT SUM(_C777_) FROM T123",element.toSql());
	}

	
	@Test
	public void testTranslateModelWhere() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123 where id > 2").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT _C111_ FROM T123 WHERE _C444_ > :b0",element.toSql());
		assertEquals(new Long(2), parameters.get("b0"));
	}
	
	@Test
	public void testTranslateModelWhereBetween() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123 where id between '1' and 2").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT _C111_ FROM T123 WHERE _C444_ BETWEEN :b0 AND :b1",element.toSql());
		assertEquals(new Long(1), parameters.get("b0"));
		assertEquals(new Long(2), parameters.get("b1"));
	}
	
	@Test
	public void testTranslateModelWhereIn() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123 where id in ('1',2,3)").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT _C111_ FROM T123 WHERE _C444_ IN ( :b0, :b1, :b2 )",element.toSql());
		assertEquals(new Long(1), parameters.get("b0"));
		assertEquals(new Long(2), parameters.get("b1"));
		assertEquals(new Long(3), parameters.get("b2"));
	}
	
	@Test
	public void testTranslateModelWhereLike() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123 where id like '%3'").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT _C111_ FROM T123 WHERE _C444_ LIKE :b0",element.toSql());
		assertEquals("%3", parameters.get("b0"));
	}
	
	@Test
	public void testTranslateModelWhereNull() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123 where id is not null").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT _C111_ FROM T123 WHERE _C444_ IS NOT NULL",element.toSql());
	}
	
	@Test
	public void testTranslateModelWhereIsTrue() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123 where id is true").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT _C111_ FROM T123 WHERE _C444_ IS TRUE",element.toSql());
	}
	
	@Test
	public void testTranslateModelWhereIsNaN() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123 where isNaN(aDouble)").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT _C111_ FROM T123 WHERE ( _DBL_C777_ IS NOT NULL AND _DBL_C777_ = 'NaN' )",element.toSql());
	}
	
	@Test
	public void testTranslateModelWhereIsInfinity() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123 where isInfinity(aDouble)").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT _C111_ FROM T123 WHERE ( _DBL_C777_ IS NOT NULL AND _DBL_C777_ IN ( '-Infinity', 'Infinity' ) )",element.toSql());
	}
	
	@Test
	public void testTranslateModelGroupBy() throws ParseException{
		QuerySpecification element = new TableQueryParser("select bar, count(foo) from syn123 group by bar").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT _C333_, COUNT(_C111_) FROM T123 GROUP BY _C333_",element.toSql());
	}
	
	@Test
	public void testTranslateModelOrderBy() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123 order by bar").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT _C111_ FROM T123 ORDER BY _C333_",element.toSql());
	}
	
	@Test
	public void testTranslateModelOrderByFunction() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123 order by max(bar)").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT _C111_ FROM T123 ORDER BY MAX(_C333_)",element.toSql());
	}
	
	@Test
	public void testTranslateModelOrderDouble() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123 order by aDouble").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT _C111_ FROM T123 ORDER BY _C777_",element.toSql());
	}
	
	@Test
	public void testTranslateModelOrderDoubleAs() throws ParseException{
		QuerySpecification element = new TableQueryParser("select aDouble as f1 from syn123 order by f1").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT CASE WHEN _DBL_C777_ IS NULL THEN _C777_ ELSE _DBL_C777_ END AS f1 FROM T123 ORDER BY f1",element.toSql());
	}
	
	@Test
	public void testTranslateModelOrderFunctionDouble() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123 order by min(aDouble)").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT _C111_ FROM T123 ORDER BY MIN(_C777_)",element.toSql());
	}
	
	@Test
	public void testTranslateModelSelectArithmetic() throws ParseException{
		QuerySpecification element = new TableQueryParser("select -(2+2)*10").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT -(2+2)*10",element.toSql());
	}
	
	@Test
	public void testTranslateModelSelectArithmeticRightHandSide() throws ParseException{
		QuerySpecification element = new TableQueryParser("select * from syn123 where foo = -(2+3)*10").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT * FROM T123 WHERE _C111_ = -(:b0+:b1)*:b2",element.toSql());
		assertEquals("2", parameters.get("b0"));
		assertEquals("3", parameters.get("b1"));
		assertEquals("10", parameters.get("b2"));
	}
	
	@Test
	public void testTranslateModelSelectArithmeticFunction() throws ParseException{
		QuerySpecification element = new TableQueryParser("select sum((id+foo)/aDouble) as \"sum\" from syn123").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT SUM((_C444_+_C111_)/CASE WHEN _DBL_C777_ IS NULL THEN _C777_ ELSE _DBL_C777_ END) AS `sum` FROM T123",element.toSql());
	}
	
	/**
	 * This use case is referenced in PLFM-4566.
	 * @throws ParseException
	 */
	@Test
	public void testTranslateModelSelectArithmeticGroupByOrderBy() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo%10, count(*) from syn123 group by foo%10 order by foo%10").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT _C111_%10, COUNT(*) FROM T123 GROUP BY _C111_%10 ORDER BY _C111_%10",element.toSql());
	}
	
	/**
	 * Column reference on the right-hand-side should be replaced with a valid reference to that columnn.
	 * @throws ParseException
	 */
	@Test
	public void testTranslateModelRegularIdentiferRightHandSideColumnReference() throws ParseException{
		QuerySpecification element = new TableQueryParser("select * from syn123 where foo = bar").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT * FROM T123 WHERE _C111_ = _C333_",element.toSql());
	}
	
	/**
	 * Regular Identifier on the right-hand-side that does not match a column should be treated as a 
	 * column reference.
	 * 
	 * @throws ParseException
	 */
	@Test
	public void testTranslateModelRegularIdentiferRightHandSideNotColumnReference() throws ParseException{
		QuerySpecification element = new TableQueryParser("select * from syn123 where foo = notReference").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT * FROM T123 WHERE _C111_ = notReference",element.toSql());
	}
	
	/**
	 * Column reference on the right-hand-side should be replaced with a valid reference to that column.
	 * @throws ParseException
	 */
	@Test
	public void testTranslateModelDelimitedIdentiferRightHandSideColumnReference() throws ParseException{
		QuerySpecification element = new TableQueryParser("select * from syn123 where foo = \"bar\"").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT * FROM T123 WHERE _C111_ = _C333_",element.toSql());
	}
	
	/**
	 * Regular Identifier on the right-hand-side that does not match a column should be treated as a 
	 * column reference in backticks. See: PLFM-3867.
	 * 
	 * @throws ParseException
	 */
	@Test
	public void testTranslateModelDelemitedIdentiferRightHandSideNotColumnReference() throws ParseException{
		QuerySpecification element = new TableQueryParser("select * from syn123 where foo = \"notReference\"").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT * FROM T123 WHERE _C111_ = `notReference`",element.toSql());
	}
	
	@Test
	public void testTranslateModelArithmeticAndColumnReferenceOnRightHandSide() throws ParseException{
		QuerySpecification element = new TableQueryParser("select * from syn123 where foo = 2*3/bar").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT * FROM T123 WHERE _C111_ = :b0*:b1/_C333_",element.toSql());
		assertEquals("2", parameters.get("b0"));
		assertEquals("3", parameters.get("b1"));
	}
	
	@Test
	public void testTranslateModelArithmeticAndColumnReferenceOnRightHandSide2() throws ParseException{
		QuerySpecification element = new TableQueryParser("select * from syn123 where foo = (2+3)/bar").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT * FROM T123 WHERE _C111_ = (:b0+:b1)/_C333_",element.toSql());
	}
	
	@Test
	public void testTranslateModelArithmeticGroupBy() throws ParseException{
		QuerySpecification element = new TableQueryParser("select * from syn123 group by bar/456 - min(bar)").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT * FROM T123 GROUP BY _C333_/456-MIN(_C333_)",element.toSql());
	}
	
	@Test
	public void testTranslateMySqlFunctionRightHandSide() throws ParseException{
		QuerySpecification element = new TableQueryParser("select * from syn123 where foo > unix_timestamp(CURRENT_TIMESTAMP - INTERVAL 1 MONTH)/1000").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT * FROM T123 WHERE _C111_ > UNIX_TIMESTAMP(CURRENT_TIMESTAMP-INTERVAL 1 MONTH)/:b0",element.toSql());
		assertEquals("1000", parameters.get("b0"));
	}
	
	/**
	 * Double quoted alias should be wrapped in backticks.
	 * See PLFM-4736
	 * @throws ParseException
	 */
	@Test
	public void testTranslateDoubleQuotedAliasOrder() throws ParseException{
		QuerySpecification element = new TableQueryParser("select bar as \"a1\", count(foo) as \"a2\" from syn123 group by \"a1\" order by \"a2\" desc").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT _C333_ AS `a1`, COUNT(_C111_) AS `a2` FROM T123 GROUP BY `a1` ORDER BY `a2` DESC",element.toSql());
	}
	
	/**
	 * Value in double quotes.  Any value in double quotes should be treated as a column reference in backticks.
	 * See: PLFM-3866
	 * @throws ParseException
	 */
	@Test
	public void testTranslateValueInDoubleQuotes() throws ParseException{
		QuerySpecification element = new TableQueryParser("select * from syn123 where foo in(\"one\",\"two\")").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap);
		assertEquals("SELECT * FROM T123 WHERE _C111_ IN ( `one`, `two` )",element.toSql());
	}
	
	@Test
	public void testGetColumnTypeInfoArray(){
		SelectColumn one = new SelectColumn();
		one.setColumnType(ColumnType.STRING);
		SelectColumn two = new SelectColumn();
		two.setColumnType(ColumnType.ENTITYID);
		SelectColumn three = new SelectColumn();
		three.setColumnType(ColumnType.INTEGER);
		
		List<SelectColumn> selectColumns = Lists.newArrayList(one, two, three);
		ColumnTypeInfo[] expected = new ColumnTypeInfo[]{
			ColumnTypeInfo.STRING,
			ColumnTypeInfo.ENTITYID,
			ColumnTypeInfo.INTEGER,
		};
		ColumnTypeInfo[] results = SQLTranslatorUtils.getColumnTypeInfoArray(selectColumns);
		assertTrue(Arrays.equals(expected, results));
	}

	@Test
	public void testValidateSelectColumnWithRealColumnModel() {
		SelectColumn selectColumn = new SelectColumn();
		selectColumn.setName("model");
		SQLTranslatorUtils.validateSelectColumn(selectColumn, null, new ColumnModel(), new ActualIdentifier(new RegularIdentifier("someColumn")));
	}

	@Test
	public void testValidateSelectColumnWithFunction() {
		SelectColumn selectColumn = new SelectColumn();
		selectColumn.setName("function");
		SQLTranslatorUtils.validateSelectColumn(selectColumn, FunctionReturnType.DOUBLE, null, new ActualIdentifier(new RegularIdentifier("someColumn")));
	}

	@Test
	public void testValidateSelectColumnWithRowId() {
		SelectColumn selectColumn = new SelectColumn();
		selectColumn.setName("row_id");
		SQLTranslatorUtils.validateSelectColumn(selectColumn, null, null, null);
	}

	@Test
	public void testValidateSelectColumnWithRowVersion() {
		SelectColumn selectColumn = new SelectColumn();
		selectColumn.setName("row_version");
		SQLTranslatorUtils.validateSelectColumn(selectColumn, null, null, null);
	}

	@Test
	public void testValidateSelectColumnWithStringConstant() {
		SelectColumn selectColumn = new SelectColumn();
		selectColumn.setName("contant");
		SQLTranslatorUtils.validateSelectColumn(selectColumn, null, null, new UnsignedLiteral(new GeneralLiteral(new CharacterStringLiteral("constant"))));
	}

	@Test
	public void testValidateSelectColumnWithNumber() {
		SelectColumn selectColumn = new SelectColumn();
		selectColumn.setName("contant");
		SQLTranslatorUtils.validateSelectColumn(selectColumn, null, null, new UnsignedLiteral(new UnsignedNumericLiteral(new ExactNumericLiteral(1L))));
	}

	@Test
	public void testValidateSelectColumnInvalid() {
		SelectColumn selectColumn = new SelectColumn();
		selectColumn.setName("invalid");
		try {
			SQLTranslatorUtils.validateSelectColumn(selectColumn, null, null, new ActualIdentifier(new RegularIdentifier("invalid")));
			fail("Should throw IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			String message = e.getMessage();
			assertTrue(message.contains("invalid"));
			assertTrue(message.contains("Unknown column"));
	}
	}
}
