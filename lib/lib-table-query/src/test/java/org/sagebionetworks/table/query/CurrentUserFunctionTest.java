package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.CurrentUserFunction;
import org.sagebionetworks.table.query.model.FunctionReturnType;

public class CurrentUserFunctionTest {

	@Test
	public void testCurrentUser() throws ParseException {
		CurrentUserFunction element = new TableQueryParser("current_user()").currentUserFunction();
		assertEquals("CURRENT_USER()", element.toSql());
		assertEquals(FunctionReturnType.USERID, element.getFunctionReturnType());
	}
	
	@Test
	public void testGetChidren() throws ParseException {
		CurrentUserFunction element = new TableQueryParser("current_user()").currentUserFunction();
		assertEquals(Collections.emptyList(), element.getChildren());
	}
}
