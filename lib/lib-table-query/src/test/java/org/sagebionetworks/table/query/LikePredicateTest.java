package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.Element;
import org.sagebionetworks.table.query.model.EscapeCharacter;
import org.sagebionetworks.table.query.model.LikePredicate;
import org.sagebionetworks.table.query.model.Pattern;
import org.sagebionetworks.table.query.model.Predicate;
import org.sagebionetworks.table.query.model.ReplaceableBox;
import org.sagebionetworks.table.query.model.UnsignedLiteral;
import org.sagebionetworks.table.query.util.SqlElementUtils;

import com.google.common.collect.Lists;

public class LikePredicateTest {

	@Test
	public void testLikePredicateToSQL() throws ParseException {
		ColumnReference columnReferenceLHS = SqlElementUtils.createColumnReference("foo");
		Boolean not = null;
		Pattern pattern = SqlElementUtils.createPattern("'%middle%'");
		EscapeCharacter escapeCharacter = null;
		LikePredicate element = new LikePredicate(columnReferenceLHS, not, pattern, escapeCharacter);
		assertEquals("foo LIKE '%middle%'", element.toString());
	}

	@Test
	public void testLikePredicateToSQLNot() throws ParseException {
		ColumnReference columnReferenceLHS = SqlElementUtils.createColumnReference("foo");
		Boolean not = Boolean.TRUE;
		Pattern pattern = SqlElementUtils.createPattern("'%middle%'");
		EscapeCharacter escapeCharacter = null;
		LikePredicate element = new LikePredicate(columnReferenceLHS, not, pattern, escapeCharacter);
		assertEquals("foo NOT LIKE '%middle%'", element.toString());
	}

	@Test
	public void testLikePredicateToSQLEscape() throws ParseException {
		ColumnReference columnReferenceLHS = SqlElementUtils.createColumnReference("foo");
		Boolean not = Boolean.TRUE;
		Pattern pattern = SqlElementUtils.createPattern("'%middle%'");
		EscapeCharacter escapeCharacter = SqlElementUtils.createEscapeCharacter("'$$'");
		LikePredicate element = new LikePredicate(columnReferenceLHS, not, pattern, escapeCharacter);
		assertEquals("foo NOT LIKE '%middle%' ESCAPE '$$'", element.toString());
	}

	@Test
	public void testHasPredicate() throws ParseException {
		Predicate predicate = new TableQueryParser("foo like '%aa%'").predicate();
		LikePredicate element = predicate.getFirstElementOfType(LikePredicate.class);
		assertEquals("foo", element.getLeftHandSide().toSql());
		List<UnsignedLiteral> values = Lists.newArrayList(element.getRightHandSideValues());
		assertNotNull(values);
		assertEquals(1, values.size());
		assertEquals("%aa%", values.get(0).toSqlWithoutQuotes());
	}

	@Test
	public void testHasPredicateEscape() throws ParseException {
		Predicate predicate = new TableQueryParser("foo like '%aa%' ESCAPE '@'").predicate();
		LikePredicate element = predicate.getFirstElementOfType(LikePredicate.class);
		assertEquals("foo", element.getLeftHandSide().toSql());
		List<UnsignedLiteral> values = Lists.newArrayList(element.getRightHandSideValues());
		assertNotNull(values);
		assertEquals(2, values.size());
		assertEquals("%aa%", values.get(0).toSqlWithoutQuotes());
		assertEquals("@", values.get(1).toSqlWithoutQuotes());
	}

	@Test
	public void testGetChildren() throws ParseException {
		Predicate predicate = new TableQueryParser("foo like '%aa%' ESCAPE '@'").predicate();
		LikePredicate element = predicate.getFirstElementOfType(LikePredicate.class);
		List<Element> children = element.getChildrenStream().collect(Collectors.toList());
		assertEquals(Arrays.asList(new ReplaceableBox<ColumnReference>(element.getLeftHandSide()), element.getPattern(),
				element.getEscapeCharacter()), children);
	}
}
