package org.sagebionetworks.table.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class SQLQueryTest {
	
	Map<String, Long> columnNameToIdMap;
	
	@Before
	public void before(){
		columnNameToIdMap = new HashMap<String, Long>();
		columnNameToIdMap.put("foo", 111L);
		columnNameToIdMap.put("has space", 222L);
		columnNameToIdMap.put("bar", 333L);
		columnNameToIdMap.put("foobar", 444L);
	}
	
	@Test
	public void testTranslateColumnReferenceNoRightHandSide() throws ParseException{
		ColumnReference columnReference = SqlElementUntils.createColumnReference("foo");
		StringBuilder builder = new StringBuilder();
		SQLTranslatorUtils.translate(columnReference, builder, columnNameToIdMap);
		assertEquals("C111", builder.toString());
	}
	
	@Test
	public void testTranslateColumnReferenceIgnoreCase() throws ParseException{
		ColumnReference columnReference = SqlElementUntils.createColumnReference("Foo");
		StringBuilder builder = new StringBuilder();
		SQLTranslatorUtils.translate(columnReference, builder, columnNameToIdMap);
		assertEquals("C111", builder.toString());
	}
	
	@Test 
	public void testTranslateColumnReferenceUnknownColumn() throws ParseException{
		try{
			ColumnReference columnReference = SqlElementUntils.createColumnReference("fake");
			StringBuilder builder = new StringBuilder();
			SQLTranslatorUtils.translate(columnReference, builder, columnNameToIdMap);
			fail("this column does not exist so it should have failed.");
		}catch(IllegalArgumentException e){
			assertTrue(e.getMessage().contains("fake"));
		}
	}
	
	@Test
	public void testTranslateColumnReferenceWithRightHandSide() throws ParseException{
		ColumnReference columnReference = SqlElementUntils.createColumnReference("foo.bar");
		StringBuilder builder = new StringBuilder();
		SQLTranslatorUtils.translate(columnReference, builder, columnNameToIdMap);
		assertEquals("C111_bar", builder.toString());
	}
	
	@Test
	public void testTranslateColumnReferenceWithQuotes() throws ParseException{
		ColumnReference columnReference = SqlElementUntils.createColumnReference("\"has space\"");
		StringBuilder builder = new StringBuilder();
		SQLTranslatorUtils.translate(columnReference, builder, columnNameToIdMap);
		assertEquals("C222", builder.toString());
	}
	
	@Test
	public void testTranslateColumnReferenceRowId() throws ParseException{
		ColumnReference columnReference = SqlElementUntils.createColumnReference("ROW_ID");
		StringBuilder builder = new StringBuilder();
		SQLTranslatorUtils.translate(columnReference, builder, columnNameToIdMap);
		assertEquals("ROW_ID", builder.toString());
	}
	
	@Test
	public void testTranslateColumnReferenceRowVersionIgnoreCase() throws ParseException{
		ColumnReference columnReference = SqlElementUntils.createColumnReference("row_version");
		StringBuilder builder = new StringBuilder();
		SQLTranslatorUtils.translate(columnReference, builder, columnNameToIdMap);
		assertEquals("ROW_VERSION", builder.toString());
	}
	
	@Test
	public void testSelectStar() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123", columnNameToIdMap);
		assertEquals("SELECT * FROM T123", translator.getOutputSQL());
		assertFalse(translator.isAggregatedResult());
		assertNotNull(translator.getSelectColumnIds());
		assertTrue(translator.getSelectColumnIds().containsAll(columnNameToIdMap.values()));
	}
	@Test
	public void testSelectSingColumns() throws ParseException{
		SqlQuery translator = new SqlQuery("select foo from syn123", columnNameToIdMap);
		assertEquals("SELECT C111, ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertFalse(translator.isAggregatedResult());
		List<Long> expectedSelect = Arrays.asList(111L);
		assertEquals(expectedSelect, translator.getSelectColumnIds());
	}
	
	@Test
	public void testSelectMultipleColumns() throws ParseException{
		SqlQuery translator = new SqlQuery("select foo, bar from syn123", columnNameToIdMap);
		assertEquals("SELECT C111, C333, ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertFalse(translator.isAggregatedResult());
		List<Long> expectedSelect = Arrays.asList(111L, 333L);
		assertEquals(expectedSelect, translator.getSelectColumnIds());
	}
	
	@Test
	public void testSelectDistinct() throws ParseException{
		SqlQuery translator = new SqlQuery("select distinct foo, bar from syn123", columnNameToIdMap);
		assertEquals("SELECT DISTINCT C111, C333, ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
		assertFalse(translator.isAggregatedResult());
		List<Long> expectedSelect = Arrays.asList(111L, 333L);
		assertEquals(expectedSelect, translator.getSelectColumnIds());
	}
	
	@Test
	public void testSelectCountStar() throws ParseException{
		SqlQuery translator = new SqlQuery("select count(*) from syn123", columnNameToIdMap);
		assertEquals("SELECT COUNT(*) FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertTrue(translator.getSelectColumnIds().isEmpty());
	}
	
	@Test
	public void testSelectAggregate() throws ParseException{
		SqlQuery translator = new SqlQuery("select avg(foo) from syn123", columnNameToIdMap);
		assertEquals("SELECT AVG(C111) FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertTrue(translator.getSelectColumnIds().isEmpty());
	}
	
	@Test
	public void testSelectAggregateMultiple() throws ParseException{
		SqlQuery translator = new SqlQuery("select avg(foo), max(bar) from syn123", columnNameToIdMap);
		assertEquals("SELECT AVG(C111), MAX(C333) FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertTrue(translator.getSelectColumnIds().isEmpty());
	}
	
	@Test
	public void testSelectDistinctAggregate() throws ParseException{
		SqlQuery translator = new SqlQuery("select count(distinct foo) from syn123", columnNameToIdMap);
		assertEquals("SELECT COUNT(DISTINCT C111) FROM T123", translator.getOutputSQL());
		assertTrue(translator.isAggregatedResult());
		assertTrue(translator.getSelectColumnIds().isEmpty());
	}
	
	@Test
	public void testComparisonPredicate() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo <> 1");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translate(predicate, builder, parameters, columnNameToIdMap);
		assertEquals("C111 <> :b0", builder.toString());
		assertEquals("1",parameters.get("b0"));
	}
	
	
	@Test
	public void testInPredicateOne() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo in(1)");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translate(predicate, builder, parameters, columnNameToIdMap);
		assertEquals("C111 IN (:b0)", builder.toString());
		assertEquals("1",parameters.get("b0"));
	}
	
	@Test
	public void testInPredicateMore() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo in(1,2,3)");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translate(predicate, builder, parameters, columnNameToIdMap);
		assertEquals("C111 IN (:b0, :b1, :b2)", builder.toString());
		assertEquals("1",parameters.get("b0"));
		assertEquals("2",parameters.get("b1"));
		assertEquals("3",parameters.get("b2"));
	}
	
	@Test
	public void testBetweenPredicate() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo between 1 and 2");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translate(predicate, builder, parameters, columnNameToIdMap);
		assertEquals("C111 BETWEEN :b0 AND :b1", builder.toString());
		assertEquals("1",parameters.get("b0"));
		assertEquals("2",parameters.get("b1"));
	}
	
	@Test
	public void testBetweenPredicateNot() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo not between 1 and 2");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translate(predicate, builder, parameters, columnNameToIdMap);
		assertEquals("C111 NOT BETWEEN :b0 AND :b1", builder.toString());
		assertEquals("1",parameters.get("b0"));
		assertEquals("2",parameters.get("b1"));
	}
	
	@Test
	public void testLikePredicate() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo like 'bar%'");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translate(predicate, builder, parameters, columnNameToIdMap);
		assertEquals("C111 LIKE :b0", builder.toString());
		assertEquals("bar%",parameters.get("b0"));
	}
	
	@Test
	public void testLikePredicateEscape() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo like 'bar|_' escape '|'");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translate(predicate, builder, parameters, columnNameToIdMap);
		assertEquals("C111 LIKE :b0 ESCAPE :b1", builder.toString());
		assertEquals("bar|_",parameters.get("b0"));
		assertEquals("|",parameters.get("b1"));
	}
	
	@Test
	public void testLikePredicateNot() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo not like 'bar%'");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translate(predicate, builder, parameters, columnNameToIdMap);
		assertEquals("C111 NOT LIKE :b0", builder.toString());
		assertEquals("bar%",parameters.get("b0"));
	}
	
	@Test
	public void testNullPredicate() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo is null");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translate(predicate, builder, parameters, columnNameToIdMap);
		assertEquals("C111 IS NULL", builder.toString());
	}
	
	@Test
	public void testNullPredicateNot() throws ParseException{
		Predicate predicate = SqlElementUntils.createPredicate("foo is not null");
		StringBuilder builder = new StringBuilder();
		HashMap<String, Object> parameters = new HashMap<String, Object>();
		SQLTranslatorUtils.translate(predicate, builder, parameters, columnNameToIdMap);
		assertEquals("C111 IS NOT NULL", builder.toString());
	}
	
	@Test
	public void testWhereSimple() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123 where foo = 1", columnNameToIdMap);
		// The value should be bound in the SQL
		assertEquals("SELECT * FROM T123 WHERE C111 = :b0", translator.getOutputSQL());
		// The value should be in the parameters map.
		assertEquals("1",translator.getParameters().get("b0"));
	}
	
	@Test
	public void testWhereOr() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123 where foo = 1 or bar = 2", columnNameToIdMap);
		// The value should be bound in the SQL
		assertEquals("SELECT * FROM T123 WHERE C111 = :b0 OR C333 = :b1", translator.getOutputSQL());
		// The value should be in the parameters map.
		assertEquals("1",translator.getParameters().get("b0"));
		assertEquals("2",translator.getParameters().get("b1"));
	}
	
	
	@Test
	public void testWhereAnd() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123 where foo = 1 and bar = 2", columnNameToIdMap);
		// The value should be bound in the SQL
		assertEquals("SELECT * FROM T123 WHERE C111 = :b0 AND C333 = :b1", translator.getOutputSQL());
		// The value should be in the parameters map.
		assertEquals("1",translator.getParameters().get("b0"));
		assertEquals("2",translator.getParameters().get("b1"));
	}
	
	@Test
	public void testWhereNested() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123 where (foo = 1 and bar = 2) or foobar = 3", columnNameToIdMap);
		// The value should be bound in the SQL
		assertEquals("SELECT * FROM T123 WHERE (C111 = :b0 AND C333 = :b1) OR C444 = :b2", translator.getOutputSQL());
		// The value should be in the parameters map.
		assertEquals("1",translator.getParameters().get("b0"));
		assertEquals("2",translator.getParameters().get("b1"));
		assertEquals("3",translator.getParameters().get("b2"));
	}
	
	@Test
	public void testGroupByOne() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123 group by foo", columnNameToIdMap);
		// The value should be bound in the SQL
		assertEquals("SELECT * FROM T123 GROUP BY C111", translator.getOutputSQL());
	}
	
	@Test
	public void testGroupByMultiple() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123 group by foo, bar", columnNameToIdMap);
		// The value should be bound in the SQL
		assertEquals("SELECT * FROM T123 GROUP BY C111, C333", translator.getOutputSQL());
	}
	
	@Test
	public void testOrderByOneNoSpec() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123 order by foo", columnNameToIdMap);
		// The value should be bound in the SQL
		assertEquals("SELECT * FROM T123 ORDER BY C111", translator.getOutputSQL());
	}
	
	@Test
	public void testOrderByOneWithSpec() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123 order by foo desc", columnNameToIdMap);
		// The value should be bound in the SQL
		assertEquals("SELECT * FROM T123 ORDER BY C111 DESC", translator.getOutputSQL());
	}
	
	
	@Test
	public void testOrderByMultipleNoSpec() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123 order by foo, bar", columnNameToIdMap);
		// The value should be bound in the SQL
		assertEquals("SELECT * FROM T123 ORDER BY C111, C333", translator.getOutputSQL());
	}
	
	@Test
	public void testOrderByMultipeWithSpec() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123 order by foo asc, bar desc", columnNameToIdMap);
		// The value should be bound in the SQL
		assertEquals("SELECT * FROM T123 ORDER BY C111 ASC, C333 DESC", translator.getOutputSQL());
	}
	
	@Test
	public void testLimit() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123 limit 100", columnNameToIdMap);
		// The value should be bound in the SQL
		assertEquals("SELECT * FROM T123 LIMIT :b0", translator.getOutputSQL());
		assertEquals(100L,translator.getParameters().get("b0"));
	}
	
	@Test
	public void testLimitAndOffset() throws ParseException{
		SqlQuery translator = new SqlQuery("select * from syn123 limit 100 offset 2", columnNameToIdMap);
		// The value should be bound in the SQL
		assertEquals("SELECT * FROM T123 LIMIT :b0 OFFSET :b1", translator.getOutputSQL());
		assertEquals(100L,translator.getParameters().get("b0"));
		assertEquals(2L,translator.getParameters().get("b1"));
	}
	
	@Test
	public void testAllParts() throws ParseException{
		SqlQuery translator = new SqlQuery("select foo, bar from syn123 where foobar >= 1.89e4 group by foo order by bar desc limit 10 offset 0", columnNameToIdMap);
		// The value should be bound in the SQL
		assertEquals("SELECT C111, C333, ROW_ID, ROW_VERSION FROM T123 WHERE C444 >= :b0 GROUP BY C111 ORDER BY C333 DESC LIMIT :b1 OFFSET :b2", translator.getOutputSQL());
		assertEquals("1.89e4",translator.getParameters().get("b0"));
		assertEquals(10L,translator.getParameters().get("b1"));
		assertEquals(0L,translator.getParameters().get("b2"));
	}

}
