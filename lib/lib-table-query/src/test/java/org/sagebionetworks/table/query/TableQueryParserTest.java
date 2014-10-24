package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.SQLElement;
import org.sagebionetworks.table.query.model.SearchCondition;
import org.sagebionetworks.table.query.model.SelectList;
import org.sagebionetworks.table.query.model.SetFunctionSpecification;
import org.sagebionetworks.table.query.model.TableExpression;
import org.sagebionetworks.table.query.model.SignedLiteral;
import org.sagebionetworks.table.query.model.SignedValueSpecification;
import org.sagebionetworks.table.query.model.ValueExpressionPrimary;

public class TableQueryParserTest {
	
	/**
	 * Delimited Identifiers are surrounded by double quotes.
	 * 
	 * @throws ParseException
	 */
	@Test
	public void testDelimitedIdentifier() throws ParseException{
		StringBuilder builder = new StringBuilder();
		// Double quotes are used to escape double quotes.
		TableQueryParser parser = new TableQueryParser("\"This is a long string in quotes\"");
		parser.delimitedIentifier(builder);
		assertEquals("This is a long string in quotes", builder.toString());
	}

	/**
	 * Double quotes within a delimited identifier are escaped with double quotes
	 * @throws ParseException
	 */
	@Test
	public void testDelimitedIdentifierEscape() throws ParseException{
		StringBuilder builder = new StringBuilder();
		TableQueryParser parser = new TableQueryParser("\"A string \"\"within a\"\" string.\"");
		parser.delimitedIentifier(builder);
		assertEquals("A string \"within a\" string.", builder.toString());
	}

	@Test
	public void testDelimitedIdentifierEmptyString() throws ParseException{
		StringBuilder builder = new StringBuilder();
		TableQueryParser parser = new TableQueryParser("\"\"");
		parser.delimitedIentifier(builder);
		assertEquals("", builder.toString());
	}
	
	/**
	 * Character String Literals are surrounded by single quotes.
	 * 
	 * @throws ParseException
	 */
	@Test
	public void testCharacterStringLiteral() throws ParseException{
		StringBuilder builder = new StringBuilder();
		// Double quotes are used to escape double quotes.
		TableQueryParser parser = new TableQueryParser("'This is a long string in quotes'");
		parser.characterStringLiteral(builder);
		assertEquals("This is a long string in quotes", builder.toString());
	}

	/**
	 * Single quotes within a Character String Literal are escaped with single quotes
	 * @throws ParseException
	 */
	@Test
	public void testCharacterStringLiteralEscape() throws ParseException{
		StringBuilder builder = new StringBuilder();
		TableQueryParser parser = new TableQueryParser("'A string ''within a'' string.'");
		parser.characterStringLiteral(builder);
		assertEquals("A string 'within a' string.", builder.toString());
	}

	@Test
	public void testCharacterStringLiteralEmptyString() throws ParseException{
		StringBuilder builder = new StringBuilder();
		TableQueryParser parser = new TableQueryParser("''");
		parser.characterStringLiteral(builder);
		assertEquals("", builder.toString());
	}
	
	
	@Test
	public void testSignedInteger() throws ParseException {
		StringBuilder builder = new StringBuilder();
		TableQueryParser parser = new TableQueryParser(" 1234567890 ");
		parser.signedInteger(builder);
		assertEquals("1234567890", builder.toString());
	}
	
	@Test
	public void testSignedIntegerPlus() throws ParseException{
		StringBuilder builder = new StringBuilder();
		TableQueryParser parser = new TableQueryParser(" +1234567890 ");
		parser.signedInteger(builder);
		assertEquals("+1234567890", builder.toString());
	}
	
	@Test
	public void testSignedIntegerMinus() throws ParseException{
		StringBuilder builder = new StringBuilder();
		TableQueryParser parser = new TableQueryParser(" -1234567890 ");
		parser.signedInteger(builder);
		assertEquals("-1234567890", builder.toString());
	}
	
