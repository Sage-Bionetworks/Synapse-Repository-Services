package org.sagebionetworks.table.cluster;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnSingleValueFilterOperator;
import org.sagebionetworks.repo.model.table.ColumnSingleValueQueryFilter;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.QueryFilter;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.table.cluster.columntranslation.ColumnTranslationReferenceLookup;
import org.sagebionetworks.table.cluster.columntranslation.SchemaColumnTranslationReference;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.ActualIdentifier;
import org.sagebionetworks.table.query.model.ArrayHasPredicate;
import org.sagebionetworks.table.query.model.BooleanPrimary;
import org.sagebionetworks.table.query.model.CharacterStringLiteral;
import org.sagebionetworks.table.query.model.ColumnNameReference;
import org.sagebionetworks.table.query.model.CurrentUserFunction;
import org.sagebionetworks.table.query.model.DerivedColumn;
import org.sagebionetworks.table.query.model.ExactNumericLiteral;
import org.sagebionetworks.table.query.model.FromClause;
import org.sagebionetworks.table.query.model.FunctionReturnType;
import org.sagebionetworks.table.query.model.GeneralLiteral;
import org.sagebionetworks.table.query.model.GroupByClause;
import org.sagebionetworks.table.query.model.HasPredicate;
import org.sagebionetworks.table.query.model.InPredicate;
import org.sagebionetworks.table.query.model.Pagination;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.RegularIdentifier;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.UnsignedLiteral;
import org.sagebionetworks.table.query.model.UnsignedNumericLiteral;
import org.sagebionetworks.table.query.model.ValueExpressionPrimary;
import org.sagebionetworks.table.query.util.SqlElementUntils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ETAG;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_ID;
import static org.sagebionetworks.repo.model.table.TableConstants.ROW_VERSION;

@ExtendWith(MockitoExtension.class)
public class SQLTranslatorUtilsTest {

	private static final String DATE1 = "11-11-11";
	private static final String DATE1TIME = "1320969600000";

	private static final String DATE2TIME = "1298332800000";
	private static final String DATE2 = "11-02-22";


	@Mock
	ColumnNameReference mockHasQuoteValue;
	@Mock
	ResultSet mockResultSet;
	
	ColumnTranslationReferenceLookup columnMap;
	
	ColumnModel columnFoo;
	ColumnModel columnHasSpace;
	ColumnModel columnBar;
	ColumnModel columnId;
	ColumnModel columnSpecial;
	ColumnModel columnDouble;
	ColumnModel columnDate;
	ColumnModel columnQuoted;
	
	List<ColumnModel> schema;
	
	List<SelectColumn> selectList;
	ColumnTypeInfo[] infoArray;
	Long rowId;
	Long rowVersion;
	Long userId;
	IdAndVersion tableIdAndVersion;
	String etag;
	
	@BeforeEach
	public void before() throws Exception {
		columnFoo = TableModelTestUtils.createColumn(111L, "foo", ColumnType.STRING);
		columnHasSpace = TableModelTestUtils.createColumn(222L, "has space", ColumnType.STRING);
		columnBar = TableModelTestUtils.createColumn(333L, "bar", ColumnType.STRING);
		columnId = TableModelTestUtils.createColumn(444L, "id", ColumnType.INTEGER);
		String specialChars = "Specialchars~!@#$%^^&*()_+|}{:?></.,;'[]\'";
		columnSpecial = TableModelTestUtils.createColumn(555L, specialChars, ColumnType.DOUBLE);
		columnDouble = TableModelTestUtils.createColumn(777L, "aDouble", ColumnType.DOUBLE);
		columnDate = TableModelTestUtils.createColumn(888L, "aDate", ColumnType.DATE);
		columnQuoted = TableModelTestUtils.createColumn(999L, "colWith\"Quotes\"InIt", ColumnType.STRING);

		schema = Lists.newArrayList(columnFoo, columnHasSpace, columnBar, columnId, columnSpecial, columnDouble, columnDate, columnQuoted);
		// setup the map
		columnMap = new ColumnTranslationReferenceLookup(schema);
		
		SelectColumn one = new SelectColumn();
		one.setColumnType(ColumnType.STRING);
		SelectColumn two = new SelectColumn();
		two.setColumnType(ColumnType.BOOLEAN);
		
		selectList = Lists.newArrayList(one, two);
		infoArray = SQLTranslatorUtils.getColumnTypeInfoArray(selectList);
		
		rowId = 123L;
		rowVersion = 2L;
		userId = 1L;
		etag = "anEtag";
		tableIdAndVersion = IdAndVersion.parse("syn123.456");
	}

	@Test
	public void testIsNumericType(){
		assertFalse(SQLTranslatorUtils.isNumericType(ColumnType.BOOLEAN));
		assertTrue(SQLTranslatorUtils.isNumericType(ColumnType.DATE));
		assertTrue(SQLTranslatorUtils.isNumericType(ColumnType.DOUBLE));
		assertFalse(SQLTranslatorUtils.isNumericType(ColumnType.ENTITYID));
		assertTrue(SQLTranslatorUtils.isNumericType(ColumnType.SUBMISSIONID));
		assertTrue(SQLTranslatorUtils.isNumericType(ColumnType.EVALUATIONID));
		assertTrue(SQLTranslatorUtils.isNumericType(ColumnType.FILEHANDLEID));
		assertTrue(SQLTranslatorUtils.isNumericType(ColumnType.INTEGER));
		assertFalse(SQLTranslatorUtils.isNumericType(ColumnType.LARGETEXT));
		assertFalse(SQLTranslatorUtils.isNumericType(ColumnType.LINK));
		assertFalse(SQLTranslatorUtils.isNumericType(ColumnType.STRING));
		assertTrue(SQLTranslatorUtils.isNumericType(ColumnType.USERID));
	}
	
	@Test
	public void testIsNumericTypeNull(){
		assertThrows(IllegalArgumentException.class, () -> {
			SQLTranslatorUtils.isNumericType(null);
		});
	}
	
	@Test
	public void testIsNumericAllTypes(){
		// Should work for all types without errors
		for(ColumnType type: ColumnType.values()){
			SQLTranslatorUtils.isNumericType(type);
		}
	}
	
	@Test
	public void testGetBaseColulmnTypeNoQuotes(){
		when(mockHasQuoteValue.hasQuotesRecursive()).thenReturn(false);
		// call under test
		ColumnType type = SQLTranslatorUtils.getBaseColulmnType(mockHasQuoteValue);
		ColumnType expected = ColumnType.DOUBLE;
		assertEquals(expected, type);
	}
	
