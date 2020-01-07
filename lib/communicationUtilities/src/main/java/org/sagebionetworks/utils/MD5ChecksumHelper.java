package org.sagebionetworks.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Utilities for calculating and verifying MD5s
 */
public class MD5ChecksumHelper {

	// Exactly 32 characters, hexadecimal.
	private static final Pattern VALID_MD5_REGEX = Pattern.compile("^[0-9a-fA-F]{32}$");

	/**
	 * Determines if a hexadecimal string is a valid MD5 digest (i.e. is exactly 32 hexadecimal characters).
	 * @param hexDigest The hexadecimal string
	 * @return true iff the string is a valid MD5 digest
	 */
	public static boolean isValidMd5Digest(String hexDigest) {
		if (hexDigest == null) throw new IllegalArgumentException("The MD5 digest may not be null");
		return VALID_MD5_REGEX.matcher(hexDigest).matches();
	}

	/**
	 * Compute the MD5 for a file
	 * @param filename
	 * @return the hex version of the checksum
	 * @throws IOException
	 */
	public static String getMD5Checksum(String filename) throws IOException {
		return getMD5Checksum(new FileInputStream(filename));
	}
	
	/**
	 * Create the MD5 of the passed file.
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static String getMD5Checksum(File file) throws IOException {
		return getMD5Checksum(new FileInputStream(file));
	}
	
	/**
	 * Computes the MD5 for the byte array created from the String 
	 * @param content
	 * @return
	 * @throws IOException
	 */
	public static String getMD5Checksum(byte[] content) throws IOException {
		return getMD5Checksum(new ByteArrayInputStream(content));
	}

	/**
	 * Computes the MD5 for an input stream. Closes the input stream upon completion
	 * @param is
	 * @return
	 * @throws IOException
	 */
	public static String getMD5Checksum(InputStream is) throws IOException {
		try {
			return DigestUtils.md5Hex(is);
		} finally {
			is.close();
		}
	}

}
