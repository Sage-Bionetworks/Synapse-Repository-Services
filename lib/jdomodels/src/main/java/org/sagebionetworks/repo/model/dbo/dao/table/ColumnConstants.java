package org.sagebionetworks.repo.model.dbo.dao.table;

public class ColumnConstants {

	/**
	 * The maximum number of characters in a string column.
	 */
	public static final int MAX_CHARS_IN_STRING_COLUMN = 2000;
	/**
	 * The maximum number of bytes of a string.
	 */
	public static final int MAX_STRING_BYTES = new String(new char[MAX_CHARS_IN_STRING_COLUMN]).getBytes().length;
	/**
	 * The maximum number of bytes of a boolean when represented as a string.
	 */
	public static final int MAX_BOOLEAN_BYTES_AS_STRING = "FALSE".getBytes().length;
	/**
	 * The maximum number of bytes of a long when represented as a string.
	 */
	public static final int MAX_LONG_BYTES_AS_STRING = Long.toString(-Long.MAX_VALUE).getBytes().length;
	/**
	 * The maximum number of bytes of a double when represented as a string.
	 */
	public static final int MAX_DOUBLE_BYTES_AS_STRING = Double.toString(-Double.MAX_VALUE).getBytes().length;
	/**
	 * The maximum number of bytes of a FileHandle ID when represented as a string (same as long).
	 */
	public static final int MAX_FILE_HANDLE_ID_BYTES_AS_STRING = MAX_LONG_BYTES_AS_STRING;
}
