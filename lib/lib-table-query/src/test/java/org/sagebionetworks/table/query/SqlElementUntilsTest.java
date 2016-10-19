package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import javax.management.Query;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.table.QueryResult;
import org.sagebionetworks.repo.model.table.SortDirection;
import org.sagebionetworks.repo.model.table.SortItem;
import org.sagebionetworks.table.query.model.BooleanTerm;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SearchCondition;
import org.sagebionetworks.table.query.model.WhereClause;
import org.sagebionetworks.table.query.util.SimpleAggregateQueryException;
import org.sagebionetworks.table.query.util.SqlElementUntils;

import com.google.common.collect.Lists;

public class SqlElementUntilsTest {
	WhereClause whereClause;
	String facetSearchConditionString;
	StringBuilder stringBuilder;
	QuerySpecification simpleModel;
	String columnName;
	
	@Before
	public void setUp() throws ParseException{
		whereClause = new WhereClause(SqlElementUntils.createSearchCondition("water=wet AND sky=blue"));
		facetSearchConditionString = "(tabs > spaces)";
		stringBuilder = new StringBuilder();
		simpleModel = TableQueryParser.parserQuery("select * from syn123 where i like 'trains'");
		columnName = "burrito";
	}
	
	@Test
	public void testConvertToPaginated() throws ParseException {
		QuerySpecification model = TableQueryParser.parserQuery("select foo, bar from syn123 where foo = 1 order by bar");
		QuerySpecification converted = SqlElementUntils.convertToPaginatedQuery(model, 234L, 567L);
		assertNotNull(converted);
		assertEquals("SELECT foo, bar FROM syn123 WHERE foo = 1 ORDER BY bar LIMIT 567 OFFSET 234", converted.toString());
	}

	@Test
	public void testReplacePaginated() throws ParseException {
		QuerySpecification model = TableQueryParser.parserQuery("select foo, bar from syn123 where foo = 1 order by bar limit 1 offset 2");
		QuerySpecification converted = SqlElementUntils.convertToPaginatedQuery(model, 234L, 567L);
		assertNotNull(converted);
		assertEquals("SELECT foo, bar FROM syn123 WHERE foo = 1 ORDER BY bar LIMIT 567 OFFSET 234", converted.toString());
	}

	@Test
	public void testConvertToSorted() throws ParseException {
		QuerySpecification model = TableQueryParser.parserQuery("select foo, bar from syn123 where foo = 1 order by bar limit 1");
		SortItem sort1 = new SortItem();
		sort1.setColumn("foo");
		SortItem sort2 = new SortItem();
		sort2.setColumn("zoo");
		sort2.setDirection(SortDirection.ASC);
		SortItem sort3 = new SortItem();
		sort3.setColumn("zaa");
		sort3.setDirection(SortDirection.DESC);
		QuerySpecification converted = SqlElementUntils.convertToSortedQuery(model, Lists.newArrayList(sort1, sort2, sort3));
		assertNotNull(converted);
		assertEquals("SELECT foo, bar FROM syn123 WHERE foo = 1 ORDER BY foo ASC, zoo ASC, zaa DESC, bar LIMIT 1", converted.toString());
	}

	@Test
	public void testConvertToSortedEscaped() throws ParseException {
		QuerySpecification model = TableQueryParser
				.parserQuery("select \"foo-bar\", bar from syn123 where \"foo-bar\" = 1 order by bar limit 1");
		SortItem sort1 = new SortItem();
		sort1.setColumn("foo-bar");
		SortItem sort2 = new SortItem();
		sort2.setColumn("zoo");
		sort2.setDirection(SortDirection.ASC);
		SortItem sort3 = new SortItem();
		sort3.setColumn("zaa");
		sort3.setDirection(SortDirection.DESC);
		QuerySpecification converted = SqlElementUntils.convertToSortedQuery(model, Lists.newArrayList(sort1, sort2, sort3));
		assertNotNull(converted);
		assertEquals("SELECT \"foo-bar\", bar FROM syn123 WHERE \"foo-bar\" = 1 ORDER BY \"foo-bar\" ASC, zoo ASC, zaa DESC, bar LIMIT 1",
				converted.toString());
	}

