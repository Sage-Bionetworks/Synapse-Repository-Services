package org.sagebionetworks.repo.model.table;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ColumnConstants {

	/**
	 * With the upgrade to MySQL 8, the default character set for
	 * tables changed from 'utf8' to 'utf8mb4'  The old 'utf8' is not a 
	 * 'true' utf-8 as it is limited to only three bytes per character or less.  True
	 * utf-8 requires a maximum of 4 bytes per character or less.  MySQL addressed
	 * this issue by adding a 'utf8mb4' which means utf-8 max bytes 4.;
	 */
	public static final int MAX_BYTES_PER_CHAR_UTF_8 = 4;
	
	/**
	 * It appears that the amount of memory used per character
	 * is dependent on the JVM implementation.  Four bytes
	 * per character seems to be the maximum.
	 */
	public static final int MAX_BYTES_PER_CHAR_MEMORY = 4;
	
	/**
	 * This is the maximum number of bytes for a single row in MySQL.
	 * This determines the maximum schema size for a table.
	 */
	public static final int MY_SQL_MAX_BYTES_PER_ROW = 64000;
	
	
	/**
	 * The maximum number of columns per MySQL table.
	 */
	public static final int MY_SQL_MAX_COLUMNS_PER_TABLE = 152;

	/**
	 * The maximum number of bytes of a boolean when represented as a string.
	 */
	public static final int MAX_BOOLEAN_BYTES_AS_STRING = "FALSE".getBytes(StandardCharsets.UTF_8).length;
	/**
	 * The maximum number of bytes of a integer (a long in java terms) when represented as a string.
	 */
	public static final int MAX_INTEGER_BYTES_AS_STRING = Long.toString(-Long.MAX_VALUE).getBytes(StandardCharsets.UTF_8).length;
	/**
	 * The maximum number of bytes of a double when represented as a string.
	 */
	public static final int MAX_DOUBLE_BYTES_AS_STRING = Double.toString(-Double.MAX_VALUE).getBytes(StandardCharsets.UTF_8).length;
	/**
	 * The maximum number of bytes of a FileHandle ID when represented as a string (same as long).
	 */
	public static final int MAX_FILE_HANDLE_ID_BYTES_AS_STRING = MAX_INTEGER_BYTES_AS_STRING;
	/**
	 * The maximum number of bytes of an entity ID when represented as a string (syn<id>[.<version>)
	 */
	public static final int MAX_ENTITY_ID_BYTES_AS_STRING = 3 + MAX_INTEGER_BYTES_AS_STRING + 1 + MAX_INTEGER_BYTES_AS_STRING;
	
	/**
	 * The maximum number of bytes of a User ID when represented as a string (same as long).
	 */
	public static final int MAX_USER_ID_BYTES_AS_STRING = MAX_INTEGER_BYTES_AS_STRING;
	
	/**
	 * The maximum available memory to each machine in bytes.
	 * Currently 3 GB.
	 */
	public static final long MAX_AVAILABLE_MEMORY_PER_MACHINE_BYTES = 1024L*1024L*1024L*3L;
	
	/**
	 * Sine a single table row must fit in memory.
	 * The maximum of memory for a single row is a percentage of the total available memory.
	 * Currently set to 2% of available memory.
	 */
	public static final long MAX_MEMORY_PER_ROW_BYTES = (long) (MAX_AVAILABLE_MEMORY_PER_MACHINE_BYTES*0.02);
	
	/**
	 * Large text values must be under two MB.
	 * 
	 */
	public static final long MAX_LARGE_TEXT_BYTES = 1024*1024*2; // 2 MB
	
	/**
	 * The maximum number of LARGE_TEXT columns per table is a function of the 
	 * Memory available to the machines and the maximum size of single LARGE_TEXT value. 
	 */
	public static final long MAX_NUMBER_OF_LARGE_TEXT_COLUMNS_PER_TABLE = MAX_MEMORY_PER_ROW_BYTES/MAX_LARGE_TEXT_BYTES;
	
	/**
	 * Conversion of the maximum number of LARGE_TEXT columns in memory to a size in terms of the max bytes per row in MySQL.
	 */
	public static final int SIZE_OF_LARGE_TEXT_FOR_COLUMN_SIZE_ESTIMATE_BYTES = (int) (MY_SQL_MAX_BYTES_PER_ROW/MAX_NUMBER_OF_LARGE_TEXT_COLUMNS_PER_TABLE);
	
	/**
	 * The maximum number of characters allowed for a LARGETEXT value.
	 */
	public static final long MAX_LARGE_TEXT_CHARACTERS = MAX_LARGE_TEXT_BYTES/MAX_BYTES_PER_CHAR_UTF_8;
	
	public static final String CHARACTER_SET_UTF8_COLLATE_UTF8_GENERAL_CI = "CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci";
	
	/**
	 * When we switched from the MySQL proprietary 3 byte 'utf8' to the real 4 byte 'utf8mb4' we had tables
	 * that were already close to the MySQL maximum size limit.  Switching these tables to use
	 * the 4 byte UTF-8 would push these tables over the limit.  Therefore, we continue to build
	 * these tables using the proprietary 3 byte 'utf8', while all other tables are switched to the real 
	 * 4 byte UTF-8 (utf8mb4).  See PLFM-5458.
	 */
	public static final String DEPREICATED_THREE_BYTE_UTF8 = "CHARACTER SET utf8 COLLATE utf8_general_ci";
	
	/**
	 * These are the tables that are too large for the four byte 'utf8mb4' that still
	 * must be build with the MySQL proprietary 3 byte 'utf8'. See PLFM-5458.
	 */
	@SuppressWarnings("serial")
	public static final Set<Long> TABLES_TOO_LARGE_FOR_FOUR_BYTE_UTF8 = Collections
			.unmodifiableSet(new HashSet<Long>() {
				{
					add(3420233L);
					add(3420252L);
					add(3420259L);
					add(3420485L);
					add(3474927L);
					add(3474928L);
					add(10227900L);
					add(11968325L);
				}
			});

	/**
	 * Is the given tableId too large for large for the four byte 'utf8mb4'?
	 * 
	 * @param tableId
	 * @return
	 */
	public static boolean isTableTooLargeForFourByteUtf8(Long tableId) {
		return TABLES_TOO_LARGE_FOR_FOUR_BYTE_UTF8.contains(tableId);
	}
	
	public static final int MAX_MYSQL_VARCHAR_INDEX_LENGTH = 255;
	
	/**
	 * The maximum number of characters allowed for string columns.
	 */
	public static final Long MAX_ALLOWED_STRING_SIZE = 1000L;
	
	/**
	 * The default maximum number of characters for string columns.
	 */
	public static final Long DEFAULT_STRING_SIZE = 50L;

	/**
	 * The maximum number of elements allowed for list column types.
	 */
	public static final Long MAX_ALLOWED_LIST_LENGTH = 100L;

	/**
	 * Size of a 64 bit reference in bytes.
	 */
	public static final int SIZE_OF_REFERENCE_BYTES = 64/8;
	
	/**
	 * The minimum size of a row includes rowId & versionNumber as strings plus three 64 bit references.
	 */
	public static final int MINIMUM_ROW_SIZE = MAX_INTEGER_BYTES_AS_STRING*2+SIZE_OF_REFERENCE_BYTES*3;
	
	/**
	 * The minimum size of a rows value.
	 */
	public static final int MINUMUM_ROW_VALUE_SIZE = SIZE_OF_REFERENCE_BYTES*4+80;

}
