package org.sagebionetworks.repo.model.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.sagebionetworks.repo.model.entity.IdAndVersionParser.ParseException;


public class IdAndVersionParserTest {
	
	@Test
	public void testAllParts() {
		// call under test
		IdAndVersion id = IdAndVersionParser.parseIdAndVersion("syn123.456");
		assertNotNull(id);
		assertEquals(new Long(123), id.getId());
		assertEquals(new Long(456), id.getVersion().get());
	}
	
	@Test
	public void testUppderSyn() {
		// call under test
		IdAndVersion id = IdAndVersionParser.parseIdAndVersion("SYN123.456");
		assertNotNull(id);
		assertEquals(new Long(123), id.getId());
		assertEquals(new Long(456), id.getVersion().get());
	}
	
	@Test
	public void testMixedCase() {
		// call under test
		IdAndVersion id = IdAndVersionParser.parseIdAndVersion("sYn123.456");
		assertNotNull(id);
		assertEquals(new Long(123), id.getId());
		assertEquals(new Long(456), id.getVersion().get());
	}
	
	@Test
	public void testJustIdAllDigits() {
		// call under test
		IdAndVersion id = IdAndVersionParser.parseIdAndVersion("1234567890");
		assertNotNull(id);
		assertEquals(new Long(1234567890), id.getId());
		assertFalse(id.getVersion().isPresent());
	}

	@Test
	public void testSingleDigit() {
		// call under test
		IdAndVersion id = IdAndVersionParser.parseIdAndVersion("1");
		assertNotNull(id);
		assertEquals(new Long(1), id.getId());
		assertFalse(id.getVersion().isPresent());
	}
	
	@Test
	public void testNoSyn() {
		// call under test
		IdAndVersion id = IdAndVersionParser.parseIdAndVersion("7890.456");
		assertNotNull(id);
		assertEquals(new Long(7890), id.getId());
		assertEquals(new Long(456), id.getVersion().get());
	}
	
	@Test
	public void testWhiteSpace() {
		// call under test
		IdAndVersion id = IdAndVersionParser.parseIdAndVersion(" \t\nsyn123.456\n\t ");
		assertNotNull(id);
		assertEquals(new Long(123), id.getId());
		assertEquals(new Long(456), id.getVersion().get());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testNullString() {
		// call under test
		IdAndVersionParser.parseIdAndVersion(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testEmpty() {
		// call under test
		IdAndVersionParser.parseIdAndVersion("");
	}
	
	@Test
	public void testMissingY() {
		try {
			// call under test
			IdAndVersionParser.parseIdAndVersion("sny123");
			fail();
		}catch(IllegalArgumentException e) {
			assertTrue(e.getCause() instanceof ParseException);
			assertEquals(1, ((ParseException)e.getCause()).getErrorIndex());
		}
	}
	
	@Test
	public void testMissingN() {
		try {
			// call under test
			IdAndVersionParser.parseIdAndVersion("syy123");
			fail();
		}catch(IllegalArgumentException e) {
			assertTrue(e.getCause() instanceof ParseException);
			assertEquals(2, ((ParseException)e.getCause()).getErrorIndex());
		}
	}
	
	@Test
	public void testNonDigitsBelow() {
		try {
			// call under test
			IdAndVersionParser.parseIdAndVersion("syn123/456");
			fail();
		}catch(IllegalArgumentException e) {
			assertTrue(e.getCause() instanceof ParseException);
			assertEquals(6, ((ParseException)e.getCause()).getErrorIndex());
		}
	}
	@Test
	public void testNonDigitsAbove() {
		try {
			// call under test
			IdAndVersionParser.parseIdAndVersion("syn123:456");
			fail();
		}catch(IllegalArgumentException e) {
			assertTrue(e.getCause() instanceof ParseException);
			assertEquals(6, ((ParseException)e.getCause()).getErrorIndex());
		}
	}
	
	@Test
	public void testTrailingJunck() {
		try {
			// call under test
			IdAndVersionParser.parseIdAndVersion("syn123.456 foo");
			fail();
		}catch(IllegalArgumentException e) {
			assertTrue(e.getCause() instanceof ParseException);
			assertEquals(11, ((ParseException)e.getCause()).getErrorIndex());
		}
	}
	
	@Test
	public void testMissingId() {
		try {
			// call under test
			IdAndVersionParser.parseIdAndVersion("syn.456");
			fail();
		}catch(IllegalArgumentException e) {
			assertTrue(e.getCause() instanceof ParseException);
			assertEquals(3, ((ParseException)e.getCause()).getErrorIndex());
		}
	}
	
	@Test
	public void testMissingVersion() {
		try {
			// call under test
			IdAndVersionParser.parseIdAndVersion("syn0.");
			fail();
		}catch(IllegalArgumentException e) {
			assertTrue(e.getCause() instanceof ParseException);
			assertEquals(5, ((ParseException)e.getCause()).getErrorIndex());
		}
	}
	
	@Test
	public void testTooManyDigits() {
		try {
			// call under test
			IdAndVersionParser.parseIdAndVersion("syn92233720368547758071");
			fail();
		}catch(IllegalArgumentException e) {
			assertTrue(e.getCause() instanceof ParseException);
			assertEquals(23, ((ParseException)e.getCause()).getErrorIndex());
		}
	}

}
