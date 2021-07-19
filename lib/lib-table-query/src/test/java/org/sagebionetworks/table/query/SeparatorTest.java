package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.CharacterStringLiteral;
import org.sagebionetworks.table.query.model.Separator;

public class SeparatorTest {
	
	@Test
	public void testSeparator() throws ParseException {
		Separator element = new TableQueryParser("separator '@'").separatorClause();
		assertEquals("SEPARATOR '@'", element.toSql());
		CharacterStringLiteral value = element.getSeparatorValue();
		assertNotNull(value);
		assertEquals("'@'", value.toSql());
	}
	
	@Test
	public void testGetChildren() throws ParseException {
		Separator element = new TableQueryParser("separator '@'").separatorClause();
		assertEquals(Collections.singleton(element.getSeparatorValue()), element.getChildren());
	}

}