	@Test
	public void testGetBaseColulmnTypeWithQuotes(){
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
		DerivedColumn derivedColumn = new TableQueryParser("ROW_ID").derivedColumn();
		// call under test
		SelectColumn results = SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
		assertNotNull(results);
		assertEquals("ROW_ID", results.getName());
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
		DerivedColumn derivedColumn = new TableQueryParser("ROW_VERSION").derivedColumn();
		// call under test
		SelectColumn results = SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
		assertNotNull(results);
		assertEquals("ROW_VERSION", results.getName());
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
	public void testGetSelectColumnsCurrentUser() throws ParseException{
		DerivedColumn derivedColumn = new TableQueryParser("current_user()").derivedColumn();
		// call under test
		SelectColumn results = SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
		assertNotNull(results);
		assertEquals("CURRENT_USER()", results.getName());
		assertEquals(ColumnType.USERID, results.getColumnType());
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
	public void testGetSelectColumnsColumnNameWithQuotes() throws ParseException{
		DerivedColumn derivedColumn = new TableQueryParser("\"colWith\"\"Quotes\"\"InIt\"").derivedColumn();
		// call under test
		SelectColumn results = SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
		assertNotNull(results);
		assertEquals("colWith\"Quotes\"InIt", results.getName());
		assertEquals(ColumnType.STRING, results.getColumnType());
		assertEquals("999", results.getId());
	}

	@Test
	public void testGetSelectColumnsAliasNameWithQuotes() throws ParseException{
		DerivedColumn derivedColumn = new TableQueryParser("foo as \"aliasWith\"\"Quotes\"\"InIt\"").derivedColumn();
		// call under test
		SelectColumn results = SQLTranslatorUtils.getSelectColumns(derivedColumn, columnMap);
		assertNotNull(results);
		assertEquals("aliasWith\"Quotes\"InIt", results.getName());
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
	
	@Test
	public void testCreateSelectListFromSchemaNull(){
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			SQLTranslatorUtils.createSelectListFromSchema(null);
		});
	}
	
	@Test
	public void testGetSelectColumnsSelectStar() throws ParseException{
		boolean isAggregate = false;
		SelectList element = new TableQueryParser("*").selectList();
		assertThrows(IllegalStateException.class, () -> {
			//  call under test.
			SQLTranslatorUtils.getSelectColumns(element, columnMap, isAggregate);
		});
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
			assertNull(select.getId(), "This is an aggregate so all column ids must be null.");
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
			assertNull(select.getId(), "This is not an aggregate but since one select does not match the schema, all column Ids should be null.");
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
			assertNull(select.getId(), "This is an aggregate and one select does not match the schema so all column Ids should be null.");
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
		when(mockResultSet.getLong(ROW_ID)).thenReturn(rowId);
		when(mockResultSet.getLong(ROW_VERSION)).thenReturn(rowVersion);
		when(mockResultSet.getString(ROW_ETAG)).thenReturn(etag);
		when(mockResultSet.getString(1)).thenReturn("aString");
		when(mockResultSet.getString(2)).thenReturn("true");
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
		when(mockResultSet.getString(1)).thenReturn("aString");
		when(mockResultSet.getString(2)).thenReturn("true");
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
		when(mockResultSet.getString(1)).thenReturn("aString");
		when(mockResultSet.getString(2)).thenReturn("true");
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
		when(mockResultSet.getLong(ROW_ID)).thenReturn(rowId);
		when(mockResultSet.getLong(ROW_VERSION)).thenReturn(rowVersion);
		when(mockResultSet.getString(1)).thenReturn("aString");
		when(mockResultSet.getString(2)).thenReturn("true");
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
	public void testTranslateFromClause() throws ParseException{
		FromClause element = new TableQueryParser("FROM syn123").fromClause();
		SQLTranslatorUtils.translate(element);
		assertEquals("T123",element.getTableReference().getTableName());
	}
	
	@Test
	public void testTranslateTableReferenceVerion() throws ParseException{
		FromClause element = new TableQueryParser("FROM syn123.456").fromClause();
		SQLTranslatorUtils.translate(element);
		assertEquals("T123_456",element.getTableReference().getTableName());
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
		ValueExpressionPrimary column = new TableQueryParser("ROW_ID").valueExpressionPrimary();
		SQLTranslatorUtils.translateSelect(column, columnMap);
		assertEquals("ROW_ID", column.toSql());
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
		ValueExpressionPrimary column = new TableQueryParser("ROW_ID").valueExpressionPrimary();
		SQLTranslatorUtils.translateOrderBy(column, columnMap);
		assertEquals("ROW_ID", column.toSql());
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
		BooleanPrimary element = new TableQueryParser("isNaN(_C777_)").booleanPrimary();
		SQLTranslatorUtils.replaceBooleanFunction(element, columnMap);
		assertEquals("( _DBL_C777_ IS NOT NULL AND _DBL_C777_ = 'NaN' )", element.toSql());
	}
	
	@Test
	public void testReplaceBooleanFunctionIsInfinity() throws ParseException{
		BooleanPrimary element = new TableQueryParser("isInfinity(_C777_)").booleanPrimary();
		SQLTranslatorUtils.replaceBooleanFunction(element, columnMap);
		assertEquals("( _DBL_C777_ IS NOT NULL AND _DBL_C777_ IN ( '-Infinity', 'Infinity' ) )", element.toSql());
	}
	
	@Test
	public void testReplaceBooleanFunctionNonDoubleColumn() throws ParseException{
		BooleanPrimary element = new TableQueryParser("isInfinity(_C444_)").booleanPrimary();
		assertThrows(IllegalArgumentException.class, () -> {
			SQLTranslatorUtils.replaceBooleanFunction(element, columnMap);
		});
	}
	
	@Test
	public void testReplaceBooleanFunctionUnknownColumn() throws ParseException{
		BooleanPrimary element = new TableQueryParser("isInfinity(someUnknown)").booleanPrimary();
		assertThrows(IllegalArgumentException.class, () -> {
			SQLTranslatorUtils.replaceBooleanFunction(element, columnMap);
		});
	}
	
	@Test
	public void testReplaceBooleanFunctionNotBooleanFunction() throws ParseException{
		BooleanPrimary element = new TableQueryParser("id = 123").booleanPrimary();
		SQLTranslatorUtils.replaceBooleanFunction(element, columnMap);
		assertEquals("id = 123", element.toSql(), "Non-BooleanFunctions should not be changed by this method.");
	}
	
	@Test
	public void testReplaceBooleanFunctionSearchCondition() throws ParseException{
		BooleanPrimary element = new TableQueryParser("(id = 123 OR id = 456)").booleanPrimary();
		SQLTranslatorUtils.replaceBooleanFunction(element, columnMap);
		assertEquals("( id = 123 OR id = 456 )", element.toSql(), "SearchConditions should not be changed by this method.");
	}

	@Test
	public void testReplaceArrayHasPredicate_ReferencedColumn_FalseIsList() throws ParseException {
		columnFoo.setColumnType(ColumnType.STRING);//not a list type
		columnMap = new ColumnTranslationReferenceLookup(schema);

		BooleanPrimary booleanPrimary = SqlElementUntils.createBooleanPrimary("foo has ('asdf', 'qwerty', 'yeet')");
		//call translate so that bind variable replacement occurs, matching the state of the ArrayHasPredicate when replaceArrayHasPredicate is called in actual code.
		SQLTranslatorUtils.translate(booleanPrimary.getFirstElementOfType(ArrayHasPredicate.class), new HashMap<>(), columnMap);

		assertThrows(IllegalArgumentException.class, () -> {
			SQLTranslatorUtils.replaceArrayHasPredicate(booleanPrimary, columnMap, tableIdAndVersion);
		});
	}

	@Test
	public void testReplaceArrayHasPredicate_ReferencedColumn_unknown() throws ParseException {
		columnFoo.setColumnType(ColumnType.STRING_LIST);
		columnMap = new ColumnTranslationReferenceLookup(schema);

		//should not ever happen since translate would have to translate column name to something that it didnt have in its maping
		BooleanPrimary booleanPrimary = SqlElementUntils.createBooleanPrimary("_C723895794567246_ has ('asdf', 'qwerty', 'yeet')");

		assertThrows(IllegalArgumentException.class, () -> {
			SQLTranslatorUtils.replaceArrayHasPredicate(booleanPrimary, columnMap, tableIdAndVersion);
		});
	}

	@Test
	public void testReplaceArrayHasPredicate_Has() throws ParseException {
		columnFoo.setColumnType(ColumnType.STRING_LIST);
		columnMap = new ColumnTranslationReferenceLookup(schema);

		BooleanPrimary booleanPrimary = SqlElementUntils.createBooleanPrimary("foo has ('asdf', 'qwerty', 'yeet')");
		//call translate so that bind variable replacement occurs, matching the state of when replaceArrayHasPredicate is called in actual code.
		SQLTranslatorUtils.translate(booleanPrimary.getFirstElementOfType(ArrayHasPredicate.class), new HashMap<>(), columnMap);

		SQLTranslatorUtils.replaceArrayHasPredicate(booleanPrimary, columnMap, tableIdAndVersion);

		assertEquals("ROW_ID IN ( SELECT ROW_ID_REF_C111_ FROM T123_456_INDEX_C111_ WHERE _C111__UNNEST IN ( :b0, :b1, :b2 ) )", booleanPrimary.toSql());

	}

	@Test
	public void testReplaceArrayHasPredicate_NotHas() throws ParseException {
		columnFoo.setColumnType(ColumnType.STRING_LIST);
		columnMap = new ColumnTranslationReferenceLookup(schema);


		BooleanPrimary booleanPrimary = SqlElementUntils.createBooleanPrimary("foo not has ('asdf', 'qwerty', 'yeet')");
		//call translate so that bind variable replacement occurs, matching the state of when replaceArrayHasPredicate is called in actual code.
		SQLTranslatorUtils.translate(booleanPrimary.getFirstElementOfType(ArrayHasPredicate.class), new HashMap<>(), columnMap);

		SQLTranslatorUtils.replaceArrayHasPredicate(booleanPrimary, columnMap, tableIdAndVersion);

		assertEquals("ROW_ID NOT IN ( SELECT ROW_ID_REF_C111_ FROM T123_456_INDEX_C111_ WHERE _C111__UNNEST IN ( :b0, :b1, :b2 ) )", booleanPrimary.toSql());

	}

	//Test to ensure that a list of values containing 'syn' prefix are removed (e.g. ( ('syn123','syn456')  --->  ('123','456') )
	@Test
	public void testTranslate_Has_onEntityIdList() throws ParseException {
		columnFoo.setColumnType(ColumnType.ENTITYID_LIST);
		columnMap = new ColumnTranslationReferenceLookup(schema);

		BooleanPrimary booleanPrimary = SqlElementUntils.createBooleanPrimary("foo has ('syn123', 456, 'syn789')");
		HashMap<String, Object> parameters = new HashMap<>();

		//method under test
		SQLTranslatorUtils.translate(booleanPrimary.getFirstElementOfType(ArrayHasPredicate.class), parameters, columnMap);

		//parameter mapping should have stripped out the "syn" prefixes
		Map<String, Object> expected = new HashMap<String, Object>(){{
				put("b0", 123L);
				put("b1", 456L);
				put("b2", 789L);
		}};

		assertEquals(expected, parameters);
	}

	@Test
	public void testReplaceArrayHasPredicate_NotAnArrayHasPredicate() throws ParseException{
		BooleanPrimary notArrayHasPredicate = SqlElementUntils.createBooleanPrimary("foo IN (\"123\", \"456\")");
		//call translate so that bind variable replacement occurs, matching the state of when replaceArrayHasPredicate is called in actual code.
		SQLTranslatorUtils.translate(notArrayHasPredicate.getFirstElementOfType(InPredicate.class), new HashMap<>(), columnMap);

		String beforeCallSqll = notArrayHasPredicate.toSql();
		SQLTranslatorUtils.replaceArrayHasPredicate(notArrayHasPredicate, columnMap, tableIdAndVersion);
		//if not an ArrayHasPredicate, nothing should have changed
		assertEquals(beforeCallSqll, notArrayHasPredicate.toSql());
	}

	@Test
	public void tesTranslateArrayFunction_columnNotFound() throws ParseException {
		//_C987654_ does not exist
		QuerySpecification querySpecification = TableQueryParser.parserQuery(
				"SELECT UNNEST(_C987654_) FROM T123 ORDER BY UNNEST(_C987654_)"
		);

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			//method under test
			SQLTranslatorUtils.translateArrayFunctions(querySpecification, columnMap, tableIdAndVersion);
		}).getMessage();

		assertEquals("Unknown column reference: _C987654_", errorMessage);
	}

