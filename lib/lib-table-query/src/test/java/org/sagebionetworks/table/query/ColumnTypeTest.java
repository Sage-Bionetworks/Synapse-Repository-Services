package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.table.ColumnType;

public class ColumnTypeTest {

	
	@Test
	public void testEachType() throws ParseException {
		for(ColumnType type: ColumnType.values()) {
			ColumnType result = new TableQueryParser(type.name()).columnType();
			assertEquals(type, result);
		}
	}
	
	@Test
	public void testColumnTypeWithOther() {
		String message = assertThrows(IllegalArgumentException.class, ()->{
			new TableQueryParser("Other").columnType();
		}).getMessage();
		assertEquals("No enum constant org.sagebionetworks.repo.model.table.ColumnType.OTHER", message);
	}
	
}
