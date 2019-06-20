package org.sagebionetworks.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MD5ChecksumHelperTest {

	// TODO: Write tests to validate our MD5 calculator

	@Test
	public void checkValidMd5s() {
		// lowercase
		assertTrue(MD5ChecksumHelper.isValidMd5Digest("b7241f813875de12517c11394add7ae4"));

		// uppercase
		assertTrue(MD5ChecksumHelper.isValidMd5Digest("AFBEC5715A69F5A8339686FED3EB98D5"));
	}

	@Test
	public void checkInvalidMd5s() {
		// too short
		assertFalse(MD5ChecksumHelper.isValidMd5Digest("b7241f813875de12517c11394add7ae"));

		// too long
		assertFalse(MD5ChecksumHelper.isValidMd5Digest("b7241f813875de12517c11394add7ae4a"));

		// non-hexadecimal (starts with 'g')
		assertFalse(MD5ChecksumHelper.isValidMd5Digest("g7241f813875de12517c11394add7ae4"));

		// empty string
		assertFalse(MD5ChecksumHelper.isValidMd5Digest(""));

		// null
		assertThrows(IllegalArgumentException.class, () -> MD5ChecksumHelper.isValidMd5Digest(null));
	}

}