	@Test
	public void tesTranslateArrayFunction_columnNotInSchema() throws ParseException {
		//can not perform UNNEST on one of the metadata columns
		QuerySpecification querySpecification = TableQueryParser.parserQuery(
				"SELECT UNNEST(ROW_ID) FROM T123 ORDER BY UNNEST(ROW_ID)"
		);

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			//method under test
			SQLTranslatorUtils.translateArrayFunctions(querySpecification, columnMap, tableIdAndVersion);
		}).getMessage();

		assertEquals("UNNEST() may only be used on columns defined in the schema", errorMessage);
	}

	@Test
	public void tesTranslateArrayFunction_columnNotListType() throws ParseException {
		columnFoo.setColumnType(ColumnType.STRING);
		//need to recreate the translation reference
		columnMap = new ColumnTranslationReferenceLookup(schema);

		//_C111_ is a STRING type instead of STRING_LIST
		QuerySpecification querySpecification = TableQueryParser.parserQuery(
				"SELECT UNNEST(_C111_) FROM T123 ORDER BY UNNEST(_C111_)"
		);

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			//method under test
			SQLTranslatorUtils.translateArrayFunctions(querySpecification, columnMap, tableIdAndVersion);
		}).getMessage();

		assertEquals("UNNEST() only works for columns that hold list values", errorMessage);
	}

	@Test
	public void tesTranslateArrayFunction_multipleColumns() throws ParseException {
		columnFoo.setColumnType(ColumnType.STRING_LIST);
		columnBar.setColumnType(ColumnType.STRING_LIST);

		//need to recreate the translation reference
		columnMap = new ColumnTranslationReferenceLookup(schema);

		QuerySpecification querySpecification = TableQueryParser.parserQuery(
				"SELECT _C222_, UNNEST(_C111_), UNNEST(_C333_) FROM T123 ORDER BY UNNEST(_C111_), UNNEST(_C333_)"
		);

		//method under test
		SQLTranslatorUtils.translateArrayFunctions(querySpecification, columnMap, tableIdAndVersion);

		String expected = "SELECT _C222_, _C111__UNNEST, _C333__UNNEST " +
				"FROM T123 " +
				"LEFT JOIN T123_456_INDEX_C111_ ON T123.ROW_ID = T123_456_INDEX_C111_.ROW_ID_REF_C111_ " +
				"LEFT JOIN T123_456_INDEX_C333_ ON T123.ROW_ID = T123_456_INDEX_C333_.ROW_ID_REF_C333_ " +
				"ORDER BY _C111__UNNEST, _C333__UNNEST";
		assertEquals(expected, querySpecification.toSql());
	}


	@Test
	public void testTranslate_PredicateColumnReferenceNotExist() throws ParseException {
		//reference a column not found in schema
		Predicate predicate = SqlElementUntils.createPredicate("NOTINSCHEMA <> 1");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		assertThrows(IllegalArgumentException.class, () -> {
			SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
		});
	}

	@Test
	public void testComparisonPredicate() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo <> 1");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
		assertEquals("_C111_ <> :b0", predicate.toSql());
		assertEquals("1", parameters.get("b0"));
	}

	@Test
	public void testStringComparisonPredicate() throws ParseException {
		Predicate predicate = SqlElementUntils.createPredicate("foo <> 'aaa'");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
		assertEquals("_C111_ <> :b0", predicate.toSql());
		assertEquals("aaa", parameters.get("b0"));
	}

	@Test
	public void testStringComparisonBooleanPredicate() throws ParseException {
		Predicate predicate = SqlElementUntils.createPredicate("foo = true");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
		assertEquals("_C111_ = TRUE", predicate.toSql());
		assertEquals(0, parameters.size());
	}

	@Test
	public void testComparisonPredicateDateNumber() throws ParseException {
		Predicate predicate = SqlElementUntils.createPredicate("aDate <> 1");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
		assertEquals("_C888_ <> :b0", predicate.toSql());
		assertEquals(new Long(1), parameters.get("b0"));
	}

	@Test
	public void testComparisonPredicateDateString() throws ParseException {
		Predicate predicate = SqlElementUntils.createPredicate("aDate <> '2011-11-11'");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
		assertEquals("_C888_ <> :b0", predicate.toSql());
		assertEquals(Long.parseLong(DATE1TIME), parameters.get("b0"));
	}

	@Test
	public void testComparisonPredicateDateParsing() throws ParseException {
		for (String date : new String[] { DATE1, "2011-11-11", "2011-11-11 0:00", "2011-11-11 0:00:00", "2011-11-11 0:00:00.0",
				"2011-11-11 0:00:00.00", "2011-11-11 0:00:00.000" }) {
			HashMap<String, Object> parameters = new HashMap<String, Object>();

			Predicate predicate =  SqlElementUntils.createPredicate("aDate <> '" + date + "'");
			HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
			SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
			assertEquals("_C888_ <> :b0", predicate.toSql());
			assertEquals(Long.parseLong(DATE1TIME), parameters.get("b0"));
		}
		for (String date : new String[] { "2001-01-01", "2001-01-01", "2001-1-1", "2001-1-01", "2001-01-1" }) {
			HashMap<String, Object> parameters = new HashMap<String, Object>();

			Predicate predicate =  SqlElementUntils.createPredicate("aDate <> '" + date + "'");
			HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
			SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
			assertEquals("_C888_ <> :b0", predicate.toSql());
			assertEquals(Long.parseLong("978307200000"), parameters.get("b0"));
		}
		for (String date : new String[] { "2011-11-11 01:01:01.001", "2011-11-11 1:01:1.001", "2011-11-11 1:1:1.001" }) {
			HashMap<String, Object> parameters = new HashMap<String, Object>();

			Predicate predicate =  SqlElementUntils.createPredicate("aDate <> '" + date + "'");
			HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
			SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
			assertEquals("_C888_ <> :b0", predicate.toSql());
			assertEquals(Long.parseLong("1320973261001"), parameters.get("b0"));
		}
	}

	@Test
	public void testInPredicateOne() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo in(1)");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
		assertEquals("_C111_ IN ( :b0 )", predicate.toSql());
		assertEquals("1", parameters.get("b0"));
	}

	@Test
	public void testInPredicateMore() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo in(1,2,3)");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
		assertEquals("_C111_ IN ( :b0, :b1, :b2 )", predicate.toSql());
		assertEquals("1", parameters.get("b0"));
		assertEquals("2", parameters.get("b1"));
		assertEquals("3", parameters.get("b2"));
	}

	@Test
	public void testInPredicateDate() throws ParseException {
		Predicate predicate = SqlElementUntils.createPredicate("aDate in('" + DATE1 + "','" + DATE2 + "')");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
		assertEquals("_C888_ IN ( :b0, :b1 )", predicate.toSql());
		assertEquals(Long.parseLong(DATE1TIME), parameters.get("b0"));
		assertEquals(Long.parseLong(DATE2TIME), parameters.get("b1"));
	}

	@Test
	public void testBetweenPredicate() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo between 1 and 2");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
		assertEquals("_C111_ BETWEEN :b0 AND :b1", predicate.toSql());
		assertEquals("1", parameters.get("b0"));
		assertEquals("2", parameters.get("b1"));
	}

	@Test
	public void testBetweenPredicateDate() throws ParseException {
		Predicate predicate = SqlElementUntils.createPredicate("aDate between '" + DATE1 + "' and '" + DATE2 + "'");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
		assertEquals("_C888_ BETWEEN :b0 AND :b1", predicate.toSql());
		assertEquals(Long.parseLong(DATE1TIME), parameters.get("b0"));
		assertEquals(Long.parseLong(DATE2TIME), parameters.get("b1"));
	}

	@Test
	public void testBetweenPredicateNot() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo not between 1 and 2");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
		assertEquals("_C111_ NOT BETWEEN :b0 AND :b1", predicate.toSql());
		assertEquals("1", parameters.get("b0"));
		assertEquals("2", parameters.get("b1"));
	}

	@Test
	public void testLikePredicate() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo like 'bar%'");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
		assertEquals("_C111_ LIKE :b0", predicate.toSql());
		assertEquals("bar%",parameters.get("b0"));
	}

	@Test
	public void testLikePredicateEscape() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo like 'bar|_' escape '|'");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
		assertEquals("_C111_ LIKE :b0 ESCAPE :b1", predicate.toSql());
		assertEquals("bar|_",parameters.get("b0"));
		assertEquals("|",parameters.get("b1"));
	}

	@Test
	public void testLikePredicateNot() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo not like 'bar%'");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
		assertEquals("_C111_ NOT LIKE :b0", predicate.toSql());
		assertEquals("bar%",parameters.get("b0"));
	}

	@Test
	public void testNullPredicate() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo is null");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
		assertEquals("_C111_ IS NULL", predicate.toSql());
	}

	@Test
	public void testNullPredicateNot() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo is not null");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
		assertEquals("_C111_ IS NOT NULL", predicate.toSql());
	}


	@Test
	public void testTranslateRightHandeSideNullElement(){
		UnsignedLiteral element = null;
		Map<String, Object> parameters = new HashMap<String, Object>();
		assertThrows(IllegalArgumentException.class, () -> {
			SQLTranslatorUtils.translateRightHandeSide(element, ColumnType.STRING, parameters);
		});
	}
	
	@Test
	public void testTranslateRightHandeSideNullParameters() throws ParseException{
		UnsignedLiteral element = new TableQueryParser("'aString'").unsignedLiteral();
		Map<String, Object> parameters = null;
		assertThrows(IllegalArgumentException.class, () -> {
			SQLTranslatorUtils.translateRightHandeSide(element, ColumnType.STRING, parameters);
		});
	}
	
	@Test
	public void testTranslateRightHandeSideNullColumn() throws ParseException{
		UnsignedLiteral element = new TableQueryParser("'aString'").unsignedLiteral();
		Map<String, Object> parameters = new HashMap<String, Object>();
		assertThrows(IllegalArgumentException.class, () -> {
			SQLTranslatorUtils.translateRightHandeSide(element, null, parameters);
		});
	}
	
	@Test
	public void testTranslateRightHandeSideString() throws ParseException{
		UnsignedLiteral element = new TableQueryParser("'aString'").unsignedLiteral();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateRightHandeSide(element, ColumnType.STRING, parameters);
		assertEquals(":b0", element.toSqlWithoutQuotes());
		assertEquals("aString", parameters.get("b0"));
	}
	
	
	@Test
	public void testTranslateRightHandeSideInteger() throws ParseException{
		UnsignedLiteral element = new TableQueryParser("123456").unsignedLiteral();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateRightHandeSide(element, ColumnType.INTEGER, parameters);
		assertEquals(":b0", element.toSqlWithoutQuotes());
		assertEquals(new Long(123456), parameters.get("b0"));
	}
	
	@Test
	public void testTranslateRightHandeSideIntegerLikeValue() throws ParseException{
		UnsignedLiteral element = new TableQueryParser("'12345%'").unsignedLiteral();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateRightHandeSide(element, ColumnType.INTEGER, parameters);
		assertEquals(":b0", element.toSqlWithoutQuotes());
		assertEquals("12345%", parameters.get("b0"));
	}
	
	@Test
	public void testTranslateRightHandeSideDouble() throws ParseException{
		UnsignedLiteral element = new TableQueryParser("1.45").unsignedLiteral();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateRightHandeSide(element, ColumnType.DOUBLE, parameters);
		assertEquals(":b0", element.toSqlWithoutQuotes());
		assertEquals(new Double(1.45), parameters.get("b0"));
	}
	
	@Test
	public void testTranslateRightHandeSideDateString() throws ParseException{
		UnsignedLiteral element = new TableQueryParser("'16-01-29 13:55:33.999'").unsignedLiteral();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateRightHandeSide(element, ColumnType.DATE, parameters);
		assertEquals(":b0", element.toSqlWithoutQuotes());
		assertEquals(new Long(1454075733999L), parameters.get("b0"));
	}
	
	@Test
	public void testTranslateRightHandeSideDateEpoch() throws ParseException{
		UnsignedLiteral element = new TableQueryParser("1454075733999").unsignedLiteral();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateRightHandeSide(element, ColumnType.DATE, parameters);
		assertEquals(":b0", element.toSqlWithoutQuotes());
		assertEquals(new Long(1454075733999L), parameters.get("b0"));
	}
	@Test
	public void testTranslateRightHandeSideInterval() throws ParseException{
		UnsignedLiteral element = new TableQueryParser("INTERVAL 3 MONTH").unsignedLiteral();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateRightHandeSide(element, ColumnType.DATE, parameters);
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
	
	@Test
	public void testTranslateGroupByNull() throws ParseException{
		GroupByClause element = null;
		assertThrows(IllegalArgumentException.class, () -> {
			SQLTranslatorUtils.translate(element, columnMap);
		});
	}
	
	@Test
	public void testTranslateGroupByMapNull() throws ParseException{
		GroupByClause element = new TableQueryParser("group by doesNotExist").groupByClause();
		assertThrows(IllegalArgumentException.class, () -> {
			SQLTranslatorUtils.translate(element, null);
		});
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
		assertThrows( IllegalArgumentException.class, () -> {
					SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
				});
		assertEquals("_D999_ IS NOT NULL",element.toSql());
	}
	
	@Test
	public void testTranslateHasPredicateNullElement() throws ParseException{
		HasPredicate hasPredicate = null;
		Map<String, Object> parameters = new HashMap<String, Object>();
		assertThrows(IllegalArgumentException.class, () -> {
			SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
		});
	}
	
	@Test
	public void testTranslateHasPredicateNullParameters() throws ParseException{
		Predicate element = new TableQueryParser("id <> 3").predicate();
		HasPredicate hasPredicate = element.getFirstElementOfType(HasPredicate.class);
		Map<String, Object> parameters = null;
		assertThrows(IllegalArgumentException.class, () -> {
			SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);
		});
	}
	
	@Test
	public void testTranslateHasPredicateNullMap() throws ParseException{
		Predicate element = new TableQueryParser("id <> 3").predicate();
		HasPredicate hasPredicate = element.getFirstElementOfType(HasPredicate.class);
		Map<String, Object> parameters = new HashMap<String, Object>();
		assertThrows(IllegalArgumentException.class, () -> {
			SQLTranslatorUtils.translate(hasPredicate, parameters, null);
		});
	}

	@Test
	public void testTranslateHasPredicate_ROW_ID_column() throws ParseException{
		Predicate element = new TableQueryParser("ROW_ID <> 3").predicate();
		HasPredicate hasPredicate = element.getFirstElementOfType(HasPredicate.class);
		Map<String, Object> parameters = new HashMap<String, Object>();

		//method under test
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);

		assertEquals("ROW_ID <> :b0", element.toSql());
		assertEquals(3L, parameters.get("b0"));
	}


	@Test
	public void testTranslateHasPredicate_ROW_VERSION_column() throws ParseException{
		Predicate element = new TableQueryParser("ROW_VERSION <> 54").predicate();
		HasPredicate hasPredicate = element.getFirstElementOfType(HasPredicate.class);
		Map<String, Object> parameters = new HashMap<String, Object>();

		//method under test
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);

		assertEquals("ROW_VERSION <> :b0", element.toSql());
		assertEquals(54L, parameters.get("b0"));
	}

	@Test
	public void testTranslateHasPredicate_ROW_BENEFACTOR_column() throws ParseException{
		Predicate element = new TableQueryParser("ROW_BENEFACTOR <> 54").predicate();
		HasPredicate hasPredicate = element.getFirstElementOfType(HasPredicate.class);
		Map<String, Object> parameters = new HashMap<String, Object>();

		//method under test
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);

		assertEquals("ROW_BENEFACTOR <> :b0", element.toSql());
		assertEquals(54L, parameters.get("b0"));
	}

	@Test
	public void testTranslateHasPredicate_ROW_ETAG_column() throws ParseException{
		String uuid = UUID.randomUUID().toString();
		Predicate element = new TableQueryParser("ROW_ETAG <> '" + uuid + "'").predicate();
		HasPredicate hasPredicate = element.getFirstElementOfType(HasPredicate.class);
		Map<String, Object> parameters = new HashMap<String, Object>();

		//method under test
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnMap);

		assertEquals("ROW_ETAG <> :b0", element.toSql());
		assertEquals(uuid, parameters.get("b0"));
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
	
	@Test
	public void testTranslatePaginationNull() throws ParseException{
		Pagination element = null;
		Map<String, Object> parameters = new HashMap<String, Object>();
		assertThrows(IllegalArgumentException.class, () -> {
			SQLTranslatorUtils.translate(element, parameters);
		});
	}
	
	@Test
	public void testTranslatePaginationParametersNull() throws ParseException{
		Pagination element = new TableQueryParser("limit 1").pagination();
		Map<String, Object> parameters = null;
		assertThrows(IllegalArgumentException.class, () -> {
			SQLTranslatorUtils.translate(element, parameters);
		});
	}
	
	@Test
	public void testTranslateModelSimple() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT _C111_ FROM T123",element.toSql());
	}
	
	@Test
	public void testTranslateModelSelectFunction() throws ParseException{
		QuerySpecification element = new TableQueryParser("select sum(foo) from syn123").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT SUM(_C111_) FROM T123",element.toSql());
	}
	
	@Test
	public void testTranslateModelSelectDouble() throws ParseException{
		QuerySpecification element = new TableQueryParser("select aDouble from syn123").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT CASE WHEN _DBL_C777_ IS NULL THEN _C777_ ELSE _DBL_C777_ END FROM T123",element.toSql());
	}
	
	@Test
	public void testTranslateModelSelectDoubleFunction() throws ParseException{
		QuerySpecification element = new TableQueryParser("select sum(aDouble) from syn123").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT SUM(_C777_) FROM T123",element.toSql());
	}

	
	@Test
	public void testTranslateModelWhere() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123 where id > 2").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT _C111_ FROM T123 WHERE _C444_ > :b0",element.toSql());
		assertEquals(new Long(2), parameters.get("b0"));
	}
	
	@Test
	public void testTranslateModelWhereBetween() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123 where id between '1' and 2").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT _C111_ FROM T123 WHERE _C444_ BETWEEN :b0 AND :b1",element.toSql());
		assertEquals(new Long(1), parameters.get("b0"));
		assertEquals(new Long(2), parameters.get("b1"));
	}
	
	@Test
	public void testTranslateModelWhereIn() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123 where id in ('1',2,3)").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT _C111_ FROM T123 WHERE _C444_ IN ( :b0, :b1, :b2 )",element.toSql());
		assertEquals(new Long(1), parameters.get("b0"));
		assertEquals(new Long(2), parameters.get("b1"));
		assertEquals(new Long(3), parameters.get("b2"));
	}
	
	@Test
	public void testTranslateModelWhereLike() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123 where id like '%3'").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT _C111_ FROM T123 WHERE _C444_ LIKE :b0",element.toSql());
		assertEquals("%3", parameters.get("b0"));
	}
	
	@Test
	public void testTranslateModelWhereNull() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123 where id is not null").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT _C111_ FROM T123 WHERE _C444_ IS NOT NULL",element.toSql());
	}
	
	@Test
	public void testTranslateModelWhereIsTrue() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123 where id is true").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT _C111_ FROM T123 WHERE _C444_ IS TRUE",element.toSql());
	}
	
	@Test
	public void testTranslateModelWhereIsNaN() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123 where isNaN(aDouble)").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT _C111_ FROM T123 WHERE ( _DBL_C777_ IS NOT NULL AND _DBL_C777_ = 'NaN' )",element.toSql());
	}
	
	@Test
	public void testTranslateModelWhereIsInfinity() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123 where isInfinity(aDouble)").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT _C111_ FROM T123 WHERE ( _DBL_C777_ IS NOT NULL AND _DBL_C777_ IN ( '-Infinity', 'Infinity' ) )",element.toSql());
	}
	
	@Test
	public void testTranslateModelGroupBy() throws ParseException{
		QuerySpecification element = new TableQueryParser("select bar, count(foo) from syn123 group by bar").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT _C333_, COUNT(_C111_) FROM T123 GROUP BY _C333_",element.toSql());
	}
	
	@Test
	public void testTranslateModelOrderBy() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123 order by bar").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT _C111_ FROM T123 ORDER BY _C333_",element.toSql());
	}
	
	@Test
	public void testTranslateModelOrderByFunction() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123 order by max(bar)").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT _C111_ FROM T123 ORDER BY MAX(_C333_)",element.toSql());
	}
	
	@Test
	public void testTranslateModelOrderDouble() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123 order by aDouble").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT _C111_ FROM T123 ORDER BY _C777_",element.toSql());
	}
	
	@Test
	public void testTranslateModelOrderDoubleAs() throws ParseException{
		QuerySpecification element = new TableQueryParser("select aDouble as f1 from syn123 order by f1").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT CASE WHEN _DBL_C777_ IS NULL THEN _C777_ ELSE _DBL_C777_ END AS f1 FROM T123 ORDER BY f1",element.toSql());
	}
	
	@Test
	public void testTranslateModelOrderFunctionDouble() throws ParseException{
		QuerySpecification element = new TableQueryParser("select foo from syn123 order by min(aDouble)").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT _C111_ FROM T123 ORDER BY MIN(_C777_)",element.toSql());
	}
	
	@Test
	public void testTranslateModelSelectArithmetic() throws ParseException{
		QuerySpecification element = new TableQueryParser("select -(2+2)*10").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT -(2+2)*10",element.toSql());
	}
	
	@Test
	public void testTranslateModelSelectArithmeticRightHandSide() throws ParseException{
		QuerySpecification element = new TableQueryParser("select * from syn123 where foo = -(2+3)*10").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT * FROM T123 WHERE _C111_ = -(:b0+:b1)*:b2",element.toSql());
		assertEquals("2", parameters.get("b0"));
		assertEquals("3", parameters.get("b1"));
		assertEquals("10", parameters.get("b2"));
	}
	
	@Test
	public void testTranslateModelSelectArithmeticFunction() throws ParseException{
		QuerySpecification element = new TableQueryParser("select sum((id+foo)/aDouble) as \"sum\" from syn123").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
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
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
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
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
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
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
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
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT * FROM T123 WHERE _C111_ = _C333_",element.toSql());
	}

	/**
	 * Column reference on the right-hand-side should be replaced with a valid reference to that column.
	 * @throws ParseException
	 */
	@Test
	public void testTranslateModelDelimitedIdentiferRightHandSideMultipleColumnReference() throws ParseException{
		QuerySpecification element = new TableQueryParser("select * from syn123 where foo = \"bar\" + \"foo\"").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT * FROM T123 WHERE _C111_ = _C333_+_C111_",element.toSql());
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
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT * FROM T123 WHERE _C111_ = `notReference`",element.toSql());
	}
	
	@Test
	public void testTranslateModelArithmeticAndColumnReferenceOnRightHandSide() throws ParseException{
		QuerySpecification element = new TableQueryParser("select * from syn123 where foo = 2*3/bar").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT * FROM T123 WHERE _C111_ = :b0*:b1/_C333_",element.toSql());
		assertEquals("2", parameters.get("b0"));
		assertEquals("3", parameters.get("b1"));
	}
	
	@Test
	public void testTranslateModelArithmeticAndColumnReferenceOnRightHandSide2() throws ParseException{
		QuerySpecification element = new TableQueryParser("select * from syn123 where foo = (2+3)/bar").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT * FROM T123 WHERE _C111_ = (:b0+:b1)/_C333_",element.toSql());
	}
	
	@Test
	public void testTranslateModelArithmeticGroupBy() throws ParseException{
		QuerySpecification element = new TableQueryParser("select * from syn123 group by bar/456 - min(bar)").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT * FROM T123 GROUP BY _C333_/456-MIN(_C333_)",element.toSql());
	}
	
	@Test
	public void testTranslateMySqlFunctionRightHandSide() throws ParseException{
		QuerySpecification element = new TableQueryParser("select * from syn123 where foo > unix_timestamp(CURRENT_TIMESTAMP - INTERVAL 1 MONTH)/1000").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
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
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
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
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT * FROM T123 WHERE _C111_ IN ( `one`, `two` )",element.toSql());
	}

	@Test
	public void testTranslateModel_InPredicate_ValueNoQuotes() throws ParseException{
		QuerySpecification element = new TableQueryParser("select * from syn123 where id in(1, 2)").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT * FROM T123 WHERE _C444_ IN ( :b0, :b1 )",element.toSql());
		assertEquals(1L, parameters.get("b0"));
		assertEquals(2L, parameters.get("b1"));
	}

	@Test
	public void testTranslateModel_InPredicate_ValueSingleQuotes() throws ParseException{
		QuerySpecification element = new TableQueryParser("select * from syn123 where foo in('asdf', 'qwerty')").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals("SELECT * FROM T123 WHERE _C111_ IN ( :b0, :b1 )",element.toSql());
		assertEquals("asdf", parameters.get("b0"));
		assertEquals("qwerty", parameters.get("b1"));
	}

	@Test
	public void testTranslateModel_HASKeyword() throws ParseException {
		columnDouble.setColumnType(ColumnType.INTEGER_LIST);
		columnFoo.setColumnType(ColumnType.STRING_LIST);

		//need to recreate the translation reference
		columnMap = new ColumnTranslationReferenceLookup(schema);

		QuerySpecification element = new TableQueryParser( "select * from syn123 where aDouble has (1,2,3) and ( foo has ('yah') or bar = 'yeet')").querySpecification();
		Map<String, Object> parameters = new HashMap<>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		assertEquals( "SELECT * FROM T123 WHERE ROW_ID IN ( SELECT ROW_ID_REF_C777_ FROM T123_INDEX_C777_ WHERE _C777__UNNEST IN ( :b0, :b1, :b2 ) ) AND ( ROW_ID IN ( SELECT ROW_ID_REF_C111_ FROM T123_INDEX_C111_ WHERE _C111__UNNEST IN ( :b3 ) ) OR _C333_ = :b4 )",element.toSql());
		assertEquals(1L, parameters.get("b0"));
		assertEquals(2L, parameters.get("b1"));
		assertEquals(3L, parameters.get("b2"));
		assertEquals("yah", parameters.get("b3"));
		assertEquals("yeet", parameters.get("b4"));
	}

	@Test
	public void testTranslateModel_UnnestArrayColumn() throws ParseException{
		columnFoo.setColumnType(ColumnType.STRING_LIST);//not a list type
		columnMap = new ColumnTranslationReferenceLookup(schema);

		QuerySpecification element = new TableQueryParser("select unnest(foo) , count(*) from syn123 where bar in ('asdf', 'qwerty') group by Unnest(foo)").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		String expectedSql = "SELECT _C111__UNNEST, COUNT(*) " +
				"FROM T123 " +
				"LEFT JOIN T123_INDEX_C111_ ON T123.ROW_ID = T123_INDEX_C111_.ROW_ID_REF_C111_ " +
				"WHERE _C333_ IN ( :b0, :b1 ) " +
				"GROUP BY _C111__UNNEST";
		assertEquals(expectedSql,element.toSql());
		assertEquals("asdf", parameters.get("b0"));
		assertEquals("qwerty", parameters.get("b1"));
	}

	@Test
	public void testTranslateModel_CurrentUserFunction() throws ParseException{
		columnMap = new ColumnTranslationReferenceLookup(schema);
		QuerySpecification element = new TableQueryParser("select count(*) from syn123 where bar = CURRENT_USER()").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		String expectedSql = "SELECT COUNT(*) FROM T123 WHERE _C333_ = :b0";
		assertEquals(expectedSql, element.toSql());
		assertEquals(userId.toString(), parameters.get("b0"));
	}

	@Test
	public void testTranslateModel_translateSynapseFunctions() throws ParseException{
		columnMap = new ColumnTranslationReferenceLookup(schema);
		QuerySpecification element = new TableQueryParser("select bar from syn123 where bar = CURRENT_USER()").querySpecification();
		assertNotNull(element.getFirstElementOfType(CurrentUserFunction.class));
		assertNull(element.getFirstElementOfType(UnsignedLiteral.class));
		SQLTranslatorUtils.translateSynapseFunctions(element, userId);
		assertNull(element.getFirstElementOfType(CurrentUserFunction.class));
        assertNotNull(element.getFirstElementOfType(UnsignedLiteral.class));
        assertEquals(element.getFirstElementOfType(UnsignedLiteral.class).toSql(), userId.toString());
	}

	@Test
	public void testTranslateModel_UnnestArrayColumn_multipleJoins() throws ParseException{
		columnFoo.setColumnType(ColumnType.STRING_LIST);//not a list type
		columnBar.setColumnType(ColumnType.STRING_LIST);//not a list type
		columnMap = new ColumnTranslationReferenceLookup(schema);

		QuerySpecification element = new TableQueryParser("select unnest(foo) , unnest(bar) from syn123").querySpecification();
		Map<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translateModel(element, parameters, columnMap, userId);
		String expectedSql = "SELECT T123_INDEX_C111_._C111_, T123_INDEX_C333_._C333_ " +
				"FROM T123 " +
				"JOIN T123_INDEX_C111_ ON T123.ROW_ID = T123_INDEX_C111_.ROW_ID " +
				"JOIN T123_INDEX_C333_ ON T123.ROW_ID = T123_INDEX_C333_.ROW_ID";
		assertTrue(parameters.isEmpty());
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
	public void testValidateSelectColumnWithRealReference() {
		SelectColumn selectColumn = new SelectColumn();
		selectColumn.setName("model");
		SQLTranslatorUtils.validateSelectColumn(selectColumn, null, new SchemaColumnTranslationReference(columnFoo), new ActualIdentifier(new RegularIdentifier("someColumn")));
	}

	@Test
	public void testValidateSelectColumnWithFunction() {
		SelectColumn selectColumn = new SelectColumn();
		selectColumn.setName("function");
		SQLTranslatorUtils.validateSelectColumn(selectColumn, FunctionReturnType.DOUBLE, null, new ActualIdentifier(new RegularIdentifier("someColumn")));
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

	@Test
	public void testTranslateQueryFilters_nullEmptyList() {
		assertThrows(IllegalArgumentException.class, () ->
				// method under test
				SQLTranslatorUtils.translateQueryFilters(null)
		);

		assertThrows(IllegalArgumentException.class, () ->
				// method under test
				SQLTranslatorUtils.translateQueryFilters(Collections.emptyList())
		);
	}

	@Test
	public void testTranslateQueryFilters_singleColumns() {
		ColumnSingleValueQueryFilter filter = new ColumnSingleValueQueryFilter();
		filter.setColumnName("myCol");
		filter.setOperator(ColumnSingleValueFilterOperator.LIKE);
		filter.setValues(Arrays.asList("foo%", "%bar","%baz%"));

		// method under test
		String searchCondition = SQLTranslatorUtils.translateQueryFilters(Arrays.asList(filter));
		assertEquals("(\"myCol\" LIKE 'foo%' OR \"myCol\" LIKE '%bar' OR \"myCol\" LIKE '%baz%')", searchCondition);
	}

	@Test
	public void testTranslateQueryFilters_multipleColumns(){
		ColumnSingleValueQueryFilter filter = new ColumnSingleValueQueryFilter();
		filter.setColumnName("myCol");
		filter.setOperator(ColumnSingleValueFilterOperator.LIKE);
		filter.setValues(Arrays.asList("foo%", "%bar","%baz%"));

		ColumnSingleValueQueryFilter filter2 = new ColumnSingleValueQueryFilter();
		filter2.setColumnName("otherCol");
		filter2.setOperator(ColumnSingleValueFilterOperator.LIKE);
		filter2.setValues(Arrays.asList("%asdf"));

		// method under test
		String searchCondition = SQLTranslatorUtils.translateQueryFilters(Arrays.asList(filter, filter2));
		assertEquals("(\"myCol\" LIKE 'foo%' OR \"myCol\" LIKE '%bar' OR \"myCol\" LIKE '%baz%') AND (\"otherCol\" LIKE '%asdf')", searchCondition);
	}

	@Test
	public void testTranslateQueryFilters_LikeFilter_singleValues(){
		ColumnSingleValueQueryFilter filter = new ColumnSingleValueQueryFilter();
		filter.setColumnName("myCol");
		filter.setOperator(ColumnSingleValueFilterOperator.LIKE);
		filter.setValues(Arrays.asList("foo%"));

		StringBuilder builder = new StringBuilder();
		// method under test
		SQLTranslatorUtils.translateQueryFilters(builder, filter);
		assertEquals("(\"myCol\" LIKE 'foo%')", builder.toString());
	}

	@Test
	public void testTranslateQueryFilters_LikeFilter_multipleValues(){
		ColumnSingleValueQueryFilter filter = new ColumnSingleValueQueryFilter();
		filter.setColumnName("myCol");
		filter.setOperator(ColumnSingleValueFilterOperator.LIKE);
		filter.setValues(Arrays.asList("foo%", "%bar","%baz%"));

		StringBuilder builder = new StringBuilder();
		// method under test
		SQLTranslatorUtils.translateQueryFilters(builder, filter);
		assertEquals("(\"myCol\" LIKE 'foo%' OR \"myCol\" LIKE '%bar' OR \"myCol\" LIKE '%baz%')", builder.toString());
	}

	@Test
	public void testTranslateQueryFilters_LikeFilter_nullEmptyColName(){
		ColumnSingleValueQueryFilter filter = new ColumnSingleValueQueryFilter();
		filter.setOperator(ColumnSingleValueFilterOperator.LIKE);
		filter.setValues(Arrays.asList("foo%", "%bar","%baz%"));
		StringBuilder builder = new StringBuilder();

		filter.setColumnName(null);
		assertThrows(IllegalArgumentException.class, ()->
				// method under test
				SQLTranslatorUtils.translateQueryFilters(builder, filter)
		);

		filter.setColumnName("");
		assertThrows(IllegalArgumentException.class, ()->
				// method under test
				SQLTranslatorUtils.translateQueryFilters(builder, filter)
		);
	}

	@Test
	public void testTranslateQueryFilters_LikeFilter_nullEmptyValues(){
		ColumnSingleValueQueryFilter filter = new ColumnSingleValueQueryFilter();
		filter.setColumnName("myCol");
		StringBuilder builder = new StringBuilder();
		filter.setOperator(ColumnSingleValueFilterOperator.LIKE);
		filter.setValues(null);
		assertThrows(IllegalArgumentException.class, ()->
				// method under test
				SQLTranslatorUtils.translateQueryFilters(builder, filter)
		);

		filter.setValues(Collections.emptyList());
		assertThrows(IllegalArgumentException.class, ()->
				// method under test
				SQLTranslatorUtils.translateQueryFilters(builder, filter)
		);
	}

	@Test
	public void testTranslateQueryFilters_UnknownImplementation(){
		QueryFilter filter = new QueryFilter(){
			@Override
			public JSONObjectAdapter initializeFromJSONObject(JSONObjectAdapter jsonObjectAdapter) throws JSONObjectAdapterException {
				return null;
			}

			@Override
			public JSONObjectAdapter writeToJSONObject(JSONObjectAdapter jsonObjectAdapter) throws JSONObjectAdapterException {
				return null;
			}
		};

		assertThrows(IllegalArgumentException.class, ()->
				// method under test
				SQLTranslatorUtils.translateQueryFilters(new StringBuilder(), filter)
		);
	}
}
