package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.query.model.CharacterStringLiteral;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.Element;
import org.sagebionetworks.table.query.model.TextMatchesMySQLPredicate;
import org.sagebionetworks.table.query.model.TextMatchesPredicate;
import org.sagebionetworks.table.query.model.UnsignedLiteral;

public class TextMatchesMySQLPredicateTest {
	
	@Test
	public void testToSQL() {
		TextMatchesPredicate inputPredicate = new TextMatchesPredicate(new CharacterStringLiteral("some string"));
		TextMatchesMySQLPredicate element = new TextMatchesMySQLPredicate(inputPredicate);
		
		// Call under test
		String sql = element.toSql();
		
		assertEquals("MATCH(ROW_SEARCH_CONTENT) AGAINST('some string')", sql);
	}
	
	@Test
	public void testGetChildren() {
		TextMatchesPredicate inputPredicate = new TextMatchesPredicate(new CharacterStringLiteral("some string"));
		TextMatchesMySQLPredicate element = new TextMatchesMySQLPredicate(inputPredicate);
		
		// Call under test
		Iterator<Element> children = element.getChildren().iterator();
		
		assertEquals("'some string'", children.next().toSql());
		assertFalse(children.hasNext());
	}
	
	@Test
	public void testGetLeftHandSide() throws ParseException {
		TextMatchesPredicate inputPredicate = new TextMatchesPredicate(new CharacterStringLiteral("some string"));
		TextMatchesMySQLPredicate element = new TextMatchesMySQLPredicate(inputPredicate);
		
		// Call under test
		ColumnReference columnReference = element.getLeftHandSide();
		
		assertEquals(ColumnType.STRING, columnReference.getImplicitColumnType());
		assertEquals(TableConstants.ROW_SEARCH_CONTENT, columnReference.toSql());
	}
	
	@Test
	public void testGetRightHandSideValues() {
		TextMatchesPredicate inputPredicate = new TextMatchesPredicate(new CharacterStringLiteral("some string"));
		TextMatchesMySQLPredicate element = new TextMatchesMySQLPredicate(inputPredicate);
		
		// Call under test
		Iterator<UnsignedLiteral> rightHandSideValues = element.getRightHandSideValues().iterator();
		
		assertEquals("'some string'", rightHandSideValues.next().toSql());
		assertFalse(rightHandSideValues.hasNext());
	}

}
