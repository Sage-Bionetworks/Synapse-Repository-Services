package org.sagebionetworks.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Code lifted from http://www.rgagnon.com/javadetails/java-0416.html
 * 
 * Found similar code later in org/apache/commons/codec/digest/DigestUtils.java
 * 
 * @author deflaux
 * 
 */
public class MD5ChecksumHelper {

	/**
	 * @param filename
	 * @return the hex version of the checksum
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public static String getMD5Checksum(String filename) throws IOException {
		byte[] b = createChecksum(filename);
		return getHexString(b);
	}
	
	/**
	 * Create the MD5 of the passed file.
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static String getMD5Checksum(File file) throws IOException {
		byte[] b = createChecksum(new FileInputStream(file));
		return getHexString(b);
	}
	
	public static String getMD5ChecksumForString(String content) throws IOException {
		ByteArrayInputStream inputStream = null;
		try {
			inputStream = new ByteArrayInputStream(content.getBytes());
			byte[] raw = createChecksum(inputStream);
			return getHexString(raw);
		} finally {
			if (inputStream!=null) inputStream.close();
		}
	}

	/**
	 * @param filename
	 * @return the checksum as a byte array
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public static byte[] createChecksum(String filename) throws IOException {
		InputStream fis = new FileInputStream(filename);
		return createChecksum(fis);
	}

	public static byte[] createChecksum(InputStream fis) throws IOException {
		byte[] buffer = new byte[1024];
		MessageDigest complete;
		try {
			complete = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e.getMessage());
		}
		int numRead;
		do {
			numRead = fis.read(buffer);
			if (numRead > 0) {
				complete.update(buffer, 0, numRead);
			}
		} while (numRead != -1);
		fis.close();
		return complete.digest();
	}

	static final byte[] HEX_CHAR_TABLE = { (byte) '0', (byte) '1', (byte) '2',
			(byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7',
			(byte) '8', (byte) '9', (byte) 'a', (byte) 'b', (byte) 'c',
			(byte) 'd', (byte) 'e', (byte) 'f' };

	/**
	 * @param raw
	 * @return convert a byte array to hex
	 * @throws UnsupportedEncodingException
	 */
	public static String getHexString(byte[] raw)
			throws UnsupportedEncodingException {
		byte[] hex = new byte[2 * raw.length];
		int index = 0;

		for (byte b : raw) {
			int v = b & 0xFF;
			hex[index++] = HEX_CHAR_TABLE[v >>> 4];
			hex[index++] = HEX_CHAR_TABLE[v & 0xF];
		}
		return new String(hex, "ASCII");
	}

}
