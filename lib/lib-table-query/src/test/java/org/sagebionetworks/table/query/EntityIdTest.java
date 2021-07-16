package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.EntityId;

public class EntityIdTest {

	@Test
	public void testEntityIdLowerCase() throws ParseException {
		EntityId value = new TableQueryParser("syn123").entityId();
		assertEquals("syn123", value.toSql());
	}

	@Test
	public void testEntityIdUpperCase() throws ParseException {
		EntityId value = new TableQueryParser("SYN123").entityId();
		assertEquals("syn123", value.toSql());
	}

	@Test
	public void testEntityIdMixedCase() throws ParseException {
		EntityId value = new TableQueryParser("SyN123").entityId();
		assertEquals("syn123", value.toSql());
	}

	@Test
	public void testEntityIdNoDigits() throws ParseException {
		assertThrows(ParseException.class, ()->{
			new TableQueryParser("syn").entityId();
		});
	}

	@Test
	public void testEntityIdWithVersion() throws ParseException {
		EntityId value = new TableQueryParser("syn123.456").entityId();
		assertEquals("syn123.456", value.toSql());
	}

	@Test
	public void testIdAndVersionDotOnly() throws ParseException {
		EntityId value = new TableQueryParser("syn123.").entityId();
		assertEquals("syn123", value.toSql());
	}

	@Test
	public void testIdAndVersionNoSyn() throws ParseException {
		assertThrows(ParseException.class, ()->{
			new TableQueryParser("123.456").entityId();
		});
	}
	
	@Test
	public void testGetChildren() throws ParseException {
		EntityId element = new TableQueryParser("syn123.").entityId();
		assertEquals(Collections.emptyList(), element.getChildren());
	}
}
