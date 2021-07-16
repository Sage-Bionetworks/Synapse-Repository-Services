package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.GeneralLiteral;
import org.sagebionetworks.table.query.model.UnsignedLiteral;
import org.sagebionetworks.table.query.model.UnsignedNumericLiteral;

public class UnsignedLiteralTest {

	@Test
	public void testUnsignedLiteralLong() throws ParseException {
		UnsignedLiteral element = new TableQueryParser("4567").unsignedLiteral();
		assertEquals("4567", element.toSql());
		// Should be able to find UnsignedNumericLiteral child
		UnsignedNumericLiteral child = element.getFirstElementOfType(UnsignedNumericLiteral.class);
		assertEquals("4567", child.toSql());
	}

	@Test
	public void testUnsignedLiteralLongSigned() throws ParseException {
		assertThrows(ParseException.class, () -> {
			// signs are not allowed
			new TableQueryParser("-4567").unsignedLiteral();
		});
	}

	@Test
	public void testUnsignedLiteralSingleQuotesString() throws ParseException {
		UnsignedLiteral element = new TableQueryParser("'in single quotes'").unsignedLiteral();
		assertEquals("'in single quotes'", element.toSql());
		// Should be able to find GeneralLiteral child
		GeneralLiteral child = element.getFirstElementOfType(GeneralLiteral.class);
		assertEquals("'in single quotes'", child.toSql());
	}

	@Test
	public void testGetChildren() throws ParseException {
		UnsignedLiteral element = new TableQueryParser("4567").unsignedLiteral();
		assertEquals(Collections.singleton(element.getChild()), element.getChildren());
	}
}
