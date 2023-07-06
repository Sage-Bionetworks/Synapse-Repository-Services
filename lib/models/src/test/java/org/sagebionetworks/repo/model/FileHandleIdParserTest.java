package org.sagebionetworks.repo.model;


import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.file.FileHandleIdParser;
import org.sagebionetworks.repo.model.file.FileHandleIdParser.ParseException;
import static org.junit.jupiter.api.Assertions.*;


public class FileHandleIdParserTest {
	
	@Test
	public void testAllParts() {
		// call under test
		String id = FileHandleIdParser.parseFileHandleId("fh123");
		assertNotNull(id);
		assertEquals("123", id);
	}
	
	@Test
	public void testUpperFh() {
		// call under test
		String id = FileHandleIdParser.parseFileHandleId("FH123");
		assertNotNull(id);
		assertEquals("123", id);
	}
	
	@Test
	public void testMixedCase() {
		// call under test
		String id = FileHandleIdParser.parseFileHandleId("Fh123");
		assertNotNull(id);
		assertEquals("123", id);
	}
	
	@Test
	public void testJustDigits() {
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			FileHandleIdParser.parseFileHandleId("1234567890");
		});
	}

	@Test
	public void testWhiteSpace() {
		// call under test
		String id = FileHandleIdParser.parseFileHandleId(" \t\nfh123\n\t ");
		assertNotNull(id);
		assertEquals("123", id);
	}
	
	@Test
	public void testNullString() {
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			FileHandleIdParser.parseFileHandleId(null);
		});
	}
	
	@Test
	public void testEmpty() {
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			FileHandleIdParser.parseFileHandleId("");
		});
	}
	
	@Test
	public void testMissingh() {
		try {
			// call under test
			FileHandleIdParser.parseFileHandleId("f123");
			fail();
		}catch(IllegalArgumentException e) {
			assertTrue(e.getCause() instanceof ParseException);
			assertEquals(1, ((ParseException)e.getCause()).getErrorIndex());
		}
	}
	
	@Test
	public void testMissingf() {
		try {
			// call under test
			FileHandleIdParser.parseFileHandleId("h123");
			fail();
		}catch(IllegalArgumentException e) {
			System.out.println(e.getCause());
			assertTrue(e.getCause() instanceof ParseException);
			assertEquals(0, ((ParseException)e.getCause()).getErrorIndex());
		}
	}
	
	@Test
	public void testNonDigitsBelow() {
		try {
			// call under test
			FileHandleIdParser.parseFileHandleId("fh123/456");
			fail();
		}catch(IllegalArgumentException e) {
			assertTrue(e.getCause() instanceof ParseException);
			assertEquals(5, ((ParseException)e.getCause()).getErrorIndex());
		}
	}
	@Test
	public void testNonDigitsAbove() {
		try {
			// call under test
			FileHandleIdParser.parseFileHandleId("fh123:456");
			fail();
		}catch(IllegalArgumentException e) {
			assertTrue(e.getCause() instanceof ParseException);
			assertEquals(5, ((ParseException)e.getCause()).getErrorIndex());
		}
	}
	
	@Test
	public void testTrailingJunk() {
		try {
			// call under test
			FileHandleIdParser.parseFileHandleId("fh123456 foo");
			fail();
		}catch(IllegalArgumentException e) {
			assertTrue(e.getCause() instanceof ParseException);
			assertEquals(9, ((ParseException)e.getCause()).getErrorIndex());
		}
	}
	
	@Test
	public void testMissingId() {
		try {
			// call under test
			FileHandleIdParser.parseFileHandleId("fh");
			fail();
		}catch(IllegalArgumentException e) {
			assertTrue(e.getCause() instanceof ParseException);
			assertEquals(2, ((ParseException)e.getCause()).getErrorIndex());
		}
	}

	@Test
	public void testTooManyDigits() {
		try {
			// call under test
			FileHandleIdParser.parseFileHandleId("fh92233720368547758071");
			fail();
		}catch(IllegalArgumentException e) {
			assertTrue(e.getCause() instanceof ParseException);
			assertEquals(22, ((ParseException)e.getCause()).getErrorIndex());
		}
	}

	@Test
	public void testStartsWithFhAllParts() {
		// call under test
		assertEquals(true, FileHandleIdParser.startsWithFh("fh123"));
	}

	@Test
	public void testStartsWithFhUpperFh() {
		// call under test
		assertEquals(true, FileHandleIdParser.startsWithFh("FH123"));
	}

	@Test
	public void testStartsWithFhMixedCase() {
		// call under test
		assertEquals(true, FileHandleIdParser.startsWithFh("Fh123"));
	}

	@Test
	public void testStartsWithFhJustDigits() {
		// call under test
		assertEquals(false, FileHandleIdParser.startsWithFh("12345"));
	}

	@Test
	public void testStartsWithFhWhiteSpace() {
		// call under test
		assertEquals(true, FileHandleIdParser.startsWithFh(" \t\nfh123\n\t "));
	}

	@Test
	public void testStartsWithFhNullString() {
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			FileHandleIdParser.startsWithFh(null);
		});
	}

	@Test
	public void testStartsWithFhEmpty() {
		// call under test
		assertThrows(IllegalArgumentException.class, () -> {
			FileHandleIdParser.startsWithFh("");
		});
	}

	@Test
	public void testStartsWithFhMissingh() {
		// call under test
		assertEquals(false, FileHandleIdParser.startsWithFh("f123"));
	}

	@Test
	public void testStartswithFhMissingf() {
		// call under test
		assertEquals(false, FileHandleIdParser.startsWithFh("f123"));
	}
}
