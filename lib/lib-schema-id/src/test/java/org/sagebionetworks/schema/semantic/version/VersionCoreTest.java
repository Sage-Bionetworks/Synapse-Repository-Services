package org.sagebionetworks.schema.semantic.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class VersionCoreTest {

	NumericIdentifier major;
	NumericIdentifier minor;
	NumericIdentifier patch;

	@BeforeEach
	public void before() {
		major = new NumericIdentifier(1L);
		minor = new NumericIdentifier(2L);
		patch = new NumericIdentifier(3L);
	}

	@Test
	public void testToString() {
		VersionCore core = new VersionCore(major, minor, patch);
		assertEquals("1.2.3", core.toString());
	}

	@Test
	public void testMajorNull() {
		major = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			new VersionCore(major, minor, patch);
		}).getMessage();
		assertEquals("Major cannot be null", message);
	}
	
	@Test
	public void testMinorNull() {
		minor = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			new VersionCore(major, minor, patch);
		}).getMessage();
		assertEquals("Minor cannot be null", message);
	}
	
	@Test
	public void testPatchNull() {
		patch = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			new VersionCore(major, minor, patch);
		}).getMessage();
		assertEquals("Patch cannot be null", message);
	}
	
	@Test
	public void testHashAndEquals() {
		EqualsVerifier.forClass(VersionCore.class).verify();
	}

}
