package org.sagebionetworks.table.query;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.util.SqlElementUtils;

public class ColumnReferenceTest {

	@Test
	public void testToSQL() throws ParseException {
		ColumnReference ref = SqlElementUtils.createColumnReference("lhs.rhs");
		assertEquals("lhs.rhs", ref.toString());
	}

	@Test
	public void testToSQLNoRHS() throws ParseException {
		ColumnReference ref = SqlElementUtils.createColumnReference("lhs");
		assertEquals("lhs", ref.toString());
	}

	@Test
	public void testToSQLDelimited() throws ParseException {
		ColumnReference ref = SqlElementUtils.createColumnReference("\"has space\".\"has\"\"quote\"");
		assertEquals("\"has space\".\"has\"\"quote\"", ref.toString());
	}

	@Test
	public void testGetChidren() throws ParseException {
		ColumnReference element = new TableQueryParser("\"has space\".\"has\"\"quote\"").columnReference();
		assertEquals(Arrays.asList(element.getNameLHS().get(), element.getNameRHS()), element.getChildren());
	}
	
	@Test
	public void testNameLHSWithLHS() throws ParseException {
		ColumnReference element = new TableQueryParser("lhs.rhs").columnReference();
		assertTrue(element.getNameLHS().isPresent());
		assertEquals("lhs", element.getNameLHS().get().toSql());
	}
	
	@Test
	public void testNameLHSWithoutLHS() throws ParseException {
		ColumnReference element = new TableQueryParser("rhs").columnReference();
		assertFalse(element.getNameLHS().isPresent());
	}
	
	@Test
	public void testReplaceElement() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 group by bar, a").querySpecification();
		ColumnReference old = model.getFirstElementOfType(ColumnReference.class);
		ColumnReference replacement = new TableQueryParser("_C123_").columnReference();
		// call under test
		old.replaceElement(replacement);
		assertEquals("SELECT * FROM syn123 GROUP BY _C123_, a", model.toSql());
		assertNotNull(replacement.getParent());
		assertNull(old.getParent());
	}
}
