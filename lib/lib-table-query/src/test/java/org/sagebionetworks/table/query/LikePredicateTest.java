package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.EscapeCharacter;
import org.sagebionetworks.table.query.model.LikePredicate;
import org.sagebionetworks.table.query.model.Pattern;
import org.sagebionetworks.table.query.util.SqlElementUntils;

public class LikePredicateTest {

	
	@Test
	public void testLikePredicateToSQL() throws ParseException{
		ColumnReference columnReferenceLHS = SqlElementUntils.createColumnReference("foo");
		Boolean not = null;
		Pattern pattern = SqlElementUntils.createPattern("'%middle%'");
		EscapeCharacter escapeCharacter = null;
		LikePredicate element = new LikePredicate(columnReferenceLHS, not, pattern, escapeCharacter);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("foo LIKE '%middle%'", builder.toString());
	}
	
	@Test
	public void testLikePredicateToSQLNot() throws ParseException{
		ColumnReference columnReferenceLHS = SqlElementUntils.createColumnReference("foo");
		Boolean not = Boolean.TRUE;
		Pattern pattern = SqlElementUntils.createPattern("'%middle%'");
		EscapeCharacter escapeCharacter = null;
		LikePredicate element = new LikePredicate(columnReferenceLHS, not, pattern, escapeCharacter);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("foo NOT LIKE '%middle%'", builder.toString());
	}
	
	@Test
	public void testLikePredicateToSQLEscape() throws ParseException{
		ColumnReference columnReferenceLHS = SqlElementUntils.createColumnReference("foo");
		Boolean not = Boolean.TRUE;
		Pattern pattern = SqlElementUntils.createPattern("'%middle%'");
		EscapeCharacter escapeCharacter = SqlElementUntils.createEscapeCharacter("'$$'");
		LikePredicate element = new LikePredicate(columnReferenceLHS, not, pattern, escapeCharacter);
		StringBuilder builder = new StringBuilder();
		element.toSQL(builder, null);
		assertEquals("foo NOT LIKE '%middle%' ESCAPE '$$'", builder.toString());
	}
}
