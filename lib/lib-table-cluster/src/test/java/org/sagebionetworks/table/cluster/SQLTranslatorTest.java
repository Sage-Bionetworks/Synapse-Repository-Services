package org.sagebionetworks.table.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.table.query.ParseException;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class SQLTranslatorTest {
	
	Map<String, Long> columnNameToIdMap;
	
	@Before
	public void before(){
		columnNameToIdMap = new HashMap<String, Long>();
		columnNameToIdMap.put("foo", 111L);
		columnNameToIdMap.put("has space", 222L);
		columnNameToIdMap.put("bar", 333L);
	}
	
	@Test
	public void testTranslateColumnReferenceNoRightHandSide() throws ParseException{
		ColumnReference columnReference = SqlElementUntils.createColumnReference("foo");
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
	public void testSelectStar() throws ParseException{
		SqlTranslator translator = new SqlTranslator("select * from syn123", columnNameToIdMap);
		assertEquals("SELECT * FROM T123", translator.getOutputSQL());
	}
	@Test
	public void testSelectSingColumns() throws ParseException{
		SqlTranslator translator = new SqlTranslator("select foo from syn123", columnNameToIdMap);
		assertEquals("SELECT C111, ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
	}
	
	@Test
	public void testSelectMultipleColumns() throws ParseException{
		SqlTranslator translator = new SqlTranslator("select foo, bar from syn123", columnNameToIdMap);
		assertEquals("SELECT C111, C333, ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
	}
	
	@Test
	public void testSelectDistinct() throws ParseException{
		SqlTranslator translator = new SqlTranslator("select distinct foo, bar from syn123", columnNameToIdMap);
		assertEquals("SELECT DISTINCT C111, C333, ROW_ID, ROW_VERSION FROM T123", translator.getOutputSQL());
	}
	
	@Test
	public void testSelectCountStar() throws ParseException{
		SqlTranslator translator = new SqlTranslator("select count(*) from syn123", columnNameToIdMap);
		assertEquals("SELECT COUNT(*) FROM T123", translator.getOutputSQL());
	}
	
	@Test
	public void testSelectAggregate() throws ParseException{
		SqlTranslator translator = new SqlTranslator("select avg(foo) from syn123", columnNameToIdMap);
		assertEquals("SELECT AVG(C111) FROM T123", translator.getOutputSQL());
	}
	
	@Test
	public void testSelectAggregateMultiple() throws ParseException{
		SqlTranslator translator = new SqlTranslator("select avg(foo), max(bar) from syn123", columnNameToIdMap);
		assertEquals("SELECT AVG(C111), MAX(C333) FROM T123", translator.getOutputSQL());
	}
	
	@Test
	public void testSelectDistinctAggregate() throws ParseException{
		SqlTranslator translator = new SqlTranslator("select count(distinct foo) from syn123", columnNameToIdMap);
		assertEquals("SELECT COUNT(DISTINCT C111) FROM T123", translator.getOutputSQL());
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
		SqlTranslator translator = new SqlTranslator("select * from syn123 where foo = 1", columnNameToIdMap);
		// The value should be bound in the SQL
		assertEquals("SELECT * FROM T123 WHERE C111 = :b0", translator.getOutputSQL());
		// The value should be in the paremeters map.
		assertEquals("1",translator.getParameters().get("b0"));
	}
	
	@Test
	public void testWhereOr() throws ParseException{
		SqlTranslator translator = new SqlTranslator("select * from syn123 where foo = 1 or bar = 2", columnNameToIdMap);
		// The value should be bound in the SQL
		assertEquals("SELECT * FROM T123 WHERE C111 = :b0 OR C333 = :b1", translator.getOutputSQL());
		// The value should be in the paremeters map.
		assertEquals("1",translator.getParameters().get("b0"));
		assertEquals("2",translator.getParameters().get("b1"));
	}
	
	
	@Test
	public void testWhereAnd() throws ParseException{
		SqlTranslator translator = new SqlTranslator("select * from syn123 where foo = 1 and bar = 2", columnNameToIdMap);
		// The value should be bound in the SQL
		assertEquals("SELECT * FROM T123 WHERE C111 = :b0 AND C333 = :b1", translator.getOutputSQL());
		// The value should be in the paremeters map.
		assertEquals("1",translator.getParameters().get("b0"));
		assertEquals("2",translator.getParameters().get("b1"));
	}
	
}
