package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import com.google.common.base.Strings;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.table.query.model.CharacterStringLiteral;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SQLElement;
import org.sagebionetworks.table.query.model.SearchCondition;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.SetFunctionSpecification;
import org.sagebionetworks.table.query.model.TableExpression;
import org.sagebionetworks.table.query.model.UnsignedNumericLiteral;
import org.sagebionetworks.table.query.model.ValueExpressionPrimary;

public class TableQueryParserTest {	
	
	@Test
	public void testSignedInteger() throws ParseException {
		UnsignedNumericLiteral element =  new TableQueryParser(" 1234567890 ").unsignedNumericLiteral();
		assertEquals("1234567890", element.toSql());
	}
	
	@Test
	public void testColumnReferenceLiteralLHS() throws ParseException{
		TableQueryParser parser = new TableQueryParser("foo");
		ColumnReference columnReference = parser.columnReference();
		assertNotNull(columnReference);
		String sql = toSQL(columnReference);
		assertEquals("foo", sql);
	}

	@Test
	public void testCharacterStringLiteral_UnderStringSizeLimit() throws ParseException{
		String maxString = "'" + Strings.repeat("a", ColumnConstants.MAX_ALLOWED_STRING_SIZE.intValue()) +  "'";
		TableQueryParser parser = new TableQueryParser(maxString);
		CharacterStringLiteral columnReference = parser.characterStringLiteral();
		assertNotNull(columnReference);
		String sql = toSQL(columnReference);
		assertEquals(maxString, sql);
	}

	@Test
	public void testCharacterStringLiteral_ExceedSizeLimit() throws ParseException{
		String exceedMaxString = "'" + Strings.repeat("a", ColumnConstants.MAX_ALLOWED_STRING_SIZE.intValue() + 1) +  "'";
		TableQueryParser parser = new TableQueryParser(exceedMaxString);
		assertThrows(IllegalArgumentException.class, () -> parser.characterStringLiteral());

	}
	
	@Test
	public void testColumnReferenceLiteralLHSAndRHS() throws ParseException{
		TableQueryParser parser = new TableQueryParser("foo.bar");
		ColumnReference columnReference = parser.columnReference();
		assertNotNull(columnReference);
		String sql = toSQL(columnReference);
		assertEquals("foo.bar", sql);
	}
	
	@Test
	public void testColumnReferenceStringLHS() throws ParseException{
		TableQueryParser parser = new TableQueryParser("\"with space\"");
		ColumnReference columnReference = parser.columnReference();
		assertNotNull(columnReference);
		String sql = toSQL(columnReference);
		assertEquals("\"with space\"", sql);
	}
	
	@Test
	public void testColumnReferenceStringLHSandRHS() throws ParseException{
		TableQueryParser parser = new TableQueryParser("\"with space\".\"cat's\"");
		ColumnReference columnReference = parser.columnReference();
		assertNotNull(columnReference);
		String sql = toSQL(columnReference);
		assertEquals("\"with space\".\"cat's\"", sql);
	}
	

	@Test
	public void testSetFunctionSpecification() throws ParseException{
		TableQueryParser parser = new TableQueryParser("count( distinct \"name\")");
		SetFunctionSpecification setFunction = parser.setFunctionSpecification();
		assertNotNull(setFunction);
		String sql = toSQL(setFunction);
		assertEquals("COUNT(DISTINCT \"name\")", sql);
	}
	
	@Test
	public void testValueExpressionPrimaryColumnReference() throws ParseException{
		TableQueryParser parser = new TableQueryParser("\"with space\".\"cat's\"");
		ValueExpressionPrimary valueExpressionPrimary = parser.valueExpressionPrimary();
		assertNotNull(valueExpressionPrimary);
		String sql = toSQL(valueExpressionPrimary);
		assertEquals("\"with space\".\"cat's\"", sql);
	}
	
	@Test
	public void testSelectListStart() throws ParseException{
		TableQueryParser parser = new TableQueryParser("*");
		SelectList element = parser.selectList();
		assertNotNull(element);
		String sql = toSQL(element);
		assertEquals("*", sql);
	}
	
	@Test
	public void testSelectListSingleLiteral() throws ParseException{
		TableQueryParser parser = new TableQueryParser("foo, \"bar\", max(cats)");
		SelectList element = parser.selectList();
		assertNotNull(element);
		String sql = toSQL(element);
		assertEquals("foo, \"bar\", MAX(cats)", sql);
	}
	
