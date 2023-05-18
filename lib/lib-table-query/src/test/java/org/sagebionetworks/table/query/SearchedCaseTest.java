package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.SearchedCase;

public class SearchedCaseTest {

	@Test
	public void testSimpleCase() throws ParseException {
		SearchedCase element = new TableQueryParser("when foo > bar then 1").searchedCase();
		assertEquals("WHEN foo > bar THEN 1", element.toSql());
	}
	
	@Test
	public void testSimpleCaseWithElse() throws ParseException {
		SearchedCase element = new TableQueryParser("when foo > bar then 1 else 0").searchedCase();
		assertEquals("WHEN foo > bar THEN 1 ELSE 0", element.toSql());
	}
}
