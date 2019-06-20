package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FileHandleUtilsTest {

	@Test
	public void checkValidMd5s() {
		// lowercase
		assertTrue(FileHandleUtils.isValidMd5Digest("b7241f813875de12517c11394add7ae4"));

		// uppercase
		assertTrue(FileHandleUtils.isValidMd5Digest("AFBEC5715A69F5A8339686FED3EB98D5"));
	}

	@Test
	public void checkInvalidMd5s() {
		// too short
		assertFalse(FileHandleUtils.isValidMd5Digest("b7241f813875de12517c11394add7ae"));

		// too long
		assertFalse(FileHandleUtils.isValidMd5Digest("b7241f813875de12517c11394add7ae4a"));

		// non-hexadecimal (starts with 'g')
		assertFalse(FileHandleUtils.isValidMd5Digest("g7241f813875de12517c11394add7ae4"));

		// empty string
		assertFalse(FileHandleUtils.isValidMd5Digest(""));

		// null
		assertThrows(IllegalArgumentException.class, () -> FileHandleUtils.isValidMd5Digest(null));
	}

}