	@Test
	public void testExactNumericLiteralLeadingZero() throws ParseException{
		StringBuilder builder = new StringBuilder();
		TableQueryParser parser = new TableQueryParser("0.123");
		parser.exactNumericLiteral(builder);
		assertEquals("0.123", builder.toString());
	}
	
	@Test
	public void testExactNumericLiteralLeadingPeriod() throws ParseException{
		StringBuilder builder = new StringBuilder();
		TableQueryParser parser = new TableQueryParser(".123");
		parser.exactNumericLiteral(builder);
		assertEquals(".123", builder.toString());
	}
	
	@Test
	public void testApproximateNumericLiteral() throws ParseException{
		StringBuilder builder = new StringBuilder();
		TableQueryParser parser = new TableQueryParser("1.123e-12");
		parser.approximateNumericLiteral(builder);
		assertEquals("1.123e-12", builder.toString());
	}
	
	@Test
	public void testSignedNumericLiteralNoExponent() throws ParseException {
		StringBuilder builder = new StringBuilder();
		TableQueryParser parser = new TableQueryParser(".12");
		parser.signedNumericLiteral(builder);
		assertEquals(".12", builder.toString());
	}
	
	@Test
	public void testSignedNumericLiteralIntegerStartNoExponent() throws ParseException {
		StringBuilder builder = new StringBuilder();
		TableQueryParser parser = new TableQueryParser("123.1");
		parser.signedNumericLiteral(builder);
		assertEquals("123.1", builder.toString());
	}
	
	@Test
	public void testSignedNumericLiteralWithExponent() throws ParseException {
		StringBuilder builder = new StringBuilder();
		TableQueryParser parser = new TableQueryParser("9.12E+123");
		parser.signedNumericLiteral(builder);
		assertEquals("9.12E+123", builder.toString());
	}
	
	@Test
	public void testSignedNumericLiteralNoIntegerStartWithExponent() throws ParseException {
		StringBuilder builder = new StringBuilder();
		TableQueryParser parser = new TableQueryParser(".12E+123");
		parser.signedNumericLiteral(builder);
		assertEquals(".12E+123", builder.toString());
	}
	
	@Test
	public void testSignedNumericLiteralWithNoSign() throws ParseException{
		StringBuilder builder = new StringBuilder();
		TableQueryParser parser = new TableQueryParser("9.12E+123");
		parser.signedNumericLiteral(builder);
		assertEquals("9.12E+123", builder.toString());
	}
	
	@Test
	public void testSignedNumericLiteralPlus() throws ParseException{
		StringBuilder builder = new StringBuilder();
		TableQueryParser parser = new TableQueryParser("+9.12E+123");
		parser.signedNumericLiteral(builder);
		assertEquals("+9.12E+123", builder.toString());
	}
	
	@Test
	public void testSignedNumericLiteralMinus() throws ParseException{
		StringBuilder builder = new StringBuilder();
		TableQueryParser parser = new TableQueryParser("-9.12E+123");
		parser.signedNumericLiteral(builder);
		assertEquals("-9.12E+123", builder.toString());
	}
	
	/**
	 * Regular identifiers must start with a letter.
	 * 
	 * @throws ParseException
	 */
	@Test (expected=ParseException.class)
	public void testRegularIdentifierBadStart() throws ParseException{
		StringBuilder builder = new StringBuilder();
		TableQueryParser parser = new TableQueryParser("123");
		parser.regularIdentifier(builder);
	}
	
	@Test 
	public void testRegularIdentifierHappy() throws ParseException{
		StringBuilder builder = new StringBuilder();
		TableQueryParser parser = new TableQueryParser("T1_23a");
		parser.regularIdentifier(builder);
		assertEquals("T1_23a", builder.toString());
	}
	/**
	 * Since 'e' can also be an exponent make sure it works in an identifier.
	 * @throws ParseException
	 */
	@Test 
	public void testRegularIdentifierWithE() throws ParseException{
		StringBuilder builder = new StringBuilder();
		TableQueryParser parser = new TableQueryParser("ebay");
		parser.regularIdentifier(builder);
		assertEquals("ebay", builder.toString());
	}
	
