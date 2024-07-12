package org.sagebionetworks.repo.model.dbo.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class MigrationFileTypeTest {

	@Test
	public void testFromFileName() {

		assertEquals(MigrationFileType.JSON, MigrationFileType.fromFileName("foo.json"));
		assertEquals(MigrationFileType.JSON, MigrationFileType.fromFileName("bar.JSON"));
		assertEquals(MigrationFileType.JSON, MigrationFileType.fromFileName("foo.9.json"));
		assertEquals(MigrationFileType.XML, MigrationFileType.fromFileName("foo.xml"));
		assertEquals(MigrationFileType.XML, MigrationFileType.fromFileName("foo.XML"));
		assertEquals(MigrationFileType.XML, MigrationFileType.fromFileName("foo.1.XML"));
	}

	@Test
	public void testFromFileNameWithNull() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			MigrationFileType.fromFileName(null);
		}).getMessage();
		assertEquals("fileName is required.", message);
	}
	
	@Test
	public void testFromFileNameWithNoDots() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			MigrationFileType.fromFileName("nodots");
		}).getMessage();
		assertEquals("Unknown file type: 'nodots'", message);
	}
}