	@Test
	public void testPredicateComparison() throws ParseException{
		TableQueryParser parser = new TableQueryParser("foo.bar >= 10.1e-10");
		Predicate element = parser.predicate();
		String sql = toSQL(element);
		assertEquals("foo.bar >= 1.01E-9", sql);
	}
	
	@Test
	public void testPredicateSignedComparison() throws ParseException {
		TableQueryParser parser = new TableQueryParser("foo.bar >= -200");
		Predicate element = parser.predicate();
		String sql = toSQL(element);
		assertEquals("foo.bar >= -200", sql);
	}

	@Test
	public void testPredicateNull() throws ParseException{
		TableQueryParser parser = new TableQueryParser("foo is not null");
		Predicate element = parser.predicate();
		String sql = toSQL(element);
		assertEquals("foo IS NOT NULL", sql);
	}
	
	@Test
	public void testPredicateNotBetween() throws ParseException{
		TableQueryParser parser = new TableQueryParser("foo not between a and b");
		Predicate element = parser.predicate();
		String sql = toSQL(element);
		assertEquals("foo NOT BETWEEN a AND b", sql);
	}
	
	@Test
	public void testPredicateBetween() throws ParseException{
		TableQueryParser parser = new TableQueryParser("foo between a and b");
		Predicate element = parser.predicate();
		String sql = toSQL(element);
		assertEquals("foo BETWEEN a AND b", sql);
	}
	
	@Test
	public void testPredicateNotLike() throws ParseException{
		TableQueryParser parser = new TableQueryParser("bar not like '%a'");
		Predicate element = parser.predicate();
		String sql = toSQL(element);
		assertEquals("bar NOT LIKE '%a'", sql);
	}
	
	@Test
	public void testPredicateLike() throws ParseException{
		TableQueryParser parser = new TableQueryParser("bar like '%a'");
		Predicate element = parser.predicate();
		String sql = toSQL(element);
		assertEquals("bar LIKE '%a'", sql);
	}
	
	@Test
	public void testPredicateNotIn() throws ParseException{
		TableQueryParser parser = new TableQueryParser("bar not in(a, b,c)");
		Predicate element = parser.predicate();
		String sql = toSQL(element);
		assertEquals("bar NOT IN ( a, b, c )", sql);
	}
	
	@Test
	public void testPredicateIn() throws ParseException{
		TableQueryParser parser = new TableQueryParser("bar in(a, b,c)");
		Predicate element = parser.predicate();
		String sql = toSQL(element);
		assertEquals("bar IN ( a, b, c )", sql);
	}
	
	@Test
	public void testSearchConditionOr() throws ParseException{
		TableQueryParser parser = new TableQueryParser("bar <> a or foo > 3");
		SearchCondition element = parser.searchCondition();
		String sql = toSQL(element);
		assertEquals("bar <> a OR foo > 3", sql);
	}
	
	@Test
	public void testSearchConditionAnd() throws ParseException{
		TableQueryParser parser = new TableQueryParser("bar =1 and foo = 2");
		SearchCondition element = parser.searchCondition();
		String sql = toSQL(element);
		assertEquals("bar = 1 AND foo = 2", sql);
	}
	
	@Test
	public void testSearchConditionNestedOr() throws ParseException{
		TableQueryParser parser = new TableQueryParser("(bar =1 and foo = 2) or www is not null");
		SearchCondition element = parser.searchCondition();
		String sql = toSQL(element);
		assertEquals("( bar = 1 AND foo = 2 ) OR www IS NOT NULL", sql);
	}
	
	@Test
	public void testSearchConditionNestedAnd() throws ParseException{
		TableQueryParser parser = new TableQueryParser("(bar =1 and foo = 2) and www is not null");
		SearchCondition element = parser.searchCondition();
		String sql = toSQL(element);
		assertEquals("( bar = 1 AND foo = 2 ) AND www IS NOT NULL", sql);
	}
	
	@Test
	public void testTableExpression() throws ParseException{
		TableQueryParser parser = new TableQueryParser("from syn123");
		TableExpression element = parser.tableExpression();
		assertNotNull(element);
		String sql = toSQL(element);
		assertEquals("FROM syn123", sql);
	}
	
	@Test
	public void testTableExpressionWithWhere() throws ParseException{
		TableQueryParser parser = new TableQueryParser("from syn123 where a > 'b'");
		TableExpression element = parser.tableExpression();
		assertNotNull(element);
		String sql = toSQL(element);
		assertEquals("FROM syn123 WHERE a > 'b'", sql);
	}
	
