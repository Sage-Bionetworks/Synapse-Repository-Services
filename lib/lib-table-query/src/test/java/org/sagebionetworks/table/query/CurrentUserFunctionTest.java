package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.CurrentUserFunction;
import org.sagebionetworks.table.query.model.FunctionReturnType;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.UnsignedLiteral;

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
	
	@Test
	public void testReplaceElement() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 where bar = current_user()").querySpecification();
		CurrentUserFunction old = model.getFirstElementOfType(CurrentUserFunction.class);
		UnsignedLiteral replacement = new TableQueryParser("123456").unsignedLiteral();
		// call under test
		old.replaceElement(replacement);
		assertEquals("SELECT * FROM syn123 WHERE bar = 123456", model.toSql());
		assertNotNull(replacement.getParent());
		assertNull(old.getParent());
	}
}