	@Test
	public void testReplaceSorted() throws ParseException {
		QuerySpecification model = TableQueryParser.parserQuery("select foo, bar from syn123 where foo = 1 order by bar limit 1");
		SortItem sort1 = new SortItem();
		sort1.setColumn("bar");
		sort1.setDirection(SortDirection.DESC);
		SortItem sort2 = new SortItem();
		sort2.setColumn("foo");
		QuerySpecification converted = SqlElementUntils.convertToSortedQuery(model, Lists.newArrayList(sort1, sort2));
		assertNotNull(converted);
		assertEquals("SELECT foo, bar FROM syn123 WHERE foo = 1 ORDER BY bar DESC, foo ASC LIMIT 1", converted.toString());
	}
	
	@Test
	public void testOverridePaginationQueryWithoutPagingOverridesNull() throws ParseException{
		QuerySpecification model = TableQueryParser.parserQuery("select * from syn123");
		Long limit = null;
		Long offset = null;
		Long maxRowsPerPage = 1000L;
		QuerySpecification converted = SqlElementUntils.overridePagination(model, offset, limit, maxRowsPerPage);
		assertEquals("SELECT * FROM syn123 LIMIT 1000 OFFSET 0", converted.toString());
	}
	
	@Test
	public void testOverridePaginationQueryWithoutPagingOverrideLimitNull() throws ParseException{
		QuerySpecification model = TableQueryParser.parserQuery("select * from syn123");
		Long limit = null;
		Long offset = 12L;
		Long maxRowsPerPage = 1000L;
		QuerySpecification converted = SqlElementUntils.overridePagination(model, offset, limit, maxRowsPerPage);
		assertEquals("SELECT * FROM syn123 LIMIT 1000 OFFSET 12", converted.toString());
	}
	
	@Test
	public void testOverridePaginationQueryWithoutPagingOverrideOffsetNull() throws ParseException{
		QuerySpecification model = TableQueryParser.parserQuery("select * from syn123");
		Long limit = 15L;
		Long offset = null;
		Long maxRowsPerPage = 1000L;
		QuerySpecification converted = SqlElementUntils.overridePagination(model, offset, limit, maxRowsPerPage);
		assertEquals("SELECT * FROM syn123 LIMIT 15 OFFSET 0", converted.toString());
	}
	
	@Test
	public void testOverridePaginationQueryWithLimitOverridesNull() throws ParseException{
		QuerySpecification model = TableQueryParser.parserQuery("select * from syn123 limit 34");
		Long limit = null;
		Long offset = null;
		Long maxRowsPerPage = 1000L;
		QuerySpecification converted = SqlElementUntils.overridePagination(model, offset, limit, maxRowsPerPage);
		assertEquals("SELECT * FROM syn123 LIMIT 34 OFFSET 0", converted.toString());
	}
	
	@Test
	public void testOverridePaginationQueryWithLimitOffsetOverridesNull() throws ParseException{
		QuerySpecification model = TableQueryParser.parserQuery("select * from syn123 limit 34 offset 12");
		Long limit = null;
		Long offset = null;
		Long maxRowsPerPage = 1000L;
		QuerySpecification converted = SqlElementUntils.overridePagination(model, offset, limit, maxRowsPerPage);
		assertEquals("SELECT * FROM syn123 LIMIT 34 OFFSET 12", converted.toString());
	}
	
	@Test
	public void testOverridePaginationQueryWithLimitOffsetOverrideLimitNull() throws ParseException{
		QuerySpecification model = TableQueryParser.parserQuery("select * from syn123 limit 34 offset 12");
		Long limit = null;
		Long offset = 2L;
		Long maxRowsPerPage = 1000L;
		QuerySpecification converted = SqlElementUntils.overridePagination(model, offset, limit, maxRowsPerPage);
		assertEquals("SELECT * FROM syn123 LIMIT 32 OFFSET 14", converted.toString());
	}
	