	/**
	 * We must be able to pares all of the example SQL.
	 */
	@Test
	public void testAllExamples(){
		List<SQLExample> exampleList = SQLExampleProvider.getSQLExamples();
		assertNotNull(exampleList);
		assertTrue(exampleList.size() > 2);
		// Test each example
		for(SQLExample example: exampleList){

			try{
				// Make sure we can parse the SQL
				System.out.println("Parsing: "+example.getSql());
				TableQueryParser.parserQuery(example.getSql());
			} catch(ParseException e){
				e.printStackTrace();
				fail("Failed to parse: '"+example.getSql()+"' Error: "+e.getMessage());
			}
		}
	}
	
	@Test
	public void testSelectStar() throws ParseException{
		// Parse the query into a basic model object
		QuerySpecification sq = TableQueryParser.parserQuery("select * from syn123");
		assertNotNull(sq);
		assertEquals(null,  sq.getSetQuantifier());
		assertNotNull(sq.getSelectList());
		assertEquals(Boolean.TRUE,  sq.getSelectList().getAsterisk(),"Simple select * was missing the asterisk");
		assertEquals(null,  sq.getSelectList().getColumns(), "Select * should not have any columns");
		assertNotNull(sq.getTableExpression());
		assertNotNull(sq.getTableExpression().getFromClause());
		assertNotNull(sq.getTableExpression().getFromClause().getTableReference());
		assertNotNull(sq.getTableExpression().getFromClause().getTableReference().getTableName());
		assertEquals("syn123", sq.getTableExpression().getFromClause().getTableReference().getTableName());
	}
	
	@Test
	public void testSelectTableNameSigned() throws ParseException {
		// Parse the query into a basic model object
		QuerySpecification sq = TableQueryParser.parserQuery("select * from syn123");
		assertNotNull(sq);
		assertNotNull(sq.getTableExpression());
		assertNotNull(sq.getTableExpression().getFromClause());
		assertNotNull(sq.getTableExpression().getFromClause().getTableReference());
		assertNotNull(sq.getTableExpression().getFromClause().getTableReference().getTableName());
		assertEquals("syn123", sq.getTableExpression().getFromClause().getTableReference().getTableName());
	}
	
	
	/**
	 * Helper to convert a SQLElement to its SQL string.
	 * @param element
	 * @return
	 */
	public static String toSQL(SQLElement element){
		return element.toString();
	}
	
	@Test
	public void testSelectMultipleColumns() throws ParseException{
		QuerySpecification element = TableQueryParser.parserQuery("select foo, bar, foobar from syn123");
		String sql = toSQL(element);
		assertEquals("SELECT foo, bar, foobar FROM syn123", sql);
	}
	
	@Test
	public void testSelectDoubleQuotedColumnName() throws ParseException{
		QuerySpecification element = TableQueryParser.parserQuery("select \"foo \"\"&\"\" Bar\" from syn123");
		String sql = toSQL(element);
		assertEquals("SELECT \"foo \"\"&\"\" Bar\" FROM syn123", sql);
	}
	
	@Test
	public void testSelectGroupBy() throws ParseException{
		QuerySpecification element = TableQueryParser.parserQuery("select foo, count(bar) from syn456 group by foo");
		String sql = toSQL(element);
		assertEquals("SELECT foo, COUNT(bar) FROM syn456 GROUP BY foo", sql);
	}

	@Test
	public void testQueryAllParts() throws ParseException{
		QuerySpecification element = TableQueryParser.parserQuery("select foo, count(bar) from syn456 where bar = 'cat''s' group by foo order by bar limit 1 offset 2");
		String sql = toSQL(element);
		assertEquals("SELECT foo, COUNT(bar) FROM syn456 WHERE bar = 'cat''s' GROUP BY foo ORDER BY bar LIMIT 1 OFFSET 2", sql);
	}
	
	@Test
	public void testQueryEndOfFile() throws ParseException{
		assertThrows(ParseException.class, ()->{
			// There must not be anything at the end of the query.
			TableQueryParser.parserQuery("select foo from syn456 limit 1 offset 2 select foo");
		});
	}
	