	@Test 
	public void testRegularIdentifierWithEAndNumber() throws ParseException{
		StringBuilder builder = new StringBuilder();
		TableQueryParser parser = new TableQueryParser("e123");
		parser.regularIdentifier(builder);
		assertEquals("e123", builder.toString());
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
	public void testSignedLiteralSignedNumericLiteral() throws ParseException {
		TableQueryParser parser = new TableQueryParser("123.456e-1");
		SignedLiteral model = parser.signedLiteral();
		assertNotNull(model);
		String sql = toSQL(model);
		assertEquals("123.456e-1", sql);
	}
	
	@Test
	public void testSignedLiteralGeneralLiteral() throws ParseException {
		TableQueryParser parser = new TableQueryParser("'Batman''s car'");
		SignedLiteral model = parser.signedLiteral();
		assertNotNull(model);
		String sql = toSQL(model);
		assertEquals("'Batman''s car'", sql);
	}
	
	@Test
	public void testSignedNumericLiteralInteger() throws ParseException {
		TableQueryParser parser = new TableQueryParser("123");
		StringBuilder builder = new StringBuilder();
		parser.signedNumericLiteral(builder);
		assertEquals("123", builder.toString());
	}
	
	@Test
	public void testSignedNumericLiteralDouble() throws ParseException {
		TableQueryParser parser = new TableQueryParser("123.456");
		StringBuilder builder = new StringBuilder();
		parser.signedNumericLiteral(builder);
		assertEquals("123.456", builder.toString());
	}
	
	@Test
	public void testSignedNumericLiteralExponent() throws ParseException {
		TableQueryParser parser = new TableQueryParser("123.456e2");
		StringBuilder builder = new StringBuilder();
		parser.signedNumericLiteral(builder);
		assertEquals("123.456e2", builder.toString());
	}
	
	@Test
	public void testSignedValueSpecificationInteger() throws ParseException {
		TableQueryParser parser = new TableQueryParser("123456");
		SignedValueSpecification signedValueSpec = parser.signedValueSpecification();
		assertNotNull(signedValueSpec);
		String sql = toSQL(signedValueSpec);
		assertEquals("123456", sql);
	}
	
	@Test
	public void testSignedValueSpecificationIntegerMinus() throws ParseException {
		TableQueryParser parser = new TableQueryParser("-123456");
		SignedValueSpecification signedValueSpec = parser.signedValueSpecification();
		assertNotNull(signedValueSpec);
		String sql = toSQL(signedValueSpec);
		assertEquals("-123456", sql);
	}

	@Test
	public void testSignedValueSpecificationIntegerPlus() throws ParseException {
		TableQueryParser parser = new TableQueryParser("+123456");
		SignedValueSpecification signedValueSpec = parser.signedValueSpecification();
		assertNotNull(signedValueSpec);
		String sql = toSQL(signedValueSpec);
		assertEquals("+123456", sql);
	}

	@Test
	public void testSignedValueSpecificationCharacterString() throws ParseException {
		TableQueryParser parser = new TableQueryParser("'a string'");
		SignedValueSpecification signedValueSpec = parser.signedValueSpecification();
		assertNotNull(signedValueSpec);
		String sql = toSQL(signedValueSpec);
		assertEquals("'a string'", sql);
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
		assertEquals("foo.bar >= 10.1e-10", sql);
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
		assertEquals("Simple select * was missing the asterisk", Boolean.TRUE,  sq.getSelectList().getAsterisk());
		assertEquals("Select * should not have any columns", null,  sq.getSelectList().getColumns());
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
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		return builder.toString();
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
	
	@Test (expected=ParseException.class)
	public void testQueryEndOfFile() throws ParseException{
		// There must not be anything at the end of the query.
		TableQueryParser.parserQuery("select foo from syn456 limit 1 offset 2 select foo");
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
		assertEquals("SELECT doesNotExist, isIn, string, sAnd, sNot, WeLikeIt FROM SyN456 LIMIT 1 OFFSET 2", sql);
	}
}
