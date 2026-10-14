package org.sagebionetworks.repo.manager.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

public class AllowedSessionFileTypeTest {

	@Test
	public void testMatchByContentType() {
		assertEquals(Optional.of(AllowedSessionFileType.CSV), AllowedSessionFileType.match("text/csv", null));
		assertEquals(Optional.of(AllowedSessionFileType.PDF), AllowedSessionFileType.match("application/pdf", null));
		assertEquals(Optional.of(AllowedSessionFileType.TXT), AllowedSessionFileType.match("text/plain", null));
		assertEquals(Optional.of(AllowedSessionFileType.JSON), AllowedSessionFileType.match("application/json", null));
		assertEquals(Optional.of(AllowedSessionFileType.TSV),
				AllowedSessionFileType.match("text/tab-separated-values", null));
	}

	@Test
	public void testMatchByContentTypeIgnoresCaseAndParameters() {
		// Content types are matched case-insensitively and ignore parameters like charset.
		assertEquals(Optional.of(AllowedSessionFileType.CSV),
				AllowedSessionFileType.match("TEXT/CSV; charset=UTF-8", null));
	}

	@Test
	public void testMatchByExtensionWhenContentTypeGeneric() {
		// A generic/unknown content type still matches on the file name extension.
		assertEquals(Optional.of(AllowedSessionFileType.CSV),
				AllowedSessionFileType.match("application/octet-stream", "data.csv"));
		assertEquals(Optional.of(AllowedSessionFileType.PDF),
				AllowedSessionFileType.match(null, "report.PDF"));
		assertEquals(Optional.of(AllowedSessionFileType.TSV),
				AllowedSessionFileType.match("application/octet-stream", "data.tsv"));
		assertEquals(Optional.of(AllowedSessionFileType.MAF),
				AllowedSessionFileType.match("application/octet-stream", "variants.maf"));
	}

	@Test
	public void testMatchWithUnsupportedType() {
		assertTrue(AllowedSessionFileType.match("image/png", "picture.png").isEmpty());
		assertTrue(AllowedSessionFileType.match(null, "archive.zip").isEmpty());
		assertTrue(AllowedSessionFileType.match(null, "noextension").isEmpty());
		assertTrue(AllowedSessionFileType.match(null, null).isEmpty());
	}

	@Test
	public void testDescribeAllowed() {
		String description = AllowedSessionFileType.describeAllowed();
		assertTrue(description.contains("PDF"));
		assertTrue(description.contains("CSV"));
		assertTrue(description.contains("TXT"));
		assertTrue(description.contains("JSON"));
		assertTrue(description.contains("TSV"));
		assertTrue(description.contains("MAF"));
	}

	@Test
	public void testMatchTrailingDotIsNotAnExtension() {
		// A name ending in a dot has no extension and no matching content type.
		assertFalse(AllowedSessionFileType.match(null, "weird.").isPresent());
	}
}
