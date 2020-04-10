package org.sagebionetworks.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.schema.element.ElementList;
import org.sagebionetworks.schema.element.SimpleString;

import nl.jqno.equalsverifier.EqualsVerifier;

public class ElementListTest {

	String delimiter;

	@BeforeEach
	public void before() {
		delimiter = "#";
	}

	@Test
	public void testToString() {
		ElementList<SimpleString> stringList = new ElementList<SimpleString>(delimiter);
		assertEquals("", stringList.toString());
		stringList.add(new SimpleString("foo"));
		assertEquals("foo", stringList.toString());
		stringList.add(new SimpleString("bar"));
		assertEquals("foo#bar", stringList.toString());
		stringList.add(new SimpleString("foobar"));
		assertEquals("foo#bar#foobar", stringList.toString());
	}
	
	@Test
	public void testIterator() {
		ElementList<SimpleString> stringList = new ElementList<SimpleString>(delimiter);
		stringList.add(new SimpleString("1"));
		stringList.add(new SimpleString("2"));
		Iterator<SimpleString> it = stringList.iterator();
		assertNotNull(it);
		assertTrue(it.hasNext());
		assertEquals("1", it.next().toString());
		assertTrue(it.hasNext());
		assertEquals("2", it.next().toString());
		assertFalse(it.hasNext());
	}

	@Test
	public void testNullDelimiter() {
		delimiter = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {

			new ElementList<SimpleString>(delimiter);
		}).getMessage();
		assertEquals("Delimiter cannot be null", message);
	}

	@Test
	public void testAddNullElement() {
		ElementList<SimpleString> stringList = new ElementList<SimpleString>(delimiter);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			stringList.add(null);
		}).getMessage();
		assertEquals("Cannot add a null element", message);
	}

	@Test
	public void testHashEquals() {
		EqualsVerifier.forClass(ElementList.class).verify();
	}

}