	/**
	 * See PLFM-2618
	 * Validate that literals can contain key words but not be keywords.
	 * @throws ParseException 
	 * 
	 */
	@Test
	public void testKeyWordsInLiterals() throws ParseException{
		QuerySpecification element = TableQueryParser.parserQuery("select doesNotExist, isIn, string, sAnd, sNot, WeLikeIt from SyN456 limit 1 offset 2");
		assertNotNull(element);
		String sql = toSQL(element);
		assertEquals("SELECT doesNotExist, isIn, string, sAnd, sNot, WeLikeIt FROM syn456 LIMIT 1 OFFSET 2", sql);
	}
	
	/**
	 * See PLFM-3878
	 * @throws ParseException 
	 */
	public void testCountDistinctMultipleColumns() throws ParseException{
		QuerySpecification element = TableQueryParser.parserQuery("select count(distinct one, two) from SyN456");
		assertNotNull(element);
		String sql = toSQL(element);
		assertEquals("", sql);
	}
	
	/**
	 * Test for PLFM-4566
	 * @throws ParseException 
	 */
	@Test
	public void testArithmetic() throws ParseException{
		QuerySpecification element = TableQueryParser.parserQuery("select foo/100 from syn123");
		assertEquals("SELECT foo/100 FROM syn123", element.toSql());
	}

	/**
	 * Test for PLFM-4510
	 * Make sure that all ASCII values are recognized by the parser
	 */
	@Test
	public void testAsciiTokens() {
		for (char c = 0; c < 256; c++) {
			try {
				TableQueryParser.parserQuery("select foo" + c + " from syn123");
			} catch (ParseException pe) {
				// No problem
			} catch (TokenMgrError tme) {
				fail("Encountered an unexpected TokenMgrError: " + tme);
			}
		}
	}
	
	/**
	 * Test for PLFM-5281
	 * @throws ParseException 
	 */
	@Test
	public void testGroupConcat() throws ParseException {
		QuerySpecification element = TableQueryParser.parserQuery(
				"select foo, group_concat(distinct bar order by bar desc separator '#') from syn123 group by foo");
		assertEquals("SELECT foo, GROUP_CONCAT(DISTINCT bar ORDER BY bar DESC SEPARATOR '#') FROM syn123 GROUP BY foo",
				element.toSql());
	}
	
	@Test
	public void testTableWithVersion() throws ParseException {
		QuerySpecification element = TableQueryParser.parserQuery("select * from syn123.567 where foo = 'bar'");
		assertEquals("SELECT * FROM syn123.567 WHERE foo = 'bar'",	element.toSql());
	}
	
	@Test
	public void testTableNameTooManyDots() throws ParseException {
		assertThrows(ParseException.class, ()->{
			TableQueryParser.parserQuery("select * from syn123.567.333 where foo = 'bar'");
		});
	}
	
	@Test
	public void testTableNameTrailingDot() throws ParseException {
		assertThrows(ParseException.class, ()->{
			TableQueryParser.parserQuery("select * from syn123.567. where foo = 'bar'");
		});
	}
	
	@Test
	public void testEntityIdColumnName() throws ParseException {
		QuerySpecification element = TableQueryParser.parserQuery("select * from syn123 where syn567 = 'bar'");
		assertEquals("SELECT * FROM syn123 WHERE syn567 = 'bar'", element.toSql());
	}
	
	@Test
	public void testUnknownCharacters() throws ParseException {
		char unknownChar = 0xffff;
		QuerySpecification element = TableQueryParser.parserQuery("select * from syn123 " +unknownChar);
		assertEquals("SELECT * FROM syn123", element.toSql());
	}

	@Test
	public void testCurrentUser() throws ParseException {
		QuerySpecification element = TableQueryParser.parserQuery("select * from syn123 where user = CURRENT_USER()");
		assertEquals("SELECT * FROM syn123 WHERE user = CURRENT_USER()", element.toSql());
	}

	@Test
	public void testCurrentUserInvalidPlacement() throws ParseException {
		assertThrows(ParseException.class, ()->{
			TableQueryParser.parserQuery("select * from syn123 where CURRENT_USER() = CURRENT_USER()");
		});
	}

	@Test
	public void testCurrentUserInvalidPlacement2() throws ParseException {
		assertThrows(ParseException.class, ()->{
			TableQueryParser.parserQuery("select * from CURRENT_USER() where foo = CURRENT_USER()");
		});
	}

	@Test
	public void testCurrentUserInvalidSpelling() throws ParseException {
		assertThrows(ParseException.class, ()->{
			TableQueryParser.parserQuery("select * from syn123 where foo = CURRENT_USER");
		});
	}

}
