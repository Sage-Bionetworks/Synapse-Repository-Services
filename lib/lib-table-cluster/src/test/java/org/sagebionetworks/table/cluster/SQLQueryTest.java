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
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.model.visitors.ToTranslatedSqlVisitor;
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
	
	private static final String DOUBLE_COLUMN = "CASE WHEN _DBL_C777_ IS NULL THEN _C777_ ELSE _DBL_C777_ END";
	private static final String STAR_COLUMNS = "_C111_, _C222_, _C333_, _C444_, _C555_, _C666_, " + DOUBLE_COLUMN
			+ " AS _C777_, _C888_, _C999_";

	@Before
	public void before(){
		columnNameToModelMap = Maps.newLinkedHashMap(); // retains order of values
		columnNameToModelMap.put("foo", TableModelTestUtils.createColumn(111L, "foo", ColumnType.STRING));
		columnNameToModelMap.put("has space", TableModelTestUtils.createColumn(222L, "has space", ColumnType.STRING));
		columnNameToModelMap.put("bar", TableModelTestUtils.createColumn(333L, "bar", ColumnType.STRING));
		columnNameToModelMap.put("foo_bar", TableModelTestUtils.createColumn(444L, "foo_bar", ColumnType.STRING));
		columnNameToModelMap.put("Foo", TableModelTestUtils.createColumn(555L, "Foo", ColumnType.STRING));
		columnNameToModelMap.put("datetype", TableModelTestUtils.createColumn(666L, "datetype", ColumnType.DATE));
		columnNameToModelMap.put("doubletype", TableModelTestUtils.createColumn(777L, "doubletype", ColumnType.DOUBLE));
		columnNameToModelMap.put("inttype", TableModelTestUtils.createColumn(888L, "inttype", ColumnType.INTEGER));
		columnNameToModelMap.put("has-hyphen", TableModelTestUtils.createColumn(999L, "has-hyphen", ColumnType.STRING));
		tableSchema = new ArrayList<ColumnModel>(columnNameToModelMap.values());
	}
	
	@Test
	public void testTranslateColumnReferenceNoRightHandSide() throws ParseException{
		ColumnReference columnReference = SqlElementUntils.createColumnReference("foo");
		ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(null, columnNameToModelMap);
		columnReference.doVisit(visitor);
		assertEquals("_C111_", visitor.getSql());
	}
	
	@Test
	public void testTranslateColumnReferenceCaseSensitive() throws ParseException{
		ColumnReference columnReference = SqlElementUntils.createColumnReference("Foo");
		ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(null, columnNameToModelMap);
		columnReference.doVisit(visitor);
		assertEquals("_C555_", visitor.getSql());
	}
	
	@Test
	public void testTranslateColumnReferenceTrim() throws ParseException{
		ColumnReference columnReference = SqlElementUntils.createColumnReference("Foo ");
		ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(null, columnNameToModelMap);
		columnReference.doVisit(visitor);
		assertEquals("_C555_", visitor.getSql());
	}
	
	@Test 
	public void testTranslateColumnReferenceUnknownColumn() throws ParseException{
		try{
			ColumnReference columnReference = SqlElementUntils.createColumnReference("fake");
			ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(null, columnNameToModelMap);
			columnReference.doVisit(visitor);
			fail("this column does not exist so it should have failed.");
		}catch(IllegalArgumentException e){
			assertTrue(e.getMessage().contains("fake"));
		}
	}
	
	@Test
	public void testTranslateColumnReferenceWithRightHandSide() throws ParseException{
		ColumnReference columnReference = SqlElementUntils.createColumnReference("foo.bar");
		ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(null, columnNameToModelMap);
		columnReference.doVisit(visitor);
		assertEquals("foo__C333_", visitor.getSql());
	}
	
	@Test
	public void testTranslateColumnReferenceWithQuotes() throws ParseException{
		ColumnReference columnReference = SqlElementUntils.createColumnReference("\"has space\"");
		ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(null, columnNameToModelMap);
		columnReference.doVisit(visitor);
		assertEquals("_C222_", visitor.getSql());
	}
	
	@Test
	public void testTranslateColumnReferenceRowId() throws ParseException{
		ColumnReference columnReference = SqlElementUntils.createColumnReference("ROW_ID");
		ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(null, columnNameToModelMap);
		columnReference.doVisit(visitor);
		assertEquals("ROW_ID", visitor.getSql());
	}
	
	@Test
	public void testTranslateColumnReferenceRowVersionIgnoreCase() throws ParseException{
		ColumnReference columnReference = SqlElementUntils.createColumnReference("row_version");
		ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(null, columnNameToModelMap);
		columnReference.doVisit(visitor);
		assertEquals("ROW_VERSION", visitor.getSql());
	}
	
	@Test
	public void testSelectStar() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123", tableSchema);
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertFalse(translator.isAggregatedResult());
		assertNotNull(translator.getSelectColumnModels());
		assertEquals(translator.getSelectColumnModels().selectColumnCount(), 9);
		assertEquals(this.tableSchema, translator.getSelectColumnModels().getColumnModels());
	}

	@Test
	public void testSelectStarEscaping() throws ParseException {
		SqlQuery translator = new SqlQuery("select * from syn123", tableSchema);
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		String sql = translator.getModel().toString();
		assertEquals("SELECT foo, \"has space\", bar, foo_bar, Foo, datetype, doubletype, inttype, \"has-hyphen\" FROM syn123", sql);
		translator = new SqlQuery(sql, tableSchema);
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
	}

	@Test
	public void testSelectSingleColumns() throws ParseException {
		SqlQuery translator = new SqlQuery("select foo from syn123", tableSchema);
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertFalse(translator.isAggregatedResult());
		List<ColumnModel> expectedSelect = Arrays.asList(columnNameToModelMap.get("foo"));
		assertEquals(expectedSelect, translator.getSelectColumnModels().getColumnModels());
	}
	
	@Test
	public void testSelectMultipleColumns() throws ParseException{
		SqlQuery translator = new SqlQuery("select foo, bar from syn123", tableSchema);
		assertEquals("SELECT _C111_, _C333_, ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertFalse(translator.isAggregatedResult());
		List<ColumnModel> expectedSelect = Arrays.asList(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar"));
		assertEquals(expectedSelect, translator.getSelectColumnModels().getColumnModels());
	}
	
	@Test
	public void testSelectDistinct() throws ParseException{
		SqlQuery translator = new SqlQuery("select distinct foo, bar from syn123", tableSchema);
		assertEquals("SELECT DISTINCT _C111_, _C333_ FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		List<SelectColumn> expectedSelect = Lists.newArrayList(TableModelUtils.createSelectColumn("foo", ColumnType.STRING, null),
				TableModelUtils.createSelectColumn("bar", ColumnType.STRING, null));
		assertEquals(expectedSelect, translator.getSelectColumnModels().getSelectColumns());
	}
	
	@Test
	public void testSelectConstant() throws ParseException {
		SqlQuery translator = new SqlQuery("select 'not a foo' from syn123", tableSchema);
		assertEquals("SELECT 'not a foo', ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertEquals(Lists.newArrayList(TableModelUtils.createSelectColumn("not a foo", ColumnType.STRING, null)), translator
				.getSelectColumnModels().getSelectColumns());
		assertEquals("not a foo", translator.getSelectColumnModels().getSelectColumns().get(0).getName());
	}

	@Test
	public void testSelectCountStar() throws ParseException{
		SqlQuery translator = new SqlQuery("select count(*) from syn123", tableSchema);
		assertEquals("SELECT COUNT(*) FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertEquals(Lists.newArrayList(TableModelUtils.createSelectColumn("COUNT(*)", ColumnType.INTEGER, null)), translator
				.getSelectColumnModels().getSelectColumns());
	}
	
	@Test
	public void testSelectAggregate() throws ParseException{
		SqlQuery translator = new SqlQuery("select avg(inttype) from syn123", tableSchema);
		assertEquals("SELECT AVG(_C888_) FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertEquals(Lists.newArrayList(TableModelUtils.createSelectColumn("AVG(inttype)", ColumnType.DOUBLE, null)), translator
				.getSelectColumnModels().getSelectColumns());
	}
	
	@Test
	public void testSelectAggregateMoreColumns() throws ParseException {
		SqlQuery translator = new SqlQuery("select avg(inttype), bar from syn123", tableSchema);
		assertEquals("SELECT AVG(_C888_), _C333_ FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertEquals(Lists.newArrayList(TableModelUtils.createSelectColumn("AVG(inttype)", ColumnType.DOUBLE, null),
				TableModelUtils.createSelectColumn("bar", ColumnType.STRING, null)), translator.getSelectColumnModels().getSelectColumns());
	}

	@Test
	public void testSelectGroupByAggregate() throws ParseException {
		SqlQuery translator = new SqlQuery("select foo from syn123 group by foo", tableSchema);
		assertEquals("SELECT _C111_ FROM T123 GROUP BY _C111_", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertEquals(Lists.newArrayList(TableModelUtils.createSelectColumn("foo", ColumnType.STRING, null)), translator
				.getSelectColumnModels().getSelectColumns());
	}

	@Test
	public void testSelectAggregateMultiple() throws ParseException{
		SqlQuery translator = new SqlQuery("select min(foo), max(bar) from syn123", tableSchema);
		assertEquals("SELECT MIN(_C111_), MAX(_C333_) FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertEquals(Lists.newArrayList(TableModelUtils.createSelectColumn("MIN(foo)", ColumnType.STRING, null),
				TableModelUtils.createSelectColumn("MAX(bar)", ColumnType.STRING, null)), translator.getSelectColumnModels()
				.getSelectColumns());
	}
	
	@Test
	public void testSelectDistinctAggregate() throws ParseException{
		SqlQuery translator = new SqlQuery("select count(distinct foo) from syn123", tableSchema);
		assertEquals("SELECT COUNT(DISTINCT _C111_) FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertEquals(Lists.newArrayList(TableModelUtils.createSelectColumn("COUNT(DISTINCT foo)", ColumnType.INTEGER, null)), translator
				.getSelectColumnModels().getSelectColumns());
	}
	
	@Test
	public void testComparisonPredicate() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo <> 1");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(parameters, columnNameToModelMap);
		predicate.doVisit(visitor);
		assertEquals("_C111_ <> :b0", visitor.getSql());
		assertEquals("1", parameters.get("b0"));
	}
	
	@Test
	public void testStringComparisonPredicate() throws ParseException {
		Predicate predicate = SqlElementUntils.createPredicate("foo <> 'aaa'");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(parameters, columnNameToModelMap);
		predicate.doVisit(visitor);
		assertEquals("_C111_ <> :b0", visitor.getSql());
		assertEquals("aaa", parameters.get("b0"));
	}

	@Test
	public void testStringComparisonBooleanPredicate() throws ParseException {
		Predicate predicate = SqlElementUntils.createPredicate("foo = true");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(parameters, columnNameToModelMap);
		predicate.doVisit(visitor);
		assertEquals("_C111_ = TRUE", visitor.getSql());
		assertEquals(0, parameters.size());
	}

	@Test
	public void testComparisonPredicateDateNumber() throws ParseException {
		Predicate predicate = SqlElementUntils.createPredicate("datetype <> 1");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(parameters, columnNameToModelMap);
		predicate.doVisit(visitor);
		assertEquals("_C666_ <> :b0", visitor.getSql());
		assertEquals("1", parameters.get("b0"));
	}

	@Test
	public void testComparisonPredicateDateString() throws ParseException {
		Predicate predicate = SqlElementUntils.createPredicate("datetype <> '2011-11-11'");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(parameters, columnNameToModelMap);
		predicate.doVisit(visitor);
		assertEquals("_C666_ <> :b0", visitor.getSql());
		assertEquals(DATE1TIME, parameters.get("b0"));
	}

	@Test
	public void testComparisonPredicateDateParsing() throws ParseException {
		for (String date : new String[] { DATE1, "2011-11-11", "2011-11-11 0:00", "2011-11-11 0:00:00", "2011-11-11 0:00:00.0",
				"2011-11-11 0:00:00.00", "2011-11-11 0:00:00.000" }) {
			HashMap<String, Object> parameters = new HashMap<String, Object>();

			ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(parameters, columnNameToModelMap);
			SqlElementUntils.createPredicate("datetype <> '" + date + "'").doVisit(visitor);
			assertEquals("_C666_ <> :b0", visitor.getSql());
			assertEquals(DATE1TIME, parameters.get("b0"));
		}
		for (String date : new String[] { "2001-01-01", "2001-01-01", "2001-1-1", "2001-1-01", "2001-01-1" }) {
			HashMap<String, Object> parameters = new HashMap<String, Object>();

			ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(parameters, columnNameToModelMap);
			SqlElementUntils.createPredicate("datetype <> '" + date + "'").doVisit(visitor);
			assertEquals("_C666_ <> :b0", visitor.getSql());
			assertEquals("978307200000", parameters.get("b0"));
		}
		for (String date : new String[] { "2011-11-11 01:01:01.001", "2011-11-11 1:01:1.001", "2011-11-11 1:1:1.001" }) {
			HashMap<String, Object> parameters = new HashMap<String, Object>();

			ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(parameters, columnNameToModelMap);
			SqlElementUntils.createPredicate("datetype <> '" + date + "'").doVisit(visitor);
			assertEquals("_C666_ <> :b0", visitor.getSql());
			assertEquals("1320973261001", parameters.get("b0"));
		}
	}

	@Test
	public void testInPredicateOne() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo in(1)");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(parameters, columnNameToModelMap);
		predicate.doVisit(visitor);
		assertEquals("_C111_ IN ( :b0 )", visitor.getSql());
		assertEquals("1", parameters.get("b0"));
	}
	
	@Test
	public void testInPredicateMore() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo in(1,2,3)");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(parameters, columnNameToModelMap);
		predicate.doVisit(visitor);
		assertEquals("_C111_ IN ( :b0, :b1, :b2 )", visitor.getSql());
		assertEquals("1", parameters.get("b0"));
		assertEquals("2", parameters.get("b1"));
		assertEquals("3", parameters.get("b2"));
	}
	
	@Test
	public void testInPredicateDate() throws ParseException {
		Predicate predicate = SqlElementUntils.createPredicate("datetype in('" + DATE1 + "','" + DATE2 + "')");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(parameters, columnNameToModelMap);
		predicate.doVisit(visitor);
		assertEquals("_C666_ IN ( :b0, :b1 )", visitor.getSql());
		assertEquals(DATE1TIME, parameters.get("b0"));
		assertEquals(DATE2TIME, parameters.get("b1"));
	}

	@Test
	public void testBetweenPredicate() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo between 1 and 2");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(parameters, columnNameToModelMap);
		predicate.doVisit(visitor);
		assertEquals("_C111_ BETWEEN :b0 AND :b1", visitor.getSql());
		assertEquals("1", parameters.get("b0"));
		assertEquals("2", parameters.get("b1"));
	}
	
	@Test
	public void testBetweenPredicateDate() throws ParseException {
		Predicate predicate = SqlElementUntils.createPredicate("datetype between '" + DATE1 + "' and '" + DATE2 + "'");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(parameters, columnNameToModelMap);
		predicate.doVisit(visitor);
		assertEquals("_C666_ BETWEEN :b0 AND :b1", visitor.getSql());
		assertEquals(DATE1TIME, parameters.get("b0"));
		assertEquals(DATE2TIME, parameters.get("b1"));
	}

	@Test
	public void testBetweenPredicateNot() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo not between 1 and 2");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(parameters, columnNameToModelMap);
		predicate.doVisit(visitor);
		assertEquals("_C111_ NOT BETWEEN :b0 AND :b1", visitor.getSql());
		assertEquals("1", parameters.get("b0"));
		assertEquals("2", parameters.get("b1"));
	}
	
	@Test
	public void testLikePredicate() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo like 'bar%'");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(parameters, columnNameToModelMap);
		predicate.doVisit(visitor);
		assertEquals("_C111_ LIKE :b0", visitor.getSql());
		assertEquals("bar%",parameters.get("b0"));
	}
	
	@Test
	public void testLikePredicateEscape() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo like 'bar|_' escape '|'");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(parameters, columnNameToModelMap);
		predicate.doVisit(visitor);
		assertEquals("_C111_ LIKE :b0 ESCAPE :b1", visitor.getSql());
		assertEquals("bar|_",parameters.get("b0"));
		assertEquals("|",parameters.get("b1"));
	}
	
	@Test
	public void testLikePredicateNot() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo not like 'bar%'");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(parameters, columnNameToModelMap);
		predicate.doVisit(visitor);
		assertEquals("_C111_ NOT LIKE :b0", visitor.getSql());
		assertEquals("bar%",parameters.get("b0"));
	}
	
	@Test
	public void testNullPredicate() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo is null");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(parameters, columnNameToModelMap);
		predicate.doVisit(visitor);
		assertEquals("_C111_ IS NULL", visitor.getSql());
	}
	
	@Test
	public void testNullPredicateNot() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo is not null");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		ToTranslatedSqlVisitor visitor = new ToTranslatedSqlVisitorImpl(parameters, columnNameToModelMap);
		predicate.doVisit(visitor);
		assertEquals("_C111_ IS NOT NULL", visitor.getSql());
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
	public void testWhereDouble() throws ParseException {
		SqlQuery translator = new SqlQuery("select * from syn123 where isNaN(doubletype) or isInfinity(DOUBLETYPE)", tableSchema);
		// The value should be bound in the SQL
		assertEquals(
				"SELECT "
						+ STAR_COLUMNS
						+ ", ROW_ID, ROW_VERSION FROM T123 WHERE (_DBL_C777_ IS NOT NULL AND _DBL_C777_ = 'NaN') OR (_DBL_C777_ IS NOT NULL AND _DBL_C777_ IN ('-Infinity', 'Infinity'))",
				translator.getOutputSQL());
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
		SqlQuery translator = new SqlQuery("select * from syn123 where (foo = 1 and bar = 2) or foo_bar = 3", tableSchema);
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
	public void testFoundRows() throws ParseException {
		SqlQuery translator = new SqlQuery("select found_rows() from syn123", tableSchema);
		assertEquals("SELECT FOUND_ROWS() FROM T123", translator.getOutputSQL());
	}

	@Test
	public void testAllParts() throws ParseException{
		SqlQuery translator = new SqlQuery("select foo, bar from syn123 where foo_bar >= 1.89e4 order by bar desc limit 10 offset 0",
				tableSchema);
		// The value should be bound in the SQL
		assertEquals("SELECT _C111_, _C333_, ROW_ID, ROW_VERSION FROM T123 WHERE _C444_ >= :b0 ORDER BY _C333_ DESC LIMIT :b1 OFFSET :b2",
				translator.getOutputSQL());
		assertEquals("1.89e4", translator.getParameters().get("b0"));
		assertEquals(10L, translator.getParameters().get("b1"));
		assertEquals(0L, translator.getParameters().get("b2"));
	}

	@Test
	public void testAllPartsWithGrouping() throws ParseException {
		SqlQuery translator = new SqlQuery(
				"select found_rows(), foo, bar from syn123 where foo_bar >= 1.89e4 group by foo order by bar desc limit 10 offset 0",
				tableSchema);
		// The value should be bound in the SQL
		assertEquals(
				"SELECT FOUND_ROWS(), _C111_, _C333_ FROM T123 WHERE _C444_ >= :b0 GROUP BY _C111_ ORDER BY _C333_ DESC LIMIT :b1 OFFSET :b2",
				translator.getOutputSQL());
		assertEquals("1.89e4",translator.getParameters().get("b0"));
		assertEquals(10L,translator.getParameters().get("b1"));
		assertEquals(0L,translator.getParameters().get("b2"));
	}

	@Test
	public void testTypeSetFunctionStrings() throws Exception {
		SqlQuery translator = new SqlQuery("select found_rows(), count(*), min(foo), max(foo), count(foo) from syn123", tableSchema);
		assertEquals("SELECT FOUND_ROWS(), COUNT(*), MIN(_C111_), MAX(_C111_), COUNT(_C111_) FROM T123", translator.getOutputSQL());
		assertEquals(TableModelUtils.createSelectColumn("FOUND_ROWS()", ColumnType.INTEGER, null), translator.getSelectColumnModels()
				.getSelectColumns().get(0));
		assertEquals(TableModelUtils.createSelectColumn("COUNT(*)", ColumnType.INTEGER, null), translator.getSelectColumnModels()
				.getSelectColumns().get(1));
		assertEquals(TableModelUtils.createSelectColumn("MIN(foo)", ColumnType.STRING, null), translator.getSelectColumnModels()
				.getSelectColumns().get(2));
		assertEquals(TableModelUtils.createSelectColumn("MAX(foo)", ColumnType.STRING, null), translator.getSelectColumnModels()
				.getSelectColumns().get(3));
		assertEquals(TableModelUtils.createSelectColumn("COUNT(foo)", ColumnType.INTEGER, null), translator.getSelectColumnModels()
				.getSelectColumns().get(4));
	}

	@Test
	public void testTypeSetFunctionIntegers() throws Exception {
		SqlQuery translator = new SqlQuery("select min(inttype), max(inttype), sum(inttype), avg(inttype), count(inttype) from syn123",
				tableSchema);
		assertEquals("SELECT MIN(_C888_), MAX(_C888_), SUM(_C888_), AVG(_C888_), COUNT(_C888_) FROM T123", translator.getOutputSQL());
		assertEquals(TableModelUtils.createSelectColumn("MIN(inttype)", ColumnType.INTEGER, null), translator.getSelectColumnModels()
				.getSelectColumns().get(0));
		assertEquals(TableModelUtils.createSelectColumn("MAX(inttype)", ColumnType.INTEGER, null), translator.getSelectColumnModels()
				.getSelectColumns().get(1));
		assertEquals(TableModelUtils.createSelectColumn("SUM(inttype)", ColumnType.INTEGER, null), translator.getSelectColumnModels()
				.getSelectColumns().get(2));
		assertEquals(TableModelUtils.createSelectColumn("AVG(inttype)", ColumnType.DOUBLE, null), translator.getSelectColumnModels()
				.getSelectColumns().get(3));
		assertEquals(TableModelUtils.createSelectColumn("COUNT(inttype)", ColumnType.INTEGER, null), translator.getSelectColumnModels()
				.getSelectColumns().get(4));
	}

	@Test
	public void testTypeSetFunctionDoubles() throws Exception {
		SqlQuery translator = new SqlQuery(
				"select min(doubletype), max(doubletype), sum(doubletype), avg(doubletype), count(doubletype) from syn123", tableSchema);
		assertEquals("SELECT MIN(" + DOUBLE_COLUMN + "), MAX(" + DOUBLE_COLUMN + "), SUM(" + DOUBLE_COLUMN + "), AVG(" + DOUBLE_COLUMN
				+ "), COUNT(" + DOUBLE_COLUMN + ") FROM T123", translator.getOutputSQL());
		assertEquals(TableModelUtils.createSelectColumn("MIN(doubletype)", ColumnType.DOUBLE, null), translator.getSelectColumnModels()
				.getSelectColumns().get(0));
		assertEquals(TableModelUtils.createSelectColumn("MAX(doubletype)", ColumnType.DOUBLE, null), translator.getSelectColumnModels()
				.getSelectColumns().get(1));
		assertEquals(TableModelUtils.createSelectColumn("SUM(doubletype)", ColumnType.DOUBLE, null), translator.getSelectColumnModels()
				.getSelectColumns().get(2));
		assertEquals(TableModelUtils.createSelectColumn("AVG(doubletype)", ColumnType.DOUBLE, null), translator.getSelectColumnModels()
				.getSelectColumns().get(3));
		assertEquals(TableModelUtils.createSelectColumn("COUNT(doubletype)", ColumnType.INTEGER, null), translator.getSelectColumnModels()
				.getSelectColumns().get(4));
	}
}