	@Test
	public void testOverridePaginationQueryWithLimitOffsetOverrideOffsetNull() throws ParseException{
		QuerySpecification model = TableQueryParser.parserQuery("select * from syn123 limit 34 offset 12");
		Long limit = 3L;
		Long offset = null;
		Long maxRowsPerPage = 1000L;
		QuerySpecification converted = SqlElementUntils.overridePagination(model, offset, limit, maxRowsPerPage);
		assertEquals("SELECT * FROM syn123 LIMIT 3 OFFSET 12", converted.toString());
	}
	
	@Test
	public void testOverridePaginationQueryQueryLimitGreaterThanOverrideOffset() throws ParseException{
		QuerySpecification model = TableQueryParser.parserQuery("select * from syn123 limit 100 offset 50");
		Long limit = 25L;
		Long offset = 10L;
		Long maxRowsPerPage = 1000L;
		QuerySpecification converted = SqlElementUntils.overridePagination(model, offset, limit, maxRowsPerPage);
		assertEquals("SELECT * FROM syn123 LIMIT 25 OFFSET 60", converted.toString());
	}
	
	@Test
	public void testOverridePaginationQueryQueryLimitLessThanOverrideOffset() throws ParseException{
		QuerySpecification model = TableQueryParser.parserQuery("select * from syn123 limit 100 offset 50");
		Long limit = 25L;
		Long offset = 101L;
		Long maxRowsPerPage = 1000L;
		QuerySpecification converted = SqlElementUntils.overridePagination(model, offset, limit, maxRowsPerPage);
		assertEquals("SELECT * FROM syn123 LIMIT 0 OFFSET 151", converted.toString());
	}
	
	@Test
	public void testOverridePaginationQueryMaxRowsLessLimit() throws ParseException{
		QuerySpecification model = TableQueryParser.parserQuery("select * from syn123 limit 100");
		Long limit = null;
		Long offset = null;
		Long maxRowsPerPage = 99L;
		QuerySpecification converted = SqlElementUntils.overridePagination(model, offset, limit, maxRowsPerPage);
		assertEquals("SELECT * FROM syn123 LIMIT 99 OFFSET 0", converted.toString());
	}
	
	@Test
	public void testOverridePaginationMaxRowPerPageNull() throws ParseException{
		QuerySpecification model = TableQueryParser.parserQuery("select * from syn123 limit 100 offset 75");
		Long limit = 50L;
		Long offset = 10L;
		Long maxRowsPerPage = null;
		QuerySpecification converted = SqlElementUntils.overridePagination(model, offset, limit, maxRowsPerPage);
		assertEquals("SELECT * FROM syn123 LIMIT 50 OFFSET 85", converted.toString());
	}
	
	@Test
	public void testOverridePaginationAllNull() throws ParseException{
		QuerySpecification model = TableQueryParser.parserQuery("select * from syn123 limit 100 offset 75");
		Long limit = null;
		Long offset = null;
		Long maxRowsPerPage = null;
		QuerySpecification converted = SqlElementUntils.overridePagination(model, offset, limit, maxRowsPerPage);
		assertEquals("SELECT * FROM syn123 LIMIT 100 OFFSET 75", converted.toString());
	}
	
	@Test
	public void testCreateCountSqlNonAggregate() throws ParseException, SimpleAggregateQueryException{
		QuerySpecification model = new TableQueryParser("select * from syn123 where bar < 1.0 order by foo, bar limit 2 offset 5").querySpecification();
		String countSql = SqlElementUntils.createCountSql(model);
		assertEquals("SELECT COUNT(*) FROM syn123 WHERE bar < 1.0", countSql);
	}
	
	@Test
	public void testCreateCountSqlGroupBy() throws ParseException, SimpleAggregateQueryException{
		QuerySpecification model = new TableQueryParser("select foo, bar, count(*) from syn123 group by foo, bar").querySpecification();
		String countSql = SqlElementUntils.createCountSql(model);
		assertEquals("SELECT COUNT(DISTINCT foo, bar) FROM syn123", countSql);
	}
	
