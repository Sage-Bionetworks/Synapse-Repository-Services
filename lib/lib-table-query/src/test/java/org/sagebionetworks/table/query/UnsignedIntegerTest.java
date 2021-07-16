package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.UnsignedInteger;

public class UnsignedIntegerTest {

	@Test
	public void testUnsigned() throws ParseException {
		UnsignedInteger element = new TableQueryParser("123456").unsignedInteger();
		assertEquals("123456", element.toSql());
	}

	@Test
	public void testSigned() throws ParseException {
		assertThrows(ParseException.class, () -> {
			// signs are not allowed
			new TableQueryParser("-123456").unsignedInteger();
		});
	}

	@Test
	public void testGetChildren() throws ParseException {
		UnsignedInteger element = new TableQueryParser("123456").unsignedInteger();
		assertEquals(Collections.emptyList(), element.getChildren());
	}

}
