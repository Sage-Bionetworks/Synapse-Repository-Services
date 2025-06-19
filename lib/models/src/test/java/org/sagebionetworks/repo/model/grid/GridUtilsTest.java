package org.sagebionetworks.repo.model.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class GridUtilsTest {
	
	@Test
	public void testGridSessionIdTranslation() {
		Long sessionId = 1234567L;
		String asString = "MTIzNDU2Nw==";
		// call under test
		assertEquals(asString, GridUtils.gridSessionIdAsString(sessionId));
		// call under test
		assertEquals(sessionId, GridUtils.gridSessionIdAsLong(asString));
	}
	
	@Test
	public void testGridSessionIdAsLongWithNullId() {
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			GridUtils.gridSessionIdAsLong(null);
		}).getMessage();
		assertEquals("id is required.", message);
	}

	@Test
	public void testGridSessionIdAsStringWithNullId() {
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			GridUtils.gridSessionIdAsString(null);
		}).getMessage();
		assertEquals("id is required.", message);
	}

}