	@Test
	public void testCreateCountSqlDistinct() throws ParseException, SimpleAggregateQueryException{
		QuerySpecification model = new TableQueryParser("select distinct foo, bar from syn123").querySpecification();
		String countSql = SqlElementUntils.createCountSql(model);
		assertEquals("SELECT COUNT(DISTINCT foo, bar) FROM syn123", countSql);
	}
	
	@Test (expected=SimpleAggregateQueryException.class)
	public void testCreateCountSimpleAggregateCountStar() throws ParseException, SimpleAggregateQueryException{
		QuerySpecification model = new TableQueryParser("select count(*) from syn123").querySpecification();
		SqlElementUntils.createCountSql(model);
	}
	
	@Test (expected=SimpleAggregateQueryException.class)
	public void testCreateCountSimpleAggregateMultipleAggregate() throws ParseException, SimpleAggregateQueryException{
		QuerySpecification model = new TableQueryParser("select sum(foo), max(bar) from syn123").querySpecification();
		SqlElementUntils.createCountSql(model);
	}
	
	@Test
	public void testCreateSelectFromGroupBy() throws Exception{
		QuerySpecification model = new TableQueryParser("select foo as a, bar from syn123 group by bar, a").querySpecification();
		String result = SqlElementUntils.createSelectFromGroupBy(model.getSelectList(), model.getTableExpression().getGroupByClause());
		assertEquals("bar, foo", result);
	}
	
	@Test
	public void testCreateSelectFromGroupBySingleQuotes() throws Exception{
		QuerySpecification model = new TableQueryParser("select 'has space' as b from syn123 group by b").querySpecification();
		String result = SqlElementUntils.createSelectFromGroupBy(model.getSelectList(), model.getTableExpression().getGroupByClause());
		assertEquals("'has space'", result);
	}
	
	@Test
	public void testCreateSelectFromGroupByDoubleQuotes() throws Exception{
		QuerySpecification model = new TableQueryParser("select \"has space\" as b from syn123 group by b").querySpecification();
		String result = SqlElementUntils.createSelectFromGroupBy(model.getSelectList(), model.getTableExpression().getGroupByClause());
		assertEquals("\"has space\"", result);
	}
	
	/**
	 * This is a test for PLFM-3899.
	 */
	@Test
	public void testCreateCountSqlAsInGroupBy() throws Exception {
		QuerySpecification model = new TableQueryParser("select foo as a from syn123 group by a").querySpecification();
		String countSql = SqlElementUntils.createCountSql(model);
		assertEquals("SELECT COUNT(DISTINCT foo) FROM syn123", countSql);
	}
	
	@Test
	public void testCreateSelectWithoutAs() throws Exception {
		QuerySpecification model = new TableQueryParser("select foo as a, bar as boo from syn123").querySpecification();
		String countSql = SqlElementUntils.createSelectWithoutAs(model.getSelectList());
		assertEquals("foo, bar", countSql);
	}
	
	@Test
	public void testCreateSelectWithoutAsQuote() throws Exception {
		QuerySpecification model = new TableQueryParser("select 'foo' as a, \"bar\" as boo from syn123").querySpecification();
		String countSql = SqlElementUntils.createSelectWithoutAs(model.getSelectList());
		assertEquals("'foo', \"bar\"", countSql);
	}
	
	/**
	 * This is a test for PLFM-3900.
	 * @throws Exception
	 */
	@Test
	public void testCreateCountSqlDistinctWithAs() throws Exception {
		QuerySpecification model = new TableQueryParser("select distinct foo as a from syn123").querySpecification();
		String countSql = SqlElementUntils.createCountSql(model);
		assertEquals("SELECT COUNT(DISTINCT foo) FROM syn123", countSql);
	}
	
	
	////////////////////////////////////////////////////////////
	// appendFacetWhereClauseToStringBuilderIfNecessary() tests
	////////////////////////////////////////////////////////////
	@Test (expected = IllegalArgumentException.class)
	public void appendFacetWhereClauseToStringBuilderIfNecessaryNullBuilder(){
		SqlElementUntils.appendFacetWhereClauseToStringBuilderIfNecessary(null, facetSearchConditionString, whereClause);
	}
	
