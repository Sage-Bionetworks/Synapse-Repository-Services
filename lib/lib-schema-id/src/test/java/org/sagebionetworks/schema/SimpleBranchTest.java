package org.sagebionetworks.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.schema.element.Element;
import org.sagebionetworks.schema.element.SimpleBranch;
import org.sagebionetworks.schema.element.SimpleString;

public class SimpleBranchTest {

	@Test
	public void testToString() {
		String input = "abc";
		SimpleString element = new SimpleString(input);
		SimpleBranch branch = new SimpleBranch(element);
		assertEquals(input, branch.toString());
	}

	@Test
	public void testNullChild() {
		Element child = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new SimpleBranch(child);
		}).getMessage();
		assertEquals("Child cannot be null", message);
	}
}
