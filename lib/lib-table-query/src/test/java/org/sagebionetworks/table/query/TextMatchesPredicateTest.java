package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.table.query.model.CharacterStringLiteral;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.Element;
import org.sagebionetworks.table.query.model.TextMatchesPredicate;
import org.sagebionetworks.table.query.model.UnsignedLiteral;

public class TextMatchesPredicateTest {

	@Test
	public void testTextMatchesPredicate() throws ParseException {
		TextMatchesPredicate element = new TableQueryParser("TEXT_MATCHES('test')").textMatchesPredicate();
		
		// Call under test
		String sql = element.toSql();
		
		assertEquals("TEXT_MATCHES('test')", sql);
		
	}
	
	@Test
	public void testTextMatchesPredicateWithCaseInsensitive() throws ParseException {
		TextMatchesPredicate element = new TableQueryParser("text_MATCHES('test')").textMatchesPredicate();
		
		// Call under test
		String sql = element.toSql();
		
		assertEquals("TEXT_MATCHES('test')", sql);
		
	}
	
	@Test
	public void testTextMatchesPredicateWithNonCharacterLiteral() throws ParseException {
		assertThrows(ParseException.class, () -> {			
			new TableQueryParser("TEXT_MATCHES(1.0)").textMatchesPredicate();
		});
		assertThrows(ParseException.class, () -> {			
			new TableQueryParser("TEXT_MATCHES(false)").textMatchesPredicate();
		});
		assertThrows(ParseException.class, () -> {			
			new TableQueryParser("TEXT_MATCHES(1)").textMatchesPredicate();
		});
		assertThrows(ParseException.class, () -> {			
			new TableQueryParser("TEXT_MATCHES(\"something\")").textMatchesPredicate();
		});
	}
	
	@Test
	public void testToSQL() {
		CharacterStringLiteral literal = new CharacterStringLiteral("some string");
		TextMatchesPredicate element = new TextMatchesPredicate(literal);
		
		// Call under test
		String sql = element.toSql();
		
		assertEquals("TEXT_MATCHES('some string')", sql);
	}
	
	@Test
	public void testGetChildren() {
		CharacterStringLiteral literal = new CharacterStringLiteral("some string");
		TextMatchesPredicate element = new TextMatchesPredicate(literal);
		
		// Call under test
		Iterator<Element> children = element.getChildren().iterator();
		
		assertEquals("'some string'", children.next().toSql());
		assertFalse(children.hasNext());
	}
	
	@Test
	public void testGetLeftHandSide() throws ParseException {
		CharacterStringLiteral literal = new CharacterStringLiteral("some string");
		TextMatchesPredicate element = new TextMatchesPredicate(literal);
		
		// Call under test
		ColumnReference columnReference = (ColumnReference) element.getLeftHandSide().getChild();
		
		assertEquals(ColumnType.STRING, columnReference.getImplicitColumnType());
		assertEquals(TableConstants.ROW_SEARCH_CONTENT, columnReference.toSql());
	}
	
	@Test
	public void testGetRightHandSideValues() {
		CharacterStringLiteral literal = new CharacterStringLiteral("some string");
		TextMatchesPredicate element = new TextMatchesPredicate(literal);
		
		// Call under test
		Iterator<UnsignedLiteral> rightHandSideValues = element.getRightHandSideValues().iterator();
		
		assertEquals("'some string'", rightHandSideValues.next().toSql());
		assertFalse(rightHandSideValues.hasNext());
	}
}
