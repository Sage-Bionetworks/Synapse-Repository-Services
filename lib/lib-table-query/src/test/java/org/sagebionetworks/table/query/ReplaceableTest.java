package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.Element;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.TableReference;

public class ReplaceableTest {

	@Test
	public void testReplaceElement() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 group by bar, a").querySpecification();
		TableReference old = model.getFirstElementOfType(TableReference.class);
		Element oldChild = old.getChild();
		TableReference replacement = new TableQueryParser("T123").tableReference();
		// call under test
		old.replaceElement(replacement);
		assertEquals("SELECT * FROM T123 GROUP BY bar, a", model.toSql());
		assertNotNull(replacement.getParent());
		assertNull(old.getParent());
		assertNull(oldChild.getParent());
	}
	
	@Test
	public void testReplaceElementWithNullReplacement() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 group by bar, a").querySpecification();
		TableReference old = model.getFirstElementOfType(TableReference.class);
		TableReference replacement = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			old.replaceElement(replacement);
		}).getMessage();
		assertEquals("Replacement cannot be null", message);
	}
	
	@Test
	public void testReplaceElementWithNullParent() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 group by bar, a").querySpecification();
		TableReference old = model.getFirstElementOfType(TableReference.class);
		old.recursiveClearParent();
		TableReference replacement = new TableQueryParser("T123").tableReference();
		String message = assertThrows(IllegalStateException.class, ()->{
			// call under test
			old.replaceElement(replacement);
		}).getMessage();
		assertEquals("Cannot replace Element since its parent is null", message);
	}
}
