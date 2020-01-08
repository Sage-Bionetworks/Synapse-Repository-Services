package org.sagebionetworks.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.jupiter.api.Test;

class MD5ChecksumHelperTest {

	@Test
	public void checkValidMD5LowerCase() {
		assertTrue(MD5ChecksumHelper.isValidMd5Digest("b7241f813875de12517c11394add7ae4"));
	}

	@Test
	public void checkValidMD5UpperCase() {
		assertTrue(MD5ChecksumHelper.isValidMd5Digest("AFBEC5715A69F5A8339686FED3EB98D5"));
	}

	@Test
	public void checkInvalidMD5TooShort() {
		assertFalse(MD5ChecksumHelper.isValidMd5Digest("b7241f813875de12517c11394add7ae"));
	}

	@Test
	public void checkInvalidMD5TooLong() {
		assertFalse(MD5ChecksumHelper.isValidMd5Digest("b7241f813875de12517c11394add7ae4a"));
	}

	@Test
	public void checkInvalidMD5NonHexadecimal() {
		// starts with 'g'
		assertFalse(MD5ChecksumHelper.isValidMd5Digest("g7241f813875de12517c11394add7ae4"));
	}

	@Test
	public void checkInvalidMD5EmptyString() {
		assertFalse(MD5ChecksumHelper.isValidMd5Digest(""));
	}

	@Test
	public void checkInvalidMD5Null() {
		assertThrows(IllegalArgumentException.class, () -> MD5ChecksumHelper.isValidMd5Digest(null));
	}

	@Test
	public void checkValidMD5_Base64() {
		assertTrue(MD5ChecksumHelper.isValidMd5Digest("+XHdk3AQ65DR96cC0n7N4g=="));
	}

	@Test
	public void checkInValidMD5_Base64_IncorrectStringLength() {
		assertFalse(MD5ChecksumHelper.isValidMd5Digest("k3AQ65DR96cC0n7N4g=="));
		assertFalse(MD5ChecksumHelper.isValidMd5Digest("7N4g+XHdk3AQ65DR96cC0n7N4g=="));
	}

	@Test
	public void checkInValidMD5_Base64_notBase64String() {
		//string that is size 24 but not in base64 character set
		assertFalse(MD5ChecksumHelper.isValidMd5Digest(String.join("", Collections.nCopies(24, "?"))));
	}

	@Test
	public void checkInValidMD5_Base64_IncorrectDecodedByteLength() {
		// "=" is a padding string which means it does not convert into any bits.
		// this string has an additional value replaced with a padding character.
		assertFalse(MD5ChecksumHelper.isValidMd5Digest("+XHdk3AQ65DR96cC0n7N4==="));
	}

	@Test
	public void testGetMD5Checksum() throws Exception {
		String result = MD5ChecksumHelper.getMD5Checksum(new ByteArrayInputStream("test string".getBytes(StandardCharsets.UTF_8)));
		// Note: this test is only valid if the above tests pass!
		assertTrue(MD5ChecksumHelper.isValidMd5Digest(result));
	}


}