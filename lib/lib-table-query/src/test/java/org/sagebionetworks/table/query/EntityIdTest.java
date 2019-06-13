package org.sagebionetworks.table.query;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
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

	@Test(expected = ParseException.class)
	public void testEntityIdNoDigits() throws ParseException {
		new TableQueryParser("syn").entityId();
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

	@Test(expected = ParseException.class)
	public void testIdAndVersionNoSyn() throws ParseException {
		new TableQueryParser("123.456").entityId();
	}
}
