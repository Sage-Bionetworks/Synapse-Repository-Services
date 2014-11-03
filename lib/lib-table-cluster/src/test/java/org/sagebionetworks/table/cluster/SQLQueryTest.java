package org.sagebionetworks.table.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.util.SqlElementUntils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class SQLQueryTest {
	
	private static final String DATE1 = "11-11-11";
	private static final String DATE1TIME = "1320969600000";

	private static final String DATE2TIME = "1298332800000";
	private static final String DATE2 = "11-02-22";

	Map<String, ColumnModel> columnNameToModelMap;
	List<ColumnModel> tableSchema;
	
	private static final String DOUBLE_COLUMN = "CASE WHEN _DBL_C777_ IS NULL THEN _C777_ ELSE _DBL_C777_ END AS _C777_";
	private static final String STAR_COLUMNS = "_C111_, _C222_, _C333_, _C444_, _C555_, _C666_, " + DOUBLE_COLUMN;

	@Before
	public void before(){
		columnNameToModelMap = Maps.newLinkedHashMap(); // retains order of values
		columnNameToModelMap.put("foo", TableModelTestUtils.createColumn(111L, "foo", ColumnType.STRING));
		columnNameToModelMap.put("has space", TableModelTestUtils.createColumn(222L, "has space", ColumnType.STRING));
		columnNameToModelMap.put("bar", TableModelTestUtils.createColumn(333L, "bar", ColumnType.STRING));
		columnNameToModelMap.put("foobar", TableModelTestUtils.createColumn(444L, "foobar", ColumnType.STRING));
		columnNameToModelMap.put("Foo", TableModelTestUtils.createColumn(555L, "Foo", ColumnType.STRING));
		columnNameToModelMap.put("datetype", TableModelTestUtils.createColumn(666L, "datetype", ColumnType.DATE));
		columnNameToModelMap.put("doubletype", TableModelTestUtils.createColumn(777L, "doubletype", ColumnType.DOUBLE));
		tableSchema = new ArrayList<ColumnModel>(columnNameToModelMap.values());
	}
	
	@Test
	public void testTranslateColumnReferenceNoRightHandSide() throws ParseException{
		ColumnReference columnReference = SqlElementUntils.createColumnReference("foo");
		StringBuilder builder = new StringBuilder();
		columnReference.toSQL(builder, SQLTranslatorUtils.createColumnConvertor(null, columnNameToModelMap));
		assertEquals("_C111_", builder.toString());
	}
	
	@Test
	public void testTranslateColumnReferenceCaseSensitive() throws ParseException{
		ColumnReference columnReference = SqlElementUntils.createColumnReference("Foo");
		StringBuilder builder = new StringBuilder();
		columnReference.toSQL(builder, SQLTranslatorUtils.createColumnConvertor(null, columnNameToModelMap));
		assertEquals("_C555_", builder.toString());
	}
	
	@Test
	public void testTranslateColumnReferenceTrim() throws ParseException{
		ColumnReference columnReference = SqlElementUntils.createColumnReference("Foo ");
		StringBuilder builder = new StringBuilder();
		columnReference.toSQL(builder, SQLTranslatorUtils.createColumnConvertor(null, columnNameToModelMap));
		assertEquals("_C555_", builder.toString());
	}
	
	@Test 
	public void testTranslateColumnReferenceUnknownColumn() throws ParseException{
		try{
			ColumnReference columnReference = SqlElementUntils.createColumnReference("fake");
			StringBuilder builder = new StringBuilder();
			columnReference.toSQL(builder, SQLTranslatorUtils.createColumnConvertor(null, columnNameToModelMap));
			fail("this column does not exist so it should have failed.");
		}catch(IllegalArgumentException e){
			assertTrue(e.getMessage().contains("fake"));
		}
	}
	
	@Test
	public void testTranslateColumnReferenceWithRightHandSide() throws ParseException{
		ColumnReference columnReference = SqlElementUntils.createColumnReference("foo.bar");
		StringBuilder builder = new StringBuilder();
		columnReference.toSQL(builder, SQLTranslatorUtils.createColumnConvertor(null, columnNameToModelMap));
		assertEquals("foo__C333_", builder.toString());
	}
	
	@Test
	public void testTranslateColumnReferenceWithQuotes() throws ParseException{
		ColumnReference columnReference = SqlElementUntils.createColumnReference("\"has space\"");
		StringBuilder builder = new StringBuilder();
		columnReference.toSQL(builder, SQLTranslatorUtils.createColumnConvertor(null, columnNameToModelMap));
		assertEquals("_C222_", builder.toString());
	}
	
	@Test
	public void testTranslateColumnReferenceRowId() throws ParseException{
		ColumnReference columnReference = SqlElementUntils.createColumnReference("ROW_ID");
		StringBuilder builder = new StringBuilder();
		columnReference.toSQL(builder, SQLTranslatorUtils.createColumnConvertor(null, columnNameToModelMap));
		assertEquals("ROW_ID", builder.toString());
	}
	
	@Test
	public void testTranslateColumnReferenceRowVersionIgnoreCase() throws ParseException{
		ColumnReference columnReference = SqlElementUntils.createColumnReference("row_version");
		StringBuilder builder = new StringBuilder();
		columnReference.toSQL(builder, SQLTranslatorUtils.createColumnConvertor(null, columnNameToModelMap));
		assertEquals("ROW_VERSION", builder.toString());
	}
	
	@Test
	public void testSelectStar() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123", tableSchema);
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertFalse(translator.isAggregatedResult());
		assertNotNull(translator.getSelectColumnModels());
		assertEquals(translator.getSelectColumnModels().size(), 7);
		assertTrue(translator.getSelectColumnModels().containsAll(this.tableSchema));
	}
	@Test
	public void testSelectSingColumns() throws ParseException{
		SqlQuery translator = new SqlQuery("select foo from syn123", tableSchema);
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertFalse(translator.isAggregatedResult());
		List<ColumnModel> expectedSelect = Arrays.asList(columnNameToModelMap.get("foo"));
		assertEquals(expectedSelect, translator.getSelectColumnModels());
	}
	
	@Test
	public void testSelectMultipleColumns() throws ParseException{
		SqlQuery translator = new SqlQuery("select foo, bar from syn123", tableSchema);
		assertEquals("SELECT _C111_, _C333_, ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertFalse(translator.isAggregatedResult());
		List<ColumnModel> expectedSelect = Arrays.asList(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar"));
		assertEquals(expectedSelect, translator.getSelectColumnModels());
	}
	
	@Test
	public void testSelectDistinct() throws ParseException{
		SqlQuery translator = new SqlQuery("select distinct foo, bar from syn123", tableSchema);
		assertEquals("SELECT DISTINCT _C111_, _C333_, ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertFalse(translator.isAggregatedResult());
		List<ColumnModel> expectedSelect = Arrays.asList(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar"));
		assertEquals(expectedSelect, translator.getSelectColumnModels());
	}
	
	@Test
	public void testSelectCountStar() throws ParseException{
		SqlQuery translator = new SqlQuery("select count(*) from syn123", tableSchema);
		assertEquals("SELECT COUNT(*) FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertTrue(translator.getSelectColumnModels().isEmpty());
	}
	
	@Test
	public void testSelectAggregate() throws ParseException{
		SqlQuery translator = new SqlQuery("select avg(foo) from syn123", tableSchema);
		assertEquals("SELECT AVG(_C111_) FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertTrue(translator.getSelectColumnModels().isEmpty());
	}
	
	@Test
	public void testSelectGroupByAggregate() throws ParseException {
		SqlQuery translator = new SqlQuery("select foo from syn123 group by foo", tableSchema);
		assertEquals("SELECT _C111_ FROM T123 GROUP BY _C111_", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertEquals(Lists.newArrayList(tableSchema.get(0)), translator.getSelectColumnModels());
	}

	@Test
	public void testSelectAggregateMultiple() throws ParseException{
		SqlQuery translator = new SqlQuery("select avg(foo), max(bar) from syn123", tableSchema);
		assertEquals("SELECT AVG(_C111_), MAX(_C333_) FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertTrue(translator.getSelectColumnModels().isEmpty());
	}
	
	@Test
	public void testSelectDistinctAggregate() throws ParseException{
		SqlQuery translator = new SqlQuery("select count(distinct foo) from syn123", tableSchema);
		assertEquals("SELECT COUNT(DISTINCT _C111_) FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertTrue(translator.getSelectColumnModels().isEmpty());
	}
	
	@Test
	public void testComparisonPredicate() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo <> 1");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		predicate.toSQL(builder, SQLTranslatorUtils.createColumnConvertor(parameters, columnNameToModelMap));
		assertEquals("_C111_ <> :b0", builder.toString());
		assertEquals("1", parameters.get("b0"));
	}
	
	@Test
	public void testStringComparisonPredicate() throws ParseException {
		Predicate predicate = SqlElementUntils.createPredicate("foo <> 'aaa'");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		predicate.toSQL(builder, SQLTranslatorUtils.createColumnConvertor(parameters, columnNameToModelMap));
		assertEquals("_C111_ <> :b0", builder.toString());
		assertEquals("aaa", parameters.get("b0"));
	}

	@Test
	public void testStringComparisonBooleanPredicate() throws ParseException {
		Predicate predicate = SqlElementUntils.createPredicate("foo = true");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		predicate.toSQL(builder, SQLTranslatorUtils.createColumnConvertor(parameters, columnNameToModelMap));
		assertEquals("_C111_ = TRUE", builder.toString());
		assertEquals(0, parameters.size());
	}

	@Test
	public void testComparisonPredicateDateNumber() throws ParseException {
		Predicate predicate = SqlElementUntils.createPredicate("datetype <> 1");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		predicate.toSQL(builder, SQLTranslatorUtils.createColumnConvertor(parameters, columnNameToModelMap));
		assertEquals("_C666_ <> :b0", builder.toString());
		assertEquals("1", parameters.get("b0"));
	}

	@Test
	public void testComparisonPredicateDateString() throws ParseException {
		Predicate predicate = SqlElementUntils.createPredicate("datetype <> '2011-11-11'");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		predicate.toSQL(builder, SQLTranslatorUtils.createColumnConvertor(parameters, columnNameToModelMap));
		assertEquals("_C666_ <> :b0", builder.toString());
		assertEquals(DATE1TIME, parameters.get("b0"));
	}

	@Test
	public void testComparisonPredicateDateParsing() throws ParseException {
		for (String date : new String[] { DATE1, "2011-11-11", "2011-11-11 0:00", "2011-11-11 0:00:00", "2011-11-11 0:00:00.0",
				"2011-11-11 0:00:00.00", "2011-11-11 0:00:00.000" }) {
			StringBuilder builder = new StringBuilder();
			HashMap<String, Object> parameters = new HashMap<String, Object>();

			SqlElementUntils.createPredicate("datetype <> '" + date + "'").toSQL(builder,
					SQLTranslatorUtils.createColumnConvertor(parameters, columnNameToModelMap));
			assertEquals("_C666_ <> :b0", builder.toString());
			assertEquals(DATE1TIME, parameters.get("b0"));
		}
		for (String date : new String[] { "2001-01-01", "2001-01-01", "2001-1-1", "2001-1-01", "2001-01-1" }) {
			StringBuilder builder = new StringBuilder();
			HashMap<String, Object> parameters = new HashMap<String, Object>();

			SqlElementUntils.createPredicate("datetype <> '" + date + "'").toSQL(builder,
					SQLTranslatorUtils.createColumnConvertor(parameters, columnNameToModelMap));
			assertEquals("_C666_ <> :b0", builder.toString());
			assertEquals("978307200000", parameters.get("b0"));
		}
		for (String date : new String[] { "2011-11-11 01:01:01.001", "2011-11-11 1:01:1.001", "2011-11-11 1:1:1.001" }) {
			StringBuilder builder = new StringBuilder();
			HashMap<String, Object> parameters = new HashMap<String, Object>();

			SqlElementUntils.createPredicate("datetype <> '" + date + "'").toSQL(builder,
					SQLTranslatorUtils.createColumnConvertor(parameters, columnNameToModelMap));
			assertEquals("_C666_ <> :b0", builder.toString());
			assertEquals("1320973261001", parameters.get("b0"));
		}
	}

	@Test
	public void testInPredicateOne() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo in(1)");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		predicate.toSQL(builder, SQLTranslatorUtils.createColumnConvertor(parameters, columnNameToModelMap));
		assertEquals("_C111_ IN ( :b0 )", builder.toString());
		assertEquals("1", parameters.get("b0"));
	}
	
	@Test
	public void testInPredicateMore() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo in(1,2,3)");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		predicate.toSQL(builder, SQLTranslatorUtils.createColumnConvertor(parameters, columnNameToModelMap));
		assertEquals("_C111_ IN ( :b0, :b1, :b2 )", builder.toString());
		assertEquals("1", parameters.get("b0"));
		assertEquals("2", parameters.get("b1"));
		assertEquals("3", parameters.get("b2"));
	}
	
	@Test
	public void testInPredicateDate() throws ParseException {
		Predicate predicate = SqlElementUntils.createPredicate("datetype in('" + DATE1 + "','" + DATE2 + "')");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		predicate.toSQL(builder, SQLTranslatorUtils.createColumnConvertor(parameters, columnNameToModelMap));
		assertEquals("_C666_ IN ( :b0, :b1 )", builder.toString());
		assertEquals(DATE1TIME, parameters.get("b0"));
		assertEquals(DATE2TIME, parameters.get("b1"));
	}

	@Test
	public void testBetweenPredicate() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo between 1 and 2");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		predicate.toSQL(builder, SQLTranslatorUtils.createColumnConvertor(parameters, columnNameToModelMap));
		assertEquals("_C111_ BETWEEN :b0 AND :b1", builder.toString());
		assertEquals("1", parameters.get("b0"));
		assertEquals("2", parameters.get("b1"));
	}
	
	@Test
	public void testBetweenPredicateDate() throws ParseException {
		Predicate predicate = SqlElementUntils.createPredicate("datetype between '" + DATE1 + "' and '" + DATE2 + "'");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		predicate.toSQL(builder, SQLTranslatorUtils.createColumnConvertor(parameters, columnNameToModelMap));
		assertEquals("_C666_ BETWEEN :b0 AND :b1", builder.toString());
		assertEquals(DATE1TIME, parameters.get("b0"));
		assertEquals(DATE2TIME, parameters.get("b1"));
	}

	@Test
	public void testBetweenPredicateNot() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo not between 1 and 2");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		predicate.toSQL(builder, SQLTranslatorUtils.createColumnConvertor(parameters, columnNameToModelMap));
		assertEquals("_C111_ NOT BETWEEN :b0 AND :b1", builder.toString());
		assertEquals("1", parameters.get("b0"));
		assertEquals("2", parameters.get("b1"));
	}
	
	@Test
	public void testLikePredicate() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo like 'bar%'");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		predicate.toSQL(builder, SQLTranslatorUtils.createColumnConvertor(parameters, columnNameToModelMap));
		assertEquals("_C111_ LIKE :b0", builder.toString());
		assertEquals("bar%",parameters.get("b0"));
	}
	
	@Test
	public void testLikePredicateEscape() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo like 'bar|_' escape '|'");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		predicate.toSQL(builder, SQLTranslatorUtils.createColumnConvertor(parameters, columnNameToModelMap));
		assertEquals("_C111_ LIKE :b0 ESCAPE :b1", builder.toString());
		assertEquals("bar|_",parameters.get("b0"));
		assertEquals("|",parameters.get("b1"));
	}
	
	@Test
	public void testLikePredicateNot() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo not like 'bar%'");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		predicate.toSQL(builder, SQLTranslatorUtils.createColumnConvertor(parameters, columnNameToModelMap));
		assertEquals("_C111_ NOT LIKE :b0", builder.toString());
		assertEquals("bar%",parameters.get("b0"));
	}
	
	@Test
	public void testNullPredicate() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo is null");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		predicate.toSQL(builder, SQLTranslatorUtils.createColumnConvertor(parameters, columnNameToModelMap));
		assertEquals("_C111_ IS NULL", builder.toString());
	}
	
	@Test
	public void testNullPredicateNot() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo is not null");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		predicate.toSQL(builder, SQLTranslatorUtils.createColumnConvertor(parameters, columnNameToModelMap));
		assertEquals("_C111_ IS NOT NULL", builder.toString());
	}
	
	@Test
	public void testWhereSimple() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123 where foo = 1", tableSchema);
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 WHERE _C111_ = :b0",
				translator.getOutputSQL());
		// The value should be in the parameters map.
		assertEquals("1", translator.getParameters().get("b0"));
	}
	
	@Test
	public void testWhereOr() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123 where foo = 1 or bar = 2", tableSchema);
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 WHERE _C111_ = :b0 OR _C333_ = :b1",
				translator.getOutputSQL());
		// The value should be in the parameters map.
		assertEquals("1", translator.getParameters().get("b0"));
		assertEquals("2", translator.getParameters().get("b1"));
	}
	
	
	@Test
	public void testWhereAnd() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123 where foo = 1 and bar = 2", tableSchema);
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 WHERE _C111_ = :b0 AND _C333_ = :b1",
				translator.getOutputSQL());
		// The value should be in the parameters map.
		assertEquals("1", translator.getParameters().get("b0"));
		assertEquals("2", translator.getParameters().get("b1"));
	}
	
	@Test
	public void testWhereNested() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123 where (foo = 1 and bar = 2) or foobar = 3", tableSchema);
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 WHERE ( _C111_ = :b0 AND _C333_ = :b1 ) OR _C444_ = :b2",
				translator.getOutputSQL());
		// The value should be in the parameters map.
		assertEquals("1", translator.getParameters().get("b0"));
		assertEquals("2", translator.getParameters().get("b1"));
		assertEquals("3", translator.getParameters().get("b2"));
	}
	
	@Test
	public void testGroupByOne() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123 group by foo", tableSchema);
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + " FROM T123 GROUP BY _C111_",
				translator.getOutputSQL());
	}
	
	@Test
	public void testGroupByMultiple() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123 group by foo, bar", tableSchema);
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + " FROM T123 GROUP BY _C111_, _C333_",
				translator.getOutputSQL());
	}
	
	@Test
	public void testOrderByOneNoSpec() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123 order by foo", tableSchema);
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 ORDER BY _C111_",
				translator.getOutputSQL());
	}
	
	@Test
	public void testOrderByOneWithSpec() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123 order by foo desc", tableSchema);
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 ORDER BY _C111_ DESC",
				translator.getOutputSQL());
	}
	
	@Test
	public void testOrderByDouble() throws ParseException {
		SqlQuery translator = new SqlQuery("select * from syn123 order by doubletype desc", tableSchema);
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 ORDER BY T123._C777_ DESC", translator.getOutputSQL());
	}
	
	@Test
	public void testOrderByMultipleNoSpec() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123 order by foo, bar", tableSchema);
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 ORDER BY _C111_, _C333_",
				translator.getOutputSQL());
	}
	
	@Test
	public void testOrderByMultipeWithSpec() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123 order by foo asc, bar desc", tableSchema);
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 ORDER BY _C111_ ASC, _C333_ DESC",
				translator.getOutputSQL());
	}
	
	@Test
	public void testLimit() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123 limit 100", tableSchema);
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 LIMIT :b0",
				translator.getOutputSQL());
		assertEquals(100L,translator.getParameters().get("b0"));
	}
	
	@Test
	public void testLimitAndOffset() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123 limit 100 offset 2", tableSchema);
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 LIMIT :b0 OFFSET :b1",
				translator.getOutputSQL());
		assertEquals(100L,translator.getParameters().get("b0"));
		assertEquals(2L, translator.getParameters().get("b1"));
	}
	
	@Test
	public void testAllParts() throws ParseException{
		SqlQuery translator = new SqlQuery(
				"select found_rows(), foo, bar from syn123 where foobar >= 1.89e4 order by bar desc limit 10 offset 0", tableSchema);
		// The value should be bound in the SQL
		assertEquals(
				"SELECT FOUND_ROWS(), _C111_, _C333_, ROW_ID, ROW_VERSION FROM T123 WHERE _C444_ >= :b0 ORDER BY _C333_ DESC LIMIT :b1 OFFSET :b2",
				translator.getOutputSQL());
		assertEquals("1.89e4", translator.getParameters().get("b0"));
		assertEquals(10L, translator.getParameters().get("b1"));
		assertEquals(0L, translator.getParameters().get("b2"));
	}

	@Test
	public void testAllPartsWithGrouping() throws ParseException {
		SqlQuery translator = new SqlQuery(
				"select found_rows(), foo, bar from syn123 where foobar >= 1.89e4 group by foo order by bar desc limit 10 offset 0",
				tableSchema);
		// The value should be bound in the SQL
		assertEquals(
				"SELECT FOUND_ROWS(), _C111_, _C333_ FROM T123 WHERE _C444_ >= :b0 GROUP BY _C111_ ORDER BY _C333_ DESC LIMIT :b1 OFFSET :b2",
				translator.getOutputSQL());
		assertEquals("1.89e4",translator.getParameters().get("b0"));
		assertEquals(10L,translator.getParameters().get("b1"));
		assertEquals(0L,translator.getParameters().get("b2"));
	}
}
