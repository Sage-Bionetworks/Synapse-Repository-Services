package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.table.query.model.CastTarget;

public class CastTargetTest {

	@Test
	public void testCastTargetWithUnsingedInteger() throws ParseException {
		CastTarget element = new TableQueryParser("123").castTarget();
		assertEquals("123", element.toSql());
	}
	
	@Test
	public void testCastTargetWithCoumnType() throws ParseException {
		CastTarget element = new TableQueryParser(ColumnType.BOOLEAN_LIST.name()).castTarget();
		assertEquals("BOOLEAN_LIST", element.toSql());
	}
}
