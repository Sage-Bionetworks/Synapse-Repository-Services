package org.sagebionetworks.utils;

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
	public static String getMD5Checksum(String filename)
			throws NoSuchAlgorithmException, IOException {
		byte[] b = createChecksum(filename);
		return getHexString(b);
	}

	/**
	 * @param filename
	 * @return the checksum as a byte array
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public static byte[] createChecksum(String filename) throws IOException,
			NoSuchAlgorithmException {
		InputStream fis = new FileInputStream(filename);

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
