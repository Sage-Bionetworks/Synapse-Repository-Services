package org.sagebionetworks.table.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.dbo.dao.table.TableModelTestUtils;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.SelectColumn;
import org.sagebionetworks.repo.model.table.SortDirection;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.table.cluster.utils.TableModelUtils;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.TableQueryParser;
import org.sagebionetworks.table.query.model.HasPredicate;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SelectList;
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
			+ ", _C888_, _C999_";
	
	ColumnModel cm;
	List<ColumnModel> schema;
	String sql;

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
		
		cm = new ColumnModel();
		cm.setName("5ormore");
		cm.setColumnType(ColumnType.INTEGER);
		cm.setId("111");
		schema = Lists.newArrayList(cm);
		sql = "select * from syn123";
	}
		
	@Test (expected=IllegalArgumentException.class)
	public void testSelectStarEmtpySchema() throws ParseException{
		tableSchema = new LinkedList<ColumnModel>();
		SqlQuery translator = new SqlQueryBuilder("select * from syn123", tableSchema).build();
	}
	
	@Test
	public void testSelectStar() throws ParseException{
		SqlQuery translator = new SqlQueryBuilder("select * from syn123", tableSchema).build();
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertFalse(translator.isAggregatedResult());
		assertNotNull(translator.getSelectColumns());
		assertEquals(translator.getSelectColumns().size(), 9);
		assertEquals(TableModelUtils.getSelectColumns(this.tableSchema), translator.getSelectColumns());
	}

	@Test
	public void testSelectStarEscaping() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder("select * from syn123", tableSchema).build();
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		String sql = translator.getModel().toString();
		assertEquals("SELECT \"foo\", \"has space\", \"bar\", \"foo_bar\", \"Foo\", \"datetype\", \"doubletype\", \"inttype\", \"has-hyphen\" FROM syn123", sql);
		translator = new SqlQueryBuilder(sql, tableSchema).build();
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
	}

	@Test
	public void testSelectSingleColumns() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder("select foo from syn123", tableSchema).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertFalse(translator.isAggregatedResult());
		List<ColumnModel> expectedSelect = Arrays.asList(columnNameToModelMap.get("foo"));
		assertEquals(TableModelUtils.getSelectColumns(expectedSelect), translator.getSelectColumns());
	}
	
	@Test
	public void testSelectMultipleColumns() throws ParseException{
		SqlQuery translator = new SqlQueryBuilder("select foo, bar from syn123", tableSchema).build();
		assertEquals("SELECT _C111_, _C333_, ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertFalse(translator.isAggregatedResult());
		List<ColumnModel> expectedSelect = Arrays.asList(columnNameToModelMap.get("foo"), columnNameToModelMap.get("bar"));
		assertEquals(TableModelUtils.getSelectColumns(expectedSelect), translator.getSelectColumns());
	}
	
	@Test
	public void testSelectDistinct() throws ParseException{
		SqlQuery translator = new SqlQueryBuilder("select distinct foo, bar from syn123", tableSchema).build();
		assertEquals("SELECT DISTINCT _C111_, _C333_ FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		List<SelectColumn> expectedSelect = Lists.newArrayList(TableModelUtils.createSelectColumn("foo", ColumnType.STRING, null),
				TableModelUtils.createSelectColumn("bar", ColumnType.STRING, null));
		assertEquals(expectedSelect, translator.getSelectColumns());
	}
	
	@Test
	public void selectRowIdAndVersionStar() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder("select * from syn123", tableSchema).build();
		assertTrue(translator.includesRowIdAndVersion());
	}
	
	@Test
	public void selectRowIdAndVersionSingleColumn() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder("select foo from syn123", tableSchema).build();
		assertTrue(translator.includesRowIdAndVersion());
	}
	
	@Test
	public void selectRowIdAndVersionDistinct() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder("select distinct foo from syn123", tableSchema).build();
		assertFalse(translator.includesRowIdAndVersion());
	}
	
	@Test
	public void selectRowIdAndVersionCount() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder("select count(*) from syn123", tableSchema).build();
		assertFalse(translator.includesRowIdAndVersion());
	}
	
	@Test
	public void selectRowIdAndVersionAggregateFunction() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder("select max(foo) from syn123", tableSchema).build();
		assertFalse(translator.includesRowIdAndVersion());
	}
	
	@Test
	public void selectRowIdAndVersionNonAggreageFunction() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder("select concat('a',foo) from syn123", tableSchema).build();
		assertTrue(translator.includesRowIdAndVersion());
	}
	
	@Test
	public void selectRowIdAndVersionConstant() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder("select 'a constant' from syn123", tableSchema).build();
		assertTrue(translator.includesRowIdAndVersion());
	}
	
	@Test
	public void selectRowIdAndVersionConstantPlusColumn() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder("select foo, 'a constant' from syn123", tableSchema).build();
		assertTrue(translator.includesRowIdAndVersion());
	}
	
	@Test
	public void selectRowIdAndVersionArithmeticNoColumns() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder("select 5 div 2 from syn123", tableSchema).build();
		assertTrue(translator.includesRowIdAndVersion());
	}
	
	@Test
	public void selectRowIdAndVersionArithmeticAndColumn() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder("select 5 div 2, foo from syn123", tableSchema).build();
		assertTrue(translator.includesRowIdAndVersion());
	}
	
	@Test
	public void selectRowIdAndVersionArithmeticOfColumns() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder("select 5 div foo from syn123", tableSchema).build();
		assertTrue(translator.includesRowIdAndVersion());
	}
	
	@Test
	public void selectRowIdAndVersionGroupBy() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder("select foo, count(*) from syn123 group by foo", tableSchema).build();
		assertFalse(translator.includesRowIdAndVersion());
	}
	
	@Test
	public void selectRowIdAndVersionAggregateFunctionNoGroup() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder("select foo, max(bar) from syn123", tableSchema).build();
		assertFalse(translator.includesRowIdAndVersion());
	}
	
	@Test
	public void selectRowIdAndNonAggregateFunction() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder("select foo, DAYOFMONTH(bar) from syn123", tableSchema).build();
		assertTrue(translator.includesRowIdAndVersion());
	}
	
	@Test
	public void selectRowIdAndNonAggregateFunctionColumnRef() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder("select DAYOFMONTH(bar) from syn123", tableSchema).build();
		assertTrue(translator.includesRowIdAndVersion());
	}
	
	@Test
	public void selectRowIdAndNonAggregateFunctionOnly() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder("select DAYOFMONTH('2017-12-12') from syn123", tableSchema).build();
		assertTrue(translator.includesRowIdAndVersion());
	}
	
	@Test
	public void selectRowIdAndAs() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder("select foo as \"cats\" from syn123", tableSchema).build();
		assertTrue(translator.includesRowIdAndVersion());
	}
	
	@Test
	public void testSelectConstant() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder("select 'not a foo' from syn123", tableSchema).build();
		assertEquals("SELECT 'not a foo', ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertEquals(Lists.newArrayList(TableModelUtils.createSelectColumn("not a foo", ColumnType.STRING, null)), translator
				.getSelectColumns());
		assertEquals("not a foo", translator.getSelectColumns().get(0).getName());
	}

	@Test
	public void testSelectCountStar() throws ParseException{
		SqlQuery translator = new SqlQueryBuilder("select count(*) from syn123", tableSchema).build();
		assertEquals("SELECT COUNT(*) FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertEquals(Lists.newArrayList(TableModelUtils.createSelectColumn("COUNT(*)", ColumnType.INTEGER, null)), translator
				.getSelectColumns());
	}
	
	@Test
	public void testSelectAggregate() throws ParseException{
		SqlQuery translator = new SqlQueryBuilder("select avg(inttype) from syn123", tableSchema).build();
		assertEquals("SELECT AVG(_C888_) FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertEquals(Lists.newArrayList(TableModelUtils.createSelectColumn("AVG(inttype)", ColumnType.DOUBLE, null)), translator
				.getSelectColumns());
	}
	
	@Test
	public void testSelectAggregateMoreColumns() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder("select avg(inttype), bar from syn123", tableSchema).build();
		assertEquals("SELECT AVG(_C888_), _C333_ FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertEquals(Lists.newArrayList(TableModelUtils.createSelectColumn("AVG(inttype)", ColumnType.DOUBLE, null),
				TableModelUtils.createSelectColumn("bar", ColumnType.STRING, null)), translator.getSelectColumns());
	}

	@Test
	public void testSelectGroupByAggregate() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder("select foo from syn123 group by foo", tableSchema).build();
		assertEquals("SELECT _C111_ FROM T123 GROUP BY _C111_", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertEquals(Lists.newArrayList(TableModelUtils.createSelectColumn("foo", ColumnType.STRING, null)), translator
				.getSelectColumns());
	}

	@Test
	public void testSelectAggregateMultiple() throws ParseException{
		SqlQuery translator = new SqlQueryBuilder("select min(foo), max(bar) from syn123", tableSchema).build();
		assertEquals("SELECT MIN(_C111_), MAX(_C333_) FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertEquals(Lists.newArrayList(TableModelUtils.createSelectColumn("MIN(foo)", ColumnType.STRING, null),
				TableModelUtils.createSelectColumn("MAX(bar)", ColumnType.STRING, null)), translator.getSelectColumns());
	}
	
	@Test
	public void testSelectDistinctAggregate() throws ParseException{
		SqlQuery translator = new SqlQueryBuilder("select count(distinct foo) from syn123", tableSchema).build();
		assertEquals("SELECT COUNT(DISTINCT _C111_) FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertEquals(Lists.newArrayList(TableModelUtils.createSelectColumn("COUNT(DISTINCT foo)", ColumnType.INTEGER, null)), translator
				.getSelectColumns());
	}
	
	@Test
	public void testComparisonPredicate() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo <> 1");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnNameToModelMap);
		assertEquals("_C111_ <> :b0", predicate.toSql());
		assertEquals("1", parameters.get("b0"));
	}
	
	@Test
	public void testStringComparisonPredicate() throws ParseException {
		Predicate predicate = SqlElementUntils.createPredicate("foo <> 'aaa'");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnNameToModelMap);
		assertEquals("_C111_ <> :b0", predicate.toSql());
		assertEquals("aaa", parameters.get("b0"));
	}

	@Test
	public void testStringComparisonBooleanPredicate() throws ParseException {
		Predicate predicate = SqlElementUntils.createPredicate("foo = true");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnNameToModelMap);
		assertEquals("_C111_ = TRUE", predicate.toSql());
		assertEquals(0, parameters.size());
	}

	@Test
	public void testComparisonPredicateDateNumber() throws ParseException {
		Predicate predicate = SqlElementUntils.createPredicate("datetype <> 1");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnNameToModelMap);
		assertEquals("_C666_ <> :b0", predicate.toSql());
		assertEquals(new Long(1), parameters.get("b0"));
	}

	@Test
	public void testComparisonPredicateDateString() throws ParseException {
		Predicate predicate = SqlElementUntils.createPredicate("datetype <> '2011-11-11'");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnNameToModelMap);
		assertEquals("_C666_ <> :b0", predicate.toSql());
		assertEquals(Long.parseLong(DATE1TIME), parameters.get("b0"));
	}

	@Test
	public void testComparisonPredicateDateParsing() throws ParseException {
		for (String date : new String[] { DATE1, "2011-11-11", "2011-11-11 0:00", "2011-11-11 0:00:00", "2011-11-11 0:00:00.0",
				"2011-11-11 0:00:00.00", "2011-11-11 0:00:00.000" }) {
			HashMap<String, Object> parameters = new HashMap<String, Object>();

			Predicate predicate =  SqlElementUntils.createPredicate("datetype <> '" + date + "'");
			HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
			SQLTranslatorUtils.translate(hasPredicate, parameters, columnNameToModelMap);
			assertEquals("_C666_ <> :b0", predicate.toSql());
			assertEquals(Long.parseLong(DATE1TIME), parameters.get("b0"));
		}
		for (String date : new String[] { "2001-01-01", "2001-01-01", "2001-1-1", "2001-1-01", "2001-01-1" }) {
			HashMap<String, Object> parameters = new HashMap<String, Object>();

			Predicate predicate =  SqlElementUntils.createPredicate("datetype <> '" + date + "'");
			HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
			SQLTranslatorUtils.translate(hasPredicate, parameters, columnNameToModelMap);
			assertEquals("_C666_ <> :b0", predicate.toSql());
			assertEquals(Long.parseLong("978307200000"), parameters.get("b0"));
		}
		for (String date : new String[] { "2011-11-11 01:01:01.001", "2011-11-11 1:01:1.001", "2011-11-11 1:1:1.001" }) {
			HashMap<String, Object> parameters = new HashMap<String, Object>();

			Predicate predicate =  SqlElementUntils.createPredicate("datetype <> '" + date + "'");
			HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
			SQLTranslatorUtils.translate(hasPredicate, parameters, columnNameToModelMap);
			assertEquals("_C666_ <> :b0", predicate.toSql());
			assertEquals(Long.parseLong("1320973261001"), parameters.get("b0"));
		}
	}

	@Test
	public void testInPredicateOne() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo in(1)");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnNameToModelMap);
		assertEquals("_C111_ IN ( :b0 )", predicate.toSql());
		assertEquals("1", parameters.get("b0"));
	}
	
	@Test
	public void testInPredicateMore() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo in(1,2,3)");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnNameToModelMap);
		assertEquals("_C111_ IN ( :b0, :b1, :b2 )", predicate.toSql());
		assertEquals("1", parameters.get("b0"));
		assertEquals("2", parameters.get("b1"));
		assertEquals("3", parameters.get("b2"));
	}
	
	@Test
	public void testInPredicateDate() throws ParseException {
		Predicate predicate = SqlElementUntils.createPredicate("datetype in('" + DATE1 + "','" + DATE2 + "')");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnNameToModelMap);
		assertEquals("_C666_ IN ( :b0, :b1 )", predicate.toSql());
		assertEquals(Long.parseLong(DATE1TIME), parameters.get("b0"));
		assertEquals(Long.parseLong(DATE2TIME), parameters.get("b1"));
	}

	@Test
	public void testBetweenPredicate() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo between 1 and 2");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnNameToModelMap);
		assertEquals("_C111_ BETWEEN :b0 AND :b1", predicate.toSql());
		assertEquals("1", parameters.get("b0"));
		assertEquals("2", parameters.get("b1"));
	}
	
	@Test
	public void testBetweenPredicateDate() throws ParseException {
		Predicate predicate = SqlElementUntils.createPredicate("datetype between '" + DATE1 + "' and '" + DATE2 + "'");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnNameToModelMap);
		assertEquals("_C666_ BETWEEN :b0 AND :b1", predicate.toSql());
		assertEquals(Long.parseLong(DATE1TIME), parameters.get("b0"));
		assertEquals(Long.parseLong(DATE2TIME), parameters.get("b1"));
	}

	@Test
	public void testBetweenPredicateNot() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo not between 1 and 2");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnNameToModelMap);
		assertEquals("_C111_ NOT BETWEEN :b0 AND :b1", predicate.toSql());
		assertEquals("1", parameters.get("b0"));
		assertEquals("2", parameters.get("b1"));
	}
	
	@Test
	public void testLikePredicate() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo like 'bar%'");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnNameToModelMap);
		assertEquals("_C111_ LIKE :b0", predicate.toSql());
		assertEquals("bar%",parameters.get("b0"));
	}
	
	@Test
	public void testLikePredicateEscape() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo like 'bar|_' escape '|'");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnNameToModelMap);
		assertEquals("_C111_ LIKE :b0 ESCAPE :b1", predicate.toSql());
		assertEquals("bar|_",parameters.get("b0"));
		assertEquals("|",parameters.get("b1"));
	}
	
	@Test
	public void testLikePredicateNot() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo not like 'bar%'");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnNameToModelMap);
		assertEquals("_C111_ NOT LIKE :b0", predicate.toSql());
		assertEquals("bar%",parameters.get("b0"));
	}
	
	@Test
	public void testNullPredicate() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo is null");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnNameToModelMap);
		assertEquals("_C111_ IS NULL", predicate.toSql());
	}
	
	@Test
	public void testNullPredicateNot() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo is not null");
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		HasPredicate hasPredicate = predicate.getFirstElementOfType(HasPredicate.class);
		SQLTranslatorUtils.translate(hasPredicate, parameters, columnNameToModelMap);
		assertEquals("_C111_ IS NOT NULL", predicate.toSql());
	}
	
	@Test
	public void testWhereSimple() throws ParseException{
		SqlQuery translator = new SqlQueryBuilder("select * from syn123 where foo = 1", tableSchema).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 WHERE _C111_ = :b0",
				translator.getOutputSQL());
		// The value should be in the parameters map.
		assertEquals("1", translator.getParameters().get("b0"));
	}
	
	@Test
	public void testWhereDoubleFunction() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder("select * from syn123 where isNaN(doubletype) or isInfinity(doubletype)", tableSchema).build();
		// The value should be bound in the SQL
		assertEquals(
				"SELECT "
						+ STAR_COLUMNS
						+ ", ROW_ID, ROW_VERSION FROM T123 WHERE"
						+ " ( _DBL_C777_ IS NOT NULL AND _DBL_C777_ = 'NaN' ) "
						+ "OR ( _DBL_C777_ IS NOT NULL AND _DBL_C777_ IN ( '-Infinity', 'Infinity' ) )",
				translator.getOutputSQL());
	}
	
	@Test
	public void testWhereDouble() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder("select foo from syn123 where doubletype between 1.0 and 2.0", tableSchema).build();
		// The value should be bound in the SQL
		assertEquals(
				"SELECT _C111_, ROW_ID, ROW_VERSION FROM T123 WHERE _C777_ BETWEEN :b0 AND :b1",
				translator.getOutputSQL());
		assertEquals(new Double(1.0), translator.getParameters().get("b0"));
		assertEquals(new Double(2.0), translator.getParameters().get("b1"));
	}

	@Test
	public void testWhereOr() throws ParseException{
		SqlQuery translator = new SqlQueryBuilder("select * from syn123 where foo = 1 or bar = 2", tableSchema).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 WHERE _C111_ = :b0 OR _C333_ = :b1",
				translator.getOutputSQL());
		// The value should be in the parameters map.
		assertEquals("1", translator.getParameters().get("b0"));
		assertEquals("2", translator.getParameters().get("b1"));
	}
	
	
	@Test
	public void testWhereAnd() throws ParseException{
		SqlQuery translator = new SqlQueryBuilder("select * from syn123 where foo = 1 and bar = 2", tableSchema).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 WHERE _C111_ = :b0 AND _C333_ = :b1",
				translator.getOutputSQL());
		// The value should be in the parameters map.
		assertEquals("1", translator.getParameters().get("b0"));
		assertEquals("2", translator.getParameters().get("b1"));
	}
	
	@Test
	public void testWhereNested() throws ParseException{
		SqlQuery translator = new SqlQueryBuilder("select * from syn123 where (foo = 1 and bar = 2) or foo_bar = 3", tableSchema).build();
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
		SqlQuery translator = new SqlQueryBuilder("select * from syn123 group by foo", tableSchema).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + " FROM T123 GROUP BY _C111_",
				translator.getOutputSQL());
	}
	
	@Test
	public void testGroupByMultiple() throws ParseException{
		SqlQuery translator = new SqlQueryBuilder("select * from syn123 group by foo, bar", tableSchema).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + " FROM T123 GROUP BY _C111_, _C333_",
				translator.getOutputSQL());
	}
	
	@Test
	public void testOrderByOneNoSpec() throws ParseException{
		SqlQuery translator = new SqlQueryBuilder("select * from syn123 order by foo", tableSchema).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 ORDER BY _C111_",
				translator.getOutputSQL());
	}
	
	@Test
	public void testOrderByOneWithSpec() throws ParseException{
		SqlQuery translator = new SqlQueryBuilder("select * from syn123 order by foo desc", tableSchema).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 ORDER BY _C111_ DESC",
				translator.getOutputSQL());
	}
	
	@Test
	public void testOrderByDouble() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder("select foo from syn123 order by doubletype desc", tableSchema).build();
		// The value should be bound in the SQL
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123 ORDER BY _C777_ DESC", translator.getOutputSQL());
	}
	
	@Test
	public void testOrderByMultipleNoSpec() throws ParseException{
		SqlQuery translator = new SqlQueryBuilder("select * from syn123 order by foo, bar", tableSchema).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 ORDER BY _C111_, _C333_",
				translator.getOutputSQL());
	}
	
	@Test
	public void testOrderByMultipeWithSpec() throws ParseException{
		SqlQuery translator = new SqlQueryBuilder("select * from syn123 order by foo asc, bar desc", tableSchema).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 ORDER BY _C111_ ASC, _C333_ DESC",
				translator.getOutputSQL());
	}
	
	@Test
	public void testLimit() throws ParseException{
		SqlQuery translator = new SqlQueryBuilder("select * from syn123 limit 100", tableSchema).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 LIMIT :b0",
				translator.getOutputSQL());
		assertEquals(100L,translator.getParameters().get("b0"));
	}
	
	@Test
	public void testLimitAndOffset() throws ParseException{
		SqlQuery translator = new SqlQueryBuilder("select * from syn123 limit 100 offset 2", tableSchema).build();
		// The value should be bound in the SQL
		assertEquals("SELECT " + STAR_COLUMNS + ", ROW_ID, ROW_VERSION FROM T123 LIMIT :b0 OFFSET :b1",
				translator.getOutputSQL());
		assertEquals(100L,translator.getParameters().get("b0"));
		assertEquals(2L, translator.getParameters().get("b1"));
	}

	@Test
	public void testAllParts() throws ParseException{
		SqlQuery translator = new SqlQueryBuilder("select foo, bar from syn123 where foo_bar >= 1.89e4 order by bar desc limit 10 offset 0",
				tableSchema).build();
		// The value should be bound in the SQL
		assertEquals("SELECT _C111_, _C333_, ROW_ID, ROW_VERSION FROM T123 WHERE _C444_ >= :b0 ORDER BY _C333_ DESC LIMIT :b1 OFFSET :b2",
				translator.getOutputSQL());
		assertEquals("18900.0", translator.getParameters().get("b0"));
		assertEquals(10L, translator.getParameters().get("b1"));
		assertEquals(0L, translator.getParameters().get("b2"));
	}

	@Test
	public void testAllPartsWithGrouping() throws ParseException {
		SqlQuery translator = new SqlQueryBuilder(
				"select foo, bar from syn123 where foo_bar >= 1.89e4 group by foo order by bar desc limit 10 offset 0",
				tableSchema).build();
		// The value should be bound in the SQL
		assertEquals(
				"SELECT _C111_, _C333_ FROM T123 WHERE _C444_ >= :b0 GROUP BY _C111_ ORDER BY _C333_ DESC LIMIT :b1 OFFSET :b2",
				translator.getOutputSQL());
		assertEquals("18900.0",translator.getParameters().get("b0"));
		assertEquals(10L,translator.getParameters().get("b1"));
		assertEquals(0L,translator.getParameters().get("b2"));
	}

	@Test
	public void testTypeSetFunctionStrings() throws Exception {
		SqlQuery translator = new SqlQueryBuilder("select count(*), min(foo), max(foo), count(foo) from syn123", tableSchema).build();
		assertEquals("SELECT COUNT(*), MIN(_C111_), MAX(_C111_), COUNT(_C111_) FROM T123", translator.getOutputSQL());
		assertEquals(TableModelUtils.createSelectColumn("COUNT(*)", ColumnType.INTEGER, null), translator.getSelectColumns()
				.get(0));
		assertEquals(TableModelUtils.createSelectColumn("MIN(foo)", ColumnType.STRING, null), translator.getSelectColumns()
				.get(1));
		assertEquals(TableModelUtils.createSelectColumn("MAX(foo)", ColumnType.STRING, null), translator.getSelectColumns()
				.get(2));
		assertEquals(TableModelUtils.createSelectColumn("COUNT(foo)", ColumnType.INTEGER, null), translator.getSelectColumns()
				.get(3));
	}

	@Test
	public void testTypeSetFunctionIntegers() throws Exception {
		SqlQuery translator = new SqlQueryBuilder("select min(inttype), max(inttype), sum(inttype), avg(inttype), count(inttype) from syn123",
				tableSchema).build();
		assertEquals("SELECT MIN(_C888_), MAX(_C888_), SUM(_C888_), AVG(_C888_), COUNT(_C888_) FROM T123", translator.getOutputSQL());
		assertEquals(TableModelUtils.createSelectColumn("MIN(inttype)", ColumnType.INTEGER, null), translator.getSelectColumns()
				.get(0));
		assertEquals(TableModelUtils.createSelectColumn("MAX(inttype)", ColumnType.INTEGER, null), translator.getSelectColumns()
				.get(1));
		assertEquals(TableModelUtils.createSelectColumn("SUM(inttype)", ColumnType.INTEGER, null), translator.getSelectColumns()
				.get(2));
		assertEquals(TableModelUtils.createSelectColumn("AVG(inttype)", ColumnType.DOUBLE, null), translator.getSelectColumns()
				.get(3));
		assertEquals(TableModelUtils.createSelectColumn("COUNT(inttype)", ColumnType.INTEGER, null), translator.getSelectColumns()
				.get(4));
	}

	@Test
	public void testTypeSetFunctionDoubles() throws Exception {
		SqlQuery translator = new SqlQueryBuilder(
				"select min(doubletype), max(doubletype), sum(doubletype), avg(doubletype), count(doubletype) from syn123", tableSchema).build();
		assertEquals("SELECT MIN(_C777_), MAX(_C777_), SUM(_C777_), AVG(_C777_), COUNT(_C777_) FROM T123", translator.getOutputSQL());
		assertEquals(TableModelUtils.createSelectColumn("MIN(doubletype)", ColumnType.DOUBLE, null), translator.getSelectColumns()
				.get(0));
		assertEquals(TableModelUtils.createSelectColumn("MAX(doubletype)", ColumnType.DOUBLE, null), translator.getSelectColumns()
				.get(1));
		assertEquals(TableModelUtils.createSelectColumn("SUM(doubletype)", ColumnType.DOUBLE, null), translator.getSelectColumns()
				.get(2));
		assertEquals(TableModelUtils.createSelectColumn("AVG(doubletype)", ColumnType.DOUBLE, null), translator.getSelectColumns()
				.get(3));
		assertEquals(TableModelUtils.createSelectColumn("COUNT(doubletype)", ColumnType.INTEGER, null), translator.getSelectColumns()
				.get(4));
	}
	
	/**
	 * @see <a href="https://sagebionetworks.jira.com/browse/PLFM-3864">3864</a>
	 * @throws Exception
	 */
	@Test
	public void testPLFM_3864() throws Exception{
		SqlQuery translator = new SqlQueryBuilder(
				"select doubletype as f1 from syn123", tableSchema).build();
		assertEquals("SELECT"
				+ " CASE WHEN _DBL_C777_ IS NULL THEN _C777_ ELSE _DBL_C777_ END AS f1"
				+ ", ROW_ID, ROW_VERSION"
				+ " FROM T123", translator.getOutputSQL());
	}
	
	/**
	 * @see <a href="https://sagebionetworks.jira.com/browse/PLFM-3864">3864</a>
	 * @throws Exception
	 */
	@Test
	public void testPLFM_3864withOrderBy() throws Exception{
		SqlQuery translator = new SqlQueryBuilder(
				"select doubletype as f1 from syn123 order by f1", tableSchema).build();
		assertEquals("SELECT"
				+ " CASE WHEN _DBL_C777_ IS NULL THEN _C777_ ELSE _DBL_C777_ END AS f1"
				+ ", ROW_ID, ROW_VERSION"
				+ " FROM T123 ORDER BY f1", translator.getOutputSQL());
	}
	
	/**
	 * @see <a href="https://sagebionetworks.jira.com/browse/PLFM-3865">3865</a>
	 * @throws Exception
	 */
	@Test
	public void testPLFM_3865() throws Exception{
		SqlQuery translator = new SqlQueryBuilder(
				"select max(doubletype), min(doubletype) from syn123 order by min(doubletype)", tableSchema).build();
		assertEquals("SELECT"
				+ " MAX(_C777_)"
				+ ", MIN(_C777_) "
				+ "FROM T123 "
				+ "ORDER BY MIN(_C777_)", translator.getOutputSQL());
	}
	
	/**
	 * @see <a href="https://sagebionetworks.jira.com/browse/PLFM-3865">3865</a>
	 * @see <a href="https://sagebionetworks.jira.com/browse/PLFM-3864">3864</a>
	 * @throws Exception
	 */
	@Test
	public void testPLFM_3865andPLFM_3864() throws Exception{
		SqlQuery translator = new SqlQueryBuilder(
				"select max(doubletype) as f1, min(doubletype) as f2 from syn123 order by f2", tableSchema).build();
		assertEquals("SELECT"
				+ " MAX(_C777_) AS f1"
				+ ", MIN(_C777_) AS f2 "
				+ "FROM T123 "
				+ "ORDER BY f2", translator.getOutputSQL());
	}
	
	/**
	 * We should be throwing 'column a not found' for this case.
	 * @see <a href="https://sagebionetworks.jira.com/browse/PLFM-3866">3866</a>
	 * @throws Exception
	 */
	@Test
	public void testPLFM_3866() throws ParseException{
		SqlQuery translator = new SqlQueryBuilder("select foo from syn123 where foo in (\"a\")", tableSchema).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123 WHERE _C111_ IN ( `a` )", translator.getOutputSQL());
	}
	
	/**
	 * We should be throwing 'column a not found' for this case.
	 * @see <a href="https://sagebionetworks.jira.com/browse/PLFM-3867">3867</a>
	 * @throws Exception
	 */
	@Test
	public void testPLFM_3867() throws ParseException{
		SqlQuery translator = new SqlQueryBuilder("select foo from syn123 where foo = \"a\"", tableSchema).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123 WHERE _C111_ = `a`", translator.getOutputSQL());
	}
	
	@Test
	public void testTranslateNotIsNaN() throws ParseException{
		SqlQuery translator = new SqlQueryBuilder("select foo from syn123 where not isNaN(doubletype)", tableSchema).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123 WHERE NOT ( _DBL_C777_ IS NOT NULL AND _DBL_C777_ = 'NaN' )", translator.getOutputSQL());
	}
	
	/**
	 * @see <a href="https://sagebionetworks.jira.com/browse/PLFM-3869">3869</a>
	 * @throws Exception
	 */
	@Test
	public void testPLFM_3869() throws ParseException{
		tableSchema = Lists.newArrayList(TableModelTestUtils.createColumn(123L, "aDouble", ColumnType.DOUBLE));
		SqlQuery translator = new SqlQueryBuilder("select * from syn123 where not isNaN(aDouble)", tableSchema).build();
		assertEquals("SELECT "
				+ "CASE WHEN _DBL_C123_ IS NULL THEN _C123_ ELSE _DBL_C123_ END,"
				+ " ROW_ID, ROW_VERSION"
				+ " FROM T123 WHERE NOT ( _DBL_C123_ IS NOT NULL AND _DBL_C123_ = 'NaN' )", translator.getOutputSQL());
	}
	
	/**
	 * The casue of PLFM-3870 was double columns functions were translated as
	 * SUM(CASE WHEN _DBL_C777_ IS NULL THEN _C777_ ELSE _DBL_C777_ END)
	 * Therefore, the string 'NaN' was passed to the function and treated as '1'.
	 * To fix the issue we simply use the origial double column for aggregate function, for example
	 * SUM(_C777_)
	 * 
	 * @see <a href="https://sagebionetworks.jira.com/browse/PLFM-3870">3870</a>
	 * 
	 * @throws ParseException
	 */
	@Test
	public void testPLFM_3870() throws ParseException{
		SqlQuery translator = new SqlQueryBuilder("select sum(doubletype) from syn123", tableSchema).build();
		assertEquals("SELECT SUM(_C777_) FROM T123", translator.getOutputSQL());
	}
	
	@Ignore // not sure if we are going to suppor this yet.
	@Test
	public void testTranslateRightHandSideNaN() throws ParseException{
		SqlQuery translator = new SqlQueryBuilder("select foo from syn123 where aDouble <> 'NaN'", tableSchema).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123 WHERE (_DBL_C123_ IS NULL OR _DBL_C123_ <> 'NaN')", translator.getOutputSQL());
	}
	
	@Test
	public void testMaxRowSizeBytesSelectStar() throws ParseException{
		// the size will include the size of the schema
		int maxSizeSchema = TableModelUtils.calculateMaxRowSize(tableSchema);
		SqlQuery translator = new SqlQueryBuilder("select * from syn123", tableSchema).build();
		assertEquals(maxSizeSchema, translator.getMaxRowSizeBytes());
	}
	
	@Test
	public void testMaxRowSizeBytesCountStar() throws ParseException{
		// the size is the size of an integer
		int maxSizeSchema = TableModelUtils.calculateMaxSizeForType(ColumnType.INTEGER, null);
		SqlQuery translator = new SqlQueryBuilder("select count(*) from syn123", tableSchema).build();
		assertEquals(maxSizeSchema, translator.getMaxRowSizeBytes());
	}
	
	@Test
	public void testMaxRowsPerPage() throws ParseException{
		int maxSizeSchema = TableModelUtils.calculateMaxRowSize(tableSchema);
		Long maxBytesPerPage = maxSizeSchema*2L;
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();
		SqlQuery translator = new SqlQueryBuilder(model, tableSchema, null, null, maxBytesPerPage).build();
		assertEquals(maxSizeSchema, translator.getMaxRowSizeBytes());
		assertEquals(new Long(2L), translator.getMaxRowsPerPage());
	}
	
	@Test
	public void testMaxRowsPerPageMaxBytesSmall() throws ParseException{
		int maxSizeSchema = TableModelUtils.calculateMaxRowSize(tableSchema);
		Long maxBytesPerPage = 3L;
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();
		SqlQuery translator = new SqlQueryBuilder(model, tableSchema, null, null, maxBytesPerPage).build();
		assertEquals(maxSizeSchema, translator.getMaxRowSizeBytes());
		assertEquals(new Long(1L), translator.getMaxRowsPerPage());
	}
	
	@Test
	public void testMaxRowsPerPageNullMaxBytesPerPage() throws ParseException{
		int maxSizeSchema = TableModelUtils.calculateMaxRowSize(tableSchema);
		Long maxBytesPerPage = null;
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();
		SqlQuery translator = new SqlQueryBuilder(model, tableSchema, null, null, maxBytesPerPage).build();
		assertEquals(maxSizeSchema, translator.getMaxRowSizeBytes());
		assertEquals(null, translator.getMaxRowsPerPage());
	}
	
	@Test
	public void testOverrideLimitAndOffset() throws ParseException{
		Long overideOffset = 1L;
		Long overideLimit = 10L;
		Long maxBytesPerPage = 10000L;
		QuerySpecification model = new TableQueryParser("select foo from syn123").querySpecification();
		SqlQuery translator = new SqlQueryBuilder(model, tableSchema, overideOffset, overideLimit, maxBytesPerPage).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123 LIMIT :b0 OFFSET :b1",translator.getOutputSQL());
		assertEquals(overideLimit, translator.getParameters().get("b0"));
		assertEquals(overideOffset, translator.getParameters().get("b1"));
		// the original model should remain unchanged.
		assertEquals("SELECT foo FROM syn123",translator.getModel().toSql());
	}
	
	@Test
	public void testOverrideLimitAndOffsetNullWithMaxBytesPerPage() throws ParseException{
		Long overideOffset = null;
		Long overideLimit = null;
		Long maxBytesPerPage = 1L;
		QuerySpecification model = new TableQueryParser("select foo from syn123").querySpecification();
		SqlQuery translator = new SqlQueryBuilder(model, tableSchema, overideOffset, overideLimit, maxBytesPerPage).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123 LIMIT :b0 OFFSET :b1",translator.getOutputSQL());
		assertEquals(new Long(1), translator.getParameters().get("b0"));
		assertEquals(new Long(0), translator.getParameters().get("b1"));
	}
	
	@Test
	public void testOverrideNull() throws ParseException{
		Long overideOffset = null;
		Long overideLimit = null;
		Long maxBytesPerPage = null;
		QuerySpecification model = new TableQueryParser("select foo from syn123").querySpecification();
		SqlQuery translator = new SqlQueryBuilder(model, tableSchema, overideOffset, overideLimit, maxBytesPerPage).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123",translator.getOutputSQL());
	}
	
	@Test
	public void testCopyConstructor() throws ParseException{
		Long overideOffset = 1L;
		Long overideLimit = 101L;
		Long maxBytesPerPage = 501L;
		QuerySpecification originalModel = new TableQueryParser("select foo from syn123").querySpecification();
		SortItem sortItem = new SortItem();
		sortItem.setColumn("bar");
		sortItem.setDirection(SortDirection.DESC);
		List<SortItem> sortList = Lists.newArrayList(sortItem);
		SqlQuery original = new SqlQueryBuilder(originalModel)
		.tableSchema(tableSchema)
		.overrideOffset(overideOffset)
		.overrideLimit(overideLimit)
		.maxBytesPerPage(maxBytesPerPage)
		.tableType(EntityType.entityview)
		.includeEntityEtag(true)
		.sortList(sortList)
		.build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION, ROW_ETAG FROM T123 ORDER BY _C333_ DESC LIMIT :b0 OFFSET :b1", original.getOutputSQL());

		QuerySpecification newModel = new TableQueryParser(original.getModel().toSql()).querySpecification();
		SelectList newSelectList = new TableQueryParser("foo as bar").selectList();
		newModel.replaceSelectList(newSelectList);
		
		SqlQuery copy = new SqlQueryBuilder(newModel, original).build();
		assertEquals("SELECT _C111_ AS bar, ROW_ID, ROW_VERSION, ROW_ETAG FROM T123 ORDER BY _C333_ DESC LIMIT :b0 OFFSET :b1", copy.getOutputSQL());
		assertEquals(3L, copy.getParameters().get("b0"));
		assertEquals(overideOffset, copy.getParameters().get("b1"));
		assertEquals(tableSchema, copy.getTableSchema());
		assertEquals(maxBytesPerPage, copy.maxBytesPerPage);
		assertEquals(overideOffset, copy.overrideOffset);
		assertEquals(overideLimit, copy.overrideLimit);
		assertEquals(EntityType.entityview, copy.tableType);
		assertEquals(true, copy.includeEntityEtag);
	}
	
	@Test
	public void testPLFM_4161() throws ParseException{
		String sql = "select * from syn123";
		SqlQuery query = new SqlQueryBuilder(sql, schema).build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123", query.getOutputSQL());
	}
	
	@Test
	public void testDeafultConsistent() throws ParseException{
		// call under test
		SqlQuery query = new SqlQueryBuilder(sql)
		.tableSchema(schema)
		.isConsistent(null)
		.build();
		// should default to table.
		assertTrue(query.isConsistent());
	}
	
	@Test
	public void testNotConsistent() throws ParseException{
		// call under test
		SqlQuery query = new SqlQueryBuilder(sql)
		.tableSchema(schema)
		.isConsistent(false)
		.build();
		assertFalse(query.isConsistent());
	}
	
	@Test
	public void testDeafultType() throws ParseException{
		// call under test
		SqlQuery query = new SqlQueryBuilder(sql)
		.tableSchema(schema)
		.tableType(null)
		.build();
		// should default to table.
		assertEquals(EntityType.table, query.getTableType());
	}
	
	@Test
	public void testTableDefaultEtag() throws ParseException{
		SqlQuery query = new SqlQueryBuilder(sql)
		.tableSchema(schema)
		.tableType(EntityType.table)
		.includeEntityEtag(null)
		.build();
		// should default to false
		assertFalse(query.includeEntityEtag());
	}
	
	@Test
	public void testViewDefaultEtag() throws ParseException{
		SqlQuery query = new SqlQueryBuilder(sql)
		.tableSchema(schema)
		.tableType(EntityType.entityview)
		.includeEntityEtag(null)
		.build();
		// should default to false
		assertFalse(query.includeEntityEtag());
	}
	
	@Test
	public void testSelectViewWithEtag() throws ParseException{
		SqlQuery query = new SqlQueryBuilder(sql)
		.tableSchema(schema)
		.tableType(EntityType.entityview)
		.includeEntityEtag(true)
		.build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION, ROW_ETAG FROM T123", query.getOutputSQL());
		assertTrue(query.includeEntityEtag());
	}
	
	@Test
	public void testSelectViewWithoutEtag() throws ParseException{
		SqlQuery query = new SqlQueryBuilder(sql)
		.tableSchema(schema)
		.tableType(EntityType.entityview)
		.includeEntityEtag(null)
		.build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123", query.getOutputSQL());
		assertFalse(query.includeEntityEtag());
	}
	
	@Test
	public void testSelectTableWithEtag() throws ParseException{
		SqlQuery query = new SqlQueryBuilder(sql)
		.tableSchema(schema)
		.tableType(EntityType.table)
		.includeEntityEtag(true)
		.build();
		assertEquals("SELECT _C111_, ROW_ID, ROW_VERSION FROM T123", query.getOutputSQL());
		assertFalse(query.includeEntityEtag());
	}
	
	@Test
	public void testSelectViewWithEtagAggregate() throws ParseException{
		sql = "select count(*) from syn123";
		SqlQuery query = new SqlQueryBuilder(sql)
		.tableSchema(schema)
		.tableType(EntityType.entityview)
		.includeEntityEtag(true)
		.build();
		assertEquals("SELECT COUNT(*) FROM T123", query.getOutputSQL());
	}
	
	/**
	 * This is a test for PLFM-4736.
	 * @throws ParseException
	 */
	@Test
	public void testAliasGroupByOrderBy() throws ParseException {
		sql = "select \"foo\" as \"f\", sum(inttype) as \"i` sum\" from syn123 group by \"f\" order by \"i` sum\" DESC";
		SqlQuery query = new SqlQueryBuilder(sql)
		.tableSchema(tableSchema)
		.tableType(EntityType.table)
		.build();
		assertEquals("SELECT _C111_ AS `f`, SUM(_C888_) AS `i`` sum` FROM T123 GROUP BY `f` ORDER BY `i`` sum` DESC", query.getOutputSQL());
	}

}