	@Test
	public void appendFacetWhereClauseToStringBuilderIfNecessaryNullFacetSearchConditionStringNullWhereClause(){
		SqlElementUntils.appendFacetWhereClauseToStringBuilderIfNecessary(stringBuilder, null, null);
		assertEquals(0, stringBuilder.length());
	}
	
	@Test
	public void appendFacetWhereClauseToStringBuilderIfNecessaryNullWhereClause(){
		SqlElementUntils.appendFacetWhereClauseToStringBuilderIfNecessary(stringBuilder, facetSearchConditionString, null);
		assertEquals("WHERE ("+ facetSearchConditionString + ")", stringBuilder.toString());
	}
	
	@Test
	public void appendFacetWhereClauseToStringBuilderIfNecessaryNullFacetSearchConditionString(){
		SqlElementUntils.appendFacetWhereClauseToStringBuilderIfNecessary(stringBuilder, null, whereClause);
		assertEquals("WHERE ("+ whereClause.getSearchCondition().toSql() + ")", stringBuilder.toString());
	}
	
	@Test
	public void appendFacetWhereClauseToStringBuilderIfNecessaryNoNulls(){
		SqlElementUntils.appendFacetWhereClauseToStringBuilderIfNecessary(stringBuilder, facetSearchConditionString, whereClause);
		assertEquals("WHERE ("+ whereClause.getSearchCondition().toSql() + ") AND (" + facetSearchConditionString + ")", stringBuilder.toString());
	}
	
	/////////////////////////////////////////////////////////
	// createFilteredFacetCountSqlQuerySpecification() tests
	/////////////////////////////////////////////////////////
	@Test(expected = IllegalArgumentException.class)
	public void testCreateFilteredFacetCountSqlQuerySpecificationNullColumnName(){
		SqlElementUntils.createFilteredFacetCountSqlQuerySpecification(null, simpleModel, facetSearchConditionString);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testCreateFilteredFacetCountSqlQuerySpecificationNullModel(){
		SqlElementUntils.createFilteredFacetCountSqlQuerySpecification(columnName, null, facetSearchConditionString);

	}
	
	@Test
	public void testCreateFilteredFacetCountSqlQuerySpecification(){
		QuerySpecification result = SqlElementUntils.createFilteredFacetCountSqlQuerySpecification(columnName, simpleModel, facetSearchConditionString);
		assertEquals("SELECT burrito AS value, COUNT(*) AS frequency FROM syn123 WHERE ( i LIKE 'trains' ) AND ( ( tabs > spaces ) ) GROUP BY burrito LIMIT 100", result.toSql());
	}
	
	/////////////////////////////////////////////////////////
	// createFilteredFacetRangeSqlQuerySpecification() tests
	/////////////////////////////////////////////////////////
	
	@Test(expected = IllegalArgumentException.class)
	public void testCreateFilteredFacetRangeSqlQuerySpecificationNullColumnName(){
		SqlElementUntils.createFilteredFacetRangeSqlQuerySpecification(null, simpleModel, facetSearchConditionString);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testCreateFilteredFacetRangeSqlQuerySpecificationNullModel(){
		SqlElementUntils.createFilteredFacetRangeSqlQuerySpecification(columnName, null, facetSearchConditionString);
	}
	
	@Test
	public void testCreateFilteredFacetRangeSqlQuerySpecification(){
		QuerySpecification result = SqlElementUntils.createFilteredFacetRangeSqlQuerySpecification(columnName, simpleModel, facetSearchConditionString);
		assertEquals("SELECT MIN(burrito) AS minimum, MAX(burrito) AS maximum FROM syn123 WHERE ( i LIKE 'trains' ) AND ( ( tabs > spaces ) )", result.toSql());
	}
}
