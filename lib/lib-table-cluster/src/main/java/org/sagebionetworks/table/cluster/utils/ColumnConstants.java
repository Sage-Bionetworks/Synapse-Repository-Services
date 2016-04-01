package org.sagebionetworks.table.cluster.utils;

import java.io.UnsupportedEncodingException;

public class ColumnConstants {

	public static final int MAX_BYTES_PER_CHAR_UTF_8;
	static{
		char[] chars = new char[]{Character.MAX_VALUE};
		try {
			MAX_BYTES_PER_CHAR_UTF_8 = new String(chars).getBytes("UTF-8").length;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * The maximum number of bytes of a boolean when represented as a string.
	 */
	public static final int MAX_BOOLEAN_BYTES_AS_STRING;
	static{
		try {
			MAX_BOOLEAN_BYTES_AS_STRING = "FALSE".getBytes("UTF-8").length;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	/**
	 * The maximum number of bytes of a integer (a long in java terms) when represented as a string.
	 */
	public static final int MAX_INTEGER_BYTES_AS_STRING;
	static{
		try {
			MAX_INTEGER_BYTES_AS_STRING = Long.toString(-Long.MAX_VALUE).getBytes("UTF-8").length;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	/**
	 * The maximum number of bytes of a double when represented as a string.
	 */
	public static final int MAX_DOUBLE_BYTES_AS_STRING;
	static{
		try {
			MAX_DOUBLE_BYTES_AS_STRING = Double.toString(-Double.MAX_VALUE).getBytes("UTF-8").length;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	/**
	 * The maximum number of bytes of a FileHandle ID when represented as a string (same as long).
	 */
	public static final int MAX_FILE_HANDLE_ID_BYTES_AS_STRING = MAX_INTEGER_BYTES_AS_STRING;
	/**
	 * The maximum number of bytes of an entity ID when represented as a string (syn<id>[.<version>)
	 */
	public static final int MAX_ENTITY_ID_BYTES_AS_STRING = 3 + MAX_INTEGER_BYTES_AS_STRING + 1 + MAX_INTEGER_BYTES_AS_STRING;
	
	/**
	 * While the database will not count the bytes of a blob against the total size of 
	 * of a row, we still need an estimate of the size of these blobs in memory.  
	 * This will limit the size of table change sets and the maximum number of blob columns
	 * that can be added to a table.
	 * 
	 */
	public static final int DEFAULT_LARGE_TEXT_BYTES = MAX_BYTES_PER_CHAR_UTF_8 *1000;
	
	/**
	 * Large text values must be under one MB.
	 * 
	 */
	public static final long MAX_LARGE_TEXT_BYTES = 1024*1024; // 1 MB
	
	/**
	 * The maximum number of characters allowed for a LARGETEXT value.
	 */
	public static final long MAX_LARGE_TEXT_CHARACTERS = MAX_LARGE_TEXT_BYTES/MAX_BYTES_PER_CHAR_UTF_8;
}
